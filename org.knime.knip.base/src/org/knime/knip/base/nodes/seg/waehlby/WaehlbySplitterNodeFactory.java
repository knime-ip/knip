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
package org.knime.knip.base.nodes.seg.waehlby;

import java.util.List;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.TwoValuesToCellNodeDialog;
import org.knime.knip.base.node.TwoValuesToCellNodeFactory;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.util.MinimaUtils;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;

/**
 * Waehlby Cell Clump Splitter node factory
 *
 * @param <T>
 * @param <L>
 * @author Jonathan Hale (University of Konstanz)
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class WaehlbySplitterNodeFactory<T extends RealType<T>, L extends Comparable<L>>
        extends TwoValuesToCellNodeFactory<LabelingValue<L>, ImgPlusValue<T>> {

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    private static SettingsModelInteger createDistanceThresholdModel() {
        return new SettingsModelInteger("dist_thresh", 6);
    }

    private static SettingsModelInteger createGaussSizeModel() {
        return new SettingsModelInteger("gauss_size", 1);
    }

    private static SettingsModelInteger createMergeSizeThresholdModel() {
        return new SettingsModelInteger("size_thresh", 25);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TwoValuesToCellNodeDialog<LabelingValue<L>, ImgPlusValue<T>> createNodeDialog() {
        return new TwoValuesToCellNodeDialog<LabelingValue<L>, ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "General", new DialogComponentNumberEdit(createDistanceThresholdModel(),
                        "Distance Merge Threshold"));
                addDialogComponent("Options", "General",
                                   new DialogComponentNumberEdit(createGaussSizeModel(), "Gauss Size"));
                addDialogComponent("Options", "General", new DialogComponentNumberEdit(createMergeSizeThresholdModel(),
                        "Size Merge Threshold"));

                addDialogComponent("Options", "Dimensions",
                                   new DialogComponentDimSelection(createDimSelectionModel(), "Dimensions", 2, 2)); //2, 5

            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getDefaultSuffixForAppend() {
                return "_wbcs";
            }
        };
    }

    @Override
    public TwoValuesToCellNodeModel<LabelingValue<L>, ImgPlusValue<T>, LabelingCell<String>> createNodeModel() {
        return new TwoValuesToCellNodeModel<LabelingValue<L>, ImgPlusValue<T>, LabelingCell<String>>() {

            private LabelingCellFactory m_labCellFactory;

            private final SettingsModelDimSelection m_smDimSelection = createDimSelectionModel();

            private final SettingsModelInteger m_smDistanceThreshold = createDistanceThresholdModel();

            private final SettingsModelInteger m_smMergeThreshold = createMergeSizeThresholdModel();

            private final SettingsModelInteger m_smGaussSize = createGaussSizeModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_smDimSelection);
                settingsModels.add(m_smDistanceThreshold);
                settingsModels.add(m_smMergeThreshold);
                settingsModels.add(m_smGaussSize);
            }

            @Override
            protected LabelingCell<String> compute(final LabelingValue<L> labelingValue, final ImgPlusValue<T> imgValue)
                    throws Exception {

                final RandomAccessibleInterval<LabelingType<L>> fromCellLabeling = labelingValue.getLabeling();
                final RandomAccessibleInterval<LabelingType<L>> zeroMinFromCellLabeling =
                        MinimaUtils.getZeroMinLabeling(fromCellLabeling);

                final ImgPlus<T> fromCellImg = imgValue.getImgPlus();
                final ImgPlus<T> zeroMinFromCellImg = MinimaUtils.getZeroMinImgPlus(fromCellImg);

                final RandomAccessibleInterval<LabelingType<String>> out =
                        KNIPGateway.ops().create().imgLabeling(zeroMinFromCellLabeling);

                int[] selectedDimIndices = m_smDimSelection.getSelectedDimIndices(labelingValue.getLabelingMetadata());

                WaehlbySplitterOp<L, T> op = new WaehlbySplitterOp<L, T>(
                        WaehlbySplitterOp.SEG_TYPE.SHAPE_BASED_SEGMENTATION, m_smDistanceThreshold.getIntValue(),
                        m_smMergeThreshold.getIntValue(), m_smGaussSize.getIntValue());

                SubsetOperations.iterate(op, selectedDimIndices, zeroMinFromCellLabeling, zeroMinFromCellImg, out,
                                         getExecutorService());

                return m_labCellFactory.createCell(MinimaUtils.getTranslatedLabeling(fromCellLabeling, out),
                                                   labelingValue.getLabelingMetadata());

            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_labCellFactory = new LabelingCellFactory(exec);
            }

        };
    }
}
