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
package org.knime.knip.base.nodes.view.imgparadjust;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.knip.base.data.img.ImgPlusValue;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgParAdjustNodeModel<T extends RealType<T>> extends NodeModel {

    private class Semaphore {
        private boolean m_state;

        public Semaphore(final boolean state) {
            setState(state);
        }

        /**
         * @return the state
         */
        public boolean getState() {
            return m_state;
        }

        /**
         * @param state the state to set
         */
        public void setState(final boolean state) {
            m_state = state;
        }
    }

    /*
     * semaphore to interrupt the execution of the node to wait for the
     * interactive setting of the contrast enhancement parameters
     */
    private final Semaphore m_continueExec = new Semaphore(false);

    /*
     * the table used by the node view
     */
    private BufferedDataTable m_imageTable;

    /**
     * @param nrInDataPorts
     * @param nrOutDataPorts
     */
    protected ImgParAdjustNodeModel() {
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        return null;
    }

    private void continueExecution(final Semaphore semaphore) {
        m_imageTable = null;
        semaphore.setState(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        /*
         * Initialising the table for the view
         */
        // getting/creating the table consisting of just one column

        final DataTableSpec inSpec = inData[0].getDataTableSpec();
        final List<Integer> tmp = new ArrayList<Integer>(inSpec.getNumColumns());
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            if (inSpec.getColumnSpec(i).getType().isCompatible(ImgPlusValue.class)) {
                tmp.add(i);
            }
        }
        final int[] indicesToKeep = new int[tmp.size()];
        for (int i = 0; i < indicesToKeep.length; i++) {
            indicesToKeep[i] = tmp.get(i);
        }

        final ColumnRearranger rearranger = new ColumnRearranger(inData[0].getDataTableSpec());

        rearranger.keepOnly(indicesToKeep);
        m_imageTable = exec.createBufferedDataTable(inData[0], exec.createSubProgress(.1));
        stateChanged();

        /*
         * Waiting for the user (in the node view) till the
         * setContrast... method is called
         */
        exec.setMessage("Waiting for user input (node view)");
        suspendExecution(m_continueExec, exec);
        exec.setMessage("");

        return null;

    }

    /**
     * @return the image table containing the selected image column (for the node view)
     */
    public DataTable getImageTable() {
        return m_imageTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    /**
     * Add/overwrite flow variable.
     * 
     * @param name
     * @param value
     */
    public void pushFlowVariable(final String name, final double value) {
        pushFlowVariableDouble(name, value);
        continueExecution(m_continueExec);
    }

    /**
     * Add/overwrite flow variable.
     * 
     * @param name
     * @param value
     */
    public void pushFlowVariable(final String name, final int value) {
        pushFlowVariableInt(name, value);
        continueExecution(m_continueExec);
    }

    /**
     * Add/overwrite flow variable.
     * 
     * @param name
     * @param value
     */
    public void pushFlowVariable(final String name, final String value) {
        pushFlowVariableString(name, value);
        continueExecution(m_continueExec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_imageTable = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    private void suspendExecution(final Semaphore semaphore, final ExecutionContext exec) throws InterruptedException,
            CanceledExecutionException {
        while (!semaphore.getState()) {
            try {
                Thread.sleep(1000);
                exec.checkCanceled();
            } catch (final CanceledExecutionException e) {
                reset();
                stateChanged();
                throw e;

            } catch (final InterruptedException e) {
                reset();
                stateChanged();
                throw e;
            }

        }
        semaphore.setState(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

    }

}
