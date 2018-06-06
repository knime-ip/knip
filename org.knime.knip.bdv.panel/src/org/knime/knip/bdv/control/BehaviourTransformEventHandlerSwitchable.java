package org.knime.knip.bdv.control;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.BehaviourTransformEventHandler;
import bdv.BehaviourTransformEventHandlerFactory;
import bdv.util.BehaviourTransformEventHandlerPlanar;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandlerFactory;
import net.imglib2.ui.TransformListener;

/**
 * Allows switching between 2D and 3D mode within the same BDV instance. 
 * 
 * Based on https://github.com/bigdataviewer/bigdataviewer-vistools/commit/94496b043e1a6d86786a02db75fd43fa739e6a77
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class BehaviourTransformEventHandlerSwitchable implements BehaviourTransformEventHandler<AffineTransform3D> {
	private final BehaviourTransformEventHandlerPlanar handler2D;

	private final FixedViewportBehaviourTransformEventHandler3D handler3D;

	private BehaviourTransformEventHandler<AffineTransform3D> current;

	private TransformListener<AffineTransform3D> listener;

	private final TransformListener<AffineTransform3D> dummyListener = new TransformListener<AffineTransform3D>() {
		@Override
		public void transformChanged(final AffineTransform3D transform) {
		}
	};

	public static TransformEventHandlerFactory<AffineTransform3D> factory() {
		return new BehaviourTransformEventHandlerSwitchableFactory();
	}

	public static class BehaviourTransformEventHandlerSwitchableFactory
			implements BehaviourTransformEventHandlerFactory<AffineTransform3D> {
		private InputTriggerConfig config = new InputTriggerConfig();

		@Override
		public void setConfig(final InputTriggerConfig config) {
			this.config = config;
		}

		@Override
		public BehaviourTransformEventHandlerSwitchable create(
				final TransformListener<AffineTransform3D> transformListener) {
			return new BehaviourTransformEventHandlerSwitchable(transformListener, config);
		}
	}

	public BehaviourTransformEventHandlerSwitchable(final TransformListener<AffineTransform3D> listener,
			final InputTriggerConfig config) {
		this.listener = listener;
		handler2D = new BehaviourTransformEventHandlerPlanar(listener, config);
		handler3D = new FixedViewportBehaviourTransformEventHandler3D(listener, config);
		current = handler3D;
	}

	public boolean is2D() {
		return current == handler2D;
	}

	public void set2D(final boolean is2D) {
		if (is2D) {
			if (current != handler2D) {
				current = handler2D;
				handler2D.setTransformListener(listener);
				handler3D.setTransformListener(dummyListener);
				handler2D.setTransform(handler3D.getTransform());
			}
		} else {
			if (current != handler3D) {
				current = handler3D;
				handler3D.setTransformListener(listener);
				handler2D.setTransformListener(dummyListener);
				handler3D.setTransform(handler2D.getTransform());
			}
		}
	}

	@Override
	public void install(final TriggerBehaviourBindings bindings) {
		current.install(bindings);
	}

	@Override
	public AffineTransform3D getTransform() {
		return current.getTransform();
	}

	@Override
	public void setTransform(final AffineTransform3D transform) {
		current.setTransform(transform);
	}

	@Override
	public void setCanvasSize(final int width, final int height, final boolean updateTransform) {
		handler2D.setCanvasSize(width, height, updateTransform);
		handler3D.setCanvasSize(width, height, updateTransform);
	}

	@Override
	public void setTransformListener(final TransformListener<AffineTransform3D> transformListener) {
		this.listener = transformListener;
		current.setTransformListener(listener);
	}

	@Override
	public String getHelpString() {
		return null;
	}
}