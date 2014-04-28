/*
 * ------------------------------------------------------------------------
 *
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
 */
package org.knime.knip.base.nodes.loops.sliceloopstart;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingFactory;
import net.imglib2.labeling.LabelingView;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.meta.MetadataUtil;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.type.NativeType;
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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;

/**
 *
 * @author Andreas Graumann, University of Konstanz
 * @author Christian Dietz, University of Konstanz
 */
public class SliceLoopStartNodeModel<T extends RealType<T> & NativeType<T>, L extends Comparable<L>> extends NodeModel
        implements LoopStartNodeTerminator {

    /**
     * @param nrInDataPorts
     * @param nrOutDataPorts
     */
    protected SliceLoopStartNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts);
    }

    private SettingsModelDimSelection m_dimSelection = createDimSelection();

    private SettingsModelInteger m_chunkSize = createChunkSizeModel();

    private ImgPlus<T> m_currImageInCol = null;

    private Labeling<L> m_currLabeling = null;

    private int m_imgIndex = -1;

    private int m_currIdx = -1;

    private CloseableRowIterator m_iterator;

    private Interval[] m_intervals;

    private boolean imgTerminated;

    private boolean isAllTerminated;

    private ImgPlusCellFactory m_imgPlusCellFactory;

    private LabelingCellFactory m_labelingCellFactory;

    private DataRow m_currentRow;

    static NodeLogger LOGGER = NodeLogger.getLogger(SliceLoopStartNodeModel.class);

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // we just have one output
        DataTableSpec[] specs = new DataTableSpec[1];
        specs[0] = createResSpec(inSpecs[0]);

        reset();
        return specs;
    }

    /**
     * @return
     */
    protected static SettingsModelInteger createChunkSizeModel() {
        return new SettingsModelInteger("chunk_size", 1);
    }

    /**
     * @return
     */
    protected static SettingsModelDimSelection createDimSelection() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    /**
     * @return
     */
    private DataTableSpec createResSpec(final DataTableSpec inSpec) {
        int numCol = inSpec.getNumColumns();

        ArrayList<String> names = new ArrayList<String>();
        ArrayList<DataType> types = new ArrayList<DataType>();

        for (int i = 0; i < numCol; i++) {
            DataColumnSpec colSpec = inSpec.getColumnSpec(i);

            DataType colType = colSpec.getType();
            if (!colType.isCompatible(ImgPlusValue.class) && !colType.isCompatible(LabelingValue.class)) {
                LOGGER.warn("Waning: Column " + colSpec.getName()
                        + " will be ignored! Only ImgPlusValue and LabelingValue allowed!");
            } else {
                names.add(colSpec.getName());
                types.add(colType);
            }
        }

        return new DataTableSpec(names.toArray(new String[names.size()]), types.toArray(new DataType[types.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        // check input spec
        DataTableSpec inSpec = inData[0].getSpec();
        DataTableSpec outSpec = createResSpec(inSpec);

        // no iterator, yet!
        if (m_iterator == null) {
            final BufferedDataTable inTable = inData[0];
            m_iterator = inTable.iterator();
            if (m_iterator.hasNext()) {
                m_currentRow = m_iterator.next();
            }
            m_imgPlusCellFactory = new ImgPlusCellFactory(exec);
            m_labelingCellFactory = new LabelingCellFactory(exec);
        }

        // no current image/label
        if (m_currImageInCol == null || m_currLabeling == null) {


            DataColumnSpec colSpec = inSpec.getColumnSpec(0);
            DataType colType = colSpec.getType();

            // Input column is of type ImgPlusValue!
            if (colType.isCompatible(ImgPlusValue.class)) {

                m_currImageInCol = ((ImgPlusValue<T>)m_currentRow.getCell(0)).getImgPlus();
            }
            else if (colType.isCompatible(LabelingValue.class)) {

                m_currLabeling = ((LabelingValue<L>)m_currentRow.getCell(0)).getLabeling();
            }
        }

        if (m_intervals == null) {
            DataColumnSpec colSpec = inSpec.getColumnSpec(0);
            DataType colType = colSpec.getType();

            // Input column is of type ImgPlusValue!
            if (colType.isCompatible(ImgPlusValue.class)) {

                m_intervals = m_dimSelection.getIntervals(m_currImageInCol, m_currImageInCol);
            }
            else if (colType.isCompatible(LabelingValue.class)) {

                LabelingMetadata meta =  ((LabelingValue<L>)m_currentRow.getCell(0)).getLabelingMetadata();
                m_intervals = m_dimSelection.getIntervals(meta, ((LabelingValue<L>)m_currentRow.getCell(0)).getLabeling());
            }
            m_currIdx = 0;
        }

        // prepare container for output
        final BufferedDataContainer container = exec.createDataContainer(outSpec);

        // create view with slices, depending on chunk size
        for (int i = m_currIdx; i < (Math.min(m_currIdx + m_chunkSize.getIntValue(), m_intervals.length)); i++) {

            ArrayList<DataCell> cells = new ArrayList<DataCell>();

            // loop over all columns
            for (int j = 0; j < inSpec.getNumColumns(); j++) {

                DataColumnSpec colSpec = inSpec.getColumnSpec(j);
                DataType colType = colSpec.getType();

                // Input column is of type ImgPlusValue!
                if (colType.isCompatible(ImgPlusValue.class)) {

                    // get image in cell
                    m_currImageInCol = ((ImgPlusValue<T>)m_currentRow.getCell(j)).getImgPlus();

                    // get intervals of image
                    m_intervals = m_dimSelection.getIntervals(m_currImageInCol, m_currImageInCol);

                    // get meta data
                    ImgPlusMetadata oldMetaData = ((ImgPlusValue<T>)m_currentRow.getCell(j)).getMetadata();

                    // create new meta data
                    ImgPlusMetadata newMetaData = new DefaultImgMetadata(oldMetaData.numDimensions()-1);
                    newMetaData.setName(oldMetaData.getName());
                    newMetaData.setSource(oldMetaData.getSource());
                    MetadataUtil.copyAndCleanTypedSpace(m_intervals[i], oldMetaData, newMetaData);

                    // create DataCell
                    DataCell cell =
                            m_imgPlusCellFactory.createCell(getImgPlusAsView(m_currImageInCol, m_intervals[i],
                                                                             m_currImageInCol.factory()), newMetaData);
                    cells.add(cell);
                }
                // Input column is of type LabelingValue
                else if (colType.isCompatible(LabelingValue.class)) {

                    // get labeling in cell
                    m_currLabeling = ((LabelingValue<L>)m_currentRow.getCell(j)).getLabeling();

                    // meta data
                    LabelingMetadata oldMetaData = ((LabelingValue<L>)m_currentRow.getCell(j)).getLabelingMetadata();

                    // get intervals of image
                    m_intervals = m_dimSelection.getIntervals(oldMetaData, ((LabelingValue<L>)m_currentRow.getCell(j)).getLabeling());

                    LabelingMetadata newMetaData = new DefaultLabelingMetadata(oldMetaData.numDimensions()-1, oldMetaData.getLabelingColorTable());
                    newMetaData.setName(oldMetaData.getName());
                    newMetaData.setSource(oldMetaData.getSource());
                    MetadataUtil.copyAndCleanTypedSpace(m_intervals[i], oldMetaData, newMetaData);
                    // create DataCell
                    DataCell cell = m_labelingCellFactory.createCell(getLabelingAsView(m_currLabeling, m_intervals[i],
                                                                               m_currLabeling.factory()), newMetaData);

                    cells.add(cell);
                }
            }

            // write cells to row
            DataRow row = new DefaultRow("Slice " + i, cells.toArray(new DataCell[cells.size()]));
            container.addRowToTable(row);
        }

        // new current index
        m_currIdx = (Math.min(m_currIdx + m_chunkSize.getIntValue(), m_intervals.length));

        // are we finished?
        if (m_currIdx == m_intervals.length) {
            imgTerminated = true;
            isAllTerminated = !m_iterator.hasNext();

            if (!isAllTerminated) {
                m_currImageInCol = null;
                m_intervals = null;
                m_currentRow = m_iterator.next();
            }
        }

        //}
        container.close();

        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_currIdx = 0;
        m_currImageInCol = null;
        m_currLabeling = null;
        m_intervals = null;
        imgTerminated = false;
        isAllTerminated = false;
        m_iterator = null;
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
        m_dimSelection.saveSettingsTo(settings);
        m_chunkSize.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dimSelection.validateSettings(settings);
        m_chunkSize.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dimSelection.loadSettingsFrom(settings);
        m_chunkSize.loadSettingsFrom(settings);
    }

    public boolean terminateImg() {
        return imgTerminated;
    }

    @Override
    public boolean terminateLoop() {
        return isAllTerminated;
    }

    private Img<T> getImgPlusAsView(final Img<T> in, final Interval i, final ImgFactory<T> fac) {
        return new ImgView<T>(SubsetOperations.subsetview(in, i), fac);
    }

    private Labeling<L> getLabelingAsView(final Labeling<L> in, final Interval i, final LabelingFactory fac) {
        return new LabelingView<L>(SubsetOperations.subsetview(in, i), fac);
    }
}
