/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.base.nodes.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableView;
import org.knime.knip.cellviewer.CellViewsManager;
import org.knime.knip.cellviewer.interfaces.CellViewFactory;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.event.EventServiceClient;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.events.TableOverviewDisableEvent;
import org.knime.knip.core.ui.imgviewer.events.TablePositionEvent;
import org.knime.knip.core.ui.imgviewer.panels.ViewerControlEvent;
import org.knime.knip.core.ui.imgviewer.panels.ViewerScrollEvent;
import org.knime.knip.core.ui.imgviewer.panels.ViewerScrollEvent.Direction;
import org.knime.knip.core.util.waitingindicator.WaitingIndicatorUtils;
import org.knime.node.v210.ViewDocument.View;
import org.knime.node.v210.ViewsDocument.Views;

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003, 2010
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
 *
 * History
 *   29 Jan 2010 (hornm): created
 */

/**
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author <a href="mailto:jonathan.hale@uni.kn">Jonathan Hale</a>
 *
 * @param <T> {@link NodeModel} subclass this {@link TableCellViewNodeView} belongs to.
 */
public class TableCellViewNodeView<T extends NodeModel & BufferedDataTableHolder> extends NodeView<T>
        implements EventServiceClient {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TableCellViewNodeView.class);

    /**
     * Add the description of the view.
     *
     * @param views
     */
    public static void addViewDescriptionTo(final Views views) {
        final View view = views.addNewView();
        view.setIndex(new BigInteger("0"));
        view.setName("Image Viewer");

        final Map<CellViewFactory, List<String>> descs =
                CellViewsManager.getInstance().getTableCellViewDescriptions();
        view.newCursor()
                .setTextValue("Another, possibly interactive, view on table cells. Displays the selected cells with their associated viewer if it exists. Available views are:");
        view.addNewBr();
        for (final Entry<CellViewFactory, List<String>> entry : descs.entrySet()) {

            view.addB("- " + entry.getKey().getCellViewName());
            view.addNewBr();
            for (final String d : entry.getValue()) {
                view.addI("-- " + d);
                view.addNewBr();
            }

        }

    }

    /**
     * Add the description of the view.
     *
     * @param views
     *
     * @deprecated Consider using {@link org.knime.node.v210.ViewsDocument.Views}
     */
    @Deprecated
    public static void addViewDescriptionTo(final org.knime.node2012.ViewsDocument.Views views) {
        final org.knime.node2012.ViewDocument.View view = views.addNewView();
        view.setIndex(0);
        view.setName("Image Viewer");

        final Map<Class<? extends DataValue>, List<String>> descs =
                TableCellViewsManager.getInstance().getTableCellViewDescriptions();
        view.newCursor()
                .setTextValue("Another, possibly interactive, view on table cells. Displays the selected cells with their associated viewer if it exists. Available views are:");
        view.addNewBr();
        for (final Entry<Class<? extends DataValue>, List<String>> entry : descs.entrySet()) {

            view.addB("- " + entry.getKey().getSimpleName());
            view.addNewBr();
            for (final String d : entry.getValue()) {
                view.addI("-- " + d);
                view.addNewBr();
            }

        }

    }

    protected Map<String, TableCellView> m_cellViewCache;

    protected Map<TableCellViewFactory, List<TableCellView>> m_factoryCache;

    // protected JTabbedPane m_cellViewTabs;

    protected ChangeListener m_changeListener;

    protected ActionListener m_bottomQuickViewListener;

    protected ActionListener m_leftQuickViewListener;

    protected ActionListener m_OverviewListener;

    protected int m_col = -1;

    protected List<DataCell> m_currentCells = new LinkedList<DataCell>();

    protected List<TableCellView> m_currentCachedViews;

    protected ListSelectionListener m_listSelectionListenerA;

    protected ListSelectionListener m_listSelectionListenerB;

    protected final int m_portIdx;

    protected boolean m_adjusting;

    protected JPanel m_tableViewPanel;

    protected static final String m_defStatusBarText =
            "Click on a cell or drag and select multiple cells to continue ...";

    // private static final ExecutorService UPDATE_EXECUTOR = Executors
    // .newCachedThreadPool(new ThreadFactory() {
    // private final AtomicInteger m_counter = new AtomicInteger();
    //
    // @Override
    // public Thread newThread(final Runnable r) {
    // Thread t = new Thread(
    // r,
    // "TableCellViewer-Updater-"
    // + m_counter.incrementAndGet());
    // t.setDaemon(true);
    // return t;
    // }
    // });

    protected int m_row = -1;

    protected TableContentView m_tableContentView;

    protected TableContentModel m_tableModel;

    protected TableView m_tableView;

    protected TabbedCellView m_cellView;

    protected Map<String, Component> m_viewComponents;

    protected boolean m_hiliteAdded = false;

    protected final EventService m_eventService;

    private PlaneSelectionEvent m_planeSel;

    private int m_prevSelectedIndex = -1;

    private JLabel m_statusLabel;

    public TableCellViewNodeView(final T nodeModel) {
        this(nodeModel, 0);
    }

    /**
     * @param nodeModel
     */
    public TableCellViewNodeView(final T nodeModel, final int portIdx) {
        super(nodeModel);
        m_portIdx = portIdx;
        m_eventService = new EventService();
        m_eventService.subscribe(this);
        final JLabel load = new JLabel("Loading port content ...");
        load.setPreferredSize(new Dimension(500, 500));
        setComponent(load);

    }

    /*
     * called if the selected cell changes
     */
    private void cellSelectionChanged() {

        //        if (m_adjusting) {
        //            return;
        //        }

        final int row = m_tableContentView.getSelectionModel().getLeadSelectionIndex();
        final int col = m_tableContentView.getColumnModel().getSelectionModel().getLeadSelectionIndex();

        cellSelectionChanged(row, col);
    }

    /**
     * Helper function to check if a call to getValueAt(row, col) would return an IOOBEx
     **/
    private boolean cellExists(final int row, final int col) {
        return (col >= 0 && col < m_tableModel.getColumnCount() && row >= 0 && row < m_tableModel.getRowCount());
    }

    /*
     * called if the selected cell changes
     */
    private void cellSelectionChanged(final int row, final int col) {
        if (cellExists(row, col)) {
            int[] cols = new int[]{col};
            int[] rows = new int[]{row};
            rowColIntervalSelectionChanged(rows, cols);
        }
    }

    private void rowColIntervalSelectionChanged(final int[] rowIndices, final int[] colIndices) {

        final int selection = m_cellView.getSelectedIndex();

        List<Class<? extends DataValue>> prefClasses = new LinkedList<Class<? extends DataValue>>();
        DataCell currentCell;

        int col = -1;
        int row = -1;

        m_currentCells.clear();

        for (int i : rowIndices) {
            for (int j : colIndices) {

                try {
                    currentCell = m_tableContentView.getContentModel().getValueAt(i, j);
                } catch (final IndexOutOfBoundsException e2) {

                    return;
                }
                col = j;
                row = i;
                m_currentCells.add(currentCell);
                prefClasses.add(currentCell.getType().getPreferredValueClass());

            }
        }

        if (m_currentCells.size() == 0) {
            return;
        } else {
            if (m_currentCells.size() == 1) {
                m_col = col;
                m_row = row;
                m_eventService.publish(new TablePositionEvent(m_tableModel.getColumnCount(), m_tableModel.getRowCount(),
                                                              col + 1, row + 1, m_tableModel.getColumnName(col), m_tableModel.getRowKey(row).toString()));
            } else {

            }
        }

        m_cellView.removeAllTabs();

        List<TableCellViewFactory> compatibleFactories =
                TableCellViewsManager.getInstance().getCompatibleFactories(prefClasses);
        List<TableCellView> availableViews = new LinkedList<TableCellView>();

        if (compatibleFactories.isEmpty()) {
            return;
        }

        for (TableCellViewFactory f : compatibleFactories) {
            if (m_factoryCache.containsKey(f)) {
                availableViews.addAll(m_factoryCache.get(f));
            } else {
                List<TableCellView> views = new LinkedList<TableCellView>(Arrays.asList(f.createTableCellViews()));
                m_factoryCache.put(f, views);
                availableViews.addAll(views);
            }
        }

        for (final TableCellView v : availableViews) {
            try {
                m_cellView.addTab(v.getName(), v.getViewComponent());
                //  updateToolTips(row, col, v);
            } catch (final Exception ex) {
                LOGGER.error("Could not add Tab " + v.getName(), ex);
            }
        }
        m_adjusting = true;
        m_cellView.setSelectedIndex(Math.max(0, Math.min(selection, m_cellView.getTabCount() - 1)));

        m_adjusting = false;

        if (m_currentCells.size() == 1) {
            m_adjusting = true;
            m_cellView.scrollTablesToIndex(row, col);

            m_adjusting = false;
        } else {
            m_eventService.publish(new TableOverviewDisableEvent(false, false));
        }

        if (getComponent() != m_cellView) {
            setComponent(m_cellView);
        }
    }

    protected void initViewComponents() {

        // Initialize tableView
        initializeView();

        // Load data into view. Disable a waiting indicator while doing so.
        final JPanel loadpanel = new JPanel();
        loadpanel.setPreferredSize(new Dimension(600, 400));

        // Show waiting indicator and work in background.
        WaitingIndicatorUtils.setWaiting(loadpanel, true);
        SwingWorker<T, Integer> worker = new SwingWorker<T, Integer>() {

            @Override
            protected T doInBackground() throws Exception {
                m_tableContentView.setModel(m_tableModel);

                return null;
            }

            @Override
            protected void done() {
                WaitingIndicatorUtils.setWaiting(loadpanel, false);
                setComponent(m_tableViewPanel);
            }
        };

        worker.execute();
        while (!worker.isDone()) {
            //do nothing
        }
        m_changeListener = new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                tabSelectionChanged();
            }
        };

        // Initialize CellView
        m_cellView = new TabbedCellView(m_tableView);
        m_cellView.setEventService(m_eventService);

        m_cellView.addTabChangeListener(m_changeListener);

        //        Temporarily add loadpanel as component, so that the ui stays responsive.
        setComponent(loadpanel);

    }

    /**
     * Creates the underlying TableView and registers it to this dialog.
     */
    private void initializeView() {
        m_tableContentView = new TableContentView();

        m_tableContentView.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        m_listSelectionListenerA = new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                if (!m_adjusting) {
                    rowColIntervalSelectionChanged(m_tableContentView.getSelectedRows(),
                                                   m_tableContentView.getSelectedColumns());
                }
            }
        };

        m_tableContentView.getSelectionModel().addListSelectionListener(m_listSelectionListenerA);
        m_listSelectionListenerB = new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                if (!m_adjusting) {
                    rowColIntervalSelectionChanged(m_tableContentView.getSelectedRows(),
                                                   m_tableContentView.getSelectedColumns());
                }
            }
        };

        m_tableContentView.getColumnModel().getSelectionModel().addListSelectionListener(m_listSelectionListenerB);
        m_tableView = new TableView(m_tableContentView);
        m_tableViewPanel = new JPanel(new BorderLayout());
        m_tableViewPanel.add(m_tableView, BorderLayout.CENTER);

        JPanel statusBar = new JPanel();
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        m_tableViewPanel.add(statusBar, BorderLayout.SOUTH);
        statusBar.setPreferredSize(new Dimension(m_tableViewPanel.getWidth(), 16));
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));
        m_statusLabel = new JLabel(m_defStatusBarText);
        m_statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusBar.add(m_statusLabel);

        m_tableView.setHiLiteHandler(getNodeModel().getInHiLiteHandler(0));

        if (!m_hiliteAdded) {
            getJMenuBar().add(m_tableView.createHiLiteMenu());
            m_hiliteAdded = true;
        }

        m_cellViewCache = new HashMap<String, TableCellView>();

        m_factoryCache = new HashMap<TableCellViewFactory, List<TableCellView>>();

        m_viewComponents = new HashMap<String, Component>();
    }

    protected void loadPortContent() {
        m_tableModel = new TableContentModel();
        m_tableModel.setDataTable(getNodeModel().getInternalTables()[m_portIdx]);

        initViewComponents();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        if ((getNodeModel().getInternalTables() == null) || (getNodeModel().getInternalTables().length == 0)
                || (getNodeModel().getInternalTables()[m_portIdx] == null)) {
            if (m_cellViewCache != null) {
                for (final TableCellView v : m_cellViewCache.values()) {
                    v.onReset();
                }
            }
            m_row = -1;
            m_col = -1;
            final JLabel nodata = new JLabel("No data table available!");
            nodata.setPreferredSize(new Dimension(500, 500));
            setComponent(nodata);
        } else {
            loadPortContent();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {

        if (m_cellViewCache != null) {
            for (final TableCellView v : m_cellViewCache.values()) {
                v.onClose();
            }
        }

        if (m_tableContentView != null) {
            m_tableContentView.getSelectionModel().removeListSelectionListener(m_listSelectionListenerA);
            m_tableContentView.getColumnModel().getSelectionModel()
                    .removeListSelectionListener(m_listSelectionListenerB);
        }

        if (m_cellView != null) {
            m_cellView.removeTabChangeListener(m_changeListener);
        }

        m_cellViewCache = null;
        m_viewComponents = null;
        m_cellView = null;

        m_tableContentView = null;
        m_tableModel = null;
        //        m_currentCell = null;
        m_tableView = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // NB

    }

    /*
     * called if the selected tab changes
     */
    protected void tabSelectionChanged() {

        if ((m_cellView.getSelectedIndex() == -1) || (m_currentCells.size() == 0)) {
            m_prevSelectedIndex = -1;
            return;
        }

        if (m_cellView.getSelectedIndex() == m_prevSelectedIndex) {
            return;
        }

        m_prevSelectedIndex = m_cellView.getSelectedIndex();

        List<Class<? extends DataValue>> prefClasses = new LinkedList<Class<? extends DataValue>>();
        for (DataCell c : m_currentCells) {
            prefClasses.add(c.getType().getPreferredValueClass());
        }

        List<TableCellViewFactory> compatibleFactories =
                TableCellViewsManager.getInstance().getCompatibleFactories(prefClasses);

        int selected = m_cellView.getSelectedIndex();
        int i;
        for (i = 0; i < compatibleFactories.size(); ++i) {
            if (m_factoryCache.get(compatibleFactories.get(i)).size() <= selected) {
                selected -= m_factoryCache.get(compatibleFactories.get(i)).size();
            } else {
                break;
            }
        }

        TableCellView cv = m_factoryCache.get(compatibleFactories.get(i)).get(selected);

        cv.updateComponent(m_currentCells);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {

    }

    @EventListener
    public void onViewerScrollEvent(final ViewerScrollEvent e) {

        if (e.getDirection() == Direction.NORTH) {
            cellSelectionChanged(m_row - 1, m_col);
        }
        if (e.getDirection() == Direction.EAST) {
            cellSelectionChanged(m_row, m_col + 1);
        }
        if (e.getDirection() == Direction.WEST) {
            cellSelectionChanged(m_row, m_col - 1);
        }
        if (e.getDirection() == Direction.SOUTH) {
            cellSelectionChanged(m_row + 1, m_col);
        }

    }

    @EventListener
    public void onPlaneSel(final PlaneSelectionEvent e) {
        m_planeSel = e;
    }

    @EventListener
    public void onOverviewToggle(final ViewerControlEvent e) {
        if (getComponent() == m_cellView) {
            if (m_cellView.isTableViewVisible()) {
                m_cellView.hideTableView();
            }
            m_tableContentView.clearSelection();
            setComponent(m_tableViewPanel);
        }

    }

}

//}
