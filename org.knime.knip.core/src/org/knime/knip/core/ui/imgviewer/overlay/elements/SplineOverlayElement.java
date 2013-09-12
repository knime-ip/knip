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
package org.knime.knip.core.ui.imgviewer.overlay.elements;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.imglib2.RealPoint;
import net.imglib2.roi.PolygonRegionOfInterest;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SplineOverlayElement<L extends Comparable<L>> extends AbstractPolygonOverlayElement<L> {
    private static final int SPLINE_STEPS = 12;

    private Polygon m_tmpPoly;

    public SplineOverlayElement() {
        super();
        m_tmpPoly = new Polygon();
    }

    public SplineOverlayElement(final long[] planePos, final int[] orientation, final String... overlayLabels) {
        super(planePos, orientation, overlayLabels);
        m_tmpPoly = new Polygon();
    }

    @Override
    public boolean add(final long x, final long y) {
        if (m_isClosed) {
            return false;
        } else {
            m_tmpPoly.addPoint((int)x, (int)y);
        }

        updateSpline();
        return true;
    }

    @Override
    public void close() {
        m_isClosed = true;
        updateSpline();
    }

    @SuppressWarnings("rawtypes")
    protected void updateSpline() {
        if (m_tmpPoly.npoints >= 2) {
            Cubic[] X;
            Cubic[] Y;
            if (m_isClosed) {
                X = calcClosedNaturalCubic(m_tmpPoly.npoints - 1, m_tmpPoly.xpoints);
                Y = calcClosedNaturalCubic(m_tmpPoly.npoints - 1, m_tmpPoly.ypoints);
            } else {
                X = calcNaturalCubic(m_tmpPoly.npoints - 1, m_tmpPoly.xpoints);
                Y = calcNaturalCubic(m_tmpPoly.npoints - 1, m_tmpPoly.ypoints);
            }

            /*
             * very crude technique - just break each segment up
             * into steps lines
             */
            m_poly = new Polygon();
            m_poly.addPoint(Math.round(X[0].eval(0)), Math.round(Y[0].eval(0)));
            m_roi = new PolygonRegionOfInterest();
            m_roi.addVertex(0, new RealPoint((double)Math.round(X[0].eval(0)), Math.round(Y[0].eval(0))));
            int idx = 1;
            for (int i = 0; i < X.length; i++) {
                for (int j = 1; j <= SPLINE_STEPS; j++) {
                    final float u = j / (float)SPLINE_STEPS;
                    m_poly.addPoint(Math.round(X[i].eval(u)), Math.round(Y[i].eval(u)));
                    m_roi.addVertex(idx++, new RealPoint((double)Math.round(X[i].eval(u)), Math.round(Y[i].eval(u))));
                }
            }
        } else {
            m_poly = m_tmpPoly;
        }
    }

    @Override
    public void translate(final int selectedIndex, final long x, final long y) {
        if (selectedIndex < 0) {
            return;
        }

        m_tmpPoly.xpoints[selectedIndex] += x;
        m_tmpPoly.ypoints[selectedIndex] += y;

        m_tmpPoly.invalidate();
        updateSpline();

    }

    @Override
    public void translate(final long x, final long y) {

        for (int i = 0; i < m_tmpPoly.npoints; i++) {
            m_tmpPoly.xpoints[i] += x;
            m_tmpPoly.ypoints[i] += y;
        }

        m_tmpPoly.invalidate();
        updateSpline();
    }

    @Override
    protected void renderPointInterior(final Graphics2D g) {
        for (int i = 0; i < m_tmpPoly.npoints; i++) {
            g.fillOval(m_tmpPoly.xpoints[i] - DRAWING_RADIUS, m_tmpPoly.ypoints[i] - DRAWING_RADIUS,
                       2 * DRAWING_RADIUS, 2 * DRAWING_RADIUS);
        }

    }

    @Override
    protected void renderPointOutline(final Graphics2D g) {
        for (int i = 0; i < m_tmpPoly.npoints; i++) {
            g.drawOval(m_tmpPoly.xpoints[i] - DRAWING_RADIUS, m_tmpPoly.ypoints[i] - DRAWING_RADIUS,
                       2 * DRAWING_RADIUS, 2 * DRAWING_RADIUS);
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        m_tmpPoly = (Polygon)in.readObject();
        updateSpline();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);

        out.writeObject(m_tmpPoly);
    }

    /*
     * ###############################################################
     * ################################# UTILITIES ###################
     * ###############################################################
     */

    /*
     * calculates the natural cubic spline that interpolates y[0], y[1], ...
     * y[n] The first segment is returned as C[0].a + C[0].b*u + C[0].c*u^2
     * + C[0].d*u^3 0<=u <1 the other segments are in C[1], C[2], ... C[n-1]
     *
     * source: http://www.cse.unsw.edu.au/~lambert/splines/
     */

    private Cubic[] calcNaturalCubic(final int n, final int[] x) {
        final float[] gamma = new float[n + 1];
        final float[] delta = new float[n + 1];
        final float[] D = new float[n + 1];
        int i;
        /*
         * We solve the equation [2 1 ] [D[0]] [3(x[1] - x[0]) ] |1 4 1
         * | |D[1]| |3(x[2] - x[0]) | | 1 4 1 | | . | = | . | | ..... |
         * | . | | . | | 1 4 1| | . | |3(x[n] - x[n-2])| [ 1 2] [D[n]]
         * [3(x[n] - x[n-1])]
         *
         * by using row operations to convert the matrix to upper
         * triangular and then back sustitution. The D[i] are the
         * derivatives at the knots.
         */

        gamma[0] = 1.0f / 2.0f;
        for (i = 1; i < n; i++) {
            gamma[i] = 1 / (4 - gamma[i - 1]);
        }
        gamma[n] = 1 / (2 - gamma[n - 1]);

        delta[0] = 3 * (x[1] - x[0]) * gamma[0];
        for (i = 1; i < n; i++) {
            delta[i] = ((3 * (x[i + 1] - x[i - 1])) - delta[i - 1]) * gamma[i];
        }
        delta[n] = ((3 * (x[n] - x[n - 1])) - delta[n - 1]) * gamma[n];

        D[n] = delta[n];
        for (i = n - 1; i >= 0; i--) {
            D[i] = delta[i] - (gamma[i] * D[i + 1]);
        }

        /* now compute the coefficients of the cubics */
        final Cubic[] C = new Cubic[n];
        for (i = 0; i < n; i++) {
            C[i] =
                    new Cubic(x[i], D[i], (3 * (x[i + 1] - x[i])) - (2 * D[i]) - D[i + 1], (2 * (x[i] - x[i + 1]))
                            + D[i] + D[i + 1]);
        }
        return C;
    }

    /*
     * calculates the closed natural cubic spline that interpolates x[0],
     * x[1], ... x[n] The first segment is returned as C[0].a + C[0].b*u +
     * C[0].c*u^2 + C[0].d*u^3 0<=u <1 the other segments are in C[1], C[2],
     * ... C[n]
     *
     * source: http://www.cse.unsw.edu.au/~lambert/splines/
     */

    private Cubic[] calcClosedNaturalCubic(final int n, final int[] x) {
        final float[] w = new float[n + 1];
        final float[] v = new float[n + 1];
        final float[] y = new float[n + 1];
        final float[] D = new float[n + 1];
        float z, F, G, H;
        int k;
        /*
         * We solve the equation [4 1 1] [D[0]] [3(x[1] - x[n]) ] |1 4 1
         * | |D[1]| |3(x[2] - x[0]) | | 1 4 1 | | . | = | . | | ..... |
         * | . | | . | | 1 4 1| | . | |3(x[n] - x[n-2])| [1 1 4] [D[n]]
         * [3(x[0] - x[n-1])]
         *
         * by decomposing the matrix into upper triangular and lower
         * matrices and then back sustitution. See Spath "Spline
         * Algorithms for Curves and Surfaces" pp 19--21. The D[i] are
         * the derivatives at the knots.
         */
        w[1] = v[1] = z = 1.0f / 4.0f;
        y[0] = z * 3 * (x[1] - x[n]);
        H = 4;
        F = 3 * (x[0] - x[n - 1]);
        G = 1;
        for (k = 1; k < n; k++) {
            v[k + 1] = z = 1 / (4 - v[k]);
            w[k + 1] = -z * w[k];
            y[k] = z * ((3 * (x[k + 1] - x[k - 1])) - y[k - 1]);
            H = H - (G * w[k]);
            F = F - (G * y[k - 1]);
            G = -v[k] * G;
        }
        H = H - ((G + 1) * (v[n] + w[n]));
        y[n] = F - ((G + 1) * y[n - 1]);

        D[n] = y[n] / H;
        D[n - 1] = y[n - 1] - ((v[n] + w[n]) * D[n]); /*
                                                      * This equation is
                                                      * WRONG! in my copy
                                                      * of Spath
                                                      */
        for (k = n - 2; k >= 0; k--) {
            D[k] = y[k] - (v[k + 1] * D[k + 1]) - (w[k + 1] * D[n]);
        }

        /* now compute the coefficients of the cubics */
        final Cubic[] C = new Cubic[n + 1];
        for (k = 0; k < n; k++) {
            C[k] =
                    new Cubic(x[k], D[k], (3 * (x[k + 1] - x[k])) - (2 * D[k]) - D[k + 1], (2 * (x[k] - x[k + 1]))
                            + D[k] + D[k + 1]);
        }
        C[n] = new Cubic(x[n], D[n], (3 * (x[0] - x[n])) - (2 * D[n]) - D[0], (2 * (x[n] - x[0])) + D[n] + D[0]);
        return C;
    }

    /*
     * calculates the natural cubic spline that interpolates y[0], y[1], ...
     * y[n] The first segment is returned as C[0].a + C[0].b*u + C[0].c*u^2
     * + C[0].d*u^3 0<=u <1 the other segments are in C[1], C[2], ... C[n-1]
     *
     * source: http://www.cse.unsw.edu.au/~lambert/splines/
     */

    /* this class represents a cubic polynomial */
    private class Cubic<OverlayClass> {

        private float m_a, m_b, m_c, m_d; /* a + b*u + c*u^2 +d*u^3 */

        public Cubic(final float a, final float b, final float c, final float d) {
            this.m_a = a;
            this.m_b = b;
            this.m_c = c;
            this.m_d = d;
        }

        /** evaluate cubic */
        public float eval(final float u) {
            return (((((m_d * u) + m_c) * u) + m_b) * u) + m_a;
        }
    }

    @Override
    public boolean containsPoint(final long x, final long y) {
        return m_poly.contains(x, y);
    }

}
