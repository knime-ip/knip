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
 * Created on 27 Sep 2017 by bw
 */
package org.knime.knip.base.nodes.util.tilelooper.config;

import org.knime.core.node.InvalidSettingsException;

/**
 * A settings model which can store if a number is provided and the number. Additionally the number has to be in a given
 * range.
 *
 * @author Benjamin Wilhelm, MPI-CBG, Dresden
 */
public class SettingsModelOptionalNumberRange extends SettingsModelOptionalNumber {

    private final int m_min;

    private final int m_max;

    /**
     * Creates a settings model which can store if a number is provided and the number. Additionally the number has to
     * be in a given range.
     *
     * @param configName The name of the config object.
     * @param defaultValue The default value for the number.
     * @param defaultHasNumber If a number is given by default.
     * @param min The minimum value of the number.
     * @param max The maximum value of the number.
     */
    public SettingsModelOptionalNumberRange(final String configName, final int defaultValue,
                                            final boolean defaultHasNumber, final int min, final int max) {
        super(configName, defaultValue, defaultHasNumber);
        m_min = min;
        m_max = max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SettingsModelOptionalNumberRange createClone() {
        return new SettingsModelOptionalNumberRange(getConfigName(), getIntValue(), hasNumber(), m_min, m_max);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateValue(final int value) throws InvalidSettingsException {
        if (value > m_max || value < m_min) {
            throw new InvalidSettingsException("Number must be between " + m_min + " and " + m_max + ".");
        }
    }


    int getMin() {
        return m_min;
    }

    int getMax() {
        return m_max;
    }
}
