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

import net.imglib2.labeling.Labeling;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.TwoValuesToCellNodeDialog;
import org.knime.knip.base.node.TwoValuesToCellNodeFactory;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;

/**
 * Cell Clump Splitter.
 *
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class WaehlbySplitterNodeFactory<T extends RealType<T>, L extends Comparable<L>> extends
        TwoValuesToCellNodeFactory<LabelingValue<L>, ImgPlusValue<T>> {

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TwoValuesToCellNodeDialog<LabelingValue<L>, ImgPlusValue<T>> createNodeDialog() {
        return new TwoValuesToCellNodeDialog<LabelingValue<L>, ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {

                addDialogComponent("Options", "Dimensions", new DialogComponentDimSelection(createDimSelectionModel(),
                        "Dimensions", 2, 5));

            }
        };
    }

    @Override
    public TwoValuesToCellNodeModel<LabelingValue<L>, ImgPlusValue<T>, LabelingCell<Integer>> createNodeModel() {
        return new TwoValuesToCellNodeModel<LabelingValue<L>, ImgPlusValue<T>, LabelingCell<Integer>>() {

            private LabelingCellFactory m_labCellFactory;

            private final SettingsModelDimSelection m_smDimSelection = createDimSelectionModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_smDimSelection);
            }

            @Override
            protected LabelingCell<Integer> compute(final LabelingValue<L> cellLabelingVal, final ImgPlusValue<T> imgValue)
                    throws Exception {

                final Labeling<L> labeling = cellLabelingVal.getLabeling();
                Labeling<Integer> out = labeling.<Integer> factory().create(labeling);

                int[] selectedDimIndices =
                        m_smDimSelection.getSelectedDimIndices(cellLabelingVal.getLabelingMetadata());

                WaehlbySplitterOp<L, T> op = new WaehlbySplitterOp<L, T>(WaehlbySplitterOp.SEG_TYPE.SHAPE_BASED_SEGMENTATION);

                SubsetOperations.iterate(op, selectedDimIndices, labeling, imgValue.getImgPlus(), out,
                                         getExecutorService());

                return m_labCellFactory.createCell(out, cellLabelingVal.getLabelingMetadata());

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
