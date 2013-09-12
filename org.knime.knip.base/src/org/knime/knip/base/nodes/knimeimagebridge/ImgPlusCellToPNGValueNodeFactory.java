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
package org.knime.knip.base.nodes.knimeimagebridge;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import net.imglib2.Interval;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.interval.binary.IntervalsFromDimSelection;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.image.png.PNGImageCell;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.Real2GreyColorRenderer;

/**
 * Converts ImgPlusValue<T> of any type to PNGImageCell
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgPlusCellToPNGValueNodeFactory<T extends RealType<T>> extends ValueToCellNodeFactory<ImgPlusValue<T>> {

    private NodeLogger LOGGER = NodeLogger.getLogger(ImgPlusCellToPNGValueNodeFactory.class);

    static SettingsModelDimSelection createXYDimSelectionModel() {
        return new SettingsModelDimSelection("dimensions_plane_xy", "X", "Y");
    }

    static SettingsModelDimSelection createChannelDimSelectionModel() {
        return new SettingsModelDimSelection("dimensions_plane_channel", "Channel");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

            private SettingsModelDimSelection m_channelSelection;

            private SettingsModelDimSelection m_xySelection;

            /**
             * {@inheritDoc}
             */
            @Override
            public void addDialogComponents() {

                addDialogComponent("Options", "XY Dimension Selection", new DialogComponentDimSelection(m_xySelection =
                        createXYDimSelectionModel(), "Dimensions Plane", 2, 2));

                addDialogComponent("Options", "Channel Dimension Selection", new DialogComponentDimSelection(
                        m_channelSelection = createChannelDimSelectionModel(), "Dimensions Plane", 0, 1));

            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
                super.saveAdditionalSettingsTo(settings);

                if (m_channelSelection.getNumSelectedDimLabels() > 0) {

                    String channelLabel = m_channelSelection.getSelectedDimLabels().iterator().next();
                    for (String xyLabel : m_xySelection.getSelectedDimLabels()) {
                        if (xyLabel.equalsIgnoreCase(channelLabel)) {
                            throw new InvalidSettingsException("XY Dimensions must not contain the channel dimension!");
                        }
                    }
                }
            }

        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue<T>, ListCell> createNodeModel() {
        return new ValueToCellNodeModel<ImgPlusValue<T>, ListCell>() {

            SettingsModelDimSelection m_dimensionsSelectionXY = createXYDimSelectionModel();

            SettingsModelDimSelection m_dimensionsSelectionChannel = createChannelDimSelectionModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_dimensionsSelectionXY);
                settingsModels.add(m_dimensionsSelectionChannel);
            }

            /**
             * {@inheritDoc}
             * 
             * @throws IllegalArgumentException
             */
            @Override
            protected ListCell compute(final ImgPlusValue<T> cellValue) throws IOException {

                // Order of dimensions always X Y C
                final ImgPlus<T> imgPlus = cellValue.getImgPlus();

                final List<DataCell> cells = new ArrayList<DataCell>(0);

                final int[] channelDim =
                        m_dimensionsSelectionChannel.getSelectedDimIndices(imgPlus.numDimensions(), imgPlus);

                final int[] planeDim = m_dimensionsSelectionXY.getSelectedDimIndices(imgPlus.numDimensions(), imgPlus);

                if (planeDim.length != 2) {
                    LOGGER.warn("The two selected plane dimensions must be contained in the source image. Missing Cell returned.");
                    cells.add(DataType.getMissingCell());
                    return CollectionCellFactory.createListCell(cells);
                }

                // Properties of input are read in
                final boolean channelSelected = channelDim.length == 1;

                if (channelSelected) {
                    if ((imgPlus.dimension(channelDim[0]) != 2) && (imgPlus.dimension(channelDim[0]) != 3)) {
                        LOGGER.warn("Channel selected, but invalid amount of dimensions. Only dimensions of size two or three can be rendered as RGB images. Therefore a grey value image is rendered.");
                    }
                }

                // Combining the both dim selections
                ArrayList<Integer> indicesAsList = new ArrayList<Integer>();
                if (channelSelected) {
                    indicesAsList.add(channelDim[0]);
                }
                indicesAsList.add(planeDim[0]);
                indicesAsList.add(planeDim[1]);

                Collections.sort(indicesAsList);

                int[] indices = new int[indicesAsList.size()];
                for (int i = 0; i < indicesAsList.size(); i++) {
                    indices[i] = indicesAsList.get(i);
                }

                final Interval[] intervals = IntervalsFromDimSelection.compute(indices, imgPlus);

                final long[] min = new long[imgPlus.numDimensions()];
                for (final Interval interval : intervals) {

                    interval.min(min);

                    final Real2GreyColorRenderer<T> renderer =
                            new Real2GreyColorRenderer<T>(channelSelected ? channelDim[0] : -1);

                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ImageIO.write(AWTImageTools.makeBuffered(renderer.render(imgPlus, planeDim[0], planeDim[1], min)
                            .image()), "PNG", out);

                    cells.add(new PNGImageContent(out.toByteArray()).toImageCell());
                }

                return CollectionCellFactory.createListCell(cells);
            }

            @Override
            protected DataType getOutDataCellListCellType() {
                return DataType.getType(PNGImageCell.class);
            }
        };
    }
}
