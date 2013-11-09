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
 * ---------------------------------------------------------------------
 *
 * Created on 07.11.2013 by Daniel
 */
package org.knime.knip.base.nodes.proc.clahe;

import java.util.Arrays;

/**
 *
 * @author Daniel
 */
public class ClahePoint {

    private long[] m_coordinates;

    /**
     * @param coordinates
     */
    public ClahePoint(final long[] coordinates) {
        this.m_coordinates = coordinates;
    }

    /**
     * @return the coordinates in all dimensions
     */
    public long[] getCoordinates() {
        return this.m_coordinates;
    }

    /**
     * @return the dimensionality of this point
     */
    public int numDim() {
        return m_coordinates.length;
    }

    /**
     * @param i the dimension
     * @return the coordinate value in the given dimension
     */
    public long dim(final int i) {
        return this.m_coordinates[i];
    }

    /**
     * @param otherPoint a different ClahePoint
     * @return the distance between this ClahePoint and another ClahePoint
     */
    public float distance(final ClahePoint otherPoint) {
        if (this.numDim() != otherPoint.numDim()) {
            throw new IllegalArgumentException("Can't measure distance between points with different dimensionality");
        }

        float sum = 0;
        for (int i = 0; i < this.numDim(); i++) {
            sum += Math.pow(this.dim(i) - otherPoint.dim(i), 2);
        }

        return (float)Math.sqrt(sum);
    }

    /**
     * @param offsets the offset used by the CLAHE operation
     * @return if this point is a center
     */
    public boolean isCenter(final long[] offsets) {
        if (this.numDim() != offsets.length) {
            throw new IllegalArgumentException("Different number of dimensions");
        }

        for (int i = 0; i < this.numDim(); i++) {
            if (this.dim(i) % offsets[i] != 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ClahePoint)) {
            return false;
        }

        ClahePoint otherPoint = (ClahePoint)obj;

        if (this.numDim() != otherPoint.numDim()) {
            return false;
        }

        for (int i = 0; i < this.numDim(); i++) {
            if (this.dim(i) != otherPoint.dim(i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(m_coordinates);
    }

    /**
     * @param imageDimensions the dimensions of an image
     * @return boolean true or false, whether this point lies inside the image boundaries or not.
     */
    public boolean isInsideImage(final long[] imageDimensions) {

        for (int i = 0; i < this.numDim(); i++) {
            if (this.dim(i) < 0 || this.dim(i) >= imageDimensions[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ClahePoint [m_coordinates=" + Arrays.toString(m_coordinates) + "]";
    }
}
