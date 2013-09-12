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
package org.knime.knip.base.data;

import java.awt.Image;
import java.awt.RenderingHints;
import java.io.IOException;

import net.imglib2.img.Img;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.meta.Named;
import net.imglib2.meta.Sourced;
import net.imglib2.ops.operation.imgplus.unary.ImgPlusCopy;
import net.imglib2.type.logic.BitType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.Real2GreyColorRenderer;
import org.knime.knip.core.data.algebra.ExtendedPolygon;
import org.knime.knip.core.io.externalization.ExtendedPolygonDeSerializer;
import org.knime.knip.core.util.ImgUtils;

/**
 * Cell containing a Polygon.
 *
 * @author hornm, University of Konstanz
 * @param <T> image type
 * @deprecated Will be removed in future releases
 */

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
@Deprecated
@SuppressWarnings("serial")
public class PolygonCell extends DataCell implements PolygonValue, ImgPlusValue<BitType>, IntervalValue {

    /** Factory for (de-)serializing a ImageRefCell. */
    @SuppressWarnings("rawtypes")
    @Deprecated
    private static class PolygonSerializer implements DataCellSerializer<PolygonCell> {
        /**
         * {@inheritDoc}
         */
        @Override
        public PolygonCell deserialize(final DataCellDataInput input) throws IOException {
            return new PolygonCell(ExtendedPolygonDeSerializer.deserialize(input));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final PolygonCell cell, final DataCellDataOutput output) throws IOException {
            ExtendedPolygonDeSerializer.serialize(cell.getPolygon(), output);
        }

    }

    @Deprecated
    private static final PolygonSerializer SERIALIZER = new PolygonSerializer();

    /**
     * Convenience access member for <code>DataType.getType(StringCell.class)</code>.
     * 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(PolygonCell.class);

    /**
     * Returns the factory to read/write DataCells of this class from/to a DataInput/DataOutput. This method is called
     * via reflection.
     * 
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final PolygonSerializer getCellSerializer() {
        return SERIALIZER;
    }

    /**
     * Returns the preferred value class of this cell implementation. This method is called per reflection to determine
     * which is the preferred renderer, comparator, etc.
     * 
     * @return ImageValue.class;
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return PolygonValue.class;
    }

    private ImgPlus<BitType> m_imgPlus = null;

    private final long[] m_min;

    /* the polygon */
    private final ExtendedPolygon m_poly;

    /**
     * Creates a new cell wrapping a polygon.
     * 
     * @param ref
     * @deprecated Will be removed in future releases
     */
    @Deprecated
    public PolygonCell(final ExtendedPolygon poly) {
        this(poly, null);

    }

    /**
     * @deprecated Will be removed in future releases
     * 
     */
    @Deprecated
    public PolygonCell(final ExtendedPolygon poly, final long[] min) {
        m_poly = poly;
        if (min != null) {
            m_min = min.clone();
        } else {
            m_min = null;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CalibratedSpace getCalibratedSpace() {
        return getImgPlus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getDimensions() {
        final Img<BitType> img = getImgPlus();
        final long[] dims = new long[img.numDimensions()];
        img.dimensions(dims);
        return dims;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlus<BitType> getImgPlus() {
        if (m_imgPlus == null) {
            m_imgPlus = new ImgPlus<BitType>(m_poly.createBitmask());
        }
        return m_imgPlus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlus<BitType> getImgPlusCopy() {
        final ImgPlus<BitType> imgPlus = getImgPlus();
        return new ImgPlusCopy<BitType>().compute(imgPlus, new ImgPlus<BitType>(ImgUtils.createEmptyImg(imgPlus)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getMaximum() {
        final Img<BitType> img = getImgPlus();
        final long[] max = new long[img.numDimensions()];
        img.max(max);
        return max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlusMetadata getMetadata() {
        return getImgPlus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getMinimum() {
        return m_min.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Named getName() {
        return getImgPlus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<BitType> getPixelType() {
        return BitType.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtendedPolygon getPolygon() {
        return m_poly;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Sourced getSource() {
        return getImgPlus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getThumbnail(final RenderingHints renderingHints) {
        final int height = KNIMEKNIPPlugin.getMaximumImageCellHeight();
        final ImgPlus<BitType> mask = getImgPlus();

        // int width = (int) (mask.dimension(0) * (height / mask
        // .dimension(1)));

        return AWTImageTools.<BitType> renderScaledStandardColorImg(mask, new Real2GreyColorRenderer<BitType>(2),
                                                                    (double)height / (double)mask.dimension(1),
                                                                    new long[mask.numDimensions()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_poly.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_poly.toString();
    }

}
