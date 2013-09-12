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
package org.knime.knip.core.algorithm.logtrackmate;

import net.imglib2.Localizable;
import net.imglib2.type.numeric.NumericType;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SubPixelLocalization<T extends NumericType<T> & Comparable<T>> implements Localizable,
        Comparable<SubPixelLocalization<T>> {

    public static enum LocationType {
        INVALID, MIN, MAX
    };

    protected long[] pixelCoordinates;

    protected double[] subPixelLocationOffset;

    protected T value;

    protected T interpolatedValue;

    protected String errorMessage = "";

    protected LocationType locationType;

    public SubPixelLocalization(final long[] pixelCoordinates, T value, LocationType locationType) {
        super();
        this.pixelCoordinates = pixelCoordinates;
        this.value = value.copy();
        this.locationType = locationType;
        this.subPixelLocationOffset = new double[pixelCoordinates.length];
        this.interpolatedValue = value.createVariable();
        this.interpolatedValue.setZero();
    }

    @Override
    public void localize(float[] position) {
        for (int i = 0; i < position.length; i++) {
            position[i] = (float)(pixelCoordinates[i] + subPixelLocationOffset[i]);
        }
    }

    @Override
    public void localize(double[] position) {
        for (int i = 0; i < position.length; i++) {
            position[i] = pixelCoordinates[i] + subPixelLocationOffset[i];
        }
    }

    @Override
    public float getFloatPosition(int d) {
        return (float)(pixelCoordinates[d] + subPixelLocationOffset[d]);
    }

    @Override
    public double getDoublePosition(int d) {
        return pixelCoordinates[d] + subPixelLocationOffset[d];
    }

    @Override
    public int numDimensions() {
        return pixelCoordinates.length;
    }

    @Override
    public void localize(int[] position) {
        for (int i = 0; i < position.length; i++) {
            position[i] = (int)pixelCoordinates[i];
        }
    }

    @Override
    public void localize(long[] position) {
        for (int i = 0; i < position.length; i++) {
            position[i] = pixelCoordinates[i];
        }
    }

    @Override
    public int getIntPosition(int d) {
        return (int)pixelCoordinates[d];
    }

    @Override
    public long getLongPosition(int d) {
        return pixelCoordinates[d];
    }

    public void setSubPixelLocationOffset(double offset, int d) {
        this.subPixelLocationOffset[d] = offset;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getInterpolatedValue() {
        return interpolatedValue;
    }

    public void setErrorMessage(String error) {
        this.errorMessage = error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }

    @Override
    public int compareTo(final SubPixelLocalization<T> inPeak) {
        /*
         * You're probably wondering why this is negated.
         * It is because Collections.sort() sorts only in the forward direction.
         * I want these to be sorted in the descending order, and the Collections.sort
         * method is the only thing that should ever touch Peak.compareTo.
         * This is faster than sorting, then reversing.
         */
        if (value.compareTo(inPeak.value) == 1) {
            return -1;
        } else if (value.compareTo(inPeak.value) == 0) {
            return 0;
        } else {
            return 1;
        }
    }
}
