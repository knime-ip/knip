/*
 * ------------------------------------------------------------------------
 *
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
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.nodes.util.tilelooper.config.SettingsModelOptionalNumber;
import org.knime.knip.base.nodes.util.tilelooper.config.SettingsModelOptionalNumberRange;
import org.knime.knip.base.nodes.util.tilelooper.imglib2.TiledView;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.types.OutOfBoundsStrategyEnum;
import org.knime.knip.core.types.OutOfBoundsStrategyFactory;

import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imagej.ops.MetadataUtil;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * Node model for the tile loop start node.
 *
 * @param <T> Type of the image values.
 *
 * @author Benjamin Wilhelm, MPI-CBG, Dresden
 */
public class TileIteratorLoopStartNodeModel<T extends RealType<T>> extends NodeModel
        implements LoopStartNodeTerminator, BufferedDataTableHolder {

    // ----------------------------------------------- Static settings constants ----------------------

    private static final int DEFAULT_TILE_SIZE = 100;

    private static final int DEFAULT_OVERLAP = 0;

    /** Percentage of tile which must be outside of the image for a warning to appear. */
    private static final float BAD_TILESIZE_WARNING_THRESHOLD = 0.5f;

    private static final String BAD_TILESIZE_WARNING_MESSAGE = "Bad tilesize in dimension %s. More than "
            + BAD_TILESIZE_WARNING_THRESHOLD * 100 + " %% of the tile outside of the image.";

    private static final String IMG_COLUMN_CONF_KEY = "img_column_key";

    private static final String OUT_OF_BOUNDS_CONF_KEY = "out_of_bounds";

    private static final String TILE_SIZE_CONF_KEY = "tile_size_";

    private static final String OVERLAP_CONF_KEY = "overlap_";

    // ----------------------------------------------- Settings models ---------------------------------

    private final SettingsModelString m_columnSelection = createImgColumnModel();

    private final SettingsModelString m_outOfBoundsStrategy = createOutOfBoundsModel();

    private final SettingsModelOptionalNumber[] m_tileSizes = createTileSizeModels();

    private final SettingsModelOptionalNumber[] m_overlap = createOverlapModels();

    // ---------------------------------------------- Loop helper stuff -------------------------------

    private long m_iteration;

    private BufferedDataTable m_table;

    private CloseableRowIterator m_iterator;

    private ImgPlusCellFactory m_cellFactory;

    private long[] m_currentGrid;

    private long[] m_currentImgSize;

    private long[] m_currentOverlap;

    // ----------------------------------------------- Misc --------------------------------------------

    private BufferedDataTable m_dataTable;

    /**
     * Creates a new node model for the tile loop end node.
     */
    public TileIteratorLoopStartNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        assert m_iteration == 0;
        return new DataTableSpec[]{TileIteratorUtils.createOutSpecs(inSpecs[0], m_columnSelection, this.getClass())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        BufferedDataTable table = inData[0];

        // Do some preperation
        if (m_iteration == 0) {
            m_table = table;
            m_iterator = table.iterator();
        }
        m_cellFactory = new ImgPlusCellFactory(exec);

        // Create the output table
        BufferedDataContainer cont = exec.createDataContainer(TileIteratorUtils
                .createOutSpecs(table.getDataTableSpec(), m_columnSelection, this.getClass()));

        if (m_iterator.hasNext()) {
            // Get the image to tile
            final DataRow dataRow = m_iterator.next();
            final DataCell imgCell = dataRow.getCell(TileIteratorUtils
                    .getSelectedColumnIndex(table.getDataTableSpec(), m_columnSelection, this.getClass()));

            if (imgCell instanceof ImgPlusValue) {
                @SuppressWarnings("unchecked")
                ImgPlusValue<T> imgVal = (ImgPlusValue<T>)imgCell;
                ImgPlus<T> img = imgVal.getImgPlus();
                ImgPlusMetadata imgMetadata = imgVal.getMetadata();
                ImgFactory<T> imgFactory = img.factory();

                // Resize the image so it fits the tiles
                m_currentImgSize = Intervals.dimensionsAsLongArray(img);
                long[] tileSize = getTileSize(m_currentImgSize, imgMetadata);
                long[] min = new long[m_currentImgSize.length];
                long[] max = new long[m_currentImgSize.length];
                m_currentGrid = new long[m_currentImgSize.length];
                m_currentOverlap = getOverlap(imgMetadata);
                for (int i = 0; i < m_currentImgSize.length; i++) {
                    m_currentGrid[i] = (long)Math.ceil(m_currentImgSize[i] / (float)tileSize[i]);
                    min[i] = img.min(i);
                    max[i] = min[i] + m_currentGrid[i] * tileSize[i] - 1;

                    // Check if the tile size is very bad
                    if (max[i] - min[i] - m_currentImgSize[i] >= tileSize[i] * BAD_TILESIZE_WARNING_THRESHOLD) {
                        setWarningMessage(String.format(BAD_TILESIZE_WARNING_MESSAGE, imgMetadata.axis(i).type().getLabel()));
                    }
                }
                OutOfBoundsFactory<T, RandomAccessibleInterval<T>> outOfBoundsFactory =
                        getOutOfBoundsStrategy(img.firstElement());
                RandomAccessibleInterval<T> extended =
                        Views.zeroMin(Views.interval(Views.extend(img, outOfBoundsFactory), min, max));

                // Tile the image
                TiledView<T> tiledView = new TiledView<>(extended, tileSize, m_currentOverlap);

                // Loop over all tiles and add them
                Cursor<RandomAccessibleInterval<T>> cursor = Views.flatIterable(tiledView).cursor();
                int i = 0;
                while (cursor.hasNext()) {
                    RandomAccessibleInterval<T> tileRAI = cursor.next();

                    // Create a ImgPlus using the metadata of the original image
                    ImgPlusMetadata metadata =
                            MetadataUtil.copyImgPlusMetadata(imgMetadata, new DefaultImgMetadata(imgMetadata.numDimensions()));
                    ImgPlus<T> tile = new ImgPlus<>(ImgView.wrap(tileRAI, imgFactory), metadata);
                    tile.setSource(metadata.getSource());

                    // Add to table
                    DataCell outCell = m_cellFactory.createCell(tile);
                    cont.addRowToTable(new DefaultRow(dataRow.getKey().getString() + TileIteratorUtils.ROW_KEY_DELIMITER + ++i,
                            outCell));
                }
            } else if (imgCell instanceof MissingCell){
                // We just keep missing cells as they are
                cont.addRowToTable(new DefaultRow(
                        dataRow.getKey().getString() + TileIteratorUtils.ROW_KEY_DELIMITER, imgCell));
            } else {
                // We neither have an image nor a missing cell. This should not happen as we asked for an image column
                throw new IllegalStateException("Cell of wrong type: " + imgCell.getType().getName());
            }
        }

        // This iteration is done now
        m_iteration++;
        cont.close();
        m_dataTable = cont.getTable();
        return new BufferedDataTable[]{m_dataTable};
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
    public boolean terminateLoop() {
        boolean continueLoop = m_iterator == null || m_iterator.hasNext();
        return !continueLoop;
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
        m_outOfBoundsStrategy.saveSettingsTo(settings);
        for (SettingsModel ts : m_tileSizes) {
            ts.saveSettingsTo(settings);
        }
        for (SettingsModel ov : m_overlap) {
            ov.saveSettingsTo(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnSelection.validateSettings(settings);
        m_outOfBoundsStrategy.validateSettings(settings);
        for (SettingsModel ts : m_tileSizes) {
            ts.validateSettings(settings);
        }
        for (SettingsModel ov : m_overlap) {
            ov.validateSettings(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnSelection.loadSettingsFrom(settings);
        m_outOfBoundsStrategy.loadSettingsFrom(settings);
        for (SettingsModel ts : m_tileSizes) {
            ts.loadSettingsFrom(settings);
        }
        for (SettingsModel ov : m_overlap) {
            ov.loadSettingsFrom(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_iteration = 0;
        if (m_iterator != null) {
            m_iterator.close();
        }
        m_iterator = null;
        m_table = null;
        m_cellFactory = null;
    }

    // ----------------- Static methods for settings models -------------------------

    /**
     * @return Model to store the Img Column.
     */
    static SettingsModelString createImgColumnModel() {
        return new SettingsModelString(IMG_COLUMN_CONF_KEY, "");
    }

    /**
     * @return Model to store the out of bounds strategy
     */
    static SettingsModelString createOutOfBoundsModel() {
        return new SettingsModelString(OUT_OF_BOUNDS_CONF_KEY, OutOfBoundsStrategyEnum.MIRROR_DOUBLE.name());
    }

    /**
     * @return Models to store the tile size per dimension
     */
    static SettingsModelOptionalNumber[] createTileSizeModels() {
        final SettingsModelOptionalNumber[] models =
                new SettingsModelOptionalNumber[KNIMEKNIPPlugin.parseDimensionLabels().length];
        for (int i = 0; i < models.length; i++) {
            models[i] = new SettingsModelOptionalNumberRange(TILE_SIZE_CONF_KEY + i, DEFAULT_TILE_SIZE, false, 1,
                    Integer.MAX_VALUE);
        }
        return models;
    }

    /**
     * @return Models to store the overlap per dimension
     */
    static SettingsModelOptionalNumber[] createOverlapModels() {
        final SettingsModelOptionalNumber[] models =
                new SettingsModelOptionalNumber[KNIMEKNIPPlugin.parseDimensionLabels().length];
        for (int i = 0; i < models.length; i++) {
            models[i] = new SettingsModelOptionalNumberRange(OVERLAP_CONF_KEY + i, DEFAULT_OVERLAP, false, 0,
                    Integer.MAX_VALUE);
        }
        return models;
    }
    /**
     * @param inSpec
     * @return
     */
    // ----------------- Helpers for interpreting the settings ----------------------

    /**
     * Calculates the tile size for the current image using the user settings.
     *
     * @param dimSize The size of the current image.
     * @param metadata The metadata of the image for the axis names.
     * @return the tile size.
     */
    protected long[] getTileSize(final long[] dimSize, final ImgPlusMetadata metadata) {
        long[] tileSize = TileIteratorUtils.modelToArray(metadata, m_tileSizes);
        for (int i = 0; i < tileSize.length; i++) {
            // Use the dimension length of the image if the tile size wasn't set
            // or is bigger than the dimension length of the image.
            tileSize[i] = tileSize[i] > 0 && tileSize[i] < dimSize[i] ? tileSize[i] : dimSize[i];
        }
        return tileSize;
    }

    /**
     * Calculates the overlap for the current image using the user settings.
     *
     * @param metadata The metadata of the image for the axis names.
     * @return The overlap in each dimension.
     */
    protected long[] getOverlap(final ImgPlusMetadata metadata) {
        long[] overlap = TileIteratorUtils.modelToArray(metadata, m_overlap);
        for (int i = 0; i < overlap.length; i++) {
            overlap[i] = overlap[i] > 0 ? overlap[i] : DEFAULT_OVERLAP;
        }
        return overlap;
    }

    /**
     * Creates an {@link OutOfBoundsFactory} for the chosen out of bounds strategy.
     *
     * @param val A value of the image to get the type.
     * @return A {@link OutOfBoundsFactory}.
     */
    protected OutOfBoundsFactory<T, RandomAccessibleInterval<T>> getOutOfBoundsStrategy(final T val) {
        return OutOfBoundsStrategyFactory.getStrategy(m_outOfBoundsStrategy.getStringValue(), val);
    }

    // ---------------- Communication with the Loop End ------------------------------

    long[] getCurrentGrid() {
        return m_currentGrid;
    }

    long[] getCurrentImgSize() {
        return m_currentImgSize;
    }

    long[] getCurrentOverlap() {
        return m_currentOverlap;
    }
}
