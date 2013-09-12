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
package org.knime.knip.core.io;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.DefaultCalibratedAxis;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.types.NativeTypes;

/**
 * This class generates random Images.
 * 
 * 
 * @param <T> the type of the image to create
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class ImgGenerator {

    private boolean m_randomSize = false;

    private boolean m_randomFill = false;

    private boolean m_randomType = true;

    private boolean m_randomFactory = true;

    private NativeTypes m_type = null;

    private ImgFactoryTypes m_factory = null;

    private double m_value = 0.0;

    private int m_sizeX;

    private int m_sizeY;

    private int m_sizeZ;

    private int m_sizeChannel;

    private int m_sizeT;

    private int m_minSizeX;

    private int m_minSizeY;

    private int m_minSizeZ;

    private int m_minSizeChannel;

    private int m_minSizeT;

    private List<Long> m_dimList;

    private List<AxisType> m_axisList;

    /**
     * Set up a new generator with a lot of values.
     * 
     * @param randomSize if the size of ALL dimensions should be randomized
     * @param randomFill if the image should be filled with random values, inside the bounds of the image type
     * @param randomType if a random type should be used
     * @param randomFactory if a random factory should be used
     * @param type the type to use for the image, null means choose randomly the first time, regardless of the setting
     *            of random type, after that always use the created type if random factory is false
     * @param factory the factory to use for the image, null means choose randomly the first time, regardless of the
     *            setting of random factory, after that always use the created factory if random factory is false
     * @param value the value to use for filling the image
     * @param minSizeX the minimum size of the x dimensions, a value of 0 means ignore this dimensions
     * @param minSizeY the minimum size of the y dimensions, a value of 0 means ignore this dimensions
     * @param minSizeZ the minimum size of the z dimensions, a value of 0 means ignore this dimensions
     * @param minSizeC the minimum size of the c dimensions, a value of 0 means ignore this dimensions
     * @param minSizeT the minimum size of the t dimensions, a value of 0 means ignore this dimensions
     * @param sizeX the size of the x dimensions, a value of 0 means ignore this dimensions
     * @param sizeY the size of the y dimensions, a value of 0 means ignore this dimensions
     * @param sizeZ the size of the z dimensions, a value of 0 means ignore this dimensions
     * @param sizeC the size of the c dimensions, a value of 0 means ignore this dimensions
     * @param sizeT the size of the t dimensions, a value of 0 means ignore this dimensions
     */
    public ImgGenerator(final boolean randomSize, final boolean randomFill, final boolean randomType,
                        final boolean randomFactory, final NativeTypes type, final ImgFactoryTypes factory,
                        final double value, final int minSizeX, final int minSizeY, final int minSizeZ,
                        final int minSizeC, final int minSizeT, final int sizeX, final int sizeY, final int sizeZ,
                        final int sizeC, final int sizeT) {
        // use setters to ensure bounds
        setRandomSize(randomSize);
        setRandomFill(randomFill);
        setRandomType(randomType);
        setRandomFactory(randomFactory);
        setType(type);
        setFactory(factory);
        setValue(value);
        setSizeX(minSizeX, sizeX);
        setSizeY(minSizeY, sizeY);
        setSizeZ(minSizeZ, sizeZ);
        setSizeChannel(minSizeC, sizeC);
        setSizeT(minSizeT, sizeT);
    }

    /**
     * A convenience constructor to set up an image with the following values.<br>
     * 
     * randomSize = false<br>
     * randomFill = false<br>
     * randomType = true<br>
     * randomFactory = true<br>
     * type = null<br>
     * factory = null<br>
     * value = 0.0<br>
     * sizeX = 1,1<br>
     * sizeY = 1,1<br>
     * sizeZ = 0,0<br>
     * sizeC = 0,0<br>
     * sizeT = 0,0<br>
     * 
     * {@inheritDoc}
     * 
     * @see Object#ImageGeneratorNodeGenerator()
     */
    public ImgGenerator() {
        this(false, false, true, true, null, null, 0.0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0);
    }

    /**
     * Create a new imgage with using the current settings.
     * 
     * @return the new image
     */
    @SuppressWarnings("unchecked")
    public final <T extends NativeType<T> & RealType<T>> ImgPlus<T> nextImage() {

        // Set up new utils
        m_dimList = new ArrayList<Long>();
        m_axisList = new ArrayList<AxisType>();

        ImgFactoryTypes facType;

        // select a factory
        if (m_factory == null) {
            m_factory = ImgFactoryTypes.values()[randomBoundedInt(ImgFactoryTypes.values().length - 2)];
        }

        if (m_randomFactory) {
            facType = ImgFactoryTypes.values()[randomBoundedInt(ImgFactoryTypes.values().length - 2)];
        } else {
            facType = m_factory;
        }

        final ImgFactory<T> imgFac = ImgFactoryTypes.getImgFactory(facType);

        // process all dimensions
        processDimension(m_minSizeX, m_sizeX, "X");
        processDimension(m_minSizeY, m_sizeY, "Y");
        processDimension(m_minSizeZ, m_sizeZ, "Z");
        processDimension(m_minSizeChannel, m_sizeChannel, "Channel");
        processDimension(m_minSizeT, m_sizeT, "Time");

        final long[] dims = new long[m_dimList.size()];

        for (int d = 0; d < m_dimList.size(); d++) {
            dims[d] = m_dimList.get(d);
        }

        // Type of img is selected
        NativeTypes type;

        if (m_type == null) {
            m_type = NativeTypes.values()[randomBoundedInt(NativeTypes.values().length - 1)];
        }

        if (m_randomType) {
            type = NativeTypes.values()[randomBoundedInt(NativeTypes.values().length - 1)];
        } else {
            type = m_type;
        }

        // create the actual image
        final T val = (T)NativeTypes.getTypeInstance(type);
        final Img<T> img = imgFac.create(dims, val);

        // fill the image
        final Cursor<T> cursor = img.cursor();
        while (cursor.hasNext()) {
            cursor.fwd();

            double result;

            if (m_randomFill) {
                double sign = 1;

                if (val.getMinValue() < 0) {
                    if (Math.random() > 0.5) { // ~50% negative
                        sign = -1;
                    }
                }

                if (type.equals(NativeTypes.DOUBLETYPE) || type.equals(NativeTypes.FLOATTYPE)) {
                    //random value between -1 and 1
                    result = Math.random() * sign;
                } else {
                    //random value in type range
                    result = Math.random() * val.getMaxValue() * sign;
                }
            } else {
                result = m_value;
            }

            cursor.get().setReal(result);
        }

        final ImgPlus<T> imgPlus = new ImgPlus<T>(img);

        int d = 0;
        for (final AxisType a : m_axisList) {
            imgPlus.setAxis(new DefaultCalibratedAxis(a), d++);
        }

        return imgPlus;
    }

    /**
     * Add this dimensions to the list of axes and dims.
     * 
     * @param minVal minimum size of the dimension allways smaller or equal val
     * @param val the value, 0 means ignore
     * @param label the label to use for the axis
     */
    private void processDimension(final int minVal, final int val, final String label) {

        double dimVal = (val - minVal);
        if (m_randomSize) {
            dimVal *= Math.random();
        }
        dimVal += minVal;

        dimVal = Math.round(dimVal);

        // ignore empty dimensions
        if (dimVal != 0) {
            m_dimList.add((long)dimVal);
            m_axisList.add(Axes.get(label));
        }
    }

    private int randomBoundedInt(final int bound) {
        return (int)Math.round(Math.random() * bound);
    }

    /**
     * Sets the sizeX for this instance.
     * 
     * @param minSizeX the required minimum size for X
     * @param sizeX The sizeX.
     */
    public final void setSizeX(final int minSizeX, final int sizeX) {
        m_sizeX = sizeX;
        m_minSizeX = minSizeX;
    }

    /**
     * Sets the sizeY for this instance.
     * 
     * @param minSizeY the required minimum size for Y
     * @param sizeY The sizeY.
     */
    public final void setSizeY(final int minSizeY, final int sizeY) {
        m_sizeY = sizeY;
        m_minSizeY = minSizeY;
    }

    /**
     * Sets the sizeZ for this instance.
     * 
     * @param minSizeZ the required minimum size for Z
     * @param sizeZ The sizeZ.
     */
    public final void setSizeZ(final int minSizeZ, final int sizeZ) {
        m_sizeZ = sizeZ;
        m_minSizeZ = minSizeZ;
    }

    /**
     * Sets the sizeChannel for this instance.
     * 
     * @param minSizeChannel the required minimum size for Channel
     * @param sizeChannel The sizeChannel.
     */
    public final void setSizeChannel(final int minSizeChannel, final int sizeChannel) {
        m_sizeChannel = sizeChannel;
        m_minSizeChannel = minSizeChannel;
    }

    /**
     * Sets the sizeT for this instance.
     * 
     * @param minSizeT the required minimum size for T
     * @param sizeT The sizeT.
     */
    public final void setSizeT(final int minSizeT, final int sizeT) {
        m_sizeT = sizeT;
        m_minSizeT = minSizeT;
    }

    /**
     * Sets whether or not this instance is randomSize.
     * 
     * @param randomSize The randomSize.
     */
    public final void setRandomSize(final boolean randomSize) {
        m_randomSize = randomSize;
    }

    /**
     * Sets whether or not this instance is randomFill.
     * 
     * @param randomFill The randomFill.
     */
    public final void setRandomFill(final boolean randomFill) {
        m_randomFill = randomFill;
    }

    /**
     * Sets the type for this instance.
     * 
     * A value of null means choose a random type, regardless of the randomType setting.
     * 
     * @param type The type.
     */
    public final void setType(final NativeTypes type) {
        m_type = type;
    }

    /**
     * Sets the factory for this instance.
     * 
     * A value of null means choose a random factory, regardless of the randomFactory setting.
     * 
     * @param factory The factory.
     */
    public final void setFactory(final ImgFactoryTypes factory) {
        m_factory = factory;
    }

    /**
     * Sets the value for this instance.
     * 
     * @param value The value.
     */
    public final void setValue(final double value) {
        m_value = value;
    }

    /**
     * Sets whether or not this instance is randomType.
     * 
     * @param randomType The randomType.
     */
    public final void setRandomType(final boolean randomType) {
        m_randomType = randomType;
    }

    /**
     * Sets whether or not this instance is randomFactory.
     * 
     * @param randomFactory The randomFactory.
     */
    public final void setRandomFactory(final boolean randomFactory) {
        m_randomFactory = randomFactory;
    }
}
