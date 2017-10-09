/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2017
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
 * Created on 27 Sep 2017 by Benjamin Wilhelm
 */
package org.knime.knip.base.nodes.util.tilelooper.config;

import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * A settings model which can store if a number is provided and the number.
 *
 * @author Benjamin Wilhelm, MPI-CBG, Dresden
 */
public class SettingsModelOptionalNumber extends SettingsModel {

    private static final String CFGKEY_VALUE = "value";

    private static final String CFGKEY_HAS_NUMBER = "hasNumber";

    private final String m_configName;

    private int m_value;

    private boolean m_hasNumber;

    /**
     * Creates a settings model which can store if a number is provided and the number.
     *
     * @param configName The name of the config object.
     * @param defaultValue The default value for the number.
     * @param defaultHasNumber If a number is given by default.
     */
    public SettingsModelOptionalNumber(final String configName, final int defaultValue, final boolean defaultHasNumber) {
        if ((configName == null) || "".equals(configName)) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        m_value = defaultValue;
        m_hasNumber = defaultHasNumber;
        m_configName = configName;
    }

    /**
     * @return the number value of the setting.
     */
    public int getIntValue() {
        return m_value;
    }

    /**
     * @return if a number is provided by the user.
     */
    public boolean hasNumber() {
        return m_hasNumber;
    }

    /**
     * @param value The value to set the number to.
     */
    public void setIntValue(final int value) {
        boolean notify = m_value != value;
        m_value = value;

        if (notify) {
            notifyChangeListeners();
        }
    }

    /**
     * @param hasNumber If a number is provided.
     */
    public void setHasNumber(final boolean hasNumber) {
        boolean notify = m_hasNumber != hasNumber;
        m_hasNumber = hasNumber;

        if (notify) {
            notifyChangeListeners();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SettingsModelOptionalNumber createClone() {
        return new SettingsModelOptionalNumber(m_configName, m_value, m_hasNumber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_optional_integer";
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
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {

        try {
            // use the current value, if no value is stored in the settings
            Config config = settings.getConfig(m_configName);
            setIntValue(config.getInt(CFGKEY_VALUE));
            setHasNumber(config.getBoolean(CFGKEY_HAS_NUMBER));
        } catch (IllegalArgumentException | InvalidSettingsException iae) {
            // if the argument is not accepted: keep the old value.
        } finally {
            // always notify the listeners. That is, because there could be an
            // invalid value displayed in the listener.
            notifyChangeListeners();
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
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        validateValue(settings.getConfig(m_configName).getInt(CFGKEY_VALUE));
    }

    /**
     * Check if the value is valid. Should be overwritten by deriving classes.
     *
     * @param value The value to check.
     * @throws InvalidSettingsException If the value is not valid.
     */
    protected void validateValue(final int value) throws InvalidSettingsException{
        // Can be overwritten by deriving class
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            // no default value, throw an exception instead
            Config config = settings.getConfig(m_configName);
            setIntValue(config.getInt(CFGKEY_VALUE));
            setHasNumber(config.getBoolean(CFGKEY_HAS_NUMBER));
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        Config config = settings.addConfig(m_configName);
        config.addInt(CFGKEY_VALUE, m_value);
        config.addBoolean(CFGKEY_HAS_NUMBER, hasNumber());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prependChangeListener(final ChangeListener l) {
        super.prependChangeListener(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "" + getIntValue();
    }

}
