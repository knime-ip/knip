/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2015
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
 * ---------------------------------------------------------------------
 *
 * Created on 19.04.2015 by Andi
 */
package org.knime.knip.base.nodes.view;

import java.awt.Component;

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

import org.knime.core.node.tableview.TableView;

//TODO: Update Doc
/**
 * This Class represents the Cell-side part of the TableCellViewer - plus some additional convenience features.
 *
 * @author Andreas Burger, University of Konstance
 */
public class TabbedCellView extends AbstractCellView {

    protected JTabbedPane m_cellView;

    public TabbedCellView(final TableView tableView) {
        super(tableView);

        m_cellView = new JTabbedPane();
        m_verticalSplit.setTopComponent(m_cellView);


    }

    /**
     * Add an additional tab to the tabbed pane held by this view.
     *
     * @param title - the title to be displayed in this tab
     * @param component - the component to be displayed when this tab is clicked
     * @see JTabbedPane#addTab(String, Component)
     */
    public void addTab(final String title, final Component component) {
        m_cellView.addTab(title, component);
    }

    /**
     * Add a changeListener to the tabbed pane held by this view.
     *
     * @param listener - the ChangeListener to add
     * @see JTabbedPane#addChangeListener(ChangeListener)
     */
    public void addTabChangeListener(final ChangeListener listener) {
        m_cellView.addChangeListener(listener);
    }

    /**
     * Remove a changeListener from the tabbed pane held by this view.
     *
     * @param listener - the ChangeListener to remove
     * @see JTabbedPane#removeChangeListener(ChangeListener)
     */
    public void removeTabChangeListener(final ChangeListener changeListener) {
        m_cellView.removeChangeListener(changeListener);

    }

    /**
     * Get the tab selected in the tabbed pane embedded in this view
     *
     * @return the index of the selected tab
     * @see JTabbedPane#getSelectedIndex()
     */
    public int getSelectedIndex() {
        return m_cellView.getSelectedIndex();
    }

    /**
     * Set the tab selected in the tabbed pane embedded in this view
     *
     * @param index - the index to be selected
     * @see JTabbedPane#setSelectedIndex(int)
     */
    public void setSelectedIndex(final int index) {
        m_cellView.setSelectedIndex(index);
    }

    /**
     * @see JTabbedPane#removeAll()
     */
    public void removeAllTabs() {
        m_cellView.removeAll();
    }

    /**
     * Get the number of tabs currently available in the tabbed pane of this view
     *
     * @return an integer specifying the number of tabbed pages
     * @see JTabbedPane#getTabCount()
     */
    public int getTabCount() {
        return m_cellView.getTabCount();
    }

}
