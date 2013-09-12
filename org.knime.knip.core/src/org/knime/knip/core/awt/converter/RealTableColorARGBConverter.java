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
import net.imglib2.display.AbstractArrayColorTable;
import net.imglib2.display.ColorTable16;
import net.imglib2.display.ColorTable8;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

/**
 * Takes a real and converts it into a pixel of an ARGB image using a color table. If no color table is set using
 * {@link #setColorTable(ColorTable16)} or {@link #setColorTable(ColorTable8)} a linear ramp grey color table is used as
 * default.
 * 
 * 
 * @param <R>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class RealTableColorARGBConverter<R extends RealType<R>> implements Converter<R, ARGBType> {

    private final double m_localMin;

    private final double m_normalizationFactor;

    private AbstractArrayColorTable<?> m_table;

    private int m_rangeFactor;

    public RealTableColorARGBConverter(final double normalizationFactor, final double localMin) {

        m_localMin = localMin;
        m_normalizationFactor = normalizationFactor;

        m_table = new ColorTable8();
        m_rangeFactor = 255;
    }

    public void setColorTable(final ColorTable8 table) {
        m_rangeFactor = 255;
        m_table = table;
    }

    public void setColorTable(final ColorTable16 table) {
        m_rangeFactor = 65535;
        m_table = table;
    }

    @Override
    public void convert(final R input, final ARGBType output) {

        int intVal;
        double val;

        if (m_normalizationFactor == 1) {
            val = ((input.getRealDouble() - input.getMinValue()) / (input.getMaxValue() - input.getMinValue()));

        } else {
            val =
                    (((input.getRealDouble() - m_localMin) / (input.getMaxValue() - input.getMinValue())) * m_normalizationFactor);

        }

        intVal = (int)Math.round(val * m_rangeFactor);

        if (intVal < 0) {
            intVal = 0;
        } else if (intVal > m_rangeFactor) {
            intVal = m_rangeFactor;
        }

        output.set(m_table.argb(intVal));
    }
}
