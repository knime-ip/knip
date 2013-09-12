/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.knime.knip.core.data.algebra;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;

/**
 * Class representing a simple point and serves to facilitate simple operations on it.
 * 
 * @author hornm, schoenen University of Konstanz
 */
public class RealVector extends RealPoint implements Cloneable {

    public RealVector(final int nDimensions) {
        super(nDimensions);
    }

    public RealVector(final double[] position) {
        super(position);
    }

    public RealVector(final float[] position) {
        super(position);
    }

    public RealVector(final RealLocalizable localizable) {
        super(localizable);
    }

    /* --- some operations --- */
    /**
     * Addition. The points must be of same dimensionality, no check is made.
     * 
     * @param p
     * @return
     */
    public RealVector add(final RealVector p) {
        final RealVector res = new RealVector(numDimensions());
        for (int i = 0; i < numDimensions(); i++) {
            res.setPosition(getDoublePosition(i) + p.getDoublePosition(i), i);
        }
        return res;
    }

    public RealVector subtract(final RealVector p) {
        final RealVector res = new RealVector(numDimensions());
        for (int i = 0; i < numDimensions(); i++) {
            res.setPosition(getDoublePosition(i) - p.getDoublePosition(i), i);
        }
        return res;
    }

    /**
     * Multiplies a point with a scalar.
     * 
     * @param scalar
     * @return
     */
    public RealVector mapMultiply(final double scalar) {
        final RealVector res = new RealVector(numDimensions());
        for (int i = 0; i < numDimensions(); i++) {
            res.setPosition(getDoublePosition(i) * scalar, i);
        }
        return res;
    }

    public RealVector mapMultiply(final float scalar) {
        final RealVector res = new RealVector(numDimensions());
        for (int i = 0; i < numDimensions(); i++) {
            res.setPosition(getDoublePosition(i) * scalar, i);
        }
        return res;
    }

    /**
     * Length (magnitude)
     */
    public double length() {
        double l = 0;
        for (int i = 0; i < numDimensions(); i++) {
            l += getDoublePosition(i) * getDoublePosition(i);
        }
        return Math.sqrt(l);
    }

    public RealVector norm1() {
        double l = 0;
        for (int i = 0; i < numDimensions(); i++) {
            l += Math.abs(getDoublePosition(i));
        }
        return mapMultiply(1 / l);
    }

    /**
     * Two norm.
     * 
     * @return normalized vector
     */
    public RealVector norm2() {
        return mapMultiply(1 / length());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RealVector clone() {
        return new RealVector(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        long l = Double.doubleToLongBits(getDoublePosition(0));
        int hash = (int)(l ^ (l >>> 32));
        for (int i = 1; i < numDimensions(); i++) {
            l = Double.doubleToLongBits(getDoublePosition(i));
            hash = (31 * hash) + (int)(l ^ (l >>> 32));
        }
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RealVector)) {
            return false;
        }
        final RealVector other = (RealVector)obj;
        if (numDimensions() != other.numDimensions()) {
            return false;
        }
        for (int i = 0; i < numDimensions(); i++) {
            if (getDoublePosition(i) != other.getDoublePosition(i)) {
                return false;
            }
        }
        return true;
    }
}
