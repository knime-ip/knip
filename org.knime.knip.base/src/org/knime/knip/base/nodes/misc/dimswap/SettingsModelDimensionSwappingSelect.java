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
package org.knime.knip.base.nodes.misc.dimswap;

import java.util.Arrays;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * TODO: Documentation
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @author Felix Schoenenberger (University of Konstanz)
 */
public class SettingsModelDimensionSwappingSelect extends SettingsModel {

    private static final String CFG_DIM_LUT = "cfg-dim-lut";

    private int[] m_backDimensionLookup;

    private final String m_configName;

    /**
     * Defaul Constructor
     *
     * @param configName name of this configuration
     * @param numDimensions number of dimensions
     */
    public SettingsModelDimensionSwappingSelect(final String configName, final int numDimensions) {
        m_configName = configName;
        m_backDimensionLookup = new int[numDimensions];
        for (int i = 0; i < numDimensions; i++) {
            m_backDimensionLookup[i] = i;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected <T extends SettingsModel> T createClone() {
        final SettingsModelDimensionSwappingSelect copy =
                new SettingsModelDimensionSwappingSelect(m_configName, numDimensions());
        copy.m_backDimensionLookup = m_backDimensionLookup.clone();
        return (T)copy;
    }

    public int getBackDimensionLookup(final int to) {
        return m_backDimensionLookup[to];
    }

    public int getFwdDimensionLookup(final int from) {
        for (int i = 0; i < m_backDimensionLookup.length; i++) {
            if (m_backDimensionLookup[i] == from) {
                return i;
            }
        }
        return -1;
    }

    public int numDimensions() {
        return m_backDimensionLookup.length;
    }

    public void setBackDimensionLookup(final int to, final int from) {
        m_backDimensionLookup[from] = to;
    }

    public void setFwdDimensionLookup(final int to, final int from) {
        m_backDimensionLookup[to] = from;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        try {
            loadSettingsForModel(settings);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final Config c = settings.getConfig(m_configName);
        m_backDimensionLookup = c.getIntArray(CFG_DIM_LUT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) {
        saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        final Config c = settings.addConfig(m_configName);
        c.addIntArray(CFG_DIM_LUT, m_backDimensionLookup);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getCanonicalName() + "" + m_configName + " " + Arrays.toString(m_backDimensionLookup);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) {
        //
    }
}
