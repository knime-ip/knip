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
package org.knime.knip.base.nodes.proc.imgjep;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.util.EnumListProvider;

/**
 * Dialog for Mathematical Formula node. Shows expression text field, and three list containing variables, functions,
 * and constants.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ImgJEPNodeDialogPane extends NodeDialogPane {

    private static class ConstantListRenderer extends DefaultListCellRenderer {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        /** {@inheritDoc} */
        @Override
        public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                      final boolean isSelected, final boolean cellHasFocus) {
            Object v;
            if (value instanceof ImgJEPConstant) {
                final ImgJEPConstant j = (ImgJEPConstant)value;
                v = j.getFunctionFullName();
                setToolTipText(j.getDescription());
            } else {
                v = null;
                setToolTipText(null);
            }
            return super.getListCellRendererComponent(list, v, index, isSelected, cellHasFocus);
        }
    }

    private static class FunctionListRenderer extends DefaultListCellRenderer {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        /** {@inheritDoc} */
        @Override
        public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                      final boolean isSelected, final boolean cellHasFocus) {
            Object v;
            if (value instanceof ImgJEPFunction) {
                final ImgJEPFunction j = (ImgJEPFunction)value;
                v = j.getFunctionFullName();
                setToolTipText(j.getDescription());
            } else {
                v = null;
                setToolTipText(null);
            }
            return super.getListCellRendererComponent(list, v, index, isSelected, cellHasFocus);
        }
    }

    /**
     * Renderer that will display the rowindex and rowkey with different background.
     */
    private static class ListRenderer extends DataColumnSpecListCellRenderer {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        /** {@inheritDoc} */
        @Override
        public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                      final boolean isSelected, final boolean cellHasFocus) {
            final Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof String) {
                c.setFont(list.getFont().deriveFont(Font.ITALIC));
            }
            return c;
        }
    }

    private final DialogComponent m_adjustImgDim;

    private final JRadioButton m_appendRadio;

    private final JList m_constantList;

    private DataTableSpec m_currenteSpec = null;

    private final JEditorPane m_expEdit;

    private final JList m_functionList;

    private final JTextField m_newColNameField;

    private final JTextField m_newTableColNameField;

    private final JRadioButton m_newTableRadio;

    private final DialogComponentColumnNameSelection m_refColumnSelection;

    private final ColumnSelectionPanel m_replaceCombo;

    private final JRadioButton m_replaceRadio;

    private final DialogComponent m_resType;

    private final JList m_varList;

    /** Inits GUI. */
    @SuppressWarnings("unchecked")
    public ImgJEPNodeDialogPane() {
        m_varList = new JList(new DefaultListModel());
        m_varList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_varList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                final Object selected = m_varList.getSelectedValue();
                if (selected != null) {
                    final DataColumnSpec colSpec = (DataColumnSpec)selected;
                    final String enter = "$" + colSpec.getName().toString() + "$";
                    m_expEdit.replaceSelection(enter);
                    m_varList.clearSelection();
                    m_expEdit.requestFocus();
                }
            }
        });
        m_varList.setCellRenderer(new ListRenderer());
        m_functionList = new JList(ImgJEPFunction.values());
        m_functionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_functionList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                final ImgJEPFunction selected = (ImgJEPFunction)m_functionList.getSelectedValue();
                if (selected != null) {
                    final String selectedString = m_expEdit.getSelectedText();
                    final StringBuilder newStr = new StringBuilder(selected.getFunctionName());
                    if (!selected.isInfixOperation()) {
                        newStr.append('(');
                        for (int i = 0; i < selected.getNrArgs(); i++) {
                            newStr.append(i > 0 ? ", " : "");
                            if ((i == 0) && (selectedString != null)) {
                                newStr.append(selectedString);
                            }
                        }
                        newStr.append(')');
                    }
                    m_expEdit.replaceSelection(newStr.toString());
                    if ((selected.getNrArgs() > 0) && (selectedString == null)) {
                        final int caretPos = m_expEdit.getCaretPosition();
                        m_expEdit.setCaretPosition(1 + m_expEdit.getText()
                                .indexOf('(', caretPos - newStr.toString().length()));
                    }
                    m_functionList.clearSelection();
                    m_expEdit.requestFocus();
                }
            }
        });
        m_functionList.setCellRenderer(new FunctionListRenderer());
        m_constantList = new JList(ImgJEPConstant.values());
        m_constantList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                final ImgJEPConstant selected = (ImgJEPConstant)m_constantList.getSelectedValue();
                if (selected != null) {
                    final boolean needsArgument = selected.getNrArgs() > 0;
                    final String selectedString = m_expEdit.getSelectedText();
                    final StringBuilder newStr = new StringBuilder();
                    newStr.append(selected.getFunctionName());
                    newStr.append(needsArgument ? "(" : "");
                    for (int i = 0; i < selected.getNrArgs(); i++) {
                        newStr.append(i > 0 ? ", " : "");
                        if ((i == 0) && (selectedString != null)) {
                            newStr.append(selectedString);
                        }
                    }
                    newStr.append(needsArgument ? ")" : "");
                    m_expEdit.replaceSelection(newStr.toString());
                    if (needsArgument && (selectedString == null)) {
                        final int caretPos = m_expEdit.getCaretPosition();
                        final int newCaret = m_expEdit.getText().indexOf(')', caretPos - newStr.toString().length());
                        if (newCaret > 0) {
                            m_expEdit.setCaretPosition(newCaret);
                        }
                    }
                    m_constantList.clearSelection();
                    m_expEdit.requestFocus();
                }
            }
        });
        m_constantList.setCellRenderer(new ConstantListRenderer());
        m_expEdit = new JEditorPane();
        m_expEdit.setFont(Font.getFont("Monospaced"));
        m_newColNameField = new JTextField(10);
        m_newTableColNameField = new JTextField(10);
        m_appendRadio = new JRadioButton("Append Column ");
        m_appendRadio.setToolTipText("Appends a new column to the input " + "table with a given name and type.");
        m_replaceRadio = new JRadioButton("Replace Column: ");
        m_replaceRadio.setToolTipText("Replaces the column and changes " + "the column type accordingly");
        m_newTableRadio = new JRadioButton("New Table: ");
        m_newTableRadio.setToolTipText("Creates a new table " + "only keeping the result column");
        // show all columns
        m_replaceCombo = new ColumnSelectionPanel((Border)null, DataValue.class);
        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(m_appendRadio);
        buttonGroup.add(m_replaceRadio);
        buttonGroup.add(m_newTableRadio);
        final ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_replaceCombo.setEnabled(m_replaceRadio.isSelected());
                m_newColNameField.setEnabled(m_appendRadio.isSelected());
                m_newTableColNameField.setEnabled(m_newTableRadio.isSelected());
            }
        };
        m_appendRadio.addActionListener(actionListener);
        m_replaceRadio.addActionListener(actionListener);
        m_newTableRadio.addActionListener(actionListener);

        final JPanel finalPanel = new JPanel(new BorderLayout());
        finalPanel.add(createPanel(), BorderLayout.CENTER);

        final JPanel additionalOptions = new JPanel();
        additionalOptions.setLayout(new BoxLayout(additionalOptions, BoxLayout.Y_AXIS));
        finalPanel.add(additionalOptions, BorderLayout.SOUTH);

        // result pixel type panel
        m_resType =
                new DialogComponentStringSelection(new SettingsModelString(ImgJEPNodeModel.CFG_RESULT_TYPE,
                        NativeTypes.FLOATTYPE.toString()), "Result pixel type",
                        EnumListProvider.getStringList(NativeTypes.values()));
        additionalOptions.add(m_resType.getComponentPanel(), BorderLayout.SOUTH);

        addTab("Math Expression", finalPanel);

        // reference image selection

        final JPanel settingsPanel = new JPanel(new BorderLayout());

        final JPanel referenceSettigns = new JPanel(new BorderLayout());

        m_refColumnSelection =
                new DialogComponentColumnNameSelection(new SettingsModelString(ImgJEPNodeModel.CFG_REF_COLUMN, ""),
                        "Reference Image", 0, false, true, ImgPlusValue.class);

        final JLabel description = new JLabel("(Auto guess of reference if <none> is selected.)");

        referenceSettigns.add(description, BorderLayout.NORTH);

        referenceSettigns.add(m_refColumnSelection.getComponentPanel(), BorderLayout.CENTER);

        settingsPanel.add(referenceSettigns, BorderLayout.NORTH);

        // option to extend the image

        m_adjustImgDim =
                new DialogComponentBoolean(new SettingsModelBoolean(ImgJEPNodeModel.CFG_ADJUST_IMG_DIM, false),
                        "Adjust image dimensions if not compatible");
        settingsPanel.add(m_adjustImgDim.getComponentPanel(), BorderLayout.CENTER);

        addTab("Advanced Settings", settingsPanel);

    }

    private JPanel createPanel() {
        final JPanel finalPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gdb = new GridBagConstraints();
        gdb.insets = new Insets(15, 15, 0, 15);
        gdb.anchor = GridBagConstraints.SOUTH;
        gdb.weighty = 0.0;
        gdb.weightx = 0.0;
        gdb.gridx = 0;
        gdb.gridy = 0;
        finalPanel.add(new JLabel("Column List"), gdb);
        gdb.gridx = 1;
        finalPanel.add(new JLabel("Mathematical Function"), gdb);
        gdb.gridx = 2;
        finalPanel.add(new JLabel("Constants"), gdb);

        gdb.gridx = 0;
        gdb.gridy++;
        gdb.insets.bottom = 15;
        gdb.insets.top = 0;
        gdb.weighty = 2.0;
        gdb.weightx = 1.0;
        gdb.anchor = GridBagConstraints.CENTER;
        gdb.fill = GridBagConstraints.BOTH;
        finalPanel.add(new JScrollPane(m_varList), gdb);
        gdb.gridx = 1;
        finalPanel.add(new JScrollPane(m_functionList), gdb);
        gdb.gridx = 2;
        finalPanel.add(new JScrollPane(m_constantList), gdb);

        gdb.insets.top = 10;
        gdb.insets.bottom = 0;
        gdb.gridwidth = 3;
        gdb.gridy++;
        gdb.gridx = 0;
        gdb.weighty = 0.0;
        gdb.anchor = GridBagConstraints.SOUTHWEST;
        finalPanel.add(new JLabel("Expression"), gdb);
        gdb.weighty = 1.0;
        gdb.insets.top = 0;
        gdb.insets.bottom = 15;
        gdb.gridy++;
        finalPanel.add(new JScrollPane(m_expEdit, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), gdb);

        gdb.insets.top = 15;
        gdb.insets.bottom = 15;
        gdb.gridy++;
        gdb.gridx = 0;
        gdb.weighty = 0.0;
        gdb.weightx = 0.0;
        gdb.gridwidth = 2;
        gdb.ipadx = 5;
        gdb.fill = GridBagConstraints.NONE;
        final JPanel replaceOrAppend = new JPanel(new GridBagLayout());
        final GridBagConstraints gdb2 = new GridBagConstraints();
        gdb2.insets = new Insets(5, 5, 5, 5);
        gdb2.fill = GridBagConstraints.HORIZONTAL;
        gdb2.anchor = GridBagConstraints.WEST;
        gdb2.gridx = 0;
        gdb2.gridy = 0;
        replaceOrAppend.add(m_appendRadio, gdb2);
        gdb2.gridx++;
        replaceOrAppend.add(m_newColNameField, gdb2);
        gdb2.gridy++;
        gdb2.gridx = 0;
        replaceOrAppend.add(m_replaceRadio, gdb2);
        gdb2.gridx++;
        replaceOrAppend.add(m_replaceCombo, gdb2);
        gdb2.gridy++;
        gdb2.gridx = 0;
        replaceOrAppend.add(m_newTableRadio, gdb2);
        gdb2.gridx++;
        replaceOrAppend.add(m_newTableColNameField, gdb2);

        finalPanel.add(replaceOrAppend, gdb);

        return finalPanel;
    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
            throws NotConfigurableException {
        final String exp = settings.getString(ImgJEPNodeModel.CFG_EXPRESSION, "");
        final String defaultColName = "new column";
        final String newColName = settings.getString(ImgJEPNodeModel.CFG_COLUMN_NAME, defaultColName);
        final int tableCreationMode =
                settings.getInt(ImgJEPNodeModel.CFG_TABLE_CREATION_MODE, ImgJEPNodeModel.REPLACE_COLUMN);
        m_newColNameField.setText("");
        m_newTableColNameField.setText("");
        // will select newColName only if it is in the spec list
        m_replaceCombo.update(specs[0], newColName);
        m_currenteSpec = specs[0];
        if (tableCreationMode == ImgJEPNodeModel.REPLACE_COLUMN) {
            m_replaceRadio.doClick();
        } else if (tableCreationMode == ImgJEPNodeModel.APPEND_COLUMN) {
            m_appendRadio.doClick();
            final String newColString = (newColName != null ? newColName : defaultColName);
            m_newColNameField.setText(newColString);
        } else {
            m_newTableRadio.doClick();
            final String newColString = (newColName != null ? newColName : defaultColName);
            m_newTableColNameField.setText(newColString);
        }
        m_expEdit.setText(exp);
        final DefaultListModel listModel = (DefaultListModel)m_varList.getModel();
        listModel.removeAllElements();
        for (int i = 0; i < specs[0].getNumColumns(); i++) {
            final DataColumnSpec colSpec = specs[0].getColumnSpec(i);
            if (colSpec.getType().isCompatible(ImgPlusValue.class) || colSpec.getType().isCompatible(DoubleValue.class)
                    || colSpec.getType().isCompatible(IntValue.class)) {
                listModel.addElement(colSpec);
            }

        }

        m_resType.loadSettingsFrom(settings, specs);
        m_adjustImgDim.loadSettingsFrom(settings, specs);
        m_refColumnSelection.loadSettingsFrom(settings, specs);

    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        String newColName = null;
        int tableCreationMode;
        if (m_replaceRadio.isSelected()) {
            tableCreationMode = ImgJEPNodeModel.REPLACE_COLUMN;
            newColName = m_replaceCombo.getSelectedColumn();
        } else if (m_appendRadio.isSelected()) {
            tableCreationMode = ImgJEPNodeModel.APPEND_COLUMN;
            newColName = m_newColNameField.getText();
        } else {
            tableCreationMode = ImgJEPNodeModel.NEW_TABLE;
            newColName = m_newTableColNameField.getText();
        }

        settings.addInt(ImgJEPNodeModel.CFG_TABLE_CREATION_MODE, tableCreationMode);
        settings.addString(ImgJEPNodeModel.CFG_COLUMN_NAME, newColName);
        final String exp = m_expEdit.getText();
        settings.addString(ImgJEPNodeModel.CFG_EXPRESSION, exp);
        if (m_currenteSpec != null) {
            new ImgExpressionParser(exp, m_currenteSpec, -1);
        }
        m_resType.saveSettingsTo(settings);
        m_adjustImgDim.saveSettingsTo(settings);
        m_refColumnSelection.saveSettingsTo(settings);
    }

}
