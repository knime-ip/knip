/* Copyright (c) 2006-2007 Timothy Wall, All Rights Reserved
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * http://sourceforge.net/projects/furbelow/
 */
package org.knime.knip.core.util.waitingindicator.libs;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Prevents mouse and key input to a {@link JComponent} or {@link JFrame},
 * while dimming the component and displaying a wait cursor.
 */
public class WaitIndicator extends AbstractComponentDecorator implements KeyEventDispatcher {

    /** Place the wait indicator over the entire frame. */
    public WaitIndicator(final JFrame frame) {
        this(frame.getLayeredPane());
    }

    /** Place the wait indicator over the given component. */
    public WaitIndicator(final JComponent target) {
        super(target);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
        getPainter().addMouseListener(new MouseAdapter() { });
        getPainter().addMouseMotionListener(new MouseMotionAdapter() { });
        getPainter().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    /** Remove the wait indicator. */
    @Override
    public void dispose() {
        super.dispose();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
    }

    /** Consume events targeted at our target component.  Return true to
     * consume the event.
     */
    public boolean dispatchKeyEvent(final KeyEvent e) {
        return SwingUtilities.isDescendingFrom(e.getComponent(), getComponent());
    }

    /** The default dims the blocked component. */
    @Override
    public void paint(Graphics g) {
        Color bg = getComponent().getBackground();
        Color c = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 128);
        Rectangle r = getDecorationBounds();
        g = g.create();
        g.setColor(c);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.dispose();
    }
}
