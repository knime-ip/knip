package org.knime.knip.bdv.uicomponents;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListCellRenderer;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import org.knime.knip.bdv.control.BDVHandlePanel;
import org.knime.knip.bdv.events.AddSourceEvent;
import org.knime.knip.bdv.events.ColorChangeEvent;
import org.knime.knip.bdv.events.DisplayModeFuseActiveEvent;
import org.knime.knip.bdv.events.DisplayModeGroupActiveEvent;
import org.knime.knip.bdv.events.GroupAddNewEvent;
import org.knime.knip.bdv.events.GroupRemoveEvent;
import org.knime.knip.bdv.events.GroupSelectedEvent;
import org.knime.knip.bdv.events.GroupVisibilityChangeEvent;
import org.knime.knip.bdv.events.ManualTransformEnableEvent;
import org.knime.knip.bdv.events.RemoveSourceEvent;
import org.knime.knip.bdv.events.RemoveSourceFromGroupEvent;
import org.knime.knip.bdv.events.SourceAddedToGroupEvent;
import org.knime.knip.bdv.events.SourceSelectionChangeEvent;
import org.knime.knip.bdv.events.SourceVisibilityChangeEvent;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;

import bdv.tools.transformation.ManualTransformActiveListener;
import bdv.tools.transformation.ManualTransformationEditor;
import bdv.viewer.DisplayMode;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.VisibilityAndGrouping.Event;
import bdv.viewer.VisibilityAndGrouping.UpdateListener;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.miginfocom.swing.MigLayout;

/**
 * The tabbed pane with all BDV-UI components.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 * @param <I>
 * @param <T>
 * @param <L>
 */
public class SelectionAndGroupingTabs<I extends IntegerType<I>, T extends NumericType<T>, L> extends JTabbedPane {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The UI foreground color.
	 */
	private static final Color FOREGROUND_COLOR = Color.darkGray;

	/**
	 * The UI background color.
	 */
	private static final Color BACKGROUND_COLOR = Color.white;

	/**
	 * Item to add new groups.
	 */
	private static final String NEW_GROUP = "<New Group>";

	/**
	 * The event service.
	 */
	private final EventService es;

	/**
	 * Combobox displaying all current sources.
	 */
	private JComboBox<String> sourcesComboBox;

	/**
	 * Combobox displaying all groups with an option to create new groups.
	 */
	private JComboBox<String> groupesComboBox;

	/**
	 * Map holding the source names mapped to the {@link SourceProperties}.
	 */
	private final Map<String, MetaSourceProperties<T>> sourceLookup = new HashMap<>();

	/**
	 * Map holding the group names mapped to the {@link GroupProperties}.
	 */
	private final Map<String, GroupProperties> groupLookup = new HashMap<>();

	/**
	 * The currently selected group.
	 */
	private int currentSelection;

	/**
	 * Eye icon normal size.
	 */
	private ImageIcon visibleIcon;

	/**
	 * Crossed eye icon normal size.
	 */
	private ImageIcon notVisibleIcon;

	/**
	 * Eye icon small.
	 */
	private ImageIcon visibleIconSmall;

	/**
	 * Crossed eye icon small.
	 */
	private ImageIcon notVisibleIconSmall;

	/**
	 * Label representing the visibility state of the source.
	 */
	private JLabel sourceVisibilityLabel;

	/**
	 * Only display selected source.
	 */
	private boolean singleSourceMode;

	/**
	 * Single source mode checkbox.
	 */
	private JCheckBox singleSourceModeCheckbox;

	/**
	 * Label representing the visibility state of the group.
	 */
	private JLabel groupVisibilityLabel;

	/**
	 * Single groupp mode checkbox.
	 */
	private JCheckBox singleGroupModeCheckbox;

	/**
	 * Remove the selected group button.
	 */
	private JButton removeGroup;

	/**
	 * Splitpane holding the selected sources and remaining (not selected) sources
	 * of a group.
	 */
	private JSplitPane selection;

	/**
	 * Sources which are part of the selected group.
	 */
	private JPanel selectedSources;

	/**
	 * Sources which are not part of the selected group.
	 */
	private JPanel remainingSources;

	/**
	 * Activity state of manual transformation.
	 */
	private boolean manualTransformationActive;

	/**
	 * Block event firing during source removal.
	 */
	private boolean block;

	/**
	 * This class holds the selection and grouping tab of the big data viewer UI.
	 * 
	 * @param es
	 *            the event service
	 * @param bdvHandlePanel
	 *            the bdv handle panel
	 */
	public SelectionAndGroupingTabs(final EventService es, final BDVHandlePanel<I, T, L> bdvHandlePanel) {
		this.es = es;

		es.subscribe(this);
		initialize();

		setupTabbedPane(es, bdvHandlePanel);
		addListeners(bdvHandlePanel.getViewerPanel().getVisibilityAndGrouping(),
				bdvHandlePanel.getManualTransformEditor());

	}

	/**
	 * Initialize components.
	 */
	private void initialize() {
		visibleIcon = new ImageIcon(SelectionAndGroupingTabs.class.getResource("visible.png"), "Visible");
		notVisibleIcon = new ImageIcon(SelectionAndGroupingTabs.class.getResource("notVisible.png"), "Not Visible");

		visibleIconSmall = new ImageIcon(SelectionAndGroupingTabs.class.getResource("visible_small.png"), "Visible");
		notVisibleIconSmall = new ImageIcon(SelectionAndGroupingTabs.class.getResource("notVisible_small.png"),
				"Not Visible");

		groupLookup.put("All", new GroupProperties("All", true));
	}

	/**
	 * Add tabs source and group to tabbed pane.
	 * 
	 * Also notify the bdv handle of tab switches.
	 * 
	 * @param es
	 *            event service
	 * @param bdvHandlePanel
	 *            bdv handle
	 */
	private void setupTabbedPane(final EventService es, final BDVHandlePanel<I, T, L> bdvHandlePanel) {
		UIManager.put("TabbedPane.contentAreaColor", BACKGROUND_COLOR);
		this.setUI(new CustomTabbedPaneUI());

		this.setBackground(BACKGROUND_COLOR);
		this.setForeground(FOREGROUND_COLOR);

		this.addTab("Source Control", createSourceControl(bdvHandlePanel));

		this.addTab("Group Control", createGroupControl());

		// Notify panel of the mode change.
		this.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				// nothing
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// nothing
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// nothing
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// nothing
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (getSelectedIndex() == 1) {
					es.publish(new DisplayModeGroupActiveEvent(true));
				} else {
					sourcesComboBox.setSelectedIndex(
							bdvHandlePanel.getViewerPanel().getVisibilityAndGrouping().getCurrentSource());
					es.publish(new DisplayModeGroupActiveEvent(false));
				}
			}
		});
	}

	/**
	 * Link the components to the BDV handle components to keep the state of bdv and
	 * UI consistent.
	 * 
	 * @param visibilityAndGrouping
	 * @param manualTransformationEditor
	 */
	private void addListeners(final VisibilityAndGrouping visibilityAndGrouping,
			final ManualTransformationEditor manualTransformationEditor) {

		manualTransformationEditor.addManualTransformActiveListener(new ManualTransformActiveListener() {

			@Override
			public void manualTransformActiveChanged(final boolean enabled) {
				setEnableSelectionAndGrouping(!enabled);
				manualTransformationActive = enabled;
			}
		});

		visibilityAndGrouping.addUpdateListener(new UpdateListener() {

			@Override
			public void visibilityChanged(Event e) {
				if (e.id == VisibilityAndGrouping.Event.DISPLAY_MODE_CHANGED) {
					final DisplayMode mode = visibilityAndGrouping.getDisplayMode();
					if (mode.equals(DisplayMode.FUSEDGROUP)) {
						singleGroupModeCheckbox.setSelected(false);
						singleSourceModeCheckbox.setSelected(false);
						singleSourceMode = false;

						setEnableVisibilityIcons(true);

						setSelectedIndex(1);
					} else if (mode.equals(DisplayMode.FUSED)) {
						singleGroupModeCheckbox.setSelected(false);
						singleSourceModeCheckbox.setSelected(false);
						singleSourceMode = false;

						setEnableVisibilityIcons(true);

						setSelectedIndex(0);
					} else if (mode.equals(DisplayMode.GROUP)) {
						singleGroupModeCheckbox.setSelected(true);
						singleSourceModeCheckbox.setSelected(true);
						singleSourceMode = true;

						setEnableVisibilityIcons(false);

						setSelectedIndex(1);
					} else {
						singleGroupModeCheckbox.setSelected(true);
						singleSourceModeCheckbox.setSelected(true);
						sourceVisibilityLabel.setEnabled(false);
						singleSourceMode = true;

						setEnableVisibilityIcons(false);

						setSelectedIndex(0);
					}
					sourcesComboBox.repaint();
					groupesComboBox.repaint();
				}
			}
		});
	}

	/**
	 * Add information of new source to the UI.
	 * 
	 * Put it into the corresponding group, set visibility and add it to the source
	 * selection.
	 * 
	 * @param event
	 */
	@EventHandler
	public synchronized void addSource(final AddSourceEvent<T> event) {
		block = true;
		final MetaSourceProperties<T> p = new MetaSourceProperties<T>(event.getSourceName(), event.getSourceID(),
				event.getType(), event.getGroupNames(), event.getColor(), event.getDims(), event.isVisible(),
				event.isLabeling());
		sourceLookup.put(p.getSourceName(), p);

		for (String groupName : event.getGroupNames()) {
			if (!groupLookup.containsKey(groupName)) {
				groupLookup.put(groupName, new GroupProperties(groupName, event.getSourceName(), event.isVisible()));
				groupesComboBox.addItem(groupName);
			} else {
				groupLookup.get(groupName).addSource(event.getSourceName());
			}

		}
		groupLookup.get("All").addSource(event.getSourceName());

		sourcesComboBox.addItem(p.getSourceName());
		sourcesComboBox.setSelectedItem(p.getSourceName());
		groupesComboBox.setSelectedIndex(1);
		block = false;
	}

	@EventHandler
	public synchronized void removeSource(final RemoveSourceEvent event) {

		for (final GroupProperties group : groupLookup.values()) {
			group.getSourceNames().remove(event.getSourceName());
		}
		block = true;
		sourcesComboBox.removeItem(event.getSourceName());
		block = false;
		sourceLookup.remove(event.getSourceName());

	}

	/**
	 * Toggle the visibility of a source.
	 * 
	 * @param event
	 */
	@EventHandler
	public void sourceVisibilityChanged(final SourceVisibilityChangeEvent event) {
		sourceLookup.get(event.getSourceName()).setVisible(event.isVisible());
		sourcesComboBox.repaint();
	}

	/**
	 * Toggle the visibility of a group.
	 * 
	 * @param event
	 */
	@EventHandler
	public void groupVisibilityChanged(final GroupVisibilityChangeEvent event) {
		groupLookup.get(event.getGroupName()).setVisible(event.isVisible());
		groupesComboBox.repaint();
	}

	/**
	 * Change between the different display modes.
	 * 
	 * If fuse mode is inactive the source can not be deactivated (set to not
	 * visible).
	 * 
	 * @param event
	 */
	@EventHandler
	public void fusedModeChanged(final DisplayModeFuseActiveEvent event) {
		setEnableVisibilityIcons(event.isActive());
	}

	private void setEnableVisibilityIcons(final boolean active) {
		if (!active) {
			groupVisibilityLabel.setEnabled(false);
			groupVisibilityLabel.setIcon(visibleIcon);
			sourceVisibilityLabel.setEnabled(false);
			sourceVisibilityLabel.setIcon(visibleIcon);
		} else {
			groupVisibilityLabel.setEnabled(true);
			if (groupLookup.get(groupesComboBox.getSelectedItem()).isVisible()) {
				groupVisibilityLabel.setIcon(visibleIcon);
			} else {
				groupVisibilityLabel.setIcon(notVisibleIcon);
			}
			sourceVisibilityLabel.setEnabled(true);
			if (sourceLookup.get(sourcesComboBox.getSelectedItem()).isVisible()) {
				sourceVisibilityLabel.setIcon(visibleIcon);
			} else {
				sourceVisibilityLabel.setIcon(notVisibleIcon);
			}
		}
	}

	/**
	 * Handle toggle between single source transformation or transformation of all
	 * sources.
	 * 
	 * @param event
	 */
	@EventHandler
	public void singleTransformationEvent(ManualTransformEnableEvent event) {
		setEnableSelectionAndGrouping(!event.isEnabled());
	}

	/**
	 * Toggle component enable.
	 * 
	 * @param active
	 *            state
	 */
	private void setEnableSelectionAndGrouping(final boolean active) {
		sourcesComboBox.setEnabled(active);
		singleSourceModeCheckbox.setEnabled(active);
		groupesComboBox.setEnabled(active);
		singleGroupModeCheckbox.setEnabled(active);
		selectedSources.setEnabled(active);
		remainingSources.setEnabled(active);
		for (Component c : selectedSources.getComponents()) {
			if (c instanceof JLabel)
				c.setEnabled(active);
		}
		for (Component c : remainingSources.getComponents()) {
			if (c instanceof JLabel)
				c.setEnabled(active);
		}
		removeGroup.setEnabled(active);
		this.setEnabled(active);
	}

	/**
	 * Build the source control panel.
	 * 
	 * @param bdvHandlePanel
	 *            the bdv handle
	 * @return the source contorl panel
	 */
	private Component createSourceControl(final BDVHandlePanel<I, T, L> bdvHandlePanel) {
		final JPanel p = new JPanel(new MigLayout("fillx", "[grow][][]", "[][]push[][]"));
		p.setBackground(BACKGROUND_COLOR);

		// source selection combobox
		sourcesComboBox = new JComboBox<>();
		sourcesComboBox.setMaximumSize(new Dimension(270, 30));
		sourcesComboBox.setRenderer(new SourceComboBoxRenderer());
		sourcesComboBox.setBackground(BACKGROUND_COLOR);

		p.add(sourcesComboBox, "growx");

		// source visibility icon (eye icon)
		sourceVisibilityLabel = new JLabel(visibleIcon);
		sourceVisibilityLabel.setBackground(BACKGROUND_COLOR);
		sourceVisibilityLabel.setToolTipText("Show source in fused mode.");
		sourceVisibilityLabel.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				if (!singleSourceMode) {
					boolean sourceActiveState = sourceLookup.get(sourcesComboBox.getSelectedItem()).isVisible();
					if (sourceActiveState) {
						sourceActiveState = !sourceActiveState;
						sourceVisibilityLabel.setIcon(notVisibleIcon);
					} else {
						sourceActiveState = !sourceActiveState;
						sourceVisibilityLabel.setIcon(visibleIcon);
					}
					sourceLookup.get(sourcesComboBox.getSelectedItem()).setVisible(sourceActiveState);
					es.publish(new SourceVisibilityChangeEvent((String) sourcesComboBox.getSelectedItem(),
							sourceActiveState));
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// nothing
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// nothing
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// nothing
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// nothing
			}
		});

		// color choser component
		final JButton colorButton = new JButton();
		colorButton.setPreferredSize(new Dimension(15, 15));
		colorButton.setBackground(BACKGROUND_COLOR);
		colorButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ev) {
				Color newColor = null;
				if (ev.getSource() == colorButton
						&& !sourceLookup.get(sourcesComboBox.getSelectedItem()).isLabeling()) {
					newColor = JColorChooser.showDialog(null, "Select Source Color",
							sourceLookup.get(sourcesComboBox.getSelectedItem()).getColor());
					if (newColor != null) {
						colorButton.setBackground(newColor);

						sourceLookup.get(sourcesComboBox.getSelectedItem()).setColor(newColor);
					}
				}
				es.publish(new ColorChangeEvent((String) sourcesComboBox.getSelectedItem(), newColor));
			}
		});

		// add action listener to source combobox
		sourcesComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == sourcesComboBox) {
					sourcesComboBox.setToolTipText((String) sourcesComboBox.getSelectedItem());
					final MetaSourceProperties<T> p = sourceLookup.get(sourcesComboBox.getSelectedItem());
					if (p != null) {
						if (!block)
							es.publish(new SourceSelectionChangeEvent<>(p));
						colorButton.setBackground(p.getColor());
						if (!singleSourceMode) {
							sourceVisibilityLabel.setEnabled(true);
							if (p.isVisible()) {
								sourceVisibilityLabel.setIcon(visibleIcon);
							} else {
								sourceVisibilityLabel.setIcon(notVisibleIcon);
							}
						} else {
							sourceVisibilityLabel.setIcon(visibleIcon);
							sourceVisibilityLabel.setEnabled(false);
						}
					}
				}
			}
		});

		p.add(colorButton);
		p.add(sourceVisibilityLabel, "wrap");

		// add information panel
		p.add(new InformationPanel<>(es), "span, growx, wrap");

		// single source mode checkbox to toggle between fused mode and single source
		// mode
		singleSourceModeCheckbox = new JCheckBox("Single Source Mode");
		singleSourceModeCheckbox.setBackground(BACKGROUND_COLOR);
		singleSourceModeCheckbox.setToolTipText("Display only the selected source.");
		singleSourceModeCheckbox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				es.publish(new DisplayModeFuseActiveEvent(!singleSourceModeCheckbox.isSelected()));
				singleGroupModeCheckbox.setSelected(singleSourceModeCheckbox.isSelected());
			}
		});

		// add range slider for intensity boundaries.
		final RangeSliderSpinnerPanel<I, T, L> ds = new RangeSliderSpinnerPanel<>(es, bdvHandlePanel, sourceLookup);
		ds.setPreferredSize(new Dimension(20, 20));
		p.add(ds, "span, growx, wrap");
		p.add(singleSourceModeCheckbox, "span, growx");
		return p;
	}

	/**
	 * Build the group control panel.
	 * 
	 * @return the group control panel
	 */
	private Component createGroupControl() {
		final JPanel p = new JPanel(new MigLayout("fillx", "[grow][][]", ""));
		p.setBackground(BACKGROUND_COLOR);

		groupesComboBox = new JComboBox<>();
		groupesComboBox.setMaximumSize(new Dimension(269, 30));
		groupesComboBox.setRenderer(new GroupComboBoxRenderer());
		groupesComboBox.setBackground(BACKGROUND_COLOR);
		groupesComboBox.setForeground(FOREGROUND_COLOR);
		// entry which opens the add-group dialog
		groupesComboBox.addItem(NEW_GROUP);
		// the default group containing all entries
		groupesComboBox.addItem("All");

		// remove group button
		removeGroup = new JButton("-");
		removeGroup.setForeground(FOREGROUND_COLOR);
		removeGroup.setBackground(BACKGROUND_COLOR);
		removeGroup.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == removeGroup) {
					es.publish(new GroupRemoveEvent((String) groupesComboBox.getSelectedItem()));
					int selectedIndex = groupesComboBox.getSelectedIndex();
					groupesComboBox.setSelectedIndex(1);
					groupesComboBox.removeItemAt(selectedIndex);
				}
			}
		});

		// panel which holds all sources which are part of the selected group
		selectedSources = new JPanel(new MigLayout("fillx", "[grow]", "[]"));
		selectedSources.setBackground(BACKGROUND_COLOR);
		selectedSources.setBorder(null);

		// panel which holds all sources which are NOT part of the selected group
		remainingSources = new JPanel(new MigLayout("fillx", "[grow]", "[]"));
		remainingSources.setBackground(BACKGROUND_COLOR);
		remainingSources.setBorder(null);

		// the split pane holding selected and remaining sources
		selection = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		selection.setPreferredSize(new Dimension(selection.getPreferredSize().width, 150));
		selection.setUI(new BasicSplitPaneUI() {
			public BasicSplitPaneDivider createDefaultDivider() {
				return new BasicSplitPaneDivider(this) {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void paint(Graphics g) {
						g.setColor(BACKGROUND_COLOR);
						g.fillRect(0, 0, getSize().width, getSize().height);
						super.paint(g);
					}
				};
			}
		});
		selection.setDividerLocation(70);
		selection.setBackground(BACKGROUND_COLOR);
		selection.setForeground(FOREGROUND_COLOR);
		selection.setBorder(null);
		final JScrollPane scrollPaneTop = new JScrollPane(selectedSources, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPaneTop.getVerticalScrollBar().setUI(new WhiteScrollBarUI());
		scrollPaneTop.getHorizontalScrollBar().setUI(new WhiteScrollBarUI());
		selection.setTopComponent(scrollPaneTop);
		final JScrollPane scrollPaneBottom = new JScrollPane(remainingSources, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPaneBottom.getVerticalScrollBar().setUI(new WhiteScrollBarUI());
		scrollPaneBottom.getHorizontalScrollBar().setUI(new WhiteScrollBarUI());
		selection.setBottomComponent(scrollPaneBottom);

		// Action listener handling the current group and updating selected and
		// remaining sources.
		// Also handles new group creation.
		groupesComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ev) {
				if (ev.getSource() == groupesComboBox) {
					Object selection = groupesComboBox.getSelectedItem();
					if (selection != null && selection instanceof String) {
						final String s = (String) selection;

						if (s.equals(NEW_GROUP)) {
							final String newGroupName = JOptionPane.showInputDialog(p, "New Group Name:");
							if (newGroupName != null && !newGroupName.isEmpty()) {
								if (groupLookup.containsKey(newGroupName)) {
									JOptionPane.showMessageDialog(p, "This group already exists.");
									groupesComboBox.setSelectedItem(newGroupName);
								} else {
									groupLookup.put(newGroupName, new GroupProperties(newGroupName, true));

									groupesComboBox.addItem(newGroupName);
									es.publish(new GroupAddNewEvent(newGroupName));
									groupesComboBox.setSelectedItem(newGroupName);
								}
							} else {
								groupesComboBox.setSelectedIndex(currentSelection);
							}
						}

						currentSelection = groupesComboBox.getSelectedIndex();
						if (getSelectedIndex() == 1)
							es.publish(new GroupSelectedEvent((String) groupesComboBox.getSelectedItem()));
						if (!singleSourceMode) {
							groupVisibilityLabel.setEnabled(true);
							if (groupLookup.get(groupesComboBox.getSelectedItem()).isVisible()) {
								groupVisibilityLabel.setIcon(visibleIcon);
							} else {
								groupVisibilityLabel.setIcon(notVisibleIcon);
							}
						} else {
							groupVisibilityLabel.setIcon(visibleIcon);
							groupVisibilityLabel.setEnabled(false);
						}

						selectedSources.removeAll();
						remainingSources.removeAll();

						sourceLookup.keySet().forEach(new Consumer<String>() {

							@Override
							public void accept(String t) {
								if (groupLookup.get(groupesComboBox.getSelectedItem()).getSourceNames().contains(t)) {
									selectedSources.add(createEntry(t), "growx, wrap");
								} else {
									remainingSources.add(createEntry(t), "growx, wrap");
								}
								repaintComponents();
							}

							private Component createEntry(String t) {
								final JLabel p = new JLabel(t);
								p.setBackground(BACKGROUND_COLOR);
								p.setForeground(FOREGROUND_COLOR);
								p.setBorder(null);
								p.addMouseListener(new MouseListener() {

									@Override
									public void mouseReleased(MouseEvent e) {
										if (!manualTransformationActive) {
											final GroupProperties group = groupLookup
													.get(groupesComboBox.getSelectedItem());
											if (group.getSourceNames().contains(t)) {
												group.removeSource(t);
												selectedSources.remove(p);
												remainingSources.add(p, "growx, wrap");
												es.publish(new RemoveSourceFromGroupEvent(t,
														(String) groupesComboBox.getSelectedItem()));
											} else {
												group.addSource(t);
												remainingSources.remove(p);
												selectedSources.add(p, "growx, wrap");
												es.publish(new SourceAddedToGroupEvent(t,
														(String) groupesComboBox.getSelectedItem()));
											}

											repaintComponents();
										}
									}

									@Override
									public void mousePressed(MouseEvent e) {
										// nothing
									}

									@Override
									public void mouseExited(MouseEvent e) {
										// nothing
									}

									@Override
									public void mouseEntered(MouseEvent e) {
										// nothing
									}

									@Override
									public void mouseClicked(MouseEvent e) {
										// nothing
									}
								});
								return p;
							}
						});

						removeGroup.setEnabled(groupesComboBox.getSelectedIndex() > 1);
					}
				}
			}

		});
		groupesComboBox.setSelectedIndex(-1);
		p.add(groupesComboBox, "growx");

		// label displaying the visibility state of the current group (eye icon)
		groupVisibilityLabel = new JLabel(visibleIcon);
		groupVisibilityLabel.setBackground(BACKGROUND_COLOR);
		groupVisibilityLabel.setBorder(null);
		groupVisibilityLabel.setToolTipText("Show group in fused-group mode.");
		groupVisibilityLabel.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				if (!singleSourceMode) {
					boolean groupActiveState = groupLookup.get(groupesComboBox.getSelectedItem()).isVisible();
					if (groupActiveState) {
						groupActiveState = !groupActiveState;
						groupVisibilityLabel.setIcon(notVisibleIcon);
					} else {
						groupActiveState = !groupActiveState;
						groupVisibilityLabel.setIcon(visibleIcon);
					}
					es.publish(new GroupVisibilityChangeEvent((String) groupesComboBox.getSelectedItem(),
							groupActiveState));
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// nothing
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// nothing
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// nothing
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// nothing
			}
		});

		p.add(groupVisibilityLabel);

		p.add(removeGroup, "growx, wrap");

		// checkbox to toggle between fused group mode and single group mode
		singleGroupModeCheckbox = new JCheckBox("Single Group Mode");
		singleGroupModeCheckbox.setBackground(BACKGROUND_COLOR);
		singleGroupModeCheckbox.setToolTipText("Display only the currently selected group.");
		singleGroupModeCheckbox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				es.publish(new DisplayModeFuseActiveEvent(!singleGroupModeCheckbox.isSelected()));
				singleSourceModeCheckbox.setSelected(singleGroupModeCheckbox.isSelected());
				sourcesComboBox.repaint();
				groupesComboBox.repaint();
			}
		});
		p.add(selection, "span, growx, wrap");

		p.add(singleGroupModeCheckbox, "span, growx");

		return p;
	}

	private void repaintComponents() {
		selectedSources.revalidate();
		remainingSources.revalidate();
		SelectionAndGroupingTabs.this.selection.revalidate();
		selectedSources.repaint();
		remainingSources.repaint();
		SelectionAndGroupingTabs.this.selection.repaint();
	}

	// A white look and feel for scroll bars.
	private final class WhiteScrollBarUI extends BasicScrollBarUI {
		@Override
		protected void configureScrollBarColors() {
			LookAndFeel.installColors(scrollbar, "ScrollBar.background", "ScrollBar.foreground");
			thumbHighlightColor = BACKGROUND_COLOR;
			thumbLightShadowColor = BACKGROUND_COLOR;
			thumbDarkShadowColor = BACKGROUND_COLOR;
			thumbColor = Color.lightGray;
			trackColor = BACKGROUND_COLOR;
			trackHighlightColor = BACKGROUND_COLOR;
		}

		@Override
		protected JButton createDecreaseButton(int orientation) {
			BasicArrowButton button = new BasicArrowButton(orientation, BACKGROUND_COLOR, BACKGROUND_COLOR,
					Color.lightGray, BACKGROUND_COLOR);
			button.setBorder(new LineBorder(BACKGROUND_COLOR));
			button.setBackground(BACKGROUND_COLOR);
			return button;
		}

		@Override
		protected JButton createIncreaseButton(int orientation) {
			BasicArrowButton button = new BasicArrowButton(orientation, BACKGROUND_COLOR, BACKGROUND_COLOR,
					Color.lightGray, BACKGROUND_COLOR);
			button.setBorder(new LineBorder(BACKGROUND_COLOR));
			button.setBackground(BACKGROUND_COLOR);
			return button;
		}
	}

	// A combobox renderer displaying the visibility state of the sources.
	class SourceComboBoxRenderer extends JLabel implements ListCellRenderer<String> {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
				boolean isSelected, boolean cellHasFocus) {

			if (value != null) {
				this.setText(value);
				this.setToolTipText(value);
				this.setIcon(visibleIconSmall);
				if (!singleSourceMode && !sourceLookup.get(value).isVisible()) {
					this.setIcon(notVisibleIconSmall);
				}
			} else {
				this.setIcon(null);
			}

			if (isSelected) {
				setForeground(Color.gray);
			} else {
				setForeground(FOREGROUND_COLOR);
			}

			return this;
		}

	}

	// A combobox renderer displaying the visibility state of the groups.
	class GroupComboBoxRenderer extends JLabel implements ListCellRenderer<String> {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
				boolean isSelected, boolean cellHasFocus) {

			if (value != null) {
				if (!value.equals(NEW_GROUP)) {
					this.setIcon(visibleIconSmall);
					if (!singleSourceMode && !groupLookup.get(value).isVisible()) {
						this.setIcon(notVisibleIconSmall);
					}
				} else {
					this.setIcon(null);
				}
				this.setText(value);
				this.setToolTipText(value);
			}

			if (isSelected) {
				setForeground(Color.gray);
			} else {
				setForeground(FOREGROUND_COLOR);
			}

			return this;
		}

	}
}
