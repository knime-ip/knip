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
package org.knime.knip.base.nodes.seg.voronoi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.VoronoiLikeRegionGrowing;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeTools;

/**
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class VoronoiSegNodeModel<T extends RealType<T>, L extends Comparable<L>> extends NodeModel implements
        BufferedDataTableHolder {

    /** the default value for the threshold. */
    private static final int DEFAULT_THRESHOLD = 0;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(VoronoiSegNodeModel.class);

    public static final String[] RESULT_COLUMNS = new String[]{"Complete segmentation", "Segmentation without seeds",
            "Both, complete and without seeds"};

    static SettingsModelBoolean createFillHolesModel() {
        return new SettingsModelBoolean("fill_holes", true);
    }

    /**
     * the column name of the images to work on (full images).
     */
    static SettingsModelString createImgColModel() {
        return new SettingsModelString("source_image", null);
    }

    static SettingsModelString createResultColumnsModel() {
        return new SettingsModelString("result_columns", RESULT_COLUMNS[0]);
    }

    /**
     * the column name of the labeling containing the seed regions.
     */
    static SettingsModelString createSeedLabelingModel() {
        return new SettingsModelString("seed_labeling", null);
    }

    /**
     * pixel values below this threshold are considered background.
     */
    static SettingsModelInteger createThresholdModel() {
        return new SettingsModelInteger("threshold", DEFAULT_THRESHOLD);
    }

    /* data table for table cell view */
    private BufferedDataTable m_data;

    private final SettingsModelBoolean m_fillHoles = createFillHolesModel();

    private LabelingCellFactory m_labCellFactory;

    private final SettingsModelString m_resultCols = createResultColumnsModel();

    private final SettingsModelString m_seedLabCol = createSeedLabelingModel();

    private final SettingsModelString m_srcImgCol = createImgColModel();

    private final SettingsModelInteger m_threshold = createThresholdModel();

    /**
     * The default constructor.
     */
    public VoronoiSegNodeModel() {
        super(1, 1);
        reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        final ColumnRearranger rearranger = new ColumnRearranger(inSpecs[0]);
        rearranger.append(createCellFactory(inSpecs[0]));

        return new DataTableSpec[]{rearranger.createSpec()};
    }

    @SuppressWarnings("unchecked")
    private CellFactory createCellFactory(final DataTableSpec inSpec) throws InvalidSettingsException {
        return new CellFactory() {

            private final int imgColIdx = getImgColIdx(inSpec);

            private final int seedColIdx = getSegColIdx(inSpec);

            @Override
            public DataCell[] getCells(final DataRow row) {

                final ImgPlusValue<T> cellValue = ((ImgPlusValue<T>)row.getCell(imgColIdx));
                final ImgPlus<T> img = cellValue.getImgPlus();
                final Labeling<L> seed = ((LabelingValue<L>)row.getCell(seedColIdx)).getLabeling();
                final long[] imgDims = new long[img.numDimensions()];
                final long[] labDims = new long[seed.numDimensions()];
                img.dimensions(imgDims);
                seed.dimensions(labDims);

                if (!Arrays.equals(imgDims, labDims)) {
                    LOGGER.error("Labeling and Image dimensions in row " + row.getKey() + " are not compatible.");
                    if (m_resultCols.getStringValue().equals(RESULT_COLUMNS[2])) {
                        setWarningMessage("Some errors occured!");
                        return new DataCell[]{DataType.getMissingCell(), DataType.getMissingCell()};

                    } else {
                        setWarningMessage("Some errors occured!");
                        return new DataCell[]{DataType.getMissingCell()};

                    }

                }

                // prepare for the segmentation
                final T type = img.firstElement().createVariable();
                type.setReal(m_threshold.getIntValue());
                final VoronoiLikeRegionGrowing<L, T> vrg =
                        new VoronoiLikeRegionGrowing<L, T>(img, type, m_fillHoles.getBooleanValue());
                Labeling<L> res = null;
                // do the segmentation
                res = vrg.compute(seed, seed.<L> factory().create(seed));

                DataCell[] cells;
                if (m_resultCols.getStringValue().equals(RESULT_COLUMNS[0])) {
                    try {
                        cells =
                                new DataCell[]{m_labCellFactory.createCell(res, ((LabelingValue<L>)row
                                        .getCell(seedColIdx)).getLabelingMetadata())};
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    final Labeling<L> resSeedless = res.<L> factory().create(res);
                    final Cursor<LabelingType<L>> srcCur = seed.cursor();
                    final Cursor<LabelingType<L>> resCur = res.cursor();
                    final Cursor<LabelingType<L>> resSeedlessCur = resSeedless.cursor();
                    while (srcCur.hasNext()) {
                        srcCur.fwd();
                        resCur.fwd();
                        resSeedlessCur.fwd();
                        if (!resCur.get().getLabeling().isEmpty() && srcCur.get().getLabeling().isEmpty()) {
                            resSeedlessCur.get().set(resCur.get());
                        }
                    }
                    try {
                        if (m_resultCols.getStringValue().equals(RESULT_COLUMNS[1])) {

                            cells =
                                    new DataCell[]{m_labCellFactory.createCell(resSeedless, ((LabelingValue<L>)row
                                            .getCell(seedColIdx)).getLabelingMetadata())};

                        } else {
                            cells =
                                    new DataCell[]{
                                            m_labCellFactory.createCell(res,
                                                                        ((LabelingValue<L>)row.getCell(seedColIdx))
                                                                                .getLabelingMetadata()),
                                            m_labCellFactory.createCell(resSeedless, ((LabelingValue<L>)row
                                                    .getCell(seedColIdx)).getLabelingMetadata())};
                        }
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }

                }
                return cells;
            }

            @Override
            public DataColumnSpec[] getColumnSpecs() {
                final List<DataColumnSpec> specs = new ArrayList<DataColumnSpec>(2);
                final DataColumnSpecCreator creator = new DataColumnSpecCreator("Foo", LabelingCell.TYPE);
                if (m_resultCols.getStringValue().equals(RESULT_COLUMNS[0])
                        || m_resultCols.getStringValue().equals(RESULT_COLUMNS[2])) {
                    creator.setName("Segmentation");
                    specs.add(creator.createSpec());
                }
                if (m_resultCols.getStringValue().equals(RESULT_COLUMNS[1])
                        || m_resultCols.getStringValue().equals(RESULT_COLUMNS[2])) {
                    creator.setName("Segmentation (no seeds)");
                    specs.add(creator.createSpec());
                }
                return specs.toArray(new DataColumnSpec[specs.size()]);
            }

            @Override
            public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey,
                                    final ExecutionMonitor exec) {
                exec.setProgress((double)curRowNr / rowCount);

            }

        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        final ColumnRearranger rearranger = new ColumnRearranger(inData[0].getDataTableSpec());
        rearranger.append(createCellFactory(inData[0].getDataTableSpec()));

        m_labCellFactory = new LabelingCellFactory(exec);

        final BufferedDataTable[] res =
                new BufferedDataTable[]{exec.createColumnRearrangeTable(inData[0], rearranger, exec)};
        m_data = res[0];
        return res;
    }

    private int getImgColIdx(final DataTableSpec inSpec) throws InvalidSettingsException {
        // check settings for the original image, the full picture of
        // the cells.
        int imgColIdx = inSpec.findColumnIndex(m_srcImgCol.getStringValue());

        if (imgColIdx == -1) {
            if ((imgColIdx = NodeTools.autoOptionalColumnSelection(inSpec, m_srcImgCol, ImgPlusValue.class)) >= 0) {
                setWarningMessage("Auto-configure Image Column: " + m_srcImgCol.getStringValue());
            } else {
                throw new InvalidSettingsException("No column selected!");
            }
        }
        return imgColIdx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_data};
    }

    private int getSegColIdx(final DataTableSpec inSpec) throws InvalidSettingsException {
        // check settings for the seed region image
        int seedColIdx = inSpec.findColumnIndex(m_seedLabCol.getStringValue());

        if (seedColIdx == -1) {
            if ((seedColIdx = NodeTools.autoOptionalColumnSelection(inSpec, m_seedLabCol, LabelingValue.class)) >= 0) {
                setWarningMessage("Auto-configure Image Column: " + m_seedLabCol.getStringValue());
            } else {
                throw new InvalidSettingsException("No column selected!");
            }
        }
        return seedColIdx;
    }

    /**
     * does nothing.
     * 
     * @see org.knime.core.node.NodeModel# loadInternals(java.io.File, org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to load
    }

    /**
     * load settings.
     * 
     * @see org.knime.core.node.NodeModel#loadValidatedSettingsFrom (NodeSettingsRO)
     * @param settings where from
     * @throws InvalidSettingsException when sth's wrong
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_srcImgCol.loadSettingsFrom(settings);
        m_seedLabCol.loadSettingsFrom(settings);
        m_threshold.loadSettingsFrom(settings);
        m_fillHoles.loadSettingsFrom(settings);
        m_resultCols.loadSettingsFrom(settings);

    }

    /**
     * reset.
     * 
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
    }

    /**
     * does nothing.
     * 
     * @see org.knime.core.node.NodeModel# saveInternals(java.io.File, org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to save
    }

    /**
     * save settings.
     * 
     * @see org.knime.core.node.NodeModel#saveSettingsTo (NodeSettingsWO)
     * @param settings where
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_srcImgCol.saveSettingsTo(settings);
        m_seedLabCol.saveSettingsTo(settings);
        m_threshold.saveSettingsTo(settings);
        m_fillHoles.saveSettingsTo(settings);
        m_resultCols.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_data = tables[0];

    }

    /**
     * validate settings.
     * 
     * @see org.knime.core.node.NodeModel#validateSettings (NodeSettingsRO)
     * @param settings settings
     * @throws InvalidSettingsException when sth's wrong
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_srcImgCol.validateSettings(settings);
        m_seedLabCol.validateSettings(settings);
        m_threshold.validateSettings(settings);
        m_fillHoles.validateSettings(settings);
        m_resultCols.validateSettings(settings);

    }

}
