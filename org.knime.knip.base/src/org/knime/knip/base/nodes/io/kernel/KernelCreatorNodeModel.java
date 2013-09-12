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
package org.knime.knip.base.nodes.io.kernel;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.node.nodesettings.BaseObjectExt0;
import org.knime.knip.base.node.nodesettings.SettingsModelSerializableObjects;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class KernelCreatorNodeModel<T extends RealType<T>> extends NodeModel implements BufferedDataTableHolder {

    @SuppressWarnings("rawtypes")
    protected static SettingsModelSerializableObjects<SerializableSetting> createKernelListModel() {
        return new SettingsModelSerializableObjects<SerializableSetting>("kernels", new BaseObjectExt0());
    }

    /*
     * data table for the table cell view
     */
    private BufferedDataTable m_data;

    @SuppressWarnings("rawtypes")
    private final SettingsModelSerializableObjects<SerializableSetting> m_kernelList = createKernelListModel();

    protected final Map<String, Class<?>> m_pool;

    private DataTableSpec m_spec;

    public KernelCreatorNodeModel(final Map<String, Class<?>> pool) {
        super(0, 1);
        m_pool = pool;
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        m_spec = new DataTableSpec(new DataColumnSpecCreator("Kernel", ImgPlusCell.TYPE).createSpec());
        return new DataTableSpec[]{m_spec};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        final BufferedDataContainer con = exec.createDataContainer(m_spec);
        final ImgPlusCellFactory imgCellFactory = new ImgPlusCellFactory(exec);
        int id = 1;
        for (final SerializableSetting<Img<T>[]> conf : m_kernelList.getObjects()) {
            for (final Img<T> img : conf.get()) {
                con.addRowToTable(new DefaultRow("Kernel " + id, imgCellFactory.createCell(new ImgPlus<T>(img))));
                id++;
            }
        }

        con.close();

        // data for the table cell view
        m_data = con.getTable();
        return new BufferedDataTable[]{m_data};
    }

    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_data};
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do here

    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_kernelList.loadSettingsFrom(settings);
    }

    @Override
    protected void reset() {
        m_data = null;
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do here

    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_kernelList.saveSettingsTo(settings);

    }

    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_data = tables[0];

    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_kernelList.validateSettings(settings);

    }

}
