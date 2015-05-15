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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
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
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.panels.ControlPanel;
import org.knime.knip.core.ui.imgviewer.panels.ImageToolTipButton;
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
public class TableCellViewNodeView<T extends NodeModel & BufferedDataTableHolder> extends NodeView<T> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TableCellViewNodeView.class);

    /**
     * Add the description of the view.
     *
     * @param views
     */
    public static void addViewDescriptionTo(final Views views) {
        final View view = views.addNewView();
        view.setIndex(new BigInteger("0"));
        view.setName("Table Cell View");

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
        view.setName("Table Cell View");

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

    protected Map<String, List<TableCellView>> m_cellViews;

    // protected JTabbedPane m_cellViewTabs;

    protected ChangeListener m_changeListener;

    protected ActionListener m_quickViewListener;

    protected ActionListener m_overviewListener;

    protected int m_col = -1;

    protected DataCell m_currentCell;

    protected ListSelectionListener m_listSelectionListenerA;

    protected ListSelectionListener m_listSelectionListenerB;

    protected final int m_portIdx;

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

    protected CellView m_cellView;

    protected Map<String, Component> m_viewComponents;

    protected boolean m_hiliteAdded = false;

    public TableCellViewNodeView(final T nodeModel) {
        this(nodeModel, 0);
    }

    /**
     * @param nodeModel
     */
    public TableCellViewNodeView(final T nodeModel, final int portIdx) {
        super(nodeModel);
        m_portIdx = portIdx;
        final JLabel load = new JLabel("Loading port content ...");
        load.setPreferredSize(new Dimension(500, 500));
        setComponent(load);

    }

    /*
     * called if the selected cell changes
     */
    private void cellSelectionChanged() {

        final int row = m_tableContentView.getSelectionModel().getLeadSelectionIndex();
        final int col = m_tableContentView.getColumnModel().getSelectionModel().getLeadSelectionIndex();

        cellSelectionChanged(row, col);
    }

    private boolean cellExists(final int row, final int col)
    {
        return (col >= 0 && col < m_tableModel.getColumnCount() && row >= 0 && row < m_tableModel.getRowCount());
    }

    /*
     * called if the selected cell changes
     */
    private void cellSelectionChanged(final int row, final int col) {

        // if neither the row nor the column changed, switch to view
        if ((row == m_row) && (col == m_col)) {
            setComponent(m_cellView);
            return;
        }

        try {
            m_currentCell = m_tableContentView.getContentModel().getValueAt(row, col);
        } catch (final IndexOutOfBoundsException e2) {

            return;
        }

        m_row = row;
        m_col = col;

        final int selection = m_cellView.getSelectedIndex();
        List<TableCellView> cellView;


        final String currentDataCellClass = m_currentCell.getClass().getCanonicalName();
        boolean isLabeling = false;
        if(m_tableModel.getValueAt(row, col) instanceof LabelingCell) {
            isLabeling = true;
        }

        // cache cell view
        if ((cellView = m_cellViews.get(currentDataCellClass)) == null) {
            // if the cell view wasn't loaded, yet, load
            // configuration and put
            // it to the cache
            cellView =
                    TableCellViewsManager.getInstance().createTableCellViews(m_currentCell.getType().getValueClasses());
            // if no cell view exists for the selected cell
            if (cellView.size() == 0) {
                m_currentCell = null;
                m_cellView.addTab("Viewer", new JLabel("This cell type doesn't provide a Table Cell Viewer!"));
                return;
            }
            m_cellViews.put(currentDataCellClass, cellView);
            for (final TableCellView v : cellView) {
                // cache the view component
                final Component comp = v.getViewComponent();
                if (v.getViewComponent() instanceof ImgViewer) {
                    // Register buttons
                    ImgViewer vc = (ImgViewer)v.getViewComponent();
                    ViewerComponent[] ctrls = vc.getControls();
                    for (int i = 0; i < 4; ++i) {
                        ImageToolTipButton b = (ImageToolTipButton)((ControlPanel)ctrls[i]).getButton();
                        if (i == 0) {
                            b.addActionListener(new ActionListener() {

                                @Override
                                public void actionPerformed(final ActionEvent arg0) {
                                    int r = m_row + 1;
                                    cellSelectionChanged(r, m_col);

                                }

                            });
                        }
                        if (i == 1) {
                            b.addActionListener(new ActionListener() {

                                @Override
                                public void actionPerformed(final ActionEvent arg0) {
                                    int c = m_col - 1;
                                    cellSelectionChanged(m_row, c);

                                }

                            });

                        }
                        if (i == 2) {
                            b.addActionListener(new ActionListener() {

                                @Override
                                public void actionPerformed(final ActionEvent arg0) {
                                    int c = m_col + 1;
                                    cellSelectionChanged(m_row, c);

                                }

                            });
                        }
                        if (i == 3) {
                            b.addActionListener(new ActionListener() {

                                @Override
                                public void actionPerformed(final ActionEvent arg0) {
                                    int r = m_row - 1;
                                    cellSelectionChanged(r, m_col);

                                }

                            });
                        }

                    }
                    vc.getQuickViewButton().addActionListener(m_quickViewListener);
                    vc.getOverViewButton().addActionListener(m_overviewListener);
                }
                m_viewComponents.put(currentDataCellClass + ":" + v.getName(), comp);

            }
        }

        // add the components to the tabs
        for (final TableCellView v : cellView) {
            try {
                m_cellView.addTab(v.getName(), m_viewComponents.get(currentDataCellClass + ":" + v.getName()));
                updateToolTips(row, col, isLabeling, v);
            } catch (final Exception ex) {
                LOGGER.error("Could not add Tab " + v.getName(), ex);
            }
        }

        m_cellView.setSelectedIndex(Math.max(0, Math.min(selection, m_cellView.getTabCount() - 1)));

        if (getComponent() != m_cellView) {
            setComponent(m_cellView);
            m_tableContentView.clearSelection();
        }

    }

    /**
     * @param row
     * @param col
     * @param isLabeling
     * @param v
     */
    private void updateToolTips(final int row, final int col, final boolean isLabeling, final TableCellView v) {
        if (v.getViewComponent() instanceof ImgViewer) {
            // Register buttons
            ImgViewer vc = (ImgViewer)v.getViewComponent();
            ViewerComponent[] ctrls = vc.getControls();
            for (int i = 0; i < 4; ++i) {
                ImageToolTipButton b = (ImageToolTipButton)((ControlPanel)ctrls[i]).getButton();
                if (i == 0) {
                    if(cellExists(m_row+1, col)) {
                        if(!isLabeling) {
                            b.setToolTipImage(((ImgPlusCell)m_tableModel.getValueAt(m_row+1, col)).getThumbnail(null));
                        } else {
                            b.setToolTipImage(((LabelingCell)m_tableModel.getValueAt(m_row+1, col)).getThumbnail(null));
                        }
                    }
                }
                if (i == 1) {

                    if(cellExists(row, col-1)) {
                        if(!isLabeling) {
                            b.setToolTipImage(((ImgPlusCell)m_tableModel.getValueAt(row, col-1)).getThumbnail(null));
                        } else {
                            b.setToolTipImage(((LabelingCell)m_tableModel.getValueAt(row, col-1)).getThumbnail(null));
                        }
                    }
                }
                if (i == 2) {

                    if(cellExists(row, col+1)) {
                        if(!isLabeling) {
                            b.setToolTipImage(((ImgPlusCell)m_tableModel.getValueAt(row, col+1)).getThumbnail(null));
                        } else {
                            b.setToolTipImage(((LabelingCell)m_tableModel.getValueAt(row, col+1)).getThumbnail(null));
                        }
                    }
                }
                if (i == 3) {

                    if(cellExists(m_row-1, col)) {
                        if(!isLabeling) {
                            b.setToolTipImage(((ImgPlusCell)m_tableModel.getValueAt(m_row-1, col)).getThumbnail(null));
                        } else {
                            b.setToolTipImage(((LabelingCell)m_tableModel.getValueAt(m_row-1, col)).getThumbnail(null));
                        }
                    }
                }

            }
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
                setComponent(m_tableView);
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

        initializeListeners();

        // Initialize CellView
        m_cellView = new CellView(m_tableView);

        m_cellView.addTabChangeListener(m_changeListener);

        //        Temporarily add loadpanel as component, so that the ui stays responsive.
        setComponent(loadpanel);
    }

    /**
     * Creates the underlying TableView and registers it to this dialog.
     */
    private void initializeView() {
        m_tableContentView = new TableContentView();

        m_tableContentView.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        m_listSelectionListenerA = new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                cellSelectionChanged();
            }
        };

        m_tableContentView.getSelectionModel().addListSelectionListener(m_listSelectionListenerA);
        m_listSelectionListenerB = new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                cellSelectionChanged();
            }
        };


        m_tableContentView.getColumnModel().getSelectionModel().addListSelectionListener(m_listSelectionListenerB);
        m_tableView = new TableView(m_tableContentView);

        m_tableView.setHiLiteHandler(getNodeModel().getInHiLiteHandler(0));

        if (!m_hiliteAdded) {
            getJMenuBar().add(m_tableView.createHiLiteMenu());
            m_hiliteAdded = true;
        }

        m_cellViews = new HashMap<String, List<TableCellView>>();
        m_viewComponents = new HashMap<String, Component>();
    }

    /**
     * Creates the Listeners used in the displayed views.
     */
    private void initializeListeners() {
        m_quickViewListener = new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_cellView.setTableViewVisible(!m_cellView.isTableViewVisible());

            }
        };

        m_overviewListener = new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                if (getComponent() == m_cellView) {
                    if (m_cellView.isTableViewVisible()) {
                        m_cellView.setTableViewVisible(false);
                    }
                    m_tableContentView.clearSelection();
                    setComponent(m_tableView);
                } else {
                    // Should not happen.
                }

            }
        };
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
            if (m_cellViews != null) {
                for (final String key : m_cellViews.keySet()) {
                    for (final TableCellView v : m_cellViews.get(key)) {
                        v.onReset();
                    }
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

        if (m_cellViews != null) {
            for (final String key : m_cellViews.keySet()) {
                for (final TableCellView v : m_cellViews.get(key)) {
                    v.onClose();
                }
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

        m_cellViews = null;
        m_viewComponents = null;
        m_cellView = null;

        m_tableContentView = null;
        m_tableModel = null;
        m_currentCell = null;
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

        if ((m_cellView.getSelectedIndex() == -1) || (m_currentCell == null)) {
            return;
        }

        m_cellViews.get(m_currentCell.getClass().getCanonicalName()).get(m_cellView.getSelectedIndex())
                .updateComponent(m_currentCell);

    }
}

//}
