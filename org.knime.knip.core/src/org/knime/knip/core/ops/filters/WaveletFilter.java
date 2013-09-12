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
package org.knime.knip.core.ops.filters;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.roi.RectangleRegionOfInterest;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * Image projection.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author jmetzner, University of Konstanz
 */
public class WaveletFilter<T extends RealType<T>, K extends IterableInterval<T> & RandomAccessibleInterval<T>>
        implements UnaryOperation<K, K> {

    public final static int MIN_DIMS = 1;

    public final static int MAX_DIMS = 3;

    private final ExecutorService m_executor;

    private final double m_lambdaMin;

    private final double m_lambdaMax;

    private final double m_ignorePercent;

    /* Inital ROI */
    private RectangleRegionOfInterest m_selectedRowRoi;

    /* Inital origin of the sliding window */
    private double[] m_selectedRowRoiOrigin;

    /* Extend of the sliding window */
    private double[] m_selectedRowRoiExtend;

    /* Region of interest SrcCur */
    private Cursor<T> m_selectedRowTempRoiCur;

    /* Region of interest SrcCur */
    private Cursor<T> m_srcCursor;

    /* Inital ROI */
    private RectangleRegionOfInterest m_beginRoi;

    /* Inital origin of the sliding window */
    private double[] m_beginRoiOrigin;

    /* Extend of the sliding window */
    private double[] m_beginRoiExtend;

    /* Region of interest SrcCur */
    private Cursor<T> m_beginTempRoiCur;

    /* Region of interest SrcCur */
    private int m_rowLength;

    private RandomAccess<T> m_tempRandomAccess;

    private Cursor<T> m_resCursor;

    private Cursor<T> m_tempCursor;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public K compute(final K src, final K res) {
        final int numDim = src.numDimensions();
        if (numDim > MAX_DIMS) {
            throw new IllegalArgumentException("Too many dimensions are selected.");
        }
        if (src.numDimensions() < MIN_DIMS) {
            throw new IllegalArgumentException("Two dimensions have to be selected.");
        }
        final boolean[] dimSrcCur = new boolean[numDim];
        final long[] dims = new long[numDim];
        final long[] useDims = new long[dims.length];

        for (int i = 0; i < numDim; ++i) {
            final long d = src.dimension(i);
            useDims[i] = (long)(d * (1 - m_ignorePercent));
            if (dimSrcCur[i] = (src.dimension(i) > 1)) {
                int n = 2;
                while (d > n) {
                    n <<= 1;
                }
                dims[i] = n;
            } else {
                dims[i] = d;
            }
        }
        Img<T> temp;
        try {
            temp =
                    new ArrayImgFactory().imgFactory(src.firstElement().createVariable())
                            .create(dims, src.firstElement().createVariable());
            m_tempRandomAccess = temp.randomAccess();
        } catch (final IncompatibleTypeException e1) {
            throw new IllegalArgumentException("Cannot create temp img.");
        }
        m_srcCursor = src.cursor();

        while (m_srcCursor.hasNext()) {
            m_srcCursor.fwd();
            m_tempRandomAccess.setPosition(m_srcCursor);
            m_tempRandomAccess.get().setReal(m_srcCursor.get().getRealDouble());
        }
        m_srcCursor.reset();

        final T obj = src.firstElement();
        obj.setReal(0);

        final Queue<FutureTask<WaveletDecompositionThread>> hashQueueDecomposition =
                new LinkedList<FutureTask<WaveletDecompositionThread>>();
        final Queue<FutureTask<WaveletCompositionThread>> hashQueueComposition =
                new LinkedList<FutureTask<WaveletCompositionThread>>();

        {
            for (int dim = 0; dim < numDim; ++dim) {
                if (dimSrcCur[dim]) {
                    if ((m_selectedRowRoi == null)
                            || ((m_selectedRowRoiOrigin.length != m_selectedRowRoi.numDimensions()) && (m_beginRoi == null))
                            || (m_beginRoiOrigin.length != m_beginRoi.numDimensions())) {
                        m_selectedRowRoiOrigin = new double[numDim];
                        m_selectedRowRoiExtend = new double[numDim];
                        m_beginRoiOrigin = new double[numDim];
                        m_beginRoiExtend = new double[numDim];
                    }
                    for (int d = 0; d < numDim; d++) {
                        if (dim == d) {
                            m_rowLength = (int)temp.dimension(d);
                            m_selectedRowRoiExtend[d] = temp.dimension(d);
                            m_beginRoiExtend[d] = 1;
                        } else {
                            m_selectedRowRoiExtend[d] = 1;
                            m_beginRoiExtend[d] = temp.dimension(d);
                        }
                        m_selectedRowRoiOrigin[d] = 0;
                        m_beginRoiOrigin[d] = 0;
                    }
                    m_beginRoi = new RectangleRegionOfInterest(m_beginRoiOrigin, m_beginRoiExtend);
                    m_beginTempRoiCur = m_beginRoi.getIterableIntervalOverROI(Views.extendValue(temp, obj)).cursor();
                    m_beginRoi.setOrigin(m_beginRoiOrigin);

                    m_selectedRowRoi = new RectangleRegionOfInterest(m_selectedRowRoiOrigin, m_selectedRowRoiExtend);
                    m_selectedRowTempRoiCur =
                            m_selectedRowRoi.getIterableIntervalOverROI(Views.extendValue(temp, obj)).cursor();

                    while (m_beginTempRoiCur.hasNext()) {
                        m_beginTempRoiCur.next();
                        m_tempRandomAccess.setPosition(m_beginTempRoiCur);
                        final double[] pos = new double[numDim];
                        for (int d = 0; d < numDim; d++) {
                            pos[d] = m_tempRandomAccess.getDoublePosition(d);
                        }
                        m_selectedRowRoiOrigin = pos;
                        m_selectedRowRoi.setOrigin(m_selectedRowRoiOrigin);
                        int p = 0;
                        final double[] row = new double[m_rowLength];
                        while (m_selectedRowTempRoiCur.hasNext()) {
                            row[p++] = m_selectedRowTempRoiCur.next().getRealDouble();
                        }
                        m_selectedRowTempRoiCur.reset();
                        final WaveletDecompositionThread wave = new WaveletDecompositionThread(row, pos);
                        final FutureTask<WaveletDecompositionThread> task =
                                new FutureTask<WaveletDecompositionThread>(wave);
                        hashQueueDecomposition.add(task);
                        m_executor.execute(task);
                    }
                    m_beginTempRoiCur.reset();

                    while (!hashQueueDecomposition.isEmpty()) {
                        final FutureTask<WaveletDecompositionThread> result = hashQueueDecomposition.poll();
                        try {
                            final WaveletDecompositionThread wave = result.get();
                            m_tempRandomAccess.setPosition(wave.getPos());
                            final double[] row = wave.getWaveOut();
                            for (int p = 0; p < m_rowLength; ++p) {
                                m_tempRandomAccess.setPosition(p, dim);
                                m_tempRandomAccess.get().setReal(row[p]);
                            }
                        } catch (final ExecutionException e) {
                            final Throwable th = e.getCause();
                            if (th == null) {
                                throw new IllegalArgumentException("Unknow Error during execution.");
                            } else if (th instanceof InterruptedException) {
                                throw new IllegalArgumentException("Canceled");
                            } else {
                                throw new IllegalArgumentException("Error:" + th);
                            }
                        } catch (final InterruptedException e) {
                            throw new IllegalArgumentException("Canceled");
                        }
                    }
                }
            }
        }

        {

            /**
             * Wavelet Filter
             */

            m_tempCursor = temp.cursor();

            while (m_tempCursor.hasNext()) {

                if ((m_tempCursor.next().getRealDouble() < m_lambdaMax)
                        && (m_tempCursor.get().getRealDouble() > m_lambdaMin)) {
                    m_tempCursor.get().setZero();
                }
                for (int i = 0; i < useDims.length; ++i) {
                    if (useDims[i] < m_tempCursor.getDoublePosition(i)) {
                        m_tempCursor.get().setZero();
                        continue;
                    }
                }
            }

            m_tempCursor.reset();

        }

        {

            for (int dim = numDim - 1; 0 <= dim; --dim) {
                if (dimSrcCur[dim]) {
                    if ((m_selectedRowRoi == null)
                            || ((m_selectedRowRoiOrigin.length != m_selectedRowRoi.numDimensions()) && (m_beginRoi == null))
                            || (m_beginRoiOrigin.length != m_beginRoi.numDimensions())) {
                        m_selectedRowRoiOrigin = new double[numDim];
                        m_selectedRowRoiExtend = new double[numDim];
                        m_beginRoiOrigin = new double[numDim];
                        m_beginRoiExtend = new double[numDim];
                    }
                    for (int d = 0; d < numDim; d++) {
                        if (dim == d) {
                            m_rowLength = (int)temp.dimension(d);
                            m_selectedRowRoiExtend[d] = temp.dimension(d);
                            m_beginRoiExtend[d] = 1;
                        } else {
                            m_selectedRowRoiExtend[d] = 1;
                            m_beginRoiExtend[d] = temp.dimension(d);
                        }
                        m_selectedRowRoiOrigin[d] = 0;
                        m_beginRoiOrigin[d] = 0;
                    }
                    m_beginRoi = new RectangleRegionOfInterest(m_beginRoiOrigin, m_beginRoiExtend);
                    m_beginTempRoiCur = m_beginRoi.getIterableIntervalOverROI(Views.extendValue(temp, obj)).cursor();
                    m_beginRoi.setOrigin(m_beginRoiOrigin);

                    m_selectedRowRoi = new RectangleRegionOfInterest(m_selectedRowRoiOrigin, m_selectedRowRoiExtend);
                    m_selectedRowTempRoiCur =
                            m_selectedRowRoi.getIterableIntervalOverROI(Views.extendValue(temp, obj)).cursor();

                    while (m_beginTempRoiCur.hasNext()) {
                        m_beginTempRoiCur.next();
                        m_tempRandomAccess.setPosition(m_beginTempRoiCur);
                        final double[] pos = new double[numDim];
                        for (int d = 0; d < numDim; d++) {
                            pos[d] = m_tempRandomAccess.getDoublePosition(d);
                        }
                        m_selectedRowRoiOrigin = pos;
                        m_selectedRowRoi.setOrigin(m_selectedRowRoiOrigin);
                        int p = 0;
                        final double[] row = new double[m_rowLength];
                        while (m_selectedRowTempRoiCur.hasNext()) {
                            row[p++] = m_selectedRowTempRoiCur.next().getRealDouble();
                        }
                        m_selectedRowTempRoiCur.reset();
                        final WaveletCompositionThread wave = new WaveletCompositionThread(row, pos);
                        final FutureTask<WaveletCompositionThread> task =
                                new FutureTask<WaveletCompositionThread>(wave);
                        hashQueueComposition.add(task);
                        m_executor.execute(task);
                    }
                    m_beginTempRoiCur.reset();

                    while (!hashQueueComposition.isEmpty()) {
                        final FutureTask<WaveletCompositionThread> result = hashQueueComposition.poll();
                        try {
                            final WaveletCompositionThread wave = result.get();
                            m_tempRandomAccess.setPosition(wave.getPos());
                            final double[] row = wave.getWaveOut();
                            for (int p = 0; p < m_rowLength; ++p) {
                                m_tempRandomAccess.setPosition(p, dim);
                                m_tempRandomAccess.get().setReal(row[p]);
                            }
                        } catch (final ExecutionException e) {
                            final Throwable th = e.getCause();
                            if (th == null) {
                                throw new IllegalArgumentException("Unknow Error during execution.");
                            } else if (th instanceof InterruptedException) {
                                throw new IllegalArgumentException("Canceled");
                            } else {
                                throw new IllegalArgumentException("Error:" + th);
                            }
                        } catch (final InterruptedException e) {
                            throw new IllegalArgumentException("Canceled");
                        }
                    }
                }
            }
        }

        m_resCursor = res.cursor();

        while (m_resCursor.hasNext()) {
            m_resCursor.fwd();
            m_tempRandomAccess.setPosition(m_resCursor);
            m_resCursor.get().setReal(m_tempRandomAccess.get().getRealDouble());
        }
        m_resCursor.reset();

        return res;
    }

    private class WaveletDecompositionThread implements Callable<WaveletDecompositionThread> {
        private final double[] m_waveOut;

        private final double[] m_waveIn;

        private final long[] m_pos;

        public WaveletDecompositionThread(final double[] in, final double[] pos) {
            m_waveOut = new double[in.length];
            m_waveIn = in;
            this.m_pos = new long[pos.length];
            for (int d = 0; d < pos.length; ++d) {
                this.m_pos[d] = (long)pos[d];
            }
        }

        public long[] getPos() {
            return m_pos;
        }

        public double[] getWaveOut() {
            return m_waveOut;
        }

        @Override
        public WaveletDecompositionThread call() {
            int length = m_waveIn.length;
            while (length > 1) {
                for (int i = 0; i < (length >> 1); ++i) {
                    final int p = i << 1;
                    m_waveOut[i] = (m_waveIn[p] + m_waveIn[p + 1]) / 2;
                    m_waveOut[(length >> 1) + i] = (m_waveIn[p] - m_waveIn[p + 1]) / 2;
                }
                for (int i = 0; i < length; ++i) {
                    m_waveIn[i] = m_waveOut[i];
                }
                length >>= 1;
            }
            return this;
        }
    }

    class WaveletCompositionThread implements Callable<WaveletCompositionThread> {
        private final double[] waveOut;

        private final double[] waveIn;

        private final long[] pos;

        public WaveletCompositionThread(final double[] in, final double[] pos) {
            waveOut = new double[in.length];
            waveIn = in;
            this.pos = new long[pos.length];
            for (int d = 0; d < pos.length; ++d) {
                this.pos[d] = (long)pos[d];
            }
        }

        public long[] getPos() {
            return pos;
        }

        public double[] getWaveOut() {
            return waveOut;
        }

        @Override
        public WaveletCompositionThread call() {
            final int n = waveIn.length;
            final int q = n << 1;
            int i = 2;
            int v = 0;
            int u = n >> 1;
            int d = 1;
            for (int t = 0; t < n; ++t) {
                waveOut[t] = waveIn[0];
            }
            while (i < q) {
                waveOut[v % n] += ((i % 2) == 0 ? 1 : -1) * waveIn[i - d];
                if ((++v % u) == 0) {
                    ++i;
                    d += ((i % 2) == 1 ? 1 : 0);
                    u >>= ((v % n) == 0 ? 1 : 0);
                }
            }
            return this;
        }
    }

    /**
     * @param executor
     * @param lambda_min
     * @param lambda_max
     * @param ignorePercent
     */
    public WaveletFilter(final ExecutorService executor, final double lambda_min, final double lambda_max,
                         final double ignorePercent) {
        m_executor = executor;
        m_lambdaMin = lambda_min;
        m_lambdaMax = lambda_max;
        m_ignorePercent = ignorePercent;
    }

    @Override
    public UnaryOperation<K, K> copy() {
        return new WaveletFilter<T, K>(m_executor, m_lambdaMin, m_lambdaMax, m_ignorePercent);
    }
}
