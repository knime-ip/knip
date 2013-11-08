package org.knime.knip.base.nodes.proc.resampler;

import net.imglib2.RealPoint;
import net.imglib2.realtransform.AbstractScale;

/**
 * <em>n</em>-d arbitrary scaling.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class Scale extends AbstractScale {
    final protected Scale inverse;

    protected Scale(final double[] s, final Scale inverse, final RealPoint[] ds) {
        super(s, ds);
        this.inverse = inverse;
    }

    public Scale(final double... s) {
        super(s.clone(), new RealPoint[s.length]);
        final double[] si = new double[s.length];
        final RealPoint[] dis = new RealPoint[s.length];
        for (int d = 0; d < s.length; ++d) {
            si[d] = 1.0 / s[d];
            final RealPoint dd = new RealPoint(s.length);
            dd.setPosition(s[d], d);
            ds[d] = dd;
            final RealPoint ddi = new RealPoint(s.length);
            ddi.setPosition(si[d], d);
        }

        inverse = new Scale(si, this, dis);
    }

    @Override
    public void set(final double... s) {
        for (int d = 0; d < s.length; ++d) {
            this.s[d] = s[d];
            inverse.s[d] = 1.0 / s[d];
            ds[d].setPosition(s[d], d);
            inverse.ds[d].setPosition(inverse.s[d], d);
        }
    }

    @Override
    public Scale inverse() {
        return inverse;
    }

    @Override
    public Scale copy() {
        return new Scale(s);
    }
}
