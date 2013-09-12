package org.knime.knip.core.ops.interval;

import java.util.ArrayList;

public class AnalyticPoint implements Comparable {

    private double m_colValue;

    private int[] m_coords;

    private boolean m_processed = false;

    private boolean m_analyzed = false;

    private boolean m_isMax = false;

    public AnalyticPoint() {
        m_colValue = 0;
        m_coords = null;
    }

    public AnalyticPoint(int[] coords, double val) {
        m_colValue = val;
        m_coords = coords;
    }

    public void setMax(boolean bool) {
        m_isMax = bool;
    }

    public void setProcessed(boolean bool) {
        m_processed = bool;
    }

    public boolean isProcessed() {
        return m_processed;
    }

    public void setAnalyzed(boolean bool) {
        m_analyzed = bool;
    }

    public boolean isAnalyzed() {
        return m_analyzed;
    }

    public boolean isMax() {
        return m_isMax;
    }

    public int[] getPosition() {
        return m_coords;
    }

    public int compareTo(Object p) {
        if (p instanceof AnalyticPoint) {
            return (int)((((AnalyticPoint)p).getValue() - m_colValue) * 255);
        } else {
            return 0;
        }
    }

    public double distanceTo(AnalyticPoint p) {
        int[] pPos = p.getPosition();
        int[] qPos = this.getPosition();

        double dist = 0;
        for (int i = 0; i < pPos.length && i < qPos.length; ++i) {
            dist += (double)(pPos[i] - qPos[i]) * (double)(pPos[i] - qPos[i]);
        }

        return Math.abs(Math.sqrt((double)dist));
    }

    public boolean equals(Object p) {
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

    public int hashCode(long[] dim) {
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

    public AnalyticPoint copy() {
        return new AnalyticPoint(this.m_coords, this.m_colValue);
    }

    public ArrayList<int[]> getNeighborPositions(long[][] mover) {
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

    public double getValue() {
        return m_colValue;
    }
}
