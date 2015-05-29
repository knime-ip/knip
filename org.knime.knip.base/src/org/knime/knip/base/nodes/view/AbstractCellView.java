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
 * Created on May 29, 2015 by pop210958
 */
package org.knime.knip.base.nodes.view;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.knime.core.node.tableview.TableView;

/**
 *
 * @author pop210958
 */
public abstract class AbstractCellView extends JPanel {

    public static enum TableDir {
        BOTTOM, LEFT
    }

    protected JPanel m_tablePanel;

    protected final TableView m_tableView;

    private boolean m_isBottomVisible = false;

    private boolean m_isLeftVisible = false;

    protected JSplitPane m_verticalSplit;

    protected JSplitPane m_horizontalSplit;

    public AbstractCellView(final TableView tableView) {

        m_tableView = tableView;

        m_verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        m_horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;


        m_horizontalSplit.setRightComponent(m_verticalSplit);

        m_tablePanel = new JPanel(new BorderLayout());

        add(m_horizontalSplit, gbc);

    }

    /**
     * Sets the visibility of the TableView given by its location in the View.
     *
     * @param isVisible - boolean specifying the desired visibility state
     * @param d The location of the table whose visibility will be set
     */
    public void setTableViewVisible(final boolean isVisible, final TableDir d) {
        m_tablePanel.add(m_tableView);
        if (d == TableDir.BOTTOM) {
            if (isVisible) {
                closeTableView(TableDir.LEFT);
                m_isLeftVisible = !isVisible;
                showTableView(d);
            } else {
                closeTableView(d);
            }
            m_isBottomVisible = isVisible;
        } else {

            if (isVisible) {
                closeTableView(TableDir.BOTTOM);
                m_isBottomVisible = !isVisible;
                showTableView(d);
            } else {
                closeTableView(d);
            }
            m_isLeftVisible = isVisible;
        }
        validate();
    }

    private void closeTableView(final TableDir d) {
        if (d == TableDir.BOTTOM) {
            if (m_isBottomVisible) {
                m_verticalSplit.remove(2);
            }
        } else {
            if (m_isLeftVisible) {
                m_horizontalSplit.remove(2);
            }
        }
    }

    private void showTableView(final TableDir d) {

        if (d == TableDir.BOTTOM) {

            m_verticalSplit.setBottomComponent(m_tablePanel);
            m_verticalSplit.setDividerLocation(this.getHeight()
                    - (m_tableView.getColumnHeaderViewHeight() + m_tableView.getRowHeight() + m_verticalSplit
                            .getDividerSize()));

        } else {

            m_horizontalSplit.setLeftComponent(m_tablePanel);
            m_horizontalSplit.setDividerLocation(m_tableView.getRowHeader().getWidth() + m_tableView.getColumnWidth()
                    + m_verticalSplit.getDividerSize());
        }
    }

    /**
     * Sets the visibility of both TableViews.
     *
     * @param isVisible - boolean specifying the desired visibility state
     * @param d The location of the table whose visibility will be set
     */
    public void hideTableViews() {
        closeTableView(TableDir.LEFT);
        closeTableView(TableDir.BOTTOM);
        m_isBottomVisible = false;
        m_isLeftVisible = false;

        validate();
    }

    public boolean isTableViewVisible() {
        return m_isLeftVisible || m_isBottomVisible;
    }

    /**
     * @param d The direction of the table whose visibility shall be checked
     * @return whether the TableView is currently visible or not
     */
    public boolean isTableViewVisible(final TableDir d) {
        if (d == TableDir.LEFT) {
            return m_isLeftVisible;
        } else {
            return m_isBottomVisible;
        }
    }

}