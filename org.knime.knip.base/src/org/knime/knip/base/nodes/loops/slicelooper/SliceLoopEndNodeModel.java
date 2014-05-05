/*
 * ------------------------------------------------------------------------
 *
@   *  Copyright (C) 2003 - 2013
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
package org.knime.knip.base.nodes.loops.slicelooper;

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
 * @author dietzc, University of Konstanz
 * @param <T>
 * @param <L>
 */
public class SliceLoopEndNodeModel<T extends RealType<T> & NativeType<T>, L extends Comparable<L>> extends NodeModel
        implements LoopEndNode {

    private HashMap<Integer, ArrayList<DataValue>> m_cells = null;

    private CloseableRowIterator m_iterator = null;

    private DataRow m_currentRow = null;

    private BufferedDataContainer m_resultContainer = null;

    private ImgPlusCellFactory m_imgPlusCellFactory = null;

    private LabelingCellFactory m_labelingCellFactory = null;

    private int m_count = -1;

    private static NodeLogger LOGGER = NodeLogger.getLogger(SliceLoopEndNodeModel.class);

    /**
     * @param nrInDataPorts
     * @param nrOutDataPorts
     */
    protected SliceLoopEndNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        //reset();
        return new DataTableSpec[]{SliceLooperUtils.createResSpec(LOGGER, inSpecs[0])};
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        if (!(getLoopStartNode() instanceof SliceLoopStartNodeModel)) {
            //TODO more meaningful message
            throw new IllegalStateException("End node without correct start node!");
        }

        @SuppressWarnings("unchecked")
        final SliceLoopStartNodeModel<T, L> loopStartNode = (SliceLoopStartNodeModel<T, L>)getLoopStartNode();

        // check input spec
        final DataTableSpec inSpec = inData[0].getSpec();
        final DataTableSpec outSpec = SliceLooperUtils.createResSpec(LOGGER, inSpec);

        final BufferedDataTable inTable = inData[0];
        // prepare container for output
        //final BufferedDataContainer container = exec.createDataContainer(outSpec);

        if (m_resultContainer == null) {
            m_resultContainer = exec.createDataContainer(outSpec);
        }

        if (m_iterator == null) {
            m_iterator = inTable.iterator();
            if (m_iterator.hasNext()) {
                //m_currentRow = m_iterator.next();
            }

            m_imgPlusCellFactory = new ImgPlusCellFactory(exec);
            m_labelingCellFactory = new LabelingCellFactory(exec);

        }

        // hier nur durch valide spalten gehen. und auch nur, wenn eine row terminated wurde

        if (m_cells == null) {
            m_cells = new HashMap<Integer, ArrayList<DataValue>>();
        }

        while (m_iterator.hasNext()) {
            m_currentRow = m_iterator.next();
            for (final int j : SliceLooperUtils.getValidIdx(inSpec)) {

                ArrayList<DataValue> list = m_cells.get(j);
                if (list == null) {
                    m_cells.put(j, list = new ArrayList<DataValue>());
                }
                list.add(m_currentRow.getCell(j));
            }
        }
        m_iterator = null;

        // save all slices of an image,
        if (loopStartNode.isRowTerminated()) {

            final DataCell[] outCells = new DataCell[outSpec.getNumColumns()];

            for (final int j : SliceLooperUtils.getValidIdx(inSpec)) {

                final DataValue firstValue = m_cells.get(j).get(0);

                final Interval[] refIntervals = loopStartNode.getRefIntervals();

                if (firstValue instanceof ImgPlusValue) {
                    // it is an img

                    final ImgPlusValue<T> firstImgValue = (ImgPlusValue<T>)firstValue;

                    final ImgFactory<T> fac = firstImgValue.getImgPlus().factory();

                    final Img<T> res =
                            fac.create(loopStartNode.getResDimensions(firstImgValue.getImgPlus()), firstImgValue
                                    .getImgPlus().firstElement().createVariable());

                    int i = 0;
                    for (final DataValue value : m_cells.get(j)) {
                        final Img<T> outSlice = SliceLooperUtils.getImgPlusAsView(res, refIntervals[i++], fac);
                        final ImgPlus<T> imgPlus = ((ImgPlusValue<T>)value).getImgPlus();
                        SliceLooperUtils.copy(imgPlus, outSlice);
                    }

                    final ImgPlusMetadata outMetadata =
                            MetadataUtil.copyImgPlusMetadata(firstImgValue.getMetadata(),
                                                             new DefaultImgMetadata(res.numDimensions()));

                    MetadataUtil.copySource(firstImgValue.getMetadata(), outMetadata);
                    CalibratedSpace<CalibratedAxis> space = loopStartNode.getRefSpace();
                    MetadataUtil.copyTypedSpace(space, outMetadata);

                    outCells[j] = m_imgPlusCellFactory.createCell(res, outMetadata);

                } else {
                    // it must be a labeling
                    final LabelingValue<L> firstLabelingValue = (LabelingValue<L>)firstValue;
                    final LabelingFactory<L> fac = firstLabelingValue.getLabeling().factory();

                    final Labeling<L> res =
                            fac.create(loopStartNode.getResDimensions(firstLabelingValue.getLabeling()));

                    int i = 0;
                    for (final DataValue value : m_cells.get(j)) {
                        final Labeling<L> outSlice = SliceLooperUtils.getLabelingAsView(res, refIntervals[i++], fac);
                        final Labeling<L> labeling = ((LabelingValue<L>)value).getLabeling();
                        SliceLooperUtils.copy(labeling, outSlice);
                    }

                    final LabelingMetadata outMetadata =
                            new DefaultLabelingMetadata(res.numDimensions(), firstLabelingValue.getLabelingMetadata()
                                    .getLabelingColorTable());

                    MetadataUtil.copyName(firstLabelingValue.getLabelingMetadata(), outMetadata);
                    MetadataUtil.copySource(firstLabelingValue.getLabelingMetadata(), outMetadata);

                    CalibratedSpace<CalibratedAxis> space = loopStartNode.getRefSpace();
                    MetadataUtil.copyTypedSpace(space, outMetadata);

                    outCells[j] = m_labelingCellFactory.createCell(res, outMetadata);
                }
            }

            m_cells.clear();
            m_cells = null;

            // write cells to row
            DataRow row = new DefaultRow("Row" + ++m_count, outCells);
            m_resultContainer.addRowToTable(row);

        }

        if (loopStartNode.terminateLoop()) {
            //
            //            return alle zusammengesammelten Bildchen
            m_resultContainer.close();
            return new BufferedDataTable[]{m_resultContainer.getTable()};
        } else {
            super.continueLoop();
            return new BufferedDataTable[1];
        }
        //

        //  return null;
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

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do here
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do here
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
    }
}
