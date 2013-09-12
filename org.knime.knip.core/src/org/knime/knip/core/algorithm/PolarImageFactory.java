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
package org.knime.knip.core.algorithm;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.util.CursorTools;

/**
 * Class which helps to retrieve a RimImage from a given grayscale image at a given position.
 * 
 * 
 * @author hornm
 * 
 */

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class PolarImageFactory<T extends RealType<T>> {

    private RandomAccessible<T> m_interval;

    private int m_angularDimension;

    private long m_numAngles;

    /**
     * Creates a new PolarImageFactory. Note that in some cases a OutOfBoundFactory needs to be defined for the cursor!
     * If not, an ArrayIndexOutOfBoundsException will be thrown.
     * 
     * @param interval the source 'image'
     * 
     * @param cursor the cursor which has to point on a 2D Image, requiering an OutOfBoundStrategy
     * @param radius the radius of the rim images obtained with this RimImageFactory
     * 
     */

    public PolarImageFactory(final RandomAccessible<T> interval) {
        this(interval, -1, -1);
    }

    /**
     * Creates a new PolarImageFactory. Note that in some cases a OutOfBoundFactory needs to be defined for the cursor!
     * If not, an ArrayIndexOutOfBoundsException will be thrown.
     * 
     * Here, angular information, coded in the given dimension (dividing the circle in the according number of parts),
     * is used additionally. For example, the third dimension has a dimension size of 4, then at the angle of, let's
     * say, 90° the second plane will be used to get the pixel values for the polar image.
     * 
     * @param interval
     * @param angularDimension the dimension which holds angular information (must exist, if not an
     *            ArrayIndexOutOfBounds will be thrown)
     * @param numAng the number of angles in the angular dimensions (i.e. the it's dimension size)
     * 
     */

    public PolarImageFactory(final RandomAccessible<T> interval, final int angularDimension, final long numAng) {
        m_interval = interval;
        m_angularDimension = angularDimension;
        m_numAngles = numAng;

    }

    /**
     * Creates a PolarImage at the position (x,y), i.e. retrieving the pixels at circles with different radiuses, put
     * together to an image.
     * 
     * @param center
     * 
     * @param radius the radius of the polar images obtained with this PolarImageFactory, equivalent to the with of the
     *            resulting image
     * 
     * @param length the number of points to be saved a on circle -> is equivalent to the later length/height of the
     *            RimImage
     * 
     * @return the polar image
     * 
     */
    public Img<T> createPolarImage(final long[] center, final int radius, final int length) {

        @SuppressWarnings("rawtypes")
        final Img<T> res =
                new ArrayImgFactory().create(new int[]{radius, length}, m_interval.randomAccess().get()
                        .createVariable());

        return createPolarImage(center, length, res);

    }

    /**
     * Creates a PolarImage at the position (x,y), i.e. retrieving the pixels at circles with different radiuses, put
     * together to an image.
     * 
     * @param center
     * @param radius the radius of the polar images obtained with this PolarImageFactory, equivalent to the with of the
     *            resulting image
     * 
     * @param length the number of points to be saved a on circle -> is equivalent to the later length/height of the
     *            RimImage
     * 
     * @return the polar image
     * 
     */
    public Img<T> createPolarImage(final double[] center, final int radius, final int length) {

        final long[] centroid = new long[center.length];

        for (int l = 0; l < center.length; l++) {
            centroid[l] = (long)center[l];
        }

        return createPolarImage(centroid, radius, length);

    }

    /**
     * Creates a PolarImage at the position (x,y), i.e. retrieving the pixels at circles with different radiuses, put
     * together to an image.
     * 
     * @param center
     * 
     * @param length the number of points to be saved a on circle -> is equivalent to the later length/height of the
     *            RimImage
     * @param resImg writes the result into resImg, you have to make sure that the result image has the right dimensions
     *            (radius x length)
     * 
     * @return the polar image
     * 
     */
    public Img<T> createPolarImage(final long[] center, final int length, final Img<T> resImg) {

        if (m_angularDimension != -1) {
            return createPolarImage(center, length, m_angularDimension, m_numAngles, resImg);
        }

        final RandomAccess<T> srcRA = m_interval.randomAccess();

        CursorTools.setPosition(srcRA, center);

        int tmpx, tmpy;
        final Cursor<T> polarC = resImg.localizingCursor();
        double angle;
        while (polarC.hasNext()) {
            polarC.fwd();
            angle = ((double)polarC.getIntPosition(1) / (double)length) * 2 * Math.PI;
            tmpx = (int)(Math.round(polarC.getLongPosition(0) * Math.cos(angle)) + center[0]);
            tmpy = (int)(-Math.round(polarC.getIntPosition(0) * Math.sin(angle)) + center[1]);
            srcRA.setPosition(tmpx, 0);
            srcRA.setPosition(tmpy, 1);
            polarC.get().set(srcRA.get());
        }

        return resImg;

    }

    /*
     * Creates a PolarImage at the position (x,y), i.e. retrieving the
     * pixels at circles with different radiuses, put together to an image.
     * In contrast to the
     * <code>createPolarImage(center,length)</code>-method, here angular
     * information, coded in the third dimension (dividing the circle in the
     * according number of parts), is used additionally. For example, the
     * third dimension has a dimension size of 4, then at the angle of,
     * let's say, 90° the second plane will be used to get the pixel values
     * for the polar image.
     *
     * @param center
     *
     * @param length the number of points to be saved a on circle -> is
     * equivalent to the later length/height of the RimImage
     *
     * @param angularDimension the dimension, the different angles
     *
     * @return the polar image
     */
    private Img<T> createPolarImage(final long[] center, final int length, final int angularDimension,
                                    final long numAngles, final Img<T> resImg) {

        final RandomAccess<T> srcRA = m_interval.randomAccess();

        int tmpx, tmpy;
        final Cursor<T> polarC = resImg.localizingCursor();
        double angle;
        int angID;

        while (polarC.hasNext()) {
            polarC.fwd();
            angle = ((double)polarC.getIntPosition(1) / (double)length) * 2 * Math.PI;
            tmpx = (int)(Math.round(polarC.getLongPosition(0) * Math.cos(angle)) + center[0]);
            tmpy = (int)(-Math.round(polarC.getIntPosition(0) * Math.sin(angle)) + center[1]);

            angID = (int)((Math.round((angle / (2 * Math.PI)) * numAngles)) % numAngles);

            srcRA.setPosition(tmpx, 0);
            srcRA.setPosition(tmpy, 1);
            srcRA.setPosition(angID, angularDimension);

            polarC.get().set(srcRA.get());

        }

        return resImg;

    }
}
