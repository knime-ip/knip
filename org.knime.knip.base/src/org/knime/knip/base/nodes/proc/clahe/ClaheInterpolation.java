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
 * Created on 17.11.2013 by Daniel Seebacher
 */
package org.knime.knip.base.nodes.proc.clahe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class provides the function needed by the CLAHE algorithm to make a linear interpolation in n-dimensions
 * (linear, bilinear, trilinear, quadlinear, etc.)
 *
 * @author Daniel Seebacher
 */
public class ClaheInterpolation {

    /**
     * @param currentPoint the current point (somewhere in between the neighbors)
     * @param neighbors a list containing all neighbors
     * @param oldValue the old value at the position of the current point
     * @param histValues the values of the histograms at the positions of the neighbors
     * @return the new value for this position
     */
    public static double interpolate(final ClahePoint currentPoint, final List<ClahePoint> neighbors,
                                    final double oldValue, final double[] histValues) {

        // create a list containg all points and their value and sort them using the dimension comparator
        List<InterpolationPoint> ips = new ArrayList<ClaheInterpolation.InterpolationPoint>();
        for (int i = 0; i < neighbors.size(); i++) {
            ips.add(new InterpolationPoint(neighbors.get(i).getCoordinates(), histValues[i]));
        }
        Collections.sort(ips, getDimensionComparator());


        return interpolate(currentPoint, ips, 0);
    }

    /**
     * This method makes a linear interpolation in every dimension.
     * @param currentPoint the point for which a new value should be calculated
     * @param ips the points used for the interpolation
     * @param dim the dimension in which the interpolation is made
     * @return
     */
    private static double interpolate(final ClahePoint currentPoint, final List<InterpolationPoint> ips, final int dim) {

        // because the points were sorted beforehand the values for an interpolation are always at i and i+1
        List<InterpolationPoint> newIPS = new ArrayList<ClaheInterpolation.InterpolationPoint>();
        for (int i = 0; i < ips.size(); i += 2) {
            // calculate the distances in the dimension i
            double distanceOne = Math.abs(ips.get(i).dim(dim) - currentPoint.dim(dim));
            double distanceTwo = Math.abs(ips.get(i + 1).dim(dim) - currentPoint.dim(dim));
            double completeDistance = distanceOne + distanceTwo;

            // calculate the weights
            double weightOne = 1 - distanceOne / completeDistance;
            double weightTwo = 1 - distanceTwo / completeDistance;

            // calculate the new value
            double val = (ips.get(i).getValue() * weightOne)
                    + (ips.get(i + 1).getValue() * weightTwo);

            // check if it is a number (could happen if there are no values in a dimension)
            if(Double.isNaN(val)){
                newIPS.add(new InterpolationPoint(ips.get(i).getCoordinates(), 0));
            } else {
                newIPS.add(new InterpolationPoint(ips.get(i).getCoordinates(), val));
            }
        }

        if (newIPS.size() == 1) {
            return newIPS.get(0).getValue();
        } else {
            return interpolate(currentPoint, newIPS, dim + 1);
        }
    }

    /**
     * Used to sort the array of interpolation points by their values in the different dimensions (think about a reversed lexicographic order).
     * For example: {0, 1, 2} < {0, 10, 2} < {0, 1, 3}
     * @return Comparator
     */
    private static Comparator<InterpolationPoint> getDimensionComparator() {
        return new Comparator<InterpolationPoint>() {
            @Override
            public int compare(final InterpolationPoint o1, final InterpolationPoint o2) {

                int numDim = o1.numDim();

                for (int i = numDim - 1; i >= 0; i--) {
                    if (o1.dim(i) < o2.dim(i)) {
                        return -1;
                    } else if (o1.dim(i) > o2.dim(i)) {
                        return 1;
                    }
                }

                return 0;
            }
        };
    }

    private static class InterpolationPoint {

        private long[] m_coordinates;

        private double m_value;

        /**
         * @param coordinates
         * @param value
         */
        public InterpolationPoint(final long[] coordinates, final double value) {
            this.m_coordinates = coordinates;
            this.m_value = value;
        }

        /**
         * @return the coordinates in all dimensions
         */
        public long[] getCoordinates() {
            return this.m_coordinates;
        }

        /**
         * @return the dimensionality of this point
         */
        public int numDim() {
            return m_coordinates.length;
        }

        /**
         * @param i the dimension
         * @return the coordinate value in the given dimension
         */
        public long dim(final int i) {
            return this.m_coordinates[i];
        }

        public double getValue() {
            return this.m_value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Arrays.toString(m_coordinates) + "\t" + m_value;
        }
    }

}
