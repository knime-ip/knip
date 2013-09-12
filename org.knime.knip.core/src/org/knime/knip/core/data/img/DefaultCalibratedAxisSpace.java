package org.knime.knip.core.data.img;

import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.DefaultCalibratedSpace;

/*
 * Simple marker class
 *
 * @author zinsmaie
 */
public class DefaultCalibratedAxisSpace extends DefaultCalibratedSpace implements CalibratedAxisSpace {

    public DefaultCalibratedAxisSpace(final int numDims) {
        super(numDims);
    }

    /**
     * @param obj
     */
    public DefaultCalibratedAxisSpace(final CalibratedSpace<CalibratedAxis> obj) {
        super(obj.numDimensions());
        for (int d = 0; d < obj.numDimensions(); d++) {
            setAxis(obj.axis(d), d);
            setCalibration(obj.calibration(d), d);
            setUnit(obj.unit(d), d);
        }
    }
}