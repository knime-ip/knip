package org.knime.knip.core.ops.interval;

import java.util.ArrayList;

/**
 * AnalyticsPoint is a Utility class for MaximumFinder. It stores basic maximum and value information for a point.
 * @author squareys
 */
public class AnalyticPoint implements Comparable<Object> {

    private double m_colValue;

    private int[] m_coords;

    private boolean m_processed = false;

    private boolean m_analyzed = false;

    private boolean m_isMax = false;

    /**
     * Default constructor of AnalyticPoint. Sets all to default false or null/0
     */
    public AnalyticPoint() {
        m_colValue = 0;
        m_coords = null;
    }

    /**
     * Initialize the AnalyticPoint with position (coord) and a value
     * @param coords the coordinations/position of the point
     * @param val value of the point
     */
    public AnalyticPoint(final int[] coords, final double val) {
        m_colValue = val;
        m_coords = coords;
    }

    /**
     * Set whether this point is concidered a maximum
     * @param bool
     */
    public void setMax(final boolean bool) {
        m_isMax = bool;
    }

    /**
     * Check whether this point is a maximum
     * @return
     */
    public boolean isMax() {
        return m_isMax;
    }

    /**
     * Set whether this point was processed
     * @param bool
     */
    public void setProcessed(final boolean bool) {
        m_processed = bool;
    }

    /**
     * Check whether this point was processed or not
     * @return
     */
    public boolean isProcessed() {
        return m_processed;
    }

    /**
     * Set whether this point was analyzed
     * @param bool
     */
    public void setAnalyzed(final boolean bool) {
        m_analyzed = bool;
    }

    /**
     * Check whether this point was analyzed
     * @return
     */
    public boolean isAnalyzed() {
        return m_analyzed;
    }

    /**
     * Get the position of this point
     * @return
     */
    public int[] getPosition() {
        return m_coords;
    }

    /**
     * @return value of the point
     */
    public double getValue() {
        return m_colValue;
    }


    @Override
    public int compareTo(final Object p) {
        if (p instanceof AnalyticPoint) {
            return (int)((((AnalyticPoint)p).getValue() - m_colValue) * 255);
        } else {
            return 0;
        }
    }

    /**
     * Calculate distance from this point to another
     * @param p
     * @return
     */
    public double distanceTo(final AnalyticPoint p) {
        int[] pPos = p.getPosition();
        int[] qPos = this.getPosition();

        double dist = 0;
        for (int i = 0; i < pPos.length && i < qPos.length; ++i) {
            dist += (double)(pPos[i] - qPos[i]) * (double)(pPos[i] - qPos[i]);
        }

        return Math.abs(Math.sqrt(dist));
    }

    @Override
    public boolean equals(final Object p) {
        if (p instanceof AnalyticPoint) {
            boolean eq = true;
            for (int i = 0; i < m_coords.length; ++i) {
                if (((AnalyticPoint)p).getPosition()[i] == m_coords[i]) {

                } else {
                    eq = false;
                }
            }
            return eq;
        } else if (p instanceof long[]) {
            boolean eq = true;
            for (int i = 0; i < m_coords.length; ++i) {
                if (((long[])p)[i] == m_coords[i]) {

                } else {
                    eq = false;
                }
            }
            return eq;
        } else {
            return false;
        }
    }

    /**
     * @param dim
     * @return
     */
    public int hashCode(final long[] dim) {
        int ret = 0;
        long[] r = new long[dim.length];
        for (int i = 0; i < dim.length - 1; ++i) {
            r[i] = dim[i];
            for (int j = i + 1; j < dim.length; ++j) {
                r[i] *= dim[j];
            }
            ret += r[i] * m_coords[i];
        }
        ret += m_coords[dim.length - 1];
        return ret;
    }

    /**
     * @return
     */
    public AnalyticPoint copy() {
        return new AnalyticPoint(this.m_coords, this.m_colValue);
    }

    /**
     * @param mover
     * @return
     */
    public ArrayList<int[]> getNeighborPositions(final long[][] mover) {
        ArrayList<int[]> retList = new ArrayList<int[]>();

        int[] pos = new int[m_coords.length];
        pos = m_coords.clone();
        for (int j = 0; j < mover.length; ++j) {
            for (int i = 0; i < m_coords.length; ++i) {
                pos[i] += (int)mover[j][i];
            }
            retList.add(pos.clone());
        }

        return retList;
    }

}
