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
import net.imglib2.display.ColorTable;
import net.imglib2.display.ColorTable16;
import net.imglib2.display.ColorTable8;
import net.imglib2.display.ScreenImage;
import net.imglib2.display.projectors.Abstract2DProjector;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.awt.converter.RealTableColorARGBConverter;
import org.knime.knip.core.awt.parametersupport.RendererWithColorTable;
import org.knime.knip.core.awt.parametersupport.RendererWithNormalization;
import org.knime.knip.core.awt.specializedrendering.Projector2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders the real values of a X,Y slice as ScreenImage. The position in the colorDim defines which of the provided
 * color tables is used.
 * 
 * 
 * @param <R>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class Real2TableColorRenderer<R extends RealType<R>> extends ProjectingRenderer<R> implements
        RendererWithNormalization, RendererWithColorTable {

    public final static Logger LOGGER = LoggerFactory.getLogger(Real2TableColorRenderer.class);

    // default
    private RealTableColorARGBConverter<R> m_converter;

    private final int m_colorDim;

    private ColorTable[] m_colorTables;

    public Real2TableColorRenderer(final int colorDim) {
        m_colorDim = colorDim;
        m_converter = new RealTableColorARGBConverter<R>(1.0, 0.0);
    }

    @Override
    public ScreenImage render(final RandomAccessibleInterval<R> source, final int dimX, final int dimY,
                              final long[] planePos) {

        // default implementation
        final ColorTable ct = m_colorTables[(int)planePos[m_colorDim]];

        if (ct instanceof ColorTable8) {
            m_converter.setColorTable((ColorTable8)ct);
        } else if (ct instanceof ColorTable16) {
            m_converter.setColorTable((ColorTable16)ct);
        } else {
            // fall back linear 8 gray ramp
            LOGGER.warn("Unsupported color table format. Using linear grey ramp.");
            m_converter.setColorTable(new ColorTable8());
        }

        return super.render(source, dimX, dimY, planePos);
    }

    @Override
    public void setNormalizationParameters(final double factor, final double min) {
        m_converter = new RealTableColorARGBConverter<R>(factor, min);
    }

    @Override
    public String toString() {
        return "ColorTable based Image Renderer (dim:" + m_colorDim + ")";
    }

    @Override
    protected Abstract2DProjector<R, ARGBType> getProjector(final int dimX, final int dimY,
                                                            final RandomAccessibleInterval<R> source,
                                                            final ARGBScreenImage target) {

        return new Projector2D<R, ARGBType>(dimX, dimY, source, target, m_converter);
    }

    @Override
    public void setColorTables(final ColorTable[] tables) {
        m_colorTables = tables.clone();
    }

}
