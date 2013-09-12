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
package org.knime.knip.core.features.seg;

import net.imglib2.img.Img;
import net.imglib2.ops.img.BinaryOperationAssignment;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.Dilate;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.Erode;
import net.imglib2.ops.operation.real.binary.RealXor;
import net.imglib2.ops.types.ConnectedType;
import net.imglib2.type.logic.BitType;

/**
 * Extracts the outline of a given connected component in an {@link Img} of {@link BitType}. The outline is here defined
 * as all pixels, which are next to the pixels which are on the border of the connected component. Please be aware that
 * for a correct calculation of the Perimeter only one connected component should be contained in the {@link Img} of
 * {@link BitType}
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ExtractOutlineImg implements UnaryOperation<Img<BitType>, Img<BitType>> {

    private final BinaryOperationAssignment<BitType, BitType, BitType> m_imgManWith;

    private final UnaryOperation<Img<BitType>, Img<BitType>> m_op;

    private final boolean m_outlineInsideSegment;

    public ExtractOutlineImg(final boolean outlineInsideSegment) {
        m_outlineInsideSegment = outlineInsideSegment;
        m_imgManWith =
                new BinaryOperationAssignment<BitType, BitType, BitType>(new RealXor<BitType, BitType, BitType>());
        m_op =
                m_outlineInsideSegment ? new Erode<Img<BitType>>(ConnectedType.EIGHT_CONNECTED, 1)
                        : new Dilate<Img<BitType>>(ConnectedType.FOUR_CONNECTED, 1);
    }

    @Override
    public Img<BitType> compute(final Img<BitType> op, final Img<BitType> r) {
        if (op.numDimensions() != 2) {
            throw new IllegalArgumentException("Operation only permitted on two dimensions");
        }

        m_op.compute(op, r);
        m_imgManWith.compute(op, r, r);
        return r;
    }

    @Override
    public UnaryOperation<Img<BitType>, Img<BitType>> copy() {
        return new ExtractOutlineImg(m_outlineInsideSegment);
    }
}
