package org.knime.knip.core.ops.interval;

import java.util.Arrays;

import net.imglib2.Localizable;
import net.imglib2.type.numeric.RealType;

public class AnalyticPoint<T extends RealType<T>> implements Comparable<AnalyticPoint<T>>, Localizable {

    private double m_value;

    private int[] m_coords;

    private boolean m_isMax; //is maxmimum flag

    private boolean m_equal;

    /**
     * Contructor of AnalyticPoint
     * isMax defaults to false
     * processed defaults to false
     *
     * @param pos Position of the point
     * @param val the value
     */
    public AnalyticPoint(final Localizable pos, final double val) {
        m_value = val;

        m_coords = new int[pos.numDimensions()];
        pos.localize(m_coords);

        m_isMax = false;
        m_equal = false;
    }

    public int[] getPosition() {
        return m_coords;
    }

    /**
     * Set the flag which describes this as a maximum or not.
     * @param b
     */
    public void setMax(final boolean b) {
        m_isMax = b;
    }

    /**
     * Get the flag which describes this as a maximum or not.
     * @return
     */
    public boolean isMax() {
        return m_isMax;
    }

    /**
     * @param b
     */
    public void setEqual(final boolean b) {
        m_equal = b;
    }

    /**
     * @return
     */
    public boolean isEqual() {
        return m_equal;
    }


    /**
     * Calculate distance to a Localizable
     *
     * Equivalent to Math.sqrt(distanceToSq(p));
     * @param p
     * @return distance
     */
    public double distanceTo(final Localizable p) {
        return Math.sqrt(distanceToSq(p));
    }

    /**
     * Squared distance for speed
     * @param p
     * @return
     */
    public int distanceToSq(final Localizable p) {
        int dist = 0;
        for (int i = 0; i < numDimensions(); ++i) {
            int tmp = (p.getIntPosition(i) - m_coords[i]);
            dist += tmp * tmp;
        }

        return dist;
    }

    /**
     * Calculate distance to a long[]
     *
     * Equivalent to Math.sqrt(distanceToSq(p));
     * @param p
     * @return distance
     */
    public double distanceTo(final long[] l) {
        return Math.sqrt(distanceToSq(l));
    }

    /**
     * Squared distance for speed
     * @param p
     * @return
     */
    public long distanceToSq(final long[] l) {
        long dist = 0;
        for (int i = 0; i < numDimensions(); ++i) {
            long tmp = (l[i] - m_coords[i]);
            dist += tmp * tmp;
        }

        return dist;
    }

    /**
     * {@inheritDoc}
     *
     * TODO: Necessary?
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(m_coords);
        result = prime * result + (m_isMax ? 1231 : 1237);
        return result;
    }

    /**
     * Returns a copy of this point
     * @return
     */
    public AnalyticPoint<T> copy() {
        return new AnalyticPoint<T>(this, this.m_value);
    }

    /**
     * Get the value of this point
     * @return
     */
    public double getValue() {
        return m_value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void localize(final float[] pos) {
        int n = numDimensions();
        for ( int d = 0; d < n; d++ ) {
            pos[ d ] = this.m_coords[ d ];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void localize(final double[] pos) {
        int n = numDimensions();
        for ( int d = 0; d < n; d++ ) {
            pos[ d ] = this.m_coords[ d ];
        }
    }

    /**sition
     * {@inheritDoc}
     */
    @Override
    public float getFloatPosition(final int d) {
        return getIntPosition(d);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDoublePosition(final int d) {
        return getIntPosition(d);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int numDimensions() {
        return m_coords.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void localize(final int[] pos) {
        int n = numDimensions();
        for ( int d = 0; d < n; d++ ) {
            pos[ d ] = this.m_coords[ d ];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void localize(final long[] pos) {
        int n = numDimensions();
        for ( int d = 0; d < n; d++ ) {
            pos[ d ] = this.m_coords[ d ];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIntPosition(final int d) {
        return m_coords[d];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLongPosition(final int d) {
        return getIntPosition(d);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final AnalyticPoint<T> p) {
        return new Double(m_value).compareTo(p.getValue());
    }
}
