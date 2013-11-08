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
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape.NeighborhoodsAccessible;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Operation to compute local Maxima on a RandomAccessibleInterval.
 *
 * @author Jonathan Hale, University of Konstanz
 * @param <T> Type of Input
 */

public class MaximumFinder<T extends RealType<T>> implements
        UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<BitType>> {

    private final double m_tolerance;

    private final double m_suppression;

    private OutOfBoundsFactory<T, RandomAccessibleInterval<T>> m_outOfBounds;

    /**
     * @param tolerance
     * @param suppression
     */
    public MaximumFinder(final double tolerance, final double suppression) {
        m_tolerance = tolerance;
        m_suppression = suppression;

        m_outOfBounds = new OutOfBoundsBorderFactory<T, RandomAccessibleInterval<T>>();
    }

    private NeighborhoodsAccessible<T> m_neighborhoods;

    /**
     * {@inheritDoc}
     */
    @Override
    public RandomAccessibleInterval<BitType> compute(final RandomAccessibleInterval<T> input,
                                                     final RandomAccessibleInterval<BitType> output) {

        IntervalView<T> extInput = Views.interval(Views.extend(input, m_outOfBounds), input);
        ArrayList<AnalyticPoint<T>> pList = new ArrayList<AnalyticPoint<T>>();

        //find global min and max for optimization TODO: Constructor not necessarily available anymore.
        T t = Views.iterable(input).firstElement();
        T min = t.createVariable();
        min.setReal(t.getMinValue());
        T max = t.createVariable();
        max.setReal(t.getMaxValue());
        ComputeMinMax<T> mm = new ComputeMinMax<T>(Views.iterable(extInput), max, min);
        mm.process();

        T globMin = mm.getMin();
        T globMax = mm.getMax();

        RectangleShape shape = new RectangleShape(1, true); //"true" skips middle point

        //TODO: extend input by something (e.g. given factory in constructor...)
        m_neighborhoods = shape.neighborhoods(extInput);

        Cursor<Neighborhood<T>> cursor = m_neighborhoods.cursor();
        Cursor<T> inputCursor = extInput.cursor();

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
        analyzeAndMarkMaxima(extInput, output, pList);
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
    protected void analyzeAndMarkMaxima(final RandomAccessibleInterval<T> input,
                                        final RandomAccessibleInterval<BitType> output,
                                        final ArrayList<AnalyticPoint<T>> maxPoints) {

        final int numDimensions = input.numDimensions(); //shortcut

        //TODO:
        final float maxSortingError = 1.0f;

        final int IS_MAX_AREA = 0x01;
        final int IS_PROCESSED = 0x02;
        final int IS_EQUAL = 0x04;
        final int IS_LISTED = 0x08;

        ArrayList<AnalyticPoint<T>> pList = new ArrayList<AnalyticPoint<T>>();
        ArrayList<AnalyticPoint<T>> trueMaxima = new ArrayList<AnalyticPoint<T>>();
        Collections.sort(maxPoints, Collections.reverseOrder());

        Img<IntType> metaImg = new ArrayImgFactory<IntType>().create(input, new IntType());
        OutOfBounds<IntType> raMeta = Views.extendValue(metaImg, new IntType(IS_PROCESSED)).randomAccess();

        RandomAccess<Neighborhood<T>> raNeigh = m_neighborhoods.randomAccess();
        for (AnalyticPoint<T> maxPoint : maxPoints) {
            /*
             * The actual candidate was reached by previous steps.
             * Thus it is either a member of a plateau or connected
             * to a real max and thus no real local max. Ignore it.
             */
            if (maxPoint.isProcessed()) {
                continue;
            }

            boolean sortingError = false;
            double realValue = maxPoint.getValue().getRealDouble();

            do { //while !sortingError

                // double value of the point
                long[] equal = new long[numDimensions]; // sum of all positions of equal points
                maxPoint.localize(equal);

                int nEqual = 1;
                boolean maxPossible = true;

                //set meta of current point to be IS_LISTED and IS_EQUAL (to itself)
                raMeta.setPosition(maxPoint);
                IntType maxPointMeta = raMeta.get();

                maxPointMeta.set(maxPointMeta.get() | IS_LISTED | IS_EQUAL);

                pList.clear();
                pList.add(maxPoint);

                for (AnalyticPoint<T> p : pList) {
                    raNeigh.setPosition(p);
                    Cursor<T> cNeigh = raNeigh.get().localizingCursor();

                    while (cNeigh.hasNext()) { //iterate through our ROI/the structuring element
                        T pixel = cNeigh.next();

                        if ((maxPointMeta.getInteger() & IS_LISTED) == 0) {
                            //double value of the current pixel in struct el.
                            double realPixel = pixel.getRealDouble();

                            raMeta.setPosition(cNeigh);

                            IntType meta = raMeta.get(); //shortcut

                            if ((meta.getInteger() & IS_PROCESSED) != 0) {
                                maxPossible = false; //we have reached a point processed previously, thus it is no maximum now
                                break;
                            }
                            //TODO: generalize with .add()
                            if (realPixel > realValue + maxSortingError) {
                                maxPossible = false; //we have reached a higher point, thus it is no maximum
                                break;
                            } else if (realPixel >= realValue - m_tolerance) {
                                if (pixel.compareTo(maxPoint.getValue()) > 0) { //maybe this point should have been treated earlier
                                    sortingError = true;
                                }

                                pList.add(new AnalyticPoint<T>(cNeigh, pixel));

                                meta.set(meta.getInteger() | IS_LISTED);

                                if (pixel == maxPoint.getValue()) { //prepare finding center of equal points (in case single point needed)
                                    meta.set(meta.getInteger() | IS_EQUAL);

                                    //add to equal:
                                    for (int i = 0; i < numDimensions; ++i) {
                                        equal[i] += cNeigh.getIntPosition(i);
                                    }

                                    ++nEqual; //we found one more equal point
                                }
                            }
                        }
                    }
                }

                if (sortingError) {
                    new RuntimeException("Sorting Errors do happen!");
                } else {
                    int resetMask = ~(maxPossible ? IS_LISTED : (IS_LISTED | IS_EQUAL));

                    // calculate center of equal points
                    for (int i = 0; i < numDimensions; ++i) {
                        equal[i] /= nEqual;
                    }

                    long minDist = Long.MAX_VALUE; //minimal distance to the calculated center
                    AnalyticPoint<T> nearestPoint = null;

                    for (AnalyticPoint<T> p : pList) {
                        raMeta.setPosition(p);
                        IntType meta = raMeta.get();
                        meta.set((meta.getInteger() & resetMask) | IS_PROCESSED | (maxPossible ? IS_MAX_AREA : 0)); //reset no longer needed attributes

                        if (maxPossible) {
                            if ((meta.getInteger() & IS_EQUAL) != 0) {
                                long dist = p.distanceToSq(equal);
                                if (dist < minDist) {
                                    minDist = dist; //this could be the best "single maximum" point
                                    nearestPoint = p;
                                }
                            }
                        }
                    } //iteration through pList

                    if (nearestPoint != null) {
                        if (maxPossible) {
                            trueMaxima.add(nearestPoint);
                        }
                    }
                } // if (!sortingError)
            } while (sortingError);
        } //iteration through maxPoints

        /* Remove every Point from the List that  is no max.
         * Useful for all later operations on the list.
         */
        //TODO: optimize for no suppression and suppression. Only one iteration over list for no suppression!
        ArrayList<AnalyticPoint<T>> cpList = new ArrayList<AnalyticPoint<T>>();
        RandomAccess<BitType> raOutput = output.randomAccess();
        for (AnalyticPoint<T> p : trueMaxima) {
                cpList.add(p);
                raOutput.setPosition(p.getPosition());
                raOutput.get().setReal(255);
        }

        /*
        if (m_suppression > 0) {
            doSuppression(cpList); //TODO
        }

         * Mark all single maximum points.
         */

    }

    /**
     * Suppression of Max Points within a given radius.
     */
    protected void doSuppression(final List<AnalyticPoint<T>> pList) {

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
