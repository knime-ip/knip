package org.knime.knip.bdv.control;

import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;

import java.util.Map;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.knime.knip.bdv.lut.ColorTableConverter;
import org.knime.knip.bdv.overlay.IntensityMouseOverOverlay;
import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.BehaviourTransformEventHandler;
import bdv.BehaviourTransformEventHandlerFactory;
import bdv.BigDataViewer;
import bdv.BigDataViewerActions;
import bdv.cache.CacheControl.CacheControls;
import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.bookmarks.BookmarksEditor;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.ManualTransformationEditor;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvOptions;
import bdv.viewer.DisplayMode;
import bdv.viewer.NavigationActions;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel.AlignPlane;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.TransformEventHandlerFactory;

/**
 * This is a copy of the BdvHandler from bigdataviewer-vistools but the
 * Brightness&Color panel and the Visibility&Grouping panel are removed.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class BDVHandlePanel<I extends IntegerType<I>, T extends NumericType<T>, L> extends BdvHandlePanel {

	private final ManualTransformationEditor manualTransformationEditor;

	private final Bookmarks bookmarks;

	private final BookmarksEditor bookmarksEditor;

	private final InputActionBindings keybindings;

	private final TriggerBehaviourBindings triggerbindings;

	public BDVHandlePanel(final Frame dialogOwner, final BdvOptions options,
			final Map<Integer, ColorTableConverter<L>> convs) {
		super(dialogOwner, options);
		final ViewerOptions viewerOptions = options.values.getViewerOptions();
		final InputTriggerConfig inputTriggerConfig = BigDataViewer.getInputTriggerConfig(viewerOptions);

		final TransformEventHandlerFactory<AffineTransform3D> thf = viewerOptions.values
				.getTransformEventHandlerFactory();
		if (thf instanceof BehaviourTransformEventHandlerFactory)
			((BehaviourTransformEventHandlerFactory<?>) thf).setConfig(inputTriggerConfig);

		cacheControls = new CacheControls();

		viewer = new KnipBdvViewerPanel(new ArrayList<>(), 1, cacheControls, viewerOptions);
		if (!options.values.hasPreferredSize())
			viewer.getDisplay().setPreferredSize(null);
			viewer.getDisplay().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent e) {
				tryInitTransform();
			}
		});

		setupAssignments = new SetupAssignments(new ArrayList<>(), 0, 65535);

		keybindings = new InputActionBindings();
		SwingUtilities.replaceUIActionMap(viewer, keybindings.getConcatenatedActionMap());
		SwingUtilities.replaceUIInputMap(viewer, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
				keybindings.getConcatenatedInputMap());

		triggerbindings = new TriggerBehaviourBindings();
		final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();
		mouseAndKeyHandler.setInputMap(triggerbindings.getConcatenatedInputTriggerMap());
		mouseAndKeyHandler.setBehaviourMap(triggerbindings.getConcatenatedBehaviourMap());
		viewer.getDisplay().addHandler(mouseAndKeyHandler);

		final TransformEventHandler<?> tfHandler = viewer.getDisplay().getTransformEventHandler();
		if (tfHandler instanceof BehaviourTransformEventHandler)
			((BehaviourTransformEventHandler<?>) tfHandler).install(triggerbindings);

		manualTransformationEditor = new ManualTransformationEditor(viewer, keybindings);

		bookmarks = new Bookmarks();
		bookmarksEditor = new BookmarksEditor(viewer, keybindings, bookmarks);

		final NavigationActions navactions = new NavigationActions(inputTriggerConfig);
		navactions.install(keybindings, "navigation");
		navactions.modes(viewer);
		navactions.sources(viewer);
		navactions.time(viewer);
		if (options.values.is2D())
			navactions.alignPlaneAction(viewer, AlignPlane.XY, "shift Z");
		else
			navactions.alignPlanes(viewer);

		final BigDataViewerActions bdvactions = new BigDataViewerActions(inputTriggerConfig);
		bdvactions.install(keybindings, "bdv");
		bdvactions.bookmarks(bookmarksEditor);
		bdvactions.manualTransform(manualTransformationEditor);

		new IntensityMouseOverOverlay<L, I>(viewer, convs);

		viewer.setDisplayMode(DisplayMode.FUSED);
	}
}
