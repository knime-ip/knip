package org.knime.knip.cellviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataValue;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableView;
import org.knime.knip.cellviewer.interfaces.CellView;
import org.knime.knip.cellviewer.interfaces.CellViewFactory;
import org.knime.knip.cellviewer.panels.NavigationPanel;

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003, 2016
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */

/**
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:andreas.burger@uni-konstanz.de">Andreas Burger</a>
 * @author <a href="mailto:jonathan.hale@uni.kn">Jonathan Hale</a>
 *
 * @param <T>
 *            {@link NodeModel} subclass this {@link CellNodeView} belongs to.
 */
public class CellNodeView<T extends NodeModel & BufferedDataTableHolder> extends NodeView<T> {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(CellNodeView.class);

	protected Map<String, CellView> m_cellViewCache;

	protected final int m_portIdx;

	protected JTabbedPane m_viewsPane;

	List<DataValue> m_cellValues;

	protected JPanel m_currentView;

	protected CellView m_currentProvider;

	protected JPanel m_contentPanel;

	protected JSplitPane m_splitPane;

	protected int m_col = -1;

	protected int m_row = -1;

	protected TableContentView m_tableContentView;

	protected TableContentModel m_tableModel;

	protected TableView m_tableView;

	protected boolean m_hiliteAdded = false;

	protected boolean m_tableExpanded = false;

	private JLabel m_statusLabel;

	protected static final String m_defStatusBarText = "Click on a cell or drag and select multiple cells to continue ...";

	private NavigationPanel m_navPanel;

	private boolean m_updatingTabs;

	public CellNodeView(final T nodeModel) {
		this(nodeModel, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	public CellNodeView(final T nodeModel, final int portIdx) {
		super(nodeModel);
		m_portIdx = portIdx;
		final JLabel load = new JLabel("Loading port content ...");
		load.setPreferredSize(new Dimension(1024, 1024));
		setComponent(load);

	}

	/**
	 * Helper function to check if a call to getValueAt(row, col) would return
	 * an IOOBEx
	 **/
	private boolean cellExists(final int row, final int col) {
		if (m_tableModel == null) {
			return false;
		} else {
			return (col >= 0 && col < m_tableModel.getColumnCount() && row >= 0 && row < m_tableModel.getRowCount());
		}
	}

	/**
	 * Use this method to change the selection to a single Cell
	 * 
	 * @param row
	 *            The row-index of the cell
	 * @param col
	 *            The column-index of the cell
	 */
	private void cellSelectionChanged(final int row, final int col) {
		if (cellExists(row, col)) {
			int[] cols = new int[] { col };
			int[] rows = new int[] { row };
			rowColIntervalSelectionChanged(rows, cols);
		}
	}

	/**
	 * This method is called whenever the selection changes.
	 * 
	 * @param rowIndices
	 *            An array holding the row-indices of the selected cells
	 * @param colIndices
	 *            An array holding the column-indices of the selected cells
	 */
	private void rowColIntervalSelectionChanged(final int[] rowIndices, final int[] colIndices) {

		if (rowIndices.length == 0 || colIndices.length == 0)
			return;

		// Extract classes and count of selected Cells

		List<Class<? extends DataValue>> preferredClasses;

		preferredClasses = generateClassList(rowIndices, colIndices);
		m_cellValues = generateValuesList(rowIndices, colIndices);

		// Get providing factories of registered compatible views

		List<CellViewFactory> compatibleFactories;

		compatibleFactories = CellViewsManager.getInstance().getCompatibleFactories(preferredClasses);

		// Update the navigation panel
		if (rowIndices.length == 1 && colIndices.length == 1) {
			m_col = colIndices[0];
			m_row = rowIndices[0];
			m_navPanel.updatePosition(m_tableModel.getColumnCount(), m_tableModel.getRowCount(), m_col + 1, m_row + 1,
					m_tableContentView.getColumnName(m_col), m_tableModel.getRowKey(m_row).toString());
		} else {
			m_navPanel.disableButtons();
		}

		if (compatibleFactories.isEmpty())
			return;

		// Check for cached instances of the compatible views. In case of
		// miss,
		// instantiate and cache

		for (CellViewFactory fac : compatibleFactories) {
			if (!m_cellViewCache.containsKey(fac.getCellViewName())) {
				m_cellViewCache.put(fac.getCellViewName(), fac.createCellView());
			}
		}

		// From here on, compatible views are guaranteed to be cached

		String selected = "";

		if (m_viewsPane.getTabCount() > 0) {
			int index = m_viewsPane.getSelectedIndex();
			if (index != -1)
				selected = m_viewsPane.getTitleAt(index);
			m_viewsPane.removeAll();
		}

		m_updatingTabs = true;

		// Add all compatible views to the tabbed pane
		for (CellViewFactory fac : compatibleFactories) {
			try {
				CellView p = m_cellViewCache.get(fac.getCellViewName());
				m_viewsPane.insertTab(fac.getCellViewName(), null, p.getViewComponent(), "", m_viewsPane.getTabCount());
			} catch (Exception e) {
				LOGGER.error("Error while adding tab " + fac.getCellViewName());
			}
		}

		// Keep the current viewer selected if possible.
		if (!selected.isEmpty()) {
			int index = m_viewsPane.indexOfTab(selected);

			if (index != -1)
				m_viewsPane.setSelectedIndex(index);
			else
				m_viewsPane.setSelectedIndex(0);
		} else {
			m_viewsPane.setSelectedIndex(0);
		}

		m_updatingTabs = false;

		// Update the viewers contents
		CellView p = m_cellViewCache.get(m_viewsPane.getTitleAt(m_viewsPane.getSelectedIndex()));
		p.updateComponent(m_cellValues);

		// Ensure the content is shown, not the table overview
		if (!getComponent().equals(m_contentPanel))
			setComponent(m_contentPanel);

	}

	/**
	 * Generates a list of preferred classes by checking the given
	 * cell-coordinates.
	 */
	private List<Class<? extends DataValue>> generateClassList(final int[] rowIndices, final int[] colIndices) {
		List<Class<? extends DataValue>> result = new LinkedList<Class<? extends DataValue>>();
		for (int i : rowIndices) {
			for (int j : colIndices) {
				if (cellExists(i, j)) {
					result.add(
							m_tableContentView.getContentModel().getValueAt(i, j).getType().getPreferredValueClass());
				}
			}
		}
		return result;
	}

	/**
	 * Fetches a list of values from the table using the provided indices.
	 */
	private List<DataValue> generateValuesList(final int[] rowIndices, final int[] colIndices) {
		List<DataValue> result = new LinkedList<DataValue>();
		for (int i : rowIndices) {
			for (int j : colIndices) {
				if (cellExists(i, j)) {
					result.add(m_tableContentView.getContentModel().getValueAt(i, j));
				}
			}
		}
		return result;
	}

	protected void initViewComponents() {

		// Initialize tableView
		initializeView();

		m_contentPanel = createPanel();

		// Load data into view. Disable a waiting indicator while doing so.
		final JPanel loadpanel = new JPanel(new BorderLayout());
		loadpanel.setPreferredSize(new Dimension(1024, 768));
		final JLabel loadlabel = new JLabel("Loading ...");
		loadlabel.setHorizontalAlignment(JLabel.CENTER);
		loadpanel.add(loadlabel, BorderLayout.CENTER);

		// Show waiting indicator and work in background.
		SwingWorker<T, Integer> worker = new SwingWorker<T, Integer>() {

			@Override
			protected T doInBackground() throws Exception {
				m_tableContentView.setModel(m_tableModel);
				return null;
			}

			@Override
			protected void done() {
				setComponent(m_tableView);
			}
		};

		worker.execute();
		while (!worker.isDone()) {
			// do nothing
		}

		// Temporarily add loadpanel as component, so that the ui stays
		// responsive.
		setComponent(loadpanel);

	}

	/**
	 * Creates the underlying TableView and registers it to this dialog.
	 */
	private void initializeView() {
		m_tableContentView = new TableContentView();

		m_tableContentView.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

		ListSelectionListener listener = new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				// Ensures that the listener fires exactly once per selection
				if (m_tableContentView.getSelectionModel().getValueIsAdjusting()
						&& m_tableContentView.getColumnModel().getSelectionModel().getValueIsAdjusting()) {
					return;
				}
				rowColIntervalSelectionChanged(m_tableContentView.getSelectedRows(),
						m_tableContentView.getSelectedColumns());
			}
		};

		m_tableContentView.getSelectionModel().addListSelectionListener(listener);

		m_tableContentView.getColumnModel().getSelectionModel().addListSelectionListener(listener);

		m_tableView = new TableView(m_tableContentView);

		m_tableView.setHiLiteHandler(getNodeModel().getInHiLiteHandler(0));

		if (!m_hiliteAdded) {
			getJMenuBar().add(m_tableView.createHiLiteMenu());
			m_hiliteAdded = true;
		}

		// Set preferred height to ~ 1 row
		m_tableView.setPreferredSize(new Dimension(0, (m_tableView.getRowHeight() + 16)));

		m_cellViewCache = new HashMap<String, CellView>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void modelChanged() {
		if ((getNodeModel().getInternalTables() == null) || (getNodeModel().getInternalTables().length == 0)
				|| (getNodeModel().getInternalTables()[m_portIdx] == null)) {
			if (m_cellViewCache != null) {
				for (final CellView v : m_cellViewCache.values()) {
					v.onReset();
				}
			}
			m_row = -1;
			m_col = -1;
			final JLabel nodata = new JLabel("No data table available!");
			nodata.setPreferredSize(new Dimension(500, 500));
			setComponent(nodata);
		} else {
			m_tableModel = new TableContentModel();
			m_tableModel.setDataTable(getNodeModel().getInternalTables()[m_portIdx]);

			initViewComponents();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onClose() {

		if (m_cellViewCache != null) {
			for (final CellView v : m_cellViewCache.values()) {
				v.onClose();
			}
		}

		m_cellViewCache = null;
		m_tableContentView = null;
		m_tableModel = null;
		m_tableView = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onOpen() {

	}

	public void toggleOverview() {
		if (getComponent().equals(m_contentPanel)) {
			setComponent(m_tableView);
			m_tableExpanded = false;
			m_tableContentView.clearSelection();
		}
	}

	/**
	 * Creates the main panel of the dialog.
	 * 
	 * @return The main panel of the dialog-frame.
	 */
	private JPanel createPanel() {
		JPanel result = new JPanel();
		result.setLayout(new BorderLayout());

		m_splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		result.add(m_splitPane, BorderLayout.CENTER);

		m_splitPane.setTopComponent(createTopPanel());
		m_splitPane.setResizeWeight(1);

		JPanel statusBar = new JPanel();
		statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
		result.add(statusBar, BorderLayout.SOUTH);
		statusBar.setPreferredSize(new Dimension(0, 16));
		statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));
		m_statusLabel = new JLabel(m_defStatusBarText);
		m_statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusBar.add(m_statusLabel);

		return result;
	}

	/**
	 * Creates the top component of the split pane, i.e. the actual panel
	 * containing the viewers and the navigation.
	 * 
	 * @return The top panel of the split pane.
	 */
	private JComponent createTopPanel() {
		JPanel result = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridheight = 1;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridx = 0;

		m_viewsPane = new JTabbedPane();
		result.add(m_viewsPane, gbc);

		gbc.gridheight = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 0;

		JComponent navbar = createNavBar();

		result.add(navbar, gbc);

		m_viewsPane.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (m_viewsPane.getSelectedIndex() == -1 || m_updatingTabs)
					return;

				String title = m_viewsPane.getTitleAt(m_viewsPane.getSelectedIndex());
				CellView p = m_cellViewCache.get(title);

				p.updateComponent(m_cellValues);

			}

		});

		return result;
	}

	/**
	 * Creates the Navigation panel at the bottom of the view.
	 * 
	 * @return The navigation panel
	 */
	private JComponent createNavBar() {
		Box navbar = new Box(BoxLayout.X_AXIS);

		navbar.add(Box.createHorizontalGlue());

		navbar.add(Box.createRigidArea(new Dimension(10, 40)));

		Box firstPanel = new Box(BoxLayout.X_AXIS);

		JButton overviewButton = new JButton("Back to Table");
		overviewButton.setMnemonic(KeyEvent.VK_B);
		firstPanel.add(overviewButton);

		overviewButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				toggleOverview();
			}

		});

		firstPanel.add(Box.createHorizontalStrut(20));

		Box quickViewButtonPanel = new Box(BoxLayout.X_AXIS);

		JToggleButton quickViewButton = new JToggleButton("Expand Table View");

		quickViewButtonPanel.add(quickViewButton);

		firstPanel.add(quickViewButtonPanel);

		firstPanel.add(Box.createHorizontalStrut(20));

		Box colourButtonPanel = new Box(BoxLayout.X_AXIS);

		firstPanel.add(colourButtonPanel);

		navbar.add(firstPanel);

		navbar.add(Box.createHorizontalGlue());

		quickViewButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				if (!m_tableExpanded)
					showTableView();
				else
					hideTableView();

				m_tableExpanded = !m_tableExpanded;
			}

		});

		m_navPanel = new NavigationPanel();
		navbar.add(m_navPanel);
		navbar.add(Box.createHorizontalGlue());

		Box thirdPanel = new Box(BoxLayout.X_AXIS);
		thirdPanel.add(Box.createHorizontalGlue());

		navbar.add(thirdPanel);

		m_navPanel.getUpButton().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				m_tableContentView.changeSelection(m_row - 1, m_col, false, false);

			}
		});

		m_navPanel.getDownButton().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				m_tableContentView.changeSelection(m_row + 1, m_col, false, false);

			}
		});

		m_navPanel.getLeftButton().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				m_tableContentView.changeSelection(m_row, m_col - 1, false, false);

			}
		});

		m_navPanel.getRightButton().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				m_tableContentView.changeSelection(m_row, m_col + 1, false, false);

			}
		});

		return navbar;
	}

	private void showTableView() {
		m_splitPane.setBottomComponent(m_tableView);
		m_splitPane.setDividerLocation(m_contentPanel.getHeight() - (m_tableView.getColumnHeaderViewHeight()
				+ m_tableView.getRowHeight() + m_splitPane.getDividerSize() + 4 + 16 /* Navbar */));

		m_tableView.scrollRectToVisible(m_tableContentView.getCellRect(m_row, 0, true));
	}

	private void hideTableView() {
		m_splitPane.remove(2);
	}

}
