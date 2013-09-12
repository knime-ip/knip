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
package org.knime.knip.core.util;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

import org.apache.commons.math3.linear.AbstractRealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgBasedRealMatrix<T extends RealType<T>, IN extends RandomAccessibleInterval<T>> extends
        AbstractRealMatrix {

    private final RandomAccess<T> m_rndAccess;

    private final IN m_in;

    public ImgBasedRealMatrix(final IN in) {
        if (in.numDimensions() != 2) {
            throw new IllegalArgumentException("In must have exact two dimensions to be handled as a matrix");
        }
        m_in = in;
        m_rndAccess = in.randomAccess();
    }

    @Override
    public RealMatrix createMatrix(final int rowDimension, final int columnDimension) {
        return new Array2DRowRealMatrix(rowDimension, columnDimension);
    }

    @Override
    public RealMatrix copy() {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public double getEntry(final int row, final int column) {
        m_rndAccess.setPosition(row, 1);
        m_rndAccess.setPosition(column, 0);

        return m_rndAccess.get().getRealDouble();
    }

    @Override
    public void setEntry(final int row, final int column, final double value) {
        m_rndAccess.setPosition(row, 1);
        m_rndAccess.setPosition(column, 0);
        m_rndAccess.get().setReal(value);
    }

    @Override
    public int getRowDimension() {
        return (int)m_in.dimension(0);
    }

    @Override
    public int getColumnDimension() {
        return (int)m_in.dimension(1);
    }

}
