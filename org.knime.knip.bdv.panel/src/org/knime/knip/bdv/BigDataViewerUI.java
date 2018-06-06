package org.knime.knip.bdv;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import org.knime.knip.bdv.control.BDVController;
import org.knime.knip.bdv.control.BDVHandlePanel;
import org.knime.knip.bdv.control.BehaviourTransformEventHandlerSwitchable;
import org.knime.knip.bdv.lut.ColorTableConverter;
import org.knime.knip.bdv.projector.AccumulateProjectorAlphaBlendingARGB;
import org.knime.knip.bdv.uicomponents.CardPanel;
import org.knime.knip.bdv.uicomponents.InterpolationModePanel;
import org.knime.knip.bdv.uicomponents.SelectionAndGroupingTabs;
import org.knime.knip.bdv.uicomponents.SourceProperties;
import org.knime.knip.bdv.uicomponents.TransformationPanel;
import org.scijava.Context;
import org.scijava.event.EventService;

import bdv.BigDataViewer;
import bdv.util.Bdv;
import bdv.viewer.Source;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.VolatileProjector;
import gnu.trove.map.hash.TIntIntHashMap;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.miginfocom.swing.MigLayout;

/**
 * UI for the {@link BigDataViewer} mapping all functionality to UI-Components.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 * @param <I>
 * @param <T>
 * @param <L>
 */
public class BigDataViewerUI<I extends IntegerType<I>, T extends NumericType<T>, L> {

	private static final String CONTROL_CARD_NAME = "Viewer Settings";

	private static final String SELECTION_CARD_NAME = "Selection";

	/**
	 * Splitpane holding the BDV and UI.
	 */
	private final JSplitPane splitPane;

	/**
	 * Controller which keeps BDV and UI in synch.
	 */
	private BDVController<I, T, L> bdv;

	/**
	 * Map from source names to {@link SourceProperties}.
	 */
	private final Map<String, SourceProperties<T>> sourceLookup = new HashMap<>();

	/**
	 * Map from label-source-index to {@link ColorTableConverter}.
	 */
	private Map<Integer, ColorTableConverter<L>> converters;

	/**
	 * Panel holding the control UI-Components.
	 */
	private CardPanel controlsPanel;

	/**
	 * The main panel.
	 */
	private JPanel panel;

	/**
	 * BDV {@link AccumulateProjectorFactory} which generates
	 * {@link AccumulateProjectorAlphaBlendingARGB} which adds image sources
	 * together and blends labeling sources on top with alpha-blending.
	 */
	final AccumulateProjectorFactory<ARGBType> myFactory = new AccumulateProjectorFactory<ARGBType>() {

		@Override
		public synchronized AccumulateProjectorAlphaBlendingARGB createAccumulateProjector(
				final ArrayList<VolatileProjector> sourceProjectors, final ArrayList<Source<?>> sources,
				final ArrayList<? extends RandomAccessible<? extends ARGBType>> sourceScreenImages,
				final RandomAccessibleInterval<ARGBType> targetScreenImages, final int numThreads,
				final ExecutorService executorService) {

			// lookup is true if source is a labeling
			final List<Boolean> lookup = new ArrayList<>();
			int startImgs = -1;
			int startLabs = -1;
			
			for (Source<?> s : sources) {
				try {
				lookup.add(new Boolean(sourceLookup.get(s.getName()).isLabeling()));
				}catch (Exception e) {
					for (SourceProperties<T> p : sourceLookup.values()) {
						System.out.println(p.getSourceName() + ", " + p.isLabeling());
					}
				}
			}
			
			final boolean[] labelingLookup = new boolean[lookup.size()];
			for (int i = 0; i < lookup.size(); i++) {
				final boolean b = lookup.get(i).booleanValue();
				if (startImgs < 0 && !b) {
					startImgs = i;
				}
				if (startLabs < 0 && b) {
					startLabs = i;
				}
				labelingLookup[i] = b;
			}

			return new AccumulateProjectorAlphaBlendingARGB(sourceProjectors, sourceScreenImages, targetScreenImages,
					numThreads, executorService, labelingLookup, startImgs, startLabs);
		}

	};

	public BigDataViewerUI(final JFrame frame) {
		Context c = new Context();
		c.inject(this);
		EventService es = c.getService(EventService.class);
		es.subscribe(this);

		final BDVHandlePanel<I, T, L> bdvHandlePanel = createBDVHandlePanel(frame);

		bdv = new BDVController<>(bdvHandlePanel, sourceLookup, converters, es);
		c.inject(bdv);

		controlsPanel = new CardPanel();

		// Add selection card
		final SelectionAndGroupingTabs<I, T, L> grouping = new SelectionAndGroupingTabs<>(es, bdvHandlePanel);
		final JPanel visAndGroup = new JPanel(new MigLayout("fillx, ins 2", "[grow]", ""));
		visAndGroup.setBackground(Color.WHITE);
		visAndGroup.add(grouping, "growx, wrap");
		controlsPanel.addNewCard(new JLabel(SELECTION_CARD_NAME), false, visAndGroup);

		// Add control card
		final JPanel globalControls = new JPanel(new MigLayout("fillx, ins 2", "[grow]", ""));
		globalControls.add(new TransformationPanel<I, T, L>(es, bdv), "growx, wrap");
		globalControls.add(new InterpolationModePanel(es, bdvHandlePanel.getViewerPanel()), "growx");
		controlsPanel.addNewCard(new JLabel(CONTROL_CARD_NAME), true, globalControls);

		// put UI-Components and BDV into Splitpane panel
		splitPane = createSplitPane();
		final JScrollPane scrollPane = new JScrollPane(controlsPanel);
		scrollPane.setPreferredSize(new Dimension(320, 600));
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		splitPane.setLeftComponent(bdvHandlePanel.getViewerPanel());
		splitPane.setRightComponent(scrollPane);
		splitPane.getLeftComponent().setMinimumSize(new Dimension(20, 20));

		panel = new JPanel();
		panel.setLayout(new MigLayout("fillx, filly, ins 0", "[grow]", "[grow]"));
		panel.add(splitPane, "growx, growy");
	}

	/**
	 * Create {@link BDVHandlePanel} to display sources of {@link Dimensionality}.
	 * 
	 * @param frame
	 * @param dim
	 * @return
	 */
	private BDVHandlePanel<I, T, L> createBDVHandlePanel(final JFrame frame) {
		converters = new HashMap<>();
		final BDVHandlePanel<I, T, L> bdvHandlePanel;
		bdvHandlePanel = new BDVHandlePanel<>(frame,
				Bdv.options().preferredSize(600, 600).numSourceGroups(1)
						.transformEventHandlerFactory(BehaviourTransformEventHandlerSwitchable.factory())
						.accumulateProjectorFactory(myFactory).numRenderingThreads(1),
				converters);
		return bdvHandlePanel;
	}

	/**
	 * Add image to the BDV-UI.
	 * 
	 * @param img
	 *            the image to add
	 * @param type
	 *            information
	 * @param name
	 *            of the source
	 * @param visibility
	 *            of the source
	 * @param groupNames
	 *            groups to which this source belongs
	 * @param color
	 *            of the source
	 * @param transformation
	 *            initial transformation of the source
	 * @param min
	 *            display range
	 * @param max
	 *            display range
	 */
	public synchronized void addImage(final RandomAccessibleInterval<T> img, final String type, final String name,
			final boolean visibility, final Set<String> groupNames, final Color color,
			final AffineTransform3D transformation, final double min, final double max) {
		bdv.addImg(img, type, name, visibility, groupNames, color, transformation, min, max);
		if (bdv.getNumSources() == 1) {
			controlsPanel.setCardActive(SELECTION_CARD_NAME, true);
			controlsPanel.toggleCardFold(SELECTION_CARD_NAME);
		}
	}

	/**
	 * Add labeling to the BDV-UI.
	 * 
	 * @param imgLab
	 *            the labeling image
	 * @param type
	 *            information
	 * @param name
	 *            of the source
	 * @param visibility
	 *            of the source
	 * @param groupNames
	 *            groups to which this source belongs
	 * @param transformation
	 *            initial transformation of the source
	 * @param lut 
	 */
	public synchronized void addLabeling(RandomAccessibleInterval<LabelingType<L>> img, String type,
			String name, boolean visibility, Set<String> groupNames, AffineTransform3D transformation, final TIntIntHashMap lut) {
		bdv.addLabeling(img, type, name, visibility, groupNames, transformation, lut);
		if (bdv.getNumSources() == 1) {
			controlsPanel.setCardActive(SELECTION_CARD_NAME, true);
			controlsPanel.toggleCardFold(SELECTION_CARD_NAME);
		}
	}

	/**
	 * Remove source with given name.
	 * 
	 * Note: Removes images and labelings.
	 * 
	 * @param sourceName
	 */
	public synchronized void removeSource(final String sourceName) {
		bdv.removeSource(sourceName);
		if (bdv.getNumSources() <= 0) {
			controlsPanel.toggleCardFold(SELECTION_CARD_NAME);
			controlsPanel.setCardActive(SELECTION_CARD_NAME, false);
		}
	}

	/**
	 * 
	 * @return the splitpane with BDV and BDV-UI
	 */
	public JPanel getPanel() {
		return this.panel;
	}

	/**
	 * Create splitpane.
	 * 
	 * @return splitpane
	 */
	private JSplitPane createSplitPane() {
		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setUI(new BasicSplitPaneUI() {
			public BasicSplitPaneDivider createDefaultDivider() {
				return new BasicSplitPaneDivider(this) {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void paint(Graphics g) {
						g.setColor(new Color(238, 238, 238));
						g.fillRect(0, 0, getSize().width, getSize().height);
						super.paint(g);
					}
				};
			}
		});

		splitPane.setBackground(new Color(31, 31, 45));
		splitPane.setDividerLocation(560);
		splitPane.setResizeWeight(1.0);

		return splitPane;
	}

	/**
	 * Remove all sources from BDV.
	 */
	public synchronized void removeAll() {
		Set<String> keySet = new HashSet<>(sourceLookup.keySet());
		for (String source : keySet) {
			removeSource(source);
		}
	}
	
	/**
	 * Switch BDV between 2D and 3D mode.
	 * 
	 * @param twoDimensional
	 */
	public void switch2D(final boolean twoDimensional) {
		bdv.switch2D(twoDimensional);
	}

}