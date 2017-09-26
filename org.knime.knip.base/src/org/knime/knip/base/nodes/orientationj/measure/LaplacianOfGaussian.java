package org.knime.knip.base.nodes.orientationj.measure;

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

import org.knime.knip.core.KNIPGateway;

import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * This class allows to apply a Laplacian of Gaussian filter onto a {@link RandomAccessibleInterval} with parameter
 * sigma.
 *
 * @author Simon Schmid, University of Konstanz, Germany
 */
final class LaplacianOfGaussian<T extends RealType<T>> {

    /**
     * Apply a Laplacian of Gaussian 2D. Separable implementation.
     *
     * @param input the input
     * @param sigma the sigma
     * @return calculated RAI
     */
    public RandomAccessibleInterval<T> calculate(final RandomAccessibleInterval<T> input, final double sigma) {
        if (input == null) {
            return null;
        }
        if (sigma <= 0) {
            return input;
        }

        final double[][] kernels = createKernelLoG(sigma);

        final OpService ops = KNIPGateway.ops();

        final long[] dimensions = new long[input.numDimensions()];
        input.dimensions(dimensions);
        @SuppressWarnings("unchecked")
        final RandomAccessibleInterval<T> outputX = (RandomAccessibleInterval<T>)ops.create().img(dimensions);
        final RandomAccess<T> raOutputX = outputX.randomAccess();
        @SuppressWarnings("unchecked")
        final RandomAccessibleInterval<T> outputY = (RandomAccessibleInterval<T>)ops.create().img(dimensions);
        final RandomAccess<T> raOutputY = outputY.randomAccess();

        // do for all channels, if image has several
        for (int z = 0; z < (input.numDimensions() > 2 ? input.dimension(2) : 1); z++) {
            // x direction
            for (int x = 0; x < dimensions[0]; x++) {
                final RandomAccessibleInterval<T> intervalView =
                        Views.dropSingletonDimensions(new IntervalView<T>(input, new long[]{x, 0},
                                new long[]{x, dimensions[1] - 1}));

                raOutputX.setPosition(x, 0);
                convolve(intervalView, raOutputX, kernels[0], 1);

                raOutputY.setPosition(x, 0);
                convolve(intervalView, raOutputY, kernels[1], 1);
            }
            // y direction
            for (int y = 0; y < dimensions[1]; y++) {
                final RandomAccessibleInterval<T> intervalViewX =
                        Views.dropSingletonDimensions(new IntervalView<T>(outputX, new long[]{0, y},
                                new long[]{dimensions[0] - 1, y}));
                raOutputX.setPosition(y, 1);
                convolve(intervalViewX, raOutputX, kernels[1], 0);

                RandomAccessibleInterval<T> intervalViewY = Views.dropSingletonDimensions(new IntervalView<T>(outputY,
                        new long[]{0, y}, new long[]{dimensions[0] - 1, y}));
                raOutputY.setPosition(y, 1);
                convolve(intervalViewY, raOutputY, kernels[0], 0);
            }
        }

        final Cursor<T> cursor = Views.flatIterable(outputX).cursor();
        while (cursor.hasNext()) {
            cursor.fwd();
            raOutputY.setPosition(cursor);
            raOutputY.get().setReal(raOutputY.get().getRealDouble() + cursor.get().getRealDouble());
        }

        return outputY;
    }

    private void convolve(final RandomAccessibleInterval<T> in, final RandomAccess<T> raOut, final double kernel[],
                          final int fixedAxis) {
        final RandomAccess<T> raIn = in.randomAccess();
        final int size = (int)in.dimension(0);
        final double inValues[] = new double[size];
        for (int i = 0; i < size; i++) {
            raIn.setPosition(i, 0);
            inValues[i] = raIn.get().getRealDouble();
        }
        int period = (size <= 1 ? 1 : 2 * size - 2);

        int idx;
        for (int i = 0; i < size; i++) {
            double sum = 0.0;
            for (int k = 0; k < kernel.length; k++) {
                idx = i + k - (kernel.length / 2);
                for (; idx < 0; idx += period) {
                    // noop
                }
                for (; idx >= size;) {
                    idx = period - idx;
                    idx = (idx < 0 ? -idx : idx);
                }
                sum += kernel[k] * inValues[idx];
            }
            raOut.setPosition(i, fixedAxis);
            raOut.get().setReal(sum);
        }
    }

    /**
     * Computes two kernels given sigma
     */
    private double[][] createKernelLoG(final double sigma) {
        double s2 = sigma * sigma;
        double dem = 2.0 * s2;
        int size = (int)Math.round(((int)(sigma * 3.0)) * 2.0 + 1); // always odd size
        int size2 = size / 2;
        double[][] kernel = new double[2][size];

        for (int k = 0; k < size; k++) {
            kernel[k][0] = Math.exp(-((k - size2) * (k - size2)) / dem);
        }

        double s4 = s2 * s2;
        double cst = 1.0 / (2 * Math.PI * sigma * sigma);
        for (int k = 0; k < size; k++) {
            final double x = (k - size2) * (k - size2);
            kernel[k][1] = cst * (x / s4 - 1.0 / s2) * Math.exp(-x / dem);
        }
        return kernel;
    }
}