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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

import org.knime.core.node.tableview.TableView;

/**
 *
 * @author Andreas Burger
 */
public class CellView extends JPanel {

    private JTabbedPane m_cellView;

    private final TableView m_tableView;

    private JPanel m_tablePanel;

    private boolean m_isVisible = false;

    private JSplitPane bg;

    public CellView(final TableView tableView) {
        m_tableView = tableView;
        m_cellView = new JTabbedPane();

        bg = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;

//        add(m_cellView, gbc);
//
//        gbc.gridy++;
//        gbc.gridheight = GridBagConstraints.REMAINDER;
//        gbc.fill = GridBagConstraints.HORIZONTAL;
        m_tablePanel = new JPanel(new BorderLayout());
//        add(m_tablePanel, gbc);
        bg.setTopComponent(m_cellView);
        add(bg, gbc);

    }

    public void addTab(final String title, final Component component) {
        m_cellView.addTab(title, component);
    }

    public void addTabChangeListener(final ChangeListener listener) {
        m_cellView.addChangeListener(listener);
    }

    public void removeTabChangeListener(final ChangeListener changeListener) {
        m_cellView.removeChangeListener(changeListener);

    }

    public int getSelectedIndex() {
        return m_cellView.getSelectedIndex();
    }

    public void setSelectedIndex(final int max) {
        m_cellView.setSelectedIndex(max);
    }

    public void removeAllTabs() {
        m_cellView.removeAll();
    }

    public int getTabCount() {
        return m_cellView.getTabCount();
    }

    public void setTableViewVisible(final boolean isVisible) {
        m_isVisible = isVisible;
        if(isVisible) {
            m_tablePanel.add(m_tableView, BorderLayout.CENTER);
            bg.setBottomComponent(m_tablePanel);
            bg.setDividerLocation(0.9);
        } else {
            m_tablePanel.removeAll();
            bg.remove(2);
        }
        validate();
    }

    /**
     * @return
     */
    public boolean isTableViewVisible() {
        // TODO Auto-generated method stub
        return m_isVisible;
    }

}
