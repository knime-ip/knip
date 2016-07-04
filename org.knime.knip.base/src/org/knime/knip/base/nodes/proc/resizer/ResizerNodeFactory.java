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
package org.knime.knip.base.nodes.proc.resizer;

import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.core.util.MinimaUtils;

import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.interpolation.randomaccess.LanczosInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.ops.operation.img.unary.ImgCopyOperation;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * Resizer Node
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @param <T>
 */
public class ResizerNodeFactory<T extends RealType<T>> extends ValueToCellNodeFactory<ImgPlusValue<T>> {

    private enum ResizeStrategy {
        LINEAR("Linear Interpolation"), NEAREST_NEIGHBOR("Nearest Neighbor Interpolation"),
        LANCZOS("Lanczos Interpolation"), PERIODICAL("Extend Periodical"), MIN_VALUE("Fill with Minimum Value"),
        MAX_VALUE("Fill with Maximum Value"), ZERO("Fill with Zero"), BORDER("Extend Border");

        private String displayedName;

        private ResizeStrategy(final String _name) {
            displayedName = _name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return displayedName;
        }
    }

    private enum InputFactors {
        DIM_SIZE("Absolute Image Size"), DIM_FACTOR("Relative Scaling Factor"), CALIBRATION("Image Calibration");

        private String displayedName;

        private InputFactors(final String _name) {
            displayedName = _name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return displayedName;
        }
    }

    private enum AffectedDimension {
        DIM_MANUAL("All Dimensions Manual"), DIM_ALL("Affect All Dimensions");

        private String displayedName;

        private AffectedDimension(final String _name) {
            displayedName = _name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return displayedName;
        }
    }

    private static SettingsModelString createResizingStrategyModel() {
        return new SettingsModelString("resizing_strategy", ResizeStrategy.LINEAR.toString());
    }

    private static SettingsModelString createInputFactorInterpretationModel() {
        return new SettingsModelString("input_factor_interpretation", InputFactors.DIM_FACTOR.toString());
    }

    private static SettingsModelResizeInputValues createInputFactorsModel() {
        return new SettingsModelResizeInputValues("input_factors");
    }

    private static SettingsModelString createAffectedDimensionModel() {
        return new SettingsModelString("affected_dimension", AffectedDimension.DIM_MANUAL.toString());
    }

    private static SettingsModelDouble createAffectAllDimensionModel() {
        return new SettingsModelDouble("scaling_factor", 1.0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {

                addDialogComponent("Options", "Resize Strategy",
                                   new DialogComponentStringSelection(createResizingStrategyModel(), "",
                                           EnumUtils.getStringListFromToString(ResizeStrategy.values())));

                final SettingsModelString affectedDim = createAffectedDimensionModel();
                addDialogComponent("Options", "Dimensions to Affect", new DialogComponentStringSelection(affectedDim,
                        "", EnumUtils.getStringListFromToString(AffectedDimension.values())));

                final DialogComponentResizeInputValues dimSizes =
                        new DialogComponentResizeInputValues(createInputFactorsModel());
                addDialogComponent("Options", "New Dimension Sizes (will affect calibration)", dimSizes);

                final DialogComponentNumberEdit dimSize = new DialogComponentNumberEdit(createAffectAllDimensionModel(),
                        "Scaling Factor For All Dimensions");

                addDialogComponent("Options", "New Dimension Sizes (will affect calibration)", dimSize);

                addDialogComponent("Options", "New Dimension Sizes (will affect calibration)",
                                   new DialogComponentStringSelection(createInputFactorInterpretationModel(), "",
                                           EnumUtils.getStringListFromToString(InputFactors.values())));

                affectedDim.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(final ChangeEvent arg0) {
                        final String selectedMethod = affectedDim.getStringValue();
                        if (EnumUtils.valueForName(selectedMethod,
                                                   AffectedDimension.values()) == AffectedDimension.DIM_ALL) {
                            dimSizes.setEnabled(false);
                            dimSize.setEnabled(true);
                        } else {
                            dimSizes.setEnabled(true);
                            dimSize.setEnabled(false);
                        }
                    }
                });
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getDefaultSuffixForAppend() {
                return "_resized";
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>> createNodeModel() {
        return new ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>>() {

            private ImgPlusCellFactory m_imgCellFactory;

            private final SettingsModelString m_extensionTypeModel = createResizingStrategyModel();

            private final SettingsModelResizeInputValues m_inputFactorsModel = createInputFactorsModel();

            private final SettingsModelString m_scalingTypeModel = createInputFactorInterpretationModel();

            private final SettingsModelString m_affectedDimensionModel = createAffectedDimensionModel();

            private final SettingsModelDouble m_inputFactorModel = createAffectAllDimensionModel();

            private final NodeLogger LOGGER = NodeLogger.getLogger(ValueToCellNodeModel.class);

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_affectedDimensionModel);
                settingsModels.add(m_inputFactorModel);
                settingsModels.add(m_extensionTypeModel);
                settingsModels.add(m_inputFactorsModel);
                settingsModels.add(m_scalingTypeModel);
                m_inputFactorModel.setEnabled(false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
                collectSettingsModels();
                for (final SettingsModel sm : m_settingsModels) {
                    try {
                        sm.validateSettings(settings);
                    } catch (final InvalidSettingsException e) {

                        if (!sm.equals(m_inputFactorModel) && !sm.equals(m_affectedDimensionModel)) {
                            LOGGER.warn("Problems occurred validating the settings " + sm.toString() + ": "
                                    + e.getLocalizedMessage());
                            setWarningMessage("Problems occurred while validating settings.");
                        }
                    }
                }

            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
                collectSettingsModels();
                for (final SettingsModel sm : m_settingsModels) {
                    try {
                        sm.loadSettingsFrom(settings);
                    } catch (final InvalidSettingsException e) {
                        if (!sm.equals(m_inputFactorModel) && !sm.equals(m_affectedDimensionModel)) {
                            LOGGER.warn("Problems occurred loading the settings " + sm.toString() + ": "
                                    + e.getLocalizedMessage());
                            setWarningMessage("Problems occurred while loading settings.");
                        }
                    }
                }
            }

            @Override
            protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValue) throws Exception {

                final ImgPlus<T> fromCell = cellValue.getImgPlus();
                final ImgPlus<T> zeroMinFromCell = MinimaUtils.getZeroMinImgPlus(fromCell);

                ImgPlusMetadata metadata = cellValue.getMetadata();

                final double[] inputFactors = m_inputFactorsModel.getNewDimensions(metadata);

                switch (EnumUtils.valueForName(m_affectedDimensionModel.getStringValue(), AffectedDimension.values())) {
                    case DIM_MANUAL:
                        break;
                    case DIM_ALL:
                        final double inputFactor = m_inputFactorModel.getDoubleValue();

                        for (int i = 0; i < inputFactors.length; i++) {
                            inputFactors[i] = inputFactor;
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown interpretation of input factors!");
                }

                // Extract calibration
                final double[] calibration = new double[metadata.numDimensions()];
                for (int i = 0; i < metadata.numDimensions(); i++) {
                    calibration[i] = metadata.axis(i).averageScale(0, 1);
                }

                final double[] scaleFactors;
                final long[] newDimensions = new long[inputFactors.length];

                // compute scaling factors and new dimensions:
                switch (EnumUtils.valueForName(m_scalingTypeModel.getStringValue(), InputFactors.values())) {
                    case DIM_SIZE:
                        scaleFactors = new double[inputFactors.length];
                        for (int i = 0; i < inputFactors.length; i++) {
                            newDimensions[i] = Math.round(inputFactors[i]);
                            scaleFactors[i] = inputFactors[i] / zeroMinFromCell.dimension(i);
                        }
                        break;
                    case DIM_FACTOR:
                        for (int i = 0; i < inputFactors.length; i++) {
                            newDimensions[i] = Math.round(zeroMinFromCell.dimension(i) * inputFactors[i]);
                        }
                        scaleFactors = inputFactors;
                        break;
                    case CALIBRATION:
                        scaleFactors = new double[inputFactors.length];
                        for (int i = 0; i < inputFactors.length; i++) {
                            scaleFactors[i] = calibration[i] / inputFactors[i];
                            newDimensions[i] = Math.round(zeroMinFromCell.dimension(i) * scaleFactors[i]);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown interpretation of input factors!");
                }

                // adapt calibration
                // without rounding error the formula for the new scale would shorten to "calibration/scaleFactor"... use proportions instead, i.e. "oldCalib*oldDim = newCalib*newDim".
                // TODO: Only LinearAxis support since now
                for (int i = 0; i < metadata.numDimensions(); i++) {
                    metadata.setAxis((new DefaultLinearAxis(metadata.axis(i).type(),
                            (calibration[i] * zeroMinFromCell.getImg().dimension(i)) / newDimensions[i])), i);
                }

                return m_imgCellFactory
                        .createCell(MinimaUtils.getTranslatedImgPlus(fromCell, new ImgPlus<T>(
                                resample(zeroMinFromCell,
                                         EnumUtils.valueForName(m_extensionTypeModel.getStringValue(), ResizeStrategy
                                                 .values()),
                                         new FinalInterval(newDimensions), scaleFactors),
                                metadata)));
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

    private Img<T> resample(final Img<T> img, final ResizeStrategy mode, final Interval resultingInterval,
                            final double[] scaleFactors) {

        switch (mode) {
            case LINEAR:
                // create copy of Img
                return (Img<T>)new ImgCopyOperation<T>().compute(new ImgView<T>(
                        Views.interval(Views.raster(
                                                    RealViews.affineReal(
                                                                         Views.interpolate(Views.extendBorder(img),
                                                                                           new NLinearInterpolatorFactory<T>()),
                                                                         new Scale(scaleFactors))),
                                       resultingInterval),
                        img.factory()), img.factory().create(resultingInterval, img.firstElement().createVariable()));
            case NEAREST_NEIGHBOR:
                return (Img<T>)new ImgCopyOperation<T>().compute(new ImgView<T>(
                        Views.interval(Views.raster(
                                                    RealViews.affineReal(
                                                                         Views.interpolate(Views.extendBorder(img),
                                                                                           new NearestNeighborInterpolatorFactory<T>()),
                                                                         new Scale(scaleFactors))),
                                       resultingInterval),
                        img.factory()), img.factory().create(resultingInterval, img.firstElement().createVariable()));
            case LANCZOS:
                return (Img<T>)new ImgCopyOperation<T>().compute(new ImgView<T>(
                        Views.interval(Views.raster(
                                                    RealViews.affineReal(
                                                                         Views.interpolate(Views.extendBorder(img),
                                                                                           new LanczosInterpolatorFactory<T>()),
                                                                         new Scale(scaleFactors))),
                                       resultingInterval),
                        img.factory()), img.factory().create(resultingInterval, img.firstElement().createVariable()));
            case PERIODICAL:
                return new ImgView<T>(Views.interval(Views.extendPeriodic(img), resultingInterval), img.factory());
            case ZERO:
                return new ImgView<T>(
                        Views.interval(Views.extendValue(img, img.firstElement().createVariable()), resultingInterval),
                        img.factory());
            case MIN_VALUE:
                T min = img.firstElement().createVariable();
                min.setReal(min.getMinValue());
                return new ImgView<T>(Views.interval(Views.extendValue(img, min), resultingInterval), img.factory());
            case MAX_VALUE:
                T max = img.firstElement().createVariable();
                max.setReal(max.getMaxValue());
                return new ImgView<T>(Views.interval(Views.extendValue(img, max), resultingInterval), img.factory());
            case BORDER:
                return new ImgView<T>(Views.interval(Views.extendBorder(img), resultingInterval), img.factory());
            default:
                throw new IllegalArgumentException("Unknown mode in Resample.java");
        }

    }
}
