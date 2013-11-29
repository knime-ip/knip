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

import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.type.numeric.RealType;

/**
 * A Histogram used by the CLAHE algorithm, implements different functionalities needed such as clipping or calculating
 * a new value using a cumulative distributed function.
 *
 * @author Daniel Seebacher
 *
 * @param <T>
 */
public class ClaheHistogram<T extends RealType<T>> {

    private Histogram1d<T> histogram;

    private double max;

    private double min;

    private Real1dBinMapper<T> mapper;

    private long[] clippedHistogram;

    /**
     * Constructor
     *
     * @param bins the number of bins used by this histogram
     * @param type type of the pixels of the histogram
     */
    public ClaheHistogram(final int bins, final T type) {
        this.mapper = new Real1dBinMapper<T>(type.getMinValue(), type.getMaxValue(), bins, false);
        this.histogram = new Histogram1d<T>(mapper);
        this.min = type.getMinValue();
        this.max = type.getMaxValue();

    }

    /**
     * Adds a point to this histogram
     *
     * @param value pixel value of a point
     */
    public void add(final T value) {
        histogram.increment(value);
    }

    /**
     * Clips this histogram
     *
     * @param slope the desired slope for clipping
     */
    public void clip(final double slope) {
        final int limit = (int)(slope * (histogram.totalCount() / histogram.getBinCount()) + 0.5f);
        clippedHistogram = histogram.toLongArray();
        final int numbins = clippedHistogram.length;

        int clippedEntries = 0;
        int clippedEntriesBefore;
        do {
            clippedEntriesBefore = clippedEntries;
            clippedEntries = 0;
            for (int i = 0; i < numbins; ++i) {
                final long d = clippedHistogram[i] - limit;
                if (d > 0) {
                    clippedEntries += d;
                    clippedHistogram[i] = limit;
                }
            }

            final int d = clippedEntries / numbins;
            final int m = clippedEntries % numbins;
            for (int i = 0; i < numbins; ++i) {
                clippedHistogram[i] += d;
            }

            if (m != 0) {
                final int s = numbins / m;
                for (int i = 0; i < numbins; i += s) {
                    ++clippedHistogram[i];
                }
            }
        } while (clippedEntries != clippedEntriesBefore);
    }

    /**
     * Build the cumulative distribution function to calculate the new value.
     *
     * @param oldValue the old value at a position in the input image.
     * @return the newValue which gets written in the ouput image.
     */
    public double buildCDF(final double oldValue) {
        int hMin = clippedHistogram.length - 1;
        int normalizedOldValue = (int)((oldValue - min) / (max - min) * clippedHistogram.length);
        for (int i = 0; i < hMin; ++i) {
            if (clippedHistogram[i] != 0) {
                hMin = i;
            }
        }

        long cdf = 0;
        for (int i = hMin; i < normalizedOldValue; ++i) {
            cdf += clippedHistogram[i];
        }

        long cdfMax = cdf;
        for (int i = normalizedOldValue; i < clippedHistogram.length; ++i) {
            cdfMax += clippedHistogram[i];
        }

        final long cdfMin = clippedHistogram[hMin];

        return Math.max(min, Math
                .min(max, (((double)cdf - (double)cdfMin) / ((double)cdfMax - (double)cdfMin) * (max - min)) + min));
    }
}
