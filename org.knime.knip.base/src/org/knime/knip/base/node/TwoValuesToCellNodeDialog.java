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
package org.knime.knip.base.node;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.node.v210.FullDescriptionDocument.FullDescription;
import org.knime.node.v210.InPortDocument.InPort;
import org.knime.node.v210.KnimeNodeDocument.KnimeNode;
import org.knime.node.v210.OptionDocument.Option;
import org.knime.node.v210.OutPortDocument.OutPort;
import org.knime.node.v210.PortsDocument.Ports;
import org.knime.node.v210.TabDocument.Tab;

/**
 * TODO: Standard description
 *
 * Dialog corresponding to the {@link TwoValuesToCellNodeModel} which already contains dialog components, but others can
 * still be added (this {@link #addDialogComponent(String, String, DialogComponent)}.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <VIN1>
 * @param <VIN2>
 */
public abstract class TwoValuesToCellNodeDialog<VIN1 extends DataValue, VIN2 extends DataValue> extends
        LazyNodeDialogPane {

    private SettingsModelString m_firstColumnSettingsModel;

    private SettingsModelString m_secondColumnSettingsModel;

    private SettingsModelString m_smColCreationMode;

    private SettingsModelString m_smColumnSuffix;

    /**
     * Adds the description of the column selection tab to the node description.
     *
     * @param desc
     */
    static void addTabsDescriptionTo(final FullDescription desc) {
        final Tab tab = desc.addNewTab();
        tab.setName("Column Selection");
        Option opt = tab.addNewOption();
        opt.setName("Column Creation Mode");
        opt.addNewP()
                .newCursor()
                .setTextValue("Mode how to handle the selected column. The processed column can be added to a new table, appended to the end of the table, or the old columns can be replaced by the new result");
        opt = tab.addNewOption();
        opt.setName("Column Suffix");
        opt.newCursor()
                .setTextValue("A suffix appended to the column name. If \"Append\" is not selected, it can be left empty.");
        opt = tab.addNewOption();
        opt.setName("Column Selection");
        opt.newCursor().setTextValue("Selection of the columns to be processed.");
    }

    /**
     * Adds the port description to the node description.
     *
     * @param node
     */
    static void addPortsDescriptionTo(final KnimeNode node) {
        final Ports ports = node.addNewPorts();
        final InPort inPort = ports.addNewInPort();
        inPort.newCursor().setTextValue("Images");
        inPort.setName("Images");
        inPort.setIndex(new BigInteger("0"));
        inPort.newCursor().setTextValue("Images");
        final OutPort outPort = ports.addNewOutPort();
        outPort.setName("Processed Images");
        outPort.setIndex(new BigInteger("0"));
        outPort.newCursor().setTextValue("Processed Images");
    }

    /**
     * Default Constructor
     */
    public TwoValuesToCellNodeDialog() {
        this(false);
    }

    /**
     * Possible lazy constructor
     *
     * If lazy is true addDialogComponents(); buildDialog();
     *
     * Must be called manually!
     *
     * @param lazy indicator whether lazy or not
     */
    public TwoValuesToCellNodeDialog(final boolean lazy) {
        addDCs();
        if (!lazy) {
            addDialogComponents();
            buildDialog();
        }
    }

    /*
     * Helper add the dialog components needed for the this dialog and the
     * ValueToCellNodeModel, respectively.
     */
    @SuppressWarnings("unchecked")
    private void addDCs() {
        m_smColCreationMode = TwoValuesToCellNodeModel.createColCreationModeModel();

        addDialogComponent("Column Selection", "Creation Mode", new DialogComponentStringSelection(m_smColCreationMode,
                "Column Creation Mode", TwoValuesToCellNodeModel.COL_CREATION_MODES));

        m_smColumnSuffix = TwoValuesToCellNodeModel.createColSuffixModel();

        addDialogComponent("Column Selection", "Column suffix", new DialogComponentString(m_smColumnSuffix,
                "Column suffix"));

        final Class<? extends DataValue>[] argTypeClasses = getTypeArgumentClasses();

        m_firstColumnSettingsModel = TwoValuesToCellNodeModel.createFirstColModel();

        addDialogComponent("Column Selection", "Column Selection", new DialogComponentColumnNameSelection(
                m_firstColumnSettingsModel, getFirstColumnSelectionLabel(), 0, isFirstColumnRequired(),
                !isFirstColumnRequired(), argTypeClasses[0]));

        m_secondColumnSettingsModel = TwoValuesToCellNodeModel.createSecondColModel();

        addDialogComponent("Column Selection", "Column Selection", new DialogComponentColumnNameSelection(
                m_secondColumnSettingsModel, getSecondColumnSelectionLabel(), 0, isSecondColumnRequired(),
                !isSecondColumnRequired(), argTypeClasses[1]));

        //add append suffix logic
        if (!getDefaultSuffixForAppend().isEmpty()) {
            m_smColCreationMode.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent e) {
                    if (m_smColCreationMode.getStringValue().equals(TwoValuesToCellNodeModel.COL_CREATION_MODES[1])) {
                        //append
                        if (m_smColumnSuffix.getStringValue().isEmpty()) {
                            m_smColumnSuffix.setStringValue(getDefaultSuffixForAppend());
                        }
                    } else {
                        if (m_smColumnSuffix.getStringValue().equals(getDefaultSuffixForAppend())) {
                            m_smColumnSuffix.setStringValue("");
                        }
                    }
                }
            });
        }

    }

    /**
     * @return a default suffix that is provided if the user chooses the append option
     */
    protected String getDefaultSuffixForAppend() {
        return "";
    }

    /**
     * Add the dialog components to the dialog here. Do NOT add them in the contructor!
     */
    public abstract void addDialogComponents();

    /**
     * @return the label of the first column selection
     */
    protected String getFirstColumnSelectionLabel() {
        return "First column";
    }

    /**
     * @return settings model of first column
     */
    protected SettingsModelString getFirstColumnSettingsModel() {
        return m_firstColumnSettingsModel;
    }

    /**
     * @return the label of the second column selection
     */
    protected String getSecondColumnSelectionLabel() {
        return "Second column";
    }

    /**
     * @return settings model of second column
     */
    protected SettingsModelString getSecondColumnSettingsModel() {
        return m_secondColumnSettingsModel;
    }

    /*
     * Retrieves the classes of the type arguments VIN1, VIN2 and COUT.
     */
    @SuppressWarnings("unchecked")
    private Class<? extends DataValue>[] getTypeArgumentClasses() {

        Class<?> c = getClass();
        final Class<? extends DataValue>[] res = new Class[2];
        for (int i = 0; i < 5; i++) {
            if (c.getSuperclass().equals(TwoValuesToCellNodeDialog.class)) {
                final Type[] types = ((ParameterizedType)c.getGenericSuperclass()).getActualTypeArguments();
                if (types[0] instanceof ParameterizedType) {
                    types[0] = ((ParameterizedType)types[0]).getRawType();
                }
                if (types[1] instanceof ParameterizedType) {
                    types[1] = ((ParameterizedType)types[1]).getRawType();
                }

                res[0] = (Class<VIN1>)types[0];
                res[1] = (Class<VIN2>)types[1];
                break;
            }
            c = c.getSuperclass();
        }
        return res;
    }

    /**
     * Indicates whether the first column is required to be selected or not
     *
     * @return true, if first column is required
     */
    public boolean isFirstColumnRequired() {
        return true;
    }

    /**
     * Indicates whether the second column is required to be selected or not
     *
     * @return true, if second column is required
     */
    public boolean isSecondColumnRequired() {
        return true;
    }

    /**
     * If column creation mode is 'append', a suffix needs to be chosen!
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (m_smColCreationMode.getStringValue().equals(TwoValuesToCellNodeModel.COL_CREATION_MODES[1])
                && m_smColumnSuffix.getStringValue().trim().isEmpty()) {
            throw new InvalidSettingsException(
                    "If the selected column creation mode is 'append', a column suffix for the resulting column name must to be chosen!");
        }

        super.saveAdditionalSettingsTo(settings);
    }

}
