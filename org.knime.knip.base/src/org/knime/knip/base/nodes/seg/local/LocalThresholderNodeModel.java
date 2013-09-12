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
package org.knime.knip.base.nodes.seg.local;

import java.util.Iterator;
import java.util.List;

import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.BinaryOperation;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.ops.operation.iterable.binary.localthresholder.Bernsen;
import net.imglib2.ops.operation.iterable.binary.localthresholder.MeanLocalThreshold;
import net.imglib2.ops.operation.iterable.binary.localthresholder.MedianLocalThreshold;
import net.imglib2.ops.operation.iterable.binary.localthresholder.MidGrey;
import net.imglib2.ops.operation.iterable.binary.localthresholder.Niblack;
import net.imglib2.ops.operation.iterable.binary.localthresholder.Sauvola;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.ops.iterable.SlidingShapeOpBinaryInside;
import org.knime.knip.core.types.NeighborhoodType;
import org.knime.knip.core.types.OutOfBoundsStrategyEnum;
import org.knime.knip.core.types.OutOfBoundsStrategyFactory;
import org.knime.knip.core.util.ImgPlusFactory;

/**
 * 
 * @author friedrichm, University of Konstanz
 * 
 * @param <T> the pixel type of the input and output image
 */
// TODO Remove with 2.0
/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
@Deprecated
public class LocalThresholderNodeModel<T extends RealType<T>> extends ImgPlusToImgPlusNodeModel<T, BitType> {

    protected static SettingsModelDouble createCModel() {
        return new SettingsModelDouble("c", 0.0);
    }

    protected static SettingsModelDouble createContrastThreshold() {
        return new SettingsModelDouble("contrastThreshold", 0.0);
    }

    protected static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dimselection", "X", "Y");
    }

    protected static SettingsModelDouble createKModel() {
        return new SettingsModelDouble("k", 0.5);
    }

    protected static SettingsModelString createNeighborhoodTypeNodeModel() {
        return new SettingsModelString("neighborhood_type", NeighborhoodType.RECTANGULAR.toString());
    }

    protected static SettingsModelString createOutOfBoundsModel() {
        return new SettingsModelString("outofboundsstrategy", OutOfBoundsStrategyEnum.BORDER.toString());
    }

    protected static SettingsModelDouble createRModel() {
        return new SettingsModelDouble("r", 128.0);
    }

    protected static SettingsModelString createThresholderModel() {
        return new SettingsModelString("thresholder", "");
    }

    protected static SettingsModelInteger createWindowSize() {
        return new SettingsModelInteger("windowsize", 15);
    }

    private final SettingsModelDouble m_c = createCModel();

    private final SettingsModelDouble m_contrastThreshold = createContrastThreshold();

    private final SettingsModelDouble m_k = createKModel();

    private final SettingsModelString m_neighborhoodType = createNeighborhoodTypeNodeModel();

    private final SettingsModelString m_outOfBoundsStrategy = createOutOfBoundsModel();

    private final SettingsModelDouble m_r = createRModel();

    private final SettingsModelInteger m_span = createWindowSize();

    private final SettingsModelString m_thresholder = createThresholderModel();

    public LocalThresholderNodeModel() {
        super(createDimSelectionModel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSettingsModels(final List<SettingsModel> settingsModels) {

        m_k.setEnabled(false);
        m_c.setEnabled(false);
        m_r.setEnabled(false);

        settingsModels.add(m_thresholder);
        settingsModels.add(m_outOfBoundsStrategy);
        settingsModels.add(m_k);
        settingsModels.add(m_c);
        settingsModels.add(m_r);
        settingsModels.add(m_span);
        settingsModels.add(m_contrastThreshold);
        settingsModels.add(m_neighborhoodType);
    }

    @Override
    protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<BitType>> op(final ImgPlus<T> imgPlus) {

        final LocalThresholdingMethodsEnum method =
                Enum.valueOf(LocalThresholdingMethodsEnum.class, m_thresholder.getStringValue());

        final T inputValue = imgPlus.firstElement().createVariable();

        BinaryOperation<Iterator<T>, T, BitType> thresholder = null;

        // TODO: incoperate integral img calculation
        switch (method) {
            case BERNSEN:
                // TODO integral img?
                thresholder =
                        new Bernsen<T, Iterator<T>>(m_contrastThreshold.getDoubleValue(), inputValue.getMaxValue());
                break;
            case MEAN:
                // TODO use integral img wrapper?!
                thresholder = new MeanLocalThreshold<T, Iterator<T>>(m_c.getDoubleValue());
                break;
            case MEDIAN:
                thresholder = new MedianLocalThreshold<T, Iterator<T>>(m_c.getDoubleValue());
                break;
            case MIDGREY:
                thresholder = new MidGrey<T, Iterator<T>>(m_c.getDoubleValue());
                break;
            case NIBLACK:
                thresholder = new Niblack<T, Iterator<T>>(m_k.getDoubleValue(), m_c.getDoubleValue());
                break;
            case SAUVOLA:
                thresholder = new Sauvola<T, Iterator<T>>(m_k.getDoubleValue(), m_r.getDoubleValue());
                break;
            default:
                throw new RuntimeException(new IllegalArgumentException("Unknown thresholding type"));
        }

        return Operations.wrap(new SlidingShapeOpBinaryInside<T, BitType, ImgPlus<T>, ImgPlus<BitType>>(
                                       NeighborhoodType.getNeighborhood(NeighborhoodType.valueOf(m_neighborhoodType
                                               .getStringValue()), m_span.getIntValue()), thresholder,
                                       OutOfBoundsStrategyFactory.<T, ImgPlus<T>> getStrategy(m_outOfBoundsStrategy
                                               .getStringValue(), imgPlus.firstElement())), ImgPlusFactory
                                       .<T, BitType> get(new BitType()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getMinDimensions() {
        return 1;
    }
}
