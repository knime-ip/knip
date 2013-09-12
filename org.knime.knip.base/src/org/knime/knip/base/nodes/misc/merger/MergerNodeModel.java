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
package org.knime.knip.base.nodes.misc.merger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
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
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;

/**
 * Merges an images to one.
 * 
 * @param <T> source image type
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class MergerNodeModel<T extends RealType<T>> extends NodeModel implements BufferedDataTableHolder {

    /**
     * Key to store the column to work on in the settings.
     */
    static final String CFG_COLUMNS = "columns";

    static final String CFG_MERGE_SETTINGS_DIM_NAMES = "merge_settings_dim_names";

    static final String CFG_MERGE_SETTINGS_DIMS = "merge_settings_dims";

    static final String CFG_IMG_FACTORY = "img_factory";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MergerNodeModel.class);

    static final String[] IMG_FACTORIES = {"Array Image Factory", "Planar Image Factory"};

    /*
     * The selected columns
     */
    private final SettingsModelFilterString m_columns = new SettingsModelFilterString(MergerNodeModel.CFG_COLUMNS);

    /*
     * data table for the table cell view
     */
    private BufferedDataTable m_data;

    private final SettingsModelString m_mergeSettingsDimNames = new SettingsModelString(CFG_MERGE_SETTINGS_DIM_NAMES,
            "X,Y");

    /*
     *
     */
    private final SettingsModelString m_mergeSettingsDims = new SettingsModelString(CFG_MERGE_SETTINGS_DIMS, "");

    private final SettingsModelString m_imgFactory = new SettingsModelString(CFG_IMG_FACTORY, IMG_FACTORIES[0]);

    /*
     * The specification of the resulting table.
     */
    private DataTableSpec m_outSpec;

    /**
     * One input one output.
     * 
     */
    public MergerNodeModel() {
        super(1, 1);
    }

    /* Helper to collect all columns of the given type. */
    private void collectAllColumns(final List<String> colNames, final DataTableSpec spec) {
        colNames.clear();
        for (final DataColumnSpec c : spec) {
            if (c.getType().isCompatible(ImgPlusValue.class)) {
                colNames.add(c.getName());
            }
        }
        if (colNames.size() == 0) {
            LOGGER.warn("No columns of type " + ImgPlusValue.class.getSimpleName() + " available!");
            return;
        }
        LOGGER.info("All available columns of type " + ImgPlusValue.class.getSimpleName() + " are selected!");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        getSelectedColumnIndices(inSpecs[0]);

        if (m_mergeSettingsDims.getStringValue().trim().length() == 0) {
            throw new InvalidSettingsException("Merge dimensions must not be empty!");
        }

        // try {
        // parseMergeSettings();
        // } catch (NumberFormatException e) {
        // throw new InvalidSettingsException(
        // "Invalid Merge Settings!");
        // }

        final DataColumnSpecCreator c = new DataColumnSpecCreator("Merged Img", ImgPlusCell.TYPE);
        m_outSpec = new DataTableSpec(c.createSpec());

        return new DataTableSpec[]{m_outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        final int[] selectedColIndices = getSelectedColumnIndices(inData[0].getDataTableSpec());

        // parse the merge settings
        final String[] mergeSet = m_mergeSettingsDims.getStringValue().trim().split(",");

        final RowIterator it = inData[0].iterator();
        final BufferedDataContainer con = exec.createDataContainer(m_outSpec);
        DataRow row;
        final int rowNum = inData[0].getRowCount();
        int rowCount = 0;

        final ImgPlusCellFactory imgCellFactory = new ImgPlusCellFactory(exec);

        while (it.hasNext()) {
            row = it.next();

            // determine total pixel number
            long numTotalPixels = 0;
            ImgPlus<T> img = null;
            DataCell cell;
            for (int i = 0; i < selectedColIndices.length; i++) {
                cell = row.getCell(selectedColIndices[i]);
                if (cell.isMissing()) {
                    LOGGER.warn("Missing cell in row " + row.getKey() + ". Image skipped.");
                    continue;
                }
                img = ((ImgPlusValue<T>)cell).getImgPlus();
                numTotalPixels += img.size();
            }

            if (img == null) {
                break;
            }

            // calculate the dimensions of the new image
            long tmp = 1;
            long[] dims = null;
            try {
                for (int numDim = 0; numDim < mergeSet.length; numDim++) {
                    tmp *= getDim(mergeSet, img, numDim);
                    if (numTotalPixels <= tmp) {
                        dims = new long[numDim + 1];
                        for (int d = 0; d < (numDim + 1); d++) {
                            dims[d] = getDim(mergeSet, img, d);
                        }
                    }
                }
                if (dims == null) {
                    dims = new long[mergeSet.length + 1];
                    for (int d = 0; d < mergeSet.length; d++) {
                        // System.arraycopy(mergeSet, 0,
                        // dims,
                        // 0,
                        // mergeSet.length);
                        dims[d] = getDim(mergeSet, img, d);
                    }
                    dims[dims.length - 1] = (int)Math.ceil(numTotalPixels / (double)tmp);
                }
            } catch (final InvalidSettingsException e) {
                LOGGER.warn("Error in row " + row.getKey() + ". Row skipped!", e);
                continue;
            }

            // create result image
            Img<T> res = null;
            if (m_imgFactory.getStringValue().equals(IMG_FACTORIES[0])) {
                res = new ArrayImgFactory().create(dims, (NativeType)img.firstElement().createVariable());
            } else {
                res = new PlanarImgFactory().create(dims, (NativeType)img.firstElement().createVariable());
            }

            final Cursor<T> resCur = res.cursor();
            for (int i = 0; i < selectedColIndices.length; i++) {
                cell = row.getCell(selectedColIndices[i]);
                if (cell.isMissing()) {
                    continue;
                }
                img = ((ImgPlusValue<T>)cell).getImgPlus();
                final Cursor<T> imgCur = img.cursor();
                while (imgCur.hasNext()) {
                    imgCur.fwd();
                    resCur.fwd();
                    resCur.get().set(imgCur.get());
                }
            }

            final AxisType[] axes = new AxisType[res.numDimensions()];
            final String[] axisNames = m_mergeSettingsDimNames.getStringValue().trim().split(",");

            int a;
            for (a = 0; a < Math.min(axisNames.length, axes.length); a++) {
                axes[a] = Axes.get(axisNames[a]);
            }
            for (int j = a; j < axes.length; j++) {
                axes[j] = Axes.get("Unknown" + (j - a));
            }
            con.addRowToTable(new DefaultRow(row.getKey(), imgCellFactory.createCell(new ImgPlus(res, img.getName(),
                    axes))));
            exec.checkCanceled();
            exec.setProgress((double)rowCount++ / rowNum);
        }
        con.close();
        m_data = con.getTable();
        return new BufferedDataTable[]{m_data};
    }

    private long getDim(final String[] dimStrings, final ImgPlus<T> img, final int dimIndex)
            throws InvalidSettingsException {
        try {
            return Long.parseLong(dimStrings[dimIndex]);
        } catch (final NumberFormatException e) {
            for (int d = 0; d < img.numDimensions(); d++) {
                if (dimStrings[dimIndex].equals(img.axis(d).type().getLabel())) {
                    return img.dimension(d);
                }
            }
        }
        throw new InvalidSettingsException(
                "Wrong dimension settings. Can not be parsed or dimension label doesn't exist for image "
                        + img.getName() + "!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_data};
    }

    /*
     * Retrieves the selected column indices from the given DataTableSpec
     * and the column selection. If the selection turned out to be invalid,
     * all columns are selected.
     */
    private int[] getSelectedColumnIndices(final DataTableSpec inSpec) {
        final List<String> colNames;
        if ((m_columns.getIncludeList().size() == 0) || m_columns.isKeepAllSelected()) {
            colNames = new ArrayList<String>();
            collectAllColumns(colNames, inSpec);
            m_columns.setIncludeList(colNames);

        } else {
            colNames = new ArrayList<String>();
            colNames.addAll(m_columns.getIncludeList());
            if (!validateColumnSelection(colNames, inSpec)) {
                setWarningMessage("Invalid column selection. All columns are selected!");
                collectAllColumns(colNames, inSpec);
            }
        }

        // get column indices
        final List<Integer> colIndices = new ArrayList<Integer>(colNames.size());
        for (int i = 0; i < colNames.size(); i++) {
            final int colIdx = inSpec.findColumnIndex(colNames.get(i));
            if (colIdx == -1) {
                // can not occur, actually
                LOGGER.warn("Column " + colNames.get(i) + " doesn't exist!");
            } else {
                colIndices.add(colIdx);
            }
        }

        final int[] colIdx = new int[colIndices.size()];
        for (int i = 0; i < colIdx.length; i++) {
            colIdx[i] = colIndices.get(i);
        }

        return colIdx;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columns.loadSettingsFrom(settings);
        m_mergeSettingsDims.loadSettingsFrom(settings);
        m_mergeSettingsDimNames.loadSettingsFrom(settings);
        try {
            m_imgFactory.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            //added with 1.1
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columns.saveSettingsTo(settings);
        m_mergeSettingsDims.saveSettingsTo(settings);
        m_mergeSettingsDimNames.saveSettingsTo(settings);
        m_imgFactory.saveSettingsTo(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_data = tables[0];

    }

    /* Checks if a column is not present in the DataTableSpec */
    private boolean validateColumnSelection(final List<String> colNames, final DataTableSpec spec) {
        for (int i = 0; i < colNames.size(); i++) {
            final int colIdx = spec.findColumnIndex(colNames.get(i));
            if (colIdx == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columns.validateSettings(settings);
        m_mergeSettingsDims.validateSettings(settings);
        m_mergeSettingsDimNames.validateSettings(settings);
        try {
            m_imgFactory.validateSettings(settings);
        } catch (InvalidSettingsException e) {
            //added with 1.1
        }
    }

}
