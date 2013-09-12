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
package org.knime.knip.core.features;

import java.util.BitSet;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.data.CooccurrenceMatrix;
import net.imglib2.ops.data.CooccurrenceMatrix.MatrixOrientation;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.iterableinterval.unary.MakeCooccurrenceMatrix;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.knime.knip.core.data.labeling.Signature;

/**
 * 
 * Utility class which calculates caches commonly used objects.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ObjectCalcAndCache {

    public ObjectCalcAndCache() {
    }

    private Img<BitType> m_binaryMask;

    private IterableInterval<BitType> m_bmIterableInterval;

    public Img<BitType> binaryMask(final IterableInterval<BitType> ii) {
        if (m_bmIterableInterval != ii) {
            m_bmIterableInterval = ii;
            m_binaryMask = new ArrayImgFactory<BitType>().create(ii, new BitType());
            final RandomAccess<BitType> maskRA = m_binaryMask.randomAccess();

            final Cursor<BitType> cur = ii.localizingCursor();
            while (cur.hasNext()) {
                cur.fwd();
                for (int d = 0; d < cur.numDimensions(); d++) {
                    maskRA.setPosition(cur.getLongPosition(d) - ii.min(d), d);
                }
                maskRA.get().set(true);

            }
        }
        return m_binaryMask;

    }

    private Img<BitType> m_binaryMask2D;

    private IterableInterval<BitType> m_bm2dIterableInterval;

    public Img<BitType> binaryMask2D(final IterableInterval<BitType> ii) {
        if (m_bm2dIterableInterval != ii) {
            m_bm2dIterableInterval = ii;
            final long[] dims = new long[ii.numDimensions()];
            ii.dimensions(dims);
            for (int i = 0; i < 2; i++) {
                dims[i] += 2;
            }
            final Img<BitType> mask = new ArrayImgFactory<BitType>().create(dims, new BitType());
            final RandomAccess<BitType> maskRA = mask.randomAccess();
            final Cursor<BitType> cur = ii.localizingCursor();
            while (cur.hasNext()) {
                cur.fwd();
                for (int d = 0; d < 2; d++) {
                    maskRA.setPosition((cur.getLongPosition(d) - ii.min(d)) + 1, d);
                }
                maskRA.get().set(true);
            }
            m_binaryMask2D =
                    new ImgView<BitType>(SubsetOperations.subsetview(mask, new FinalInterval(dims)), mask.factory());
        }
        return m_binaryMask2D;
    }

    private final DescriptiveStatistics m_descriptiveStatistics = new DescriptiveStatistics();

    private IterableInterval<? extends RealType<?>> m_dsIterableInterval;

    public <T extends RealType<T>> DescriptiveStatistics descriptiveStatistics(final IterableInterval<T> ii) {

        if (m_dsIterableInterval != ii) {
            m_dsIterableInterval = ii;
            m_descriptiveStatistics.clear();
            final Cursor<T> c = ii.cursor();

            while (c.hasNext()) {
                c.fwd();

                m_descriptiveStatistics.addValue(c.get().getRealDouble());
            }
        }
        return m_descriptiveStatistics;

    }

    private double[] m_centroid;

    private IterableInterval<? extends RealType<?>> m_cIterableInterval;

    public <T extends RealType<T>> double[] centroid(final IterableInterval<T> ii) {
        if (m_cIterableInterval != ii) {
            m_cIterableInterval = ii;
            final Cursor<T> c = ii.cursor();
            m_centroid = new double[ii.numDimensions()];

            long count = 0;
            while (c.hasNext()) {
                c.fwd();
                for (int i = 0; i < m_centroid.length; i++) {
                    m_centroid[i] += c.getDoublePosition(i);
                }
                count++;
            }

            for (int i = 0; i < m_centroid.length; i++) {
                m_centroid[i] /= count;
            }
        }
        return m_centroid;
    }

    private double[] m_weightedCentroid;

    private IterableInterval<? extends RealType<?>> m_wcIterableInterval;

    public <T extends RealType<T>> double[] weightedCentroid(final IterableInterval<T> ii,
                                                             final DescriptiveStatistics ds, int massDisplacement) {

        if (m_wcIterableInterval != ii) {
            m_wcIterableInterval = ii;
            m_weightedCentroid = new double[ii.numDimensions()];
            final double[] centroid = new double[ii.numDimensions()];

            final Cursor<T> c = ii.localizingCursor();

            final long[] pos = new long[c.numDimensions()];
            while (c.hasNext()) {
                c.fwd();
                c.localize(pos);

                final double val = c.get().getRealDouble();

                for (int d = 0; d < ii.numDimensions(); d++) {
                    m_weightedCentroid[d] += pos[d] * (val / ds.getSum());
                    centroid[d] += pos[d];
                }
            }

            massDisplacement = 0;
            for (int d = 0; d < ii.numDimensions(); d++) {
                centroid[d] /= ii.size();
                massDisplacement += Math.pow(m_weightedCentroid[d] - centroid[d], 2);
            }

        }
        return m_weightedCentroid;

    }

    private IterableInterval<BitType> m_sIterableInterval;

    private Signature m_signature;

    public Signature signature(final IterableInterval<BitType> ii, final int samplingRate) {

        if (m_sIterableInterval != ii) {
            m_sIterableInterval = ii;

            final double[] centroid = centroid(ii);
            final long[] pos = new long[centroid.length];
            for (int i = 0; i < pos.length; i++) {
                pos[i] = Math.round(centroid[i]);
            }

            m_signature = new Signature(binaryMask(ii), pos, samplingRate);
        }

        return m_signature;

    }

    private CooccurrenceMatrix m_coocMatrix;

    private IterableInterval<? extends RealType<?>> m_coocII;

    private int m_coocDist;

    private int m_coocGrayLevels;

    private MatrixOrientation m_coocOrientation;

    private BitSet m_coocBitSet;

    public <T extends RealType<T>> CooccurrenceMatrix cooccurenceMatrix(final IterableInterval<T> ii, final int dimX,
                                                                        final int dimY, final int distance,
                                                                        final int nrGrayLevels,
                                                                        final MatrixOrientation matrixOrientation,
                                                                        final BitSet features) {
        if ((m_coocII != ii) || (m_coocDist != distance) || (m_coocGrayLevels != nrGrayLevels)
                || (m_coocOrientation != matrixOrientation) || !features.equals(m_coocBitSet)) {
            final MakeCooccurrenceMatrix<T> matrixOp =
                    new MakeCooccurrenceMatrix<T>(dimX, dimY, distance, nrGrayLevels, matrixOrientation, features);
            if ((m_coocMatrix == null) || (m_coocGrayLevels != nrGrayLevels)) {
                // matrix still null or size must change
                m_coocMatrix = new CooccurrenceMatrix(nrGrayLevels);
            }
            matrixOp.compute(ii, m_coocMatrix);
            m_coocII = ii;
            m_coocDist = distance;
            m_coocGrayLevels = nrGrayLevels;
            m_coocOrientation = matrixOrientation;
            m_coocBitSet = features;
        }
        return m_coocMatrix;
    }

}
