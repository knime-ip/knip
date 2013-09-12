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

import java.util.Arrays;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.apache.commons.math3.linear.AbstractRealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

/**
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class KernelTools {

    public static <K extends RealType<K>> Img<K> adjustKernelDimensions(final int numResDimensions,
                                                                        final int[] kernelDims, final Img<K> kernel) {

        if (kernelDims.length > kernel.numDimensions()) {
            throw new IllegalStateException(
                    "Number of selected dimensions greater than KERNEL dimensions in KernelTools.");
        }

        if (kernelDims.length > numResDimensions) {
            throw new IllegalStateException(
                    "Number of selected dimensions greater than result dimensions in KernelTools.");
        }

        if (kernelDims.length == numResDimensions) {
            return kernel;
        }

        RandomAccessible<K> res = kernel;

        for (int d = kernel.numDimensions(); d < numResDimensions; d++) {
            res = Views.addDimension(res);
        }

        long[] max = new long[numResDimensions];
        for (int d = 0; d < kernel.numDimensions(); d++) {
            max[d] = kernel.max(d);
        }

        long[] resDims = new long[max.length];
        Arrays.fill(resDims, 1);
        for (int d = 0; d < kernelDims.length; d++) {
            res = Views.permute(res, d, kernelDims[d]);
            resDims[kernelDims[d]] = kernel.dimension(d);
        }

        return new ImgView<K>(Views.interval(res, new FinalInterval(resDims)), kernel.factory());
    }

    private static <K extends RealType<K>, KERNEL extends RandomAccessibleInterval<K>> SingularValueDecomposition
            isDecomposable(final KERNEL kernel) {

        if (kernel.numDimensions() != 2) {
            return null;
        }

        final RealMatrix mKernel = new ImgBasedRealMatrix<K>(kernel);

        final SingularValueDecomposition svd = new SingularValueDecomposition(mKernel);

        if (svd.getRank() > 1) {
            return null;
        }

        return svd;

    }

    @SuppressWarnings("unchecked")
    public static <K extends RealType<K> & NativeType<K>> RandomAccessibleInterval<K>[]
            decomposeKernel(final RandomAccessibleInterval<K> kernel) {

        SingularValueDecomposition svd = isDecomposable(SubsetOperations.subsetview(kernel, kernel));

        if (svd != null) {
            int tmp = 0;
            for (int d = 0; d < kernel.numDimensions(); d++) {
                if (kernel.dimension(d) > 1) {
                    tmp++;
                }
            }
            int[] kernelDims = new int[tmp];
            tmp = 0;
            for (int d = 0; d < kernel.numDimensions(); d++) {
                if (kernel.dimension(d) > 1) {
                    kernelDims[tmp++] = d;
                }
            }

            final RealVector v = svd.getV().getColumnVector(0);
            final RealVector u = svd.getU().getColumnVector(0);
            final double s = -Math.sqrt(svd.getS().getEntry(0, 0));
            v.mapMultiplyToSelf(s);
            u.mapMultiplyToSelf(s);

            K type = kernel.randomAccess().get().createVariable();

            RandomAccessibleInterval<K>[] decomposed = new RandomAccessibleInterval[2];

            decomposed[0] =
                    KernelTools.adjustKernelDimensions(kernel.numDimensions(), new int[]{kernelDims[0]},
                                                       KernelTools.vectorToImage(v, type, 1, new ArrayImgFactory<K>()));
            decomposed[1] =
                    KernelTools.adjustKernelDimensions(kernel.numDimensions(), new int[]{kernelDims[1]},
                                                       KernelTools.vectorToImage(u, type, 1, new ArrayImgFactory<K>()));
            return decomposed;

        } else {
            return new RandomAccessibleInterval[]{kernel};
        }

    }

    /**
     * Creates a numDims dimensions image where all dimensions d<numDims are of size 1 and the last dimensions contains
     * the vector.
     * 
     * @param <R>
     * @param ar
     * @param type
     * @param numDims number of dimensions
     * @return
     */
    public static <R extends RealType<R>> Img<R> vectorToImage(final RealVector ar, final R type, final int numDims,
                                                               final ImgFactory<R> fac) {
        long[] dims = new long[numDims];

        for (int i = 0; i < dims.length - 1; i++) {
            dims[i] = 1;
        }
        dims[dims.length - 1] = ar.getDimension();
        Img<R> res = fac.create(dims, type);
        Cursor<R> c = res.cursor();
        while (c.hasNext()) {
            c.fwd();
            c.get().setReal(ar.getEntry(c.getIntPosition(numDims - 1)));
        }

        return res;
    }

    /**
     * 
     * @param <R>
     * @param img A two dimensional image.
     * @return
     */
    public static <R extends RealType<R>> RealMatrix toMatrix(final Img<R> img) {

        assert img.numDimensions() == 2;
        RealMatrix ret = new BlockRealMatrix((int)img.dimension(0), (int)img.dimension(1));
        Cursor<R> c = img.cursor();
        while (c.hasNext()) {
            c.fwd();
            ret.setEntry(c.getIntPosition(0), c.getIntPosition(1), c.get().getRealDouble());
        }
        return ret;
    }

    private static class ImgBasedRealMatrix<TT extends RealType<TT>> extends AbstractRealMatrix {

        private final RandomAccess<TT> m_rndAccess;

        private final RandomAccessibleInterval<TT> m_in;

        public ImgBasedRealMatrix(final RandomAccessibleInterval<TT> in) {
            if (in.numDimensions() != 2) {
                throw new IllegalArgumentException("In must have exact two dimensions to be handled as a matrix");
            }
            m_in = in;
            m_rndAccess = in.randomAccess();
        }

        @Override
        public RealMatrix createMatrix(final int rowDimension, final int columnDimension) {
            return new Array2DRowRealMatrix(rowDimension, columnDimension);
        }

        @Override
        public RealMatrix copy() {
            throw new UnsupportedOperationException("Unsupported");
        }

        @Override
        public double getEntry(final int row, final int column) {
            m_rndAccess.setPosition(row, 1);
            m_rndAccess.setPosition(column, 0);

            return m_rndAccess.get().getRealDouble();
        }

        @Override
        public void setEntry(final int row, final int column, final double value) {
            m_rndAccess.setPosition(row, 1);
            m_rndAccess.setPosition(column, 0);
            m_rndAccess.get().setReal(value);
        }

        @Override
        public int getRowDimension() {
            return (int)m_in.dimension(0);
        }

        @Override
        public int getColumnDimension() {
            return (int)m_in.dimension(1);
        }

    }
}
