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
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.ops.operation.iterableinterval.unary.EqualizeHistogram;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;

import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeDialog;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeFactory;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.ops.img.ImgPlusNormalize;
import org.knime.knip.core.util.EnumListProvider;
import org.knime.knip.core.util.ImgPlusFactory;

/**
 * Factory class to produce an contrast enhancer node.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImageNormalizerNodeFactory<T extends RealType<T>> extends ImgPlusToImgPlusNodeFactory<T, T> {

    private enum ContrastEnhancementMode {
        EQUALIZE, MANUALNORMALIZE, NORMALIZE;
    }

    class ImgPlusWrapper implements UnaryOperation<ImgPlus<T>, ImgPlus<T>> {

        private final UnaryOperation<IterableInterval<T>, IterableInterval<T>> m_op;

        public ImgPlusWrapper(final UnaryOperation<IterableInterval<T>, IterableInterval<T>> op) {
            m_op = op;
        }

        @Override
        public ImgPlus<T> compute(final ImgPlus<T> input, final ImgPlus<T> output) {
            m_op.compute(input, output);
            return output;
        }

        @Override
        public ImgPlusWrapper copy() {
            return new ImgPlusWrapper(m_op.copy());
        }

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
    protected ImgPlusToImgPlusNodeDialog<T> createNodeDialog() {
        return new ImgPlusToImgPlusNodeDialog<T>(createDimSelectionModel(), 1, Integer.MAX_VALUE) {

            private SettingsModelDouble smMax;

            private SettingsModelDouble smMin;

            private SettingsModelBoolean smMinMaxOfNewImage;

            private SettingsModelDouble smSaturation;

            private SettingsModelString type;

            /**
             * {@inheritDoc}
             */
            @Override
            public void addDialogComponents() {

                // Settings Models

                type = createModeModel();

                smMin = createManualMinModel();

                smMax = createManualMaxModel();

                smSaturation = createSaturationModel();

                smMinMaxOfNewImage = createSetMinMaxOfNewImage();

                smMinMaxOfNewImage.addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged(final ChangeEvent e) {
                        m_dimSelectionModel.setEnabled(((SettingsModelBoolean)e.getSource()).getBooleanValue());
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

                addDialogComponent("Options", "Manual Settings", new DialogComponentNumber(smMin, "Min", 1.0));

                addDialogComponent("Options", "Manual Settings", new DialogComponentNumber(smMax, "Max", 1.0));

                addDialogComponent("Options", "Manual Settings", new DialogComponentBoolean(smMinMaxOfNewImage,
                        "Is Target Min/Max?"));

                addDialogComponent("Options", "Saturation (%)", new DialogComponentNumber(smSaturation, "Saturation ",
                        1));

            }

            @Override
            protected void initDialog() {
                initDialog(ContrastEnhancementMode.valueOf(type.getStringValue()));
            }

            private void initDialog(final ContrastEnhancementMode mode) {
                switch (mode) {
                    case MANUALNORMALIZE:
                        smMin.setEnabled(true);
                        smMax.setEnabled(true);
                        smMinMaxOfNewImage.setEnabled(true);
                        smSaturation.setEnabled(false);
                        m_dimSelectionModel.setEnabled(smMinMaxOfNewImage.getBooleanValue());
                        break;
                    case EQUALIZE:
                        smMin.setEnabled(false);
                        smMax.setEnabled(false);
                        smSaturation.setEnabled(false);
                        smMinMaxOfNewImage.setEnabled(false);
                        m_dimSelectionModel.setEnabled(true);
                        break;
                    case NORMALIZE:
                        smMin.setEnabled(false);
                        smMax.setEnabled(false);
                        smMinMaxOfNewImage.setEnabled(false);
                        smSaturation.setEnabled(true);
                        m_dimSelectionModel.setEnabled(true);
                        break;
                    default:
                        throw new RuntimeException("Illegal state in contrast enhancer");
                }
            }
        };
    }

    @Override
    public ImgPlusToImgPlusNodeModel<T, T> createNodeModel() {
        return new ImgPlusToImgPlusNodeModel<T, T>(createDimSelectionModel()) {

            private final SettingsModelDouble m_smManualMax = createManualMaxModel();

            private final SettingsModelDouble m_smManualMin = createManualMinModel();

            private final SettingsModelBoolean m_smMinMaxOfNewImage = createSetMinMaxOfNewImage();

            private final SettingsModelString m_smMode = createModeModel();

            private final SettingsModelDouble m_smSaturation = createSaturationModel();

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
            protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<T>> op(final ImgPlus<T> imgPlus) {
                UnaryOperation<ImgPlus<T>, ImgPlus<T>> op;
                final T val = imgPlus.firstElement().createVariable();

                switch (ContrastEnhancementMode.valueOf(m_smMode.getStringValue())) {

                    case MANUALNORMALIZE:
                        final T min = val.createVariable();
                        final T max = val.createVariable();

                        min.setReal(m_smManualMin.getDoubleValue());
                        max.setReal(m_smManualMax.getDoubleValue());

                        op =
                                new ImgPlusNormalize<T>(0, val, new ValuePair<T, T>(min, max),
                                        m_smMinMaxOfNewImage.getBooleanValue());
                        break;
                    case EQUALIZE:
                        op = new ImgPlusWrapper(new EqualizeHistogram<T>(256));
                        break;
                    case NORMALIZE:
                        op =
                                new ImgPlusNormalize<T>(m_smSaturation.getDoubleValue(), val, null,
                                        m_smMinMaxOfNewImage.getBooleanValue());
                        break;
                    default:
                        throw new RuntimeException("Illegal state in contrast enhancer");
                }

                return Operations.wrap(op, ImgPlusFactory.<T, T> get(imgPlus.firstElement()));
            }

            @Override
            protected int getMinDimensions() {
                return 1;
            }
        };

    }
}
