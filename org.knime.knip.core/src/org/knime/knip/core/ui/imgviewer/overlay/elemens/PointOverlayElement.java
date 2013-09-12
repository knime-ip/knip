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
package org.knime.knip.core.ui.imgviewer.overlay.elemens;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.roi.RectangleRegionOfInterest;

import org.knime.knip.core.ui.imgviewer.overlay.OverlayElement2D;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class PointOverlayElement<L extends Comparable<L>> extends OverlayElement2D<L> {

    private static final int DRAWING_RADIUS = 4;

    private int m_y;

    private int m_x;

    private RectangleRegionOfInterest m_roi;

    public PointOverlayElement() {
        //
    }

    public PointOverlayElement(final int x, final int y, final long[] pos, final int[] orientation,
                               final String... labels) {
        super(pos, orientation, labels);
        m_x = x;
        m_y = y;
        m_roi = new RectangleRegionOfInterest(new double[]{x, y}, new double[]{1, 1});
    }

    @Override
    public void translate(final long deltaX, final long deltaY) {
        m_x += deltaX;
        m_y += deltaY;
    }

    @Override
    public void renderInterior(final Graphics2D g) {
        g.fillOval(m_x - DRAWING_RADIUS, m_y - DRAWING_RADIUS, 2 * DRAWING_RADIUS, 2 * DRAWING_RADIUS);
    }

    @Override
    public void renderOutline(final Graphics2D g) {
        g.drawOval(m_x - DRAWING_RADIUS, m_y - DRAWING_RADIUS, 2 * DRAWING_RADIUS, 2 * DRAWING_RADIUS);
    }

    @Override
    public boolean containsPoint(final long x, final long y) {
        return (m_x == x) && (m_y == y);
    }

    @Override
    public IterableRegionOfInterest getRegionOfInterest() {
        return m_roi;
    }

    @Override
    public boolean add(final long x, final long y) {
        return false;
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(m_x);
        out.writeInt(m_y);

    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        m_x = in.readInt();
        m_y = in.readInt();
        m_roi = new RectangleRegionOfInterest(new double[]{m_x, m_y}, new double[]{1, 1});

    }

    @Override
    public Rectangle getBoundingBox() {
        return new Rectangle(m_x, m_y, 1, 1);
    }

}
