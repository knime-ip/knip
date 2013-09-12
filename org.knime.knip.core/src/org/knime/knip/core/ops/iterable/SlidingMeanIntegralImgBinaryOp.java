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
package org.knime.knip.core.ops.iterable;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.BinaryOperation;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.knime.knip.core.ops.integralimage.IntegralImgND;
import org.knime.knip.core.ops.integralimage.IntegralImgSumAgent;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SlidingMeanIntegralImgBinaryOp<T extends RealType<T>, V extends RealType<V>, IN extends RandomAccessibleInterval<T>, OUT extends IterableInterval<V>>
        extends SlidingShapeOp<T, V, IN, OUT> {

    @SuppressWarnings({"rawtypes", "unchecked"})
    private final IntegralImgND m_iiOp = new IntegralImgND(new ArrayImgFactory());

    private final BinaryOperation<DoubleType, T, V> m_binaryOp;

    private final int m_span;

    public SlidingMeanIntegralImgBinaryOp(final BinaryOperation<DoubleType, T, V> binaryOp, final RectangleShape shape,
                                          final int span, final OutOfBoundsFactory<T, IN> outOfBounds) {
        super(shape, outOfBounds);
        m_binaryOp = binaryOp;
        m_span = span;
    }

    @Override
    public UnaryOperation<IN, OUT> copy() {
        return new SlidingMeanIntegralImgBinaryOp<T, V, IN, OUT>(m_binaryOp.copy(), (RectangleShape)m_shape, m_span,
                m_outOfBounds);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected OUT compute(final IterableInterval<Neighborhood<T>> neighborhoods, final IN input, final OUT output) {

        final long[] min = new long[input.numDimensions()];
        final long[] max = new long[input.numDimensions()];

        for (int d = 0; d < input.numDimensions(); d++) {
            min[d] = -m_span;
            max[d] = (input.dimension(d) - 1) + m_span;
        }

        final Cursor<T> inCursor = Views.flatIterable(input).cursor();
        final Cursor<V> outCursor = output.cursor();

        // extend such that image is 2*span larger in each dimension
        // with corresponding outofbounds extension
        // Result: We have a IntegralImage

        final IntervalView<T> extended =
                Views.offset(Views.interval(Views.extend(input, m_outOfBounds), new FinalInterval(min, max)), min);

        final RandomAccessibleInterval<IntType> ii = Operations.compute(m_iiOp, extended);

        final DoubleType mean = new DoubleType();
        final long[] p1 = new long[input.numDimensions()];
        final long[] p2 = new long[input.numDimensions()];

        final IntegralImgSumAgent sumAgent = new IntegralImgSumAgent(ii);

        for (final Neighborhood<T> neighborhood : neighborhoods) {
            inCursor.fwd();
            outCursor.fwd();

            for (int d = 0; d < p1.length; d++) {
                final long p = inCursor.getLongPosition(d);
                p1[d] = p; // -span
                p2[d] = p + (2L * m_span); // +span
            }

            mean.setReal(sumAgent.getSum(p1, p2) / neighborhood.size());

            m_binaryOp.compute(mean, inCursor.get(), outCursor.get());
        }

        return output;
    }

}
