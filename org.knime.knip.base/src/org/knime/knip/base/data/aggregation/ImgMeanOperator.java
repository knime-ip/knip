/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2012
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 18, 2012 (hornm): created
 */

package org.knime.knip.base.data.aggregation;

import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.real.unary.RealUnaryOperation;
import net.imglib2.type.numeric.RealType;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.core.data.DataCell;

/**
 * 
 * @author Martin Horn, University of Konstanz
 * 
 */
public class ImgMeanOperator<T extends RealType<T>> extends ImgPixOperator<T> {

    protected int m_count = 0;

    public ImgMeanOperator() {
        super("Mean Image", "Mean Image");
    }

    /**
     * @param label
     */
    public ImgMeanOperator(final GlobalSettings globalSettings) {
        super("Mean Image", "Mean Image", globalSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        final boolean b = super.computeInternal(cell);
        if (!b) {
            m_count++;
        }

        return b;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
                                              final OperatorColumnSettings opColSettings) {
        return new ImgMeanOperator<T>(globalSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RealUnaryOperation<T, T> createTypeOperation() {
        return new RealUnaryOperation<T, T>() {
            @Override
            public T compute(final T input, final T output) {
                output.setReal((output.getRealDouble() * ((double)m_count / (m_count + 1)))
                        + (input.getRealDouble() * (1.0 / (m_count + 1))));
                return output;
            }

            @Override
            public UnaryOperation<T, T> copy() {
                //
                return null;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the mean image.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_count = 0;
        super.resetInternal();

    }

}
