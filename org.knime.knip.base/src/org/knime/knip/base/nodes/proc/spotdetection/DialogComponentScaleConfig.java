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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * A dialog component for the selection of wavelet levels (scales) in the wavelet spot detection plugin. This component
 * has to save two values per added level the enabled status and a user defined threshold factor. Internally both values
 * get mapped to a {@link SettingsModelDoubleArray}. For convenience use provided the static methods to read write to
 * the settings model.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DialogComponentScaleConfig extends DialogComponent {

    /**
     * creates a double[] that holds a configuration for this component specifically the enabled status and threshold
     * level for each generated wavelete level. The array can be used with {@link SettingsModelDoubleArray}. Note that
     * both parameter arrays have to be of the same length.
     * 
     * @param enabled enabled status (per wavelet level)
     * @param thresholdValue (per wavelete level)
     * @return a double[] that can be used in a {@link SettingsModelDoubleArray} as {@link SettingsModel} for this
     *         component.
     */
    static double[] createModelArray(final boolean enabled[], final double thresholdValue[]) {
        final double[] values = new double[enabled.length * 2];

        for (int i = 0; i < enabled.length; i++) {
            if (enabled[i]) {
                values[(i * 2)] = 1.0;
            } else {
                values[(i * 2)] = 0.0;
            }

            values[((i * 2) + 1)] = thresholdValue[i];
        }

        return values;
    }

    /**
     * @param model {@link SettingsModel} which contains the enabled status and threshold factor for the generated
     *            wavelet levels of this component.
     * @return the enabled stauts for each wavelet level
     */
    static boolean[] getEnabledState(final SettingsModelDoubleArray model) {
        final double[] values = model.getDoubleArrayValue();
        final boolean[] ret = new boolean[values.length / 2];

        for (int i = 0; i < values.length; i += 2) {
            if (values[i] == 1.0) {
                ret[i / 2] = true;
            }
        }
        return ret;
    }

    /**
     * @param model {@link SettingsModel} which contains the enabled status and threshold factor for the generated
     *            wavelet levels of this component.
     * @return the threshold factor for each wavelet level
     */
    static double[] getThresholdValues(final SettingsModelDoubleArray model) {
        final double[] values = model.getDoubleArrayValue();
        final double[] ret = new double[values.length / 2];

        for (int i = 1; i < values.length; i += 2) {
            ret[(i - 1) / 2] = values[i];
        }
        return ret;
    }

    private final String CHECK_BOX_TEXT = "enable";

    private ArrayList<JCheckBox> m_enabledBoxes;

    // converter methods for easier usage of the missues double array

    private JPanel m_scalePanel;

    private ArrayList<JTextField> m_threshFactorFields;

    private final int TEXT_COLUMNS = 5;

    // constructor

    /**
     * Creates a component that allows to set the wavelet level parameters for the Wavelet Spot Detection plugin. Note
     * that the enabled status and threshold factor of the levels are encoded in one {@link SettingsModelDoubleArray}.
     * Use the provided static methods to read write to the model.
     * 
     * @param model
     * @param label component headline
     */
    public DialogComponentScaleConfig(final SettingsModelDoubleArray model, final String label) {
        super(model);
        createComponent(getComponentPanel(), label);
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // we're always good - independent of the incoming spec
    }

    private synchronized void createComponent(final JPanel componentPanel, final String label) {
        componentPanel.setLayout(new BorderLayout());

        m_scalePanel = new JPanel();
        m_threshFactorFields = new ArrayList<JTextField>();
        m_enabledBoxes = new ArrayList<JCheckBox>();

        final JLabel labelComp = new JLabel(label);
        labelComp.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        componentPanel.add(labelComp, BorderLayout.NORTH);

        final JScrollPane scroll = new JScrollPane(m_scalePanel);
        componentPanel.add(scroll, BorderLayout.CENTER);

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10));
        {
            final JButton addScale = new JButton("add scale");
            final JButton removeScale = new JButton("remove scale");

            buttonPanel.add(addScale);
            buttonPanel.add(removeScale);

            addScale.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final JTextField newField = new JTextField();
                    newField.setText("1.0");
                    newField.setColumns(TEXT_COLUMNS);
                    m_threshFactorFields.add(newField);

                    m_enabledBoxes.add(new JCheckBox(CHECK_BOX_TEXT, true));

                    rebuildScalePanel();
                }
            });

            removeScale.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final int size = m_threshFactorFields.size();
                    if (size > 1) {
                        // cannot remove the first level
                        m_threshFactorFields.remove(size - 1);
                        m_enabledBoxes.remove(size - 1);

                        rebuildScalePanel();
                    }
                }
            });
        }

        componentPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private synchronized void gui2Model() throws InvalidSettingsException {
        final double[] values = new double[m_enabledBoxes.size()];
        final boolean[] enabled = new boolean[m_enabledBoxes.size()];

        // testing at least one selection
        boolean selected = false;
        for (final JCheckBox box : m_enabledBoxes) {
            selected = selected | box.isSelected();
        }
        if (!selected) {
            throw new InvalidSettingsException("at least one wavelet level has to be selected");
        }

        // testing the values
        for (final JTextField text : m_threshFactorFields) {
            try {
                Double.parseDouble(text.getText());
            } catch (final NumberFormatException nfe) {
                throw new InvalidSettingsException("threshold factor " + text.getText() + " is not a double", nfe);
            }
        }

        // saving

        for (int i = 0; i < m_enabledBoxes.size(); i++) {
            if (m_enabledBoxes.get(i).isSelected()) {
                enabled[i] = true;
            }
            values[i] = Double.valueOf(m_threshFactorFields.get(i).getText());
        }

        ((SettingsModelDoubleArray)getModel()).setDoubleArrayValue(createModelArray(enabled, values));
    }

    private synchronized void model2Gui() {
        m_threshFactorFields.clear();
        m_enabledBoxes.clear();

        final boolean[] enabled = getEnabledState(((SettingsModelDoubleArray)getModel()));
        final double[] values = getThresholdValues(((SettingsModelDoubleArray)getModel()));

        for (int i = 0; i < enabled.length; i++) {
            m_enabledBoxes.add(new JCheckBox(CHECK_BOX_TEXT, enabled[i]));

            final JTextField newField = new JTextField(String.valueOf(values[i]));
            newField.setColumns(TEXT_COLUMNS);
            m_threshFactorFields.add(newField);
        }

        rebuildScalePanel();
    }

    private synchronized void rebuildScalePanel() {
        m_scalePanel.removeAll();
        m_scalePanel.setLayout(new BorderLayout());

        final JPanel holderPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 20, 0, 0);

        for (int i = 0; i < m_threshFactorFields.size(); i++) {
            final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));

            panel.add(m_enabledBoxes.get(i));
            panel.add(new JLabel(" wavelet level " + i + " with threshold factor "));
            panel.add(m_threshFactorFields.get(i));

            holderPanel.add(panel, gbc);
            gbc.gridy++;
        }

        m_scalePanel.add(holderPanel, BorderLayout.NORTH);
        m_scalePanel.revalidate();
        m_scalePanel.repaint();
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setToolTipText(final String text) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void updateComponent() {
        model2Gui();
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        gui2Model();
    }

}
