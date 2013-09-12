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

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DirectionalGradient<T extends RealType<T>, K extends RandomAccessibleInterval<T> & IterableInterval<T>>
        implements UnaryOperation<K, K> {

    // TODO: Can be extended to diagonalA and diagonalB
    public enum GradientDirection {
        HORIZONTAL, VERTICAL;
    }

    private final int[] m_dims;

    private final boolean m_invert;

    /**
     * @param direction
     * @param invert inverts the gradient calculation, if false, the difference is calculated as left-right, else
     *            right-left
     */
    public DirectionalGradient(final GradientDirection direction, final boolean invert) {
        m_invert = invert;

        m_dims = new int[2];

        switch (direction) {
            case HORIZONTAL:
                m_dims[0] = 1;
                m_dims[1] = 0;
                break;
            case VERTICAL:
                m_dims[1] = 1;
                m_dims[0] = 0;
                break;
            default:
                break;
        }

    }

    @Override
    public K compute(final K op, final K r) {
        if (op.numDimensions() != 2) {
            throw new IllegalArgumentException("Operation can only be performed on 2 dimensional images");
        }

        final double max = op.firstElement().getMaxValue();
        final double min = op.firstElement().getMinValue();

        final RandomAccess<T> opLeftRndAccess = Views.extendMirrorDouble(op).randomAccess();
        final RandomAccess<T> opRightRndAccess = Views.extendMirrorDouble(op).randomAccess();
        final RandomAccess<T> resAccess = r.randomAccess();

        double diff;
        for (int y = 0; y < op.dimension(m_dims[0]); y++) {
            opLeftRndAccess.setPosition(y, m_dims[0]);
            opRightRndAccess.setPosition(y, m_dims[0]);
            resAccess.setPosition(y, m_dims[0]);

            opLeftRndAccess.setPosition(-1, m_dims[1]);
            opRightRndAccess.setPosition(1, m_dims[1]);
            resAccess.setPosition(0, m_dims[1]);

            for (int x = 0; x < op.dimension(m_dims[1]); x++) {

                if (m_invert) {
                    diff = (opRightRndAccess.get().getRealDouble() - opLeftRndAccess.get().getRealDouble()) + min;
                } else {
                    diff = (opLeftRndAccess.get().getRealDouble() - opRightRndAccess.get().getRealDouble()) + min;
                }

                resAccess.get().setReal(Math.max(min, Math.min(max, diff)));

                opLeftRndAccess.fwd(m_dims[1]);
                opRightRndAccess.fwd(m_dims[1]);
                resAccess.fwd(m_dims[1]);
            }
        }
        return r;
    }

    @Override
    public UnaryOperation<K, K> copy() {
        return new DirectionalGradient<T, K>(GradientDirection.HORIZONTAL, m_invert);
    }
}
