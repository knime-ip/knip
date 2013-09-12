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
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class FreeFormOverlayElement<L extends Comparable<L>> extends AbstractPolygonOverlayElement<L> {

    private boolean m_renderInterior;

    public FreeFormOverlayElement() {
        //
    }

    /**
     * 
     */
    public FreeFormOverlayElement(final long[] pos, final int[] orientation, final boolean renderInterior,
                                  final String... labels) {
        super(pos, orientation, labels);
        m_renderInterior = renderInterior;
    }

    @Override
    public void renderInterior(final Graphics2D g) {
        if (m_renderInterior) {
            super.renderInterior(g);
        }
    }

    @Override
    public void translate(final int m_selectedIndex, final long x, final long y) {
        //
    }

    @Override
    protected void renderPointInterior(final Graphics2D g) {
        // no points are rendered
    }

    @Override
    protected void renderPointOutline(final Graphics2D g) {
        // Nothing to do here
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeBoolean(m_renderInterior);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        m_renderInterior = in.readBoolean();
    }

    @Override
    public boolean containsPoint(final long x, final long y) {

        if (m_renderInterior) {
            return super.containsPoint(x, y);
        } else {
            for (int n = 0; n < m_poly.xpoints.length; n++) {
                if ((x == m_poly.xpoints[n]) && (y == m_poly.ypoints[n])) {
                    return true;
                }
            }
        }

        return false;
    }
}
