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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.Timer;

/* Gradually fade the display, then show a spinning dial indicator.
 */
public class SpinningDialWaitIndicator extends WaitIndicator implements ActionListener {

    private static final int MAX_SIZE = 64;
    private static final int FADE_INTERVAL = 1000/24;
    private static final int FADE_THRESHOLD = 192;
    // size of margins when space is sufficiently large
    private static final int MARGIN = 8;
    // margins take up 1/MARGIN_FRACTION when space is limited
    private static final int MARGIN_FRACTION = 8;

    private Timer timer;
    private int fade;
    private int verticalOffset;
    private SpinningDial dial;
    private String text;

    public SpinningDialWaitIndicator(final JFrame frame) {
        this(frame.getLayeredPane());
        JMenuBar mb = frame.getJMenuBar();
        if (mb != null) {
            // Exclude the menu bar from centering/sizing
            verticalOffset = mb.getHeight();
        }
    }

    public SpinningDialWaitIndicator(final JComponent target) {
        this(target, null);
    }

    public SpinningDialWaitIndicator(final JComponent target, final String text) {
        super(target);
        this.text = text;
        // Draw the dial centered and scaled to fit, up to a maximum size
        dial = new SpinningDial() {
            @Override
            public int getIconWidth() {
                Rectangle r = getComponent().getVisibleRect();
                int margin = Math.min(MARGIN, r.width/MARGIN_FRACTION);
                return Math.min(MAX_SIZE, r.width-margin*2);
            }
            @Override
            public int getIconHeight() {
                Rectangle r = getComponent().getVisibleRect();
                int margin = Math.min(MARGIN, r.height/MARGIN_FRACTION);
                return Math.min(MAX_SIZE, r.height-verticalOffset-margin*2);
            }
        };
        // Disable automatic animation
        dial.setFrameInterval(0);
    }

    public void setText(final String text) {
        this.text = text;
        repaint();
    }

    /** Fade the affected component to background, then apply a spinning
     * wait indicator.
     */
    @Override
    public void paint(final Graphics graphics) {
        if (timer == null) {
            timer = new Timer(FADE_INTERVAL, this);
            timer.start();
        }
        Graphics2D g = (Graphics2D)graphics.create();

        Rectangle r = getComponent().getVisibleRect();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color bg = getComponent().getBackground();
        g.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), fade));
        g.fillRect(r.x, r.y, r.width, r.height);
        if (fade < FADE_THRESHOLD) {
            return;
        }

        int x = r.x;
        if (text == null) {
            x += (r.width - dial.getIconWidth())/2;
        } else {
            x += dial.getIconWidth()/4;
        }
        int y = r.y + verticalOffset + (r.height - verticalOffset - dial.getIconHeight())/2;
        dial.paintIcon(getPainter(), g, x, y);
        if (text != null) {
            Font font = g.getFont();
            g.setFont(font.deriveFont(Font.BOLD, dial.getIconHeight()/2));
            FontMetrics m = g.getFontMetrics();
            x += dial.getIconWidth() * 5/4;
            // FIXME vertical offset is not quite right
            y += dial.getIconHeight() - (dial.getIconHeight() - m.getAscent())/2;
            g.setColor(getComponent().getForeground());
            g.drawString(text, x, y);
        }

        g.dispose();
    }

    /** Remove the wait decoration. */
    @Override
    public void dispose() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        super.dispose();
    }

    /** First fade the background, then spin the dial. */
    public void actionPerformed(final ActionEvent e) {
        if (fade < FADE_THRESHOLD) {
            fade += 32;
            if (fade >= FADE_THRESHOLD) {
                timer.setDelay(SpinningDial.SPIN_INTERVAL);
            }
        }
        else {
            dial.nextFrame();
        }
        getPainter().repaint();
    }
}
