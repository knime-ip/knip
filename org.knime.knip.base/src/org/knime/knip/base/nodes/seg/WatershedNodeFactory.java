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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.BinaryOperation;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.AbstractRegionGrowing;
import net.imglib2.ops.operation.subset.views.LabelingView;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.TwoValuesToCellNodeDialog;
import org.knime.knip.base.node.TwoValuesToCellNodeFactory;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.ops.labeling.WatershedWithThreshold;
import org.knime.knip.core.util.MiscViews;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class WatershedNodeFactory<T extends RealType<T>, L extends Comparable<L>> extends
        TwoValuesToCellNodeFactory<ImgPlusValue<T>, LabelingValue<L>> {

    private final class WatershedOperationWrapper implements BinaryOperation<Img<T>, Labeling<L>, Labeling<L>> {

        private final WatershedWithThreshold<T, L> m_ws;

        public WatershedOperationWrapper(final WatershedWithThreshold<T, L> ws) {
            m_ws = ws;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Labeling<L> compute(final Img<T> inputA, final Labeling<L> inputB, final Labeling<L> output) {
            m_ws.setSeeds(inputB);
            m_ws.setOutputLabeling(output);
            m_ws.setIntensityImage(inputA);
            m_ws.setStructuringElement(AbstractRegionGrowing.get4ConStructuringElement(inputA.numDimensions()));

            // ws.setStructuringElement(AllConnectedComponents.getStructuringElement(lab
            // .numDimensions()));
            if (!m_ws.checkInput()) {
                throw new IllegalArgumentException(m_ws.getErrorMessage());
            }
            m_ws.process();
            return m_ws.getResult();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BinaryOperation<Img<T>, Labeling<L>, Labeling<L>> copy() {
            return new WatershedOperationWrapper(m_ws);
        }

    }

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dimension_selection", "X", "Y");
    }

    private static SettingsModelDouble createThresholdValueModel() {
        return new SettingsModelDouble("threshold_value", 0);
    }

    private static SettingsModelBoolean createUseThresholdModel() {
        return new SettingsModelBoolean("use_threshold", false);
    }

    private static SettingsModelBoolean createVirtualExtendModel() {
        return new SettingsModelBoolean("virtual_extend", true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TwoValuesToCellNodeDialog<ImgPlusValue<T>, LabelingValue<L>> createNodeDialog() {
        return new TwoValuesToCellNodeDialog<ImgPlusValue<T>, LabelingValue<L>>() {
            @Override
            public void addDialogComponents() {
                final SettingsModelBoolean useThreshold = createUseThresholdModel();
                addDialogComponent("Options", "", new DialogComponentBoolean(useThreshold, "Use Threshold"));

                final SettingsModelDouble thresholdValue = createThresholdValueModel();
                addDialogComponent("Options", "", new DialogComponentNumber(thresholdValue, "Threshold Value", 1));

                useThreshold.addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged(final ChangeEvent arg0) {
                        thresholdValue.setEnabled(useThreshold.getBooleanValue());
                    }
                });
                addDialogComponent("Options", "", new DialogComponentBoolean(createVirtualExtendModel(),
                        "Virtually extend labeling"));

                addDialogComponent("Options", "", new DialogComponentDimSelection(createDimSelectionModel(),
                        "Dimension selection"));
            }

            @Override
            protected String getFirstColumnSelectionLabel() {
                return "Column of Image";
            }

            @Override
            protected String getSecondColumnSelectionLabel() {
                return "Column of Labeling with Seeds";
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TwoValuesToCellNodeModel<ImgPlusValue<T>, LabelingValue<L>, LabelingCell<L>> createNodeModel() {
        return new TwoValuesToCellNodeModel<ImgPlusValue<T>, LabelingValue<L>, LabelingCell<L>>() {

            private final SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

            private LabelingCellFactory m_labCellFactory;

            private final SettingsModelDouble m_thresholdValue = createThresholdValueModel();

            private final SettingsModelBoolean m_useThreshold = createUseThresholdModel();

            private final SettingsModelBoolean m_virtualExtend = createVirtualExtendModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {

                m_thresholdValue.setEnabled(false);

                settingsModels.add(m_useThreshold);
                settingsModels.add(m_thresholdValue);
                settingsModels.add(m_virtualExtend);
                settingsModels.add(m_dimSelection);

            }

            @Override
            protected LabelingCell<L> compute(final ImgPlusValue<T> cellValue1, final LabelingValue<L> cellValue2)
                    throws Exception {
                Labeling<L> lab = cellValue2.getLabeling();
                final ImgPlus<T> img = cellValue1.getImgPlus();
                final Labeling<L> out =
                        new NativeImgLabeling<L, IntType>(new ArrayImgFactory<IntType>().create(img, new IntType()));

                if (m_virtualExtend.getBooleanValue()) {
                    lab =
                            new LabelingView<L>(MiscViews.synchronizeDimensionality(lab,
                                                                                    cellValue2.getLabelingMetadata(),
                                                                                    img, cellValue1.getMetadata()),
                                    lab.<L> factory());
                } else if (lab.numDimensions() != img.numDimensions()) {
                    throw new IllegalArgumentException("The dimensionality of the seed labeling ("
                            + lab.numDimensions() + ") does not match that of the intensity image ("
                            + img.numDimensions() + ")");
                }

                final WatershedWithThreshold<T, L> ws = new WatershedWithThreshold<T, L>();
                if (m_useThreshold.getBooleanValue()) {
                    ws.setThreshold(m_thresholdValue.getDoubleValue());
                }

                final BinaryOperation<Img<T>, Labeling<L>, Labeling<L>> operation = new WatershedOperationWrapper(ws);
                SubsetOperations.iterate(operation, m_dimSelection.getSelectedDimIndices(cellValue1.getMetadata()),
                                         img, lab, out);

                return m_labCellFactory.createCell(out, new DefaultLabelingMetadata(cellValue1.getMetadata(),
                        cellValue1.getMetadata(), cellValue1.getMetadata(), new DefaultLabelingColorTable()));
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
