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
package org.knime.knip.base.nodes.proc;

import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.IterableInterval;
import net.imglib2.labeling.Labeling;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.BinaryObjectFactory;
import net.imglib2.ops.operation.BinaryOutputOperation;
import net.imglib2.ops.operation.ImgOperations;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.iterableinterval.unary.EqualizeHistogram;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
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
import org.knime.knip.core.ops.img.IterableIntervalNormalize;
import org.knime.knip.core.util.EnumListProvider;
import org.knime.knip.core.util.ImgUtils;

/**
 * Factory class to produce an contrast enhancer node.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <T>
 * @param <L>
 */
public class ImgNormalizerNodeFactory<T extends RealType<T>, L extends Comparable<L>> extends
        TwoValuesToCellNodeFactory<ImgPlusValue<T>, LabelingValue<L>> {

    private enum ContrastEnhancementMode {
        EQUALIZE, MANUALNORMALIZE, NORMALIZE;
    }

    private SettingsModelBoolean createSetMinMaxOfNewImage() {
        return new SettingsModelBoolean("min_max_of_new_image", false);
    }

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dimselection", "X", "Y");
    }

    private static SettingsModelDouble createManualMaxModel() {
        return new SettingsModelDouble("manualmax", 0.0);
    }

    private static SettingsModelDouble createManualMinModel() {
        return new SettingsModelDouble("manualmin", 0.0);
    }

    private static SettingsModelString createModeModel() {
        return new SettingsModelString("type", ContrastEnhancementMode.NORMALIZE.name());
    }

    private static SettingsModelDouble createSaturationModel() {
        return new SettingsModelDoubleBounded("saturation", 0, 0, 100);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TwoValuesToCellNodeDialog<ImgPlusValue<T>, LabelingValue<L>> createNodeDialog() {
        return new TwoValuesToCellNodeDialog<ImgPlusValue<T>, LabelingValue<L>>() {

            private SettingsModelDouble max;

            private SettingsModelDouble min;

            private SettingsModelBoolean minMaxOfNewImage;

            private SettingsModelDouble saturation;

            private SettingsModelDimSelection dimSelection;

            private SettingsModelString type;

            /**
             * {@inheritDoc}
             */
            @Override
            public void addDialogComponents() {

                // Settings Models

                type = createModeModel();

                min = createManualMinModel();

                max = createManualMaxModel();

                saturation = createSaturationModel();

                minMaxOfNewImage = createSetMinMaxOfNewImage();

                dimSelection = createDimSelectionModel();

                addDialogComponent("Options", "Dimension Selection", new DialogComponentDimSelection(dimSelection, "",
                        1, Integer.MAX_VALUE));

                minMaxOfNewImage.addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged(final ChangeEvent e) {
                        dimSelection.setEnabled(((SettingsModelBoolean)e.getSource()).getBooleanValue());
                    }
                });

                type.addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged(final ChangeEvent e) {
                        initDialog(ContrastEnhancementMode.valueOf(((SettingsModelString)e.getSource())
                                .getStringValue()));
                    }
                });

                // Dialog Components
                addDialogComponent("Options", "Mode", new DialogComponentStringSelection(type, "Enhancement Type",
                        EnumListProvider.getStringList(ContrastEnhancementMode.values())));

                addDialogComponent("Options", "Manual Settings", new DialogComponentNumber(min, "Min", 1.0));

                addDialogComponent("Options", "Manual Settings", new DialogComponentNumber(max, "Max", 1.0));

                addDialogComponent("Options", "Manual Settings", new DialogComponentBoolean(minMaxOfNewImage,
                        "Is Target Min/Max?"));

                addDialogComponent("Options", "Saturation (%)", new DialogComponentNumber(saturation, "Saturation ", 1));

                initDialog();
            }

            protected void initDialog() {
                initDialog(ContrastEnhancementMode.valueOf(type.getStringValue()));
            }

            private void initDialog(final ContrastEnhancementMode mode) {
                switch (mode) {
                    case MANUALNORMALIZE:
                        min.setEnabled(true);
                        max.setEnabled(true);
                        minMaxOfNewImage.setEnabled(true);
                        saturation.setEnabled(false);
                        dimSelection.setEnabled(minMaxOfNewImage.getBooleanValue());
                        break;
                    case EQUALIZE:
                        min.setEnabled(false);
                        max.setEnabled(false);
                        saturation.setEnabled(false);
                        minMaxOfNewImage.setEnabled(false);
                        dimSelection.setEnabled(true);
                        break;
                    case NORMALIZE:
                        min.setEnabled(false);
                        max.setEnabled(false);
                        minMaxOfNewImage.setEnabled(false);
                        saturation.setEnabled(true);
                        dimSelection.setEnabled(true);
                        break;
                    default:
                        throw new RuntimeException("Illegal state in contrast enhancer");
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isSecondColumnRequired() {
                return false;
            }
        };
    }

    @Override
    public TwoValuesToCellNodeModel<ImgPlusValue<T>, LabelingValue<L>, ImgPlusCell<T>> createNodeModel() {
        return new TwoValuesToCellNodeModel<ImgPlusValue<T>, LabelingValue<L>, ImgPlusCell<T>>() {

            private final SettingsModelDouble m_smManualMax = createManualMaxModel();

            private final SettingsModelDouble m_smManualMin = createManualMinModel();

            private final SettingsModelBoolean m_smMinMaxOfNewImage = createSetMinMaxOfNewImage();

            private final SettingsModelString m_smMode = createModeModel();

            private final SettingsModelDouble m_smSaturation = createSaturationModel();

            private final SettingsModelDimSelection m_dimSelectionModel = createDimSelectionModel();

            private ImgPlusCellFactory m_imgPlusCellFactory;

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {

                // Manual min
                m_smManualMin.setEnabled(false);
                settingsModels.add(m_smManualMin);

                // Manual max
                m_smManualMax.setEnabled(false);
                settingsModels.add(m_smManualMax);

                // Saturation
                settingsModels.add(m_smSaturation);
                m_smSaturation.setEnabled(true);

                m_smMinMaxOfNewImage.setEnabled(false);
                settingsModels.add(m_smMinMaxOfNewImage);

                // Mode
                settingsModels.add(m_smMode);
            }

            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_imgPlusCellFactory = new ImgPlusCellFactory(exec);
            }

            @Override
            protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValue1, final LabelingValue<L> cellValue2)
                    throws Exception {

                ImgPlus<T> res =
                        new ImgPlus<T>(ImgUtils.createEmptyCopy(cellValue1.getImgPlus()), cellValue1.getImgPlus());
                ImgPlus<T> inputA = cellValue1.getImgPlus();

                UnaryOperation<IterableInterval<T>, IterableInterval<T>> op;
                final T val = inputA.firstElement().createVariable();

                switch (ContrastEnhancementMode.valueOf(m_smMode.getStringValue())) {

                    case MANUALNORMALIZE:
                        final T min = val.createVariable();
                        final T max = val.createVariable();

                        min.setReal(m_smManualMin.getDoubleValue());
                        max.setReal(m_smManualMax.getDoubleValue());

                        op =
                                new IterableIntervalNormalize<T>(0, val, new ValuePair<T, T>(min, max),
                                        m_smMinMaxOfNewImage.getBooleanValue());
                        break;
                    case EQUALIZE:
                        op = new EqualizeHistogram<T>(256);
                        break;
                    case NORMALIZE:
                        op =
                                new IterableIntervalNormalize<T>(m_smSaturation.getDoubleValue(), val, null,
                                        m_smMinMaxOfNewImage.getBooleanValue());
                        break;
                    default:
                        throw new RuntimeException("Illegal state in contrast enhancer");
                }

                // No labeling set
                if (cellValue2 == null) {
                    SubsetOperations.iterate(ImgOperations.wrapII(op, res.firstElement().createVariable()),
                                             m_dimSelectionModel.getSelectedDimIndices(cellValue1.getImgPlus()),
                                             cellValue1.getImgPlus(), res);
                } else {
                    SubsetOperations.iterate(new ROIIterableIntervalOp(op),
                                             m_dimSelectionModel.getSelectedDimIndices(cellValue1.getImgPlus()),
                                             cellValue1.getImgPlus(), cellValue2.getLabeling(), res);
                }

                return m_imgPlusCellFactory.createCell(res);
            }
        };

    }

    class ROIIterableIntervalOp implements BinaryOutputOperation<ImgPlus<T>, Labeling<L>, ImgPlus<T>> {

        private UnaryOperation<IterableInterval<T>, IterableInterval<T>> m_op;

        public ROIIterableIntervalOp(final UnaryOperation<IterableInterval<T>, IterableInterval<T>> op) {
            m_op = op;
        }

        @Override
        public ImgPlus<T> compute(final ImgPlus<T> inputA, final Labeling<L> inputB, final ImgPlus<T> output) {

            for (L label : inputB.getLabels()) {
                IterableRegionOfInterest roi = inputB.getIterableRegionOfInterest(label);

                m_op.compute(roi.getIterableIntervalOverROI(inputA), roi.getIterableIntervalOverROI(output));

            }
            return output;
        }

        @Override
        public BinaryOutputOperation<ImgPlus<T>, Labeling<L>, ImgPlus<T>> copy() {
            return new ROIIterableIntervalOp(m_op.copy());
        }

        @Override
        public BinaryObjectFactory<ImgPlus<T>, Labeling<L>, ImgPlus<T>> bufferFactory() {
            return new BinaryObjectFactory<ImgPlus<T>, Labeling<L>, ImgPlus<T>>() {

                @Override
                public ImgPlus<T> instantiate(final ImgPlus<T> inputA, final Labeling<L> inputB) {
                    return new ImgPlus<T>(inputA.factory().create(inputA, inputA.firstElement().createVariable()),
                            inputA);
                }

            };
        }
    }
}
