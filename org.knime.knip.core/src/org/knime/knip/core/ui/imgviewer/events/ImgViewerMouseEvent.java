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
package org.knime.knip.core.ui.imgviewer.events;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import org.knime.knip.core.ui.event.KNIPEvent;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class ImgViewerMouseEvent implements KNIPEvent {

    private final int m_id;

    private boolean m_consumed;

    /**
     * Full nD position inside the image coordinate space.
     */
    private final boolean m_left;

    private final boolean m_mid;

    private final boolean m_right;

    private final int m_clickCount;

    private final boolean m_isPopupTrigger;

    private final boolean m_isControlDown;

    private final int m_posX;

    private final int m_posY;

    private boolean m_isInside;

    private final MouseEvent m_e;

    private final double m_factorA;

    private final double m_factorB;

    /**
     * @param e
     * @param factors
     * @param imgWidth
     * @param imgHeight
     */
    public ImgViewerMouseEvent(final MouseEvent e, final double[] factors, final int imgWidth, final int imgHeight) {

        m_factorA = factors[0];
        m_factorB = factors[1];

        m_e = e;
        setInside(isInsideImgView(imgWidth, imgHeight));

        m_posX = (int)Math.min(e.getX() / m_factorA, imgWidth);
        m_posY = (int)Math.min(e.getY() / m_factorB, imgHeight);

        m_id = e.getID();
        m_consumed = false;
        m_left = ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0) || (e.getButton() == MouseEvent.BUTTON1);
        m_mid = ((e.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) != 0) || (e.getButton() == MouseEvent.BUTTON2);
        m_right = ((e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0) || (e.getButton() == MouseEvent.BUTTON3);
        m_clickCount = e.getClickCount();
        m_isPopupTrigger = e.isPopupTrigger();
        m_isControlDown = e.isControlDown();
    }

    /*
     * Checks weather the mouse click appeared inside the image view pane or
     * not!
     */
    public boolean isInsideImgView(final long dimA, final long dimB) {

        return !(((m_e.getX() / m_factorA) >= dimA) || ((m_e.getX() / m_factorA) < 0)
                || ((m_e.getY() / m_factorB) >= dimB) || ((m_e.getY() / m_factorB) < 0));
    }

    /**
     * @return
     */
    public boolean wasConsumed() {
        return m_consumed;
    }

    /**
     *
     */
    public void consume() {
        m_consumed = true;
    }

    /**
     * @return
     */
    public int getID() {
        return m_id;
    }

    /**
     * @return
     */
    public boolean isLeftDown() {
        return m_left;
    }

    /**
     * @return
     */
    public boolean isMidDown() {
        return m_mid;
    }

    /**
     * @return
     */
    public boolean isRightDown() {
        return m_right;
    }

    /**
     * @return
     */
    public int getClickCount() {
        return m_clickCount;
    }

    /**
     * @return
     */
    public boolean isPopupTrigger() {
        return m_isPopupTrigger;
    }

    /**
     * @return
     */
    public boolean isControlDown() {
        return m_isControlDown;
    }

    /**
     * @return
     */
    public int getPosX() {
        return m_posX;
    }

    /**
     * @return
     */
    public int getPosY() {
        return m_posY;
    }

    /**
     * @return
     */
    public boolean isInside() {
        return m_isInside;
    }

    /**
     * @param m_isInside
     */
    public void setInside(final boolean m_isInside) {
        this.m_isInside = m_isInside;
    }
}
