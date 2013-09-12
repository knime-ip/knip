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

import java.io.IOException;

import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.UnaryOperationAssignment;
import net.imglib2.ops.operation.real.unary.RealUnaryOperation;
import net.imglib2.type.numeric.RealType;

import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;

/**
 * Simple, so far infexible version of a pixel-wise image aggregation operator (image are required to have the same
 * iteration order, i.e. dimension etc.). TODO: make it more flexible.
 * 
 * @author Martin Horn, University of Konstanz
 * 
 */
public abstract class ImgPixOperator<T extends RealType<T>> extends ImgAggregrationOperation {

    /* the pixel-wise operation to apply */
    private RealUnaryOperation<T, T> m_op = null;

    /* the tempory result holding the pixel-wise mean */
    private ImgPlus<T> m_resImg = null;

    public ImgPixOperator(final String id, final String label) {
        super(id, label, label);
    }

    /**
     */
    public ImgPixOperator(final String id, final String label, final GlobalSettings globalSettings) {
        super(id, label, globalSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        if (cell.isMissing()) {
            return true;
        }
        final ImgPlus<T> img = ((ImgPlusValue<T>)cell).getImgPlus();
        if (m_resImg == null) {
            m_op = createTypeOperation();
            m_resImg = img.copy();
        } else {
            try {
                new UnaryOperationAssignment<T, T>(m_op).compute(img, m_resImg);

            } catch (final IllegalArgumentException e) {
                setSkipMessage("Images are not compatible (dimensions, iteration order, etc.): " + e.getMessage());
                return true;
            }
        }
        return false;
    }

    /**
     * @return
     */
    protected abstract RealUnaryOperation<T, T> createTypeOperation();

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return ImgPlusCell.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        try {
            return getImgPlusCellFactory().createCell(m_resImg);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_resImg = null;
    }

}
