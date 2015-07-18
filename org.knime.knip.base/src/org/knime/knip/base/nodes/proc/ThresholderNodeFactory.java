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
import net.imglib2.img.Img;
import net.imglib2.ops.img.UnaryRelationAssigment;
import net.imglib2.ops.operation.ImgOperations;
import net.imglib2.ops.operation.SubsetOperations;
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
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.exceptions.KNIPRuntimeException;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.algorithm.types.ThresholdingType;
import org.knime.knip.core.ops.interval.AutoThreshold;
import org.knime.knip.core.util.EnumUtils;

/**
 * Factory class to produce an image thresholder node.
 *
 * @param <T>
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
@Deprecated
public class ThresholderNodeFactory<T extends RealType<T>> extends ValueToCellNodeFactory<ImgPlusValue<T>> {

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("thresholder_dimselection", "X", "Y");
    }

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
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {
            @Override
            public void addDialogComponents() {
                final SettingsModelDouble settModManual = createManualThresholdModel();
                addDialogComponent("Options", "Manual Threshold", new DialogComponentNumber(settModManual,
                        "Threshold value", .01));
                final SettingsModelString method = createThresholderSelectionModel();
                addDialogComponent("Options", "Thresholding method", new DialogComponentStringSelection(method,
                        "Method", EnumUtils.getStringListFromName(ThresholdingType.values())));
                addDialogComponent("Options", "Dim selection", new DialogComponentDimSelection(
                        createDimSelectionModel(), "", 2, 5));

                method.addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged(final ChangeEvent arg0) {
                        settModManual.setEnabled(ThresholdingType.valueOf(method.getStringValue()) == ThresholdingType.MANUAL);
                    }
                });
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getDefaultSuffixForAppend() {
                return "_thresholded";
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<BitType>> createNodeModel() {
        return new ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<BitType>>() {

            /*
             * The dimension selection
             */
            private final SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

            /*
             * ImgPlusCellFactory
             */
            private ImgPlusCellFactory m_imgCellFactory;

            /*
             * The value of the manual threshold
             */
            private final SettingsModelDouble m_manualThreshold = createManualThresholdModel();

            /*
             * The selected thresholder
             */
            private final SettingsModelString m_thresholderSettings = createThresholderSelectionModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {

                m_manualThreshold.setEnabled(true);

                settingsModels.add(m_dimSelection);
                settingsModels.add(m_manualThreshold);
                settingsModels.add(m_thresholderSettings);
            }

            @Override
            protected ImgPlusCell<BitType> compute(final ImgPlusValue<T> cellValue) throws Exception {

                final ThresholdingType thresholder =
                        Enum.valueOf(ThresholdingType.class, m_thresholderSettings.getStringValue());
                final ImgPlus<T> imgPlus = cellValue.getImgPlus();
                final ImgPlus<BitType> res =
                        new ImgPlus<BitType>((Img<BitType>)KNIPGateway.ops().createImg(imgPlus, new BitType()), imgPlus);
                if (thresholder == ThresholdingType.MANUAL) {

                    final T type = cellValue.getImgPlus().firstElement().createVariable();
                    type.setReal(m_manualThreshold.getDoubleValue());

                    new UnaryRelationAssigment<T>(new RealGreaterThanConstant<T>(type)).compute(cellValue.getImgPlus(),
                                                                                                res);

                    return m_imgCellFactory.createCell(res, cellValue.getMetadata());
                }

                try {
                    //the different thresholding methods can fail and throw a runtime exception in these cases
                    SubsetOperations.iterate(ImgOperations.wrapII(new AutoThreshold<T>(thresholder), new BitType()),
                                             m_dimSelection.getSelectedDimIndices(imgPlus), cellValue.getImgPlus(),
                                             res, getExecutorService());
                } catch (Exception e) {

                    throw new KNIPRuntimeException(e.getMessage(), e);
                }

                return m_imgCellFactory.createCell(res);
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
