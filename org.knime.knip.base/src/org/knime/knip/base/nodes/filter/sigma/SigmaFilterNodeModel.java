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
package org.knime.knip.base.nodes.filter.sigma;

import java.util.Iterator;
import java.util.List;

import net.imglib2.algorithm.region.localneighborhood.Shape;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.ops.operation.iterable.unary.Variance;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.ops.iterable.SlidingShapeOpBinaryInside;
import org.knime.knip.core.ops.iterator.SigmaFilter;
import org.knime.knip.core.types.NeighborhoodType;
import org.knime.knip.core.types.OutOfBoundsStrategyEnum;
import org.knime.knip.core.types.OutOfBoundsStrategyFactory;
import org.knime.knip.core.util.ImgPlusFactory;

/**
 * 
 * 
 * @param <T> the pixel type of the input and output image
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author wildnerm, University of Konstanz
 */
public class SigmaFilterNodeModel<T extends RealType<T>> extends ImgPlusToImgPlusNodeModel<T, T> {

    private class CombinedSigmaFilterOp implements UnaryOperation<ImgPlus<T>, ImgPlus<T>> {

        private final boolean m_outlierDetection;

        private final OutOfBoundsFactory<T, ImgPlus<T>> m_outOfBounds;

        private final double m_pixelFraction;

        private final Shape m_shape;

        private final double m_sigmaFactor;

        private final Variance<T, DoubleType> m_variance;

        public CombinedSigmaFilterOp(final Shape neighborhood, final OutOfBoundsFactory<T, ImgPlus<T>> outOfBounds,
                                     final double sigmaFactor, final double pixelFraction,
                                     final boolean outlierDetection) {
            m_variance = new Variance<T, DoubleType>();
            m_shape = neighborhood;
            m_outOfBounds = outOfBounds;
            m_pixelFraction = pixelFraction;
            m_sigmaFactor = sigmaFactor;
            m_outlierDetection = outlierDetection;
        }

        // /////////////////////////////////////////////

        private CombinedSigmaFilterOp(final Variance<T, DoubleType> varianceOp, final Shape shape,
                                      final OutOfBoundsFactory<T, ImgPlus<T>> outOfBounds, final double sigmaFactor,
                                      final double pixelFraction, final boolean outlierDetection) {

            m_variance = (Variance<T, DoubleType>)varianceOp.copy();
            m_shape = shape;
            m_outOfBounds = outOfBounds;
            m_sigmaFactor = sigmaFactor;
            m_pixelFraction = pixelFraction;
            m_outlierDetection = outlierDetection;
        }

        @Override
        public ImgPlus<T> compute(final ImgPlus<T> input, final ImgPlus<T> output) {

            final SlidingShapeOpBinaryInside<T, T, ImgPlus<T>, ImgPlus<T>> slidingWindowOp =
                    new SlidingShapeOpBinaryInside<T, T, ImgPlus<T>, ImgPlus<T>>(m_shape,
                            new SigmaFilter<T, T, Iterator<T>>(Math.sqrt(m_variance.compute(input.cursor(),
                                                                                            new DoubleType())
                                    .getRealDouble()), m_sigmaFactor, m_pixelFraction, m_outlierDetection),
                            m_outOfBounds);

            return slidingWindowOp.compute(input, output);
        }

        @Override
        public UnaryOperation<ImgPlus<T>, ImgPlus<T>> copy() {
            return new CombinedSigmaFilterOp(m_variance, m_shape, m_outOfBounds, m_sigmaFactor, m_pixelFraction,
                    m_outlierDetection);
        }
    }

    protected static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dimselection", "X", "Y");
    }

    protected static SettingsModelString createNeighborhoodTypeNodeModel() {
        return new SettingsModelString("neighborhood_type", NeighborhoodType.RECTANGULAR.toString());
    }

    protected static SettingsModelBoolean createOutlierDetectionModel() {
        return new SettingsModelBoolean("outlier_detection", false);
    }

    protected static SettingsModelString createOutOfBoundsModel() {
        return new SettingsModelString("outofboundsstrategy", OutOfBoundsStrategyEnum.BORDER.toString());
    }

    protected static SettingsModelDouble createPixelFractionModel() {
        return new SettingsModelDouble("pixel_fraction", 0.05);
    }

    protected static SettingsModelDouble createSigmaFactorModel() {
        return new SettingsModelDouble("sigma_factor", 1.0);
    }

    protected static SettingsModelInteger createWindowSize() {
        return new SettingsModelInteger("window_radius", 3);
    }

    private final SettingsModelString m_neighborhoodType = createNeighborhoodTypeNodeModel();

    private final SettingsModelBoolean m_outlierDetection = createOutlierDetectionModel();

    private final SettingsModelString m_outOfBoundsStrategy = createOutOfBoundsModel();

    private final SettingsModelDouble m_pixelFraction = createPixelFractionModel();

    private final SettingsModelDouble m_sigmaFactor = createSigmaFactorModel();

    private final SettingsModelInteger m_span = createWindowSize();

    protected SigmaFilterNodeModel() {
        super(createDimSelectionModel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSettingsModels(final List<SettingsModel> settingsModels) {

        settingsModels.add(m_outOfBoundsStrategy);
        settingsModels.add(m_span);
        settingsModels.add(m_neighborhoodType);
        settingsModels.add(m_pixelFraction);
        settingsModels.add(m_sigmaFactor);
        settingsModels.add(m_outlierDetection);
    }

    @Override
    protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<T>> op(final ImgPlus<T> imgPlus) {
        return Operations.wrap(new CombinedSigmaFilterOp(NeighborhoodType.getNeighborhood(NeighborhoodType
                                       .valueOf(m_neighborhoodType.getStringValue()), m_span.getIntValue()),
                                       OutOfBoundsStrategyFactory.<T, ImgPlus<T>> getStrategy(m_outOfBoundsStrategy
                                               .getStringValue(), imgPlus.firstElement()), m_sigmaFactor
                                               .getDoubleValue(), m_pixelFraction.getDoubleValue(), m_outlierDetection
                                               .getBooleanValue()), ImgPlusFactory.<T, T> get(imgPlus.firstElement()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getMinDimensions() {
        return 1;
    }
}
