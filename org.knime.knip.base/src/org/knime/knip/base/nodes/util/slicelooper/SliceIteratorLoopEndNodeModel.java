/*
 *  Copyright (C) 2003 - 2014
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
 * Created on 11.03.2013 by dietyc
 */
package org.knime.knip.base.nodes.util.slicelooper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingFactory;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.meta.MetadataUtil;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;

/**
 *
 * @author Andreas Graumann, University of Konstanz
 * @author Christian Dietz, University of Konstanz
 *
 * @param <T>
 * @param <L>
 */
public class SliceIteratorLoopEndNodeModel<T extends RealType<T> & NativeType<T>, L extends Comparable<L>> extends
        NodeModel implements LoopEndNode, BufferedDataTableHolder {

    /**
     * List to store all incoming cells
     */
    private HashMap<Integer, ArrayList<DataValue>> m_cells = null;

    /**
     * current iterator
     */
    private CloseableRowIterator m_iterator = null;

    /**
     * actual row of iterator
     */
    private DataRow m_currentRow = null;

    /**
     * Container for results
     */
    private BufferedDataContainer m_resultContainer = null;

    /**
     * Factory for ImgPlusCells
     */
    private ImgPlusCellFactory m_imgPlusCellFactory = null;

    /**
     * Factory for LabelingCells
     */
    private LabelingCellFactory m_labelingCellFactory = null;

    /**
     * counting number of output rows
     */
    private int m_count = -1;

    private BufferedDataTable m_data;

    /**
     * Node Logger
     */
    private static NodeLogger LOGGER = NodeLogger.getLogger(SliceIteratorLoopEndNodeModel.class);

    /**
     * Default Constructor
     */
    protected SliceIteratorLoopEndNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        //reset();
        return new DataTableSpec[]{SliceIteratorUtils.createResSpec(inSpecs[0], null, LOGGER)};
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        // do we have a valid start node?
        if (!(getLoopStartNode() instanceof SliceIteratorLoopStartNodeModel)) {
            throw new IllegalStateException("End node without correct start slice loop node!");
        }

        // get loop start node
        final SliceIteratorLoopStartNodeModel<T, L> loopStartNode =
                (SliceIteratorLoopStartNodeModel<T, L>)getLoopStartNode();

        // check input spec
        final DataTableSpec inSpec = inData[0].getSpec();
        // create output spec
        final DataTableSpec outSpec = SliceIteratorUtils.createResSpec(inSpec, null, LOGGER);

        // input table
        final BufferedDataTable inTable = inData[0];

        // create output container if it does not exist
        if (m_resultContainer == null) {
            m_resultContainer = exec.createDataContainer(outSpec);
        }

        // get the iterator and create factories
        if (m_iterator == null) {
            m_iterator = inTable.iterator();

            m_imgPlusCellFactory = new ImgPlusCellFactory(exec);
            m_labelingCellFactory = new LabelingCellFactory(exec);
        }

        // create hashmap to store incoming cells
        if (m_cells == null) {
            m_cells = new HashMap<Integer, ArrayList<DataValue>>();
        }

        // loop over iterator
        while (m_iterator.hasNext()) {
            // get next row
            m_currentRow = m_iterator.next();
            // only run over selected and valid columns
            for (final int j : SliceIteratorUtils.getSelectedAndValidIdx(inSpec, null, LOGGER)) {
                // get right list from cell hashmap
                ArrayList<DataValue> list = m_cells.get(j);
                // create list if not already done
                if (list == null) {
                    m_cells.put(j, list = new ArrayList<DataValue>());
                }
                // add current cell
                list.add(m_currentRow.getCell(j));
            }
        }
        // all images of this iteration are stored
        // need for the next table
        m_iterator = null;

        // save all slices of an image,
        if (loopStartNode.isRowTerminated()) {

            // store all cells of this row
            final DataCell[] outCells = new DataCell[outSpec.getNumColumns()];

            // get all valid columns
            for (final int j : SliceIteratorUtils.getSelectedAndValidIdx(inSpec, null, LOGGER)) {

                // get value of current column
                final DataValue firstValue = m_cells.get(j).get(0);

                // get the reference interval for image merging
                final Interval[] refIntervals = loopStartNode.getAllIntervals().get(j);//loopStartNode.getRefIntervals();

                // we have an image
                if (firstValue instanceof ImgPlusValue) {

                    // get the image value
                    final ImgPlusValue<T> firstImgValue = (ImgPlusValue<T>)firstValue;

                    // get the image factory
                    final ImgFactory<T> fac = firstImgValue.getImgPlus().factory();

                    // create new output image
                    final Img<T> res =
                            fac.create(loopStartNode.getResDimensions(firstImgValue.getImgPlus(),j), firstImgValue
                                    .getImgPlus().firstElement().createVariable());

                    // copy all image slices in new created output image
                    int i = 0;
                    for (final DataValue value : m_cells.get(j)) {
                        final Img<T> outSlice = SliceIteratorUtils.getImgPlusAsView(res, refIntervals[i++], fac);
                        final ImgPlus<T> imgPlus = ((ImgPlusValue<T>)value).getImgPlus();
                        SliceIteratorUtils.copy(imgPlus, outSlice);
                    }

                    // create new Metadata with right dimensions
                    final ImgPlusMetadata outMetadata =
                            MetadataUtil.copyImgPlusMetadata(firstImgValue.getMetadata(),
                                                             new DefaultImgMetadata(res.numDimensions()));

                    // copy meta source
                    MetadataUtil.copySource(firstImgValue.getMetadata(), outMetadata);

                    // copy calibrated space
                    CalibratedSpace<CalibratedAxis> space = loopStartNode.getRefSpace();
                    MetadataUtil.copyTypedSpace(space, outMetadata);

                    // store created cell
                    outCells[j] = m_imgPlusCellFactory.createCell(res, outMetadata);

                } else {
                    // it must be a labeling

                    // get the labeling value
                    final LabelingValue<L> firstLabelingValue = (LabelingValue<L>)firstValue;

                    // get the labeling factory
                    final LabelingFactory<L> fac = firstLabelingValue.getLabeling().factory();

                    // create new labeling
                    final Labeling<L> res =
                            fac.create(loopStartNode.getResDimensions(firstLabelingValue.getLabeling(),j));

                    // copy all labeling slices in new created labeling
                    int i = 0;
                    for (final DataValue value : m_cells.get(j)) {
                        final Labeling<L> outSlice = SliceIteratorUtils.getLabelingAsView(res, refIntervals[i++], fac);
                        final Labeling<L> labeling = ((LabelingValue<L>)value).getLabeling();
                        SliceIteratorUtils.copy(labeling, outSlice);
                    }

                    // copy meta data with right dimensions
                    final LabelingMetadata outMetadata =
                            new DefaultLabelingMetadata(res.numDimensions(), firstLabelingValue.getLabelingMetadata()
                                    .getLabelingColorTable());

                    MetadataUtil.copyName(firstLabelingValue.getLabelingMetadata(), outMetadata);
                    MetadataUtil.copySource(firstLabelingValue.getLabelingMetadata(), outMetadata);

                    CalibratedSpace<CalibratedAxis> space = loopStartNode.getRefSpace();
                    MetadataUtil.copyTypedSpace(space, outMetadata);

                    // store created cell
                    outCells[j] = m_labelingCellFactory.createCell(res, outMetadata);
                }
            }

            // clear all cells
            m_cells.clear();
            m_cells = null;

            // write cells to row
            DataRow row = new DefaultRow("Row" + ++m_count, outCells);
            m_resultContainer.addRowToTable(row);

        }

        // finished
        if (loopStartNode.terminateLoop()) {
            m_resultContainer.close();
            m_data = m_resultContainer.getTable();
            return new BufferedDataTable[]{m_data};
        } else {
            // next iteration
            super.continueLoop();
            return new BufferedDataTable[1];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_cells = null;
        m_iterator = null;
        m_resultContainer = null;
        m_currentRow = null;
        m_imgPlusCellFactory = null;
        m_labelingCellFactory = null;
        m_count = -1;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do here
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do here
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // Nothing to do here
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // Nothing to do here
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_data};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_data = tables[0];
    }
}
