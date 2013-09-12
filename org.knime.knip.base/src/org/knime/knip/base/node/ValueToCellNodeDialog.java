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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.node2012.FullDescriptionDocument.FullDescription;
import org.knime.node2012.InPortDocument.InPort;
import org.knime.node2012.KnimeNodeDocument.KnimeNode;
import org.knime.node2012.OptionDocument.Option;
import org.knime.node2012.OutPortDocument.OutPort;
import org.knime.node2012.PortsDocument.Ports;
import org.knime.node2012.TabDocument.Tab;

/**
 * Dialog corresponding to the {@link ValueToCellNodeModel} which already contains dialog components, but others can
 * still be added (this {@link #addDialogComponent(String, String, DialogComponent)}.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class ValueToCellNodeDialog<VIN extends DataValue> extends LazyNodeDialogPane {

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
        inPort.setIndex(0);
        inPort.newCursor().setTextValue("Images");
        final OutPort outPort = ports.addNewOutPort();
        outPort.setName("Processed Images");
        outPort.setIndex(0);
        outPort.newCursor().setTextValue("Processed Images");
    }

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
                .setTextValue("Mode how to handle the selected column. The processed column can be added to a new table, appended to the end of the table, or the old column can be replaced by the new result");
        opt = tab.addNewOption();
        opt.setName("Column suffix");
        opt.newCursor()
                .setTextValue("A suffix appended to the column name. If \"Append\" is not selected, it can be left empty.");
        opt = tab.addNewOption();
        opt.setName("Column Selection");
        opt.newCursor().setTextValue("Selection of the columns to be processed.");
    }

    private SettingsModelString m_smColCreationMode;

    private SettingsModelString m_smColumnSuffix;

    public ValueToCellNodeDialog() {

        addDCs();
        addDialogComponents();
        buildDialog();

    }

    /*
     * Helper add the dialog components needed for the this dialog and the
     * ValueToCellNodeModel, respectively.
     */
    @SuppressWarnings("unchecked")
    private void addDCs() {
        m_smColCreationMode = ValueToCellNodeModel.createColCreationModeModel();
        addDialogComponent("Column Selection", "Creation Mode", new DialogComponentStringSelection(m_smColCreationMode,
                "Column Creation Mode", ValueToCellNodeModel.COL_CREATION_MODES));

        m_smColumnSuffix = ValueToCellNodeModel.createColSuffixNodeModel();

        addDialogComponent("Column Selection", "Column suffix", new DialogComponentString(m_smColumnSuffix,
                "Column suffix"));

        addDialogComponent("Column Selection", "",
                           new DialogComponentColumnFilter(ValueToCellNodeModel.createColumnSelectionModel(), 0, true,
                                   getTypeArgumentClass()));

        //add append suffix logic
        if (!getDefaultSuffixForAppend().isEmpty()) {
            m_smColCreationMode.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent e) {
                    if (m_smColCreationMode.getStringValue().equals(ValueToCellNodeModel.COL_CREATION_MODES[1])) {
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

    /*
     * Retrieves the classes of the type arguments VIN and COUT.
     */
    @SuppressWarnings("unchecked")
    private Class<VIN> getTypeArgumentClass() {

        // TODO: is there a better way??

        Class<VIN> res = null;
        Class<?> c = getClass();
        for (int i = 0; i < 5; i++) {
            if (c.getSuperclass().equals(ValueToCellNodeDialog.class)) {
                final Type[] types = ((ParameterizedType)c.getGenericSuperclass()).getActualTypeArguments();
                if (types[0] instanceof ParameterizedType) {
                    types[0] = ((ParameterizedType)types[0]).getRawType();
                }

                res = (Class<VIN>)types[0];
                break;
            }
            c = c.getSuperclass();
        }

        return res;
    }

    /**
     * If column creation mode is 'append', a suffix needs to be chosen!
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (m_smColCreationMode.getStringValue().equals(ValueToCellNodeModel.COL_CREATION_MODES[1])
                && m_smColumnSuffix.getStringValue().trim().isEmpty()) {
            throw new InvalidSettingsException(
                    "If the selected column creation mode is 'append', a column suffix for the resulting column name must to be chosen!");
        }

        super.saveAdditionalSettingsTo(settings);
    }

}
