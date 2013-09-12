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
package org.knime.knip.base.nodes.proc.resampler;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.imglib2.meta.TypedAxis;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.base.KNIMEKNIPPlugin;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DialogComponentScalingValues extends DialogComponent implements ItemListener {

    /**
     * max length of points for a dimension to be selected
     */
    public static final int MAX_DIM_LENGTH = 200;

    private final TypedAxis[] m_dimLabels = KNIMEKNIPPlugin.parseDimensionLabelsAsAxis();

    /* An error message. */
    private final JLabel m_errorMessage;

    /*	 */
    private final JPanel m_panel;

    /* the input field for the scaling values */
    private final JTextField[] m_scalingValues;

    /**
     * 
     * @param model
     */
    public DialogComponentScalingValues(final SettingsModelScalingValues model) {
        super(model);

        getComponentPanel().setLayout(new BoxLayout(getComponentPanel(), BoxLayout.Y_AXIS));

        m_scalingValues = new JTextField[m_dimLabels.length];
        m_panel = new JPanel();
        for (int d = 0; d < m_dimLabels.length; d++) {
            m_scalingValues[d] = new JTextField(2);
            m_panel.add(new JLabel(m_dimLabels[d].type().getLabel()));
            m_scalingValues[d].setText("1");
            m_panel.add(m_scalingValues[d]);
        }

        getComponentPanel().add(m_panel);
        m_errorMessage = new JLabel("");
        getComponentPanel().add(m_errorMessage);

        updateComponent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // TODO: Empty whatever
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void itemStateChanged(final ItemEvent arg0) {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void setEnabledComponents(final boolean enabled) {
        for (int t = 0; t < m_scalingValues.length; t++) {
            m_scalingValues[t].setEnabled(enabled);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setToolTipText(final String text) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void updateComponent() {

        // updateModel();
        final SettingsModelScalingValues model = ((SettingsModelScalingValues)getModel());

        for (int t = 0; t < m_scalingValues.length; t++) {
            m_scalingValues[t].setText("" + model.getNewDimensions(m_dimLabels[t].type().getLabel()));
        }

        // also update the enable status
        setEnabledComponents(getModel().isEnabled());
    }

    /**
     *
     */
    private final void updateModel() {
        final SettingsModelScalingValues model = (SettingsModelScalingValues)getModel();

        for (int t = 0; t < m_scalingValues.length; t++) {
            model.setScalingValue(m_dimLabels[t].type().getLabel(), Double.parseDouble((m_scalingValues[t].getText())));

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void validateSettingsBeforeSave() throws InvalidSettingsException {
        try {

            for (int t = 0; t < m_scalingValues.length; t++) {
                Double.parseDouble(m_scalingValues[t].getText());
            }
        } catch (final NumberFormatException e) {
            throw new InvalidSettingsException("Textfield must only contain numbers");
        }
        updateModel();
    }
}
