/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.knime.knip.bdv.control;

import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.BehaviourTransformEventHandler;
import bdv.BehaviourTransformEventHandler3D;
import bdv.BehaviourTransformEventHandlerFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.TransformEventHandlerFactory;
import net.imglib2.ui.TransformListener;

/**
 * {@link FixedViewportBehaviourTransformEventHandler3D} is a copy of
 * {@link BehaviourTransformEventHandler3D} in which the scaling is disabled in
 * {@link FixedViewportBehaviourTransformEventHandler3D#setCanvasSize(int, int, boolean)}.
 * 
 * 
 * 
 * A {@link TransformEventHandler} that changes an {@link AffineTransform3D}
 * through a set of {@link Behaviour}s.
 *
 * @author Stephan Saalfeld
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class FixedViewportBehaviourTransformEventHandler3D
		implements BehaviourTransformEventHandler<AffineTransform3D> {
	public static TransformEventHandlerFactory<AffineTransform3D> factory() {
		return new FixedViewportBehaviourTransformEventHandler3DFactory();
	}

	public static class FixedViewportBehaviourTransformEventHandler3DFactory
			implements BehaviourTransformEventHandlerFactory<AffineTransform3D> {
		private InputTriggerConfig config = new InputTriggerConfig();

		@Override
		public void setConfig(final InputTriggerConfig config) {
			this.config = config;
		}

		@Override
		public FixedViewportBehaviourTransformEventHandler3D create(
				final TransformListener<AffineTransform3D> transformListener) {
			return new FixedViewportBehaviourTransformEventHandler3D(transformListener, config);
		}
	}

	/**
	 * Current source to screen transform.
	 */
	private final AffineTransform3D affine = new AffineTransform3D();

	/**
	 * Whom to notify when the {@link #affine current transform} is changed.
	 */
	private TransformListener<AffineTransform3D> listener;

	/**
	 * Copy of {@link #affine current transform} when mouse dragging started.
	 */
	final private AffineTransform3D affineDragStart = new AffineTransform3D();

	/**
	 * Coordinates where mouse dragging started.
	 */
	private double oX, oY;

	/**
	 * Current rotation axis for rotating with keyboard, indexed {@code x->0, y->1,
	 * z->2}.
	 */
	private int axis = 0;

	/**
	 * The screen size of the canvas (the component displaying the image and
	 * generating mouse events).
	 */
	private int canvasW = 1, canvasH = 1;

	/**
	 * Screen coordinates to keep centered while zooming or rotating with the
	 * keyboard. These are set to <em>(canvasW/2, canvasH/2)</em>
	 */
	private int centerX = 0, centerY = 0;

	private final Behaviours behaviours;

	public FixedViewportBehaviourTransformEventHandler3D(final TransformListener<AffineTransform3D> listener,
			final InputTriggerConfig config) {
		this.listener = listener;

		final String DRAG_TRANSLATE = "drag translate";
		final String ZOOM_NORMAL = "scroll zoom";
		final String SELECT_AXIS_X = "axis x";
		final String SELECT_AXIS_Y = "axis y";
		final String SELECT_AXIS_Z = "axis z";

		final double[] speed = { 1.0, 10.0, 0.1 };
		final String[] SPEED_NAME = { "", " fast", " slow" };
		final String[] speedMod = { "", "shift ", "ctrl " };

		final String DRAG_ROTATE = "drag rotate";
		final String SCROLL_Z = "scroll browse z";
		final String ROTATE_LEFT = "rotate left";
		final String ROTATE_RIGHT = "rotate right";
		final String KEY_ZOOM_IN = "zoom in";
		final String KEY_ZOOM_OUT = "zoom out";
		final String KEY_FORWARD_Z = "forward z";
		final String KEY_BACKWARD_Z = "backward z";

		behaviours = new Behaviours(config, "bdv");

		behaviours.behaviour(new TranslateXY(), DRAG_TRANSLATE, "button2", "button3");
		behaviours.behaviour(new Zoom(speed[0]), ZOOM_NORMAL, "meta scroll", "ctrl shift scroll");
		behaviours.behaviour(new SelectRotationAxis(0), SELECT_AXIS_X, "X");
		behaviours.behaviour(new SelectRotationAxis(1), SELECT_AXIS_Y, "Y");
		behaviours.behaviour(new SelectRotationAxis(2), SELECT_AXIS_Z, "Z");

		for (int s = 0; s < 3; ++s) {
			behaviours.behaviour(new Rotate(speed[s]), DRAG_ROTATE + SPEED_NAME[s], speedMod[s] + "button1");
			behaviours.behaviour(new TranslateZ(speed[s]), SCROLL_Z + SPEED_NAME[s], speedMod[s] + "scroll");
			behaviours.behaviour(new KeyRotate(speed[s]), ROTATE_LEFT + SPEED_NAME[s], speedMod[s] + "LEFT");
			behaviours.behaviour(new KeyRotate(-speed[s]), ROTATE_RIGHT + SPEED_NAME[s], speedMod[s] + "RIGHT");
			behaviours.behaviour(new KeyZoom(speed[s]), KEY_ZOOM_IN + SPEED_NAME[s], speedMod[s] + "UP");
			behaviours.behaviour(new KeyZoom(-speed[s]), KEY_ZOOM_OUT + SPEED_NAME[s], speedMod[s] + "DOWN");
			behaviours.behaviour(new KeyTranslateZ(speed[s]), KEY_FORWARD_Z + SPEED_NAME[s], speedMod[s] + "COMMA");
			behaviours.behaviour(new KeyTranslateZ(-speed[s]), KEY_BACKWARD_Z + SPEED_NAME[s], speedMod[s] + "PERIOD");
		}
	}

	@Override
	public void install(final TriggerBehaviourBindings bindings) {
		behaviours.install(bindings, "transform");
	}

	@Override
	public AffineTransform3D getTransform() {
		synchronized (affine) {
			return affine.copy();
		}
	}

	@Override
	public void setTransform(final AffineTransform3D transform) {
		synchronized (affine) {
			affine.set(transform);
		}
	}

	@Override
	public void setCanvasSize(final int width, final int height, final boolean updateTransform) {
		if (width == 0 || height == 0) {
			// NB: We are probably in some intermediate layout scenario.
			// Attempting to trigger a transform update with 0 size will result
			// in the exception "Matrix is singular" from imglib2-realtrasform.
			return;
		}
		if (updateTransform) {
			synchronized (affine) {
				affine.set(affine.get(0, 3) - canvasW / 2, 0, 3);
				affine.set(affine.get(1, 3) - canvasH / 2, 1, 3);
				// affine.scale( ( double ) width / canvasW );
				affine.set(affine.get(0, 3) + width / 2, 0, 3);
				affine.set(affine.get(1, 3) + height / 2, 1, 3);
				notifyListener();
			}
		}
		canvasW = width;
		canvasH = height;
		centerX = width / 2;
		centerY = height / 2;
	}

	@Override
	public void setTransformListener(final TransformListener<AffineTransform3D> transformListener) {
		listener = transformListener;
	}

	@Override
	public String getHelpString() {
		return helpString;
	}

	/**
	 * notifies {@link #listener} that the current transform changed.
	 */
	private void notifyListener() {
		if (listener != null)
			listener.transformChanged(affine);
	}

	/**
	 * One step of rotation (radian).
	 */
	final private static double step = Math.PI / 180;

	final private static String NL = System.getProperty("line.separator");

	final private static String helpString = "Mouse control:" + NL + " " + NL
			+ "Pan and tilt the volume by left-click and dragging the image in the canvas, " + NL
			+ "move the volume by middle-or-right-click and dragging the image in the canvas, " + NL
			+ "browse alongside the z-axis using the mouse-wheel, and" + NL
			+ "zoom in and out using the mouse-wheel holding CTRL+SHIFT or META." + NL + " " + NL + "Key control:" + NL
			+ " " + NL + "X - Select x-axis as rotation axis." + NL + "Y - Select y-axis as rotation axis." + NL
			+ "Z - Select z-axis as rotation axis." + NL
			+ "CURSOR LEFT - Rotate clockwise around the choosen rotation axis." + NL
			+ "CURSOR RIGHT - Rotate counter-clockwise around the choosen rotation axis." + NL + "CURSOR UP - Zoom in."
			+ NL + "CURSOR DOWN - Zoom out." + NL + "./> - Forward alongside z-axis." + NL
			+ ",/< - Backward alongside z-axis." + NL + "SHIFT - Rotate and browse 10x faster." + NL
			+ "CTRL - Rotate and browse 10x slower.";

	private void scale(final double s, final double x, final double y) {
		// center shift
		affine.set(affine.get(0, 3) - x, 0, 3);
		affine.set(affine.get(1, 3) - y, 1, 3);

		// scale
		affine.scale(s);

		// center un-shift
		affine.set(affine.get(0, 3) + x, 0, 3);
		affine.set(affine.get(1, 3) + y, 1, 3);
	}

	/**
	 * Rotate by d radians around axis. Keep screen coordinates ( {@link #centerX},
	 * {@link #centerY}) fixed.
	 */
	private void rotate(final int axis, final double d) {
		// center shift
		affine.set(affine.get(0, 3) - centerX, 0, 3);
		affine.set(affine.get(1, 3) - centerY, 1, 3);

		// rotate
		affine.rotate(axis, d);

		// center un-shift
		affine.set(affine.get(0, 3) + centerX, 0, 3);
		affine.set(affine.get(1, 3) + centerY, 1, 3);
	}

	private class Rotate implements DragBehaviour {
		private final double speed;

		public Rotate(final double speed) {
			this.speed = speed;
		}

		@Override
		public void init(final int x, final int y) {
			synchronized (affine) {
				oX = x;
				oY = y;
				affineDragStart.set(affine);
			}
		}

		@Override
		public void drag(final int x, final int y) {
			synchronized (affine) {
				final double dX = oX - x;
				final double dY = oY - y;

				affine.set(affineDragStart);

				// center shift
				affine.set(affine.get(0, 3) - oX, 0, 3);
				affine.set(affine.get(1, 3) - oY, 1, 3);

				final double v = step * speed;
				affine.rotate(0, -dY * v);
				affine.rotate(1, dX * v);

				// center un-shift
				affine.set(affine.get(0, 3) + oX, 0, 3);
				affine.set(affine.get(1, 3) + oY, 1, 3);
				notifyListener();
			}
		}

		@Override
		public void end(final int x, final int y) {
		}
	}

	private class TranslateXY implements DragBehaviour {
		@Override
		public void init(final int x, final int y) {
			synchronized (affine) {
				oX = x;
				oY = y;
				affineDragStart.set(affine);
			}
		}

		@Override
		public void drag(final int x, final int y) {
			synchronized (affine) {
				final double dX = oX - x;
				final double dY = oY - y;

				affine.set(affineDragStart);
				affine.set(affine.get(0, 3) - dX, 0, 3);
				affine.set(affine.get(1, 3) - dY, 1, 3);
				notifyListener();
			}
		}

		@Override
		public void end(final int x, final int y) {
		}
	}

	private class TranslateZ implements ScrollBehaviour {
		private final double speed;

		public TranslateZ(final double speed) {
			this.speed = speed;
		}

		@Override
		public void scroll(final double wheelRotation, final boolean isHorizontal, final int x, final int y) {
			synchronized (affine) {
				final double dZ = speed * -wheelRotation;
				// TODO (optionally) correct for zoom
				affine.set(affine.get(2, 3) - dZ, 2, 3);
				notifyListener();
			}
		}
	}

	private class Zoom implements ScrollBehaviour {
		private final double speed;

		public Zoom(final double speed) {
			this.speed = speed;
		}

		@Override
		public void scroll(final double wheelRotation, final boolean isHorizontal, final int x, final int y) {
			synchronized (affine) {
				final double s = speed * wheelRotation;
				final double dScale = 1.0 + 0.05;
				if (s > 0)
					scale(1.0 / dScale, x, y);
				else
					scale(dScale, x, y);
				notifyListener();
			}
		}
	}

	private class SelectRotationAxis implements ClickBehaviour {
		private final int axis;

		public SelectRotationAxis(final int axis) {
			this.axis = axis;
		}

		@Override
		public void click(final int x, final int y) {
			FixedViewportBehaviourTransformEventHandler3D.this.axis = axis;
		}
	}

	private class KeyRotate implements ClickBehaviour {
		private final double speed;

		public KeyRotate(final double speed) {
			this.speed = speed;
		}

		@Override
		public void click(final int x, final int y) {
			synchronized (affine) {
				rotate(axis, step * speed);
				notifyListener();
			}
		}
	}

	private class KeyZoom implements ClickBehaviour {
		private final double dScale;

		public KeyZoom(final double speed) {
			if (speed > 0)
				dScale = 1.0 + 0.1 * speed;
			else
				dScale = 1.0 / (1.0 - 0.1 * speed);
		}

		@Override
		public void click(final int x, final int y) {
			synchronized (affine) {
				scale(dScale, centerX, centerY);
				notifyListener();
			}
		}
	}

	private class KeyTranslateZ implements ClickBehaviour {
		private final double speed;

		public KeyTranslateZ(final double speed) {
			this.speed = speed;
		}

		@Override
		public void click(final int x, final int y) {
			synchronized (affine) {
				affine.set(affine.get(2, 3) + speed, 2, 3);
				notifyListener();
			}
		}
	}
}
