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

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.util.EnumUtils;

import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRealRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Resizer Node
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @param <L>
 */
public class LabelingResizerNodeFactory<L> extends ValueToCellNodeFactory<LabelingValue<L>> {

    private enum LabelingResizeStrategy {
        NEAREST_NEIGHBOR("Nearest Neighbor Interpolation"), PERIODICAL("Extend Periodical"), ZERO("Fill with Zero"),
        BORDER("Extend Border");

        private String displayedName;

        private LabelingResizeStrategy(final String _name) {
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

    private static SettingsModelString createResizingStrategyModel() {
        return new SettingsModelString("resizing_strategy", LabelingResizeStrategy.NEAREST_NEIGHBOR.toString());
    }

    private static SettingsModelString createInputFactorInterpretationModel() {
        return new SettingsModelString("input_factor_interpretation", InputFactors.DIM_FACTOR.toString());
    }

    private static SettingsModelResizeInputValues createInputFactorsModel() {
        return new SettingsModelResizeInputValues("input_factors");
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    protected ValueToCellNodeDialog<LabelingValue<L>> createNodeDialog() {
        return new ValueToCellNodeDialog<LabelingValue<L>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Resize Strategy",
                                   new DialogComponentStringSelection(createResizingStrategyModel(), "",
                                           EnumUtils.getStringListFromToString(LabelingResizeStrategy.values())));

                addDialogComponent("Options", "New Dimension Sizes (will affect calibration)",
                                   new DialogComponentResizeInputValues(createInputFactorsModel()));

                addDialogComponent("Options", "New Dimension Sizes (will affect calibration)",
                                   new DialogComponentStringSelection(createInputFactorInterpretationModel(), "",
                                           EnumUtils.getStringListFromToString(InputFactors.values())));

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
    public ValueToCellNodeModel<LabelingValue<L>, LabelingCell<L>> createNodeModel() {
        return new ValueToCellNodeModel<LabelingValue<L>, LabelingCell<L>>() {

            private LabelingCellFactory m_labelingCellFactory;

            private final SettingsModelString m_extensionTypeModel = createResizingStrategyModel();

            private final SettingsModelResizeInputValues m_inputFactorsModel = createInputFactorsModel();

            private final SettingsModelString m_scalingTypeModel = createInputFactorInterpretationModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_extensionTypeModel);
                settingsModels.add(m_inputFactorsModel);
                settingsModels.add(m_scalingTypeModel);

            }

            @Override
            protected LabelingCell<L> compute(final LabelingValue<L> cellValue) throws Exception {

                RandomAccessibleInterval<LabelingType<L>> rai = cellValue.getLabeling();

                LabelingMetadata metadata = cellValue.getLabelingMetadata();

                final double[] inputFactors = m_inputFactorsModel.getNewDimensions(metadata);

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
                            scaleFactors[i] = inputFactors[i] / rai.dimension(i);
                        }
                        break;
                    case DIM_FACTOR:
                        for (int i = 0; i < inputFactors.length; i++) {
                            newDimensions[i] = Math.round(rai.dimension(i) * inputFactors[i]);
                        }
                        scaleFactors = inputFactors;
                        break;
                    case CALIBRATION:
                        scaleFactors = new double[inputFactors.length];
                        for (int i = 0; i < inputFactors.length; i++) {
                            scaleFactors[i] = calibration[i] / inputFactors[i];
                            newDimensions[i] = Math.round(rai.dimension(i) * scaleFactors[i]);
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
                            (calibration[i] * rai.dimension(i)) / newDimensions[i])), i);
                }

                return m_labelingCellFactory
                        .createCell(resample(rai,
                                             EnumUtils.valueForName(m_extensionTypeModel.getStringValue(),
                                                                    LabelingResizeStrategy.values()),
                                             new FinalInterval(newDimensions), scaleFactors),
                                    metadata);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_labelingCellFactory = new LabelingCellFactory(exec);
            }
        };
    }

    private RandomAccessibleInterval<LabelingType<L>>
            resample(final RandomAccessibleInterval<LabelingType<L>> rai, final LabelingResizeStrategy mode,
                     final Interval resultingInterval, final double[] scaleFactors) {

        switch (mode) {
            case NEAREST_NEIGHBOR:

                RealRandomAccessible<LabelingType<L>> interpolate =
                        Views.interpolate(Views.extendBorder(rai),
                                          new NearestNeighborInterpolatorFactory<LabelingType<L>>());

                AffineRealRandomAccessible<LabelingType<L>, AffineGet> affineReal =
                        RealViews.affineReal(interpolate, new Scale(scaleFactors));

                IntervalView<LabelingType<L>> interval = Views.interval(Views.raster(affineReal), resultingInterval);
                ImgLabeling<L, ?> res = KNIPGateway.ops().create().imgLabeling(interval);
                Cursor<LabelingType<L>> cursor = res.cursor();
                for (LabelingType<L> lab : Views.iterable(interval)) {
                    cursor.next().set(lab);
                }
                return res;
            case PERIODICAL:
                return Views.interval(Views.extendPeriodic(rai), resultingInterval);
            case ZERO:
                return Views.interval(Views.extendValue(rai, Util.getTypeFromInterval(rai).createVariable()),
                                      resultingInterval);
            case BORDER:
                return Views.interval(Views.extendBorder(rai), resultingInterval);
            default:
                throw new IllegalArgumentException("Unknown mode in Resample.java");
        }

    }
}
