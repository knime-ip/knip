/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (c) 2011
 *  Slawek Mazur <slawek.mazur@bioquant.uni-heidelberg.de>
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 *  -------------------------------------------------------------------
 */

package org.knime.knip.base.nodes.seg;

import java.util.List;

import net.imglib2.labeling.Labeling;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.labeling.unary.ExcludeOnEdges;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;

/**
 * <code>NodeFactory</code> for the "VoronoiDiagram" Node.
 * 
 * 
 * @author Slawek Mazur <slawek.mazur@bioquant.uni-heidelberg.de>, dietzc <christian.dietz@uni-konstanz.de>
 */
@Deprecated
public class EdgeObjectsExcluderNodeFactory<L extends RealType<L>> extends ValueToCellNodeFactory<LabelingValue<L>> {

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dimSelection", "X", "Y");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<LabelingValue<L>> createNodeDialog() {
        return new ValueToCellNodeDialog<LabelingValue<L>>() {

            @Override
            public void addDialogComponents() {

                addDialogComponent("Options", "", new DialogComponentDimSelection(createDimSelectionModel(),
                        "Dimension selection", 2, 2));
            }
        };
    }

    @Override
    public ValueToCellNodeModel<LabelingValue<L>, LabelingCell<L>> createNodeModel() {
        return new ValueToCellNodeModel<LabelingValue<L>, LabelingCell<L>>() {

            private final SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

            private LabelingCellFactory m_labCellFactory;

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_dimSelection);
            }

            @Override
            protected LabelingCell<L> compute(final LabelingValue<L> inputValue) throws Exception {

                final ExcludeOnEdges<L> excludeOnEdges = new ExcludeOnEdges<L>();

                final Labeling<L> out =
                        SubsetOperations
                                .iterate(excludeOnEdges,
                                         m_dimSelection.getSelectedDimIndices(inputValue.getLabelingMetadata()),
                                         inputValue.getLabeling(),
                                         inputValue.getLabeling().<L> factory().create(inputValue.getLabeling()),
                                         getExecutorService());

                return m_labCellFactory.createCell(out, inputValue.getLabelingMetadata());
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
