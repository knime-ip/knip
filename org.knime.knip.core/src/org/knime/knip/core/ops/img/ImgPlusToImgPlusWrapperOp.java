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
package org.knime.knip.core.ops.img;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.util.ImgPlusFactory;

/**
 * Simple wrapper class to wrap UnaryOperations to UnaryOutputOperations which run on ImgPlus basis
 * 
 * @param <T> input type
 * @param <V> output type
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgPlusToImgPlusWrapperOp<T extends RealType<T>, V extends RealType<V>> implements
        UnaryOutputOperation<ImgPlus<T>, ImgPlus<V>> {

    private UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<V>> m_op;

    private V m_outType;

    /**
     * @param op
     * @param outType
     */
    public ImgPlusToImgPlusWrapperOp(final UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<V>> op,
                                     final V outType) {
        m_op = op;
        m_outType = outType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlus<V> compute(final ImgPlus<T> input, final ImgPlus<V> output) {
        m_op.compute(input, output);
        return output;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryObjectFactory<ImgPlus<T>, ImgPlus<V>> bufferFactory() {
        return ImgPlusFactory.<T, V> get(m_outType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOutputOperation<ImgPlus<T>, ImgPlus<V>> copy() {
        return new ImgPlusToImgPlusWrapperOp<T, V>(m_op.copy(), m_outType);
    }

}
