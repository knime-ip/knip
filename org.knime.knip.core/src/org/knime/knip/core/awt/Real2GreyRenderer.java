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
package org.knime.knip.core.awt;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.display.ScreenImage;
import net.imglib2.display.projectors.Abstract2DProjector;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.awt.converter.RealGreyARGBConverter;
import org.knime.knip.core.awt.parametersupport.RendererWithNormalization;
import org.knime.knip.core.awt.specializedrendering.FastNormalizingGreyRendering;
import org.knime.knip.core.awt.specializedrendering.Projector2D;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class Real2GreyRenderer<R extends RealType<R>> extends ProjectingRenderer<R> implements
        RendererWithNormalization {

    private double m_normalizationFactor;

    private double m_min;

    private RealGreyARGBConverter<R> m_converter;

    public Real2GreyRenderer() {
        m_converter = new RealGreyARGBConverter<R>(1.0, 0.0);
        m_normalizationFactor = 1.0;
        m_min = 0.0;
    }

    @Override
    public ScreenImage render(final RandomAccessibleInterval<R> source, final int dimX, final int dimY,
                              final long[] planePos) {

        // speed up standard cases e.g. array image...
        final ScreenImage fastResult =
                FastNormalizingGreyRendering.tryRendering(source, dimX, dimY, planePos, m_normalizationFactor, m_min);

        if (fastResult != null) {
            return fastResult;
        } else {
            // default implementation
            return super.render(source, dimX, dimY, planePos);
        }
    }

    @Override
    public void setNormalizationParameters(final double factor, final double min) {
        m_converter = new RealGreyARGBConverter<R>(factor, min);
        m_normalizationFactor = factor;
        m_min = min;
    }

    @Override
    public String toString() {
        return "Real Image Renderer";
    }

    @Override
    protected Abstract2DProjector<R, ARGBType> getProjector(final int dimX, final int dimY,
                                                            final RandomAccessibleInterval<R> source,
                                                            final ARGBScreenImage target) {

        return new Projector2D<R, ARGBType>(dimX, dimY, source, target, m_converter);
    }
}
