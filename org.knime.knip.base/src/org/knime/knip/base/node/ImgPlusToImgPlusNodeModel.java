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

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.util.MinimaUtils;

import net.imagej.ImgPlus;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.numeric.RealType;

/**
 * {@link NodeModel} to map from one {@link ImgPlus} to another {@link ImgPlus} rowwise
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <T> type of the incoming {@link ImgPlus}
 * @param <V> type of the outgoing {@link ImgPlus}
 */
public abstract class ImgPlusToImgPlusNodeModel<T extends RealType<T>, V extends RealType<V>>
        extends ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<V>> {

    /**
     * Global KNIME Logger
     */
    protected final static NodeLogger LOGGER = NodeLogger.getLogger(ImgPlusToImgPlusNodeModel.class);

    /**
     * Create the {@link SettingsModelDimSelection}
     *
     * @param axes default axes
     * @return the {@link SettingsModelDimSelection}
     */
    protected static SettingsModelDimSelection createDimSelectionModel(final String... axes) {
        return new SettingsModelDimSelection("dim_selection", axes);
    }

    /**
     * {@link SettingsModelDimSelection} to store dimension selection
     */
    protected SettingsModelDimSelection m_dimSelection;

    /**
     * {@link ImgPlusCellFactory} used to create {@link ImgPlusCell}
     */
    protected ImgPlusCellFactory m_imgCellFactory;

    /**
     * if true, parallelization is active
     */
    private boolean m_active = true;

    /**
     * Constructor
     *
     * @param isEnabled
     * @param axes
     */
    protected ImgPlusToImgPlusNodeModel(final boolean isEnabled, final String... axes) {
        m_dimSelection = createDimSelectionModel(axes);
        m_dimSelection.setEnabled(isEnabled);
    }

    /**
     * Constructor
     *
     * @param axes default axes
     */
    protected ImgPlusToImgPlusNodeModel(final String... axes) {
        this(true, axes);
    }

    /**
     * Will be removed in KNIP 2.0.0
     *
     * @param model
     */
    @Deprecated
    protected ImgPlusToImgPlusNodeModel(final SettingsModelDimSelection model) {
        this(model, true);
    }

    /**
     * Will be removed in KNIP 2.0.0
     *
     * @param model
     * @param isEnabled
     */
    @Deprecated
    protected ImgPlusToImgPlusNodeModel(final SettingsModelDimSelection model, final boolean isEnabled) {
        m_dimSelection = model;
        m_dimSelection.setEnabled(isEnabled);
    }

    @Override
    protected void collectSettingsModels() {
        super.collectSettingsModels();
        m_settingsModels.add(m_dimSelection);
    }

    @Override
    protected ImgPlusCell<V> compute(final ImgPlusValue<T> cellValue) throws Exception {

        if (!m_dimSelection.isContainedIn(cellValue.getMetadata())) {
            LOGGER.warn("image " + cellValue.getMetadata().getName() + " does not provide all selected dimensions.");
        }

        if (m_dimSelection.getNumSelectedDimLabels(cellValue.getMetadata()) < getMinDimensions()) {
            throw new KNIPException("not enough selected dimensions provided by image.");
        }

        final ImgPlus<T> imgPlus = MinimaUtils.getZeroMinImgPlus(cellValue.getImgPlus());
        final UnaryOutputOperation<ImgPlus<T>, ImgPlus<V>> op = op(imgPlus);

        final int[] selection = m_dimSelection.getSelectedDimIndices(cellValue.getMetadata());

        final long[] inMin = new long[imgPlus.numDimensions()];
        cellValue.getImgPlus().min(inMin);

        final ImgPlus<V> out = op.bufferFactory().instantiate(imgPlus);

        return m_imgCellFactory.createCell(MinimaUtils.getTranslatedImgPlus(imgPlus, SubsetOperations
                .iterate(op, selection, imgPlus, out, m_active ? getExecutorService() : null)));
    }

    /**
     * Create {@link UnaryOutputOperation} to map from one {@link ImgPlus} to another {@link ImgPlus}.
     *
     * @param imgPlus The incoming {@link ImgPlus}
     * @return {@link UnaryOutputOperation} which will be used to map from {@link ImgPlus} to another {@link ImgPlus}
     */
    protected abstract UnaryOutputOperation<ImgPlus<T>, ImgPlus<V>> op(ImgPlus<T> imgPlus);

    /**
     * Enable/Disable plane-wise or rather hypercube-wise parallelization. NB: Disable parallelization, if the
     * {@link UnaryOutputOperation} provided by the implementation is able to parallelize
     *
     * @param active
     */
    protected void enableParallelization(final boolean active) {
        this.m_active = active;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepareExecute(final ExecutionContext exec) {
        m_imgCellFactory = new ImgPlusCellFactory(exec);
    }

    /**
     * @return number of dimensions which an {@link ImgPlus} must provide to run this node
     */
    protected abstract int getMinDimensions();

}
