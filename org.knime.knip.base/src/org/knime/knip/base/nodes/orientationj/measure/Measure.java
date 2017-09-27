/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2017
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
package org.knime.knip.base.nodes.orientationj.measure;

import org.scijava.plugin.Parameter;

import net.imagej.ops.Contingent;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * This {@link Op} calculates the energy, orientation and coherency features according to the OrientationJ plugin for
 * ImageJ. <br>
 * <br>
 * This code has been inspired and ported from the <a href="http://bigwww.epfl.ch/demo/orientation/">OrientationJ</a>
 * plugin for ImageJ, written by Daniel Sage.
 *
 * @see <a href="http://bigwww.epfl.ch/demo/orientation/">OrientationJ</a>
 * @see <a href="https://imagej.net/">ImageJ</a>
 *
 * @author Simon Schmid, University of Konstanz, Germany
 * @param <T>
 */
class Measure<T extends RealType<T>> extends AbstractUnaryFunctionOp<RandomAccessibleInterval<T>, double[]>
        implements Contingent {

    @Parameter
    private OpService ops;

    @Override
    public double[] calculate(final RandomAccessibleInterval<T> in) {
        final RandomAccessibleInterval<DoubleType>[] gradients = gradientSpline(in);

        final Cursor<DoubleType> cursorGx = Views.flatIterable(gradients[0]).cursor();
        final RandomAccess<DoubleType> raGy = gradients[1].randomAccess();

        double xy = 0;
        double xx = 0;
        double yy = 0;
        long area = 0;
        while (cursorGx.hasNext()) {
            cursorGx.fwd();
            raGy.setPosition(cursorGx);
            xx += cursorGx.get().getRealDouble() * cursorGx.get().getRealDouble();
            yy += raGy.get().getRealDouble() * raGy.get().getRealDouble();
            xy += cursorGx.get().getRealDouble() * raGy.get().getRealDouble();
            area++;
        }

        xy /= area;
        xx /= area;
        yy /= area;
        double energy = xx + yy;
        double orientation = Math.toDegrees(0.5 * Math.atan2(2.0 * xy, yy - xx));
        double coherency = 0.0;
        if (xx + yy > 1) {
            coherency = Math.sqrt((yy - xx) * (yy - xx) + 4.0 * xy * xy) / (xx + yy + 10E-4);
        }
        return new double[]{energy, orientation, coherency};
    }

    /**
     * Calculates the spline and returns its gradients in x and y direction.
     */
    @SuppressWarnings("unchecked")
    private RandomAccessibleInterval<DoubleType>[] gradientSpline(final RandomAccessibleInterval<T> img) {

        int nx = (int)img.dimension(0);
        int ny = (int)img.dimension(1);
        double rowin[] = new double[nx];
        double rowck[] = new double[nx];

        double colin[] = new double[ny];
        double colck[] = new double[ny];

        double c0 = 6.0;
        double a = Math.sqrt(3.0) - 2.0;
        double sp[] = getQuadraticSpline(0.5);
        double neighbor[] = new double[3];
        double v = 0;

        final long[] dimensions = new long[img.numDimensions()];
        img.dimensions(dimensions);
        final RandomAccessibleInterval<DoubleType> gradientX = ops.create().img(dimensions);
        final RandomAccess<DoubleType> raGx = gradientX.randomAccess();
        final RandomAccessibleInterval<DoubleType> gradientY = ops.create().img(dimensions);
        final RandomAccess<DoubleType> raGy = gradientY.randomAccess();

        for (int y = 0; y < ny; y++) {
            final IntervalView<T> intervalView = new IntervalView<T>(img, new long[]{0, y}, new long[]{nx - 1, y});
            final Cursor<T> cursor = intervalView.cursor();
            for (int i = 0; i < rowin.length; i++) {
                rowin[i] = cursor.next().getRealDouble();
            }
            doSymmetricalExponentialFilter(rowin, rowck, c0, a);
            int x;
            for (x = 2; x < nx - 1; x++) {
                neighbor[0] = rowck[x - 2] - rowck[x - 1];
                neighbor[1] = rowck[x - 1] - rowck[x];
                neighbor[2] = rowck[x] - rowck[x + 1];
                v = neighbor[0] * sp[0] + neighbor[1] * sp[1] + neighbor[2] * sp[2];
                raGx.setPosition(new int[]{x, y, 0});
                raGx.get().setReal(v);
            }
            x = 1;
            neighbor[0] = rowck[1] - rowck[x - 1];
            neighbor[1] = rowck[x - 1] - rowck[x];
            neighbor[2] = rowck[x] - rowck[x + 1];
            v = neighbor[0] * sp[0] + neighbor[1] * sp[1] + neighbor[2] * sp[2];
            raGx.setPosition(new int[]{x, y, 0});
            raGx.get().setReal(v);
        }

        for (int x = 0; x < nx; x++) {
            final IntervalView<T> intervalView = new IntervalView<T>(img, new long[]{x, 0}, new long[]{x, ny - 1});
            final Cursor<T> cursor = intervalView.cursor();
            for (int i = 0; i < colin.length; i++) {
                colin[i] = cursor.next().getRealDouble();
            }
            doSymmetricalExponentialFilter(colin, colck, c0, a);
            int y;
            for (y = 2; y < ny - 1; y++) {
                neighbor[0] = colck[y - 2] - colck[y - 1];
                neighbor[1] = colck[y - 1] - colck[y];
                neighbor[2] = colck[y] - colck[y + 1];
                v = neighbor[0] * sp[0] + neighbor[1] * sp[1] + neighbor[2] * sp[2];
                raGy.setPosition(new int[]{x, y, 0});
                raGy.get().setReal(v);
            }
            y = 1;
            neighbor[0] = colck[1] - colck[y - 1];
            neighbor[1] = colck[y - 1] - colck[y];
            neighbor[2] = colck[y] - colck[y + 1];
            v = neighbor[0] * sp[0] + neighbor[1] * sp[1] + neighbor[2] * sp[2];
            raGy.setPosition(new int[]{x, y, 0});
            raGy.get().setReal(v);
        }

        return new RandomAccessibleInterval[]{gradientX, gradientY};
    }

    private double[] getQuadraticSpline(final double t) {
        double v[] = new double[3];
        v[0] = ((t - 0.5) * (t - 0.5)) / 2.0;
        v[2] = ((t + 0.5) * (t + 0.5)) / 2.0;
        v[1] = 1.0 - v[0] - v[2];
        return v;
    }

    /**
     * Performs the 1D symmetrical exponential filtering.
     */
    private void doSymmetricalExponentialFilter(final double s[], final double c[], final double c0, final double a) {
        int n = s.length;

        double cn[] = new double[n];
        double cp[] = new double[n];

        // causal
        cp[0] = computeInitialValueCausal(s, a);
        for (int k = 1; k < n; k++) {
            cp[k] = s[k] + a * cp[k - 1];
        }

        // anticausal
        cn[n - 1] = computeInitialValueAntiCausal(cp, a);

        for (int k = n - 2; k >= 0; k--) {
            cn[k] = a * (cn[k + 1] - cp[k]);
        }

        // gain
        for (int k = 0; k < n; k++) {
            c[k] = c0 * cn[k];
        }
    }

    /**
     * Returns the initial value for the causal filter using the mirror boundary conditions.
     */
    private double computeInitialValueCausal(final double vals[], final double a) {
        double p = a;
        double v = vals[0];
        for (int k = 1; k < (int)Math.ceil(Math.log(1e-6) / Math.log(Math.abs(a))); k++) {
            v = v + p * vals[k];
            p = p * a;
        }
        return v;
    }

    /**
     * Returns the initial value for the anti-causal filter using the mirror boundary conditions.
     */
    private double computeInitialValueAntiCausal(final double signal[], final double a) {
        return (a / (a * a - 1.0)) * (signal[signal.length - 1] + a * signal[signal.length - 2]);
    }

    @Override
    public boolean conforms() {
        return in().numDimensions() == 2 && in().dimension(0) >= 2 && in().dimension(1) >= 2 && in().dimension(2) == 1;
    }

}
