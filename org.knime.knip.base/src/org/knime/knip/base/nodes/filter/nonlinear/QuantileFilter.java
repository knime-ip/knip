/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */
package org.knime.knip.base.nodes.filter.nonlinear;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.numeric.RealType;

/**
 * @author dietzc, hornm, zinsmaierm, seebacherd, riesst (University of Konstanz)
 * @param <T> extends RealType<T>
 */
public class QuantileFilter<T extends RealType<T>> implements
        UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> {

    private int m_radius = 3;

    private int m_percentile = 50;

    /**
     * @param radius the radius of the kernel used for the quantile filtering
     * @param percentile the desired percentile used for the filtering (50 is equal to the median)
     */
    public QuantileFilter(final int radius, final int percentile) {
        m_radius = radius;
        m_percentile = percentile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RandomAccessibleInterval<T> compute(final RandomAccessibleInterval<T> src,
                                               final RandomAccessibleInterval<T> res) {

        if (src.numDimensions() != 2) {
            throw new IllegalArgumentException("Quantile Filter only works on 2-dimensional images");
        }

        if (src.dimension(0) <= 1 || src.dimension(1) <= 1) {
            throw new IllegalArgumentException("Dimensions of size 1 are not supported by  " + "Quantile Filter");
        }

        RandomAccess<T> srcAccess = src.randomAccess();
        RandomAccess<T> resAccess = res.randomAccess();

        int xrange = (int)src.dimension(0);
        int yrange = (int)src.dimension(1);

        double minValue = srcAccess.get().getMinValue();
        double maxValue = srcAccess.get().getMaxValue();

        // prepare column histograms
        ColumnHistogram[] columnhistograms = new ColumnHistogram[xrange];
        for (int x = 0; x < xrange; x++) {
            columnhistograms[x] = new ColumnHistogram(minValue, maxValue);

            srcAccess.setPosition(x, 0);

            // fill them with m_radius - 1 rows, makes it easier in the loop later
            for (int y = 0; y < m_radius && y < yrange; y++) {
                srcAccess.setPosition(y, 1);
                columnhistograms[x].addPixel(srcAccess.get().getRealDouble());
            }
        }

        // iterate through all rows
        for (int y = 0; y < yrange; y++) {

            // new block histogram for each row
            BlockHistogram blockHistogram = new BlockHistogram(minValue, maxValue);

            // calculate the y positions of the pixels which will be removed/added this round
            int pos_y_span = y + m_radius;

            // the extra -1 because we're in row n and want to remove the last value of which was used in row n - 1
            int neg_y_span = y - m_radius - 1;

            // initialize block histogram
            for (int x = 0; x <= m_radius && x < xrange; x++) {
                srcAccess.setPosition(x, 0);

                // check if old y value can be removed
                if (neg_y_span >= 0) {
                    srcAccess.setPosition(neg_y_span, 1);
                    columnhistograms[x].subPixel(srcAccess.get().getRealDouble());
                }

                // check if new y value can be added
                if (pos_y_span < yrange) {
                    srcAccess.setPosition(pos_y_span, 1);
                    columnhistograms[x].addPixel(srcAccess.get().getRealDouble());
                }

                // add column histogram to block histogram
                blockHistogram.add(columnhistograms[x]);
            }

            // set first value
            resAccess.setPosition(y, 1);
            resAccess.setPosition(0, 0);
            resAccess.get().setReal(blockHistogram.getQuantile(m_percentile));

            // run block through row
            for (int x = 1; x < xrange; x++) {
                int pos_x_span = x + m_radius;
                int neg_x_span = x - m_radius - 1;

                // check if old column histogram can be deleted
                if (neg_x_span >= 0) {
                    blockHistogram.sub(columnhistograms[neg_x_span]);
                }

                // check if new column histogram can be added
                if (pos_x_span < xrange) {
                    srcAccess.setPosition(pos_x_span, 0);

                    // check if old y value can be deleted
                    if (neg_y_span >= 0) {
                        srcAccess.setPosition(neg_y_span, 1);
                        columnhistograms[pos_x_span].subPixel(srcAccess.get().getRealDouble());
                    }

                    // check if new y value can be added
                    if (pos_y_span < yrange) {
                        srcAccess.setPosition(pos_y_span, 1);
                        columnhistograms[pos_x_span].addPixel(srcAccess.get().getRealDouble());
                    }

                    blockHistogram.add(columnhistograms[pos_x_span]);
                }

                resAccess.setPosition(x, 0);
                resAccess.get().setReal(blockHistogram.getQuantile(m_percentile));
            }
        }

        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> copy() {
        return new QuantileFilter<T>(m_radius, m_percentile);
    }

    /**
     * @author Daniel Seebacher, University of Konstanz
     */
    private static class ColumnHistogram {

        private final int m_pixelRange;

        private int[] m_histogram;

        private int m_count;

        private final double m_minValue;

        /**
         * Constructor.
         *
         * @param minValue The minimum pixel value
         * @param maxValue The maximum pixel value
         */
        public ColumnHistogram(final double minValue, final double maxValue) {
            m_minValue = minValue;
            m_pixelRange = (int)(maxValue - minValue);
            m_histogram = new int[m_pixelRange];
            m_count = 0;
        }

        /**
         * Add a pixel value.
         *
         * @param pixel Pixel value to add
         */
        public void addPixel(final double pixel) {
            int p = (int) (pixel - m_minValue);
            if (p < m_pixelRange) {
                ++m_histogram[p];
                ++m_count;
            }
        }

        /**
         * Substract a pixel value.
         *
         * @param pixel Pixel value to subtract
         */
        public void subPixel(final double pixel) {
            int p = (int) (pixel - m_minValue);
            if (p < m_pixelRange) {
                --m_histogram[p];
                --m_count;
            }
        }
    }

    /**
     * The Kernel used for the Percentile Filtering
     *
     * @author Daniel Seebacher, University of Konstanz
     */
    private static final class BlockHistogram extends ColumnHistogram {

        /**
         * Default constructor.
         *
         * @param minValue minValue of the input image
         * @param maxValue maxValue of the input image
         */
        public BlockHistogram(final double minValue, final double maxValue) {
            super(minValue, maxValue);
        }

        /**
         * Add a histogram.
         *
         * @param ch The histogram to add
         */
        public void add(final ColumnHistogram ch) {
            for (int i = 0; i < ch.m_histogram.length; i++) {
                super.m_histogram[i] += ch.m_histogram[i];
                super.m_count += ch.m_histogram[i];
            }
            return;
        }

        /**
         * Subtract a histogram.
         *
         * @param ch The histogram to subtract
         */
        public void sub(final ColumnHistogram ch) {
            for (int i = 0; i < ch.m_histogram.length; i++) {
                super.m_histogram[i] -= ch.m_histogram[i];
                super.m_count -= ch.m_histogram[i];
            }
            return;
        }

        /**
         * Returns the given percentile value.
         *
         * @param percentile The percentile that should be returned (in %, min. 1 - max. 99)
         * @return The percentile value
         */
        public double getQuantile(final int percentile) {
            int actcount = 0;
            int i;
            int stop = Math.max((int)((double)super.m_count * percentile / 100.0), 1);
            for (i = 0; i < super.m_histogram.length && actcount < stop; i++) {
                actcount += super.m_histogram[i];
            }
            if (i > 0) {
                i--;
            }

            return i + super.m_minValue;
        }
    }
}