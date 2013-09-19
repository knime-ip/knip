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
 * ---------------------------------------------------------------------
 *
 * Created on 19.09.2013 by zinsmaie
 */
package org.knime.knip.core.ui.imgviewer.panels.providers;

import java.awt.Image;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.ScreenImage;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.img.UnaryRelationAssigment;
import net.imglib2.ops.relation.real.unary.RealGreaterThanConstant;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.parametersupport.RendererWithNormalization;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.NormalizationParametersChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ThresholdValChgEvent;

/**
 * Renders either the original image using a selected renderer or the thresholded image.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <T> numeric type of the image that is thresholded.
 */
public class ThresholdRU<T extends RealType<T>> extends AbstractDefaultRU<BitType> {


    /** can be used instead of the threshold rendering if the selection is deactivated. */
    private ImageRU<T> m_imageRenderer = new ImageRU<T>();

    // event members

    private NormalizationParametersChgEvent m_normalizationParameters = new NormalizationParametersChgEvent(0, false);

    private RandomAccessibleInterval<T> m_src;

    private double m_thresholdVal;


    @Override
    public Image createImage() {
        if (Double.isNaN(m_thresholdVal)) {
            //selection is deactivated render normal image
            return m_imageRenderer.createImage();
        } else {
            //create a thresholded image
            final double[] normParams = m_normalizationParameters.getNormalizationParameters(m_src, m_planeSelection);

            if (m_renderer instanceof RendererWithNormalization) {
                ((RendererWithNormalization)m_renderer).setNormalizationParameters(normParams[0], normParams[1]);
            }
            RandomAccessibleInterval<BitType> bitImage = null;

            final T type = Views.iterable(m_src).firstElement().createVariable();
            bitImage = new ArrayImgFactory<BitType>().create(m_src, new BitType());
            type.setReal(m_thresholdVal);
            new UnaryRelationAssigment<T>(new RealGreaterThanConstant<T>(type)).compute(Views.iterable(m_src),
                                                                                        Views.iterable(bitImage));
            final ScreenImage ret =
                    m_renderer.render(bitImage, m_planeSelection.getPlaneDimIndex1(),
                                      m_planeSelection.getPlaneDimIndex2(), m_planeSelection.getPlanePos());

            return AWTImageTools.makeBuffered(ret.image());
        }
    }

    @Override
    public int generateHashCode() {
        int hash = super.generateHashCode();
        hash += m_normalizationParameters.hashCode();
        hash *= 31;
        hash += (int)(m_thresholdVal * 100);
        hash *= 31;
        hash += m_src.hashCode();
        hash *= 31;
        return hash;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    //event handling

    /**
     * @param e update the stored source image
     */
    @EventListener
    public void onUpdated(final IntervalWithMetadataChgEvent<T> e) {
        m_src = e.getRandomAccessibleInterval();
    }

    /**
     * @param threshold update the stored threshold value
     */
    @EventListener
    public void onThresholdUpdated(final ThresholdValChgEvent threshold) {
        m_thresholdVal = threshold.getValue();
    }

    /**
     * @param normalizationParameters update normalization parameters for saturation ...
     */
    @EventListener
    public void onUpdated(final NormalizationParametersChgEvent normalizationParameters) {
        m_normalizationParameters = normalizationParameters;
    }

    //standard methods

    @Override
    public void setEventService(final EventService service) {
        super.setEventService(service);
        m_imageRenderer.setEventService(service);
    }

    @Override
    public void saveAdditionalConfigurations(final ObjectOutput out) throws IOException {
        super.saveAdditionalConfigurations(out);
        m_normalizationParameters.writeExternal(out);

    }

    @Override
    public void loadAdditionalConfigurations(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.loadAdditionalConfigurations(in);
        m_normalizationParameters = new NormalizationParametersChgEvent();
        m_normalizationParameters.readExternal(in);
    }

}
