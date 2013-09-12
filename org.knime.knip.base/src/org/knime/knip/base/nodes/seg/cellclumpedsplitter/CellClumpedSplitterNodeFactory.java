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
package org.knime.knip.base.nodes.seg.cellclumpedsplitter;

import java.util.List;
import java.util.concurrent.ExecutorService;

import net.imglib2.labeling.Labeling;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.LocalMaximaForDistanceMap.NeighborhoodType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.ops.labeling.CellClumpedSplitter;
import org.knime.knip.core.util.EnumListProvider;

/**
 * Cell Clump Splitter.
 * 
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class CellClumpedSplitterNodeFactory<T extends RealType<T>, L extends Comparable<L>> extends
        ValueToCellNodeFactory<LabelingValue<L>> {

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    private static SettingsModelInteger createMaxIterationsModel() {
        return new SettingsModelInteger("max_interations", 100);
    }

    private static SettingsModelDouble createMinimaMaximaSizeModel() {
        return new SettingsModelDouble("mininma_maxima_size", 0);
    }

    private static SettingsModelString createNeighborhoodModel() {
        return new SettingsModelString("neighborhood", NeighborhoodType.EIGHT.toString());
    }

    private SettingsModelDouble createIgnoreValueBelowAvgPercent() {
        return new SettingsModelDouble("ignore_value_below_avg_precent", 0.0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<LabelingValue<L>> createNodeDialog() {
        return new ValueToCellNodeDialog<LabelingValue<L>>() {

            @Override
            public void addDialogComponents() {

                addDialogComponent("Options", "Filter Options", new DialogComponentNumber(
                        createMinimaMaximaSizeModel(), "Max value of a local maxima:", 1));

                addDialogComponent("Options", "Filter Options", new DialogComponentNumber(
                        createIgnoreValueBelowAvgPercent(), "Ignore percentage:", 0.1));

                addDialogComponent("Options", "Splitter", new DialogComponentStringSelection(

                createNeighborhoodModel(), "Neighboorhood", EnumListProvider.getStringList(NeighborhoodType.values())));

                addDialogComponent("Options", "Splitter", new DialogComponentNumber(createMaxIterationsModel(),
                        "Max iterations:", 10));

                addDialogComponent("Options", "Dimensions", new DialogComponentDimSelection(createDimSelectionModel(),
                        "Dimensions", 2, 5));

            }
        };
    }

    @Override
    public ValueToCellNodeModel<LabelingValue<L>, LabelingCell<Integer>> createNodeModel() {
        return new ValueToCellNodeModel<LabelingValue<L>, LabelingCell<Integer>>() {

            private ExecutorService m_executor;;

            private LabelingCellFactory m_labCellFactory;

            private final SettingsModelDimSelection m_smDimSelection = createDimSelectionModel();

            private final SettingsModelDouble m_smIgnoreValueBelowAvgPrecent = createIgnoreValueBelowAvgPercent();

            private final SettingsModelInteger m_smMaxInterations = createMaxIterationsModel();

            private final SettingsModelDouble m_smMinMaximaSize = createMinimaMaximaSizeModel();

            private final SettingsModelString m_smNeighborhood = createNeighborhoodModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_smMaxInterations);
                settingsModels.add(m_smMinMaximaSize);
                settingsModels.add(m_smIgnoreValueBelowAvgPrecent);
                settingsModels.add(m_smDimSelection);
                settingsModels.add(m_smNeighborhood);

            }

            @Override
            protected LabelingCell<Integer> compute(final LabelingValue<L> cellLabelingVal) throws Exception {

                final Labeling<L> labeling = cellLabelingVal.getLabeling();

                m_executor = getExecutorService();
                final CellClumpedSplitter<L> op =
                        new CellClumpedSplitter<L>(NeighborhoodType.valueOf(m_smNeighborhood.getStringValue()),
                                m_executor, m_smMinMaximaSize.getDoubleValue(),
                                m_smIgnoreValueBelowAvgPrecent.getDoubleValue(), m_smMaxInterations.getIntValue());

                final Labeling<Integer> out =
                        SubsetOperations.iterate(op, m_smDimSelection.getSelectedDimIndices(cellLabelingVal
                                .getLabelingMetadata()), labeling, cellLabelingVal.getLabeling().<Integer> factory()
                                .create(cellLabelingVal.getLabeling()), getExecutorService());

                return m_labCellFactory.createCell(out, cellLabelingVal.getLabelingMetadata());

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
