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
package org.knime.knip.base.nodes.proc.resampler;

import java.util.List;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.interpolation.randomaccess.LanczosInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.meta.axis.DefaultLinearAxis;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.util.EnumUtils;

/**
 * Simple Scaling Node
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @param <T>
 */
public class ResizeNodeFactory<T extends RealType<T>> extends ValueToCellNodeFactory<ImgPlusValue<T>> {

    private enum InterpolationMode {
        LINEAR("Linear Interpolation"), NEAREST_NEIGHBOR("Nearest Neighbor Interpolation"), PERIODICAL(
                "Periodical Interpolation"), LANCZOS("Lanczos Interpolation"), EMPTY("Empty");

        private String displayedName;

        private InterpolationMode(final String _name) {
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

    private enum InputModes {
        ABSOLUTE("Absolute"), RELATIVE("Relative"), CALIBRATION("Calibration");

        private String displayedName;

        private InputModes(final String _name) {
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

    private static SettingsModelString createInterpolationModel() {
        return new SettingsModelString("interpolation_mode", InterpolationMode.NEAREST_NEIGHBOR.toString());
    }

    private static SettingsModelString createScalingTypeModel() {
        return new SettingsModelString("relative_dims", InputModes.RELATIVE.toString());
    }

    private static SettingsModelScalingValues createScalingModel() {
        return new SettingsModelScalingValues("scaling");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options",
                                   "Interpolation mode",
                                   new DialogComponentStringSelection(createInterpolationModel(), "", EnumUtils
                                           .getStringListFromToString(InterpolationMode.values())));
                addDialogComponent("Options", "New Dimension Sizes (will affect calibration)",
                                   new DialogComponentScalingValues(createScalingModel()));
                addDialogComponent("Options",
                                   "New Dimension Sizes (will affect calibration)",
                                   new DialogComponentStringSelection(createScalingTypeModel(), "", EnumUtils
                                           .getStringListFromToString(InputModes.values())));

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

            private final SettingsModelString m_extensionTypeModel = createInterpolationModel();

            private final SettingsModelScalingValues m_scalingValuesModel = createScalingModel();

            private final SettingsModelString m_scalingTypeModel = createScalingTypeModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_extensionTypeModel);
                settingsModels.add(m_scalingValuesModel);
                settingsModels.add(m_scalingTypeModel);

            }

            @Override
            protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValue) throws Exception {

                ImgPlus<T> img = cellValue.getImgPlus();

                ImgPlusMetadata metadata = cellValue.getMetadata();

                final double[] scaling = m_scalingValuesModel.getNewDimensions(metadata);

                final double[] calibration = new double[metadata.numDimensions()];

                for (int i = 0; i < metadata.numDimensions(); i++) {
                    calibration[i] = metadata.axis(i).averageScale(0, 1);
                }

                final double[] scaleFactors;
                final long[] newDimensions = new long[scaling.length];

                // compute scaling factors and new dimensions:
                InputModes inputMode = EnumUtils.valueForName(m_scalingTypeModel.getStringValue(), InputModes.values());
                switch (inputMode) {
                    case ABSOLUTE:
                        scaleFactors = new double[scaling.length];
                        for (int i = 0; i < scaling.length; i++) {
                            newDimensions[i] = Math.round(scaling[i]);
                            scaleFactors[i] = scaling[i] / img.dimension(i);
                        }
                        break;
                    case RELATIVE:
                        for (int i = 0; i < scaling.length; i++) {
                            newDimensions[i] = Math.round(img.dimension(i) * scaling[i]);
                        }
                        scaleFactors = scaling;
                        break;
                    case CALIBRATION:
                        scaleFactors = new double[scaling.length];
                        for (int i = 0; i < scaling.length; i++) {
                            scaleFactors[i] = calibration[i] / scaling[i];
                            newDimensions[i] = Math.round(img.dimension(i) * scaleFactors[i]);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown mode...");
                }

                // adapt calibration
                // without rounding error the formula for the new scale would shorten to "calibration/scaleFactor"... use proportions instead, i.e. "oldCalib*oldDim = newCalib*newDim".

                for (int i = 0; i < metadata.numDimensions(); i++) {
                    metadata.setAxis((new DefaultLinearAxis(metadata.axis(i).type(), (calibration[i] * img.getImg()
                            .dimension(i)) / newDimensions[i])), i);
                }

                return m_imgCellFactory.createCell(resample(img, InterpolationMode.valueOf(m_extensionTypeModel
                                                                    .getStringValue()),
                                                            new FinalInterval(newDimensions),
                                                            scaleFactors), metadata);
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

    private Img<T> resample(final Img<T> img, final InterpolationMode mode, final Interval newDims,
                            final double[] scaleFactors) {
        IntervalView<T> interval = null;
        switch (mode) {
            case LINEAR:
                interval =
                        Views.interval(Views.raster(RealViews.affineReal(Views.interpolate(Views.extendMirrorSingle(img),
                                                                                           new NLinearInterpolatorFactory<T>()),
                                                                         new Scale(scaleFactors))), newDims);
                break;
            case NEAREST_NEIGHBOR:
                interval =
                        Views.interval(Views.raster(RealViews.affineReal(Views.interpolate(Views
                                .extendMirrorSingle(img), new NearestNeighborInterpolatorFactory<T>()), new Scale(
                                scaleFactors))), newDims);
                break;
            case LANCZOS:
                interval =
                        Views.interval(Views.raster(RealViews.affineReal(Views.interpolate(Views.extendMirrorSingle(img),
                                                                                           new LanczosInterpolatorFactory<T>()),
                                                                         new Scale(scaleFactors))), newDims);
                break;
            case PERIODICAL:
                interval = Views.interval(Views.extendPeriodic(img), newDims);
                break;
            default:
                throw new IllegalArgumentException("Unknown mode in Resample.java");
        }

        return new ImgView<T>(interval, img.factory());

    }
}
