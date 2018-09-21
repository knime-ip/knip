/* Copyright (c) 2007 Timothy Wall, All Rights Reserved
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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;

import javax.swing.Timer;

/**
 * Provide animation of auto-generated animations. Makes use of the repaint
 * tracking structure established by {@link AnimatedIcon}.
 */
public abstract class AbstractAnimatedIcon extends AnimatedIcon {
	private static final int DEFAULT_INTERVAL = 1000 / 24;

	private Timer timer;
	private int repaintInterval;
	private int frame;
	private int frameCount;

	protected AbstractAnimatedIcon() {
		this(0);
	}

	protected AbstractAnimatedIcon(final int frameCount) {
		this(frameCount, DEFAULT_INTERVAL);
	}

	protected AbstractAnimatedIcon(final int frameCount, final int interval) {
		this.frameCount = frameCount;
		setFrameInterval(interval);
	}

	/** Ensure the timer stops running, so it, too can be GC'd. */
	@Override
    protected void finalize() {
        if (timer != null) {
            timer.stop();
        }
	}

	/** Setting a frame interval of zero stops automatic animation. */
	public void setFrameInterval(final int interval) {
		repaintInterval = interval;
		if (interval != 0) {
			if (timer == null) {
				timer = new Timer(interval, new AnimationUpdater(this));
				timer.setRepeats(true);
			} else {
				timer.setDelay(interval);
			}
		} else if (timer != null) {
			timer.stop();
			timer = null;
		}
	}

	public int getFrameInterval() {
		return repaintInterval;
	}

	/** Returns the total number of frames. */
	public int getFrameCount() {
		return frameCount;
	}

	/** Advance to the next animation frame. */
	public void nextFrame() {
		setFrame(getFrame() + 1);
	}

	/** Set the current animation frame number. */
	public void setFrame(final int f) {
		this.frame = f;
		if (frameCount != 0) {
            frame = frame % frameCount;
        }
		repaint();
	}

	/** Returns the current animation frame number. */
	public int getFrame() {
		return frame;
	}

	/** Implement this method to paint the icon. */
	@Override
    protected abstract void paintFrame(Component c, Graphics g, int x, int y);

	@Override
    public abstract int getIconWidth();

	@Override
    public abstract int getIconHeight();

	@Override
    protected synchronized void registerRepaintArea(final Component c, final int x, final int y,
			final int w, final int h) {
		if (timer != null && !timer.isRunning()) {
			timer.start();
		}
		super.registerRepaintArea(c, x, y, w, h);
	}

	@SuppressWarnings("rawtypes")
	private static class AnimationUpdater implements ActionListener {

		private WeakReference ref;

		@SuppressWarnings("unchecked")
		public AnimationUpdater(final AbstractAnimatedIcon icon) {
			this.ref = new WeakReference(icon);
		}

		public void actionPerformed(final ActionEvent e) {
			AbstractAnimatedIcon icon = (AbstractAnimatedIcon) ref.get();
			if (icon != null) {
				icon.nextFrame();
			}
		}
	}
}
