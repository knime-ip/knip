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

/**
 * 
 * @author Martin Horn, University of Konstanz
 * 
 */
public class ImgMinOperator<T extends RealType<T>> extends ImgPixOperator<T> {

    public ImgMinOperator() {
        super("Min Image", "Min Image");
    }

    /**
     * @param label
     */
    public ImgMinOperator(final GlobalSettings globalSettings) {
        super("Min Image", "Min Image", globalSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
                                              final OperatorColumnSettings opColSettings) {
        return new ImgMinOperator<T>(globalSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RealUnaryOperation<T, T> createTypeOperation() {
        return new RealUnaryOperation<T, T>() {
            @Override
            public T compute(final T input, final T output) {
                output.setReal(Math.min(input.getRealDouble(), output.getRealDouble()));
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
        return "Calculates the min image.";
    }

}
