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
package org.knime.knip.core.ui.imgviewer.panels.transfunc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Stores all necessary info about a single Transferfunction.
 * 
 * The function is always considered to be piecewise and to be fixed at the left and right boundaries.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class PolylineTransferFunction implements TransferFunction, Iterable<PolylineTransferFunction.Point> {

    private static double MIN = 0.0;

    private static double MAX = 1.0;

    private static double EPSILON = 0.0001;

    /**
     * Provides a single point for a Transferfunction.
     */
    public class Point implements Comparable<Point> {

        private double m_x = MIN;

        private double m_y = MIN;

        private boolean m_fixed = false;

        private boolean m_selected = false;

        private boolean m_temp = false;

        /**
         * Sets up a new point at the given location and stores wheter it should be fixed in x direction or not.
         * 
         * The location is always stored as a fractinal value, i.e. a value between 0.0 and 1.0.
         * 
         * @param x the x position
         * @param y the y position
         * @param fixed wheter the point can be moved along the x axis
         * @param temp wheter the point is only a temp point for zooming
         */
        public Point(final double x, final double y, final boolean fixed, final boolean temp) {
            setX(x);
            setY(y);
            m_fixed = fixed;
            m_temp = temp;
        }

        public Point(final double x, final double y, final boolean fixed) {
            this(x, y, fixed, false);
        }

        /**
         * Sets up a new point at the given location and sets it to be not fixed.
         * 
         * @param x the x position
         * @param y the y position
         */
        public Point(final double x, final double y) {
            this(x, y, false);
        }

        /**
         * A copy constructor.
         * 
         * @param p the point to copy
         */
        public Point(final Point p) {
            this.m_x = p.m_x;
            this.m_y = p.m_y;
            this.m_fixed = p.m_fixed;
            this.m_temp = p.m_temp;
        }

        /**
         * Get x.
         * 
         * @return current value of x
         */
        public final double getX() {
            return m_x;
        }

        /**
         * Get y.
         * 
         * @return current value of y
         */
        public final double getY() {
            return m_y;
        }

        /**
         * Wheter or not the point is fixed.
         * 
         * @return true/false
         */
        public final boolean getFixed() {
            return m_fixed;
        }

        /**
         * Check if this point is currently highlighted.
         * 
         * @return true/false
         */
        public final boolean getSelected() {
            return m_selected;
        }

        /**
         * Set selection value.
         * 
         * @param value wheter or not this point is selected
         */
        public final void setSelected(final boolean value) {
            m_selected = value;
        }

        /**
         * Set a new position along the x axis.
         * 
         * Values larger 1.0 will be set to 1.0 and values smaller 0.0 to 0.0.
         * 
         * @param value the new value
         */
        private void setX(final double value) {
            if (!m_fixed) {
                m_x = value;

                // check for bounds
                if (m_x < MIN) {
                    m_x = MIN;
                }
                if (m_x > MAX) {
                    m_x = MAX;
                }

            }
        }

        /**
         * Set a new position along the y axis.
         * 
         * Values larger 1.0 will be set to 1.0 and values smaller 0.0 to 0.0.
         * 
         * @param value the new value
         */
        private void setY(final double value) {
            m_y = value;

            // check for bounds
            if (m_y < MIN) {
                m_y = MIN;
            }
            if (m_y > MAX) {
                m_y = MAX;
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see Comparable#compareTo(Point)
         */
        @Override
        public final int compareTo(final Point other) {
            if (m_x < other.getX()) {
                return -1;
            } else if (m_x == other.getX()) {
                return 0;
            } else {
                return 1;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof Point) {
                return ((Point)obj).getX() == m_x;
            } else {
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return new Double(m_x).hashCode();
        }
    }

    private List<Point> m_points = null;

    // lower and upper end of the zoomed in function
    private Point m_lower;

    private Point m_upper;

    // used for calculating the stretch factor
    private double m_minsf = MIN;

    private double m_maxsf = MAX;

    /**
     * Set up a new Transferfunction with two fixed points left and right.
     * 
     * 
     * This points will be positioned at (0,0) and (1,1).
     * 
     */
    public PolylineTransferFunction() {

        m_points = new LinkedList<Point>();

        // Add the two fixed Points
        m_lower = new Point(MIN, MIN, true);
        m_upper = new Point(MAX, MAX, true);

        m_points.add(m_lower);
        m_points.add(m_upper);
    }

    /**
     * A copy constuctor, resulting in a deep copy.
     * 
     * @param tf the function to copy
     */
    public PolylineTransferFunction(final PolylineTransferFunction tf) {

        // set up the list
        m_points = new LinkedList<Point>();

        // copy all the points
        for (final Point p : tf.m_points) {

            final Point cp = new Point(p);
            m_points.add(cp);

            if (tf.m_lower == p) {
                m_lower = cp;
            }

            if (tf.m_upper == p) {
                m_upper = cp;
            }
        }

        m_minsf = tf.m_minsf;
        m_maxsf = tf.m_maxsf;
    }

    @Override
    public PolylineTransferFunction copy() {
        return new PolylineTransferFunction(this);
    }

    private Point checkForPointAtPos(final double pos) {
        for (final Point p : m_points) {
            if (Math.abs(p.m_x - pos) < EPSILON) {
                return p;
            }
        }

        return null;
    }

    @Override
    public final void zoom(final double min, final double max) {

        if (min > max) {
            throw new IllegalArgumentException("Min must be smaller than max value");
        }

        if (min == max) {
            return;
        }

        if ((min == m_minsf) && (max == m_maxsf)) {
            return;
        }

        removeZoomPoints();

        addZoomPoints(min, max);

        sortPoints();
    }

    private void addZoomPoints(final double min, final double max) {

        assert (min < max);

        m_lower = addZoomPoint(min);
        m_upper = addZoomPoint(max);

        // stretch all points so that the points all lay between 0.0 and
        // 1.0
        m_minsf = m_lower.m_x;
        m_maxsf = m_upper.m_x;
        final double fac = m_maxsf - m_minsf;

        for (final Point p : m_points) {
            p.m_x = (p.m_x - m_minsf) / fac;
        }
    }

    private Point addZoomPoint(final double pos) {
        Point p = checkForPointAtPos(pos);

        if (p == null) {
            p = new Point(pos, getValueAt(pos), true, true);
            m_points.add(p);
        } else {
            p.m_fixed = true;
        }

        return p;
    }

    private void removeZoomPoints() {

        assert (m_lower != null);
        assert (m_upper != null);
        assert (m_points.size() >= 2);

        // undo the stretching of all points
        final double fac = m_maxsf - m_minsf;

        for (final Point p : m_points) {
            p.m_x = (p.m_x * fac) + m_minsf;
        }

        // remove
        removeZoomPoint(m_lower);

        removeZoomPoint(m_upper);
    }

    private void removeZoomPoint(final Point p) {

        assert (p != null);

        if (p.m_temp) {
            m_points.remove(p);
        } else {
            p.m_fixed = false;
        }
    }

    /**
     * Add a point to this Transferfunction.
     * 
     * @param x the x value
     * @param y the y value
     * @return the added Point
     */
    public final Point addPoint(final double x, final double y) {
        final Point p = new Point(x, y);
        m_points.add(p);
        sortPoints();

        return p;
    }

    /**
     * Remove a point from this Transferfunction.
     * 
     * @param p the point
     */
    public final boolean removePoint(final Point p) {
        if (!p.getFixed()) {
            m_points.remove(p);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get a list of all visible Points.<br>
     * 
     * This only includes the Points that are between the zoom values.
     * 
     * @return all points
     */
    public final List<Point> getPoints() {

        final List<Point> result = new ArrayList<Point>();

        int i = m_points.indexOf(m_lower) == -1 ? 0 : m_points.indexOf(m_lower);
        final int limit = m_points.indexOf(m_upper) == -1 ? m_points.size() - 1 : m_points.indexOf(m_upper);

        for (; i <= limit; i++) {
            result.add(m_points.get(i));
        }

        return result;
    }

    /**
     * Move a point to a new position.
     * 
     * @param p the point to move
     * @param x the x value
     * @param y the y value
     */
    public final void movePoint(final Point p, final double x, final double y) {
        p.setX(x);
        p.setY(y);

        p.m_temp = false;

        sortPoints();
    }

    /**
     * Sort the points in ascending order.
     */
    private void sortPoints() {
        Collections.sort(m_points);
    }

    /**
     * Get the value of this transfer function at the given position.
     * 
     * If pos is smaller than 0.0, it will be set to 0.0. If it is larger than 1.0, it will be set to 1.0.
     * 
     * @param pos the desired position
     * @return the value at the position
     */
    @Override
    public final double getValueAt(double pos) {

        // make sure pos is valid
        if (pos < MIN) {
            pos = MIN;
        }
        if (pos > MAX) {
            pos = MAX;
        }

        // check if this one of the points
        for (final Point p : m_points) {
            if (pos == p.getX()) {
                return p.getY();
            }
        }

        // else calculate the value
        Point left = m_points.get(0);
        Point right = m_points.get(m_points.size() - 1);

        for (final Point p : m_points) {
            if (p.getX() < pos) {
                left = p;
            } else { // fisrt value larger will be right
                right = p;
                break;
            }
        }

        final double m = (right.getY() - left.getY()) / (right.getX() - left.getX());

        return ((pos - left.getX()) * m) + left.getY();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Iterable#iterator()
     */
    @Override
    public Iterator<PolylineTransferFunction.Point> iterator() {
        return m_points.iterator();
    }
}
