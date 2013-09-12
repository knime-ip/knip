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
 * ---------------------------------------------------------------------
 *
 * Created on Sep 9, 2013 by dietzc
 */
package org.knime.knip.base.nodes.seg.colormanager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.property.ColorAttr;
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
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeTools;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;

/**
 * Apply information of ColorModel (which is appended to column) to the LabelingColorTables of the Labelings in the
 * column.
 * 
 * @param <L>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ApplyColorSettingsToLabelsNodeModel<L extends Comparable<L>> extends NodeModel implements
        BufferedDataTableHolder {

    /**
     * First port = Labelings. Second port = Table with column to be mapped.
     */
    protected ApplyColorSettingsToLabelsNodeModel() {
        super(2, 1);
    }

    private final static NodeLogger LOGGER = NodeLogger.getLogger(ApplyColorSettingsToLabelsNodeModel.class);

    private SettingsModelBoolean m_overrideExistingModel = createOverrideExistingColorTablesModel();

    private SettingsModelString m_labelingColModel = createLabelingColumnModel();

    private BufferedDataTable m_resTable;

    private int m_colHandlerColIdx = -1;

    private int m_selectedColumn;

    /**
     * 
     * SettingsModel to store if existing table should be overriden or not
     * 
     * @return
     */
    protected static SettingsModelBoolean createOverrideExistingColorTablesModel() {
        return new SettingsModelBoolean("override_existing", false);
    }

    /**
     * SettingsModel to store selected labeling column
     * 
     * @return
     */
    protected static SettingsModelString createLabelingColumnModel() {
        return new SettingsModelString("labeling_column", "-1");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        String colName = m_labelingColModel.getStringValue();
        m_selectedColumn = inSpecs[0].findColumnIndex(colName);

        if (m_selectedColumn == -1) {
            m_selectedColumn =
                    NodeTools.autoColumnSelection(inSpecs[0], m_labelingColModel, LabelingValue.class,
                                                  ApplyColorSettingsToLabelsNodeModel.class);
        }

        for (int i = 0; i < inSpecs[1].getNumColumns(); i++) {
            if ((inSpecs[1].getColumnSpec(i).getColorHandler()) != null) {
                m_colHandlerColIdx = i;
                break;
            }
        }

        if (m_colHandlerColIdx == -1) {
            throw new IllegalArgumentException("No column with attached color manager found! "
                    + "Use ColorManager to append color information to a column.");
        }

        return new DataTableSpec[]{inSpecs[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        // create mapping
        HashMap<String, Integer> mapping = new HashMap<String, Integer>();

        CloseableRowIterator colorIterator = inData[1].iterator();
        while (colorIterator.hasNext()) {
            DataRow row = colorIterator.next();
            DataCell cell = row.getCell(m_colHandlerColIdx);

            String key = "";
            if (!mapping.containsKey((key = cell.toString()))) {
                mapping.put(key, inData[1].getDataTableSpec().getRowColor(row).getColor().getRGB());
            }
        }

        LabelingCellFactory factory = new LabelingCellFactory(exec);

        BufferedDataContainer container = exec.createDataContainer(inData[0].getDataTableSpec());

        CloseableRowIterator iterator = inData[0].iterator();

        while (iterator.hasNext()) {
            DataRow row = iterator.next();
            DataCell[] cells = new DataCell[row.getNumCells()];

            for (int i = 0; i < row.getNumCells(); i++) {

                if (i == m_selectedColumn) {
                    LabelingValue<L> labVal = (LabelingValue<L>)(row.getCell(m_selectedColumn));
                    DefaultLabelingMetadata resMetadata = new DefaultLabelingMetadata(labVal.getLabelingMetadata());

                    if (m_overrideExistingModel.getBooleanValue()) {
                        resMetadata.setLabelingColorTable(new DefaultLabelingColorTable());
                    }

                    for (L label : labVal.getLabeling().getLabels()) {
                        Integer color = null;
                        if ((color = mapping.get(label.toString())) == null) {
                            LOGGER.warn("Couldn't find color for label "
                                    + label.toString()
                                    + ". Setting color for label to default. Please make sure, that input table contains exactly the same labels as the labelings!");
                            color = ColorAttr.DEFAULT.getColor().getRGB();
                        }

                        resMetadata.getLabelingColorTable().setColor(label, color);

                    }

                    cells[i] = factory.createCell(labVal.getLabeling(), resMetadata);
                } else {
                    cells[i] = row.getCell(i);
                }
            }

            container.addRowToTable(new DefaultRow(row.getKey(), cells));
        }

        container.close();

        // data for the table cell view
        m_resTable = container.getTable();

        return new BufferedDataTable[]{m_resTable};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_labelingColModel.saveSettingsTo(settings);
        m_overrideExistingModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_labelingColModel.validateSettings(settings);
        m_overrideExistingModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_labelingColModel.loadSettingsFrom(settings);
        m_overrideExistingModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_resTable = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_resTable};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_resTable = tables[0];
    }

}
