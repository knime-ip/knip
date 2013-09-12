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
package org.knime.knip.base.node;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.ThreadPoolExecutorService;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.exceptions.KNIPRuntimeException;
import org.knime.knip.base.exceptions.LoggerHelper;

/**
 * 
 * Node Model to process table cells separately.
 * 
 * @param <VIN1> type of the first input value
 * @param <VIN2> type of the second input value
 * @param <COUT> the type of the output values
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class TwoValuesToCellNodeModel<VIN1 extends DataValue, VIN2 extends DataValue, COUT extends DataCell>
        extends NodeModel implements BufferedDataTableHolder {

    /*
     * Column creation modes
     */
    public static final String[] COL_CREATION_MODES = new String[]{"New Table", "Append", "Replace first",
            "Replace second"};

    /*
     * Inport of the table to be processed
     */
    private static final int IN_TABLE_PORT_INDEX = 0;

    /*
     * Logging
     */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(TwoValuesToCellNodeModel.class);

    static SettingsModelString createColCreationModeModel() {
        return new SettingsModelString("CFG_CREATION_MODE", COL_CREATION_MODES[0]);
    }

    static SettingsModelString createColSuffixModel() {
        return new SettingsModelString("CFG_COLUMN_SUFFIX", "");
    }

    static SettingsModelString createFirstColModel() {
        return new SettingsModelString("first_column_selection", "");
    }

    private static PortType[] createPortTypes(final PortType[] additionalPorts) {
        final PortType[] inPTypes = new PortType[additionalPorts.length + 1];
        inPTypes[0] = BufferedDataTable.TYPE;
        for (int i = 0; i < additionalPorts.length; i++) {
            inPTypes[i + 1] = additionalPorts[i];
        }
        return inPTypes;
    }

    static SettingsModelString createSecondColModel() {
        return new SettingsModelString("CFG_SECOND_COLUMN_SELECTION", "");
    }

    /*
     * Stores the column creation mode.
     */
    private final SettingsModelString m_colCreationMode = createColCreationModeModel();

    /*
     * Stores the column suffix
     */
    private final SettingsModelString m_colSuffix = createColSuffixModel();

    /*
     * data table for the table cell view
     */
    private BufferedDataTable m_data;

    /*
     * Execution service for multi threading purposes
     */
    private final ThreadPoolExecutorService m_executor = new ThreadPoolExecutorService(
            KNIMEConstants.GLOBAL_THREAD_POOL.createSubPool(KNIPConstants.THREADS_PER_NODE));

    /*
     * Stores the first selected column.
     */
    private final SettingsModelString m_firstColumn = createFirstColModel();

    /*
     * Class of the first argument type.
     */
    private Class<VIN1> m_firstInValClass;

    /*
     * Number of occurred errors while processing all available cells
     */
    private int m_numOccurredErrors;

    /*
     * Class of the third argument type (output class)
     */
    private Class<COUT> m_outCellClass;

    /*
     * Stores the second selected column.
     */
    private final SettingsModelString m_secondColumn = createSecondColModel();

    /*
     * Class of the second argument type.
     */
    private Class<VIN2> m_secondInValClass;

    /*
     * Collection of all settings models used.
     */
    private List<SettingsModel> m_settingsModels = null;

    /**
     * @param colSuffix suffix to be appended to the resulting column names
     * 
     */
    protected TwoValuesToCellNodeModel() {
        super(1, 1);
        getTypeArgumentClasses();

    }

    /**
     * Contructor to add additional in-ports to the node. The data table port used within this model has index 0! Hence,
     * all additionally added ports will have a index > 0.
     * 
     * If you want to overwrite the configure/execute-methods and still want to use the functionality provided by this
     * model, then make sure to overwrite the right methods:
     * 
     * {@link #configure(PortObjectSpec[])} and {@link #execute(PortObject[], ExecutionContext)}, and don't forget to
     * call super.... .
     * 
     * @param additionalPorts specifies additional ports
     */
    protected TwoValuesToCellNodeModel(final PortType[] additionalPorts) {
        super(createPortTypes(additionalPorts), new PortType[]{BufferedDataTable.TYPE});
        getTypeArgumentClasses();
    }

    /**
     * Contructor to add additional in-ports and out-ports to the node. The data table port used within this model has
     * index 0! Hence, all additionally added ports will have a index > 0.
     * 
     * If you want to overwrite the configure/execute-methods and still want to use the functionality provided by this
     * model, then make sure to overwrite the right methods:
     * 
     * {@link #configure(PortObjectSpec[])} and {@link #execute(PortObject[], ExecutionContext)}, and don't forget to
     * call super.... .
     * 
     * @param additionalInPorts specifies additional in-ports
     * @param additionalOutPorts specifies additional out-ports
     */
    protected TwoValuesToCellNodeModel(final PortType[] additionalInPorts, final PortType[] additionalOutPorts) {
        super(createPortTypes(additionalInPorts), createPortTypes(additionalOutPorts));
        getTypeArgumentClasses();
    }

    /**
     * Adds the settings model to be saved, load and validated.
     * 
     * @param settingsModels
     */
    protected abstract void addSettingsModels(List<SettingsModel> settingsModels);

    /*
     * Helper to collect all settings models and add them to one list (if
     * not already done)
     */
    private void collectSettingsModels() {
        if (m_settingsModels == null) {
            m_settingsModels = new ArrayList<SettingsModel>();
            m_settingsModels.add(m_colCreationMode);
            m_settingsModels.add(m_colSuffix);
            m_settingsModels.add(m_firstColumn);
            m_settingsModels.add(m_secondColumn);
            addSettingsModels(m_settingsModels);
        }
    }

    /**
     * Processes two cells in combination.
     * 
     * @param cellValue1
     * @param cellValue2
     * 
     * @return
     * @throws Exception
     */
    protected abstract COUT compute(VIN1 cellValue1, VIN2 cellValue2) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        final DataTableSpec inSpec = (DataTableSpec)inSpecs[IN_TABLE_PORT_INDEX];

        final int[] colIndices = getColIndices(inSpec);
        final CellFactory cellFac = createCellFactory(inSpec, colIndices);
        ColumnRearranger colRearranger;

        if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[0])) {
            return new DataTableSpec[]{new DataTableSpec(cellFac.getColumnSpecs())};
        } else if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[1])) {
            colRearranger = new ColumnRearranger(inSpec);
            colRearranger.append(cellFac);
            return new DataTableSpec[]{colRearranger.createSpec()};
        } else if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[2])) {
            colRearranger = new ColumnRearranger(inSpec);
            colRearranger.replace(cellFac, colIndices[0]);
            return new DataTableSpec[]{colRearranger.createSpec()};
        } else {
            colRearranger = new ColumnRearranger(inSpec);
            colRearranger.replace(cellFac, colIndices[1]);
            return new DataTableSpec[]{colRearranger.createSpec()};
        }
    }

    /**
     * Creates the cell factory
     * 
     * @param inSpec
     * @param colIndices selected column indices
     * @return the cell factory
     */
    protected CellFactory createCellFactory(final DataTableSpec inSpec, final int[] colIndices) {

        DataType dt;
        if (getOutDataCellListCellType() != null) {
            dt = DataType.getType(m_outCellClass, getOutDataCellListCellType());
        } else {
            dt = DataType.getType(m_outCellClass);
        }

        DataColumnSpec cs1, cs2;
        String cs1Name = null, cs2Name = null;

        if (colIndices[0] > -1) {
            cs1 = inSpec.getColumnSpec(colIndices[0]);
            cs1Name = cs1.getName();
        }

        if (colIndices[1] > -1) {
            cs2 = inSpec.getColumnSpec(colIndices[1]);
            cs2Name = cs2.getName();
        }

        final DataColumnSpec csRes =
                new DataColumnSpecCreator((cs1Name != null ? cs1Name : "") + (cs2Name != null ? "_" + cs2Name : "")
                        + m_colSuffix.getStringValue(), dt).createSpec();

        return new CellFactory() {
            @Override
            public DataCell[] getCells(final DataRow row) {
                DataCell[] cells;
                try {

                    if ((((colIndices[0] > -1) && (row.getCell(colIndices[0]).isMissing())) || ((colIndices[1] > -1) && row
                            .getCell(colIndices[1]).isMissing()))) {
                        LOGGER.warn("Missing cell was ignored at row " + row.getKey());

                        m_numOccurredErrors++;
                        cells = new DataCell[]{DataType.getMissingCell()};
                    } else {
                        DataCell c =
                                compute(colIndices[0] > -1 ? m_firstInValClass.cast(row.getCell(colIndices[0])) : null,
                                        colIndices[1] > -1 ? m_secondInValClass.cast(row.getCell(colIndices[1])) : null);
                        if (c == null) {
                            LOGGER.warn("Node didn't provide an output at row " + row.getKey()
                                    + ". Missing cell has been inserted.");
                            cells = new DataCell[]{DataType.getMissingCell()};
                        } else {
                            cells = new DataCell[]{c};
                        }
                    }
                } catch (final Exception e) {
                    if (e.getCause() instanceof InterruptedException) {
                        LOGGER.warn("Interrupted node");
                        m_executor.getThreadPool().interruptAll();
                    } else {
                        if (e instanceof KNIPException) {
                            LoggerHelper.log("Problem occured in row" + row.getKey(), LOGGER, (KNIPException)e,
                                             "Missing cell has been inserted");
                        } else if (e instanceof KNIPRuntimeException) {
                            LoggerHelper.log("Problem occured in row" + row.getKey(), LOGGER, (KNIPRuntimeException)e,
                                             "Missing cell has been inserted");
                        } else {
                            LOGGER.error("Error in row " + row.getKey() + ": " + e.getMessage(), e);
                            throw new RuntimeException(e);
                        }
                        m_numOccurredErrors++;
                    }
                    cells = new DataCell[]{DataType.getMissingCell()};
                }
                return cells;

            }

            @Override
            public DataColumnSpec[] getColumnSpecs() {
                return new DataColumnSpec[]{csRes};
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
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

        final BufferedDataTable inTable = (BufferedDataTable)inObjects[IN_TABLE_PORT_INDEX];

        m_numOccurredErrors = 0;

        prepareExecute(exec);

        BufferedDataTable[] res;

        final int[] colIndices = getColIndices(inTable.getDataTableSpec());

        final CellFactory cellFac = createCellFactory(inTable.getDataTableSpec(), colIndices);

        ColumnRearranger colRearranger;

        if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[0])) {
            final RowIterator it = inTable.iterator();
            final BufferedDataContainer con = exec.createDataContainer(new DataTableSpec(cellFac.getColumnSpecs()));
            DataRow row;
            final int rowCount = inTable.getRowCount();
            int i = 0;
            while (it.hasNext()) {
                row = it.next();
                con.addRowToTable(new DefaultRow(row.getKey(), cellFac.getCells(row)));
                cellFac.setProgress(i, rowCount, row.getKey(), exec);
                exec.checkCanceled();
                i++;
            }
            con.close();
            res = new BufferedDataTable[]{con.getTable()};

        } else if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[1])) {
            colRearranger = new ColumnRearranger(inTable.getDataTableSpec());
            colRearranger.append(cellFac);
            res = new BufferedDataTable[]{exec.createColumnRearrangeTable(inTable, colRearranger, exec)};
        } else if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[2])) {
            colRearranger = new ColumnRearranger(inTable.getDataTableSpec());
            colRearranger.replace(cellFac, colIndices[0]);
            res = new BufferedDataTable[]{exec.createColumnRearrangeTable(inTable, colRearranger, exec)};
        } else {
            colRearranger = new ColumnRearranger(inTable.getDataTableSpec());
            colRearranger.replace(cellFac, colIndices[1]);
            res = new BufferedDataTable[]{exec.createColumnRearrangeTable(inTable, colRearranger, exec)};
        }

        if (m_numOccurredErrors > 0) {
            setWarningMessage(m_numOccurredErrors + " errors occurred while executing!");
        }

        // data table for the view
        m_data = res[0];

        return res;

    }

    protected int[] getColIndices(final DataTableSpec inSpec) throws InvalidSettingsException {

        int tmpFirstColIdx = -1;
        if (m_firstColumn.getStringValue() != null) {
            tmpFirstColIdx = NodeTools.autoColumnSelection(inSpec, m_firstColumn, m_firstInValClass, this.getClass());
        }

        int tmpSecondColIdx = -1;
        if (m_secondColumn.getStringValue() != null) {
            // try to find something in another row
            tmpSecondColIdx =
                    NodeTools.autoColumnSelection(inSpec, m_secondColumn, m_secondInValClass, this.getClass(),
                                                  tmpFirstColIdx);
        }

        return new int[]{tmpFirstColIdx, tmpSecondColIdx};
    }

    /**
     * Returns the {@link ExecutorService} which will be reseted after cancelation of the node. Use this
     * {@link ExecutorService} to submit new futures in your node.
     * 
     * @return
     */
    public ExecutorService getExecutorService() {
        return m_executor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_data};
    }

    /**
     * The cell type that is stored if the out-cell is a ListCell
     * 
     * @return
     */
    protected DataType getOutDataCellListCellType() {
        return null;
    }

    /*
     * Retrieves the classes of the type arguments VIN1, VIN2 and COUT.
     */
    @SuppressWarnings("unchecked")
    private void getTypeArgumentClasses() {

        Class<?> c = getClass();
        for (int i = 0; i < 5; i++) {
            if (c.getSuperclass().equals(TwoValuesToCellNodeModel.class)) {
                final Type[] types = ((ParameterizedType)c.getGenericSuperclass()).getActualTypeArguments();
                if (types[0] instanceof ParameterizedType) {
                    types[0] = ((ParameterizedType)types[0]).getRawType();
                }
                if (types[1] instanceof ParameterizedType) {
                    types[1] = ((ParameterizedType)types[1]).getRawType();
                }

                if (types[2] instanceof ParameterizedType) {
                    types[2] = ((ParameterizedType)types[2]).getRawType();
                }
                m_firstInValClass = (Class<VIN1>)types[0];
                m_secondInValClass = (Class<VIN2>)types[1];
                m_outCellClass = (Class<COUT>)types[2];
                break;
            }
            c = c.getSuperclass();
        }
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
        collectSettingsModels();
        for (final SettingsModel sm : m_settingsModels) {
            sm.loadSettingsFrom(settings);
        }

    }

    /**
     * Will be called before calling the {@link TwoValuesToCellNodeModel#compute(DataValue, DataValue)} multiple times.
     * Has to be overwritten if needed.
     */
    protected void prepareExecute(@SuppressWarnings("unused") final ExecutionContext exec) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_data = null;
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
        collectSettingsModels();
        for (final SettingsModel sm : m_settingsModels) {
            sm.saveSettingsTo(settings);
        }

    }

    // Methods for the table cell view //

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
        collectSettingsModels();
        for (final SettingsModel sm : m_settingsModels) {
            try {
                sm.validateSettings(settings);
            } catch (final InvalidSettingsException e) {

                LOGGER.warn("Problems occurred validating the settings " + sm.toString() + ": "
                        + e.getLocalizedMessage());
                setWarningMessage("Problems occurred while validating settings.");
            }
        }

    }

}
