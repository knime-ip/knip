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
package org.knime.knip.core.ui.imgviewer.overlay.elements;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectInput;

import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.roi.RectangleRegionOfInterest;

import org.knime.knip.core.ui.imgviewer.overlay.OverlayElementStatus;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class RectangleOverlayElement<L extends Comparable<L>> extends AbstractPolygonOverlayElement<L> {

    private Rectangle m_rect;

    private double[] m_origin;

    private double[] m_extend;

    public RectangleOverlayElement(final long[] planePos, final int[] orientation, final String... labels) {
        this(new Rectangle(), planePos, orientation, labels);
    }

    public RectangleOverlayElement() {
        super();
        m_origin = new double[2];
        m_extend = new double[2];
    }

    public RectangleOverlayElement(final Rectangle rect, final long[] planePos, final int[] orientation,
                                   final String... labels) {
        super(planePos, orientation, labels);
        m_rect = rect;
        m_origin = new double[2];
        m_extend = new double[2];
    }

    public RectangleOverlayElement(final long[] min, final long[] max, final long[] planePos, final int[] orientation,
                                   final String... labels) {
        this(new Rectangle((int)min[orientation[0]], (int)min[orientation[1]],
                (int)((max[orientation[0]] - min[orientation[0]]) + 1),
                (int)((max[orientation[1]] - min[orientation[1]]) + 1)), planePos, orientation, labels);
    }

    public void
            setRectangle(final long minExtendX, final long minExtendY, final long maxExtendX, final long maxExtendY) {
        m_poly.reset();

        add(minExtendX, minExtendY);
        add(maxExtendX, maxExtendY);

        m_rect = getBoundingBox();

    }

    @Override
    public boolean containsPoint(final long x, final long y) {
        return m_rect.contains((int)x, (int)y);
    }

    @Override
    public void renderInterior(final Graphics2D g) {
        if ((getStatus() == OverlayElementStatus.ACTIVE) || (getStatus() == OverlayElementStatus.DRAWING)) {
            renderPointInterior(g);
        }

        g.fillRect(m_rect.x, m_rect.y, m_rect.width, m_rect.height);
    }

    @Override
    public void renderOutline(final Graphics2D g) {

        if ((getStatus() == OverlayElementStatus.ACTIVE) || (getStatus() == OverlayElementStatus.DRAWING)) {
            renderPointOutline(g);
        }

        g.drawRect(m_rect.x, m_rect.y, m_rect.width, m_rect.height);
    }

    @Override
    public boolean add(final long x, final long y) {
        if (super.add(x, y)) {
            m_rect = getBoundingBox();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void translate(final int m_selectedIndex, final long x, final long y) {
        m_poly.xpoints[m_selectedIndex] += x;
        m_poly.ypoints[m_selectedIndex] += y;
        m_poly.invalidate();
        m_rect = getBoundingBox();
    }

    @Override
    public void translate(final long x, final long y) {
        super.translate(x, y);
        m_rect = getBoundingBox();
    }

    @Override
    public IterableRegionOfInterest getRegionOfInterest() {
        m_origin[0] = m_rect.x;
        m_origin[1] = m_rect.y;
        m_extend[0] = m_rect.width;
        m_extend[1] = m_rect.height;
        return new RectangleRegionOfInterest(m_origin, m_extend);
    }

    @Override
    protected void renderPointInterior(final Graphics2D g) {
        for (int i = 0; i < m_poly.npoints; i++) {
            g.fillOval(m_poly.xpoints[i] - DRAWING_RADIUS, m_poly.ypoints[i] - DRAWING_RADIUS, 2 * DRAWING_RADIUS,
                       2 * DRAWING_RADIUS);
        }

    }

    @Override
    protected void renderPointOutline(final Graphics2D g) {
        for (int i = 0; i < m_poly.npoints; i++) {
            g.drawOval(m_poly.xpoints[i] - DRAWING_RADIUS, m_poly.ypoints[i] - DRAWING_RADIUS, 2 * DRAWING_RADIUS,
                       2 * DRAWING_RADIUS);
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        m_rect = getBoundingBox();
    }

}
