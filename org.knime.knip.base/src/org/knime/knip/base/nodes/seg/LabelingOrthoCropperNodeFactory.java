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
package org.knime.knip.base.nodes.seg;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Interval;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.meta.Axes;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.DefaultCalibratedAxis;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.labeling.unary.MergeLabelings;
import net.imglib2.ops.operation.subset.views.LabelingView;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.view.Views;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentSubsetSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection;

/**
 * Factory class to produce the Histogram Operations Node.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelingOrthoCropperNodeFactory<L extends Comparable<L>> extends ValueToCellNodeFactory<LabelingValue<L>> {

    private static SettingsModelBoolean createAdjustDimModel() {
        return new SettingsModelBoolean("cfg_adjust_dimensionality", false);
    }

    private static SettingsModelSubsetSelection createSubsetSelectionModel() {
        return new SettingsModelSubsetSelection("subset_selection");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<LabelingValue<L>> createNodeDialog() {
        return new ValueToCellNodeDialog<LabelingValue<L>>() {

            @Override
            public void addDialogComponents() {

                addDialogComponent("Options", "Subset", new DialogComponentSubsetSelection(
                        createSubsetSelectionModel(), true, true));

                addDialogComponent("Options", "Options", new DialogComponentBoolean(createAdjustDimModel(),
                        "Adjust dimensionality?"));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<LabelingValue<L>, LabelingCell<L>> createNodeModel() {
        return new ValueToCellNodeModel<LabelingValue<L>, LabelingCell<L>>() {

            private LabelingCellFactory m_labCellFactory;

            private final SettingsModelBoolean m_smAdjustDimensionality = createAdjustDimModel();

            private final SettingsModelSubsetSelection m_subsetSel = createSubsetSelectionModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_subsetSel);
                settingsModels.add(m_smAdjustDimensionality);

            }

            @Override
            protected LabelingCell<L> compute(final LabelingValue<L> cellValue) throws Exception {
                Labeling<L> res;

                if (m_subsetSel.isCompletelySelected()) {
                    return m_labCellFactory.createCell(cellValue.getLabeling().copy(), cellValue.getLabelingMetadata());
                } else {

                    final Labeling<L> lab = cellValue.getLabeling();

                    final long[] dimensions = new long[lab.numDimensions()];
                    lab.dimensions(dimensions);

                    final Interval[] intervals =
                            m_subsetSel.createSelectedIntervals(dimensions, cellValue.getLabelingMetadata());

                    @SuppressWarnings("unchecked")
                    final LabelingView<L>[] subLab = new LabelingView[intervals.length];

                    for (int i = 0; i < subLab.length; i++) {
                        subLab[i] = new LabelingView<L>(Views.offsetInterval(lab, intervals[i]), lab.<L> factory());
                    }

                    // TODO: Assumption here is that
                    // Labeling =
                    // NativeImgLabeling
                    @SuppressWarnings("unchecked")
                    final MergeLabelings<L> mergeOp =
                            new MergeLabelings<L>(((NativeImgLabeling<L, ? extends IntegerType<?>>)lab).getStorageImg()
                                    .firstElement().createVariable(), m_smAdjustDimensionality.getBooleanValue());

                    res = Operations.compute(mergeOp, subLab);

                    final List<CalibratedAxis> validAxes = new ArrayList<CalibratedAxis>();
                    for (int d = 0; d < lab.numDimensions(); d++) {
                        if (!mergeOp.getInvalidDims().contains(d)) {
                            validAxes.add(new DefaultCalibratedAxis(Axes.get(cellValue.getLabelingMetadata().axis(d)
                                    .toString())));
                        }

                    }

                    return m_labCellFactory.createCell(res, cellValue.getLabelingMetadata());
                }
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
