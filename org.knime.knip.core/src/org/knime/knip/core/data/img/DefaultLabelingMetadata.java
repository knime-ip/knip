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
 * ---------------------------------------------------------------------
 *
 * Created on Sep 4, 2013 by dietzc
 */
package org.knime.knip.core.data.img;

import net.imagej.Sourced;
import net.imagej.axis.CalibratedAxis;
import net.imagej.space.CalibratedSpace;
import net.imglib2.ops.util.MetadataUtil;

import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTable;
import org.scijava.Named;

/**
 * TODO Auto-generated
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DefaultLabelingMetadata extends AbstractGeneralMetadata implements LabelingMetadata {

    private LabelingColorTable m_colorTable;

    /**
     * Creates a new DefaultLabelingMetadata object.
     *
     * @param numDims the number of dimensions
     * @param colorTable the color table
     */
    public DefaultLabelingMetadata(final int numDims, final LabelingColorTable colorTable) {
        super(numDims);
        m_colorTable = colorTable;
    }

    /**
     * TODO
     *
     * @param metadata
     */
    public DefaultLabelingMetadata(final LabelingMetadata metadata) {
        super(metadata.numDimensions());
        m_colorTable = metadata.getLabelingColorTable().copy();
        MetadataUtil.copyName(metadata, this);
        MetadataUtil.copySource(metadata, this);
        MetadataUtil.copyTypedSpace(metadata, this);
    }

    /**
     * Creates a new DefaultLabelingMetadata object. All given parameters will be copied.
     *
     * @param space
     * @param named
     * @param sourced
     * @param mapping if <code>null</code> an empty {@link DefaultLabelingColorTable} will be used.
     */
    public DefaultLabelingMetadata(final CalibratedSpace<CalibratedAxis> space, final Named named,
                                   final Sourced sourced, final LabelingColorTable mapping) {
        super(space.numDimensions());
        if (mapping == null) {
            m_colorTable = new DefaultLabelingColorTable();
        } else {
            m_colorTable = mapping.copy();
        }
        MetadataUtil.copyName(named, this);
        MetadataUtil.copySource(sourced, this);
        MetadataUtil.copyTypedSpace(space, this);
    }

    /**
     * {@inheritDoc}
     */
    public LabelingColorTable getLabelingColorTable() {
        return m_colorTable;
    }

    /**
     * {@inheritDoc}
     */
    public void setLabelingColorTable(final LabelingColorTable mapping) {
        m_colorTable = mapping;
    }

}
