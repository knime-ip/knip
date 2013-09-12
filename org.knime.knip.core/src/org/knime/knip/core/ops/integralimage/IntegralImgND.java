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
package org.knime.knip.core.ops.integralimage;

/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2012 Stephan Preibisch, Stephan Saalfeld, Tobias
 * Pietzsch, Albert Cardona, Barry DeZonia, Curtis Rueden, Lee Kamentsky, Larry
 * Lindsey, Johannes Schindelin, Christian Dietz, Grant Harris, Jean-Yves
 * Tinevez, Steffen Jaensch, Mark Longair, Nick Perry, and Jan Funke.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImgFactory;
import net.imglib2.iterator.LocalizingZeroMinIntervalIterator;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.Unsigned12BitType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

/*
 * original authors
 *
 *
 * - added associated SumAgent object and the getSumAgent method
 * - //TODO make integral image same size as input image and use ExtendedRandomAccess with 0 border instead
 */

/**
 * n-dimensional integral image that stores sums using type {@param <T>}. Care must be taken that sums do not overflow
 * the capacity of type {@param <T>}.
 * 
 * The integral image will be one pixel larger in each dimension as for easy computation of sums it has to contain
 * "zeros" at the beginning of each dimension. User {@link #bufferFactory()} to create an appropriate image.
 * 
 * Sums are done with the precision of {@param <T>} and then set to the integral image type, which may crop the values
 * according to the type's capabilities.
 * 
 * @param <R> The type of the input image.
 * @param <T> The type of the integral image.
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Stephan Preibisch
 * @author Albert Cardona
 */
public class IntegralImgND<R extends RealType<R>, T extends RealType<T> & NativeType<T>> implements
        UnaryOutputOperation<RandomAccessibleInterval<R>, RandomAccessibleInterval<T>> {

    private final ImgFactory<T> m_factory;

    private T m_type;

    public IntegralImgND(final ImgFactory<T> factory, final T type) {
        m_factory = factory;
        m_type = type;
    }

    public IntegralImgND(final ImgFactory<T> factory) {
        m_factory = factory;
        m_type = null;
    }

    @Override
    public UnaryObjectFactory<RandomAccessibleInterval<R>, RandomAccessibleInterval<T>> bufferFactory() {
        return new UnaryObjectFactory<RandomAccessibleInterval<R>, RandomAccessibleInterval<T>>() {

            @SuppressWarnings("unchecked")
            @Override
            public RandomAccessibleInterval<T> instantiate(final RandomAccessibleInterval<R> input) {

                final R probe = input.randomAccess().get().createVariable();
                if (m_type == null) {
                    if ((probe instanceof LongType) || (probe instanceof Unsigned12BitType)) {
                        m_type = (T)new LongType();
                    } else if (probe instanceof IntegerType) {
                        m_type = (T)new IntType();
                    } else {
                        m_type = (T)new DoubleType();
                    }
                }

                // the size of the first dimension is changed
                final int numDimensions = input.numDimensions();
                final long integralSize[] = new long[numDimensions];

                for (int d = 0; d < numDimensions; ++d) {
                    integralSize[d] = input.dimension(d) + 1;
                }

                return m_factory.create(integralSize, m_type);
            }
        };
    }

    @Override
    public UnaryOutputOperation<RandomAccessibleInterval<R>, RandomAccessibleInterval<T>> copy() {
        return new IntegralImgND<R, T>(m_factory, m_type);
    }

    @Override
    public RandomAccessibleInterval<T> compute(final RandomAccessibleInterval<R> input,
                                               final RandomAccessibleInterval<T> output) {

        // the following methods alter output
        if (output.numDimensions() == 1) {
            process_1D(input, output);
        } else {
            process_nD_initialDimension(input, output);
            process_nD_remainingDimensions(input, output);
        }

        // iterate a 2nd time over the input for error testing
        // this is expensive but we have to ensure that the
        // bounds of the output type
        // are big enough.
        double errorSum = 0.0d;

        final Cursor<R> inputCursor = Views.iterable(input).cursor();
        while (inputCursor.hasNext()) {
            errorSum += inputCursor.next().getRealDouble();
        }

        if ((errorSum > output.randomAccess().get().createVariable().getMaxValue()) || Double.isInfinite(errorSum)) {
            throw new RuntimeException(new IncompatibleTypeException(output,
                    "Integral image breaks type boundaries of the output image. (max value of " + errorSum
                            + " is too much)"));
        }

        return output;
    }

    public void process_1D(final RandomAccessibleInterval<R> input, final RandomAccessibleInterval<T> output) {
        final T tmpVar = output.randomAccess().get().createVariable();
        final T sum = output.randomAccess().get().createVariable();

        // the size of dimension 0
        final long size = output.dimension(0);

        final RandomAccess<R> cursorIn = input.randomAccess();
        final RandomAccess<T> cursorOut = output.randomAccess();

        cursorIn.setPosition(0, 0);
        cursorOut.setPosition(1, 0);

        // compute the first pixel
        sum.setReal(cursorIn.get().getRealDouble());
        cursorOut.get().set(sum);

        for (long i = 2; i < size; ++i) {
            cursorIn.fwd(0);
            cursorOut.fwd(0);

            tmpVar.setReal(cursorIn.get().getRealDouble());
            sum.add(tmpVar);
            cursorOut.get().set(sum);
        }
    }

    private void process_nD_initialDimension(final RandomAccessibleInterval<R> input,
                                             final RandomAccessibleInterval<T> output) {

        final int numDimensions = output.numDimensions();
        final long[] fakeSize = new long[numDimensions - 1];

        // location for the input location
        final long[] tmpIn = new long[numDimensions];

        // location for the integral location
        final long[] tmpOut = new long[numDimensions];

        // the size of dimension 0
        final long size = output.dimension(0);

        for (int d = 1; d < numDimensions; ++d) {
            fakeSize[d - 1] = output.dimension(d);
        }

        final LocalizingZeroMinIntervalIterator cursorDim = new LocalizingZeroMinIntervalIterator(fakeSize);

        final RandomAccess<R> cursorIn = input.randomAccess();
        final RandomAccess<T> cursorOut = output.randomAccess();

        final T tmpVar = output.randomAccess().get().createVariable();
        final T sum = output.randomAccess().get().createVariable();

        // iterate over all dimensions except the one we are computing
        // the
        // integral in, which is dim=0 here
        main: while (cursorDim.hasNext()) {
            cursorDim.fwd();

            // get all dimensions except the one we are currently
            // doing the
            // integral on
            cursorDim.localize(fakeSize);

            tmpIn[0] = 0;
            tmpOut[0] = 1;

            for (int d = 1; d < numDimensions; ++d) {
                tmpIn[d] = fakeSize[d - 1] - 1;
                tmpOut[d] = fakeSize[d - 1];

                // all entries of position 0 are 0
                if (tmpOut[d] == 0) {
                    continue main;
                }
            }

            // set the cursor to the beginning of the correct line
            cursorIn.setPosition(tmpIn);

            // set the cursor in the integral image to the right
            // position
            cursorOut.setPosition(tmpOut);

            // integrate over the line
            integrateLineDim0(cursorIn, cursorOut, sum, tmpVar, size);
        }
    }

    private void process_nD_remainingDimensions(final RandomAccessibleInterval<R> input,
                                                final RandomAccessibleInterval<T> output) {

        final int numDimensions = output.numDimensions();

        for (int d = 1; d < numDimensions; ++d) {
            final long[] fakeSize = new long[numDimensions - 1];
            final long[] tmp = new long[numDimensions];

            // the size of dimension d
            final long size = output.dimension(d);

            // get all dimensions except the one we are currently
            // doing the
            // integral on
            int countDim = 0;
            for (int e = 0; e < numDimensions; ++e) {
                if (e != d) {
                    fakeSize[countDim++] = output.dimension(e);
                }
            }

            final LocalizingZeroMinIntervalIterator cursorDim = new LocalizingZeroMinIntervalIterator(fakeSize);

            final RandomAccess<T> cursor = output.randomAccess();
            final T sum = output.randomAccess().get().createVariable();

            while (cursorDim.hasNext()) {
                cursorDim.fwd();

                // get all dimensions except the one we are
                // currently doing the
                // integral on
                cursorDim.localize(fakeSize);

                tmp[d] = 1;
                countDim = 0;
                for (int e = 0; e < numDimensions; ++e) {
                    if (e != d) {
                        tmp[e] = fakeSize[countDim++];
                    }
                }

                // update the cursor in the input image to the
                // current dimension
                // position
                cursor.setPosition(tmp);

                // sum up line
                integrateLine(d, cursor, sum, size);
            }
        }
    }

    protected void integrateLineDim0(final RandomAccess<R> cursorIn, final RandomAccess<T> cursorOut, final T sum,
                                     final T tmpVar, final long size) {
        // compute the first pixel
        sum.setReal(cursorIn.get().getRealDouble());
        cursorOut.get().set(sum);

        for (long i = 2; i < size; ++i) {
            cursorIn.fwd(0);
            cursorOut.fwd(0);

            tmpVar.setReal(cursorIn.get().getRealDouble());
            sum.add(tmpVar);
            cursorOut.get().set(sum);
        }
    }

    protected void integrateLine(final int d, final RandomAccess<T> cursor, final T sum, final long size) {
        // init sum on first pixel that is not zero
        sum.set(cursor.get());

        for (long i = 2; i < size; ++i) {
            cursor.fwd(d);

            sum.add(cursor.get());
            cursor.get().set(sum);
        }
    }

    // convenience method
    /**
     * 
     * @param integralImage
     * @return a helper object that efficiently computes sums on the provided integral image.
     */
    public final static <T extends RealType<T>> IntegralImgSumAgent<T>
            getSumAgent(final RandomAccessibleInterval<T> integralImage) {
        return new IntegralImgSumAgent<T>(integralImage);
    }

}
