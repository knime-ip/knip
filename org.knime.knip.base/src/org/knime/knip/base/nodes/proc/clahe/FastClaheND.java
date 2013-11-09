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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
public class FastClaheND<T extends RealType<T>> implements
        UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> {

    private final long[] m_ctxNumberDims;

    private final int m_bins;

    private final float m_slope;

    /**
     * @param bins
     * @param slope
     */
    public FastClaheND(final long[] m_ctxNumberDims, final int bins, final float slope) {
        this.m_ctxNumberDims = m_ctxNumberDims;
        this.m_bins = bins;
        this.m_slope = slope;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> copy() {
        return new FastClaheND<T>(m_ctxNumberDims, m_bins, m_slope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RandomAccessibleInterval<T> compute(final RandomAccessibleInterval<T> input,
                                               final RandomAccessibleInterval<T> output) {

        Cursor<T> inputCursor = Views.flatIterable(input).localizingCursor();
        Cursor<T> outputCursor = Views.flatIterable(output).cursor();

        long[] imageDimensions = new long[input.numDimensions()];
        for (int i = 0; i < imageDimensions.length; i++) {
            imageDimensions[i] = input.dimension(i);
        }

        long[] offsets = new long[imageDimensions.length];
        for (int i = 0; i < imageDimensions.length; i++) {
            offsets[i] = imageDimensions[i] / m_ctxNumberDims[i];
        }

        Map<ClahePoint, ClaheHistogram> ctxHistograms = new HashMap<ClahePoint, ClaheHistogram>();
        long[] pos = new long[input.numDimensions()];

        while (inputCursor.hasNext()) {
            inputCursor.next();
            inputCursor.localize(pos);

            ClahePoint center = getNearestCenter(pos, offsets);

            ClaheHistogram hist = ctxHistograms.get(center);
            if (hist == null) {
                hist = new ClaheHistogram(m_bins);
                ctxHistograms.put(center, hist);
            }

            ctxHistograms.get(center).add(inputCursor.get().getRealFloat());
        }

        // after creation of the histograms, clip them
        for (ClahePoint center : ctxHistograms.keySet()) {
            ctxHistograms.get(center).clip(m_slope);
        }

        inputCursor.reset();
        while (inputCursor.hasNext()) {
            inputCursor.next();
            inputCursor.localize(pos);
            ClahePoint currentPoint = new ClahePoint(pos);

            outputCursor.next();

            int oldValue = Math.round(inputCursor.get().getRealFloat() / 255f * m_bins);


            List<ClahePoint> neighbors = getNeighbors(currentPoint, offsets, imageDimensions);

            float newValue = interpolate(currentPoint, oldValue, neighbors, ctxHistograms);
            outputCursor.get().setReal(newValue);
        }

        return null;
    }

    /**
     * @param currentPoint
     * @param neighbors
     * @param ctxHistograms
     * @return
     */
    private float interpolate(final ClahePoint currentPoint, final float oldValue, final List<ClahePoint> neighbors,
                              final Map<ClahePoint, ClaheHistogram> ctxHistograms) {


        if (neighbors.size() == 1) {
            return ctxHistograms.get(neighbors.get(0)).buildCDF(oldValue);
        } else if(neighbors.size() == 2){

            float distanceOne = currentPoint.distance(neighbors.get(0));
            float distanceTwo = currentPoint.distance(neighbors.get(1));
            float completeDistance = distanceOne + distanceTwo;

            float weightOne = 1 - distanceOne / completeDistance;
            float weightTwo = 1 - distanceTwo / completeDistance;

            return ctxHistograms.get(neighbors.get(0)).buildCDF(oldValue) * weightOne + ctxHistograms.get(neighbors.get(1)).buildCDF(oldValue) * weightTwo;
        } else if(neighbors.size() == 4){
            ClahePoint topLeft = null;
            ClahePoint topRight = null;
            ClahePoint bottomLeft = null;
            ClahePoint bottomRight = null;
            for (ClahePoint clahePoint : neighbors) {
                if(clahePoint.dim(0) <= currentPoint.dim(0) && clahePoint.dim(1) <= currentPoint.dim(1)){
                    topLeft = clahePoint;
                } else if(clahePoint.dim(0) > currentPoint.dim(0) && clahePoint.dim(1) <= currentPoint.dim(1)){
                    topRight = clahePoint;
                } else if(clahePoint.dim(0) <= currentPoint.dim(0) && clahePoint.dim(1) > currentPoint.dim(1)){
                    bottomLeft = clahePoint;
                } else {
                    bottomRight = clahePoint;
                }
            }

            float weightX = (1 - ((float)currentPoint.dim(0) - topLeft.dim(0)) / (topLeft.distance(topRight)));
            float weightY = (1 - ((float)currentPoint.dim(1) - topLeft.dim(1)) / (topLeft.distance(bottomLeft)));

            float newValue =
                    weightY
                            * (weightX * ctxHistograms.get(topLeft).buildCDF(oldValue) + (1 - weightX)
                                    * ctxHistograms.get(topRight).buildCDF(oldValue))
                            + (1 - weightY)
                            * (weightX * ctxHistograms.get(bottomLeft).buildCDF(oldValue) + (1 - weightX)
                                    * ctxHistograms.get(bottomRight).buildCDF(oldValue));

            return newValue;
        }

        return oldValue;
    }

    /**
     * This method retrieves all nearby centers for a given point. Works in n dimensions.
     *
     * @param currentPoint the current point
     * @param offsets the offsets of the centers
     * @param imageDimensions the dimensions of the image
     * @return A List containing all nearby centers
     */
    private List<ClahePoint> getNeighbors(final ClahePoint currentPoint, final long[] offsets,
                                          final long[] imageDimensions) {

        // create output list and find the nearest center (doesn't matter if it lies outside of the image boundaries)
        List<ClahePoint> neighbors = new ArrayList<ClahePoint>();
        ClahePoint nearestCenter = getNearestCenter(currentPoint, offsets);

        // if we're at a center we only have to add the nearest center
        if (currentPoint.isInsideImage(imageDimensions) && currentPoint.equals(nearestCenter)) {
            neighbors.add(nearestCenter);
        } else {

            // calculate the point on the top left (x,y,z,... coordinates are all smaller)
            long[] topLeftCenter = Arrays.copyOf(nearestCenter.getCoordinates(), nearestCenter.numDim());
            for (int i = 0; i < topLeftCenter.length; i++) {
                if (topLeftCenter[i] > currentPoint.dim(i)) {
                    topLeftCenter[i] -= offsets[i];
                }
            }

            // create every possible combination of the indices (needed to get all neighbors);
            Integer[] indices = new Integer[nearestCenter.numDim()];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = i;
            }

            List<List<Integer>> indicesCombinations = new ArrayList<List<Integer>>();
            for (int i = 1; i <= indices.length; i++) {
                indicesCombinations.addAll(combination(Arrays.asList(indices), i));
            }

            ClahePoint topLeftCenterPoint = new ClahePoint(topLeftCenter);
            // now we can start adding the neighbors, if the top left one lies inside the image add it
            if (topLeftCenterPoint.isInsideImage(imageDimensions)) {
                neighbors.add(topLeftCenterPoint);
            }

            // we created every combination of indices, just increment the position add the indices by their offset to get all of neighbors
            for (List<Integer> indicesList : indicesCombinations) {


                long[] temp = Arrays.copyOf(topLeftCenter, topLeftCenter.length);
                for (int index : indicesList) {
                    temp[index] += offsets[index];
                }

                ClahePoint cp = new ClahePoint(temp);
                if (cp.isInsideImage(imageDimensions)) {
                    neighbors.add(cp);
                }
            }

        }

        return neighbors;
    }

    /**
     * This method creates all combinations of a given input. E.g. for {1, 2, 3} => {1} {2} {3} {1, 2} {1, 3} {2, 3} {1, 2,
     * 3}
     *
     * @param values the values
     * @param size the size of the subset
     * @return All possible combinations of the input set.
     */
    public static <L> List<List<L>> combination(final List<L> values, final int size) {

        if (0 == size) {
            return Collections.singletonList(Collections.<L> emptyList());
        }

        if (values.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<L>> combination = new LinkedList<List<L>>();

        L actual = values.iterator().next();

        List<L> subSet = new LinkedList<L>(values);
        subSet.remove(actual);

        List<List<L>> subSetCombination = combination(subSet, size - 1);

        for (List<L> set : subSetCombination) {
            List<L> newSet = new LinkedList<L>(set);
            newSet.add(0, actual);
            combination.add(newSet);
        }

        combination.addAll(combination(subSet, size));

        return combination;
    }

    private ClahePoint getNearestCenter(final long[] coordinates, final long[] offsets) {
        long[] newCoordinates = new long[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            newCoordinates[i] = getNextContextRegionValue(coordinates[i], offsets[i]);
        }

        return new ClahePoint(newCoordinates);
    }

    private ClahePoint getNearestCenter(final ClahePoint cp, final long[] offsets) {
        return getNearestCenter(cp.getCoordinates(), offsets);
    }

    private long getNextContextRegionValue(final long coordinate, final long offset) {
        long times = coordinate / offset;
        long ctxValue = times * offset + offset / 2;

        return ctxValue;
    }

}
