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
package org.knime.knip.base.data.img;

import java.awt.image.BufferedImage;

import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.type.Type;

/**
 * Meta data apart from the image itself which is stored by the ImgPlusCell.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgPlusCellMetadata {

    private final long[] m_dimensions;

    private final ImgPlusMetadata m_metadata;

    private final long[] m_min;

    @SuppressWarnings("rawtypes")
    private final Class<? extends Type> m_pixelType;

    private final long m_size;

    private final BufferedImage m_thumbnail;

    /**
     * @param metadata the image metadata
     * @param thumbnail the thumbnail, can be null and will then be created on request
     * @param min the minimum of the image interval (i.e. a offset), if <code>null</code> the minima assumed to be 0 in
     *            each dimension
     * @param dimensions the image dimensions
     * @param size the image size, i.e. the number of pixels
     * @param pixelType the pixel type class
     * @param imgType the image type class
     */
    @SuppressWarnings("rawtypes")
    public ImgPlusCellMetadata(final ImgPlusMetadata metadata, final long size, final long[] min,
                               final long[] dimensions, final Class<? extends Type> pixelType,
                               final BufferedImage thumbnail) {
        m_metadata = metadata;
        m_thumbnail = thumbnail;
        if (min != null) {
            m_min = min.clone();
        } else {
            m_min = null;
        }
        m_dimensions = dimensions.clone();
        m_size = size;
        m_pixelType = pixelType;
    }

    /**
     * @return the dimensions
     */
    public long[] getDimensions() {
        return m_dimensions;
    }

    /**
     * @return the metadata
     */
    public ImgPlusMetadata getMetadata() {
        return m_metadata;
    }

    /**
     * @return the offset of the image, might be <code>null</code> if not set
     */
    public long[] getMinimum() {
        return m_min;
    }

    /**
     * @return the pixelType
     */
    @SuppressWarnings("rawtypes")
    public Class<? extends Type> getPixelType() {
        return m_pixelType;
    }

    /**
     * @return the size
     */
    public long getSize() {
        return m_size;
    }

    /**
     * @return the thumbnail
     */
    public BufferedImage getThumbnail() {
        return m_thumbnail;
    }

}
