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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
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
import org.knime.node2012.ViewDocument.View;
import org.knime.node2012.ViewsDocument.Views;

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

    private Map<String, List<TableCellView>> m_cellViews;

    private JTabbedPane m_cellViewTabs;

    private ChangeListener m_changeListener;

    private int m_col = -1;

    private DataCell m_currentCell;

    private ListSelectionListener m_listSelectionListenerA;

    private ListSelectionListener m_listSelectionListenerB;

    private final int m_portIdx;

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

    private int m_row = -1;

    private JSplitPane m_sp;

    private TableContentView m_tableContentView;

    private TableContentModel m_tableModel;

    private TableView m_tableView;

    private Map<String, Component> m_viewComponents;

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

        // if neither the row nor the column changed, do nothing
        if ((row == m_row) && (col == m_col)) {
            return;
        }

        m_row = row;
        m_col = col;

        try {
            m_currentCell = m_tableContentView.getContentModel().getValueAt(row, col);
        } catch (final IndexOutOfBoundsException e2) {

            return;
        }
        final int selection = m_cellViewTabs.getSelectedIndex();
        m_cellViewTabs.removeAll();

        List<TableCellView> cellView;

        final String currentDataCellClass = m_currentCell.getClass().getCanonicalName();

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
                m_cellViewTabs.addTab("Viewer", new JLabel("This cell type doesn't provide a Table Cell Viewer!"));
                return;
            }
            m_cellViews.put(currentDataCellClass, cellView);
            for (final TableCellView v : cellView) {

                // cache the view component
                final Component comp = v.getViewComponent();
                m_viewComponents.put(currentDataCellClass + ":" + v.getName(), comp);

            }

        }

        // add the components to the tabs
        for (final TableCellView v : cellView) {
            try {
                m_cellViewTabs.addTab(v.getName(), m_viewComponents.get(currentDataCellClass + ":" + v.getName()));
            } catch (final Exception ex) {
                LOGGER.error("Could not add Tab " + v.getName(), ex);
            }
        }

        m_cellViewTabs.setSelectedIndex(Math.max(0, Math.min(selection, m_cellViewTabs.getTabCount() - 1)));

    }

    private void initViewComponents() {
        m_sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

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
        m_sp.add(m_tableView);
        m_cellViews = new HashMap<String, List<TableCellView>>();
        m_viewComponents = new HashMap<String, Component>();

        m_cellViewTabs = new JTabbedPane();
        m_tableContentView.setModel(m_tableModel);

        m_changeListener = new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                tabSelectionChanged();
            }
        };

        m_cellViewTabs.addChangeListener(m_changeListener);
        m_sp.add(m_cellViewTabs);
        setComponent(m_sp);
        m_sp.setDividerLocation(300);

        // add hilite menu
        getJMenuBar().add(m_tableView.createHiLiteMenu());
        m_tableView.setHiLiteHandler(getNodeModel().getInHiLiteHandler(0));
    }

    private void loadPortContent() {
        // UPDATE_EXECUTOR.execute(new Runnable() {
        // @Override
        // public void run() {
        m_tableModel = new TableContentModel();
        m_tableModel.setDataTable(getNodeModel().getInternalTables()[m_portIdx]);
        initViewComponents();
        // }
        // });
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

        if (m_cellViewTabs != null) {
            m_cellViewTabs.removeChangeListener(m_changeListener);
        }

        m_cellViews = null;
        m_viewComponents = null;
        m_cellViewTabs = null;
        m_sp = null;
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
    private void tabSelectionChanged() {

        if ((m_cellViewTabs.getSelectedIndex() == -1) || (m_currentCell == null)) {
            return;
        }

        m_cellViews.get(m_currentCell.getClass().getCanonicalName()).get(m_cellViewTabs.getSelectedIndex())
                .updateComponent(m_currentCell);

        // This fixes the bug on windows in the 3D Viewer where no
        // repainting
        // occurs if we just switch tabs without switching the image to
        // be
        // painted
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    m_cellViewTabs.repaint();
                }
            });
        }
    }

}
