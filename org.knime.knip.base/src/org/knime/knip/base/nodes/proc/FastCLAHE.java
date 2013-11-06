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
 * @param <T> extends RealType<T>
 */
public class FastCLAHE<T extends RealType<T>> implements
        UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> {

    private final int m_ctxNumberX;

    private final int m_ctxNumberY;

    private final int m_bins;

    private final float m_slope;

    private long[] m_ctxNumberDims;

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


        long[] offsets = new long[input.numDimensions()];
        for(int i = 0; i < input.numDimensions(); i++){
            offsets[i] = input.dimension(1) / m_ctxNumberDims[i];
        }


        Cursor<T> inputCursor = Views.flatIterable(input).localizingCursor();
        Cursor<T> outputCursor = Views.flatIterable(output).cursor();

        int xOffset = (int)(input.dimension(0) / m_ctxNumberX);
        int yOffset = (int)(input.dimension(1) / m_ctxNumberY);

        //create the histograms for each of the context regions
        Map<Point, Histogram> ctxHistograms = new HashMap<Point, Histogram>();
        int[] pos = new int[2];

        while (inputCursor.hasNext()) {
            inputCursor.next();
            inputCursor.localize(pos);

            // calculate the center in x direction
            Point center = getNearestCenter(pos[0], xOffset, pos[1], yOffset);

            Histogram hist = ctxHistograms.get(center);
            if (hist == null) {
                hist = new Histogram(m_bins);
                ctxHistograms.put(center, hist);
            }

            ctxHistograms.get(center).add(inputCursor.get().getRealFloat());
        }

        // after creation of the histograms, clip them
        for (Point center : ctxHistograms.keySet()) {
            ctxHistograms.get(center).clip(m_slope);
        }

        // last step, apply CLAHE
        inputCursor.reset();
        while (inputCursor.hasNext()) {

            inputCursor.next();
            inputCursor.localize(pos);

            outputCursor.next();

            int oldValue = Math.round(inputCursor.get().getRealFloat() / 255f * m_bins);

            if ((pos[0] <= xOffset && (pos[1] <= xOffset || pos[1] > input.dimension(1) - yOffset))
                    || (pos[0] >= (input.dimension(0) - xOffset) && (pos[1] <= xOffset || pos[1] > input.dimension(1)
                            - yOffset))) {
                Point p = getNearestCenter(pos[0], xOffset, pos[1], yOffset);
                outputCursor.get().setReal(ctxHistograms.get(p).buildCDF(oldValue));
            } else if (pos[0] < xOffset / 2 || pos[0] >= (input.dimension(0) - xOffset / 2 - 1)) {
                // get the x coordinate and the two y coordinates
                Point nextCenter = getNearestCenter(pos[0], xOffset, pos[1], yOffset);
                int ctxY1;
                int ctxY2;
                if (nextCenter.y < pos[1]) {
                    ctxY1 = nextCenter.y;
                    ctxY2 = ctxY1 + yOffset;
                } else {
                    ctxY2 = nextCenter.y;
                    ctxY1 = ctxY2 - yOffset;
                }

                // calculate weights for y
                float weightY1 = (1 - ((float)pos[1] - ctxY1) / (ctxY2 - ctxY1));
                float weightY2 = (1 - (ctxY2 - (float)pos[1]) / (ctxY2 - ctxY1));

                Point center1 = new Point(nextCenter.x, ctxY1);
                Point center2 = new Point(nextCenter.x, ctxY2);

                float newValue =
                        (ctxHistograms.get(center1).buildCDF(oldValue) * weightY1)
                                + (ctxHistograms.get(center2).buildCDF(oldValue) * weightY2);

                outputCursor.get().setReal(newValue);

            } else if (pos[1] < yOffset / 2 || pos[1] >= (input.dimension(1) - yOffset / 2 - 1)) {

                // get the x coordinate and the two y coordinates
                Point nextCenter = getNearestCenter(pos[0], xOffset, pos[1], yOffset);
                int ctxX1;
                int ctxX2;

                if (nextCenter.x < pos[0]) {
                    ctxX1 = nextCenter.x;
                    ctxX2 = ctxX1 + xOffset;
                } else {
                    ctxX2 = nextCenter.x;
                    ctxX1 = ctxX2 - xOffset;
                }

                // calculate weights for y
                float weightX1 = (1 - ((float)pos[0] - ctxX1) / (ctxX2 - ctxX1));
                float weightX2 = (1 - (ctxX2 - (float)pos[0]) / (ctxX2 - ctxX1));

                Point center1 = new Point(ctxX1, nextCenter.y);
                Point center2 = new Point(ctxX2, nextCenter.y);

                float newValue =
                        ctxHistograms.get(center1).buildCDF(oldValue) * weightX1
                                + ctxHistograms.get(center2).buildCDF(oldValue) * weightX2;

                outputCursor.get().setReal(newValue);
            } else {

                Point currentPosition = new Point(pos[0], pos[1]);
                if (ctxHistograms.containsKey(currentPosition)) {
                    outputCursor.get().setReal(ctxHistograms.get(currentPosition).buildCDF(oldValue));
                } else {

                    // otherwise we're somewhere at the center of the image and can make the default interpolation with the 4 context regions
                    Point nearestCenter = getNearestCenter(currentPosition.x, xOffset, currentPosition.y, yOffset);

                    Point center00;
                    Point center01;
                    Point center10;
                    Point center11;

                    if (nearestCenter.x <= pos[0]) {
                        if (nearestCenter.y <= pos[1]) {
                            center00 = nearestCenter;
                            center01 = new Point(nearestCenter.x, nearestCenter.y + yOffset);
                            center10 = new Point(nearestCenter.x + xOffset, nearestCenter.y);
                            center11 = new Point(nearestCenter.x + xOffset, nearestCenter.y + yOffset);
                        } else {
                            center00 = new Point(nearestCenter.x, nearestCenter.y - yOffset);
                            center01 = nearestCenter;
                            center10 = new Point(nearestCenter.x + xOffset, nearestCenter.y - yOffset);
                            center11 = new Point(nearestCenter.x + xOffset, nearestCenter.y);
                        }
                    } else {
                        if (nearestCenter.y <= pos[1]) {
                            center00 = new Point(nearestCenter.x - xOffset, nearestCenter.y);
                            center01 = new Point(nearestCenter.x - xOffset, nearestCenter.y + yOffset);
                            center10 = nearestCenter;
                            center11 = new Point(nearestCenter.x, nearestCenter.y + yOffset);
                        } else {
                            center00 = new Point(nearestCenter.x - xOffset, nearestCenter.y - yOffset);
                            center01 = new Point(nearestCenter.x - xOffset, nearestCenter.y);
                            center10 = new Point(nearestCenter.x, nearestCenter.y - yOffset);
                            center11 = nearestCenter;
                        }
                    }

                    float weightX = (1 - ((float)pos[0] - center00.x) / (center10.x - center00.x));
                    float weightY = (1 - ((float)pos[1] - center00.y) / (center01.y - center00.y));

                    float newValue =
                            weightY
                                    * (weightX * ctxHistograms.get(center00).buildCDF(oldValue) + (1 - weightX)
                                            * ctxHistograms.get(center10).buildCDF(oldValue))
                                    + (1 - weightY)
                                    * (weightX * ctxHistograms.get(center01).buildCDF(oldValue) + (1 - weightX)
                                            * ctxHistograms.get(center11).buildCDF(oldValue));
                    outputCursor.get().setReal(newValue);
                }

            }

        }

        return output;
    }

    private Point getNearestCenter(final int xCoord, final int xOffset, final int yCoord, final int yOffset) {
        return new Point( (int) getNextContextRegionValue(xCoord, xOffset), (int) getNextContextRegionValue(yCoord, yOffset));
    }

    private ClahePoint getNearestCenter(final long[] coordinates, final long[] offsets){
        long[] newCoordinates = new long[coordinates.length];
        for(int i = 0; i < coordinates.length; i++){
            newCoordinates[i] = getNextContextRegionValue(coordinates[i], offsets[i]);
        }

        return new ClahePoint(newCoordinates);
    }

    private long getNextContextRegionValue(final long coordinate, final long offset) {
        long times = coordinate / offset;
        long ctxValue = times * offset + offset / 2;

        return ctxValue;
    }

    private class Histogram {

        private int m_valueNumber;

        private int[] m_values;

        private int m_valueCount;

        public Histogram(final int bins) {
            this.m_valueNumber = bins;
            this.m_values = new int[bins + 1];
            this.m_valueCount = 0;
        }

        public void add(final float pixel) {
            int value = Math.round((pixel / 255.0f) * m_valueNumber);
            ++m_values[value];
            ++m_valueCount;

        }

        public void clip(final float slope) {
            int limit = (int)(m_slope * (m_valueCount / m_valueNumber));
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
         * @param clippedHistogram clipped histogram.
         * @param oldValue the old value at a position in the input image.
         * @return the newValue which gets written in the ouput image.
         */
        private float buildCDF(final float oldValue) {
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

    private class ClahePoint {

        private long[] coordinates;

        /**
         * @param coordinates
         */
        public ClahePoint(final long[] coordinates) {
            this.coordinates = coordinates;
        }

        /**
         * @return the coordinates in all dimensions
         */
        public long[] getCoordinates(){
            return this.coordinates;
        }

        /**
         * @param i the dimension
         * @return the coordinate value in the given dimension
         */
        public long dim(final int i){
            return this.coordinates[i];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Arrays.hashCode(coordinates);
        }

    }
}
