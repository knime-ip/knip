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
package org.knime.knip.base.nodes.proc.maxfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape.NeighborhoodsAccessible;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape.NeighborhoodsIterableInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.iterableinterval.unary.MinMax;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Operation to compute local maxima on a RandomAccessibleInterval.
 *
 * @author Jonathan Hale, University of Konstanz
 * @param <T> Type of Input
 */

public class MaximumFinderOp<T extends RealType<T>> implements
        UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<BitType>> {

    private final double m_tolerance;

    private final double m_suppression;

    private final boolean m_maxAreas;

    private NeighborhoodsAccessible<T> m_neighborhoodsAccessible;

    private NeighborhoodsIterableInterval<T> m_neighborhoodsIterable;

    /**
     * @param tolerance
     * @param suppression
     * @param maxAreas
     */
    public MaximumFinderOp(final double tolerance, final double suppression, final boolean maxAreas) {
        m_tolerance = tolerance;
        m_suppression = suppression;

        m_maxAreas = maxAreas;
    }

    /**
     * @param tolerance
     * @param suppression
     */
    public MaximumFinderOp(final double tolerance, final double suppression) {
        this(tolerance, suppression, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RandomAccessibleInterval<BitType> compute(final RandomAccessibleInterval<T> input,
                                                     final RandomAccessibleInterval<BitType> output) {

        //find global min and max for optimization
        T t = Views.iterable(input).firstElement();
        T min = t.createVariable();
        min.setReal(t.getMinValue());
        T max = t.createVariable();
        max.setReal(t.getMaxValue());

        ValuePair<T, T> res = Operations.compute(new MinMax<T>(), Views.iterable(input));

        T globMin = res.getA().copy();
        T globMax = res.getB().copy();

        //IntervalView<T> extInput = Views.interval(Views.extend(input, m_outOfBounds), input);
        IntervalView<T> extInput = Views.interval(Views.extendValue(input, globMin), input);
        ArrayList<AnalyticPoint<T>> pList = new ArrayList<AnalyticPoint<T>>();

        RectangleShape shape = new RectangleShape(1, true); //"true" skips middle point

        m_neighborhoodsAccessible = shape.neighborhoodsRandomAccessible(extInput);
        m_neighborhoodsIterable = shape.neighborhoods(extInput);

        Cursor<Neighborhood<T>> cursor = m_neighborhoodsIterable.cursor();
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
                AnalyticPoint<T> p = new AnalyticPoint<T>(inputCursor, value.getRealDouble());
                pList.add(p);
            }
        }

        /*
         * Analyze the maxima candidates. Find out wich ones are real local maxima.
         */
        analyzeAndMarkMaxima(extInput, output, pList);
        return output;
    }

    // META Constants
    private final static int IS_MAX_AREA = 0x01;

    private final static int IS_PROCESSED = 0x02;

    private final static int IS_EQUAL = 0x04;

    private final static int IS_LISTED = 0x08;

    private final static int IS_MAX = 0x10;

    /**
     * Analyze and mark the maxima on the output image
     *
     * @param input
     * @param output
     * @param maxPoints
     */
    protected void analyzeAndMarkMaxima(final RandomAccessibleInterval<T> input,
                                        final RandomAccessibleInterval<BitType> output,
                                        final ArrayList<AnalyticPoint<T>> maxPoints) {

        final int numDimensions = input.numDimensions(); //shortcut

        // create a temporary point list and a true maxima list
        ArrayList<AnalyticPoint<T>> pList = new ArrayList<AnalyticPoint<T>>();
        ArrayList<AnalyticPoint<T>> trueMaxima = new ArrayList<AnalyticPoint<T>>();
        //sort maxPoints in decending order
        Collections.sort(maxPoints, Collections.reverseOrder());

        Img<IntType> metaImg = new ArrayImgFactory<IntType>().create(input, new IntType());

        OutOfBounds<IntType> raMeta = Views.extendValue(metaImg, new IntType(IS_PROCESSED | IS_LISTED)).randomAccess();

        RandomAccess<Neighborhood<T>> raNeigh = m_neighborhoodsAccessible.randomAccess(Intervals.expand(metaImg, 2));
        for (AnalyticPoint<T> maxPoint : maxPoints) {
            /*
             * The actual candidate was reached by previous steps.
             * Thus it is either a member of a plateau or connected
             * to a real max and thus no real local max. Ignore it.
             */
            raMeta.setPosition(maxPoint);

            if (isBitSet(raMeta.get(), IS_PROCESSED)) {
                continue;
            }

            double realMaxValue = maxPoint.getValue();

            Queue<AnalyticPoint<T>> queue = new LinkedList<AnalyticPoint<T>>();

            // double value of the point
            long[] equal = new long[numDimensions]; // sum of all positions of equal points
            maxPoint.localize(equal);

            int nEqual = 1;
            boolean maxPossible = true;

            //set meta of current point to be IS_LISTED and IS_EQUAL (to itself)+
            raMeta.setPosition(maxPoint);
            IntType maxPointMeta = raMeta.get();
            maxPointMeta.set(maxPointMeta.get() | IS_LISTED | IS_EQUAL);

            pList.clear();

            queue.clear();
            queue.offer(maxPoint);
            pList.add(maxPoint);

            while (!queue.isEmpty()) {
                AnalyticPoint<T> p = queue.poll();
                raNeigh.setPosition(p);
                Cursor<T> cNeigh = raNeigh.get().localizingCursor();

                while (cNeigh.hasNext()) { //iterate through our ROI/the structuring element
                    T pixel = cNeigh.next();

                    raMeta.setPosition(cNeigh);
                    IntType meta = raMeta.get(); //shortcut

                    if (!isBitSet(meta, IS_LISTED)) { //if this point isn't listed already
                        if (isBitSet(meta, IS_PROCESSED)) {
                            maxPossible = false; //we have reached a point processed previously, thus it is no maximum now
                            break;
                        }

                        //double value of the current pixel in struct el.
                        double realPixel = pixel.getRealDouble();

                        if (realPixel > realMaxValue) {
                            maxPossible = false; //we have reached a higher point, thus it is no maximum
                            break;
                        } else if (realPixel >= realMaxValue - m_tolerance) {
                            AnalyticPoint<T> point = new AnalyticPoint<T>(cNeigh, pixel.getRealDouble());

                            setBit(meta, IS_LISTED);
                            queue.offer(point);
                            pList.add(point);

                            if (realPixel == realMaxValue) { //prepare finding center of equal points (in case single point needed)
                                point.setEqual(true);

                                setBit(meta, IS_EQUAL);

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

            int resetMask = ~(maxPossible ? IS_LISTED : (IS_LISTED | IS_EQUAL));

            // calculate center of equal points
            for (int i = 0; i < numDimensions; ++i) {
                equal[i] /= nEqual;
            }

            long minDist = Long.MAX_VALUE; //minimal distance to the calculated center
            AnalyticPoint<T> nearestPoint = pList.get(0);

            int maxAreaFlag = maxPossible ? IS_MAX_AREA : 0;
            for (AnalyticPoint<T> p : pList) {
                raMeta.setPosition(p);
                IntType meta = raMeta.get();
                meta.set((meta.getInteger() & resetMask) | IS_PROCESSED | maxAreaFlag); //reset the no longer needed attributes

                if (maxPossible) {
                    if (p.isEqual()) {
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
                    nearestPoint.setMax(true);

                    raMeta.setPosition(nearestPoint);
                    setBit(raMeta.get(), IS_MAX);

                    trueMaxima.add(nearestPoint);
                }
            }
        } //iteration through maxPoints

        if (m_maxAreas) {
            Cursor<IntType> metaCursor = metaImg.cursor();
            Cursor<BitType> raOutput = Views.flatIterable(output).cursor();

            while (metaCursor.hasNext()) {
                raOutput.next().set(isBitSet(metaCursor.next(), IS_MAX_AREA));
            }
        } else {
            if (m_suppression > 0) {
                doSuppression(trueMaxima);
            }

            RandomAccess<BitType> raOutput = output.randomAccess();
            for (AnalyticPoint<T> p : trueMaxima) {
                raOutput.setPosition(p);
                raOutput.get().setOne();
            }
        }
    }

    static void setBit(final IntType type, final int bit) {
        type.setInteger(type.getInteger() | bit);
    }

    static boolean isBitSet(final IntType type, final int bit) {
        return (type.get() & bit) != 0;
    }

    /**
     * Suppression of Max Points within a given radius.
     *
     * @param list
     */
    protected void doSuppression(final List<AnalyticPoint<T>> list) {
        double supSq = m_suppression * m_suppression;

        int size = list.size();
        for (int i = 0; i < size; ++i) {
            for (int j = i + 1; j < size; ++j) {
                if (list.get(i).distanceToSq(list.get(j)) < supSq) {
                    list.remove(j);
                    size--;
                    j--;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<BitType>> copy() {
        return new MaximumFinderOp<T>(m_tolerance, m_suppression, m_maxAreas);
    }

}
