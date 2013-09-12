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

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.view.Views;

import org.knime.knip.core.algorithm.BresenhamAlgorithm;
import org.knime.knip.core.util.PolygonTools;

/**
 * Adds more functionality to the java.awt.Polygon class.
 * 
 * 
 * @author hornm
 * 
 */
@Deprecated
public class ExtendedPolygon extends Polygon implements Iterable<int[]> {

    private static final long serialVersionUID = 4797555800668312421L;

    private Img<BitType> m_mask = null;

    private long[] m_center;

    /**
     *
     */
    public ExtendedPolygon() {
        super();
    }

    /**
     * Wraps the given polygon.
     * 
     * @param poly polygon to wrap
     */
    public ExtendedPolygon(final Polygon poly) {
        super(poly.xpoints, poly.ypoints, poly.npoints);
    }

    /**
     * 
     * @return the center of the polygons bounding box
     */
    public long[] getBoundingBoxCenter() {
        final Rectangle r = getBounds();

        return new long[]{r.x + (r.width / 2), r.y + (r.height / 2)};

    }

    /**
     * The center of the polygon. If no center was set, the bounding box center will be returned (not a copy!)
     * 
     * @return
     */

    public long[] getCenter() {
        if (m_center == null) {
            return getBoundingBoxCenter();
        }
        return m_center;
    }

    /**
     * Sets the new center of the polygon. No checks are made, whether it lies outside of the contour and, furthermore,
     * NO copy is made!
     * 
     * @param p
     */

    public void setCenter(final long[] p) {
        m_center = p.clone();
    }

    /**
     * Creates the bitmask.
     * 
     * @return
     */
    public Img<BitType> createBitmask() {
        if (m_mask != null) {
            return m_mask;
        }
        final Rectangle r = getBounds();
        final int w = r.width;
        final int h = r.height;
        m_mask = new ArrayImgFactory<BitType>().create(new int[]{w, h}, new BitType());
        final Cursor<BitType> c = m_mask.localizingCursor();

        while (c.hasNext()) {
            c.fwd();
            if (contains(new Point(r.x + c.getIntPosition(0), r.y + c.getIntPosition(1)))) {
                c.get().set(true);
            }
        }

        return m_mask;

    }

    /**
     * Return the number of included points in the contour
     * 
     * @return number of points
     */

    public int length() {
        return npoints;
    }

    /**
     * Return the point at the specified index.
     * 
     * @param index
     * @return
     */

    public int[] getPointAt(final int index) {
        if ((index < 0) || (index >= npoints)) {
            return null;
        } else {
            return new int[]{xpoints[index], ypoints[index]};
        }
    }

    /**
     * Determines the normal vector of the point at the given index.
     * 
     * @param index
     * @return
     */
    public float[] getNormalVecAtPoint(final int index) {
        final int i = index + length();
        Vector n;

        n = new Vector(getPointAt((i - 3) % length())).mapMultiply(-1);
        n = n.add(new Vector(getPointAt((i - 2) % length())).mapMultiply(-1));
        n = n.add(new Vector(getPointAt((i - 1) % length())).mapMultiply(-1));
        n = n.add(new Vector(getPointAt((i + 1) % length())));
        n = n.add(new Vector(getPointAt((i + 2) % length())));
        n = n.add(new Vector(getPointAt((i + 3) % length())));
        n = new Vector(new int[]{-n.getIntPosition(1), n.getIntPosition(0)});
        final float[] res = new float[n.numDimensions()];
        n.localize(res);
        return res;
    }

    /**
     * Calculates the angle of the normal vector of the point at the given index.
     * 
     * @param index
     * @return
     */

    public double getAngleAtPoint(final int index) {
        float[] n = getNormalVecAtPoint(index);
        // orthogonal
        n = new float[]{-n[1], n[0]};
        final double ang = Math.atan2(n[0], n[1]);
        return (Math.abs(ang) != ang ? (2 * Math.PI) + ang : ang);
    }

    /**
     * Resamples the polygon with the given maximum number of points.
     * 
     * @param maxNumPoints
     * 
     * @param numPoints
     * @return the new resampled contour
     */
    public ExtendedPolygon resamplePolygon(final int maxNumPoints) {

        // collect all possible points
        final List<int[]> allPoints = new java.util.Vector<int[]>();
        for (int i = 1; i < npoints; i++) {
            final int[] p1 = getPointAt(i - 1);
            final int[] p2 = getPointAt(i);
            final int[][] tmp = BresenhamAlgorithm.rasterizeLine(p1, p2);
            for (final int[] p : tmp) {
                allPoints.add(p);
            }
        }

        final int[] p1 = getPointAt(length() - 1);
        final int[] p2 = getPointAt(0);
        final int[][] tmp = BresenhamAlgorithm.rasterizeLine(p1, p2);
        for (final int[] p : tmp) {
            allPoints.add(p);
        }

        final double stepsize = (Math.max(1.0, (double)allPoints.size() / (double)(maxNumPoints + 1)));
        final ExtendedPolygon res = new ExtendedPolygon();

        for (double i = 0; i < (allPoints.size() - (2 * stepsize)); i += stepsize) {
            final int[] p = allPoints.get((int)Math.round(i));
            res.addPoint(p[0], p[1]);
        }

        return res;

    }

    /**
     * Calculate the overlap of two signatures. 0 - no overlap, 1- identical
     */
    public double overlap(final ExtendedPolygon p2) {
        final Rectangle r1 = getBounds();
        final Rectangle r2 = p2.getBounds();

        if (r1.intersects(r2)) {

            int overlapPix = 0;
            int mask1Pix = 0;
            int mask2Pix = 0;
            final Img<BitType> mask1 = createBitmask();
            final Img<BitType> mask2 = p2.createBitmask();

            final Cursor<BitType> mask1Cur = mask1.localizingCursor();
            final RandomAccess<BitType> mask2RA = Views.extendValue(mask2, new BitType(false)).randomAccess();

            final int[] pos = new int[2];
            while (mask1Cur.hasNext()) {
                mask1Cur.fwd();
                if (mask1Cur.get().get()) {
                    pos[0] = (r1.x + mask1Cur.getIntPosition(0)) - r2.x;
                    pos[1] = (r1.y + mask1Cur.getIntPosition(1)) - r2.y;
                    mask2RA.setPosition(pos);
                    if (mask2RA.get().get()) {
                        overlapPix++;
                    }
                    mask1Pix++;
                }
            }

            final Cursor<BitType> c = mask2.cursor();
            while (c.hasNext()) {
                c.fwd();
                if (c.get().get()) {
                    mask2Pix++;
                }
            }

            return (double)overlapPix / Math.min(mask1Pix, mask2Pix);

        }

        return 0;
    }

    /**
     * 
     * An iterator over the points.
     * 
     * @return the iterator
     */

    @Override
    public Iterator<int[]> iterator() {
        return new ContourIterator();
    }

    protected class ContourIterator implements Iterator<int[]> {
        private int m_i = -1;

        @Override
        public boolean hasNext() {
            return m_i < (length() - 1);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int[] next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            m_i++;
            return new int[]{xpoints[m_i], ypoints[m_i]};
        }

    }

    /**
     * Shows images for debugging purposes: the lines along the normal vectors at each point of the contour, ...
     * 
     * @param srcImg
     */
    public <T extends RealType<T>> void showDebugImage(final Img<T> srcImg) {

        final Img<ByteType> res = new ArrayImgFactory<ByteType>().create(srcImg, new ByteType());

        final RandomAccess<ByteType> resRA = res.randomAccess();

        final RandomAccess<T> srcCur = srcImg.randomAccess();

        final int samplePoints = 100;
        final int radius = 20;
        final Img<T> cImg =
                srcImg.factory().create(new int[]{radius * 2, samplePoints}, srcImg.firstElement().createVariable());
        final RandomAccess<T> cImgRA = cImg.randomAccess();

        int[][] line;
        final ExtendedPolygon tmp = resamplePolygon(samplePoints);
        for (int i = 0; i < tmp.length(); i++) {
            line = PolygonTools.getLineAt(tmp.getPointAt(i), tmp.getNormalVecAtPoint(i), radius);
            int j = 0;
            for (final int[] p : line) {
                resRA.setPosition(p[0], 0);
                resRA.setPosition(p[1], 1);
                resRA.get().set((byte)50);

                srcCur.setPosition(p[0], 0);
                srcCur.setPosition(p[1], 1);

                cImgRA.setPosition(j, 0);
                cImgRA.setPosition(i, 1);

                cImgRA.get().setReal(srcCur.get().getRealDouble());
                j++;

            }
            resRA.setPosition(tmp.getPointAt(i)[0], 0);
            resRA.setPosition(tmp.getPointAt(i)[1], 1);
            resRA.get().set((byte)128);

        }
    }

    /**
     * @return
     */
    public void getCenterOfGravityAndUpdate(final int[] currentPos) {

        int x = 0;
        int y = 0;

        double total = 0;
        for (final int[] p : this) {
            x += p[0];
            y += p[1];

            total++;
        }

        currentPos[0] = (int)Math.round(x / (total));
        currentPos[1] = (int)Math.round(y / (total));

    }

}
