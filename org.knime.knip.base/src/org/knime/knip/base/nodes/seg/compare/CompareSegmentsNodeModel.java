/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Feb 5, 2013 (hornm): created
 */
package org.knime.knip.base.nodes.seg.compare;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.knime.base.node.parallel.appender.AppendColumn;
import org.knime.base.node.parallel.appender.ColumnDestination;
import org.knime.base.node.parallel.appender.ExtendedCellFactory;
import org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.NodeUtils;

import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.type.logic.BitType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * Node to estimate the maximum relative pixel agreement of a list of segments according to a list of reference
 * segments.
 *
 * @author Martin Horn, University of Konstanz
 */
public class CompareSegmentsNodeModel extends ThreadedColAppenderNodeModel {

    final static SettingsModelString createReferenceColumnModel() {
        return new SettingsModelString("reference_column", "");
    }

    final static SettingsModelString createTargetColumnModel() {
        return new SettingsModelString("target_column", "");
    }

    final static SettingsModelBoolean createAppendNumOverlapsModel() {
        return new SettingsModelBoolean("num_overlaps", false);
    }

    final static SettingsModelBoolean createAppendOverlapRowkeysModel() {
        return new SettingsModelBoolean("overlap_rowkeys", false);
    }

    private SettingsModelString m_smRefCol = createReferenceColumnModel();

    private SettingsModelString m_smTarCol = createTargetColumnModel();

    private SettingsModelBoolean m_smAppendNumOverlaps = createAppendNumOverlapsModel();

    private SettingsModelBoolean m_smAppendOverlapRowkeys = createAppendOverlapRowkeysModel();

    /**
     * @param nrInDataPorts
     * @param nrOutDataPorts
     */
    protected CompareSegmentsNodeModel() {
        super(2, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        NodeUtils.autoColumnSelection(inSpecs[1], m_smRefCol, ImgPlusValue.class, this.getClass());
        NodeUtils.autoColumnSelection(inSpecs[0], m_smTarCol, ImgPlusValue.class, this.getClass());

        return new DataTableSpec[]{createOutputSpec(inSpecs[0], createCellFactory(null, null, null, -1))};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ExtendedCellFactory[] prepareExecute(final DataTable[] inData) throws Exception {
        int refColIdx =
                NodeUtils.autoColumnSelection(inData[1].getDataTableSpec(), m_smRefCol, ImgPlusValue.class,
                                              this.getClass());
        int tarColIdx =
                NodeUtils.autoColumnSelection(inData[0].getDataTableSpec(), m_smTarCol, ImgPlusValue.class,
                                              this.getClass());

        ImgPlusValue<BitType>[] refSegs = new ImgPlusValue[((BufferedDataTable)inData[1]).getRowCount()];
        String[] refKeys = null;
        int[] numPix = new int[refSegs.length];
        if (m_smAppendOverlapRowkeys.getBooleanValue()) {
            refKeys = new String[refSegs.length];
        }

        // collect reference segments and row keys
        int i = 0;
        for (DataRow row : inData[1]) {
            refSegs[i] = (ImgPlusValue<BitType>)row.getCell(refColIdx);
            numPix[i] = numPix(refSegs[i]);
            if (refKeys != null) {
                refKeys[i] = row.getKey().toString();
            }
            i++;
        }

        return new ExtendedCellFactory[]{createCellFactory(refSegs, refKeys, numPix, tarColIdx)};
    }

    private ExtendedCellFactory createCellFactory(final ImgPlusValue<BitType>[] refSegs, final String[] refKeys,
                                                  final int[] numPix, final int tarColIdx) {
        return new ExtendedCellFactory() {

            @Override
            public DataColumnSpec[] getColumnSpecs() {
                ArrayList<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>(3);
                colSpecs.add(new DataColumnSpecCreator("maximum relative pixel agreement", DoubleCell.TYPE)
                        .createSpec());
                if (m_smAppendNumOverlaps.getBooleanValue()) {
                    colSpecs.add(new DataColumnSpecCreator("number of overlaps", IntCell.TYPE).createSpec());
                }
                if (m_smAppendOverlapRowkeys.getBooleanValue()) {
                    colSpecs.add(new DataColumnSpecCreator("overlap keys", SetCell.getCollectionType(StringCell.TYPE))
                            .createSpec());
                }
                return colSpecs.toArray(new DataColumnSpec[colSpecs.size()]);
            }

            @Override
            public DataCell[] getCells(final DataRow row) {
                ImgPlusValue<BitType> tarVal = (ImgPlusValue<BitType>)row.getCell(tarColIdx);
                double overlap;
                int count = 0;
                double numPix1 = numPix(tarVal);
                double res = Double.MIN_VALUE;
                ArrayList<StringCell> keys = null;
                if (refKeys != null) {
                    keys = new ArrayList<StringCell>();
                }
                for (int i = 0; i < refSegs.length; i++) {
                    overlap = overlap(refSegs[i], tarVal);
                    if (overlap > 0) {
                        count++;
                        if (keys != null) {
                            keys.add(new StringCell(refKeys[i]));
                        }
                    }
                    res = Math.max(res, overlap / (numPix1 + numPix[i] - overlap));
                }
                ArrayList<DataCell> cells = new ArrayList<DataCell>(3);
                cells.add(new DoubleCell(res));
                if (m_smAppendNumOverlaps.getBooleanValue()) {
                    cells.add(new IntCell(count));
                }
                if (m_smAppendOverlapRowkeys.getBooleanValue()) {
                    cells.add(CollectionCellFactory.createSetCell(keys));
                }
                return cells.toArray(new DataCell[cells.size()]);
            }

            @Override
            public ColumnDestination[] getColumnDestinations() {
                ColumnDestination[] cd =
                        new ColumnDestination[1 + (m_smAppendNumOverlaps.getBooleanValue() ? 1 : 0)
                                + (m_smAppendOverlapRowkeys.getBooleanValue() ? 1 : 0)];
                Arrays.fill(cd, new AppendColumn());
                return cd;
            }
        };
    }

    private int overlap(final ImgPlusValue<BitType> val1, final ImgPlusValue<BitType> val2) {

        Img<BitType> img1 = val1.getImgPlus();
        Img<BitType> img2 = val2.getImgPlus();

        if (!val1.getMetadata().getSource().equals(val2.getMetadata().getSource())) {
            return 0;
        }

        RandomAccessibleInterval<BitType> iv1 = img1;
        RandomAccessibleInterval<BitType> iv2 = img2;

        //adopt image dimension if they are different
        int dimDiff = img1.numDimensions() - img2.numDimensions();
        if (dimDiff < 0) {
            for (int i = 0; i < Math.abs(dimDiff); i++) {
                iv1 = Views.addDimension(iv1, 0, 0);
            }
        } else if (dimDiff > 0) {
            for (int i = 0; i < Math.abs(dimDiff); i++) {
                iv2 = Views.addDimension(iv2, 0, 0);
            }
        }

        Interval intersect = Intervals.intersect(iv1, iv2);
        for (int i = 0; i < intersect.numDimensions(); i++) {
            if (intersect.dimension(i) <= 0) {
                return 0;
            }
        }
        RandomAccess<BitType> ra1 = iv1.randomAccess();
        RandomAccess<BitType> ra2 = iv2.randomAccess();

        IntervalIterator ii = new IntervalIterator(intersect);

        int numPix = 0;
        while (ii.hasNext()) {
            ii.fwd();
            ra1.setPosition(ii);
            if (ra1.get().get()) {
                ra2.setPosition(ii);
                if (ra2.get().get()) {
                    numPix++;
                }
            }
        }
        return numPix;
    }

    private int numPix(final ImgPlusValue<BitType> val) {
        int numPix = 0;
        for (BitType t : val.getZeroMinImgPlus()) {
            if (t.get()) {
                numPix++;
            }
        }
        return numPix;
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
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_smRefCol.saveSettingsTo(settings);
        m_smTarCol.saveSettingsTo(settings);
        m_smAppendNumOverlaps.saveSettingsTo(settings);
        m_smAppendOverlapRowkeys.saveSettingsTo(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_smRefCol.validateSettings(settings);
        m_smTarCol.validateSettings(settings);
        m_smAppendNumOverlaps.validateSettings(settings);
        m_smAppendOverlapRowkeys.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_smRefCol.loadSettingsFrom(settings);
        m_smTarCol.loadSettingsFrom(settings);
        m_smAppendNumOverlaps.loadSettingsFrom(settings);
        m_smAppendOverlapRowkeys.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }

}
