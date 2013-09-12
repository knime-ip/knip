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
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.imglib2.RealPoint;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.roi.PolygonRegionOfInterest;

import org.knime.knip.core.ui.imgviewer.overlay.OverlayElement2D;
import org.knime.knip.core.ui.imgviewer.overlay.OverlayElementStatus;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class AbstractPolygonOverlayElement<L extends Comparable<L>> extends OverlayElement2D<L> implements
        Externalizable {

    protected static final int DRAWING_RADIUS = 2;

    protected boolean m_isClosed;

    protected Polygon m_poly;

    protected PolygonRegionOfInterest m_roi;

    protected abstract void renderPointOutline(Graphics2D g);

    protected abstract void renderPointInterior(Graphics2D g);

    public abstract void translate(int m_selectedIndex, long x, long y);

    public AbstractPolygonOverlayElement() {
        super();
    }

    public AbstractPolygonOverlayElement(final long[] planePos, final int[] orientation, final String... labels) {
        this(new Polygon(), planePos, orientation, labels);
    }

    public AbstractPolygonOverlayElement(final Polygon poly, final long[] planePos, final int[] orientation,
                                         final String... labels) {
        super(planePos, orientation, labels);
        m_poly = poly;
        m_roi = new PolygonRegionOfInterest();
        for (int i = 0; i < m_poly.npoints; i++) {
            m_roi.addVertex(i, new RealPoint((double)m_poly.xpoints[i], (double)m_poly.ypoints[i]));
        }
    }

    public Polygon getPolygon() {
        return m_poly;
    }

    public void resetPolygon() {
        m_poly.reset();
    }

    public void close() {
        m_isClosed = true;
    }

    @Override
    public boolean add(final long x, final long y) {
        if (m_isClosed) {
            return false;
        }

        m_poly.addPoint((int)x, (int)y);
        m_roi.addVertex(m_roi.getVertexCount(), new RealPoint((double)x, (double)y));

        return true;
    }

    @Override
    public Rectangle getBoundingBox() {
        return m_poly.getBounds();
    }

    @Override
    public void renderInterior(final Graphics2D g) {
        if (m_isClosed) {
            g.fill(m_poly);
        }

        if ((getStatus() == OverlayElementStatus.ACTIVE) || (getStatus() == OverlayElementStatus.DRAWING)) {
            renderPointInterior(g);
        }
    }

    @Override
    public void renderOutline(final Graphics2D g) {

        if ((getStatus() == OverlayElementStatus.ACTIVE) || (getStatus() == OverlayElementStatus.DRAWING)) {
            renderPointOutline(g);
        }

        if (m_isClosed) {
            g.draw(m_poly);
        } else {
            g.drawPolyline(m_poly.xpoints, m_poly.ypoints, m_poly.npoints);
        }
    }

    public int getPointIndexByPosition(final int x, final int y, final int pickingDelta) {

        for (int i = 0; i < m_poly.npoints; i++) {
            if (((m_poly.xpoints[i] - pickingDelta) < x) && (x < (m_poly.xpoints[i] + pickingDelta))
                    && ((m_poly.ypoints[i] - pickingDelta) < y) && (y < (m_poly.ypoints[i] + pickingDelta))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void translate(final long x, final long y) {
        for (int i = 0; i < m_poly.npoints; i++) {
            m_poly.xpoints[i] += x;
            m_poly.ypoints[i] += y;
            m_roi.setVertexPosition(i, new RealPoint((double)m_poly.xpoints[i], (double)m_poly.ypoints[i]));
        }
        m_poly.invalidate();
    }

    @Override
    public IterableRegionOfInterest getRegionOfInterest() {
        return m_roi;
    }

    @Override
    public boolean containsPoint(final long x, final long y) {
        return m_poly.contains((int)x, (int)y);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(m_poly);
        out.writeBoolean(m_isClosed);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        m_poly = (Polygon)in.readObject();
        m_isClosed = in.readBoolean();

        m_roi = new PolygonRegionOfInterest();

        for (int n = 0; n < m_poly.npoints; n++) {
            m_roi.addVertex(n, new RealPoint((double)m_poly.xpoints[n], (double)m_poly.ypoints[n]));
        }
    }

}
