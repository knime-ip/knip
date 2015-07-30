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
package org.knime.knip.base.nodes.seg;

import java.util.List;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.labeling.unary.ExtractLabelingOutline;
import net.imglib2.ops.types.ConnectedType;
import net.imglib2.roi.labeling.LabelingType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.util.EnumUtils;

/**
 * Factory class to produce a Connected Component Analysis Node.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author schoenen
 */
public class ExtractOutlineLabelingNodeFactory<L extends Comparable<L>> extends
        ValueToCellNodeFactory<LabelingValue<L>> {

    private static SettingsModelString createConnectionTypeModel() {
        return new SettingsModelString("connection-type", ConnectedType.values()[0].toString());
    }

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dimensions", "X", "Y");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<LabelingValue<L>> createNodeDialog() {
        return new ValueToCellNodeDialog<LabelingValue<L>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Settings",
                                   new DialogComponentStringSelection(createConnectionTypeModel(), "Connection Type",
                                           EnumUtils.getStringListFromToString(ConnectedType.values())));

                addDialogComponent("Options", "Dimensions", new DialogComponentDimSelection(createDimSelectionModel(),
                        "Dimensions", 2, 2));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getDefaultSuffixForAppend() {
                return "_outlines";
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<LabelingValue<L>, LabelingCell<L>> createNodeModel() {
        return new ValueToCellNodeModel<LabelingValue<L>, LabelingCell<L>>() {

            private final SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

            private LabelingCellFactory m_labCellFactory;

            private final SettingsModelString m_type = createConnectionTypeModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_dimSelection);
                settingsModels.add(m_type);

            }

            @Override
            protected LabelingCell<L> compute(final LabelingValue<L> cellValue) throws Exception {
                final RandomAccessibleInterval<LabelingType<L>> lab = cellValue.getLabeling();
                final ExtractLabelingOutline<L> op =
                        new ExtractLabelingOutline<L>(EnumUtils.valueForName(m_type.getStringValue(),
                                                                             ConnectedType.values()));

                final RandomAccessibleInterval<LabelingType<L>> imgLabeling =
                        KNIPGateway.ops().create().imgLabeling(lab);

                final RandomAccessibleInterval<LabelingType<L>> out =
                        SubsetOperations.iterate(op,
                                                 m_dimSelection.getSelectedDimIndices(cellValue.getLabelingMetadata()),
                                                 cellValue.getLabeling(), imgLabeling, getExecutorService());

                return m_labCellFactory.createCell(out, cellValue.getLabelingMetadata());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_labCellFactory = new LabelingCellFactory(exec);
            }
        };
    }
}
