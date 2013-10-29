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
 * Created on Oct 29, 2013 by Daniel
 */
package org.knime.knip.base.nodes.proc;

import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 *
 * @author Daniel
 */
public class FastCLAHE<T extends RealType<T>> implements
        UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> {

    private final int m_ctxNumberX;

    private final int m_ctxNumberY;

    private final int m_bins;

    private final float m_slope;

    /**
     * @param ctxNumberX
     * @param ctxNumberY
     * @param bins
     * @param slope
     */
    public FastCLAHE(final int ctxNumberX, final int ctxNumberY, final int bins, final float slope) {
        this.m_ctxNumberX = ctxNumberX;
        this.m_ctxNumberY = ctxNumberY;
        this.m_bins = bins;
        this.m_slope = slope;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> copy() {
        return new FastCLAHE<T>(m_ctxNumberX, m_ctxNumberY, m_bins, m_slope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RandomAccessibleInterval<T> compute(final RandomAccessibleInterval<T> input,
                                               final RandomAccessibleInterval<T> output) {

        Cursor<T> inputCursor = Views.iterable(input).localizingCursor();
        Cursor<T> outputCursor = Views.iterable(output).cursor();

        int width = (int)input.dimension(0);
        int height = (int)input.dimension(1);

        int xOffset = (int)(input.dimension(0) / m_ctxNumberX);
        int yOffset = (int)(input.dimension(1) / m_ctxNumberY);

        System.out.println(xOffset);
        System.out.println(yOffset);

        //create the histograms for each of the context regions
        Map<Point, int[]> ctxHistograms = new HashMap<Point, int[]>();
        Map<Point, Float> ctxCenterValues = new HashMap<Point, Float>();
        int[] pos = new int[2];

        while (inputCursor.hasNext()) {
            inputCursor.next();
            inputCursor.localize(pos);

            // calculate the center in x direction
            Point center =
                    new Point(getNextContextRegionValue(pos[0], xOffset, width), getNextContextRegionValue(pos[1],
                                                                                                           yOffset,
                                                                                                           height));

            // last step, if we are at a center, save the image value at this position
            if (center.x == pos[0] && center.y == pos[1]) {
                ctxCenterValues.put(center, inputCursor.get().getRealFloat());
            }

            int[] hist = ctxHistograms.get(center);
            if (hist == null) {
                hist = new int[m_bins + 1];
            }

            ++hist[roundPositive(inputCursor.get().getRealFloat() / 255.0f * m_bins)];
            ctxHistograms.put(center, hist);
        }

        // after creation of the histograms, clip them
        int limit = (int)(m_slope * ((yOffset * xOffset) / m_bins));
        for (Point center : ctxHistograms.keySet()) {
            int[] hist = ctxHistograms.get(center);
            int[] clippedHist = clipHistogram(hist, limit);
            ctxHistograms.put(center, clippedHist);
        }

        // last step, apply CLAHE
        inputCursor.reset();
        while (inputCursor.hasNext()) {

            inputCursor.next();
            inputCursor.localize(pos);

            outputCursor.next();

            int oldValue = roundPositive(inputCursor.get().getRealFloat() / 255.0f * m_bins);

            try {
                // first check the four cases where we are in the corners of the image, no interpolation here
                // 1. upper left corner
                if (pos[0] <= xOffset && pos[1] <= yOffset) {
                    Point p = new Point(xOffset, yOffset);
                    float newValue = buildCDF(ctxHistograms.get(p), oldValue);
                    outputCursor.get().setReal(newValue);
                    continue;
                }
                // 2. bottom left corner
                else if (pos[0] <= xOffset && pos[1] >= (input.dimension(1) - yOffset)) {
                    Point p = new Point(xOffset, getNextContextRegionValue(pos[1], yOffset, height));

                    float newValue = buildCDF(ctxHistograms.get(p), oldValue);
                    outputCursor.get().setReal(newValue);
                    continue;
                }
                // 3. top right corner
                else if (pos[0] >= (input.dimension(0) - xOffset) && pos[1] <= yOffset) {
                    Point p = new Point(getNextContextRegionValue(pos[0], xOffset, width), yOffset);

                    float newValue = buildCDF(ctxHistograms.get(p), oldValue);
                    outputCursor.get().setReal(newValue);
                    continue;
                }
                // 4. bottom right corner
                else if (pos[0] >= (input.dimension(0) - xOffset) && pos[1] >= (input.dimension(1) - yOffset)) {
                    Point p =
                            new Point(getNextContextRegionValue(pos[0], xOffset, width),
                                    getNextContextRegionValue(pos[1], yOffset, height));

                    float newValue = buildCDF(ctxHistograms.get(p), oldValue);
                    outputCursor.get().setReal(newValue);
                    continue;
                }

                // check if were at the center of a context region
                if (getNextContextRegionValue(pos[0], xOffset, width) == pos[0]
                        && getNextContextRegionValue(pos[1], yOffset, height) == pos[1]) {

                    float newValue = buildCDF(ctxHistograms.get(new Point(pos[0], pos[1])), oldValue);
                    outputCursor.get().setReal(newValue);
                    continue;
                }

                // again 4 special cases, if we're at the edges of the image
                // 1. left edge
                if (pos[0] <= xOffset) {
                    // get the x coordinate and the two y coordinates
                    int ctxX = xOffset;
                    int ctxY1 = pos[1] - pos[1] % yOffset;
                    int ctxY2 = ctxY1 + yOffset;

                    // calculate weights for y
                    float weightY1 = ((float)pos[1] - ctxY1) / (ctxY2 - ctxY1);
                    float weightY2 = (ctxY2 - (float)pos[1]) / (ctxY2 - ctxY1);

                    Point center1 = new Point(ctxX, ctxY1);
                    Point center2 = new Point(ctxX, ctxY2);

                    float newValue =
                            (buildCDF(ctxHistograms.get(center1), oldValue) * weightY1)
                                    + (buildCDF(ctxHistograms.get(center2), oldValue) * weightY2);

                    outputCursor.get().setReal(newValue);

                    continue;
                }
                // 2. right edge
                else if (pos[0] >= (input.dimension(0) - xOffset)) {
                    // get the x coordinate and the two y coordinates
                    int ctxX = getNextContextRegionValue(pos[0], xOffset, width);
                    int ctxY1 = pos[1] - pos[1] % yOffset;
                    int ctxY2 = ctxY1 + yOffset;

                    // calculate weights for y
                    float weightY1 = ((float)pos[1] - ctxY1) / (ctxY2 - ctxY1);
                    float weightY2 = (ctxY2 - (float)pos[1]) / (ctxY2 - ctxY1);

                    Point center1 = new Point(ctxX, ctxY1);
                    Point center2 = new Point(ctxX, ctxY2);

                    float newValue =
                            (buildCDF(ctxHistograms.get(center1), oldValue) * weightY1)
                                    + (buildCDF(ctxHistograms.get(center2), oldValue) * weightY2);

                    outputCursor.get().setReal(newValue);
                    continue;
                }
                // 3. top edge
                else if (pos[1] < yOffset) {

                    // get the x coordinate and the two y coordinates
                    int ctxY = yOffset;
                    int ctxX1 = pos[0] - pos[0] % xOffset;
                    int ctxX2 = ctxX1 + xOffset;

                    // calculate weights for y
                    float weightX1 = ((float)pos[0] - ctxX1) / (ctxX2 - ctxX1);
                    float weightX2 = (ctxX2 - (float)pos[0]) / (ctxX2 - ctxX1);

                    Point center1 = new Point(ctxX1, ctxY);
                    Point center2 = new Point(ctxX2, ctxY);

                    float newValue =
                            (buildCDF(ctxHistograms.get(center1), oldValue) * weightX1)
                                    + (buildCDF(ctxHistograms.get(center2), oldValue) * weightX2);

                    outputCursor.get().setReal(newValue);

                    continue;
                }
                // 4. bottom edge
                else if (pos[1] >= (input.dimension(1) - yOffset)) {
                    // get the x coordinate and the two y coordinates
                    int ctxY = getNextContextRegionValue(pos[1], yOffset, height);
                    int ctxX1 = pos[0] - pos[0] % xOffset;
                    int ctxX2 = ctxX1 + xOffset;

                    // calculate weights for y
                    float weightX1 = ((float)pos[0] - ctxX1) / (ctxX2 - ctxX1);
                    float weightX2 = (ctxX2 - (float)pos[0]) / (ctxX2 - ctxX1);

                    Point center1 = new Point(ctxX1, ctxY);
                    Point center2 = new Point(ctxX2, ctxY);

                    float newValue =
                            (buildCDF(ctxHistograms.get(center1), oldValue) * weightX1)
                                    + (buildCDF(ctxHistograms.get(center2), oldValue) * weightX2);

                    outputCursor.get().setReal(newValue);
                    continue;
                }

                // otherwise we're somewhere at the center of the image and can make the default interpolation with the 4 context regions
                int ctxX0 = pos[0] - pos[0] % xOffset;
                int ctxX1 = ctxX0 + xOffset;
                int ctxY0 = pos[1] - pos[1] % yOffset;
                int ctxY1 = ctxY0 + yOffset;

                Point center00 = new Point(ctxX0, ctxY0);
                Point center01 = new Point(ctxX0, ctxY1);
                Point center10 = new Point(ctxX1, ctxY0);
                Point center11 = new Point(ctxX1, ctxY1);

                float weightX = ((float)pos[0] - ctxX0) / (ctxX1 - ctxX0);
                float weightY = ((float)pos[1] - ctxY0) / (ctxY1 - ctxY0);

                float newValue =
                        weightY
                                * (weightX * buildCDF(ctxHistograms.get(center00), oldValue) + (1 - weightX)
                                        * buildCDF(ctxHistograms.get(center10), oldValue))
                                + (1 - weightY)
                                * (weightX * buildCDF(ctxHistograms.get(center01), oldValue) + (1 - weightX)
                                        * buildCDF(ctxHistograms.get(center11), oldValue));
                outputCursor.get().setReal(newValue);
            } catch (NullPointerException e) {
                System.err.println(Arrays.toString(pos));
            }

        }

        return output;
    }

    private int getNextContextRegionValue(final int coordinate, final int offset, final int limit) {

        if (coordinate < offset) {
            return offset;
        }

        int times = offset * (coordinate / offset);
        int ctxValue = (coordinate % offset < offset / 2) ? times : times + offset;

        // check that we aren't over the boundary
        if (ctxValue >= limit) {
            ctxValue -= offset;
        }

        return ctxValue;
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
            for (int i = 0; i <= m_bins; ++i) {
                final int d = clippedHistogram[i] - limit;
                if (d > 0) {
                    clippedEntries += d;
                    clippedHistogram[i] = limit;
                }
            }

            final int d = clippedEntries / (m_bins + 1);
            final int m = clippedEntries % (m_bins + 1);
            for (int i = 0; i <= m_bins; ++i) {
                clippedHistogram[i] += d;
            }

            if (m != 0) {
                final int s = m_bins / m;
                for (int i = 0; i <= m_bins; i += s) {
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
    private int buildCDF(final int[] clippedHistogram, final int oldValue) {
        int hMin = m_bins;
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
        for (int i = oldValue + 1; i <= m_bins; ++i) {
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
}
