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
 * Created on 07.11.2013 by Daniel
 */
package org.knime.knip.base.nodes.proc.clahe;

/**
 * A Histogram used by the CLAHE algorithm, implements different functionalities needed such as clipping or
 * calculating a new value using a cumulative distributed function.
 * @author Daniel Seebacher
 */
public class ClaheHistogram {

    private int m_valueNumber;

    private int[] m_values;

    private int m_valueCount;

    /**
     * @param bins the number of bins used by this histogram
     */
    public ClaheHistogram(final int bins) {
        this.m_valueNumber = bins;
        this.m_values = new int[bins + 1];
        this.m_valueCount = 0;
    }

    /**
     * Adds a point to this histogram
     * @param pixel the value of a point
     */
    public void add(final float pixel) {
        int value = Math.round((pixel / 255.0f) * m_valueNumber);
        ++m_values[value];
        ++m_valueCount;

    }

    /**
     * Clips this histogram
     * @param slope the desired slope for clipping
     */
    public void clip(final float slope) {
        int limit = (int)(slope * (m_valueCount / m_valueNumber));
        int[] clippedHistogram = new int[m_values.length];
        System.arraycopy(m_values, 0, clippedHistogram, 0, m_values.length);
        int clippedEntries = 0;
        int clippedEntriesBefore;
        do {
            clippedEntriesBefore = clippedEntries;
            clippedEntries = 0;
            for (int i = 0; i <= m_valueNumber; ++i) {
                final int d = clippedHistogram[i] - limit;
                if (d > 0) {
                    clippedEntries += d;
                    clippedHistogram[i] = limit;
                }
            }

            final int d = clippedEntries / (m_valueNumber + 1);
            final int m = clippedEntries % (m_valueNumber + 1);
            for (int i = 0; i <= m_valueNumber; ++i) {
                clippedHistogram[i] += d;
            }

            if (m != 0) {
                final int s = m_valueNumber / m;
                for (int i = 0; i <= m_valueNumber; i += s) {
                    ++clippedHistogram[i];
                }
            }
        } while (clippedEntries != clippedEntriesBefore);

        m_values = clippedHistogram;
    }

    /**
     * Build the cumulative distribution function to calculate the new value.
     *
     * @param oldValue the old value at a position in the input image.
     * @return the newValue which gets written in the ouput image.
     */
    public float buildCDF(final float oldValue) {
        int hMin = m_valueNumber;
        for (int i = 0; i < hMin; ++i) {
            if (m_values[i] != 0) {
                hMin = i;
            }
        }

        int cdf = 0;
        for (int i = hMin; i <= oldValue; ++i) {
            cdf += m_values[i];
        }

        int cdfMax = cdf;
        for (int i = (int)(oldValue + 1); i <= m_valueNumber; ++i) {
            cdfMax += m_values[i];
        }

        final int cdfMin = m_values[hMin];

        return Math.round((cdf - cdfMin) / (float)(cdfMax - cdfMin) * 255.0f);
    }
}
