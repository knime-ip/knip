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
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
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
public class ResamplerNodeFactory<T extends RealType<T>> extends ValueToCellNodeFactory<ImgPlusValue<T>> {

    private enum Mode {
        LINEAR, NEAREST_NEIGHBOR, PERIODICAL, LANCZOS;
    }

    private static SettingsModelString createInterpolationModel() {
        return new SettingsModelString("interpolation_mode", Mode.NEAREST_NEIGHBOR.toString());
    }

    private static SettingsModelBoolean createRelDimsModel() {
        return new SettingsModelBoolean("relative_dims", true);
    }

    private static SettingsModelScalingValues createScalingModel() {
        return new SettingsModelScalingValues("scaling");
    }

    private static SettingsModelBoolean createVirtualInterpolationModel() {
        return new SettingsModelBoolean("virtual_interpolation", false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

            @SuppressWarnings("deprecation")
            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Interpolation Mode", new DialogComponentStringSelection(
                        createInterpolationModel(), "", EnumUtils.getStringListFromName(Mode.values())));
                addDialogComponent("Options", "New Dimension Sizes", new DialogComponentScalingValues(
                        createScalingModel()));
                addDialogComponent("Options", "New Dimension Sizes", new DialogComponentBoolean(createRelDimsModel(),
                        "Relative?"));
                addDialogComponent("Options", "Virtual Interpolation", new DialogComponentBoolean(
                        createVirtualInterpolationModel(), "Virtual Interpolation?"));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getDefaultSuffixForAppend() {
                return "_resampled";
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

            private final SettingsModelString m_interpolationSettings = createInterpolationModel();

            private final SettingsModelScalingValues m_newDimensions = createScalingModel();

            private final SettingsModelBoolean m_relativeDims = createRelDimsModel();

            private final SettingsModelBoolean m_persistModel = createVirtualInterpolationModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_interpolationSettings);
                settingsModels.add(m_newDimensions);
                settingsModels.add(m_relativeDims);
                settingsModels.add(m_persistModel);

            }

            @Override
            protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValue) throws Exception {

                ImgPlus<T> img = cellValue.getImgPlus();

                final double[] scaling = m_newDimensions.getNewDimensions(cellValue.getMetadata());
                double[] scaleFactors = new double[scaling.length];

                final long[] newDimensions = new long[scaling.length];
                if (m_relativeDims.getBooleanValue()) {
                    for (int i = 0; i < scaling.length; i++) {
                        newDimensions[i] = Math.round(img.dimension(i) * scaling[i]);
                    }
                    scaleFactors = scaling;
                } else {
                    for (int i = 0; i < scaling.length; i++) {
                        newDimensions[i] = Math.round(scaling[i]);
                        scaleFactors[i] = scaling[i] / img.dimension(i);
                    }
                }

                return m_imgCellFactory.createCell(resample(img,
                                                            Mode.valueOf(m_interpolationSettings.getStringValue()),
                                                            new FinalInterval(newDimensions), scaleFactors,
                                                            m_persistModel.getBooleanValue()), cellValue.getMetadata());
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

    private Img<T> resample(final Img<T> img, final Mode mode, final Interval newDims, final double[] scaleFactors,
                            final boolean virtualCalculation) {
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
        ImgView<T> imgView = new ImgView<T>(interval, img.factory());
        if (!virtualCalculation) {
            return imgView.copy();
        } else {
            return imgView;
        }
    }
}
