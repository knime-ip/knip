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
package org.knime.knip.base.nodes.proc.binary.morphops;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.type.logic.BitType;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.nodes.proc.binary.morphops.MorphImgOpsNodeModel.MorphOp;
import org.knime.knip.core.types.OutOfBoundsStrategyEnum;
import org.knime.knip.core.util.EnumListProvider;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class MorphImgOpsNodeDialog extends ValueToCellNodeDialog<ImgPlusValue<BitType>> {

    private SettingsModelString m_struct;

    private boolean m_structProvided;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void addDialogComponents() {

        final SettingsModelString type = MorphImgOpsNodeModel.createConnectionTypeModel();
        addDialogComponent("Options", "Structuring Element", new DialogComponentStringSelection(type,
                "Connection Type", MorphImgOpsNodeModel.ConnectedType.NAMES));
        m_struct = MorphImgOpsNodeModel.createColStructureModel();

        addDialogComponent("Options", "Structuring Element", new DialogComponentColumnNameSelection(m_struct, "Column",
                1, false, true, ImgPlusValue.class));

        addDialogComponent("Options", "Operation",
                           new DialogComponentStringSelection(MorphImgOpsNodeModel.createOperationModel(), "Method",
                                   MorphOp.NAMES));

        // TODO: Removed as not compatible with all nodes
        // addDialogComponent(
        // "Options",
        // "Operation",
        // new DialogComponentNumberEdit(
        // MorphImgOpsNodeModel
        // .createNeighborhoodCountModel(),
        // "Neighborhood Count"));

        type.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (MorphImgOpsNodeModel.ConnectedType.value(type.getStringValue()) != MorphImgOpsNodeModel.ConnectedType.STRUCTURING_ELEMENT) {
                    m_struct.setStringValue(null);
                } else if (!m_structProvided) {
                    type.setStringValue(MorphImgOpsNodeModel.ConnectedType.FOUR_CONNECTED.toString());
                    NodeLogger.getLogger(this.getClass())
                            .warn("No strucutring element in inport 2 provided. Four-Connected is chosen by default.");
                }
            }
        });

        addDialogComponent("Options", "Operation",
                           new DialogComponentNumber(MorphImgOpsNodeModel.createIterationsModel(),
                                   "Number of iterations", 1));

        addDialogComponent("Options", "Dimension selection",
                           new DialogComponentDimSelection(MorphImgOpsNodeModel.createDimSelectionModel(), "", 2,
                                   Integer.MAX_VALUE));

        addDialogComponent("Options",
                           "Out of Bounds Strategy",
                           new DialogComponentStringSelection(MorphImgOpsNodeModel.createOutOfBoundsModel(),
                                   "Out of Bounds Strategy", EnumListProvider.getStringList(OutOfBoundsStrategyEnum
                                           .values())));

    }

    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
            throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);

        m_structProvided = specs[1].getNumColumns() != 0;
        m_struct.setEnabled(m_structProvided);
    }
}
