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
import net.imglib2.ops.operation.UnaryOperation;
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
import org.knime.knip.base.node.IterableIntervalsNodeDialog;
import org.knime.knip.base.node.IterableIntervalsNodeFactory;
import org.knime.knip.base.node.IterableIntervalsNodeModel;
import org.knime.knip.core.ops.img.IterableIntervalNormalize;
import org.knime.knip.core.util.EnumUtils;

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
        IterableIntervalsNodeFactory<T, T, L> {

    private enum ContrastEnhancementMode {
        EQUALIZE, MANUALNORMALIZE, NORMALIZE;
    }

    private SettingsModelBoolean createSetMinMaxOfNewImage() {
        return new SettingsModelBoolean("min_max_of_new_image", false);
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
    protected IterableIntervalsNodeDialog<T> createNodeDialog() {
        return new IterableIntervalsNodeDialog<T>(true) {

            private SettingsModelDouble max;

            private SettingsModelDouble min;

            private SettingsModelBoolean minMaxOfNewImage;

            private SettingsModelDouble saturation;

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

                minMaxOfNewImage.addChangeListener(new ChangeListener() {

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
                        EnumUtils.getStringListFromName(ContrastEnhancementMode.values())));

                addDialogComponent("Options", "Manual Settings", new DialogComponentNumber(min, "Min", 1.0));

                addDialogComponent("Options", "Manual Settings", new DialogComponentNumber(max, "Max", 1.0));

                addDialogComponent("Options", "Manual Settings", new DialogComponentBoolean(minMaxOfNewImage,
                        "Is Target Min/Max?"));

                addDialogComponent("Options", "Saturation (%)", new DialogComponentNumber(saturation, "Saturation ", 1));

                initDialog(ContrastEnhancementMode.valueOf(type.getStringValue()));
            }

            private void initDialog(final ContrastEnhancementMode mode) {
                switch (mode) {
                    case MANUALNORMALIZE:
                        min.setEnabled(true);
                        max.setEnabled(true);
                        minMaxOfNewImage.setEnabled(true);
                        saturation.setEnabled(false);
                        m_dimSelectionModel.setEnabled(minMaxOfNewImage.getBooleanValue());
                        break;
                    case EQUALIZE:
                        min.setEnabled(false);
                        max.setEnabled(false);
                        saturation.setEnabled(false);
                        minMaxOfNewImage.setEnabled(false);
                        m_dimSelectionModel.setEnabled(true);
                        break;
                    case NORMALIZE:
                        min.setEnabled(false);
                        max.setEnabled(false);
                        minMaxOfNewImage.setEnabled(false);
                        saturation.setEnabled(true);
                        m_dimSelectionModel.setEnabled(true);
                        break;
                    default:
                        throw new RuntimeException("Illegal state in contrast enhancer");
                }
            }
        };
    }

    @Override
    public IterableIntervalsNodeModel<T, T, L> createNodeModel() {
        return new IterableIntervalsNodeModel<T, T, L>() {

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
            public UnaryOperation<IterableInterval<T>, IterableInterval<T>> operation(final IterableInterval<T> input) {

                UnaryOperation<IterableInterval<T>, IterableInterval<T>> op;
                final T val = input.firstElement().createVariable();

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

                return op;
            }

            @Override
            protected T getOutType(final IterableInterval<T> input) {
                return input.firstElement().createVariable();
            }

        };

    }
}
