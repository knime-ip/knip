/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 *
 * History
 *   11.05.2006 (gabriel): created
 */
package org.knime.knip.base.nodes.proc.binner;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Helper class the represents pixel intensity bins, stores them, restores them etc.
 *
 */
class IntensityBins {
    /** Key for left value interval. */
    private static final String LEFT_VALUE = "left_value";

    /** Key for right value interval. */
    private static final String RIGHT_VALUE = "right_value";

    /** Key for left open interval. */
    private static final String LEFT_OPEN = "left_open";

    /** Key for right open interval. */
    private static final String RIGHT_OPEN = "right_open";

    /** Key for the bin's name. */
    private static final String BIN_VALUE = "bin_value";

    private final double[] m_binValues;

    private final boolean[] m_leftOpen;

    private final double[] m_leftValues;

    private final boolean[] m_rightOpen;

    private final double[] m_rightValues;

    private final int m_numBins;

    /**
     * @param numBins number of bins
     */
    public IntensityBins(final int numBins) {
        m_numBins = numBins;
        m_binValues = new double[numBins];
        m_leftOpen = new boolean[numBins];
        m_leftValues = new double[numBins];
        m_rightOpen = new boolean[numBins];
        m_rightValues = new double[numBins];
    }

    /**
     *
     * @return the pixel type
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends RealType<T>> T getPixelType() {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        boolean floatingPoint = false;
        for (int i = 0; i < m_numBins; i++) {
            min = Math.min(m_binValues[i], min);
            max = Math.max(m_binValues[i], max);
            if (m_binValues[i] != Math.round(m_binValues[i])) {
                floatingPoint = true;
            }
        }
        if (floatingPoint) {
            return (T)new FloatType();
        } else {
            RealType[] types =
                    new RealType[]{new BitType(), new UnsignedByteType(), new ByteType(), new UnsignedShortType(),
                            new ShortType(), new UnsignedIntType(), new IntType(), new LongType(), new FloatType()};
            for (RealType t : types) {
                if (min >= t.getMinValue() && max <= t.getMaxValue()) {
                    return (T)t;
                }
            }
        }
        //should actually never happen
        throw new IllegalStateException(
                "No pixel type can be found that is able to represent one of the given bin values.");
    }

    /**
     *
     * @param index the index of the bin, must be smaller than the given number of bins in the constructor ({@link #IntensityBins(int)}!
     * @param binValue
     * @param leftOpen
     * @param leftValue
     * @param rightOpen
     * @param rightValue
     */
    public void setBinAtIndex(final int index, final double binValue, final boolean leftOpen,
                       final double leftValue, final boolean rightOpen,
                       final double rightValue) {
        m_binValues[index] = binValue;
        m_leftOpen[index] = leftOpen;
        m_leftValues[index] = leftValue;
        m_rightOpen[index] = rightOpen;
        m_rightValues[index] = rightValue;
    }


    /**
     * @return the number of bins
     */
    public int getNumBins() {
        return m_numBins;
    }

    public double getBinValue(final int index) {
        return m_binValues[index];
    }

    public boolean isLeftOpen(final int index) {
        return m_leftOpen[index];
    }

    public double getLeftValue(final int index) {
        return m_leftValues[index];
    }

    public boolean isRightOpen(final int index) {
        return m_rightOpen[index];
    }

    public double getRightValue(final int index) {
        return m_rightValues[index];
    }

    /**
     * @param source the pixel to check
     * @param target the pixel to be set in case the source is covered (should be of the type as given by
     *            {@link #getPixelType()}
     * @return <code>true</code>, if interval covers the given value
     */
    public <T1 extends RealType<T1>, T2 extends RealType<T2>> boolean covers(final T1 source, final T2 target) {
        double value = source.getRealDouble();
        for (int i = 0; i < m_numBins; i++) {
            double l = m_leftValues[i];
            double r = m_rightValues[i];
            assert (l <= r);
            if (l < value && value < r) {
                target.setReal(m_binValues[i]);
                return true;
            }
            if (l == value && !m_leftOpen[i]) {
                target.setReal(m_binValues[i]);
                return true;
            }
            if (r == value && !m_rightOpen[i]) {
                target.setReal(m_binValues[i]);
                return true;
            }
        }
        return false;

    }

    public void saveToSettings(final NodeSettingsWO bins) {
        bins.addInt("num_bins", m_numBins);
        for (int i = 0; i < m_numBins; i++) {
            bins.addDouble(BIN_VALUE + "_" + i, m_binValues[i]);
            bins.addBoolean(LEFT_OPEN + "_" + i, m_leftOpen[i]);
            bins.addDouble(LEFT_VALUE + "_" + i, m_leftValues[i]);
            bins.addBoolean(RIGHT_OPEN + "_" + i, m_rightOpen[i]);
            bins.addDouble(RIGHT_VALUE + "_" + i, m_rightValues[i]);
        }

    }

    /**
     * Create pixel bins from NodeSettings.
     *
     * @param bin read settings from
     * @throws InvalidSettingsException if bins could not be read
     */
    public IntensityBins(final NodeSettingsRO bins) throws InvalidSettingsException {
        this(bins.getInt("num_bins"));
        for (int i = 0; i < m_numBins; i++) {
            m_binValues[i] = bins.getDouble(BIN_VALUE + "_" + i);
            m_leftOpen[i] = bins.getBoolean(LEFT_OPEN + "_" + i);
            m_leftValues[i] = bins.getDouble(LEFT_VALUE + "_" + i);
            m_rightOpen[i] = bins.getBoolean(RIGHT_OPEN + "_" + i);
            m_rightValues[i] = bins.getDouble(RIGHT_VALUE + "_" + i);
        }
    }
}
