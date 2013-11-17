package org.knime.knip.core.data.img;

import net.imglib2.meta.AbstractCalibratedSpace;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.CalibratedSpace;

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
            setAxis((CalibratedAxis)obj.axis(d).copy(), d);
        }
    }
}