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
package org.knime.knip.core.algorithm.convolvers.filter.linear;

import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.BinaryObjectFactory;
import net.imglib2.ops.operation.BinaryOutputOperation;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Matrix multiplication.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class MatMul<T extends RealType<T> & NativeType<T>> implements
        BinaryOutputOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>, Img<T>> {

    /**
     * {@inheritDoc}
     * 
     * @return
     */
    @Override
    public Img<T> compute(final RandomAccessibleInterval<T> op0, final RandomAccessibleInterval<T> op1, final Img<T> r) {
        checkContraints(op0, op1);

        // perform matrix multiplication
        RandomAccess2D<T> ra1 = new RandomAccess2D<T>(op0);
        RandomAccess2D<T> ra2 = new RandomAccess2D<T>(op1);

        RandomAccess2D<T> raRes = new RandomAccess2D<T>(r);

        for (int x = 0; x < op0.dimension(0); x++) {
            for (int y = 0; y < op1.dimension(1); y++) {
                T res = raRes.get(x, y);
                for (int i = 0; i < op0.dimension(1); i++) {
                    res.setReal(res.getRealDouble() + ra1.get(x, i).getRealDouble() * ra2.get(i, y).getRealDouble());

                }
            }
        }
        return r;
    }

    private void checkContraints(final Interval op0, final Interval op1) {
        if (op0.numDimensions() != 2 || op1.numDimensions() != 2) {
            throw new IllegalArgumentException("Matrix multiplication only suitable for 2D images.");
        }
        if (op0.dimension(1) != op1.dimension(0)) {
            throw new IllegalArgumentException(
                    "Dimensions of images doesn't fit for matrix multiplication: img1.dimY != img2.dimX");
        }
    }

    /**
     * 
     * @author hornm, University of Konstanz
     */
    private class RandomAccess2D<TT extends RealType<TT>> {

        private final RandomAccess<TT> m_ra;

        public RandomAccess2D(final RandomAccessibleInterval<TT> i) {
            m_ra = i.randomAccess();
        }

        public TT get(final int row, final int col) {
            m_ra.setPosition(row, 0);
            m_ra.setPosition(col, 1);
            return m_ra.get();
        }

    }

    @Override
    public BinaryOutputOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>, Img<T>> copy() {
        return new MatMul<T>();
    }

    @Override
    public BinaryObjectFactory<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>, Img<T>> bufferFactory() {
        return new BinaryObjectFactory<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>, Img<T>>() {

            @Override
            public Img<T>
                    instantiate(final RandomAccessibleInterval<T> inputA, final RandomAccessibleInterval<T> inputB) {
                checkContraints(inputA, inputB);
                Img<T> res =
                        new ArrayImgFactory<T>().create(new long[]{inputA.dimension(0), inputB.dimension(1)}, inputB
                                .randomAccess().get().createVariable());
                return res;
            }
        };
    }
}
