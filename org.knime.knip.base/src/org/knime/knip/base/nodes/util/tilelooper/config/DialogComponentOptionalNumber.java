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

import java.text.ParseException;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.SpinnerNumberModel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObjectSpec;

/**
 * TODO This class can probably live somewhere else and be used again.
 *
 * A new dialog component which contains a combobox and a spinner to input an optional number.
 *
 * @author Benjamin Wilhelm, MPI-CBG, Dresden
 */
public class DialogComponentOptionalNumber extends DialogComponent {

    private final JCheckBox m_checkBox;

    private final JLabel m_label;

    private final JSpinner m_spinner;

    /**
     * Creates a new dialog component which contains a combobox and a spinner to input an optional number.
     *
     * @param model The settings model.
     * @param label The label to explain the configuration value.
     * @param stepSize The step size for the spinner.
     */
    public DialogComponentOptionalNumber(final SettingsModelOptionalNumber model, final String label,
                                         final int stepSize) {
        super(model);

        // Initialize Components
        m_checkBox = new JCheckBox();

        m_label = new JLabel(label);

        SpinnerNumberModel spinnerModel;
        if (model instanceof SettingsModelOptionalNumberRange) {
            int min = ((SettingsModelOptionalNumberRange)model).getMin();
            int max = ((SettingsModelOptionalNumberRange)model).getMax();
            spinnerModel = new SpinnerNumberModel(model.getIntValue(), min, max, stepSize);
        } else {
            spinnerModel = new SpinnerNumberModel(model.getIntValue(), Integer.MIN_VALUE, Integer.MAX_VALUE, stepSize);
        }
        m_spinner = new JSpinner(spinnerModel);

        final JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)m_spinner.getEditor();
        editor.getTextField().setColumns(12);
        editor.getTextField().setFocusLostBehavior(JFormattedTextField.COMMIT);

        // Change listeners

        m_spinner.addChangeListener(e -> {
            try {
                updateModel();
            } catch (final InvalidSettingsException ise) {
                // ignore it here.
            }
        });

        m_checkBox.addChangeListener(e -> {
            try {
                updateModel();
            } catch (final InvalidSettingsException ise) {
                // ignore it here.
            }
        });

        // We are not updating the model immediately when the user changes
        // the value. We update the model right before save.

        // update the spinner, whenever the model changed
        ((SettingsModelOptionalNumber)getModel()).prependChangeListener(e -> updateComponent());

        // Add components
        getComponentPanel().add(m_checkBox);
        getComponentPanel().add(m_label);
        getComponentPanel().add(m_spinner);

        updateComponent();
    }

    private void updateModel() throws InvalidSettingsException {
        try {
            final SettingsModelOptionalNumber model = (SettingsModelOptionalNumber)getModel();

            model.setHasNumber(m_checkBox.isSelected());

            m_spinner.commitEdit();
            model.setIntValue(((Integer)m_spinner.getValue()).intValue());
        } catch (final ParseException e) {
            final JComponent editor = m_spinner.getEditor();
            if (editor instanceof DefaultEditor) {
                showError(((DefaultEditor)editor).getTextField());
            }
            String errMsg = "Invalid number format. Please enter a valid integer number.";
            throw new InvalidSettingsException(errMsg);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final JComponent editor = m_spinner.getEditor();
        if (editor instanceof DefaultEditor) {
            clearError(((DefaultEditor)editor).getTextField());
        }

        // update the component only if it contains a different value than the
        // model
        try {
            final SettingsModelOptionalNumber model = (SettingsModelOptionalNumber)getModel();

            m_checkBox.setSelected(model.hasNumber());

            m_spinner.commitEdit();
            final int val = ((Integer)m_spinner.getValue()).intValue();
            if (val != model.getIntValue()) {
                m_spinner.setValue(Integer.valueOf(model.getIntValue()));
            }
        } catch (final ParseException e) {
            final SettingsModelInteger model = (SettingsModelInteger)getModel();
            m_spinner.setValue(Integer.valueOf(model.getIntValue()));
        }

        // also update the enable status of all components...
        setEnabledComponents(getModel().isEnabled());

        // update the enable status of the jspinner
        m_spinner.setEnabled(m_checkBox.isSelected());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_checkBox.setEnabled(enabled);
        m_spinner.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        // TODO Add tooltip
    }

}
