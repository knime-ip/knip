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
package org.knime.knip.base.nodes.seg.lab2table;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.meta.DefaultNamed;
import net.imglib2.meta.DefaultSourced;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.sampler.special.ConstantRandomAccessible;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntervalCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.renderer.DataValueRenderer;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeTools;
import org.knime.knip.core.data.img.DefaultImageMetadata;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;

/**
 * Labeling is converted into a KNIME table. For each possible label the region of interest is extracted and put into a
 * {@link IntervalCell} together with a {@link BitType} {@link Img}
 * 
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class Lab2TableNodeModel<L extends Comparable<L>, II extends IntegerType<II>> extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(Lab2TableNodeModel.class);

    public static final String CFG_LABELING_COLUMN = "labeling_column";

    private final SettingsModelString m_labColumn = new SettingsModelString(CFG_LABELING_COLUMN, "");

    private DataTableSpec m_outSpec;

    /**
     * One input one output.
     */
    public Lab2TableNodeModel() {
        super(1, 1);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        getLabColIdx(inSpecs[0]);

        // create outspec according to the selected features
        final ArrayList<DataColumnSpec> specs = new ArrayList<DataColumnSpec>();
        specs.add(new DataColumnSpecCreator("Bitmask", ImgPlusCell.TYPE).createSpec());
        specs.add(new DataColumnSpecCreator("Label", StringCell.TYPE).createSpec());

        final DataColumnSpecCreator colspecCreator = new DataColumnSpecCreator("Source Labeling", LabelingCell.TYPE);
        colspecCreator.setProperties(new DataColumnProperties(Collections
                .singletonMap(DataValueRenderer.PROPERTY_PREFERRED_RENDERER, "String")));
        specs.add(colspecCreator.createSpec());

        m_outSpec = new DataTableSpec(specs.toArray(new DataColumnSpec[specs.size()]));
        return new DataTableSpec[]{m_outSpec};
    }

    /*
     * Helper to create a binary mask from a region of interest.
     */
    private Img<BitType> createBinaryMask(final IterableInterval<BitType> ii) {
        final Img<BitType> mask = new ArrayImgFactory<BitType>().create(ii, new BitType());

        final RandomAccess<BitType> maskRA = mask.randomAccess();
        final Cursor<BitType> cur = ii.localizingCursor();
        while (cur.hasNext()) {
            cur.fwd();
            for (int d = 0; d < cur.numDimensions(); d++) {
                maskRA.setPosition(cur.getLongPosition(d) - ii.min(d), d);
            }
            maskRA.get().set(true);
        }

        return mask;

    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        final BufferedDataContainer con = exec.createDataContainer(m_outSpec);
        final RowIterator it = inData[0].iterator();
        DataRow row;

        final int count = inData[0].getRowCount();
        int i = 0;
        final int labColIdx = getLabColIdx(inData[0].getDataTableSpec());
        final ImgPlusCellFactory imgCellFactory = new ImgPlusCellFactory(exec);
        while (it.hasNext()) {
            row = it.next();
            if (row.getCell(labColIdx).isMissing()) {
                setWarningMessage("Some missing cells have been ignored!");
                LOGGER.warn("Missing cell was ignored at row " + row.getKey());
                continue;
            }
            final LabelingValue<L> labVal = (LabelingValue<L>)row.getCell(labColIdx);
            final Labeling<L> lab = labVal.getLabeling();

            final List<L> labels = lab.firstElement().getMapping().getLabels();

            IterableInterval ii;
            for (final L label : labels) {

                ii =
                        lab.getIterableRegionOfInterest(label)
                                .getIterableIntervalOverROI(new ConstantRandomAccessible<BitType>(new BitType(), lab
                                                                    .numDimensions()));

                final List<DataCell> cells = new ArrayList<DataCell>();

                // segment image
                final long[] min = new long[ii.numDimensions()];

                for (int j = 0; j < min.length; j++) {
                    min[j] = ii.min(j);
                }
                final LabelingMetadata lmdata = labVal.getLabelingMetadata();
                final ImgPlusMetadata mdata =
                        new DefaultImgMetadata(lmdata, new DefaultNamed(label.toString()), new DefaultSourced(
                                lmdata.getName()), new DefaultImageMetadata());
                cells.add(imgCellFactory.createCell(createBinaryMask(ii), mdata, min));

                // Segment label
                cells.add(new StringCell(label.toString()));

                // source labeling
                cells.add(row.getCell(labColIdx));

                con.addRowToTable(new DefaultRow(row.getKey() + KNIPConstants.IMGID_LABEL_DELIMITER + label.toString(),
                        cells));

            }
            exec.checkCanceled();
            exec.setProgress((double)++i / count);
        }
        con.close();
        return new BufferedDataTable[]{con.getTable()};
    }

    private int getLabColIdx(final DataTableSpec inSpec) throws InvalidSettingsException {
        int idx = inSpec.findColumnIndex(m_labColumn.getStringValue());
        if (idx == -1) {
            idx = NodeTools.autoOptionalColumnSelection(inSpec, m_labColumn, LabelingValue.class);
            setWarningMessage("Auto-configure Labeling Column: " + m_labColumn.getStringValue());
        }
        if (idx == -1) {
            throw new InvalidSettingsException("No labeling column found.");
        }
        return idx;
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //

    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_labColumn.loadSettingsFrom(settings);

    }

    @Override
    protected void reset() {
        //

    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //

    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_labColumn.saveSettingsTo(settings);

    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_labColumn.validateSettings(settings);

    }

}
