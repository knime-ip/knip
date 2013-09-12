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
package org.knime.knip.base.nodes.testing;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowIterator;
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
import org.knime.knip.base.node.NodeTools;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class ComparatorNodeModel<VIN1 extends DataValue, VIN2 extends DataValue> extends NodeModel {

    /*
     * Logging
     */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ComparatorNodeModel.class);

    static SettingsModelString createFirstColModel() {
        return new SettingsModelString("first_column_selection", "");
    }

    static SettingsModelString createSecondColModel() {
        return new SettingsModelString("CFG_SECOND_COLUMN_SELECTION", "");
    }

    /*
     * Stores the first selected column.
     */
    private final SettingsModelString m_firstColumn = createFirstColModel();

    /*
     * Stores the second selected column.
     */
    private final SettingsModelString m_secondColumn = createSecondColModel();

    /*
     * Class of the first argument type.
     */
    private Class<VIN1> m_firstInValClass;

    /*
     * Class of the second argument type.
     */
    private Class<VIN2> m_secondInValClass;

    /*
     * Number of occurred errors while processing all available cells
     */
    private int m_numOccurredErrors;

    /*
     * Inport of the table to be processed
     */
    private static final int IN_TABLE_PORT_INDEX = 0;

    private int[] m_colIndices;

    protected ComparatorNodeModel() {
        super(1, 0);
        getTypeArgumentClasses();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        m_colIndices = getColIndices(inSpecs[0]);

        if ((m_colIndices[0] < 0) || (m_colIndices[1] < 0) || (m_colIndices[0] == m_colIndices[1])) {
            throw new InvalidSettingsException(
                    "Comparator node is not correct configured! Ensure that there is at least one column for each input type.");
        }

        return new DataTableSpec[]{};
    }

    private int[] getColIndices(final DataTableSpec inSpec) throws InvalidSettingsException {

        int tmpFirstColIdx = -1;
        if (m_firstColumn.getStringValue() != null) {
            tmpFirstColIdx = NodeTools.autoColumnSelection(inSpec, m_firstColumn, m_firstInValClass, this.getClass());
        }

        int tmpSecondColIdx = -1;
        final Integer[] except = new Integer[]{-1};
        if (m_firstInValClass == m_secondInValClass) {
            // if both columns have the same type search a 2nd
            // column
            // don't use the same one
            except[0] = tmpFirstColIdx;
        }
        if (m_secondColumn.getStringValue() != null) {
            tmpSecondColIdx =
                    NodeTools.autoColumnSelection(inSpec, m_secondColumn, m_secondInValClass, this.getClass(), except);
        }

        return new int[]{tmpFirstColIdx, tmpSecondColIdx};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inObjects, final ExecutionContext exec)
            throws Exception {

        final BufferedDataTable inTable = inObjects[IN_TABLE_PORT_INDEX];
        m_numOccurredErrors = 0;

        final RowIterator it = inTable.iterator();
        DataRow row;

        while (it.hasNext()) {
            row = it.next();

            if (row.getCell(m_colIndices[0]).isMissing() && row.getCell(m_colIndices[1]).isMissing()) {
                // both are missing => thats equal => do
                // nothing
            } else if (row.getCell(m_colIndices[0]).isMissing() && row.getCell(m_colIndices[1]).isMissing()) {
                // only one is missing => exception
                LOGGER.warn("comparing " + row.getCell(m_colIndices[0]) + " with " + row.getCell(m_colIndices[1])
                        + " failed because one of the two cells is missing");
                m_numOccurredErrors++;
                throw new IllegalStateException("Two DataValues are not the same");
            } else {
                // compare them
                VIN1 in1 = null;
                VIN2 in2 = null;
                try {
                    in1 = m_firstInValClass.cast(row.getCell(m_colIndices[0]));
                    in2 = m_secondInValClass.cast(row.getCell(m_colIndices[1]));
                    compare(in1, in2);
                } catch (final Exception e) {
                    LOGGER.warn("comparing " + in1 + " with " + in2 + " failed with the exception\n" + e.toString()
                            + " in row " + row.getKey());
                    m_numOccurredErrors++;
                    throw e;
                }
            }
            exec.checkCanceled();
        }

        if (m_numOccurredErrors > 0) {
            setWarningMessage(m_numOccurredErrors + " errors occurred while executing!");
        }

        return new BufferedDataTable[]{};
    }

    protected abstract void compare(VIN1 vin1, VIN2 vin2);

    /*
     * Retrieves the classes of the type arguments VIN1, VIN2
     */
    @SuppressWarnings("unchecked")
    private void getTypeArgumentClasses() {

        Class<?> c = getClass();
        for (int i = 0; i < 5; i++) {
            if (c.getSuperclass().equals(ComparatorNodeModel.class)) {
                final Type[] types = ((ParameterizedType)c.getGenericSuperclass()).getActualTypeArguments();
                if (types[0] instanceof ParameterizedType) {
                    types[0] = ((ParameterizedType)types[0]).getRawType();
                }
                if (types[1] instanceof ParameterizedType) {
                    types[1] = ((ParameterizedType)types[1]).getRawType();
                }
                m_firstInValClass = (Class<VIN1>)types[0];
                m_secondInValClass = (Class<VIN2>)types[1];
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
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_firstColumn.saveSettingsTo(settings);
        m_secondColumn.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_firstColumn.validateSettings(settings);
        m_secondColumn.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_firstColumn.loadSettingsFrom(settings);
        m_secondColumn.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

}
