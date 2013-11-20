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

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * This class is an extension of the CLAHE algorithm to work with n-dimensional images. It divides the image into
 * context regions, for each context region a histogram is built which stores the number of unique pixel values. After
 * that new values are assigned by building a cumulative distribution function for each histogram and then by
 * interpolation between these resulting values.
 *
 *
 * @param <T> extends RealType<T>
 * @see <a href="http://en.wikipedia.org/wiki/Adaptive_histogram_equalization">[1] Adaptive histogram equalization</a>
 * @see "[2] K. Zuiderveld: Contrast Limited Adaptive Histogram Equalization. In: P. Heckbert: Graphics Gems IV, Academic Press 1994, ISBN 0-12-336155-9"
 *
 * @author Daniel Seebacher, University of Konstanz
 */
public class FastClaheND<T extends RealType<T>> implements
        UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> {

    private final long m_ctxNumberDims;

    private final int m_bins;

    private final double m_slope;

    /**
     *
     * @param i number of context regions for each dimension
     * @param bins number of bins used by the histograms
     * @param d slope used for the clipping function
     */
    public FastClaheND(final long i, final int bins, final double d) {
        this.m_ctxNumberDims = i;
        this.m_bins = bins;
        this.m_slope = d;

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



        // create image cursors, flatIterable to achieve same iteration order for both images.
        final Cursor<T> inputCursor = Views.flatIterable(input).localizingCursor();
        final Cursor<T> outputCursor = Views.flatIterable(output).cursor();

        // 1. calculate some necessary informations
        final long[] imageDimensions;
        final long[] offsets;
        final List<List<Integer>> indexCombinations;
        {
            // store image dimensions
            imageDimensions = new long[input.numDimensions()];
            for (int i = 0; i < imageDimensions.length; i++) {
                imageDimensions[i] = input.dimension(i);
            }

            // calculate offsets of the context centers in each dimensions
            offsets = new long[imageDimensions.length];
            for (int i = 0; i < imageDimensions.length; i++) {
                offsets[i] = imageDimensions[i] / m_ctxNumberDims;
            }

            // power set of the indices (needed to calculate neighbors in n-dimensions)
            Integer[] indices = new Integer[input.numDimensions()];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = i;
            }
            indexCombinations = new ArrayList<List<Integer>>();
            for (int i = 1; i <= indices.length; i++) {
                indexCombinations.addAll(combination(Arrays.asList(indices), i));
            }
        }

        // 2. create histograms and clip them
        final HashMap<ClahePoint, ClaheHistogram> ctxHistograms = new HashMap<ClahePoint, ClaheHistogram>();
        {
            // first iteration through the image to build the context histograms
            long[] pos = new long[input.numDimensions()];
            while (inputCursor.hasNext()) {
                inputCursor.next();
                inputCursor.localize(pos);

                // calculate position of nearest center
                ClahePoint center = getNearestCenter(pos, offsets);

                // add point to according context histogram (create it, if it doesn't exist yet)
                ClaheHistogram hist = ctxHistograms.get(center);
                if (hist == null) {
                    hist = new ClaheHistogram(m_bins);
                    ctxHistograms.put(center, hist);
                }

                ctxHistograms.get(center).add(inputCursor.get().getRealDouble());
            }

            // after creation of the histograms, clip them
            for (ClahePoint center : ctxHistograms.keySet()) {
                ctxHistograms.get(center).clip(m_slope);
            }
        }

        // 3. for each pixel interpolate the new value
        {
            inputCursor.reset();
            long[] pos = new long[input.numDimensions()];
            while (inputCursor.hasNext()) {
                inputCursor.next();
                inputCursor.localize(pos);
                outputCursor.next();

                // get current position and the old value at this position
                ClahePoint currentPoint = new ClahePoint(pos);
                int oldValue = (int)Math.round(inputCursor.get().getRealDouble() / 255d * m_bins);

                // find all neighboring context centers
                List<ClahePoint> neighbors = getNeighbors(currentPoint, offsets, imageDimensions, indexCombinations);

                // calculate the new value through interpolation
                double newValue = interpolate(currentPoint, oldValue, neighbors, ctxHistograms);
                outputCursor.get().setReal(newValue);
            }
        }

        return output;
    }

    /**
     * @param coordinate the coordinate in any dimension
     * @param offset the offset in the according dimension
     * @return the position of the next context center in this dimension
     */
    private long getNextContextRegionValue(final long coordinate, final long offset) {
        long times = coordinate / offset;
        long ctxValue = times * offset + offset / 2;

        return ctxValue;
    }

    /**
     * @param coordinates coordinates of a point
     * @param offsets the offsets of the context centers
     * @return The center of the nearest context region for a given point.
     */
    private ClahePoint getNearestCenter(final long[] coordinates, final long[] offsets) {
        long[] newCoordinates = new long[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            newCoordinates[i] = getNextContextRegionValue(coordinates[i], offsets[i]);
        }

        return new ClahePoint(newCoordinates);
    }

    /**
     * @param coordinates coordinates of a point
     * @param offsets the offsets of the context centers
     * @return The center of the nearest context region for a given point.
     */
    private ClahePoint getNearestCenter(final ClahePoint cp, final long[] offsets) {
        return getNearestCenter(cp.getCoordinates(), offsets);
    }

    /**
     * @param values some value
     * @param size the size of the set
     * @return The power set of the input values
     */
    private <L> List<List<L>> combination(final List<L> values, final int size) {

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

    /**
     * This method retrieves all nearby centers for a given point. Works in n dimensions.
     *
     * @param currentPoint the current point
     * @param offsets the offsets of the centers
     * @param imageDimensions the dimensions of the image
     * @param indicesCombinations2
     * @return A List containing all nearby centers
     */
    private List<ClahePoint> getNeighbors(final ClahePoint currentPoint, final long[] offsets,
                                          final long[] imageDimensions, final List<List<Integer>> indexCombinations) {

        // create output list and find the nearest center (doesn't matter if it lies outside of the image boundaries)
        List<ClahePoint> neighbors = new ArrayList<ClahePoint>();
        ClahePoint nearestCenter = getNearestCenter(currentPoint, offsets);

        // if we're at a center we only have to add the nearest center
        if (currentPoint.equals(nearestCenter) && currentPoint.isInsideImage(imageDimensions)) {
            neighbors.add(nearestCenter);
        } else {

            // calculate the point on the top left (x,y,z,... coordinates are all smaller)
            long[] topLeftCenter = Arrays.copyOf(nearestCenter.getCoordinates(), nearestCenter.numDim());
            for (int i = 0; i < topLeftCenter.length; i++) {
                // rather dirty hack but it works
                if (currentPoint.dim(i) > imageDimensions[i] / 2) {
                    if (topLeftCenter[i] >= currentPoint.dim(i)) {
                        topLeftCenter[i] -= offsets[i];
                    }
                } else {
                    if (topLeftCenter[i] > currentPoint.dim(i)) {
                        topLeftCenter[i] -= offsets[i];
                    }
                }

            }

            ClahePoint topLeftCenterPoint = new ClahePoint(topLeftCenter);

            // now we can start adding the neighbors, if the top left one lies inside the image add it
            if (topLeftCenterPoint.isInsideImage(imageDimensions)) {
                neighbors.add(topLeftCenterPoint);
            }

            // we created every combination of indices, just increment the position add the indices by their offset to get all of neighbors
            for (List<Integer> indicesList : indexCombinations) {

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
     * Calculates a new value by interpolation between the neighboring context regions.
     *
     * @param currentPoint the current point
     * @param neighbors the neighboring context centers
     * @param ctxHistograms the context histograms
     * @return the new value for this position
     */
    private double interpolate(final ClahePoint currentPoint, final double oldValue, final List<ClahePoint> neighbors,
                               final HashMap<ClahePoint, ClaheHistogram> ctxHistograms) {

        // if the number of neighbors is one, no interpolation is necessary
        if (neighbors.size() == 1) {
            return ctxHistograms.get(neighbors.get(0)).buildCDF(oldValue);
        } else {
            double[] histValues = new double[neighbors.size()];
            for (int i = 0; i < neighbors.size(); i++) {
                histValues[i] = ctxHistograms.get(neighbors.get(i)).buildCDF(oldValue);
            }

            return NDLinearInterpolation.interpolate(currentPoint, neighbors, oldValue, histValues);
        }
    }
}
