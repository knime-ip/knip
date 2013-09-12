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
package org.knime.knip.base.nodes.misc.splitter;

import java.util.Arrays;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.UnaryOutputOperation;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class IntervalsFromSplitSelection implements UnaryOutputOperation<Interval, Interval[]> {

    private final int[] m_maxNumDimsPerInterval;

    private int[] m_numIntervalsPerDim;

    /**
     * @param maxNumDimsPerInterval the maximum number of dimensions in the resulting intervals, if 0, the maximal
     *            possible number of points is assumed
     */
    public IntervalsFromSplitSelection(final int[] maxNumDimsPerInterval) {
        m_maxNumDimsPerInterval = maxNumDimsPerInterval.clone();

    }

    @Override
    public UnaryObjectFactory<Interval, Interval[]> bufferFactory() {
        return new UnaryObjectFactory<Interval, Interval[]>() {

            @Override
            public Interval[] instantiate(final Interval in) {
                m_numIntervalsPerDim = null;
                calcNumIntervalsPerDim(in);
                int numIntervals = 1;
                for (int d = 0; d < m_numIntervalsPerDim.length; d++) {
                    numIntervals *= m_numIntervalsPerDim[d];
                }
                return new Interval[numIntervals];
            }
        };
    }

    private void calcNumIntervalsPerDim(final Interval in) {
        if (m_numIntervalsPerDim == null) {
            m_numIntervalsPerDim = new int[in.numDimensions()];
            Arrays.fill(m_numIntervalsPerDim, 1);
            for (int d = 0; d < m_numIntervalsPerDim.length; d++) {
                if (m_maxNumDimsPerInterval[d] != 0) {
                    m_numIntervalsPerDim[d] = (int)Math.ceil(in.dimension(d) / (double)m_maxNumDimsPerInterval[d]);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Interval[] compute(final Interval input, final Interval[] output) {
        calcNumIntervalsPerDim(input);
        final IntervalIterator ii = new IntervalIterator(m_numIntervalsPerDim);

        int intervalIdx = 0;
        while (ii.hasNext()) {
            ii.fwd();
            final long[] min = new long[input.numDimensions()];
            final long[] max = new long[input.numDimensions()];

            for (int d = 0; d < max.length; d++) {
                min[d] = ii.getIntPosition(d) * m_maxNumDimsPerInterval[d];
                max[d] = Math.min((ii.getIntPosition(d) + 1) * m_maxNumDimsPerInterval[d], input.dimension(d)) - 1;
            }
            output[intervalIdx++] = new FinalInterval(min, max);

        }

        return output;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOutputOperation<Interval, Interval[]> copy() {
        return new IntervalsFromSplitSelection(m_maxNumDimsPerInterval.clone());
    }

}
