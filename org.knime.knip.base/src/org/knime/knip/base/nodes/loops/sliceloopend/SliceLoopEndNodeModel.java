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
 * Created on 11.03.2013 by dietyc
 */
package org.knime.knip.base.nodes.loops.sliceloopend;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.nodes.loops.sliceloopstart.SliceLoopStartNodeModel;

/**
 * @author dietzc, University of Konstanz
 */
public class SliceLoopEndNodeModel<T extends RealType<T>> extends NodeModel implements LoopEndNode {

    private HashMap<Integer, ArrayList<DataCell>> m_cells = null;

    private CloseableRowIterator m_iterator = null;

    private DataRow m_currentRow = null;

    private ImgPlusCellFactory m_imgPlusCellFactory = null;

    private LabelingCellFactory m_labelingCellFactory = null;


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
        return createOutSpec(inSpecs[0]);
    }

    /**
     * @param dataTableSpec
     * @return
     */
    private DataTableSpec[] createOutSpec(final DataTableSpec dataTableSpec) {
        DataTableSpec[] specs = new DataTableSpec[1];
        specs[0] = dataTableSpec;
        return specs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        if (!(getLoopStartNode() instanceof SliceLoopStartNodeModel)) {
            throw new IllegalStateException("End node without correct start node!");
        }

        final SliceLoopStartNodeModel loopStartNode = (SliceLoopStartNodeModel)getLoopStartNode();

        // check input spec
        DataTableSpec inSpec = inData[0].getSpec();
        DataTableSpec outSpec = createOutSpec(inSpec)[0];


        if (m_cells == null) {
            m_cells = new HashMap<Integer, ArrayList<DataCell>>();
        }

        final BufferedDataTable inTable = inData[0];
        // prepare container for output
        final BufferedDataContainer container = exec.createDataContainer(outSpec);


        if (m_iterator == null) {
            m_iterator = inTable.iterator();
            if (m_iterator.hasNext()) {
                m_currentRow = m_iterator.next();
            }

            m_imgPlusCellFactory = new ImgPlusCellFactory(exec);
            m_labelingCellFactory = new LabelingCellFactory(exec);
        }

        // collect resutls from inData and create image etc
        // using method getIterationIndices from loop start
        // loop over all columns
        for (int j = 0; j < inSpec.getNumColumns(); j++) {
            ArrayList<DataCell> list = m_cells.get(j);
            if (list == null) {
                list = new ArrayList<DataCell>();
            }
            list.add(m_currentRow.getCell(j));
            m_cells.remove(j);
            m_cells.put(j, list);
        }

        // save all slices of an image,

        if (loopStartNode.terminateImg()) {

            ArrayList<DataCell> outCells = new ArrayList<DataCell>();

            for (int j = 0; j < inSpec.getNumColumns(); j++) {

                // get type: image/labeling
                DataColumnSpec colSpec = inSpec.getColumnSpec(0);
                DataType colType = colSpec.getType();

                if (colType.isCompatible(ImgPlusValue.class)) {

                    ArrayList<DataCell> list = m_cells.get(j);

                    ImgPlus<T> tmp = ((ImgPlusValue<T>)list.get(0)).getImgPlus();
                    long[] dim = new long[tmp.numDimensions()];
                    tmp.dimensions(dim);
                    long[] dimNew = new long[tmp.numDimensions()+1];

                    for (int i = 0; i < tmp.numDimensions(); i++) {
                        dimNew[i] = dim[i];
                    }

                    dimNew[tmp.numDimensions()] = list.size();

                    Img<T> resultingImageTmp = tmp.factory().create(dimNew, tmp.firstElement());
                    ImgPlus<T> resultingImage = new ImgPlus<T>(resultingImageTmp);
                    RandomAccess<T> ra = resultingImage.randomAccess();

                    for (int i = 0; i < list.size(); i++) {
                        ImgPlus<T> img = ((ImgPlusValue<T>)list.get(i)).getImgPlus();

                        Cursor<T> it = img.cursor();
                        while (it.hasNext()) {
                            it.next();
                            int[] p = new int[it.numDimensions()];
                            it.localize(p);

                            int[] pos = new int[it.numDimensions()+1];
                            for (int d = 0; d < it.numDimensions(); d++) {
                                pos[d] = p[d];
                            }
                            pos[it.numDimensions()] = i;

                            ra.setPosition(pos);
                            ra.get().set(it.get());
                        }
                    }

                    // create DataCell
                    DataCell cell =
                            m_imgPlusCellFactory.createCell(resultingImage);
                    outCells.add(cell);
                }
                else if (colType.isCompatible(LabelingValue.class)) {

                }
            }

            // write cells to row
            DataRow row = new DefaultRow("Slice ", outCells.toArray(new DataCell[outCells.size()]));
            container.addRowToTable(row);
        }
        else {
            m_iterator.next();
        }

        if (loopStartNode.terminateLoop()) {
            //
            //            return alle zusammengesammelten Bildchen
            container.close();
            return new BufferedDataTable[]{container.getTable()};
        } else {
            super.continueLoop();
        }
        //

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_cells = null;

        m_iterator = null;

        m_currentRow = null;

        m_imgPlusCellFactory = null;

        m_labelingCellFactory = null;
        m_count = 0;
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
