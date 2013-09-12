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
package org.knime.knip.core.ui.imgviewer.panels.transfunc;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Each implementation is able to draw a specific kind of TransferFunction.<br>
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author muethingc
 */
public interface TransferFunctionPainter extends MouseListener, MouseMotionListener {

    /**
     * Paint the Function.<br>
     * 
     * @param g2 graphics2d used for painting
     */
    public void paint(final Graphics2D g2);

    /**
     * Paint the function using backbuffer selection.<br>
     * 
     * After the call to this method the caller will then call {@link checkForHilite(Color)} later and thus make each
     * instance check if the painted transfer function has been selected.
     * 
     * @param g2 graphics2d used for painting
     */
    public void paintForSelection(final Graphics2D g2);

    /**
     * Called each time it is necessary to check if a transfer function has been selected.<br>
     * 
     * The passed color will have been retrieved from the area painted to in {@link paintForSelection(Graphics2D)} and
     * thus the implementing class should be able to conclude if it or some part of it has currently the cursor above
     * it.
     * 
     * @param color the color to check for
     * 
     * @return true if the cursor is currently above the function or a part of it, false otherwise
     */
    public boolean checkForHilite(final Color color);

    /**
     * Called to singal the implementing class that something has changed and no more info about the current hiliting is
     * available.<br>
     */
    public void clearHilite();

    /**
     * Called if {@link checkForHilite(Color)} has returned true and should return a cursor that fits the current user
     * interaction of the painted transfer function.<br>
     * 
     * @return a fitting cursor
     */
    public Cursor getCursor();

    /**
     * Add a TransferFunctionChgListener.<br>
     * 
     * @param l the listener to add
     */
    public void addTransferFunctionChgListener(final TransferFunctionChgListener l);

    /**
     * Remove a TransferFunctionChgListener.<br>
     * 
     * @param l the listener to remove
     */
    public void removeTransferFunctionChgListener(final TransferFunctionChgListener l);

}
