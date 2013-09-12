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
package org.knime.knip.base.nodes.features;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * Dialog component collection which arranges the {@link DialogComponent} the one hand as usual (e.g. as be done in the
 * {@link DefaultNodeSettingsPane}. But some specific dialog components (@see
 * {@link FeatureDialogComponentCollection#addDialogComponent(String, DialogComponent)} will be added to a configuration
 * pane of a feature set (identified by a String).
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class FeatureDialogComponentCollection extends DialogComponentCollection {

    private final SettingsModelStringArray m_activeFeatureSetSettings;

    private final List<DialogComponent> m_allDialogComponents;

    private final HashMap<String, HashMap<String, List<DialogComponent>>> m_dialogComponents;

    private final HashMap<String, List<DialogComponent>> m_featureSetProvidersDialogComponents;

    private JTable m_featureSetTable;

    /**
     * @param activeFeatureSetSettings settings to store the activated feature sets
     */
    public FeatureDialogComponentCollection(final SettingsModelStringArray activeFeatureSetSettings) {
        m_activeFeatureSetSettings = activeFeatureSetSettings;

        m_dialogComponents = new HashMap<String, HashMap<String, List<DialogComponent>>>();
        m_featureSetProvidersDialogComponents = new HashMap<String, List<DialogComponent>>();
        m_allDialogComponents = new ArrayList<DialogComponent>();
    }

    /**
     * {@inheritDoc} Adds the Dialog Component to the feature list!
     */
    @Override
    public void addDialogComponent(final String features, final DialogComponent dc) {
        if (!m_featureSetProvidersDialogComponents.containsKey(features)) {
            m_featureSetProvidersDialogComponents.put(features, new ArrayList<DialogComponent>());
        }

        m_featureSetProvidersDialogComponents.get(features).add(dc);

    }

    @Override
    public void addDialogComponent(final String tab, final String group, final DialogComponent dc) {
        if (!m_dialogComponents.containsKey(tab)) {
            m_dialogComponents.put(tab, new LinkedHashMap<String, List<DialogComponent>>());
        }

        final HashMap<String, List<DialogComponent>> groups = m_dialogComponents.get(tab);

        if (!groups.containsKey(group)) {
            groups.put(group, new ArrayList<DialogComponent>());
        }

        groups.get(group).add(dc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkConsistency(final List<SettingsModel> settingsModels) {
        for (final SettingsModel sm : settingsModels) {
            for (final DialogComponent dc : m_allDialogComponents) {
                if (sm == dc.getModel()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public NodeDialogPane getDialog() {

        final int maxCheckBoxColumnWidth = 12;

        return new NodeDialogPane() {

            private Set<String> m_active;

            private JPanel m_featurePanel;

            private JPanel m_featureSetComponents;

            {

                // add standard dialog components
                for (final String tab : m_dialogComponents.keySet()) {
                    final JPanel tabPanel = createNewPanel();
                    super.addTab(tab, tabPanel);
                    for (final String group : m_dialogComponents.get(tab).keySet()) {
                        final JPanel panel = new JPanel();
                        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), group));
                        for (final DialogComponent dc : m_dialogComponents.get(tab).get(group)) {
                            panel.add(dc.getComponentPanel());
                        }

                        tabPanel.add(panel);
                    }

                }

                m_active = new HashSet<String>();

                // add feature dialog components
                m_featurePanel = new JPanel(new BorderLayout());
                m_featureSetComponents = createNewPanel();
                m_featureSetComponents.setPreferredSize(new Dimension(400, 150));

                // setting up the table containing the features
                // set list and the
                // checkboxes to active/inactivate them
                m_featureSetTable = new JTable(new AbstractTableModel() {

                    /**
                     * 
                     */
                    private static final long serialVersionUID = -7197084943688726222L;

                    private List<String> m_featureSets;

                    {
                        m_featureSets = new ArrayList<String>();
                        for (final String s : m_featureSetProvidersDialogComponents.keySet()) {
                            m_featureSets.add(s);
                        }
                    }

                    @Override
                    public java.lang.Class<?> getColumnClass(final int arg0) {
                        if (arg0 == 0) {
                            return Boolean.class;
                        } else {
                            return String.class;
                        }
                    }

                    @Override
                    public int getColumnCount() {
                        return 2;
                    }

                    @Override
                    public int getRowCount() {
                        return m_featureSets.size();
                    }

                    @Override
                    public Object getValueAt(final int rowIndex, final int columnIndex) {
                        if (columnIndex == 1) {
                            return m_featureSets.get(rowIndex);
                        } else {
                            return m_active.contains(m_featureSets.get(rowIndex));
                        }
                    }
                });

                m_featureSetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                m_featureSetTable.getColumnModel().getColumn(0).setMaxWidth(maxCheckBoxColumnWidth);
                m_featureSetTable.setShowGrid(false);
                m_featureSetTable.setPreferredSize(new Dimension(150, 100));
                m_featureSetTable.setTableHeader(null);

                addTab("Features", m_featurePanel);
                m_featurePanel.add(m_featureSetTable, BorderLayout.WEST);
                m_featurePanel.add(new JScrollPane(m_featureSetComponents), BorderLayout.CENTER);

                m_featureSetTable.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseReleased(final MouseEvent e) {
                        final String currentSet =
                                m_featureSetTable.getValueAt(m_featureSetTable.getSelectedRow(), 1).toString();

                        if (m_featureSetTable.getSelectedColumn() == 0) {

                            if (m_active.contains(currentSet)) {
                                m_active.remove(currentSet);
                            } else {
                                m_active.add(currentSet);
                            }
                            m_featureSetTable.updateUI();
                        }

                        m_featureSetComponents.removeAll();
                        for (final DialogComponent dc : m_featureSetProvidersDialogComponents.get(currentSet)) {
                            m_featureSetComponents.add(dc.getComponentPanel());
                            dc.getModel().setEnabled(m_active.contains(currentSet));
                        }

                        m_featureSetComponents.updateUI();

                    }
                });

            }

            /**
             * @param horizontal <code>true</code> if the layout is horizontal
             * @return the box
             */
            private Box createBox(final boolean horizontal) {
                final Box box;
                if (horizontal) {
                    box = new Box(BoxLayout.X_AXIS);
                    box.add(Box.createVerticalGlue());
                } else {
                    box = new Box(BoxLayout.Y_AXIS);
                    box.add(Box.createHorizontalGlue());
                }
                return box;
            }

            private JPanel createNewPanel() {
                final JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                final Box box = createBox(false);
                panel.add(box);
                return panel;
            }

            @Override
            protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
                    throws NotConfigurableException {
                for (final String tab : m_dialogComponents.keySet()) {
                    for (final String group : m_dialogComponents.get(tab).keySet()) {
                        for (final DialogComponent dc : m_dialogComponents.get(tab).get(group)) {
                            dc.loadSettingsFrom(settings, specs);
                        }
                    }
                }

                for (final String feature : m_featureSetProvidersDialogComponents.keySet()) {
                    for (final DialogComponent dc : m_featureSetProvidersDialogComponents.get(feature)) {
                        dc.loadSettingsFrom(settings, specs);
                    }
                }

                try {
                    m_activeFeatureSetSettings.loadSettingsFrom(settings);
                    m_active.addAll(Arrays.asList(m_activeFeatureSetSettings.getStringArrayValue()));
                    for (final Entry<String, List<DialogComponent>> entry : m_featureSetProvidersDialogComponents
                            .entrySet()) {
                        for (final DialogComponent dc : entry.getValue()) {
                            dc.getModel().setEnabled(m_active.contains(entry.getKey()));
                        }
                    }
                    m_featureSetComponents.updateUI();

                } catch (final InvalidSettingsException ex) {
                    ex.printStackTrace();
                }

            }

            @Override
            protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

                for (final String tab : m_dialogComponents.keySet()) {
                    for (final String group : m_dialogComponents.get(tab).keySet()) {
                        for (final DialogComponent dc : m_dialogComponents.get(tab).get(group)) {
                            dc.saveSettingsTo(settings);
                        }
                    }
                }

                for (final String feature : m_featureSetProvidersDialogComponents.keySet()) {
                    for (final DialogComponent dc : m_featureSetProvidersDialogComponents.get(feature)) {
                        dc.saveSettingsTo(settings);
                    }
                }

                m_activeFeatureSetSettings.setStringArrayValue(m_active.toArray(new String[m_active.size()]));
                m_activeFeatureSetSettings.saveSettingsTo(settings);
            }
        };
    }

}
