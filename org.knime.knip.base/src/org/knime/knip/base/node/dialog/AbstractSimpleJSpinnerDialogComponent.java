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
package org.knime.knip.base.node.dialog;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObjectSpec;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Andreas Burger (andreas.burger@uni-konstanz.de)
 * @author Daniel Seebacher (daniel.seebacher@uni-konstanz.de)
 */
abstract class AbstractSimpleJSpinnerDialogComponent extends DialogComponent implements ChangeListener {

    private final JSpinner m_spinner;

    private final JLabel m_label;

    protected AbstractSimpleJSpinnerDialogComponent(final SettingsModel model, final String labelName,
                                                    final int lowerBound, final int upperBound, final int defaultValue) {
        super(model);
        model.addChangeListener(this);

        m_spinner = new JSpinner(new SpinnerNumberModel(defaultValue, lowerBound, upperBound, 1));
        m_label = new JLabel(labelName);

        getComponentPanel().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
        getComponentPanel().add(m_label);
        getComponentPanel().add(m_spinner);

        getModel().addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent arg0) {
                updateComponent();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        SettingsModelInteger model = (SettingsModelInteger)getModel();
        m_spinner.setValue(model.getIntValue());
        getComponentPanel().updateUI();
        setEnabledComponents(getModel().isEnabled());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        SettingsModelInteger model = (SettingsModelInteger)getModel();
        model.setIntValue((Integer)m_spinner.getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_spinner.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        //nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final ChangeEvent e) {
        getComponentPanel().updateUI();
    }

}
