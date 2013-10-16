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
 * Created on Oct 6, 2013 by Daniel
 */
package org.knime.knip.base.nodes.proc;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * Modified version of the Fiji CLAHE Plugin to work with ImgLib.
 * For further information see <a href="http://fiji.sc/wiki/index.php/Enhance_Local_Contrast_(CLAHE)"> Fiji Wiki <a>
 *
 * @author Daniel Seebacher, University of Konstanz
 */
public class Clahe_new<T extends RealType<T>> implements
        UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> {

    private final int blockRadius;

    private final int nrBins;

    private final float slope;

    /**
     * @param blockSize
     * @param bins
     * @param slope
     */
    public Clahe_new(final int blockSize, final int bins, final float slope) {
        //blockRadius is half of the blockSize
        this.blockRadius = (blockSize - 1) / 2;
        this.nrBins = bins;
        this.slope = slope;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RandomAccessibleInterval<T> compute(final RandomAccessibleInterval<T> input,
                                               final RandomAccessibleInterval<T> output) {

        RandomAccess<T> inputAccess = input.randomAccess();
        Cursor<T> outputCursor = Views.flatIterable(output).cursor();

        final int width = (int)input.dimension(0);
        final int height = (int)input.dimension(1);

        for (int y = 0; y < height; ++y) {
            final int yMin = Math.max(0, y - blockRadius);
            final int yMax = Math.min(height, y + blockRadius + 1);
            final int h = yMax - yMin;

            final int xMin0 = Math.max(0, -blockRadius);
            final int xMax0 = Math.min(width - 1, blockRadius);

            // initially fill histogram
            final int[] hist = new int[nrBins + 1];
            for (int yi = yMin; yi < yMax; ++yi) {
                for (int xi = xMin0; xi < xMax0; ++xi) {
                    inputAccess.setPosition(new int[]{xi, yi});
                    ++hist[roundPositive(inputAccess.get().getRealFloat() / 255.0f * nrBins)];
                }
            }

            for (int x = 0; x < width; ++x) {
                inputAccess.setPosition(new int[]{x, y});
                final int oldValue = roundPositive(inputAccess.get().getRealFloat() / 255.0f * nrBins);

                final int xMin = Math.max(0, x - blockRadius);
                final int xMax = x + blockRadius + 1;
                final int w = Math.min(width, xMax) - xMin;
                final int n = h * w;

                final int limit = (int)(slope * n / nrBins + 0.5f);

                // remove left behind values from histogram
                if (xMin > 0) {
                    final int xMin1 = xMin - 1;
                    inputAccess.setPosition(new int[]{xMin1, yMin - 1});
                    for (int yi = yMin; yi < yMax; ++yi) {
                        inputAccess.move(1, 1);
                        --hist[roundPositive(inputAccess.get().getRealFloat() / 255.0f * nrBins)];
                    }
                }

                // add newly included values to histogram
                if (xMax <= width) {
                    final int xMax1 = xMax - 1;
                    inputAccess.setPosition(new int[]{xMax1, yMin - 1});
                    for (int yi = yMin; yi < yMax; ++yi) {
                        inputAccess.move(1, 1);
                        ++hist[roundPositive(inputAccess.get().getRealFloat() / 255.0f * nrBins)];
                    }
                }

                // clip histogram and redistribute clipped entries
                int[] clippedHistogram = clipHistogram(hist, limit);

                // build cdf of clipped histogram
                int newValue = cdf(clippedHistogram, oldValue);

                outputCursor.next();
                outputCursor.get().setReal(newValue);
            }
        }

        return output;
    }

    /**
     * Clip the histogram to avoid over amplificiation when building the cumulative distribution function
     *
     * @param histogram with the number of occurences of grey values.
     * @param limit the limit
     * @return the new clipped histogram
     */
    private int[] clipHistogram(final int[] histogram, final int limit) {
        int[] clippedHistogram = new int[histogram.length];
        System.arraycopy(histogram, 0, clippedHistogram, 0, histogram.length);
        int clippedEntries = 0;
        int clippedEntriesBefore;
        do {
            clippedEntriesBefore = clippedEntries;
            clippedEntries = 0;
            for (int i = 0; i <= nrBins; ++i) {
                final int d = clippedHistogram[i] - limit;
                if (d > 0) {
                    clippedEntries += d;
                    clippedHistogram[i] = limit;
                }
            }

            final int d = clippedEntries / (nrBins + 1);
            final int m = clippedEntries % (nrBins + 1);
            for (int i = 0; i <= nrBins; ++i) {
                clippedHistogram[i] += d;
            }

            if (m != 0) {
                final int s = nrBins / m;
                for (int i = 0; i <= nrBins; i += s) {
                    ++clippedHistogram[i];
                }
            }
        } while (clippedEntries != clippedEntriesBefore);

        return clippedHistogram;
    }

    /**
     * Build the cumulative distribution function to calculate the new value.
     *
     * @param clippedHistogram clipped histogram.
     * @param oldValue the old value at a position in the input image.
     * @return the newValue which gets written in the ouput image.
     */
    private int cdf(final int[] clippedHistogram, final int oldValue) {
        int hMin = nrBins;
        for (int i = 0; i < hMin; ++i) {
            if (clippedHistogram[i] != 0) {
                hMin = i;
            }
        }

        int cdf = 0;
        for (int i = hMin; i <= oldValue; ++i) {
            cdf += clippedHistogram[i];
        }

        int cdfMax = cdf;
        for (int i = oldValue + 1; i <= nrBins; ++i) {
            cdfMax += clippedHistogram[i];
        }

        final int cdfMin = clippedHistogram[hMin];

        return roundPositive((cdf - cdfMin) / (float)(cdfMax - cdfMin) * 255.0f);
    }

    /**
     * @param f
     * @return
     */
    private int roundPositive(final float f) {
        return (int)(f + 0.5f);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> copy() {
        return new Clahe_new(blockRadius, nrBins, slope);
    }
}
