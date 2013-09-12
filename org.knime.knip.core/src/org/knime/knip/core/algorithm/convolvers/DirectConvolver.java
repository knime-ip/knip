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
package org.knime.knip.core.algorithm.convolvers;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DirectConvolver<T extends RealType<T>, K extends RealType<K> & NativeType<K>, O extends RealType<O> & NativeType<O>>
        implements Convolver<T, K, O> {

    private RandomAccessibleInterval<K> m_kernel;

    private RandomAccessibleInterval<K>[] m_kernels;

    public DirectConvolver() {
    }

    @Override
    public RandomAccessibleInterval<O> compute(final RandomAccessible<T> input,
                                               final RandomAccessibleInterval<K> kernel,
                                               final RandomAccessibleInterval<O> output) {

        if (kernel != m_kernel) {
            m_kernels = KernelTools.decomposeKernel(kernel);
        }

        if (m_kernels.length > 1) {
            return new DirectIterativeConvolver<T, K, O>().compute(input, m_kernels, output);
        } else {
            long[] min = new long[input.numDimensions()];
            long[] max = new long[input.numDimensions()];

            for (int d = 0; d < kernel.numDimensions(); d++) {
                min[d] = -kernel.dimension(d);
                max[d] = kernel.dimension(d) + output.dimension(d);
            }

            final RandomAccess<T> srcRA = input.randomAccess(new FinalInterval(min, max));

            final Cursor<K> kernelC = Views.iterable(kernel).localizingCursor();

            final Cursor<O> resC = Views.iterable(output).localizingCursor();

            final long[] pos = new long[input.numDimensions()];
            final long[] kernelRadius = new long[kernel.numDimensions()];
            for (int i = 0; i < kernelRadius.length; i++) {
                kernelRadius[i] = kernel.dimension(i) / 2;
            }

            float val;

            while (resC.hasNext()) {
                // image
                resC.fwd();
                resC.localize(pos);

                // kernel inlined version of the method convolve
                val = 0;
                srcRA.setPosition(pos);

                kernelC.reset();
                while (kernelC.hasNext()) {
                    kernelC.fwd();

                    for (int i = 0; i < kernelRadius.length; i++) {
                        if (kernelRadius[i] > 0) { // dimension
                                                   // can
                                                   // have
                                                   // zero
                                                   // extension
                                                   // e.g.
                                                   // vertical
                                                   // 1d
                                                   // kernel
                            srcRA.setPosition(pos[i] + kernelC.getLongPosition(i) - kernelRadius[i], i);
                        }
                    }

                    val += srcRA.get().getRealDouble() * kernelC.get().getRealDouble();
                }

                resC.get().setReal(val);
            }
        }
        return output;
    }

    @Override
    public DirectConvolver<T, K, O> copy() {
        return new DirectConvolver<T, K, O>();
    }

}
