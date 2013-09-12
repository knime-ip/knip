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
package org.knime.knip.core.ui.imgviewer.overlay;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class OverlayElement2D<L extends Comparable<L>> extends OverlayElement<L> implements Externalizable {

    private Rectangle m_boundingBox;

    private long[] m_minExtend;

    private long[] m_maxExtend;

    public OverlayElement2D(final long[] planePos, final int[] orientation, final String... labels) {
        super(planePos, orientation, labels);
        m_minExtend = new long[2];
        m_maxExtend = new long[2];
    }

    public OverlayElement2D() {
        // Serialization constructor
    }

    @Override
    public boolean contains(final long[] pos) {
        if (pos.length < getOrientation().length) {
            return false;
        }
        for (int i = 0; i < pos.length; i++) {
            if (!isOrientation(i) && (pos[i] != m_planePos[i])) {
                return false;
            }
        }
        return containsPoint(pos[getOrientation()[0]], pos[getOrientation()[1]]);
    }

    @Override
    public void translate(final long[] pos) {
        translate(pos[getOrientation()[0]], pos[getOrientation()[1]]);
    }

    @Override
    public void add(final long[] pos) {
        add(pos[getOrientation()[0]], pos[getOrientation()[1]]);
    }

    @Override
    public void renderBoundingBox(final Graphics g) {
        m_boundingBox = getBoundingBox();
        g.drawRect(m_boundingBox.x, m_boundingBox.y, m_boundingBox.width, m_boundingBox.height);
    }

    @Override
    public void renderInterior(final Graphics g, final int[] dims) {
        renderInterior((Graphics2D)g);
    }

    @Override
    public void renderOutline(final Graphics g) {
        renderOutline((Graphics2D)g);
    }

    public abstract Rectangle getBoundingBox();

    public abstract void renderInterior(Graphics2D g);

    public abstract void renderOutline(Graphics2D g);

    public abstract boolean containsPoint(long x, long y);

    public abstract boolean add(long x, long y);

    public abstract void translate(long x, long y);

    @Override
    public Interval getInterval() {
        m_boundingBox = getBoundingBox();
        m_minExtend[0] = m_boundingBox.x;
        m_minExtend[1] = m_boundingBox.y;
        m_maxExtend[0] = (m_boundingBox.width + m_boundingBox.x) - 1;
        m_maxExtend[1] = (m_boundingBox.height + m_boundingBox.y) - 1;
        return new FinalInterval(m_minExtend, m_maxExtend);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);

        out.writeInt(m_minExtend.length);

        for (int i = 0; i < m_minExtend.length; i++) {
            out.writeLong(m_minExtend[i]);
            out.writeLong(m_maxExtend[i]);
        }

        out.writeObject(m_boundingBox);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        final int num = in.readInt();
        m_minExtend = new long[num];
        m_maxExtend = new long[num];

        for (int i = 0; i < num; i++) {
            m_minExtend[i] = in.readLong();
            m_maxExtend[i] = in.readLong();
        }
        m_boundingBox = (Rectangle)in.readObject();
    }
}
