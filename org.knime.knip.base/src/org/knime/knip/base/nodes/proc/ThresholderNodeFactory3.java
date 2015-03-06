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

import net.imagej.ImgPlus;
import net.imglib2.IterableInterval;
import net.imglib2.ops.img.UnaryRelationAssigment;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.relation.real.unary.RealGreaterThanConstant;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.IterableIntervalsNodeDialog;
import org.knime.knip.base.node.IterableIntervalsNodeFactory;
import org.knime.knip.base.node.IterableIntervalsNodeModel;
import org.knime.knip.core.algorithm.types.ThresholdingType;
import org.knime.knip.core.ops.interval.AutoThreshold;
import org.knime.knip.core.util.EnumUtils;

/**
 * New global thresholder which supports ROI calculation
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @param <T>
 * @param <L>
 */
public class ThresholderNodeFactory3<T extends RealType<T>, L extends Comparable<L>> extends
        IterableIntervalsNodeFactory<T, BitType, L> {

    private static SettingsModelDouble createManualThresholdModel() {
        return new SettingsModelDouble("manual_threshold", 0);
    }

    private static SettingsModelString createThresholderSelectionModel() {
        return new SettingsModelString("thresholder", ThresholdingType.MANUAL.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IterableIntervalsNodeDialog<T> createNodeDialog() {
        return new IterableIntervalsNodeDialog<T>(true) {

            private boolean activeDimSelection;

            @Override
            public void addDialogComponents() {
                final SettingsModelDouble settModManual = createManualThresholdModel();
                addDialogComponent("Options", "Manual Threshold", new DialogComponentNumber(settModManual,
                        "Threshold Value", .01));
                final SettingsModelString method = createThresholderSelectionModel();
                addDialogComponent("Options", "Thresholding Method", new DialogComponentStringSelection(method,
                        "Method", EnumUtils.getStringListFromToString(ThresholdingType.values())));

                method.addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged(final ChangeEvent arg0) {
                        final String selectedMethod = method.getStringValue();
                        if (EnumUtils.valueForName(selectedMethod, ThresholdingType.values()) == ThresholdingType.MANUAL) {
                            settModManual.setEnabled(true);
                            activeDimSelection = false;
                        } else {
                            activeDimSelection = true;
                            settModManual.setEnabled(false);
                        }
                        m_dimSelectionModel.setEnabled(activeDimSelection);
                    }
                });
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected boolean forceActiveDimensionSelection() {
                return activeDimSelection;
            }

        };

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IterableIntervalsNodeModel<T, BitType, L> createNodeModel() {
        return new IterableIntervalsNodeModel<T, BitType, L>() {

            /*
             * The value of the manual threshold
             */
            private final SettingsModelDouble m_manualThreshold = createManualThresholdModel();

            /*
             * The selected thresholder
             */
            private final SettingsModelString m_thresholderSettings = createThresholderSelectionModel();

            // the selected thresholder
            private ThresholdingType m_thresholder;

            // the element of the current iteration
            private T m_currentElement;

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                super.prepareExecute(exec);
                m_thresholder =
                        EnumUtils.valueForName(m_thresholderSettings.getStringValue(), ThresholdingType.values());
            }

            @Override
            public void prepareOperation(final T element) {
                m_currentElement = element;
            }

            /**
             * {@inheritDoc}
             */
            @SuppressWarnings("unchecked")
            @Override
            protected ImgPlusCell<BitType> compute(final ImgPlusValue<T> cellValue) throws Exception {
                ImgPlus<T> img = cellValue.getImgPlus();
                if (img.firstElement() instanceof BitType) {
                    super.setWarningMessage("Image of type 'BitType' remain untouched.");
                    return m_cellFactory.createCell((ImgPlus<BitType>)img);
                }
                return super.compute(cellValue);
            }

            @Override
            public UnaryOperation<IterableInterval<T>, IterableInterval<BitType>> operation() {
                if (m_thresholder == ThresholdingType.MANUAL) {
                    final T type = m_currentElement.createVariable();
                    type.setReal(Math.max(Math.min(m_manualThreshold.getDoubleValue(), type.getMaxValue()),
                                          type.getMinValue()));
                    return new UnaryRelationAssigment<T>(new RealGreaterThanConstant<T>(type));
                } else {
                    return new AutoThreshold<T>(m_thresholder);
                }
            }

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                m_manualThreshold.setEnabled(true);
                m_dimSelectionModel.setEnabled(false);
                settingsModels.add(m_manualThreshold);
                settingsModels.add(m_thresholderSettings);
            }

            @Override
            protected BitType getOutType(final T inType) {
                return new BitType();
            }

        };
    }
}