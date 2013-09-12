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
package org.knime.knip.base.nodes.seg.graphcuts;

import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.TwoValuesToCellNodeDialog;
import org.knime.knip.base.node.TwoValuesToCellNodeFactory;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.ops.seg.GraphCut2D;
import org.knime.knip.core.ops.seg.GraphCut2DLab;
import org.knime.knip.core.util.ImgUtils;

/**
 * 
 * Node to perform a graph cut where the sink and source a derived from a labeling.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class GraphCutNodeFactory<T extends RealType<T>, L extends Comparable<L>> extends
        TwoValuesToCellNodeFactory<ImgPlusValue<T>, LabelingValue<L>> {

    private static SettingsModelString createBGLabelModel() {
        return new SettingsModelString("bg_label", "bg");
    }

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    private static SettingsModelDimSelection createFeatDimSelectionModel() {
        return new SettingsModelDimSelection("feature_dim_selection", "");
    }

    private static SettingsModelString createFGLabelModel() {
        return new SettingsModelString("fg_label", "fg");
    }

    private static SettingsModelDoubleBounded createLambdaModel() {
        return new SettingsModelDoubleBounded("lambda", 0.5, 0.0, 1.0);
    }

    private static SettingsModelDouble createPottsWeightModel() {
        return new SettingsModelDouble("potts_weight", 500);
    }

    private static SettingsModelDouble createSinkValueModel() {
        return new SettingsModelDouble("sink_value", 1.0);
    }

    private static SettingsModelDouble createSourceValueModel() {
        return new SettingsModelDouble("source_value", 0);
    }

    private static SettingsModelBoolean createUseMinMaxModel() {
        return new SettingsModelBoolean("use_minmax", true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TwoValuesToCellNodeDialog<ImgPlusValue<T>, LabelingValue<L>> createNodeDialog() {
        return new TwoValuesToCellNodeDialog<ImgPlusValue<T>, LabelingValue<L>>() {

            @Override
            public void addDialogComponents() {

                // Settings models labeling
                final SettingsModelDoubleBounded labLambdaModel = createLambdaModel();
                final SettingsModelString fgLabelModel = createFGLabelModel();
                final SettingsModelString bgLabelModel = createBGLabelModel();

                // settings models no labeling
                final SettingsModelDouble pottWeightsModel = createPottsWeightModel();
                final SettingsModelDouble sourceValueModel = createSourceValueModel();
                final SettingsModelDouble sinkValueModel = createSinkValueModel();
                final SettingsModelBoolean minMaxModel = createUseMinMaxModel();

                getSecondColumnSettingsModel().addChangeListener(new ChangeListener() {

                    private void setLabelingSettings(final boolean state) {
                        labLambdaModel.setEnabled(state);
                        fgLabelModel.setEnabled(state);
                        bgLabelModel.setEnabled(state);
                    }

                    private void setNoLabelingSettings(final boolean state) {
                        sourceValueModel.setEnabled(state);
                        sinkValueModel.setEnabled(state);
                        pottWeightsModel.setEnabled(state);
                        minMaxModel.setEnabled(state);
                    }

                    @Override
                    public void stateChanged(final ChangeEvent arg0) {
                        if (getSecondColumnSettingsModel().getStringValue() == null) {
                            setLabelingSettings(false);
                            setNoLabelingSettings(true);
                        } else {
                            setLabelingSettings(true);
                            setNoLabelingSettings(false);
                        }
                    }
                });

                // General options
                addDialogComponent("Options", "Dimension selection", new DialogComponentDimSelection(
                        createDimSelectionModel(), "", 2, 2));

                addDialogComponent("Options", "Feature dimension (optional)", new DialogComponentDimSelection(
                        createFeatDimSelectionModel(), "", 0, 1));

                // Without labeling (init without)

                addDialogComponent("Options (labeling)", "", new DialogComponentNumber(labLambdaModel, "Lambda", 0.05));

                addDialogComponent("Options (labeling)", "",
                                   new DialogComponentString(fgLabelModel, "Foreground label"));

                addDialogComponent("Options (labeling)", "",
                                   new DialogComponentString(bgLabelModel, "Background label"));

                // Needed if no lab selected

                addDialogComponent("Options (no labeling)", "", new DialogComponentNumber(pottWeightsModel,
                        "Potts Weight", 0.05));

                addDialogComponent("Options (no labeling)", "", new DialogComponentNumber(sourceValueModel,
                        "Source Value", 0.05));

                addDialogComponent("Options (no labeling)", "", new DialogComponentNumber(sinkValueModel, "Sink Value",
                        0.05));

                addDialogComponent("Options (no labeling)", "", new DialogComponentBoolean(minMaxModel,
                        "Use image's Min/Max as Source/Sink"));

            }

            @Override
            protected String getFirstColumnSelectionLabel() {
                return "Column of Image";
            }

            @Override
            protected String getSecondColumnSelectionLabel() {
                return "Column of Labeling with Seeds";
            }

            @Override
            public boolean isFirstColumnRequired() {
                return true;
            }

            @Override
            public boolean isSecondColumnRequired() {
                return false;
            }
        };

    }

    @Override
    public TwoValuesToCellNodeModel<ImgPlusValue<T>, LabelingValue<L>, ImgPlusCell<BitType>> createNodeModel() {
        return new TwoValuesToCellNodeModel<ImgPlusValue<T>, LabelingValue<L>, ImgPlusCell<BitType>>() {

            private final SettingsModelString m_bgLabel = createBGLabelModel();

            private int[] m_colIndices;

            private final SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

            private final SettingsModelDimSelection m_featDimSelection = createFeatDimSelectionModel();

            private final SettingsModelString m_fgLabel = createFGLabelModel();

            private ImgPlusCellFactory m_imgCellFactory;

            private final SettingsModelDoubleBounded m_lambdaSelection = createLambdaModel();

            // Needed if no labeling is selected
            private final SettingsModelDouble m_pottsWeight = createPottsWeightModel();

            private final SettingsModelDouble m_sinkValue = createSinkValueModel();

            private final SettingsModelDouble m_sourceValue = createSourceValueModel();

            private final SettingsModelBoolean m_useMinMax = createUseMinMaxModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {

                if (m_colIndices[1] == -1) {
                    m_fgLabel.setEnabled(false);
                    m_bgLabel.setEnabled(false);
                    m_lambdaSelection.setEnabled(false);
                } else {
                    m_pottsWeight.setEnabled(false);
                    m_sourceValue.setEnabled(false);
                    m_useMinMax.setEnabled(false);
                    m_sinkValue.setEnabled(false);
                }

                settingsModels.add(m_dimSelection);
                settingsModels.add(m_fgLabel);
                settingsModels.add(m_bgLabel);
                settingsModels.add(m_lambdaSelection);
                settingsModels.add(m_featDimSelection);
                settingsModels.add(m_pottsWeight);
                settingsModels.add(m_sourceValue);
                settingsModels.add(m_sinkValue);
                settingsModels.add(m_useMinMax);

            }

            @Override
            protected ImgPlusCell<BitType> compute(final ImgPlusValue<T> cellValue1, final LabelingValue<L> cellValue2)
                    throws Exception {
                final ImgPlus<T> imgPlus = cellValue1.getImgPlus();
                final int[] selectedDims = m_dimSelection.getSelectedDimIndices(imgPlus.numDimensions(), imgPlus);
                final int[] selectedFeatDims =
                        m_featDimSelection.getSelectedDimIndices(imgPlus.numDimensions(), imgPlus);

                if (cellValue2 != null) {
                    GraphCut2DLab<T, L> cutOp;
                    if (selectedFeatDims.length == 0) {
                        cutOp =
                                new GraphCut2DLab<T, L>(m_lambdaSelection.getDoubleValue(), m_fgLabel.getStringValue(),
                                        m_bgLabel.getStringValue(), selectedDims[0], selectedDims[1]);

                        // TODO: Logger
                        Img<BitType> out = null;

                        out =
                                SubsetOperations.iterate(cutOp, m_dimSelection.getSelectedDimIndices(imgPlus),
                                                         cellValue1.getImgPlus(), cellValue2.getLabeling(), ImgUtils
                                                                 .createEmptyCopy(cellValue1.getImgPlus(),
                                                                                  new BitType()), getExecutorService());

                        return m_imgCellFactory.createCell(out, cellValue1.getMetadata());

                    } else {
                        cutOp =
                                new GraphCut2DLab<T, L>(m_lambdaSelection.getDoubleValue(), m_fgLabel.getStringValue(),
                                        m_bgLabel.getStringValue(), selectedDims[0], selectedDims[1],
                                        selectedFeatDims[0]);

                        return m_imgCellFactory
                                .createCell(Operations.compute(cutOp, imgPlus, cellValue2.getLabeling()),
                                            cellValue1.getMetadata());
                    }

                } else {
                    double sink;
                    double source;
                    if (m_useMinMax.getBooleanValue()) {
                        sink = imgPlus.firstElement().getMaxValue();
                        source = imgPlus.firstElement().getMinValue();
                    } else {
                        sink = m_sinkValue.getDoubleValue();
                        source = m_sourceValue.getDoubleValue();
                    }

                    if (selectedFeatDims.length == 0) {
                        final GraphCut2D<T, Img<T>, Img<BitType>> cutOp =
                                new GraphCut2D<T, Img<T>, Img<BitType>>(m_pottsWeight.getDoubleValue(),
                                        selectedDims[0], selectedDims[1], sink, source);

                        // TODO: Logger
                        Img<BitType> out = null;

                        out =
                                SubsetOperations.iterate(cutOp, m_dimSelection.getSelectedDimIndices(imgPlus),
                                                         cellValue1.getImgPlus(), ImgUtils.createEmptyCopy(cellValue1
                                                                 .getImgPlus(), new BitType()), getExecutorService());

                        return m_imgCellFactory.createCell(out, cellValue1.getMetadata());
                    } else {
                        final GraphCut2D<T, Img<T>, Img<BitType>> cutOp =
                                new GraphCut2D<T, Img<T>, Img<BitType>>(m_pottsWeight.getDoubleValue(),
                                        selectedDims[0], selectedDims[1], selectedFeatDims[0], sink, source);

                        return m_imgCellFactory
                                .createCell(Operations.compute(cutOp, imgPlus), cellValue1.getMetadata());
                    }

                }
            }

            @Override
            protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
                m_colIndices = getColIndices(inSpecs[0]);

                if (m_colIndices[1] == -1) {
                    m_fgLabel.setEnabled(false);
                    m_bgLabel.setEnabled(false);
                    m_lambdaSelection.setEnabled(false);
                } else {
                    m_pottsWeight.setEnabled(false);
                    m_sourceValue.setEnabled(false);
                    m_useMinMax.setEnabled(false);
                    m_sinkValue.setEnabled(false);
                }

                return super.configure(inSpecs);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_imgCellFactory = new ImgPlusCellFactory(exec);
            }
        };
    }

}
