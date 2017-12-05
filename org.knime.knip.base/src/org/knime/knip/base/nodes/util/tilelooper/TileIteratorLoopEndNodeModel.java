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
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
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
import org.knime.knip.base.nodes.util.tilelooper.imglib2.ArrangedView;
import org.knime.knip.base.nodes.util.tilelooper.imglib2.CombinedView;
import org.knime.knip.core.data.img.DefaultImgMetadata;

import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imagej.ops.MetadataUtil;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

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

    // ---------------------------------------------- Misc --------------------------------------------

    private final SettingsModelString m_columnSelection = createImgColumnModel();

    private BufferedDataTable m_dataTable;

    private ImgPlusCellFactory m_cellFactory;

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
        @SuppressWarnings("unchecked")
        final TileIteratorLoopStartNodeModel<T,L> loopStartNode = (TileIteratorLoopStartNodeModel<T,L>)getLoopStartNode();

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

        // Create the cell factory
        m_cellFactory = new ImgPlusCellFactory(exec);

        // Get the current grid at the start node (We may need to add or remove singleton dimensions)
        final long[] startGrid = loopStartNode.getCurrentGrid();

        // Get the overlap and make it negative in order to remove it
        final long[] startOverlap = loopStartNode.getCurrentOverlap();

        // Get the image size
        final long[] startImgSize = loopStartNode.getCurrentImgSize();


        // Stuff to initialize
        ImgPlusMetadata tileMetadata = null;
        ImgFactory<T> imgFactory = null;
        String rowKey = null;
        long[] grid = null;
        long[] negOverlap = null;
        long[] imgSize = null;

        // Loop over the tiles and store them in a list
        final List<RandomAccessibleInterval<T>> tiles = new ArrayList<>();
        final int tilesIndex =
                TileIteratorUtils.getSelectedColumnIndex(table.getDataTableSpec(), m_columnSelection, this.getClass());
        while (iterator.hasNext()) {
            // Get row
            final DataRow dataRow = iterator.next();

            // Get the row key
            if (rowKey == null) {
                final String key = dataRow.getKey().getString();
                final int splitPoint = key.lastIndexOf(TileIteratorUtils.ROW_KEY_DELIMITER);
                rowKey = key.substring(0, splitPoint);
            }

            // Get the cell
            final DataCell cell = dataRow.getCell(tilesIndex);

            // Check if it is an image and process it if it is
            if (cell instanceof ImgPlusValue) {
                @SuppressWarnings("unchecked") // We check before that our column contains images
                final ImgPlusValue<T> imgVal = (ImgPlusValue<T>)cell;
                final ImgPlus<T> img = imgVal.getImgPlus();

                // Fix dimensions if this is the first tile:
                // The user is allowed to remove dimensions in the end where the grid is 1 and overlap is 0.
                // The user is allowed to add dimensions to the end.
                if (grid == null) {
                    // Number of dimensions at the start node
                    final int startN = startImgSize.length;
                    final int newN = img.numDimensions();

                    if (newN > startN) {
                        // If we have more dimensions now, add singleton dimensions to the grid
                        // and zeros to the overlap
                        grid = IntStream.range(0, newN).mapToLong(i -> i < startN ? startGrid[i] : 1).toArray();
                        negOverlap =
                                IntStream.range(0, newN).mapToLong(i -> i < startN ? -startOverlap[i] : 0).toArray();
                        imgSize = IntStream.range(0, newN)
                                .mapToLong(i -> i < startN ? startImgSize[i] : img.dimension(i)).toArray();
                    } else if (newN < startN) {
                        // If we have less dimensions now, check if we only removed dimensions
                        // with grid = 1 and overlap = 0
                        if (!Arrays.stream(startGrid, newN, startN).allMatch(g -> g == 1)) {
                            throw new IllegalStateException("Removed dimension where grid is not 1.");
                        }
                        if (!Arrays.stream(startOverlap, newN, startN).allMatch(o -> o == 0)) {
                            throw new IllegalStateException("Removed dimension where overlap is not 0.");
                        }
                        grid = Arrays.copyOf(startGrid, newN);
                        negOverlap = Arrays.stream(startOverlap, 0, newN).map(l -> -l).toArray();
                        imgSize = Arrays.copyOf(startImgSize, newN);
                    } else {
                        // We have the same dimensions... That's easy
                        grid = startGrid.clone();
                        negOverlap = Arrays.stream(startOverlap).map(l -> -l).toArray();
                        imgSize = startImgSize.clone();
                    }
                } else {
                    // Not the first tile. Make sure that this tile has the same number of dimensions
                    if (img.numDimensions() != grid.length) {
                        throw new IllegalStateException("The table contains tiles with different number of dimensions.");
                    }
                }

                // Get the metadata
                if (tileMetadata == null) {
                    tileMetadata = imgVal.getMetadata();
                }

                // Get the factory
                if (imgFactory == null) {
                    imgFactory = img.factory();
                }

                // Remove overlap
                final RandomAccessibleInterval<T> tile = Views.zeroMin(Views.expandZero(img, negOverlap));

                // Add to the list
                tiles.add(tile);
            }
        }

        if (!tiles.isEmpty()) {
            // Combine the images
            ArrangedView<RandomAccessibleInterval<T>> arrangedView = new ArrangedView<>(tiles, grid);
            RandomAccessibleInterval<T> resultImage = new CombinedView<>(arrangedView);

            // Crop to original size
            resultImage = Views.interval(resultImage, new FinalInterval(imgSize));

            // Create a ImgPlus using the metadata of the first tile
            @SuppressWarnings("null")
            ImgPlusMetadata metadata =
                    MetadataUtil.copyImgPlusMetadata(tileMetadata, new DefaultImgMetadata(tileMetadata.numDimensions()));
            ImgPlus<T> resImg = new ImgPlus<>(ImgView.wrap(resultImage, imgFactory), metadata);
            resImg.setSource(metadata.getSource());

            // Add to table
            final DataCell outCell = m_cellFactory.createCell(resImg);
            m_resultContainer.addRowToTable(new DefaultRow(rowKey, outCell));
        } else {
            // If we got no tiles in a non empty table it probably only contains missing cells
            // In all cases we can not construct an image but the row should not disappear. Therefore we add a MissingCell
            m_resultContainer.addRowToTable(new DefaultRow(rowKey, new MissingCell(null)));
        }

        // finished
        if (loopStartNode.terminateLoop()) {
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
        m_cellFactory = null;

    }

    // ----------------- Static methods for settings models -------------------------

    /**
     * @return Model to store the Img Column.
     */
    static SettingsModelString createImgColumnModel() {
        return new SettingsModelString(IMG_COLUMN_CONF_KEY, "");
    }
}
