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

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.ops.operation.BinaryObjectFactory;
import net.imglib2.ops.operation.BinaryOperation;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class IterativeConvolver<T extends RealType<T>, K extends RealType<K>, O extends RealType<O>>
        implements MultiKernelConvolver<T, K, O> {

    protected ImgFactory<O> m_factory;

    protected OutOfBoundsFactory<T, RandomAccessibleInterval<T>> m_outOfBoundsFactoryIn;

    protected OutOfBoundsFactory<O, RandomAccessibleInterval<O>> m_outOfBoundsFactoryOut;

    private Convolver<T, K, O> m_baseConvolver;

    private Convolver<O, K, O> m_followerConvolver;

    public IterativeConvolver(final ImgFactory<O> factory,
                              final OutOfBoundsFactory<T, RandomAccessibleInterval<T>> outOfBoundsFactoryIn,
                              final OutOfBoundsFactory<O, RandomAccessibleInterval<O>> outOfBoundsFactoryOut) {
        m_baseConvolver = createBaseConvolver();
        m_followerConvolver = createFollowerConvolver();
        m_factory = factory;
        m_outOfBoundsFactoryIn = outOfBoundsFactoryIn;
        m_outOfBoundsFactoryOut = outOfBoundsFactoryOut;
    }

    public RandomAccessibleInterval<O> compute(final RandomAccessible<T> input,
                                               final RandomAccessibleInterval<K>[] kernels,
                                               final RandomAccessibleInterval<O> output) {
        concat(createBufferFactory(output), m_baseConvolver, m_followerConvolver)
                .compute(Views.extend(Views.interval(input, output), m_outOfBoundsFactoryIn), kernels,
                         Views.interval(Views.extend(output, m_outOfBoundsFactoryOut), output));

        return output;
    };

    /*
     * Some helpers
     */
    protected
            BinaryOperation<RandomAccessible<T>, RandomAccessibleInterval<K>[], RandomAccessibleInterval<O>>
            concat(final BinaryObjectFactory<RandomAccessible<T>, RandomAccessibleInterval<K>, RandomAccessibleInterval<O>> bufferFac,
                   final Convolver<T, K, O> binaryOp1, final Convolver<O, K, O> binaryOp2) {
        return new BinaryOperation<RandomAccessible<T>, RandomAccessibleInterval<K>[], RandomAccessibleInterval<O>>() {

            @Override
            public RandomAccessibleInterval<O> compute(final RandomAccessible<T> inputA,
                                                       final RandomAccessibleInterval<K>[] inputB,
                                                       final RandomAccessibleInterval<O> output) {

                if (inputB.length == 1) {
                    return binaryOp1.compute(inputA, inputB[0], output);
                }

                RandomAccessibleInterval<O> buffer = bufferFac.instantiate(inputA, null);
                RandomAccessibleInterval<O> tmpOutput;
                RandomAccessibleInterval<O> tmpInput;
                RandomAccessibleInterval<O> tmp;
                if (inputB.length % 2 == 1) {
                    tmpOutput = output;
                    tmpInput = buffer;
                } else {
                    tmpOutput = buffer;
                    tmpInput = output;
                }

                binaryOp1.compute(inputA, inputB[0], tmpOutput);

                for (int i = 1; i < inputB.length; i++) {
                    tmp = tmpInput;
                    tmpInput = tmpOutput;
                    tmpOutput = tmp;
                    binaryOp2.compute(tmpInput, inputB[i], tmpOutput);
                }

                return output;
            }

            @Override
            public BinaryOperation<RandomAccessible<T>, RandomAccessibleInterval<K>[], RandomAccessibleInterval<O>>
                    copy() {
                return concat(bufferFac, (Convolver<T, K, O>)binaryOp1.copy(), (Convolver<O, K, O>)binaryOp2.copy());
            }

        };
    }

    protected BinaryObjectFactory<RandomAccessible<T>, RandomAccessibleInterval<K>, RandomAccessibleInterval<O>>
            createBufferFactory(final RandomAccessibleInterval<O> output) {
        return new BinaryObjectFactory<RandomAccessible<T>, RandomAccessibleInterval<K>, RandomAccessibleInterval<O>>() {

            @Override
            public RandomAccessibleInterval<O> instantiate(final RandomAccessible<T> inputA,
                                                           final RandomAccessibleInterval<K> inputB) {
                Img<O> buffer = m_factory.create(output, output.randomAccess().get().createVariable());
                return Views.interval(Views.extend(buffer, m_outOfBoundsFactoryOut), buffer);
            }
        };
    }

    protected abstract Convolver<T, K, O> createBaseConvolver();

    protected abstract Convolver<O, K, O> createFollowerConvolver();

}
