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
package org.knime.knip.base.nodes.view.segmentoverlay;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeTools;
import org.knime.knip.core.util.StringTransformer;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SegmentOverlayNodeModel<T extends RealType<T>, L extends Comparable<L>> extends NodeModel implements
        BufferedDataTableHolder {

    private class ClearableTableContentModel extends TableContentModel {
        /** */
        private static final long serialVersionUID = 1L;

        // make clearCache accessible to allow clearing before the
        // content model is thrown away
        // in order to free large image resources from the java swing
        // caching strategy
        public void clearCacheBeforeClosing() {
            clearCache();
        }

    }

    enum LabelTransformVariables {
        ImgName, ImgSource, Label, LabelingName, LabelingSource, RowID;
    };

    public static final String CFG_ADJUST_VIRTUALLY = "cfg_adjust_virtually";

    public static final String CFG_EXPRESSION = "cfg_expression";

    public static final String CFG_IMG_COL = "cfg_img_col";

    public static final String CFG_LABELING_COL = "cfg_seg_col";

    public static final String CFG_SRC_IMG_ID_COL = "cfg_src_img_id_col";

    public static final int COL_IDX_IMAGE = 0;

    public static final int COL_IDX_LABELING = 1;

    public static final int COL_IDX_SINGLE_LABELING = 0;

    /*
     * Logging
     */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(SegmentOverlayNodeModel.class);

    public static int PORT_IMG = 0;

    public static int PORT_SEG = 1;

    private final SettingsModelBoolean m_adjustVirtually = new SettingsModelBoolean(CFG_ADJUST_VIRTUALLY, true);

    private ClearableTableContentModel m_contentModel;

    private final SettingsModelString m_expression = new SettingsModelString(CFG_EXPRESSION, "$"
            + LabelTransformVariables.Label + "$");

    private final SettingsModelString m_imgCol = new SettingsModelString(CFG_IMG_COL, "");

    private BufferedDataTable m_imgTable;

    private boolean m_isDataSetToModel;

    private final SettingsModelString m_labelingCol = new SettingsModelString(CFG_LABELING_COL, "");

    private BufferedDataTable m_segTable;

    protected SegmentOverlayNodeModel() {
        super(new PortType[]{new PortType(BufferedDataTable.class), new PortType(BufferedDataTable.class, true)},
                new PortType[0]);
        m_contentModel = new ClearableTableContentModel();
        m_isDataSetToModel = false;
    }

    private boolean areCompatible(final LabelingValue<L> labVal, final ImgPlusValue<T> imgPlusVal) {
        return Arrays.equals(labVal.getDimensions(), imgPlusVal.getDimensions());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        NodeTools.autoColumnSelection(inSpecs[0], m_labelingCol, LabelingValue.class, this.getClass());

        if (m_imgCol.getStringValue() == null || m_imgCol.getStringValue().length() == 0) {
            NodeTools.autoOptionalColumnSelection(inSpecs[0], m_imgCol, ImgPlusValue.class);
        } else {
            NodeTools.silentOptionalAutoColumnSelection(inSpecs[0], m_imgCol, ImgPlusValue.class);
        }

        return new DataTableSpec[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        assert (inData != null);
        assert (inData.length >= 1);

        m_imgTable = inData[PORT_IMG];
        assert (m_imgTable != null);

        m_isDataSetToModel = true;
        m_imgTable = inData[PORT_IMG];
        m_segTable = inData[PORT_SEG];

        final int imgColIdx = inData[0].getDataTableSpec().findColumnIndex(m_imgCol.getStringValue());
        final int labelingColIdx = inData[0].getDataTableSpec().findColumnIndex(m_labelingCol.getStringValue());

        DataTableSpec spec;
        if (imgColIdx != -1) {
            spec =
                    new DataTableSpec(DataTableSpec.createColumnSpecs(new String[]{"Image", "Labeling"},
                                                                      new DataType[]{ImgPlusCell.TYPE,
                                                                              LabelingCell.TYPE}));
        } else {
            spec =
                    new DataTableSpec(DataTableSpec.createColumnSpecs(new String[]{"Labeling"},
                                                                      new DataType[]{LabelingCell.TYPE}));
        }
        final BufferedDataContainer con = exec.createDataContainer(spec);
        final RowIterator imgIt = m_imgTable.iterator();

        DataRow row;

        if (m_imgTable.getRowCount() == 0) {
            return new BufferedDataTable[0];
        }

        int rowCount = 0;
        while (imgIt.hasNext()) {
            row = imgIt.next();

            // load
            final DataCell labCell = row.getCell(labelingColIdx);
            final DataCell imgCell = imgColIdx != -1 ? row.getCell(imgColIdx) : null;

            // test for missing cells
            if (labCell.isMissing() || ((imgColIdx != -1) && imgCell.isMissing())) {
                LOGGER.warn("Missing cell was ignored at row " + row.getKey());
            } else {
                // process
                if (imgColIdx != -1) {
                    // check compatibility
                    if (areCompatible((LabelingValue<L>)labCell, (ImgPlusCell<T>)imgCell)
                            && !m_adjustVirtually.getBooleanValue()) {
                        setWarningMessage("The dimensions are not compatible in row " + row.getKey());
                    }

                    con.addRowToTable(new DefaultRow(row.getKey(), imgCell, labCell));
                } else {
                    con.addRowToTable(new DefaultRow(row.getKey(), labCell));
                }

            }

            exec.checkCanceled();
            exec.setProgress((double)rowCount++ / m_imgTable.getRowCount());

        }
        con.close();
        m_contentModel.setDataTable(con.getTable());

        return new BufferedDataTable[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {

        if (m_segTable != null) {
            return new BufferedDataTable[]{(BufferedDataTable)m_contentModel.getDataTable(), m_segTable};
        } else {
            return new BufferedDataTable[]{(BufferedDataTable)m_contentModel.getDataTable()};
        }
    }

    public TableContentModel getTableContentModel() {
        // temporary workaround since setDataTable blocks
        if (!m_isDataSetToModel) {
            m_contentModel.setDataTable(m_imgTable);
            m_isDataSetToModel = true;
        }
        return m_contentModel;
    }

    public StringTransformer getTransformer() {
        return new StringTransformer(m_expression.getStringValue(), "$");
    }

    public boolean isTransformationActive() {
        // is not active == "$Label$" || ""
        final String expression = m_expression.getStringValue().trim();
        return !((expression.equalsIgnoreCase("$" + LabelTransformVariables.Label + "$")) || expression.isEmpty());
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
        m_imgCol.loadSettingsFrom(settings);
        m_labelingCol.loadSettingsFrom(settings);
        m_adjustVirtually.loadSettingsFrom(settings);

        try {
            m_expression.loadSettingsFrom(settings);
        } catch (final Exception e) {
            //
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_imgTable = null;
        m_segTable = null;
        m_contentModel.clearCacheBeforeClosing();
        m_contentModel = new ClearableTableContentModel();
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
        m_imgCol.saveSettingsTo(settings);
        m_labelingCol.saveSettingsTo(settings);
        m_adjustVirtually.saveSettingsTo(settings);
        m_expression.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        if ((tables.length != 1) && (tables.length != 2)) {
            throw new IllegalArgumentException();
        }

        // TODO: make workaround unnecessary
        // temporary workaround since setDataTable blocks
        // m_tableModel.setDataTable(tables[0]);
        m_imgTable = tables[0];

        if (tables.length > 1) {
            m_segTable = tables[1];
        }

        // HiLiteHandler inProp = getInHiLiteHandler(INPORT);
        // m_contModel.setHiLiteHandler(inProp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_imgCol.validateSettings(settings);
        m_labelingCol.validateSettings(settings);
        m_adjustVirtually.validateSettings(settings);

        try {
            m_expression.validateSettings(settings);
        } catch (final Exception e) {
            //
        }
    }

    public boolean virtuallyAdjustImgs() {
        return m_adjustVirtually.getBooleanValue();
    };

}
