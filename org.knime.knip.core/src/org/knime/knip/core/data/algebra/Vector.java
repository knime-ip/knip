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

import net.imglib2.Localizable;
import net.imglib2.Point;

/**
 * Class representing a simple point in the image. It mainly serves to facilitate simple operations on the coordinates.
 * 
 * @author hornm, schoenen University of Konstanz
 * 
 */

public class Vector extends Point implements Cloneable {

    public Vector(final int nDimensions) {
        super(nDimensions);
    }

    public Vector(final long[] position) {
        super(position);
    }

    public Vector(final int[] position) {
        super(position);
    }

    public Vector(final Localizable localizable) {
        super(localizable);
    }

    /**
     * Length (magnitude)
     */
    public long length() {
        long l = 0;
        // TODO overflows???
        for (int i = 0; i < numDimensions(); i++) {
            l += getLongPosition(i) * getLongPosition(i);
        }
        return (long)Math.sqrt(l);
    }

    public Vector norm1() {
        long l = 0;
        for (int i = 0; i < numDimensions(); i++) {
            l += Math.abs(getLongPosition(i));
        }
        return mapMultiply(1 / l);
    }

    /**
     * Two norm.
     * 
     * @return normalized vector
     */
    public Vector norm2() {
        return mapMultiply(1 / length());
    }

    /**
     * Addition. The points must be of same dimensionality, no check is made.
     * 
     * @param p
     * @return the resulting point
     */
    public Vector add(final Vector p) {
        final Vector res = new Vector(numDimensions());
        for (int i = 0; i < numDimensions(); i++) {
            res.setPosition(getLongPosition(i) + p.getLongPosition(i), i);
        }
        return res;
    }

    public Vector subtract(final Vector p) {
        final Vector res = new Vector(numDimensions());
        for (int i = 0; i < numDimensions(); i++) {
            res.setPosition(getLongPosition(i) - p.getLongPosition(i), i);
        }
        return res;
    }

    /**
     * Multiplies a point with a scalar.
     * 
     * @param scalar
     * @return the resulting point
     */
    public Vector mapMultiply(final long scalar) {
        final Vector res = new Vector(numDimensions());
        for (int i = 0; i < numDimensions(); i++) {
            res.setPosition(getLongPosition(i) * scalar, i);
        }
        return res;
    }

    public Vector mapMultiply(final int scalar) {
        final Vector res = new Vector(numDimensions());
        for (int i = 0; i < numDimensions(); i++) {
            res.setPosition(getLongPosition(i) * scalar, i);
        }
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector clone() {
        return new Vector(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        long l = getLongPosition(0);
        int hash = (int)(l ^ (l >>> 32));
        for (int i = 1; i < numDimensions(); i++) {
            l = getLongPosition(i);
            hash = (31 * hash) + (int)(l ^ (l >>> 32));
        }
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Vector)) {
            return false;
        }
        final Vector other = (Vector)obj;
        if (numDimensions() != other.numDimensions()) {
            return false;
        }
        for (int i = 0; i < numDimensions(); i++) {
            if (getLongPosition(i) != other.getLongPosition(i)) {
                return false;
            }
        }
        return true;
    }
}
