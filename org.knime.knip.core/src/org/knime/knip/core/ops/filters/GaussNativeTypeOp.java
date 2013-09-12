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

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.gauss3.SeparableSymmetricConvolution;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * 
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author dietyc
 */
public class GaussNativeTypeOp<T extends RealType<T> & NativeType<T>, TYPE extends RandomAccessibleInterval<T>>
        implements UnaryOperation<TYPE, TYPE> {

    private final double[] m_sigmas;

    private final OutOfBoundsFactory<T, TYPE> m_fac;

    private final int m_numThreads;

    /**
     * @param numThreads
     * @param sigmas
     * @param factory
     */
    public GaussNativeTypeOp(final int numThreads, final double[] sigmas, final OutOfBoundsFactory<T, TYPE> factory) {
        m_sigmas = sigmas.clone();
        m_fac = factory;
        m_numThreads = 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public TYPE compute(final TYPE input, final TYPE output) {

        if (m_sigmas.length != input.numDimensions()) {
            throw new IllegalArgumentException("Size of sigma array doesn't fit to input image");
        }

        final RandomAccessible<FloatType> rIn = (RandomAccessible<FloatType>)Views.extend(input, m_fac);

        try {
            SeparableSymmetricConvolution.convolve(Gauss3.halfkernels(m_sigmas), rIn, output, 1);

        } catch (final IncompatibleTypeException e) {
            throw new RuntimeException(e);
        }

        return output;
    }

    @Override
    public UnaryOperation<TYPE, TYPE> copy() {
        return new GaussNativeTypeOp<T, TYPE>(m_numThreads, m_sigmas.clone(), m_fac);
    }

}
