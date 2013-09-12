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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.knime.core.data.DataCell;
import org.knime.core.data.renderer.AbstractPainterDataValueRenderer;
import org.knime.knip.base.data.PolygonValue;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class PolygonValueRenderer extends AbstractPainterDataValueRenderer {

    public static final PolygonValueRenderer POLYGON_RENDERER = new PolygonValueRenderer();

    /**
     * The preferred height for the Renderer.
     */
    public static final int PREFERRED_HEIGHT = 100;

    /**
     * The preferred width for the Renderer.
     */
    public static final int PREFERRED_WIDTH = 100;

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /* the current polygon to be rendered */
    private PolygonValue m_currentValue = null;

    /* an alternative text, if the image can't be rendered */
    private String m_text;

    /**
     *
     */
    public PolygonValueRenderer() {
        //
    }

    /*
     * Helper to create the BufferedImage from the polygon
     */
    private java.awt.Image createImage() {

        final Polygon p = m_currentValue.getPolygon();
        final Rectangle r = p.getBounds();

        int width = r.width + 1;
        int height = r.height + 1;
        final BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final Graphics g = res.getGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.black);

        // Subtracting the offset
        final int[] xpoints = new int[p.npoints];
        final int[] ypoints = new int[p.npoints];
        for (int i = 0; i < xpoints.length; i++) {
            xpoints[i] = p.xpoints[i] - r.x;
            ypoints[i] = p.ypoints[i] - r.y;
        }

        g.drawPolygon(xpoints, ypoints, p.npoints);

        final double scaleFactor = getScaleFactor(width, height);

        width = (int)Math.round(width * scaleFactor);
        height = (int)Math.round(height * scaleFactor);

        return res.getScaledInstance(width, height, java.awt.Image.SCALE_FAST);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Polygon renderer";
    }

    /**
     * Returns the preferred size for an image.
     * 
     * @see java.awt.Component#getPreferredSize()
     */
    @Override
    public Dimension getPreferredSize() {

        if (m_currentValue == null) {
            return new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        }

        final Rectangle r = m_currentValue.getPolygon().getBounds();
        return new Dimension(r.width, r.height);

    }

    private double getScaleFactor(final int width, final int height) {

        double factor = 1;
        if (m_currentValue == null) {
            return factor;
        }
        final int tablecellwidth = getWidth();
        final int tablecellheight = getHeight();
        // if (tablecellwidth < width || tablecellheight < height) {
        final double ratio = (double)width / (double)height;
        final double tablecellratio = (double)tablecellwidth / (double)tablecellheight;
        // let's scale

        if (tablecellratio > ratio) {
            factor = (double)tablecellheight / (double)height;

        } else if (tablecellratio <= ratio) {
            factor = (double)tablecellwidth / (double)width;
        }

        return factor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);

        if (m_currentValue != null) {
            g.drawImage(createImage(), 0, 0, null);
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
                m_currentValue = null;
                m_text = dc.toString();
            } else if (value instanceof PolygonValue) {
                m_currentValue = (PolygonValue)value;
                m_text = "";
            } else {
                m_text = "Polygon can't be rendered.";
            }

        }

    }

}
