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
import org.knime.core.data.collection.ListCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.ThreadPoolExecutorService;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.exceptions.KNIPRuntimeException;
import org.knime.knip.base.exceptions.LoggerHelper;

/**
 * 
 * Node Model to process table cells separately.
 * 
 * 
 * @param <VIN> the type of the input values
 * @param <COUT> the type of the output values
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class ValueToCellNodeModel<VIN extends DataValue, COUT extends DataCell> extends NodeModel implements
        BufferedDataTableHolder {

    /**
     * column creation modes
     */
    public static final String[] COL_CREATION_MODES = new String[]{"New Table", "Append", "Replace"};

    /*
     * Inport of the table to be processed
     */
    private static final int IN_TABLE_PORT_INDEX = 0;

    /*
     * Logging
     */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ValueToCellNodeModel.class);

    /**
     * @return the settings model for the column creation mode
     */
    public static SettingsModelString createColCreationModeModel() {
        return new SettingsModelString("column_creation_mode", COL_CREATION_MODES[0]);
    }

    /**
     * @return the node model for the column suffix
     */
    public static SettingsModelString createColSuffixNodeModel() {
        return new SettingsModelString("column_suffix", "");

    }

    /**
     * @return settings model for the column selection
     */
    public static SettingsModelFilterString createColumnSelectionModel() {
        return new SettingsModelFilterString("column_selection");
    }

    private static PortType[] createPortTypes(final PortType[] additionalPorts) {
        final PortType[] inPTypes = new PortType[additionalPorts.length + 1];
        inPTypes[0] = BufferedDataTable.TYPE;
        for (int i = 0; i < additionalPorts.length; i++) {
            inPTypes[i + 1] = additionalPorts[i];
        }
        return inPTypes;
    }

    /*
     * Settings for the column creation mode
     */
    private final SettingsModelString m_colCreationMode = createColCreationModeModel();

    /*
     * Settings for the column suffix.
     */
    private final SettingsModelString m_colSuffix = createColSuffixNodeModel();

    /*
     * Settings to store the selected columns.
     */
    private final SettingsModelFilterString m_columns = createColumnSelectionModel();

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
     * Class of the first argument type.
     */
    protected Class<VIN> m_inValueClass;

    /*
     * Number of occurred errors while processing all available cells
     */
    private int m_numOccurredErrors;

    /*
     * Class of the second argument type.
     */
    protected Class<COUT> m_outCellClass;

    /*
     * Collection of all settings model used.
     */
    protected List<SettingsModel> m_settingsModels = null;

    /**
     */
    protected ValueToCellNodeModel() {
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
    protected ValueToCellNodeModel(final PortType[] additionalPorts) {
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
    protected ValueToCellNodeModel(final PortType[] additionalInPorts, final PortType[] additionalOutPorts) {
        super(createPortTypes(additionalInPorts), createPortTypes(additionalOutPorts));
        getTypeArgumentClasses();
    }

    /**
     * Adds the settings model to be saved, load and validated.
     * 
     * @param settingsModels
     */
    protected abstract void addSettingsModels(List<SettingsModel> settingsModels);

    /* Helper to collect all columns of the given type. */
    private void collectAllColumns(final List<String> colNames, final DataTableSpec spec) {
        colNames.clear();
        for (final DataColumnSpec c : spec) {
            if (c.getType().isCompatible(m_inValueClass)) {
                colNames.add(c.getName());
            }
        }
        if (colNames.size() == 0) {
            LOGGER.warn("No columns of type " + m_inValueClass.getSimpleName() + " available!");
            return;
        }
        LOGGER.info("All available columns of type " + m_inValueClass.getSimpleName() + " are selected!");

    }

    /*
     * Helper to collect all settings models and add them to one list (if
     * not already done)
     */
    protected void collectSettingsModels() {
        if (m_settingsModels == null) {
            m_settingsModels = new ArrayList<SettingsModel>();
            m_settingsModels.add(m_colCreationMode);
            m_settingsModels.add(m_colSuffix);
            m_settingsModels.add(m_columns);
            addSettingsModels(m_settingsModels);
        }
    }

    /**
     * Processes one single data cell. Exactly one of the compute-methods has to be overwritten.
     * 
     * @param cellValue
     * @return
     * @throws Exception
     */
    protected abstract COUT compute(VIN cellValue) throws Exception;

    /**
     * Will be called everytime a new row is processed. Can be overwritten optionally. It is called before compute();
     * 
     * @param row
     */
    protected void computeDataRow(final DataRow row) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        final DataTableSpec inSpec = (DataTableSpec)inSpecs[IN_TABLE_PORT_INDEX];

        final int[] selectedColIndices = getSelectedColumnIndices(inSpec);
        final CellFactory cellFac = createCellFactory(inSpec, selectedColIndices);

        if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[0])) {
            return new DataTableSpec[]{new DataTableSpec(cellFac.getColumnSpecs())};
        }
        if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[1])) {
            final ColumnRearranger colRearranger = new ColumnRearranger(inSpec);
            colRearranger.append(cellFac);
            return new DataTableSpec[]{colRearranger.createSpec()};

        }
        final ColumnRearranger colRearranger = new ColumnRearranger(inSpec);
        colRearranger.replace(cellFac, selectedColIndices);
        return new DataTableSpec[]{colRearranger.createSpec()};
    }

    /**
     * Creates the cell factory.
     */
    protected CellFactory createCellFactory(final DataTableSpec inSpec, final int[] colIndices) {

        final DataColumnSpec[] cspecs = new DataColumnSpec[colIndices.length];

        DataType dt;
        if (getOutDataCellListCellType() != null) {
            dt = ListCell.getCollectionType(getOutDataCellListCellType());
            // dt = DataType.getType(m_outCellClass,
            // getOutDataCellListCellType());
        } else {
            dt = DataType.getType(m_outCellClass);
        }

        for (int i = 0; i < colIndices.length; i++) {
            final DataColumnSpec cs = inSpec.getColumnSpec(colIndices[i]);
            cspecs[i] = new DataColumnSpecCreator(cs.getName() + m_colSuffix.getStringValue(), dt).createSpec();
        }

        return new CellFactory() {

            @Override
            public DataCell[] getCells(final DataRow row) {
                computeDataRow(row);
                final DataCell[] cells = new DataCell[colIndices.length];

                for (int i = 0; i < colIndices.length; i++) {
                    try {

                        if ((row.getCell(colIndices[i]).isMissing())) {
                            LOGGER.warn("Missing cell was ignored at row " + row.getKey());
                            m_numOccurredErrors++;
                            cells[i] = DataType.getMissingCell();
                        } else {
                            DataCell c = compute(m_inValueClass.cast(row.getCell(colIndices[i])));
                            if (c == null) {
                                LOGGER.warn("Node didn't provide an output at row " + row.getKey()
                                        + ". Missing cell has been inserted.");
                                cells[i] = DataType.getMissingCell();
                            } else {
                                cells[i] = c;
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
                                LoggerHelper.log("Problem occured in row" + row.getKey(), LOGGER,
                                                 (KNIPRuntimeException)e, "Missing cell has been inserted");
                            } else {
                                LOGGER.error("Error in row " + row.getKey() + ": " + e.getMessage(), e);
                                throw new RuntimeException(e);
                            }
                            m_numOccurredErrors++;
                        }
                        cells[i] = DataType.getMissingCell();
                    }

                }
                return cells;
            }

            @Override
            public DataColumnSpec[] getColumnSpecs() {
                return cspecs;
            }

            @Override
            public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey,
                                    final ExecutionMonitor exec) {
                exec.setProgress((double)curRowNr / rowCount);
            }

        };
    }

    @Override
    public StreamableOperator
            createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
                    throws InvalidSettingsException {

        final DataTableSpec inSpec = (DataTableSpec)inSpecs[IN_TABLE_PORT_INDEX];

        final int[] selectedColIndices = getSelectedColumnIndices(inSpec);
        final CellFactory cellFac = createCellFactory(inSpec, selectedColIndices);

        if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[0])) {
            return new StreamableOperator() {

                @Override
                public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                        throws Exception {
                    final RowInput input = (RowInput)inputs[IN_TABLE_PORT_INDEX];
                    final RowOutput output = (RowOutput)outputs[0];
                    final DataRow row = input.poll();
                    output.push(new DefaultRow(row.getKey(), cellFac.getCells(row)));

                }
            };
        } else {

            final ColumnRearranger colRearranger = new ColumnRearranger(inSpec);
            if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[1])) {
                colRearranger.append(cellFac);
                return colRearranger.createStreamableFunction();

            } else {
                colRearranger.replace(cellFac, selectedColIndices);
                return colRearranger.createStreamableFunction();
            }

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

        final BufferedDataTable inTable = (BufferedDataTable)inObjects[IN_TABLE_PORT_INDEX];

        exec.setProgress("Initializing ...");
        prepareExecute(exec);

        m_numOccurredErrors = 0;

        BufferedDataTable[] res;

        final int[] selectedColIndices = getSelectedColumnIndices(inTable.getDataTableSpec());
        final CellFactory cellFac = createCellFactory(inTable.getDataTableSpec(), selectedColIndices);

        exec.setProgress("Processing ...");
        if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[0])) {
            final RowIterator it = inTable.iterator();
            final BufferedDataContainer con = exec.createDataContainer(new DataTableSpec(cellFac.getColumnSpecs()));
            DataRow row;
            final int rowCount = inTable.getRowCount();
            int i = 0;
            while (it.hasNext()) {
                row = it.next();
                con.addRowToTable(new DefaultRow(row.getKey(), cellFac.getCells(row)));
                exec.checkCanceled();
                cellFac.setProgress(i, rowCount, row.getKey(), exec);
                i++;
            }
            con.close();
            res = new BufferedDataTable[]{con.getTable()};

        } else {
            final ColumnRearranger colRearranger = new ColumnRearranger(inTable.getDataTableSpec());
            if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[1])) {
                colRearranger.append(cellFac);
            } else {
                colRearranger.replace(cellFac, selectedColIndices);
            }

            res = new BufferedDataTable[]{exec.createColumnRearrangeTable(inTable, colRearranger, exec)};
        }

        if (m_numOccurredErrors > 0) {
            setWarningMessage(m_numOccurredErrors + " errors occurred while executing!");
        }

        // data for the table cell view
        m_data = res[0];

        return res;

    }

    /**
     * Returns the {@link ExecutorService} which will be reseted after cancelation of the node. Use this
     * {@link ExecutorService} to submit new futures in your node.
     * 
     * @return
     */
    protected ExecutorService getExecutorService() {
        return m_executor;
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
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

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /*
     * Retrieves the selected column indices from the given DataTableSpec
     * and the column selection. If the selection turned out to be invalid,
     * all columns are selected.
     */
    protected int[] getSelectedColumnIndices(final DataTableSpec inSpec) {
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

    /*
     * Retrieves the classes of the type arguments VIN and COUT.
     */
    @SuppressWarnings("unchecked")
    protected void getTypeArgumentClasses() {

        // TODO: is there a better way??

        Class<?> c = getClass();
        for (int i = 0; i < 5; i++) {
            if (c.getSuperclass().equals(ValueToCellNodeModel.class)) {
                final Type[] types = ((ParameterizedType)c.getGenericSuperclass()).getActualTypeArguments();
                if (types[0] instanceof ParameterizedType) {
                    types[0] = ((ParameterizedType)types[0]).getRawType();
                }
                if (types[1] instanceof ParameterizedType) {
                    types[1] = ((ParameterizedType)types[1]).getRawType();
                }
                m_inValueClass = (Class<VIN>)types[0];
                m_outCellClass = (Class<COUT>)types[1];
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
            try {
                sm.loadSettingsFrom(settings);
            } catch (final InvalidSettingsException e) {
                LOGGER.warn("Problems occurred loading the settings " + sm.toString() + ": " + e.getLocalizedMessage());
                setWarningMessage("Problems occurred while loading settings.");
            }
        }

    }

    /**
     * Will be called before calling the {@link ValueToCellNodeModel#compute(DataValue)} multiple times. Has to be
     * overwritten if needed.
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_data = tables[0];

    }

    // // Methods for the table cell view ////

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
