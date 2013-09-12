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
package org.knime.knip.base.data.labeling;

import java.awt.image.BufferedImage;

import org.knime.knip.core.data.img.LabelingMetadata;

/**
 * Meta data apart from the labeling itself which is stored by the LabelingCell.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
class LabelingCellMetadata {

    private final long[] m_dimensions;

    private final LabelingMetadata m_metadata;

    private final long m_size;

    private final BufferedImage m_thumbnail;

    /**
     * @param metadata the labeling metadata
     * @param thumbnail the thumbnail, can be null and will then be created on request
     * @param dimensions the image dimensions
     * @param size the image size, i.e. the number of pixels
     * @param color mapping from label to int color value
     */
    public LabelingCellMetadata(final LabelingMetadata metadata, final long size, final long[] dimensions,
                                final BufferedImage thumbnail) {
        m_metadata = metadata;
        m_thumbnail = thumbnail;
        m_dimensions = dimensions.clone();
        m_size = size;
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
    public LabelingMetadata getLabelingMetadata() {
        return m_metadata;
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
