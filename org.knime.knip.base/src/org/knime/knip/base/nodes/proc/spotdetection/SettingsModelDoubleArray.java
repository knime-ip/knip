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
package org.knime.knip.base.nodes.proc.spotdetection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SettingsModelDoubleArray extends SettingsModel {

    private final String m_configName;

    private double[] m_value;

    /**
     * Creates a new object holding a double[] value.
     * 
     * @param configName the identifier the value is stored with in the {@link org.knime.core.node.NodeSettings} object
     * @param defaultValue the initial value
     */
    public SettingsModelDoubleArray(final String configName, final double[] defaultValue) {
        if ((configName == null) || "".equals(configName)) {
            throw new IllegalArgumentException("The configName must be a " + "non-empty string");
        }
        m_value = defaultValue.clone();
        m_configName = configName;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelDoubleArray createClone() {
        return new SettingsModelDoubleArray(m_configName, m_value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * @return the (a copy of the) current value stored.
     */
    public double[] getDoubleArrayValue() {
        if (m_value == null) {
            return null;
        }
        final double[] result = new double[m_value.length];
        System.arraycopy(m_value, 0, result, 0, m_value.length);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_doublearray";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        try {
            // use the current value, if no value is stored in the
            // settings
            setDoubleArrayValue(settings.getDoubleArray(m_configName, m_value));
        } catch (final IllegalArgumentException iae) {
            // if the argument is not accepted: keep the old value.
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            // no default value, throw an exception instead
            setDoubleArrayValue(settings.getDoubleArray(m_configName));
        } catch (final IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        settings.addDoubleArray(m_configName, getDoubleArrayValue());
    }

    /**
     * set the value stored to (a copy of) the new value.
     * 
     * @param newValue the new value to store.
     */
    public void setDoubleArrayValue(final double[] newValue) {
        boolean same;
        if (newValue == null) {
            same = (m_value == null);
        } else {
            if ((m_value == null) || (m_value.length != newValue.length)) {
                same = false;
            } else {
                final Double[] valueObjects = new Double[m_value.length];
                for (int i = 0; i < m_value.length; i++) {
                    valueObjects[i] = m_value[i];
                }

                final Set<Double> current = new HashSet<Double>(Arrays.asList(valueObjects));
                same = true;
                for (final Double d : newValue) {
                    if (!current.contains(d)) {
                        same = false;
                        break;
                    }
                }
            }
        }

        if (newValue == null) {
            m_value = null;
        } else {
            m_value = new double[newValue.length];
            System.arraycopy(newValue, 0, m_value, 0, newValue.length);
        }

        if (!same) {
            notifyChangeListeners();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getDoubleArray(m_configName);
    }

}
