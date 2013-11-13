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
 * ---------------------------------------------------------------------
 *
 * Created on 13.11.2013 by Christian Dietz
 */
package org.knime.knip.base.node;

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.labeling.Labeling;
import net.imglib2.meta.ImgPlus;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.core.util.ImgUtils;

/**
 *
 * @author Christian Dietz
 */
public abstract class IterableIntervalsNodeModel<T extends RealType<T>, V extends RealType<V>, L extends Comparable<L>>
        extends ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<V>> {

    /*
     * Stores the first selected column.
     */
    private final SettingsModelString m_optionalColumn = createOptionalColumnModel();

    private int m_optionalColIdx;

    @SuppressWarnings("rawtypes")
    private Labeling<L> m_currentLabeling;

    private ImgPlusCellFactory m_cellFactory;

    static SettingsModelString createOptionalColumnModel() {
        return new SettingsModelString("optinal_column_selection", "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        m_optionalColIdx = getOptionalColumnIdx(((BufferedDataTable)inObjects[0]).getDataTableSpec());
        return super.execute(inObjects, exec);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void computeDataRow(final DataRow row) {
        if (m_optionalColIdx != -1) {
            m_currentLabeling = ((LabelingValue<L>)row.getCell(m_optionalColIdx)).getLabeling();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepareExecute(final ExecutionContext exec) {
        m_cellFactory = new ImgPlusCellFactory(exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void collectSettingsModels() {
        super.collectSettingsModels();
        m_settingsModels.add(m_optionalColumn);
    }

    /**
     * Get the optional column index.
     *
     * @param inSpec spec of the table
     * @return returns -1 if no column is selected.
     *
     * @throws InvalidSettingsException
     */
    protected int getOptionalColumnIdx(final DataTableSpec inSpec) throws InvalidSettingsException {

        int optionalColIdx = -1;
        if (m_optionalColumn.getStringValue() != null && !m_optionalColumn.getStringValue().equalsIgnoreCase("")) {
            optionalColIdx =
                    NodeTools.autoColumnSelection(inSpec, m_optionalColumn, LabelingValue.class, this.getClass());
        }

        return optionalColIdx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImgPlusCell<V> compute(final ImgPlusValue<T> cellValue) throws Exception {

        ImgPlus<T> in = cellValue.getImgPlus();
        ImgPlus<V> res = createResultImage(cellValue.getImgPlus());

        if (m_currentLabeling == null) {
            compute(in.getImg(), res.getImg(), in.getImg());
            return m_cellFactory.createCell(res);
        } else {
            for (L label : m_currentLabeling.getLabels()) {
                IterableRegionOfInterest iterableRegionOfInterest =
                        m_currentLabeling.getIterableRegionOfInterest(label);
                compute(iterableRegionOfInterest.getIterableIntervalOverROI(in),
                        iterableRegionOfInterest.getIterableIntervalOverROI(res), in.getImg());
            }

        }
    }

    /**
     * This method can be overriden. If you do so, make sure, that you also copy the metadata of the incoming
     * {@link ImgPlus} into the resulting {@link ImgPlus}.
     *
     * This methods assumes that the incoming {@link ImgPlus} has the same dimensionality as the outgoing
     * {@link ImgPlus}
     *
     * @return
     */
    private ImgPlus<V> createResultImage(final ImgPlus<T> in) {
        return new ImgPlus<V>(ImgUtils.createEmptyCopy(in, getOutType()), in);
    }

    /**
     * Compute the result and write it into out. You may use the src {@link RandomAccessible} to access
     * {@link Neighborhood}s, but you need to extend it on your own, to make sure that it is defined at any location.
     *
     * @param in incoming {@link IterableInterval}
     * @param out {@link IterableInterval} containing the result of the computation
     */
    public abstract void compute(final IterableInterval<T> in, final IterableInterval<V> out,
                                 final RandomAccessible<T> src);

    /**
     * The {@link RealType} of the outgoing {@link IterableInterval}.
     *
     * @return
     */
    public abstract V getOutType();
}