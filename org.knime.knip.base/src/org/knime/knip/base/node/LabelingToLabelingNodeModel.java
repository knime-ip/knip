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
package org.knime.knip.base.node;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.roi.labeling.LabelingType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeModel;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;

/**
 * Use this {@link NodeModel}, if you want to map one {@link Labeling} on another {@link Labeling} row-wise
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @param <L>
 * @param <M>
 */
public abstract class LabelingToLabelingNodeModel<L extends Comparable<L>, M extends Comparable<M>> extends
        ValueToCellNodeModel<LabelingValue<L>, LabelingCell<M>> {

    /**
     * Create {@link SettingsModelDimSelection}
     *
     * @param axes default axes of this model
     * @return the created {@link SettingsModelDimSelection}
     */
    protected static SettingsModelDimSelection createDimSelectionModel(final String... axes) {
        return new SettingsModelDimSelection("dim_selection", axes);
    }

    /**
     * {@link SettingsModelDimSelection} to store dimension selection
     */
    protected SettingsModelDimSelection m_dimSelection;

    /**
     * {@link LabelingCellFactory} of this {@link NodeModel}
     */
    private LabelingCellFactory m_labelingCellFactory;

    /**
     * Constructor to create {@link LabelingToLabelingNodeModel}
     *
     * @param isEnabled true, if dimension selection is enabled as default
     * @param axes default axes of dim selection
     */
    protected LabelingToLabelingNodeModel(final boolean isEnabled, final String... axes) {
        m_dimSelection = createDimSelectionModel(axes);
        m_dimSelection.setEnabled(isEnabled);
    }

    /**
     * Create {@link LabelingToLabelingNodeModel}
     *
     * @param axes Default axes to be set
     */
    protected LabelingToLabelingNodeModel(final String... axes) {
        this(true, axes);
    }

    /**
     * Create {@link LabelingToLabelingNodeModel}
     *
     * @param model the {@link SettingsModelDimSelection} which will be used
     */
    @Deprecated
    protected LabelingToLabelingNodeModel(final SettingsModelDimSelection model) {
        this(model, true);
    }

    /**
     * Helper constructor for backwards compatibility
     *
     * @param model
     * @param isEnabled
     */
    @Deprecated
    protected LabelingToLabelingNodeModel(final SettingsModelDimSelection model, final boolean isEnabled) {
        m_dimSelection = model;
        m_dimSelection.setEnabled(isEnabled);
    }

    @Override
    protected void collectSettingsModels() {
        super.collectSettingsModels();
        m_settingsModels.add(m_dimSelection);
    }

    @Override
    protected LabelingCell<M> compute(final LabelingValue<L> cellValue) throws Exception {

        final UnaryOutputOperation<RandomAccessibleInterval<LabelingType<L>>, RandomAccessibleInterval<LabelingType<M>>> op = op(cellValue.getLabeling());

        return m_labelingCellFactory.createCell(SubsetOperations.iterate(op, m_dimSelection
                .getSelectedDimIndices(cellValue.getLabelingMetadata()), cellValue.getLabeling(), op.bufferFactory()
                .instantiate(cellValue.getLabeling()), getExecutorService()), cellValue.getLabelingMetadata());
    }

    /**
     * Create {@link UnaryOutputOperation} to map from one {@link Labeling} to another {@link Labeling}.
     *
     * @param labeling The incoming {@link Labeling}
     * @return Operation which will be used to map from {@link Labeling} to another {@link Labeling}
     */
    protected abstract UnaryOutputOperation<RandomAccessibleInterval<LabelingType<L>>, RandomAccessibleInterval<LabelingType<M>>> op(RandomAccessibleInterval<LabelingType<L>> labeling);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepareExecute(final ExecutionContext exec) {
        m_labelingCellFactory = new LabelingCellFactory(exec);
    }

}
