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
import net.imglib2.display.projectors.Abstract2DProjector;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.awt.parametersupport.RendererWithNormalization;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class Real2GreyColorRenderer<R extends RealType<R>> extends ProjectingRenderer<R> implements
        RendererWithNormalization {

    private final int m_colorDim;

    private final Real2ColorRenderer<R> m_colorRenderer;

    private final Real2GreyRenderer<R> m_greyRenderer;

    public Real2GreyColorRenderer(final int colorDim) {
        m_colorDim = colorDim;
        m_colorRenderer = new Real2ColorRenderer<R>(colorDim);
        m_greyRenderer = new Real2GreyRenderer<R>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNormalizationParameters(final double factor, final double min) {
        m_colorRenderer.setNormalizationParameters(factor, min);
        m_greyRenderer.setNormalizationParameters(factor, min);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Abstract2DProjector<R, ARGBType> getProjector(final int dimX, final int dimY,
                                                            final RandomAccessibleInterval<R> source,
                                                            final ARGBScreenImage target) { // only 2 and 3 dim are valid
        // for color rendering
        if ((m_colorDim == -1) || (source.numDimensions() <= m_colorDim) || (source.dimension(m_colorDim) <= 1)
                || (source.dimension(m_colorDim) > 3)) {
            return m_greyRenderer.getProjector(dimX, dimY, source, target);
        } else {
            return m_colorRenderer.getProjector(dimX, dimY, source, target);
        }
    }

}
