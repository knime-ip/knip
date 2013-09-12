/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.base.nodes.view.imgparadjust;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.ScreenImage;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.img.UnaryRelationAssigment;
import net.imglib2.ops.relation.real.unary.RealGreaterThanConstant;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.parametersupport.RendererWithNormalization;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.imgviewer.events.AWTImageChgEvent;
import org.knime.knip.core.ui.imgviewer.events.NormalizationParametersChgEvent;
import org.knime.knip.core.ui.imgviewer.panels.providers.AWTImageProvider;

/**
 * Converts an {@link Img} to a {@link BufferedImage}.
 * 
 * It creates an image from a plane selection, image, image renderer, and normalization parameters. Propagates
 * {@link AWTImageChgEvent}.
 * 
 * 
 * @param <T> the {@link Type} of the {@link Img} converted to a {@link BufferedImage}
 * @param <I> the {@link Img} converted to a {@link BufferedImage}
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ThresholdBufferedImageProvider<T extends RealType<T>> extends AWTImageProvider<T> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected NormalizationParametersChgEvent m_normalizationParameters;

    private double m_thresholdVal;

    /**
     * @param cacheSize The size of the cache beeing used in {@link AWTImageProvider}
     */
    public ThresholdBufferedImageProvider(final int cacheSize) {
        super(cacheSize);
        m_normalizationParameters = new NormalizationParametersChgEvent(0, false);
    }

    /**
     * Render an image of
     * 
     * @return
     */
    @Override
    protected Image createImage() {
        final double[] normParams = m_normalizationParameters.getNormalizationParameters(m_src, m_sel);
        RandomAccessibleInterval<BitType> tmp = null;

        if (!Double.isNaN(m_thresholdVal)) {
            final T type = Views.iterable(m_src).firstElement().createVariable();

            tmp = new ArrayImgFactory<BitType>().create(m_src, new BitType());

            type.setReal(m_thresholdVal);

            new UnaryRelationAssigment<T>(new RealGreaterThanConstant<T>(type)).compute(Views.iterable(m_src),
                                                                                        Views.iterable(tmp));

        }

        if (m_renderer instanceof RendererWithNormalization) {
            ((RendererWithNormalization)m_renderer).setNormalizationParameters(normParams[0], normParams[1]);
        }
        final ScreenImage ret =
                m_renderer.render(tmp == null ? m_src : tmp, m_sel.getPlaneDimIndex1(), m_sel.getPlaneDimIndex2(),
                                  m_sel.getPlanePos());

        return AWTImageTools.makeBuffered(ret.image());
    }

    @Override
    protected int generateHashCode() {

        int hash = (super.generateHashCode() * 31) + m_normalizationParameters.hashCode();
        hash = (hash * 31) + (int)(m_thresholdVal * 100);
        return hash;

    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.loadComponentConfiguration(in);
        m_normalizationParameters = new NormalizationParametersChgEvent();
        m_normalizationParameters.readExternal(in);
    }

    @EventListener
    public void onThresholdUpdated(final ThresholdValChgEvent threshold) {
        m_thresholdVal = threshold.getValue();
    }

    /**
     * {@link EventListener} for {@link NormalizationParametersChgEvent} events The
     * {@link NormalizationParametersChgEvent} of the {@link AWTImageTools} will be updated
     * 
     * @param normalizationParameters
     */
    @EventListener
    public void onUpdated(final NormalizationParametersChgEvent normalizationParameters) {
        m_normalizationParameters = normalizationParameters;
    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        super.saveComponentConfiguration(out);
        m_normalizationParameters.writeExternal(out);

    }
}
