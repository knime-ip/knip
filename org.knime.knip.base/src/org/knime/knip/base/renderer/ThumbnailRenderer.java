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
package org.knime.knip.base.renderer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;

import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.renderer.AbstractPainterDataValueRenderer;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;

/**
 * 
 * @param <T> image type
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
@SuppressWarnings("serial")
public class ThumbnailRenderer<T extends RealType<T>> extends AbstractPainterDataValueRenderer {

    public static Key RENDERING_HINT_KEY_METADATA = new Key(1) {
        @Override
        public boolean isCompatibleValue(final Object val) {
            return val instanceof Boolean;
        }

    };

    /**
     * Singleton to render metadata
     */
    public static final ThumbnailRenderer METADATA_RENDERER = new ThumbnailRenderer(new RenderingHints(
            RENDERING_HINT_KEY_METADATA, true)) {
        @Override
        public String getDescription() {
            return "Metadata";
        }
    };

    /**
     * Singleton for the thumbanil rendering.
     */
    public static final ThumbnailRenderer THUMBNAIL_RENDERER = new ThumbnailRenderer();

    private Image m_image = null;

    private final RenderingHints m_renderingHints;

    private String m_text = null;

    protected ThumbnailRenderer() {
        this(null);
    }

    protected ThumbnailRenderer(final RenderingHints renderingHints) {
        m_renderingHints = renderingHints;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Thumbnails";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize() {
        if (m_image != null) {
            return new Dimension(m_image.getWidth(null), m_image.getHeight(null));
        } else {
            return super.getPreferredSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (m_image != null) {
            g.drawImage(m_image, 0, 0, null);
        } else {
            g.drawString(m_text, 10, 10);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof DataCell) {
            final DataCell dc = (DataCell)value;
            if (dc.isMissing()) {
                m_image = null;
                m_text = dc.toString();
            } else if (value instanceof ImgPlusValue) {
                m_image = ((ImgPlusValue)value).getThumbnail(m_renderingHints);
                m_text = dc.toString();
            } else if (value instanceof LabelingValue) {
                m_image = ((LabelingValue)value).getThumbnail(m_renderingHints);
                m_text = dc.toString();
            } else {
                m_image = null;
                m_text = dc.toString();
            }
        }

    }

}
