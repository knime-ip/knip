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
package org.knime.knip.core.ui.imgviewer.panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;

import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableUtils;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.ForcePlanePosEvent;
import org.knime.knip.core.ui.imgviewer.events.HilitedLabelsChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelPanelHiliteSelectionChgEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelPanelIsHiliteModeEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelPanelVisibleLabelsChgEvent;
import org.knime.knip.core.ui.imgviewer.events.NameSetbasedLabelFilter;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter.Operator;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;
import org.knime.knip.core.util.MiscViews;

/**
 * Panel to generate a Rulebased LabelFilter.
 * 
 * Publishes {@link RulebasedLabelFilter}
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelFilterPanel<L extends Comparable<L>> extends ViewerComponent {

    private static final long serialVersionUID = 1L;

    private JList m_jLabelList;

    private Vector<L> m_activeLabels;

    private EventService m_eventService;

    private JScrollPane m_scrollPane;

    private RulebasedLabelFilter<L> m_ruleFilter;

    private NameSetbasedLabelFilter<L> m_hiliteFilter;

    private List<JTextField> m_textFields;

    private JComboBox m_operatorBox;

    private JPanel m_textFieldsPanel;

    private HashSet<String> m_hilitedLabels;

    private Labeling<L> m_labeling;

    private boolean m_hMode = false; // state of highlighting mode

    private final JTabbedPane m_filterTabbs = new JTabbedPane();

    private JScrollPane m_filters;

    private boolean m_showHilitedOnly = false;

    private boolean m_showUnhilitedOnly = false;

    private JPopupMenu m_contextMenu;

    private JMenuItem m_hiliteSelected;

    private JMenuItem m_unhiliteSelected;

    public LabelFilterPanel() {
        this(false);
    }

    public LabelFilterPanel(final boolean enableHilite) {
        super("Labels/Filter", false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        m_ruleFilter = new RulebasedLabelFilter<L>();
        m_hiliteFilter = new NameSetbasedLabelFilter<L>(false);

        m_textFields = new ArrayList<JTextField>();

        m_activeLabels = new Vector<L>();

        m_jLabelList = new JList();
        m_jLabelList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        m_contextMenu = createContextMenu(enableHilite);

        // TODO
        m_jLabelList.addMouseListener(new MouseAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void mousePressed(final MouseEvent evt) {
                if (evt.getButton() == MouseEvent.BUTTON3) {
                    showMenu(evt);
                }
            }
        });

        m_jLabelList.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                          final boolean isSelected, final boolean cellHasFocus) {
                final Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                c.setForeground(Color.BLACK);

                if ((m_hilitedLabels != null) && m_hilitedLabels.contains(value.toString())) {

                    if (isSelected) {
                        c.setBackground(LabelingColorTableUtils.HILITED_SELECTED);
                    } else {
                        c.setBackground(LabelingColorTableUtils.HILITED);
                    }

                } else if (isSelected) {
                    c.setBackground(LabelingColorTableUtils.SELECTED);
                } else {
                    c.setBackground(LabelingColorTableUtils.STANDARD);
                }

                return c;
            }
        });

        m_scrollPane = new JScrollPane(m_jLabelList);
        m_scrollPane.setPreferredSize(new Dimension(150, 1));

        final JPanel confirmationPanel = new JPanel();
        confirmationPanel.setLayout(new BoxLayout(confirmationPanel, BoxLayout.X_AXIS));

        m_textFieldsPanel = new JPanel();
        m_textFieldsPanel.setLayout(new BoxLayout(m_textFieldsPanel, BoxLayout.Y_AXIS));

        final JButton filterButton = new JButton("Filter");
        filterButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                doFilter();
            }
        });

        final JButton addButton = new JButton("+");
        addButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_filterTabbs.setSelectedIndex(1);
                addTextField("");
            }
        });

        m_operatorBox = new JComboBox(RulebasedLabelFilter.Operator.values());
        m_operatorBox.setSize(new Dimension(40, 22));
        m_operatorBox.setMaximumSize(new Dimension(40, 22));

        confirmationPanel.add(addButton);
        confirmationPanel.add(m_operatorBox);
        confirmationPanel.add(filterButton);

        m_filters = new JScrollPane(m_textFieldsPanel);
        add(m_filterTabbs);
        m_filterTabbs.add("Labels", m_scrollPane);
        m_filterTabbs.add("Filter Rules", m_filters);
        add(confirmationPanel);
    }

    protected void addTextField(final String initValue) {
        final JPanel oneFieldRow = new JPanel();
        oneFieldRow.add(new JLabel("Rule " + (m_textFields.size() + 1) + ":"));
        oneFieldRow.setLayout(new BoxLayout(oneFieldRow, BoxLayout.X_AXIS));

        final JTextField newField = new JTextField(initValue);
        newField.setPreferredSize(new Dimension(70, 20));
        newField.setMaximumSize(new Dimension(70, 20));
        oneFieldRow.add(newField);

        final JButton removeButton = new JButton("-");
        removeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {

                for (final Component p : removeButton.getParent().getComponents()) {

                    if (p instanceof JTextField) {
                        m_textFields.remove(p);
                    }

                }
                m_textFieldsPanel.remove(removeButton.getParent());
                updateUI();
                doFilter();

            }
        });

        oneFieldRow.add(removeButton);
        m_textFields.add(newField);
        m_textFieldsPanel.add(oneFieldRow);

        updateUI();

    }

    private void updateHiliteFilter() {
        // legal states false,false true,false false, true
        assert (m_showHilitedOnly && (m_showUnhilitedOnly != true));

        if (!m_showHilitedOnly && !m_showUnhilitedOnly) {
            // show all
            m_hiliteFilter.clear();
        } else if (m_showHilitedOnly) {
            // only hilited
            final HashSet<String> filterSet = new HashSet<String>();
            if ((m_hilitedLabels != null) && (m_hilitedLabels.size() > 0)) {
                for (final L o : m_labeling.getLabels()) {
                    if (!m_hilitedLabels.contains(o.toString())) {
                        filterSet.add(o.toString());
                    }
                }
            } else {
                for (final L o : m_labeling.getLabels()) {
                    filterSet.add(o.toString());
                }
            }
            m_hiliteFilter.setFilterSet(filterSet);
        } else {
            // only unhilited
            if ((m_hilitedLabels != null) && (m_hilitedLabels.size() > 0)) {
                m_hiliteFilter.setFilterSet((HashSet<String>)m_hilitedLabels.clone());
            } else {
                m_hiliteFilter.clear();
            }
        }
    }

    protected void doFilter() {
        try {
            final Set<String> allLabels = new HashSet<String>();
            m_ruleFilter.clear();
            for (int i = 0; i < m_textFields.size(); i++) {
                m_ruleFilter.addRules(RulebasedLabelFilter.formatRegExp(m_textFields.get(i).getText()));
            }
            m_activeLabels.clear();

            // filter with hilites
            Collection<L> filtered = m_ruleFilter.filterLabeling(m_labeling.firstElement().getMapping().getLabels());

            // filter with rules
            if (m_showHilitedOnly || m_showUnhilitedOnly) {
                filtered = m_hiliteFilter.filterLabeling(filtered);
            }

            m_activeLabels.addAll(filtered);

            for (final L label : filtered) {
                allLabels.add(label.toString());
            }

            // As this is faster than checking all labels
            if ((m_ruleFilter.getRules().size() == 0) && !m_showHilitedOnly && !m_showUnhilitedOnly) {
                m_eventService.publish(new LabelPanelVisibleLabelsChgEvent(null, null));
            } else {
                m_eventService.publish(new LabelPanelVisibleLabelsChgEvent(allLabels, (Operator)(m_operatorBox)
                        .getSelectedItem()));
            }

            m_eventService.publish(new ImgRedrawEvent());
            Collections.sort(m_activeLabels);
            m_jLabelList.setListData(m_activeLabels);
        }

        catch (final NullPointerException e) {
            JOptionPane.showMessageDialog(null, "No image selected", "Error", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

    }

    /**
     * @param axes
     * @param name
     */
    @EventListener
    public void onLabelingUpdated(final IntervalWithMetadataChgEvent<LabelingType<L>> e) {
        m_labeling = MiscViews.labelingView(e.getRandomAccessibleInterval(), null);

        m_activeLabels.clear();
        for (final L label : m_labeling.firstElement().getMapping().getLabels()) {
            if (m_ruleFilter.isValid(label)) {
                m_activeLabels.add(label);
            }
        }

        Collections.sort(m_activeLabels);
        m_jLabelList.setListData(m_activeLabels);
    }

    @EventListener
    public void onHiliteChanged(final HilitedLabelsChgEvent e) {
        m_hilitedLabels = new HashSet<String>(e.getHilitedLabels());
        m_jLabelList.setListData(m_activeLabels);

        if (m_showHilitedOnly || m_showUnhilitedOnly) {
            // do filter
            // triggeres
            // redraw
            updateHiliteFilter();
            doFilter();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position getPosition() {
        return Position.EAST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
        eventService.subscribe(this);

    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        m_ruleFilter.writeExternal(out);
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {

        m_textFields.clear();
        m_textFieldsPanel.removeAll();
        m_ruleFilter = new RulebasedLabelFilter<L>();
        m_ruleFilter.readExternal(in);

        for (int s = 0; s < m_ruleFilter.getRules().size(); s++) {
            addTextField(m_ruleFilter.getRules().get(s));
        }
    }

    /*
     * Shows a contextmenu which contains highlighting options
     *
     * @param evt Mouse Event
     */
    private void showMenu(final MouseEvent evt) {

        /*
         * Disables some options if no item is selected because these
         * options need a selected Item
         */
        if (m_hiliteSelected != null) { // if hiliting is enabled
            if (m_jLabelList.isSelectionEmpty()) {
                m_hiliteSelected.setEnabled(false);
                m_unhiliteSelected.setEnabled(false);
            } else {
                m_hiliteSelected.setEnabled(true);
                m_unhiliteSelected.setEnabled(true);
            }
        }

        m_contextMenu.show(m_jLabelList, evt.getX(), evt.getY());
    }

    private JPopupMenu createContextMenu(final boolean enableHilite) {
        final JPopupMenu contextMenu = new JPopupMenu();

        final JMenuItem jumpToLabel = new JMenuItem("Jump to label");
        jumpToLabel.addActionListener(new ActionListener() {

            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(final ActionEvent e) {
                final long[] min = new long[m_labeling.numDimensions()];

                m_labeling.getRasterStart((L)m_jLabelList.getSelectedValue(), min);

                m_eventService.publish(new ForcePlanePosEvent(min));
                m_eventService.publish(new ImgRedrawEvent());
            }
        });

        final JMenuItem filterSelected = new JMenuItem("Filter selected");
        filterSelected.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                final StringBuffer buf = new StringBuffer();
                for (final Object o : m_jLabelList.getSelectedValues()) {
                    buf.append(o.toString() + "|");
                }

                if (buf.length() > 0) {
                    m_ruleFilter.clear();
                    m_textFieldsPanel.removeAll();
                    m_textFields.clear();

                    addTextField(buf.substring(0, buf.length() - 1));
                    doFilter();
                }
            }
        });

        final JMenuItem clearFilters = new JMenuItem("Clear filters");
        clearFilters.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_ruleFilter.clear();
                m_textFieldsPanel.removeAll();
                m_textFields.clear();
                doFilter();
            }
        });

        contextMenu.add(jumpToLabel);
        contextMenu.addSeparator();
        contextMenu.add(filterSelected);
        contextMenu.add(clearFilters);

        if (enableHilite) {
            m_unhiliteSelected = new JMenuItem("Unhilite Selected");
            m_unhiliteSelected.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Set<String> selection = new HashSet<String>();
                    for (final Object o : m_jLabelList.getSelectedValues()) {
                        selection.add(o.toString());
                        m_hilitedLabels.remove(o.toString());
                    }

                    m_eventService.publish(new LabelPanelHiliteSelectionChgEvent(selection, false));

                    if (m_showHilitedOnly || m_showUnhilitedOnly) {
                        // do filter
                        // triggeres
                        // redraw
                        updateHiliteFilter();
                        doFilter();
                    } else {
                        m_eventService.publish(new ImgRedrawEvent());
                    }
                }
            });

            m_hiliteSelected = new JMenuItem("HiLite Selected");
            m_hiliteSelected.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Set<String> selection = new HashSet<String>();
                    for (final Object o : m_jLabelList.getSelectedValues()) {
                        selection.add(o.toString());
                        m_hilitedLabels.add(o.toString());
                    }

                    m_eventService.publish(new LabelPanelHiliteSelectionChgEvent(selection, true));

                    if (m_showHilitedOnly || m_showUnhilitedOnly) {
                        // do filter triggeres redraw
                        updateHiliteFilter();
                        doFilter();
                    } else {
                        m_eventService.publish(new ImgRedrawEvent());
                    }
                }
            });

            final JRadioButtonMenuItem hiliteOnly = new JRadioButtonMenuItem("Show HiLited Only");
            hiliteOnly.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {

                    m_showHilitedOnly = true;
                    m_showUnhilitedOnly = false;
                    updateHiliteFilter();
                    doFilter();
                }
            });

            final JRadioButtonMenuItem unhiliteOnly = new JRadioButtonMenuItem("Show UnHiLited Only");
            unhiliteOnly.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    m_showHilitedOnly = false;
                    m_showUnhilitedOnly = true;
                    updateHiliteFilter();
                    doFilter();
                }
            });

            final JRadioButtonMenuItem showAll = new JRadioButtonMenuItem("Show All");
            showAll.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    m_showHilitedOnly = false;
                    m_showUnhilitedOnly = false;
                    updateHiliteFilter();
                    doFilter();
                }
            });

            final ButtonGroup group = new ButtonGroup();
            group.add(hiliteOnly);
            group.add(unhiliteOnly);
            group.add(showAll);
            showAll.setSelected(true);

            final JMenuItem clearAll = new JMenuItem("Clear Hilite");
            clearAll.addActionListener(new ActionListener() {
                // clears all hilites
                @Override
                public void actionPerformed(final ActionEvent e) {
                    m_eventService.publish(new LabelPanelHiliteSelectionChgEvent(m_hilitedLabels, false));
                    m_hilitedLabels.clear();

                    if (m_hiliteFilter.sizeOfFilterSet() > 0) {
                        m_hiliteFilter.clear();
                        // do filter issues img redraw
                        doFilter();
                    } else {
                        m_eventService.publish(new ImgRedrawEvent());
                    }
                }
            });

            final JCheckBoxMenuItem hiliteMode = new JCheckBoxMenuItem("HiLite mode On");
            if (m_hMode) {
                hiliteMode.setSelected(true);
            }

            hiliteMode.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(final ChangeEvent e) {
                    final boolean old = m_hMode;
                    m_hMode = ((JCheckBoxMenuItem)e.getSource()).isSelected();

                    if (old != m_hMode) {
                        m_eventService.publish(new LabelPanelIsHiliteModeEvent(m_hMode));
                        m_eventService.publish(new ImgRedrawEvent());
                    }
                }
            });

            contextMenu.addSeparator();
            contextMenu.add(hiliteMode);
            contextMenu.addSeparator();

            contextMenu.add(m_hiliteSelected);
            contextMenu.add(m_unhiliteSelected);
            contextMenu.addSeparator();

            contextMenu.add(clearAll);
            contextMenu.addSeparator();

            contextMenu.add(hiliteOnly);
            contextMenu.add(unhiliteOnly);
            contextMenu.add(showAll);
        }

        return contextMenu;
    }

    @EventListener
    public void onClose(final ViewClosedEvent e) {
        m_labeling = null;
    }
}
