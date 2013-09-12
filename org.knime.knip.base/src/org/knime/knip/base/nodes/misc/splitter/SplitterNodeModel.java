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
package org.knime.knip.base.nodes.misc.splitter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.DefaultCalibratedAxis;
import net.imglib2.meta.DefaultCalibratedSpace;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.meta.TypedAxis;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.interval.binary.IntervalsFromDimSelection;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
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
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.NodeTools;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.data.img.DefaultImgMetadata;

/**
 * Splits an image.
 * 
 * @param <T> source image type
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SplitterNodeModel<T extends RealType<T>> extends NodeModel implements BufferedDataTableHolder {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SplitterNodeModel.class);

    private final String[] m_axisLabels = KNIMEKNIPPlugin.parseDimensionLabels();

    static SettingsModelIntegerBounded[] createAdvancedModels() {
        final SettingsModelIntegerBounded[] models =
                new SettingsModelIntegerBounded[KNIMEKNIPPlugin.parseDimensionLabels().length];
        for (int i = 0; i < models.length; i++) {
            models[i] = new SettingsModelIntegerBounded(i + "_advanced_selection", 0, 0, Integer.MAX_VALUE);
            models[i].setEnabled(false);
        }
        return models;

    }

    static SettingsModelString createColumnModel() {
        return new SettingsModelString("column_selection", "");
    }

    static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    static SettingsModelBoolean createIsAdvancedModel() {
        return new SettingsModelBoolean("is_advanced", false);
    }

    private final SettingsModelIntegerBounded[] m_advancedSelection = createAdvancedModels();

    /*
     * The index of the chosen column.
     */
    private int m_colIndex;

    /*
     * The selected columns
     */
    private final SettingsModelString m_column = createColumnModel();

    /* data for the table cell viewer */
    private BufferedDataTable m_data;

    private final SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

    private final SettingsModelBoolean m_isAdvanced = createIsAdvancedModel();

    /**
     * One input one output.
     * 
     */
    public SplitterNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        m_colIndex = inSpecs[0].findColumnIndex(m_column.getStringValue());

        if (m_colIndex == -1) {
            if ((m_colIndex = NodeTools.autoOptionalColumnSelection(inSpecs[0], m_column, ImgPlusValue.class)) >= 0) {
                setWarningMessage("Auto-configure Image Column: " + m_column.getStringValue());
            } else {
                throw new InvalidSettingsException("No column selected!");
            }
        }
        return null;
    }

    private int contains(final String[] labels, final TypedAxis axis) {
        for (int i = 0; i < labels.length; i++) {
            if (axis.type().getLabel().equals(labels[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        /* determine the least common dimension */
        DataRow row;
        final CloseableRowIterator preCalcIt = inData[0].iterator();
        final long[] dims = new long[10];
        Arrays.fill(dims, Long.MAX_VALUE);
        TypedAxis[] axes = null;
        final ImgPlusCellFactory imgCellFactory = new ImgPlusCellFactory(exec);
        while (preCalcIt.hasNext()) {
            row = preCalcIt.next();
            if (row.getCell(m_colIndex).isMissing()) {
                LOGGER.warn("Missing cell in row " + row.getKey() + ". Row skipped!");
                setWarningMessage("Rows with missing cells have been removed!");
                continue;
            }
            ImgPlusValue imgValue = ((ImgPlusValue)row.getCell(m_colIndex));
            final long[] tmp = imgValue.getDimensions();
            for (int i = 0; i < tmp.length; i++) {
                dims[i] = Math.min(dims[i], tmp[i]);
            }

            // test axis information (axes have to have same labels
            // and must be
            // of the same number)
            if (axes == null) {
                axes = new TypedAxis[tmp.length];
                for (int d = 0; d < axes.length; d++) {
                    axes[d] = ((ImgPlusValue)row.getCell(m_colIndex)).getMetadata().axis(d);
                }

            } else {
                final TypedAxis[] tmpAxes = new TypedAxis[tmp.length];
                for (int d = 0; d < axes.length; d++) {
                    tmpAxes[d] = ((ImgPlusValue)row.getCell(m_colIndex)).getMetadata().axis(d);
                }

                boolean equalAxes = true;
                if (tmpAxes.length != axes.length) {
                    equalAxes = false;
                } else {
                    for (int i = 0; i < tmpAxes.length; i++) {
                        if (!tmpAxes[i].type().getLabel().equals(axes[i].type().getLabel())) {
                            equalAxes = false;
                            break;
                        }
                    }
                }
                if (!equalAxes) {
                    throw new IllegalStateException("Image dimensions and axes labels must be the same for all images!");
                }
            }
        }
        preCalcIt.close();

        if (axes == null) {
            //if no image has been pre-calculated
            return inData;
        }

        final long[] leastCommonDims = new long[axes.length];
        for (int d = 0; d < leastCommonDims.length; d++) {
            leastCommonDims[d] = dims[d];
        }

        /* create table spec */

        // determine all subsets for the advanced splitter option
        Interval[] splitIntervals;

        int[] completelySelectedDims;
        if (m_isAdvanced.getBooleanValue()) {
            final List<Integer> complSelList = new ArrayList<Integer>(m_axisLabels.length);
            final int[] maxNumDimsPerInterval = new int[axes.length];
            int j;
            for (int i = 0; i < axes.length; i++) {
                if ((j = contains(m_axisLabels, axes[i])) != -1) {
                    if (m_advancedSelection[j].getIntValue() == 0) {
                        complSelList.add(i);
                    }
                    maxNumDimsPerInterval[i] = m_advancedSelection[j].getIntValue();

                }
            }
            completelySelectedDims = new int[complSelList.size()];
            for (int i = 0; i < completelySelectedDims.length; i++) {
                completelySelectedDims[i] = complSelList.get(i);
            }

            final IntervalsFromSplitSelection ifss = new IntervalsFromSplitSelection(maxNumDimsPerInterval);

            splitIntervals = Operations.compute(ifss, new FinalInterval(leastCommonDims));

        } else {

            completelySelectedDims = m_dimSelection.getSelectedDimIndices(leastCommonDims.length, axes);
            splitIntervals =
                    IntervalsFromDimSelection.compute(completelySelectedDims, new FinalInterval(leastCommonDims));
        }

        final List<DataColumnSpec> columnSpecs = new ArrayList<DataColumnSpec>();
        final boolean[] tmpCompletelySel = new boolean[axes.length];
        for (int i = 0; i < completelySelectedDims.length; i++) {
            tmpCompletelySel[completelySelectedDims[i]] = true;
        }
        for (int j = 0; j < splitIntervals.length; j++) {
            columnSpecs.add(new DataColumnSpecCreator("Img [" + intervalToString(splitIntervals[j], tmpCompletelySel)
                    + "]", ImgPlusCell.TYPE).createSpec());

        }

        final DataTableSpec outSpec = new DataTableSpec(columnSpecs.toArray(new DataColumnSpec[columnSpecs.size()]));

        /* Perform the cropping */
        final RowIterator it = inData[0].iterator();
        final BufferedDataContainer con = exec.createDataContainer(outSpec);
        final int count = inData[0].getRowCount();
        int i = 0;
        final long[] tmpMin = new long[axes.length];
        final long[] tmpMax = new long[axes.length];
        while (it.hasNext()) {
            row = it.next();
            if (row.getCell(m_colIndex).isMissing()) {
                continue;
            }

            final ImgPlus<T> img = ((ImgPlusValue<T>)row.getCell(m_colIndex)).getImgPlus();

            final DataCell[] cells = new DataCell[splitIntervals.length];

            for (int intervalIdx = 0; intervalIdx < splitIntervals.length; intervalIdx++) {

                // set the according dimension size for
                // dimensions which are completely selected
                final Interval tmp = splitIntervals[intervalIdx];
                tmp.min(tmpMin);
                tmp.max(tmpMax);
                for (int j = 0; j < completelySelectedDims.length; j++) {
                    tmpMin[completelySelectedDims[j]] = 0;
                    tmpMax[completelySelectedDims[j]] = img.max(completelySelectedDims[j]);
                }
                final Interval interval = new FinalInterval(tmpMin, tmpMax);

                // create subimg view
                final Img<T> subImg = new ImgView<T>(SubsetOperations.subsetview(img, interval), img.factory());

                final CalibratedSpace<CalibratedAxis> typedSpace = new DefaultCalibratedSpace(subImg.numDimensions());
                int d = 0;
                for (int d0 = 0; d0 < axes.length; d0++) {
                    if (interval.dimension(d0) != 1) {
                        typedSpace.setAxis(new DefaultCalibratedAxis(axes[d0].type()), d++);
                    }
                }

                final ImgPlusMetadata metadata = new DefaultImgMetadata(typedSpace, img, img, img);
                cells[intervalIdx] = imgCellFactory.createCell(subImg, metadata);
            }

            con.addRowToTable(new DefaultRow(row.getKey(), cells));
            exec.checkCanceled();
            exec.setProgress((double)i++ / count);
        }
        con.close();
        m_data = con.getTable();
        return new BufferedDataTable[]{m_data};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_data};
    }

    private String intervalToString(final Interval interval, final boolean[] completelySelected) {
        final StringBuilder b = new StringBuilder();

        final long[] min = new long[interval.numDimensions()];
        final long[] max = new long[interval.numDimensions()];
        interval.min(min);
        interval.max(max);
        b.append("min=");
        b.append('[');
        for (int i = 0;; i++) {
            if (completelySelected[i]) {
                b.append("*");
            } else {
                b.append(min[i]);
            }
            if (i == (min.length - 1)) {
                b.append(']').toString();
                break;
            }
            b.append(", ");
        }

        b.append(";max=");
        b.append('[');
        for (int i = 0;; i++) {
            if (completelySelected[i]) {
                b.append("*");
            } else {
                b.append(max[i]);
            }
            if (i == (max.length - 1)) {
                b.append(']').toString();
                break;
            }
            b.append(", ");
        }
        return b.toString();
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
        m_column.loadSettingsFrom(settings);
        m_dimSelection.loadSettingsFrom(settings);
        m_isAdvanced.loadSettingsFrom(settings);
        for (final SettingsModel sm : m_advancedSelection) {
            sm.loadSettingsFrom(settings);
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
        m_column.saveSettingsTo(settings);
        m_dimSelection.saveSettingsTo(settings);
        m_isAdvanced.saveSettingsTo(settings);
        for (final SettingsModel sm : m_advancedSelection) {
            sm.saveSettingsTo(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_data = tables[0];

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_column.validateSettings(settings);
        m_dimSelection.validateSettings(settings);
        m_isAdvanced.validateSettings(settings);
        for (final SettingsModel sm : m_advancedSelection) {
            sm.validateSettings(settings);
        }

    }

}
