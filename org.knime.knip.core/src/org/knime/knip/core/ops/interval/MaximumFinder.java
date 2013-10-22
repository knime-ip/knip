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
package org.knime.knip.core.ops.interval;

import java.util.ArrayList;
import java.util.Collections;

import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape.NeighborhoodsAccessible;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * Operation to compute local Maxima on a RandomAccessibleInterval.
 *
 * @author Jonathan Hale, University of Konstanz
 */

public class MaximumFinder<T extends RealType<T>> implements
        UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<BitType>> {

    private final double m_tolerance;

    private final double m_suppression;

    private long[][] m_strucEl;

    private ArrayList<AnalyticPoint<T>> pList;

    public MaximumFinder(final double noise, final double suppression) {
        m_tolerance = noise;
        m_suppression = suppression;
    }

    private NeighborhoodsAccessible<T> m_neighborhoods;

    /**
     * {@inheritDoc}
     */
    public RandomAccessibleInterval<BitType> compute(final RandomAccessibleInterval<T> input,
                                                     final RandomAccessibleInterval<BitType> output) {

        pList = new ArrayList<AnalyticPoint<T>>();

        //find global min and max for optimization TODO: Constructor not necessarily available anymore.
        ComputeMinMax<T> mm = new ComputeMinMax<T>(input);
        mm.process();

        T globMin = mm.getMin();
        T globMax = mm.getMax();

        RectangleShape shape = new RectangleShape(1, true); //"true" skips middle point

        //TODO: extend input by something (e.g. given factory in constructor...)
        m_neighborhoods = shape.neighborhoods(input);

        Cursor<Neighborhood<T>> cursor = m_neighborhoods.cursor();
        Cursor<T> inputCursor = Views.iterable(input).cursor();

        while (cursor.hasNext() && inputCursor.hasNext()) {
            cursor.fwd();

            T value = inputCursor.next();

            if (value.equals(globMin)) {
                continue;
            }

            boolean maxCandidate = true;

            if (!value.equals(globMax)) {
                // if we have a global Maxima, we therefore must have a
                // local maxima aswell, we won't need to check here.

                // iterate over currently defined pixels
                for (T cur : cursor.get()) {

                    if (value.compareTo(cur) < 0) {
                        // a surrounding pixel has a higher value.
                        maxCandidate = false;
                        break;
                    }
                }
            }

            if (maxCandidate) {
                AnalyticPoint<T> p = new AnalyticPoint<T>(inputCursor, value);
                pList.add(p);
            }
        }

        /*
         * Analyze the maxima candidates. Find out wich ones are real local maxima.
         */
        analyzeAndMarkMaxima(input, input /* dimensions */, output);
        return output;
    }

    /*
     * Status Image Type should not be
     * anything higher than ByteType
     */
    /**
     * @param input
     * @param dimensions
     * @param output
     */

    protected void analyzeAndMarkMaxima(final RandomAccessibleInterval<T> input, final Dimensions dimensions,
                                        final RandomAccessibleInterval<BitType> output) {

        RandomAccess<Neighborhood<T>> raNeigh = m_neighborhoods.randomAccess();
        for (AnalyticPoint<T> p : pList) {
            /*
             * The actual candidate was reached by previous steps.
             * Thus it is either a member of a plateau or connected
             * to a real max and thus no real local max. Ignore it.
             */
            if (p.isProcessed()) {
                continue;
            }

            raNeigh.setPosition(p);

            double realValue = p.getValue().getRealDouble();

            boolean maxPossible = true;
            for (T pixel : raNeigh.get()) {
                double realPixel = pixel.getRealDouble();
                if (processed) { //TODO
                    maxPossible = false; //we have reached a point processed previously, thus it is no maximum now
                    break;
                }
                if (realPixel > realValue + maxSortingError) { //TODO
                    maxPossible = false; //we have reached a higher point, thus it is no maximum
                    break;
                } else if (realPixel >= realValue - m_tolerance) { //TODO
                    if (pixel.compareTo(p.getValue()) > 0) { //maybe this point should have been treated earlier
                        //sortingError = true; //TODO
                    }
                    pList.add(new AnalyticPoint<T>(null, pixel)); //TODO: need position of cursor here.
                    if (pixel == p.getValue()) { //prepare finding center of equal points (in case single point needed)
                        //set an EQUAL flag to the new point.
                    }
                }
            }
            //Code of ImageJ1
            if (!sortingError) { //TODO: Sorting Error stuff
                int resetMask = ~(maxPossible ? LISTED : (LISTED | EQUAL));
                xEqual /= nEqual;
                yEqual /= nEqual;
                double minDist2 = 1e20;
                int nearestI = 0;
                for (listI = 0; listI < listLen; listI++) {
                    int offset = pList[listI];
                    int x = offset % width;
                    int y = offset / width;
                    types[offset] &= resetMask; //reset attributes no longer needed
                    types[offset] |= PROCESSED; //mark as processed
                    if (maxPossible) {
                        types[offset] |= MAX_AREA;
                        if ((types[offset] & EQUAL) != 0) {
                            double dist2 = (xEqual - x) * (xEqual - x) + (yEqual - y) * (yEqual - y);
                            if (dist2 < minDist2) {
                                minDist2 = dist2; //this could be the best "single maximum" point
                                nearestI = listI;
                            }
                        }
                    }
                } // for listI
                if (maxPossible) {
                    int offset = pList[nearestI];
                    types[offset] |= MAX_POINT;
                    if (displayOrCount && !(MaximumFinder.excludeOnEdges && isEdgeMaximum)) {
                        int x = offset % width;
                        int y = offset / width;
                        if (roi == null || roi.contains(x, y)) {
                            xyVector.addElement(new int[]{x, y});
                        }
                    }
                }
            }
        }

        /* Remove every Point from the List that  is no max.
         * Useful for all later operations on the list.
         */
        ArrayList<AnalyticPoint<T>> cpList = (ArrayList<AnalyticPoint<T>>)pList.clone();
        for (AnalyticPoint<T> p : cpList) {
            if (!p.isMax()) {
                pList.remove(p);
            }
        }

        if (m_suppression > 0) {
            doSuppression();
        }

        /*
         * Mark all single maximum points.
         */

        RandomAccess<BitType> raOutput = output.randomAccess();
        for (AnalyticPoint<T> p : pList) {
            raOutput.setPosition(p.getPosition());
            raOutput.get().setReal(255);
        }

    }

    /**
     * Suppression of Max Points within a given radius.
     */
    protected void doSuppression() {

        Collections.sort(pList);

        /*Build a distance Matrix (To avoid running
         * over everything more often than we need to
         */
        //TODO
        int size = pList.size();
        for (int i = 0; i < size; ++i) {
            for (int j = i + 1; j < size; ++j) {
                if (pList.get(i).distanceTo(pList.get(j)) < m_suppression) {
                    pList.remove(j);
                    size--;
                    j--;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<BitType>> copy() {
        return new MaximumFinder<T>(m_tolerance, m_suppression);
    }

}
