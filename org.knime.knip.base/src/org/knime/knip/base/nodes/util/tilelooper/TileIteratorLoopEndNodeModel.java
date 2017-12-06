/*
 *  Copyright (C) 2003 - 2017
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
 * Created on 27 Sep 2017 by Benjamin Wilhelm
 */
package org.knime.knip.base.nodes.util.tilelooper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.scijava.Named;

import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.ops.util.MetadataUtil;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;

/**
 * Node model for the tile loop end node.
 *
 * @param <T> Type of the image values.
 *
 * @author Benjamin Wilhelm, MPI-CBG, Dresden
 */
public class TileIteratorLoopEndNodeModel<T extends RealType<T>, L extends Comparable<L>> extends NodeModel
        implements LoopEndNode, BufferedDataTableHolder {

    private static final String IMG_COLUMN_CONF_KEY = "img_column_key";

    // ---------------------------------------------- Loop helper stuff -------------------------------

    private BufferedDataContainer m_resultContainer;

    private TileIteratorLoopStartNodeModel<T, L> m_loopStartNode;

    // ---------------------------------------------- Misc --------------------------------------------

    private final SettingsModelString m_columnSelection = createImgColumnModel();

    private BufferedDataTable m_dataTable;

    private ImgPlusCellFactory m_imgCellFactory;

    private LabelingCellFactory m_labelingCellFactory;

    /**
     * Creates a new node model for the tile loop end node.
     */
    protected TileIteratorLoopEndNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{TileIteratorUtils.createOutSpecs(inSpecs[0], m_columnSelection, this.getClass())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        final BufferedDataTable table = inData[0];

        // do we have a valid start node?
        if (!(getLoopStartNode() instanceof TileIteratorLoopStartNodeModel)) {
            throw new IllegalStateException("End node without correct start tile loop node!");
        }

        // get loop start node
        m_loopStartNode = (TileIteratorLoopStartNodeModel<T, L>)getLoopStartNode();

        // create output container if it does not exist
        if (m_resultContainer == null) {
            m_resultContainer = exec.createDataContainer(TileIteratorUtils
                    .createOutSpecs(table.getDataTableSpec(), m_columnSelection, this.getClass()));
        }

        // get the iterator
        final CloseableRowIterator iterator = table.iterator();

        // check if table is not empty,
        // if it is empty we are finished here
        if (!iterator.hasNext()) {
            m_resultContainer.close();
            m_dataTable = m_resultContainer.getTable();
            return new BufferedDataTable[]{m_dataTable};
        }

        // Create the cell factories
        m_imgCellFactory = new ImgPlusCellFactory(exec);
        m_labelingCellFactory = new LabelingCellFactory(exec);

        // Get the current grid at the start node (We may need to add or remove singleton dimensions)
        final long[] startGrid = m_loopStartNode.getCurrentGrid();
        // Get the overlap and make it negative in order to remove it
        final long[] startOverlap = m_loopStartNode.getCurrentOverlap();
        // Get the image size
        final long[] startImgSize = m_loopStartNode.getCurrentImgSize();

        // List for all tiles per column
        List<TileIteratorLoopMerger<?>> columnsTiles = new ArrayList<>();
        // List for the metadata per column
        List<Named> metadatas = new ArrayList<>();
        // List for factories per column
        List<ImgFactory<T>> factories = new ArrayList<>();

        // Get row key from loop start node
        String rowKey = m_loopStartNode.getCurrentRowKey();

        // Currently only one column is allowed
        final int tilesIndex =
                TileIteratorUtils.getSelectedColumnIndex(table.getDataTableSpec(), m_columnSelection, this.getClass());
        columnsTiles.add(new TileIteratorLoopImageMerger<>(startGrid, startOverlap, startImgSize));

        // Add RAI and Metadata to Lists
        table.forEach((row) -> {
            boolean saveMetadata = metadatas.isEmpty();
            DataCell cell = row.getCell(tilesIndex);
            if (cell instanceof ImgPlusValue) {
                ImgPlusValue<T> imgPlusCell = (ImgPlusValue<T>)cell;
                columnsTiles.get(0).addTile((RandomAccessibleInterval)imgPlusCell.getImgPlus());
                if (saveMetadata) {
                    metadatas.add(imgPlusCell.getMetadata());
                    factories.add(imgPlusCell.getImgPlus().factory());
                }
            } else if (cell instanceof LabelingValue) {
                LabelingValue<LabelingType<L>> labelingCell = (LabelingValue<LabelingType<L>>)cell;
                columnsTiles.get(0).addTile((RandomAccessibleInterval)labelingCell.getLabeling());
                if (saveMetadata) {
                    metadatas.add(labelingCell.getLabelingMetadata());
                    factories.add(null);
                }
            } else {
                System.out.println("No imgplus or labeling");
                // TODO what happens now?
            }
        });

        // Loop over lists of tiles and merge the tiles
        for (int i = 0; i < columnsTiles.size(); i++) {
            DataCell outCell = null;
            if (!columnsTiles.get(i).isEmpty()) {
                RandomAccessibleInterval<?> merged = columnsTiles.get(i).mergeTiles();

                if (metadatas.get(i) instanceof ImgPlusMetadata) {
                    // Column contains images
                    ImgPlusMetadata tileMetadata = (ImgPlusMetadata)metadatas.get(i);
                    ImgPlusMetadata metadata = MetadataUtil
                            .copyImgPlusMetadata(tileMetadata, new DefaultImgMetadata(tileMetadata.numDimensions()));
                    ImgPlus<T> resImg = new ImgPlus<>(
                            ImgView.wrap((RandomAccessibleInterval<T>)merged, factories.get(i)), metadata);
                    resImg.setSource(metadata.getSource());

                    // Create cell
                    outCell = m_imgCellFactory.createCell(resImg);
                } else if (metadatas.get(i) instanceof LabelingMetadata) {
                    // Column contains labelings
                    // TODO
                    throw new IllegalStateException("Labelings are not implemented yet!");
                }
            } else {
                outCell = new MissingCell(null);
            }

            if (outCell != null) {
                // Add to table
                m_resultContainer.addRowToTable(new DefaultRow(rowKey, outCell));
            }
        }

        // finished
        if (m_loopStartNode.terminateLoop()) {
            m_resultContainer.close();
            m_dataTable = m_resultContainer.getTable();
            return new BufferedDataTable[]{m_dataTable};
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
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_dataTable};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_dataTable = tables[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Nothing to do

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Nothing to do

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnSelection.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnSelection.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnSelection.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_resultContainer = null;
        m_dataTable = null;
        m_imgCellFactory = null;

    }

    // ----------------- Static methods for settings models -------------------------

    /**
     * @return Model to store the Img Column.
     */
    static SettingsModelString createImgColumnModel() {
        return new SettingsModelString(IMG_COLUMN_CONF_KEY, "");
    }
}
