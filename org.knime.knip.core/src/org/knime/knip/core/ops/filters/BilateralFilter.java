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

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * Bilateral filtering
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author tcriess, University of Konstanz
 */
public class BilateralFilter<T extends RealType<T>, K extends RandomAccessibleInterval<T> & IterableInterval<T>>
        implements UnaryOperation<K, K> {

    public final static int MIN_DIMS = 2;

    public final static int MAX_DIMS = 2;

    private double m_sigmaR = 15;

    private double m_sigmaS = 5;

    private int m_radius = 10;

    public BilateralFilter(final double sigma_r, final double sigma_s, final int radius) {
        m_sigmaR = sigma_r;
        m_sigmaS = sigma_s;
        m_radius = radius;
    }

    private static double gauss(final double x, final double sigma) {
        final double mu = 0.0;
        return (1 / (sigma * Math.sqrt(2 * Math.PI))) * Math.exp((-0.5 * (x - mu) * (x - mu)) / (sigma * sigma));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public K compute(final K srcIn, final K res) {

        if (srcIn.numDimensions() != 2) {
            throw new IllegalArgumentException("Input must be two dimensional");
        }

        final long[] size = new long[srcIn.numDimensions()];
        srcIn.dimensions(size);

        final RandomAccess<T> cr = res.randomAccess();
        final Cursor<T> cp = srcIn.localizingCursor();
        final int[] p = new int[srcIn.numDimensions()];
        final int[] q = new int[srcIn.numDimensions()];
        final long[] mi = new long[srcIn.numDimensions()];
        final long[] ma = new long[srcIn.numDimensions()];
        final long mma1 = srcIn.max(0);
        final long mma2 = srcIn.max(1);
        IterableInterval<T> si;
        Cursor<T> cq;
        while (cp.hasNext()) {
            cp.fwd();
            cp.localize(p);
            double d;
            cp.localize(mi);
            cp.localize(ma);
            mi[0] = Math.max(0, mi[0] - m_radius);
            mi[1] = Math.max(0, mi[1] - m_radius);
            ma[0] = Math.min(mma1, ma[0] + m_radius);
            ma[1] = Math.min(mma2, ma[1] + m_radius);
            final Interval in = new FinalInterval(mi, ma);
            si = Views.iterable(SubsetOperations.subsetview(srcIn, in));
            cq = si.localizingCursor();
            double s, v = 0.0;
            double w = 0.0;
            while (cq.hasNext()) {
                cq.fwd();
                cq.localize(q);
                d = ((p[0] - q[0] - mi[0]) * (p[0] - q[0] - mi[0])) + ((p[1] - q[1] - mi[1]) * (p[1] - q[1] - mi[1]));
                d = Math.sqrt(d);
                s = gauss(d, m_sigmaS);

                d = Math.abs(cp.get().getRealDouble() - cq.get().getRealDouble());
                s *= gauss(d, m_sigmaR);

                v += s * cq.get().getRealDouble();
                w += s;
            }
            cr.setPosition(p);
            cr.get().setReal(v / w);
        }
        return res;

    }

    @Override
    public UnaryOperation<K, K> copy() {
        return new BilateralFilter<T, K>(m_sigmaR, m_sigmaS, m_radius);
    }
}
