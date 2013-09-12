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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.knime.knip.core.ui.imgviewer.ColorDispenser;

/**
 * This class is used to paint a bundle of transfer functions.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author muethingc
 */
public class TransferFunctionBundlePainter implements MouseListener, MouseMotionListener, TransferFunctionChgListener {

    /* The bundle we are currently working on */
    private TransferFunctionBundle m_bundle = null;

    /* All painters used in the current bundle */
    private final LinkedList<TransferFunctionPainter> m_painters = new LinkedList<TransferFunctionPainter>();

    private final Map<TransferFunctionColor, TransferFunctionPainter> m_color2Painter =
            new HashMap<TransferFunctionColor, TransferFunctionPainter>();

    /* the image used for the backbuffer selection */
    private BufferedImage m_backBuffer = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);

    /* all listeners */
    private final EventListenerList m_listener = new EventListenerList();

    /* the cursor that fits the current state */
    private Cursor m_cursor = new Cursor(Cursor.DEFAULT_CURSOR);

    /* The current size of the area we are painting to */
    private Dimension m_area = new Dimension(10, 10);

    /**
     * Create a new instance.
     */
    public TransferFunctionBundlePainter() {
        this(null);
    }

    /**
     * Create a new instance.
     * 
     * @param bundle the bundle of functions to use
     */
    public TransferFunctionBundlePainter(final TransferFunctionBundle bundle) {
        setBundle(bundle);
    }

    private void repaintBackBuffer() {
        final Graphics2D g2 = (Graphics2D)m_backBuffer.getGraphics();

        // paint the background
        g2.setColor(ColorDispenser.BACKGROUND_COLOR);
        g2.fillRect(0, 0, m_backBuffer.getWidth(), m_backBuffer.getHeight());

        g2.setClip(0, 0, m_backBuffer.getWidth(), m_backBuffer.getHeight());
        paint(g2, true);
    }

    public final void paint(final Graphics2D g2) {

        // check if the size of the painting area changed, and if yes
        // create a new fitting backbuffer and paint it
        final Dimension d = new Dimension((int)g2.getClipBounds().getWidth(), (int)g2.getClipBounds().getHeight());

        if (!d.equals(m_area)) {
            m_area = d;
            m_backBuffer =
                    new BufferedImage((int)m_area.getWidth(), (int)m_area.getHeight(), BufferedImage.TYPE_INT_ARGB);
        }

        paint(g2, false);
        repaintBackBuffer();
    }

    private final void paint(final Graphics2D g2, final boolean backBufSel) {
        for (final TransferFunctionPainter p : m_painters) {
            if (backBufSel) {
                p.paintForSelection(g2);
            } else {
                p.paint(g2);
            }
        }
    }

    /**
     * Used to set the currently topmost drawn Tranfer function.<br>
     * 
     * @param color the name of the function to draw
     */
    public final void setTransferFocus(final TransferFunctionColor color) {
        final TransferFunctionPainter p = m_color2Painter.get(color);

        m_painters.remove(p);
        m_painters.add(p);
    }

    /**
     * Set the functions to paint.<br>
     * 
     * @param bundle the bundle of functions
     */
    public final void setBundle(final TransferFunctionBundle bundle) {

        m_bundle = bundle;

        clearBundle();

        if (m_bundle == null) {
            return;
        }

        for (final TransferFunction tf : m_bundle) {
            addPainter(tf);
        }
    }

    private void addPainter(final TransferFunction func) {
        assert m_bundle != null;

        final TransferFunctionPainter p =
                TransferFunctionPainterFactory.create(func, m_bundle.getColorOfFunction(func).getColor());

        p.addTransferFunctionChgListener(this);

        // add for forwarding events
        m_listener.add(MouseListener.class, p);
        m_listener.add(MouseMotionListener.class, p);

        m_painters.add(p);

        m_color2Painter.put(m_bundle.getColorOfFunction(func), p);
    }

    private void clearBundle() {
        for (final TransferFunctionPainter p : m_painters) {
            m_listener.remove(MouseListener.class, p);
            m_listener.remove(MouseMotionListener.class, p);

            p.removeTransferFunctionChgListener(this);
        }

        m_painters.clear();
        m_color2Painter.clear();
    }

    public Cursor getCursor() {
        return m_cursor;
    }

    @Override
    public void transferFunctionChg(final TransferFunctionChgEvent event) {
        fireTransferFunctionChgEvent(event);
    }

    public void addTransferFunctionChgListener(final TransferFunctionChgListener l) {
        m_listener.add(TransferFunctionChgListener.class, l);
    }

    public void removeTransferFunctionChgListener(final TransferFunctionChgListener l) {
        m_listener.remove(TransferFunctionChgListener.class, l);
    }

    private void fireTransferFunctionChgEvent(final TransferFunctionChgEvent event) {
        for (final TransferFunctionChgListener l : m_listener.getListeners(TransferFunctionChgListener.class)) {
            l.transferFunctionChg(event);
        }
    }

    public void addChangeListener(final ChangeListener l) {
        m_listener.add(ChangeListener.class, l);
    }

    public void removeChangeListener(final ChangeListener l) {
        m_listener.remove(ChangeListener.class, l);
    }

    private void fireChangeEvent() {
        for (final ChangeListener l : m_listener.getListeners(ChangeListener.class)) {
            l.stateChanged(new ChangeEvent(this));
        }
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        for (final MouseMotionListener l : m_listener.getListeners(MouseMotionListener.class)) {
            l.mouseDragged(e);
        }
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        final Color color = new Color(m_backBuffer.getRGB(e.getX(), e.getY()));

        Cursor cur = null;

        for (final TransferFunctionPainter p : m_painters) {

            p.clearHilite();
            if (p.checkForHilite(color)) {
                cur = p.getCursor();
            }

        }

        if (cur == null) {
            cur = new Cursor(Cursor.DEFAULT_CURSOR);
        }

        m_cursor = cur;

        for (final MouseMotionListener l : m_listener.getListeners(MouseMotionListener.class)) {
            l.mouseMoved(e);
        }

        fireChangeEvent();
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        for (final MouseListener l : m_listener.getListeners(MouseListener.class)) {
            l.mouseClicked(e);
        }
    }

    @Override
    public void mouseEntered(final MouseEvent e) {
        for (final MouseListener l : m_listener.getListeners(MouseListener.class)) {
            l.mouseEntered(e);
        }
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        for (final MouseListener l : m_listener.getListeners(MouseListener.class)) {
            l.mouseExited(e);
        }
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        for (final MouseListener l : m_listener.getListeners(MouseListener.class)) {
            l.mousePressed(e);
        }
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        for (final MouseListener l : m_listener.getListeners(MouseListener.class)) {
            l.mouseReleased(e);
        }
    }
}
