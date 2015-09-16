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
package org.knime.knip.base.node.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.Character.Subset;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection2;
import org.knime.knip.base.node.nodesettings.SubsetSelectionUtils;

import net.imagej.axis.TypedAxis;

/**
 * Dialog component to specify a orthogonal image subset.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DialogComponentSubsetSelection2 extends DialogComponent implements ItemListener {

    /* max length of points for a dimension to be selected (shown in the list selection) */
    private static final int MAX_DIM_LENGTH_LIST = 20;

    /*Maximum number of points that can be selected per dimension*/
    private static final int MAX_DIM_LENGTH_GENERAL = 10000;

    // Binary "semaphore" to prevent the ListenerEvent to interfere with our
    // textbox
    private boolean m_allowSelectionListener = true;

    //

    /* all checkboxes */
    private JCheckBox[] m_allChkBoxes;

    /* the labels of the indivudal dimension */
    private final TypedAxis[] m_dimLabels = KNIMEKNIPPlugin.parseDimensionLabelsAsAxis();

    /* the disabled dims */
    private final int[] m_disabledDims;

    /* exclude checkboxes */
    private JCheckBox[] m_exclChkBoxes;

    /* The selections */
    @SuppressWarnings("rawtypes")
    private JList[] m_selections;

    /* flag, whether the all checkboxes should be shown */
    private boolean m_showAllCheckBoxes;

    /* flag, whether the exclude checkboxes should be shown */
    private boolean m_showExcludeCheckBoxes;

    //
    private JTextField[] m_textSelection;

    /**
     * Individual Image Plane selection. See {@link Subset} for a description, how the selection is represented in an
     * int[]-array.
     *
     * @param model
     * @param showAllCheckBoxes if "all"-checkboxes should appear
     * @param showExcludeCheckBoxes if "exclude"-checkboxes should appear
     */
    public DialogComponentSubsetSelection2(final SettingsModelSubsetSelection2 model, final boolean showAllCheckBoxes,
                                          final boolean showExcludeCheckBoxes) {
        this(model, showAllCheckBoxes, showExcludeCheckBoxes, new int[0]);
    }

    /**
     * Individual Image Plane selection. See {@link Subset} for a description, how the selection is represented in an
     * int[]-array.
     *
     * @param model
     * @param showAllCheckBoxes if "all"-checkboxes should appear
     * @param showExcludeCheckBoxes if "exclude"-checkboxes should appear
     * @param disabledDims dimension to be disabled
     */
    public DialogComponentSubsetSelection2(final SettingsModelSubsetSelection2 model, final boolean showAllCheckBoxes,
                                          final boolean showExcludeCheckBoxes, final int[] disabledDims) {
        super(model);
        m_disabledDims = disabledDims.clone();
        init(showAllCheckBoxes, showExcludeCheckBoxes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // Nothing to do here
    }

    /*
     * Creates and adds the components to the component panel.
     */
    private final void init(final boolean showAllCheckBoxes, final boolean showExcludeCheckBoxes) {

        m_showAllCheckBoxes = showAllCheckBoxes;
        m_showExcludeCheckBoxes = showExcludeCheckBoxes;

        updateComponent();

    }

    private final void itemStateChanged() {

        for (int i = 0; i < m_selections.length; i++) {

            try {
                SubsetSelectionUtils.checkInput(m_textSelection[i].getText());
                int[] selectedIndices = SubsetSelectionUtils.parseString(m_textSelection[i].getText());

                m_selections[i].clearSelection();
                m_selections[i].setSelectedIndices(selectedIndices);

            } catch (IllegalArgumentException exc) {
                m_textSelection[i].setBackground(Color.red);
                continue;
            }

            m_textSelection[i].setBackground(Color.white);

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void itemStateChanged(final ItemEvent e) {

        for (int i = 0; i < m_selections.length; i++) {
            m_allChkBoxes[i].setEnabled(!m_exclChkBoxes[i].isSelected());
            m_selections[i].setEnabled(m_exclChkBoxes[i].isSelected());
            m_textSelection[i].setEnabled(m_exclChkBoxes[i].isSelected());

            if (m_allChkBoxes[i].isEnabled()) {
                m_selections[i].setEnabled(!m_allChkBoxes[i].isSelected());
                m_textSelection[i].setEnabled(!m_allChkBoxes[i].isSelected());
            }
        }
    }

    private final void selectionChanged() {
        for (int i = 0; i < m_selections.length; i++) {
            updateTextBox(m_selections[i].getSelectedIndices(), i);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void setEnabledComponents(final boolean enabled) {
        for (int i = 0; i < m_selections.length; i++) {
            if (m_selections[i].isEnabled()) {
                m_selections[i].setEnabled(enabled);
            }
            m_allChkBoxes[i].setEnabled(enabled);
            m_exclChkBoxes[i].setEnabled(enabled);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setToolTipText(final String text) {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected final void updateComponent() {

        // updateModel();
        final SettingsModelSubsetSelection2 model = ((SettingsModelSubsetSelection2)getModel());

        final String[] listData = new String[MAX_DIM_LENGTH_LIST];
        for (int i = 0; i < (listData.length - 1); i++) {
            listData[i] = " " + i + " ";
        }
        listData[listData.length - 1] = "...";
        m_selections = new JList[m_dimLabels.length];
        m_allChkBoxes = new JCheckBox[m_dimLabels.length];
        m_exclChkBoxes = new JCheckBox[m_dimLabels.length];

        m_textSelection = new JTextField[m_dimLabels.length];

        final JLabel[] dimJLabels = new JLabel[m_dimLabels.length];

        getComponentPanel().removeAll();
        getComponentPanel().setLayout(new BoxLayout(getComponentPanel(), BoxLayout.Y_AXIS));

        for (int i = 0; i < dimJLabels.length; i++) {
            // configure the selection fields
            m_selections[i] = new JList(listData);
            m_selections[i].setLayoutOrientation(JList.HORIZONTAL_WRAP);
            m_selections[i].setVisibleRowCount(1);
            m_selections[i].setSelectedIndex(0);

            m_selections[i].addListSelectionListener(new ListSelectionListener() {

                @Override
                public void valueChanged(final ListSelectionEvent e) {
                    if (m_allowSelectionListener) {
                        selectionChanged();
                    }
                }
            });

            // scrollpanes for the coordinate selection and labes

            final JPanel p = new JPanel();
            // p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
            final JScrollPane scroller = new JScrollPane(m_selections[i]);
            scroller.setPreferredSize(new Dimension(250, 35));
            scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

            // textboxex for Coordinate selection
            final JPanel q = new JPanel(new BorderLayout());
            m_textSelection[i] = new JTextField(15);
            m_textSelection[i].setText("0");
            m_textSelection[i].setLayout(new BoxLayout(m_textSelection[i], BoxLayout.X_AXIS));
            m_textSelection[i].addKeyListener(new KeyListener() {

                @Override
                public void keyPressed(final KeyEvent e) {

                }

                @Override
                public void keyReleased(final KeyEvent arg0) {
                    m_allowSelectionListener = false;
                    itemStateChanged();
                    m_allowSelectionListener = true;
                }

                @Override
                public void keyTyped(final KeyEvent arg0) {
                    // TODO Auto-generated method stub
                }
            });
            q.add(scroller, BorderLayout.NORTH);
            q.add(m_textSelection[i], BorderLayout.SOUTH);

            // the dim labels
            dimJLabels[i] = new JLabel(m_dimLabels[i].type().getLabel());
            dimJLabels[i].setPreferredSize(new Dimension(70, (int)dimJLabels[i].getPreferredSize().getHeight()));
            p.add(dimJLabels[i]);
            p.add(q);

            // configure the exclude checkboxes
            m_exclChkBoxes[i] = new JCheckBox("exclude");
            m_exclChkBoxes[i].setSelected(false);
            m_exclChkBoxes[i].addItemListener(this);
            if (m_showExcludeCheckBoxes) {
                p.add(m_exclChkBoxes[i]);
            }

            // configure the all checkboxes
            m_allChkBoxes[i] = new JCheckBox("all");
            if (m_showAllCheckBoxes) {
                m_allChkBoxes[i].setSelected(true);
                m_allChkBoxes[i].addItemListener(this);
                m_selections[i].setEnabled(false);
                m_textSelection[i].setEnabled(false);
            } else {
                m_allChkBoxes[i].setSelected(false);
            }
            if (m_showAllCheckBoxes) {
                p.add(m_allChkBoxes[i]);
            }

            boolean addComponent = true;
            for (int d = 0; d < m_disabledDims.length; d++) {
                if (m_disabledDims[d] == i) {
                    addComponent = false;
                }
            }

            if (addComponent) {
                getComponentPanel().add(p);
            }

        }

        // update the components according to the model
        for (int i = 0; i < m_selections.length; i++) {

            final int[] selection = model.getSelection(m_dimLabels[i].type().getLabel());
            if (selection != null) {
                m_allowSelectionListener = false;
                m_selections[i].setSelectedIndices(selection);
                m_allowSelectionListener = true;
                updateTextBox(selection, i);

            }

            if (m_showAllCheckBoxes) {
                m_allChkBoxes[i].setSelected((selection == null) || (selection.length == 0));
            }

            if (m_showExcludeCheckBoxes) {
                m_exclChkBoxes[i].setSelected(!model.getIncMode(m_dimLabels[i].type().getLabel()));
                if (m_exclChkBoxes[i].isSelected()) {
                    m_allChkBoxes[i].setEnabled(false);
                }
            }
            dimJLabels[i].setText(m_dimLabels[i].type().getLabel());
        }

        // also update the enable status
        // setEnabledComponents(getModel().isEnabled());

    }

    private final void updateModel() throws InvalidSettingsException {

        final SettingsModelSubsetSelection2 model = ((SettingsModelSubsetSelection2)getModel());

        for (int i = 0; i < m_dimLabels.length; i++) {

            final boolean isIncMode = !m_exclChkBoxes[i].isSelected();
            final String dimLabel = m_dimLabels[i].type().getLabel();

            if (m_allChkBoxes[i].isSelected() && !m_exclChkBoxes[i].isSelected()) {
                model.setSelection(dimLabel, null);
            } else {
                model.setSelection(dimLabel, m_textSelection[i].getText());
            }

            model.setIncMode(dimLabel, isIncMode);
        }

    }

    private void updateTextBox(final int[] indices, final int comp) {
        String seperator = "";
        m_textSelection[comp].setText("");
        int lastSet = -2;
        for (int i = 0; i < indices.length; ++i) {
            if (lastSet < (indices[i] - 1)) {
                if (seperator == "-") {
                    m_textSelection[comp].setText(m_textSelection[comp].getText() + seperator + lastSet);
                    seperator = ",";
                }
                m_textSelection[comp].setText(m_textSelection[comp].getText() + seperator + indices[i]);
                seperator = ",";
                lastSet = indices[i];
            } else {
                seperator = "-";
                lastSet = indices[i];
            }
            if ((i == (indices.length - 1)) && (seperator == "-")) {
                m_textSelection[comp].setText(m_textSelection[comp].getText() + seperator + indices[i]);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void validateSettingsBeforeSave() throws InvalidSettingsException {
        for (int i = 0; i < m_selections.length; i++) {

            String text = m_textSelection[i].getText().replaceAll("\\s", "");

            if (text.length() == 0) {
                m_selections[i].clearSelection();
                m_selections[i].setBackground(Color.RED);
                throw new InvalidSettingsException("Zero selection not allowed");
            }

            try {
                SubsetSelectionUtils.checkInput(text);
            } catch (IllegalArgumentException exc) {
                m_textSelection[i].setBackground(Color.RED);
                throw new InvalidSettingsException(exc.getMessage());
            }

            m_selections[i].setBackground(Color.white);
        }
        updateModel();
    }

}
