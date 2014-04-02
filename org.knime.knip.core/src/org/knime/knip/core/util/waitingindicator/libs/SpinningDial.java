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
 */
package org.knime.knip.core.util.waitingindicator.libs;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

/** Provides a spinning disk of hash marks. */
public class SpinningDial extends AbstractAnimatedIcon {
    private static final int MIN_ALPHA = 32;
    private static final int DEFAULT_SIZE = 32;
    private static final int SPOKES = 16;
    /** This is a good delay between increasing the phase. */
    public static final int SPIN_INTERVAL = 1000/SPOKES;
    private int w;
    private int h;
    private Image[] frames;

    public SpinningDial() {
        this(DEFAULT_SIZE, DEFAULT_SIZE);
    }
    /** Not recommended to go below 16x16. */
    public SpinningDial(final int w, final int h) {
        this(w, h, SPOKES);
    }
    public SpinningDial(final int w, final int h, final int spokes) {
        super(spokes, SPIN_INTERVAL);
        this.w = w;
        this.h = h;
        frames = new Image[getFrameCount()];
    }
    @Override
    public int getIconHeight() {
        return h;
    }
    @Override
    public int getIconWidth() {
        return w;
    }
    /** Set the stroke width according to the size. */
    protected float getStrokeWidth(final int size) {
        return size/16f;
    }

    // TODO: move image snapshot up to abstract class
    @Override
    protected void paintFrame(final Component c, final Graphics graphics, final int x, final int y) {
        int idx = getFrame();
        if (frames[idx] == null) {
            int w = getIconWidth();
            int h = getIconHeight();
            int size = Math.min(w, h);
            Image image = c != null
                ? c.getGraphicsConfiguration().createCompatibleImage(w, h, Transparency.TRANSLUCENT)
                : new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D)image.getGraphics();
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, w, h);
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            final int FULL_SIZE = 256;
            float strokeWidth = getStrokeWidth(FULL_SIZE);
            float fraction = .6f;
            g.setStroke(new BasicStroke(strokeWidth,
                                        BasicStroke.CAP_ROUND,
                                        BasicStroke.JOIN_ROUND));
            g.translate((float)w/2, (float)h/2);
            float scale = (float)size/FULL_SIZE;
            g.scale(scale, scale);
            int alpha = 255;
            int x1, y1, x2, y2;
            int radius = FULL_SIZE/2-1-(int)(strokeWidth/2);
            int frameCount = getFrameCount();
            for (int i=0;i < frameCount;i++) {
                double cos = Math.cos(Math.PI*2-Math.PI*2*(i-idx)/frameCount);
                double sin = Math.sin(Math.PI*2-Math.PI*2*(i-idx)/frameCount);
                x1 = (int)(radius*fraction * cos);
                x2 = (int)(radius* cos);
                y1 = (int)(radius*fraction * sin);
                y2 = (int)(radius* sin);
                g.setColor(new Color(0,0,0,Math.min(255, alpha)));
                g.drawLine(x1, y1, x2, y2);
                alpha = Math.max(MIN_ALPHA, alpha*3/4);
            }
            g.dispose();
            frames[idx] = image;
        }
        graphics.drawImage(frames[idx], x, y, null);
    }

    @Override
    public String toString() {
        return "SpinningDial(" + getIconWidth() + "x" + getIconHeight() + ")";
    }
}
