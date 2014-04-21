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
package org.knime.knip.base.nodes.testing.TableCellViewer;

import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;

import org.knime.core.data.DataValue;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.knip.base.nodes.view.TableCellView;
import org.knime.knip.base.nodes.view.TableCellViewNodeView;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
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
 * @author Andreas Burger, University of Konstanz
 */
public class TestTableCellViewNodeView<T extends NodeModel & BufferedDataTableHolder> extends TableCellViewNodeView<T> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TestTableCellViewNodeView.class);

    /**
     * Add the description of the view.
     *
     * @param views
     */
    public static void addViewDescriptionTo(final Views views) {
        final View view = views.addNewView();
        view.setIndex(0);
        view.setName("Test Table Cell View");

        final Map<Class<? extends DataValue>, List<String>> descs =
                TestTableCellViewsManager.getInstance().getTableCellViewDescriptions();
        view.newCursor()
                .setTextValue("This views purpose is to enable testing of the underlying classes. In order to achieve this goal, the following test-views are used: ");
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

    private List<HiddenImageLogger> m_logger = new LinkedList<HiddenImageLogger>();

    private boolean isLogging = false;

    /**
     * @param nodeModel
     */
    public TestTableCellViewNodeView(final T nodeModel) {
        this(nodeModel, 0);
    }

    /**
     * @param nodeModel
     * @param portIdx
     */
    public TestTableCellViewNodeView(final T nodeModel, final int portIdx) {
        super(nodeModel, portIdx);
        final JLabel load = new JLabel("Loading port content ...");
        load.setPreferredSize(new Dimension(500, 500));
        setComponent(load);

    }

    /**
     * Same as {@link TestTableCellViewNodeView#TestTableCellViewNodeView(NodeModel, int)}, but allows enabling of image
     * logging
     *
     * @param nodeModel
     * @param portIdx
     * @param loggingEnabled
     */
    public TestTableCellViewNodeView(final T nodeModel, final int portIdx, final boolean loggingEnabled) {
        this(nodeModel, portIdx);
        isLogging = loggingEnabled;
    }

    /**
     * Replaces the old CellSelectionChanged method and forwards the appropriate call to the new one.
     */
    private void cellSelectionChanged() {
        cellSelectionChanged(-1, -1);
    }

    /**
     * Called if the selected cell changes or should change. If any of the passed values is -1, the current selection of
     * the content view will be used, otherwise the passed index is selected.
     *
     * @param setCol Column to select
     * @param setRow Row to select
     */
    private void cellSelectionChanged(final int setCol, final int setRow) {
        final int row, col;
        if (setCol == -1 || setRow == -1) {
            row = m_tableContentView.getSelectionModel().getLeadSelectionIndex();
            col = m_tableContentView.getColumnModel().getSelectionModel().getLeadSelectionIndex();
        } else {
            row = setRow;
            col = setCol;
        }

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
                    TestTableCellViewsManager.getInstance().createTableCellViews(m_currentCell.getType()
                                                                                         .getValueClasses());

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

                // add the image-logger to every component before storing it.
                ImgViewer viewer = (ImgViewer)comp;
                HiddenImageLogger logger = new HiddenImageLogger();
                viewer.addViewerComponent(logger);
                m_logger.add(logger);

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

    /**
     * Returns all loggers associated with views in this View.
     * @return A list of loggers if there exist any, an empty list otherwise
     */
    protected List<HiddenImageLogger> getImageLogger() {
        return m_logger;
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
        m_tableContentView = null;
        m_currentCell = null;
        m_logger.clear();
    }


    /**
     * <b>Selects all rows and columns once.</b><br>
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        loadPortContent();

        for (int i = 0; i < m_tableContentView.getRowCount(); ++i) {
            cellSelectionChanged(0, i);

            for (int j = 1; j < m_cellViewTabs.getTabCount(); ++j) {
                m_cellViewTabs.setSelectedIndex(j);


            }
        }

    }

}
