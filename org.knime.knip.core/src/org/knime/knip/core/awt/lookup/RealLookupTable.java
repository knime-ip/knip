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
package org.knime.knip.core.awt.lookup;

import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.ui.imgviewer.panels.transfunc.TransferFunction;
import org.knime.knip.core.ui.imgviewer.panels.transfunc.TransferFunctionBundle;
import org.knime.knip.core.ui.imgviewer.panels.transfunc.TransferFunctionColor;

/**
 * A lookup table to convert any realvalues to ARGB values.
 * 
 * 
 * @param <T> the Type this table should work on
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author muethingc
 */
public class RealLookupTable<T extends RealType<T>> implements LookupTable<T, ARGBType> {

    private class Alpha implements TransferFunction {

        @Override
        public double getValueAt(final double pos) {
            return 1.0;
        }

        @Override
        public TransferFunction copy() {
            return new Alpha();
        }

        @Override
        public void zoom(final double lower, final double upper) {
            // just ignore
        }
    }

    // The default amount of bins in the lookup table
    private final static int ENTRIES = 255;

    private final ARGBType[] m_values;

    private final double m_minValue;

    private final double m_scale;

    /**
     * Create a new instance, using the default value of entries in the lookup table.<br>
     * 
     * @param min the minimum value of this table
     * @param max the largest value for this table
     * @param bundle the transfer function bundle used for creating the table
     */
    public RealLookupTable(final double min, final double max, final TransferFunctionBundle bundle) {
        this(min, max, ENTRIES, bundle);
    }

    /**
     * Set up a new lookup table.<br>
     * 
     * @param min the minimum value of this table
     * @param max the largest value for this table
     * @param entries the number of entries the lookup table should have
     * @param bundle the transfer function bundle used for creating the table
     */
    public RealLookupTable(final double min, final double max, final int entries, final TransferFunctionBundle bundle) {
        m_minValue = min;
        m_scale = (entries - 1) / (max - m_minValue);
        m_values = tableFromBundle(bundle, new ARGBType[entries]);
    }

    private ARGBType[] tableFromBundle(final TransferFunctionBundle bundle, final ARGBType[] table) {
        assert (bundle != null);

        switch (bundle.getType()) {
            case GREY:
                return tableFromGreyBundle(bundle, table);
            case GREYA:
                return tableFromGreyABundle(bundle, table);
            case RGB:
                return tableFromRGBBundle(bundle, table);
            case RGBA:
                return tableFromRGBABundle(bundle, table);

            default:
                throw new IllegalArgumentException("Not yet implemented for " + bundle.getType());
        }
    }

    private ARGBType[] tableFromGreyBundle(final TransferFunctionBundle bundle, final ARGBType[] table) {
        assert ((bundle != null) && (bundle.getType() == TransferFunctionBundle.Type.GREY));

        final TransferFunction grey = bundle.get(TransferFunctionColor.GREY);

        return fillTable(table, new Alpha(), grey, grey, grey);
    }

    private ARGBType[] tableFromGreyABundle(final TransferFunctionBundle bundle, final ARGBType[] table) {
        assert ((bundle != null) && (bundle.getType() == TransferFunctionBundle.Type.GREYA));

        final TransferFunction alpha = bundle.get(TransferFunctionColor.ALPHA);
        final TransferFunction grey = bundle.get(TransferFunctionColor.GREY);

        return fillTable(table, alpha, grey, grey, grey);
    }

    private ARGBType[] tableFromRGBBundle(final TransferFunctionBundle bundle, final ARGBType[] table) {
        assert ((bundle != null) && (bundle.getType() == TransferFunctionBundle.Type.RGB));

        final TransferFunction red = bundle.get(TransferFunctionColor.RED);
        final TransferFunction green = bundle.get(TransferFunctionColor.GREEN);
        final TransferFunction blue = bundle.get(TransferFunctionColor.BLUE);

        return fillTable(table, new Alpha(), red, green, blue);
    }

    private ARGBType[] tableFromRGBABundle(final TransferFunctionBundle bundle, final ARGBType[] table) {
        assert ((bundle != null) && (bundle.getType() == TransferFunctionBundle.Type.RGBA));

        final TransferFunction alpha = bundle.get(TransferFunctionColor.ALPHA);
        final TransferFunction red = bundle.get(TransferFunctionColor.RED);
        final TransferFunction green = bundle.get(TransferFunctionColor.GREEN);
        final TransferFunction blue = bundle.get(TransferFunctionColor.BLUE);

        return fillTable(table, alpha, red, green, blue);
    }

    private ARGBType[] fillTable(final ARGBType[] table, final TransferFunction alpha, final TransferFunction red,
                                 final TransferFunction green, final TransferFunction blue) {
        assert ((table != null) && (table.length > 1));
        assert (alpha != null);
        assert (red != null);
        assert (green != null);
        assert (blue != null);

        final double step = 1.0 / (table.length - 1);
        double pos = 0.0;

        for (int i = 0; i < table.length; i++) {
            final int a = ((int)(alpha.getValueAt(pos) * 255.0)) << 24;
            final int r = ((int)(red.getValueAt(pos) * 255.0)) << 16;
            final int g = ((int)(green.getValueAt(pos) * 255.0)) << 8;
            final int b = ((int)(blue.getValueAt(pos) * 255.0));

            table[i] = new ARGBType((a | r | g | b));
            pos += step;
        }

        return table;
    }

    /**
     * Lookup the value of a pixel.<br>
     * 
     * @param pixel the value to lookup
     * @return the lookup value
     */
    public ARGBType lookup(final double pixel) {
        final int index = (int)((pixel - m_minValue) * m_scale);
        return m_values[index];
    }

    @Override
    public ARGBType lookup(final T value) {
        return lookup(value.getRealDouble());
    }
}
