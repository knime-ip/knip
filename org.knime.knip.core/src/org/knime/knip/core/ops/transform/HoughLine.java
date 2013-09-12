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
package org.knime.knip.core.ops.transform;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class HoughLine<T extends RealType<T> & NativeType<T>, S extends RealType<S> & NativeType<S>, K extends IterableInterval<T>>
        implements UnaryOutputOperation<K, Img<S>> {

    private final int m_numBinsRho;

    private final int m_numBinsTheta;

    private final S m_outType;

    private double[] m_rho;

    private double[] m_theta;

    private double m_dTheta;

    private double m_dRho;

    private T m_threshold;

    public HoughLine(final S outType, final T threshold, final int numBinsRho, final int numBinsTheta) {
        m_outType = outType;
        m_numBinsRho = numBinsRho;
        m_numBinsTheta = numBinsTheta;
        m_threshold = threshold;
    }

    @Override
    public Img<S> compute(final K op, final Img<S> res) {

        init(op);
        final long[] dims = new long[res.numDimensions()];
        res.dimensions(dims);

        final RandomAccess<S> resAccess = res.randomAccess();
        final Cursor<T> cursor = op.cursor();
        final long[] position = new long[op.numDimensions()];
        final double minTheta = -Math.PI / 2;
        final double minRho = -Util.computeLength(Util.intervalDimensions(op));

        for (int t = 0; t < m_numBinsTheta; ++t) {
            m_theta[t] = (m_dTheta * t) + minTheta;
        }
        for (int r = 0; r < m_numBinsRho; ++r) {
            m_rho[r] = (m_dRho * r) + minRho;
        }

        while (cursor.hasNext()) {
            double fRho;
            int r;
            final int[] voteLoc = new int[2];

            cursor.fwd();
            cursor.localize(position);

            for (int t = 0; t < m_numBinsTheta; ++t) {
                if (cursor.get().compareTo(m_threshold) > 0) {
                    fRho = (Math.cos(m_theta[t]) * position[0]) + (Math.sin(m_theta[t]) * position[1]);

                    r = Math.round((float)((fRho - minRho) / m_dRho));
                    voteLoc[0] = r;
                    voteLoc[1] = t;
                    try {
                        placeVote(voteLoc, resAccess);
                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return res;

    }

    private void init(final K op) {
        m_dRho = (2 * Util.computeLength(Util.intervalDimensions(op))) / (double)m_numBinsRho;
        m_threshold = op.firstElement().createVariable();
        m_dTheta = Math.PI / m_numBinsTheta;
        m_theta = new double[m_numBinsTheta];
        m_rho = new double[m_numBinsRho];

    }

    /**
     * Place a vote of value 1.
     * 
     * @param loc the integer array indicating the location where the vote is to be placed in voteSpace.
     * @return whether the vote was successful. This here particular method should always return true.
     */
    protected void placeVote(final int[] loc, final RandomAccess<S> ra) {
        ra.setPosition(loc);
        m_outType.setOne();
        ra.get().add(m_outType);
    }

    public double[] getTranslatedPos(final int[] pos) {
        return new double[]{m_rho[pos[0]], m_theta[pos[1]]};

    }

    @Override
    public UnaryOutputOperation<K, Img<S>> copy() {
        return new HoughLine<T, S, K>(m_outType.copy(), m_threshold, m_numBinsRho, m_numBinsTheta);
    }

    @Override
    public UnaryObjectFactory<K, Img<S>> bufferFactory() {
        return new UnaryObjectFactory<K, Img<S>>() {

            @Override
            public Img<S> instantiate(final K a) {
                return new ArrayImgFactory<S>().create(new long[]{m_numBinsRho, m_numBinsTheta},
                                                       m_outType.createVariable());
            }
        };
    }

}
