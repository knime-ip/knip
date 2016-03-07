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
package org.knime.knip.core.awt.converter;

import net.imglib2.converter.Converter;
import net.imglib2.display.projector.sampler.ProjectedSampler;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

/**
 * TODO Auto-generated
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class RealColorARGBConverter<R extends RealType<R>> implements Converter<ProjectedSampler<R>, ARGBType> {

    private final double m_localMin;

    /*if -1, no normalization is applied*/
    private final double m_normalizationFactor;

    /**
     * Color converter without any normalization
     */
    public RealColorARGBConverter() {
        m_localMin = 0;
        m_normalizationFactor = -1;
    }

    /**
     * Color converter where normalization is applied too.
     *
     * @param normalizationFactor
     * @param localMin
     */
    public RealColorARGBConverter(final double normalizationFactor, final double localMin) {

        m_localMin = localMin;
        m_normalizationFactor = normalizationFactor;

    }

    @Override
    public void convert(final ProjectedSampler<R> input, final ARGBType output) {

        int i = 0;
        final int[] rgb = new int[3];
        double val;

        while (input.hasNext() && (i < 3)) {

            val = input.get().getRealDouble();

            if (m_normalizationFactor == -1) {
                //no normalization, but make it unsigned
                val = val - input.get().getMinValue();
            } else {
                val = ((val - m_localMin) * m_normalizationFactor);
            }

            // normalize to be between 0 and 1
            val = val / (input.get().getMaxValue() - input.get().getMinValue());

            if (val < 0) {
                val = 0;
            } else if (val > 1) {
                val = 1;
            }

            rgb[i] = (int)Math.round(val * 255);
            i++;
            input.fwd();
        }

        final int argb = 0xff000000 | ((rgb[0] << 16) | (rgb[1] << 8) | rgb[2]);
        output.set(argb);
    }

}
