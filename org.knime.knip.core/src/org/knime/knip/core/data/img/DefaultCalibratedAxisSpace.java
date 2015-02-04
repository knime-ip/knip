package org.knime.knip.core.data.img;

import net.imagej.axis.CalibratedAxis;
import net.imagej.space.AbstractCalibratedSpace;
import net.imagej.space.CalibratedSpace;

/*
 * Simple marker class
 *
 * Depracted: Use concrete implementations of CalibratedAxisSpace
 *
 * @author zinsmaie
 */
@Deprecated
public class DefaultCalibratedAxisSpace extends AbstractCalibratedSpace<CalibratedAxis> implements CalibratedAxisSpace {

    public DefaultCalibratedAxisSpace(final int numDims) {
        super(numDims);
    }

    /**
     * @param obj
     */
    public DefaultCalibratedAxisSpace(final CalibratedSpace<CalibratedAxis> obj) {
        super(obj.numDimensions());
        for (int d = 0; d < obj.numDimensions(); d++) {
            setAxis(obj.axis(d).copy(), d);
        }
    }
}