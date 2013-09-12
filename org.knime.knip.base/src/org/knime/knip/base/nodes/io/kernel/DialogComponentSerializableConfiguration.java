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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.base.node.nodesettings.SettingsModelSerializableObjects;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DialogComponentSerializableConfiguration<T> extends DialogComponent {

    private class DownButtonListener implements ActionListener {
        /** {@inheritDoc} */
        @Override
        public void actionPerformed(final ActionEvent e) {
            final JButton button = (JButton)e.getSource();
            final int index = m_sMoveDownButtons.indexOf(button);
            moveUIControls(index, index + 1);
            updateLayout();
        }
    }

    private class RemoveButtonListener implements ActionListener {
        /** {@inheritDoc} */
        @Override
        public void actionPerformed(final ActionEvent e) {
            final JButton button = (JButton)e.getSource();
            final int index = m_sRemoveButtons.indexOf(button);
            final int x = m_sConfigurationButtons.get(index).getLocation().x;
            final int y = m_sConfigurationButtons.get(index).getLocation().y;
            final int width =
                    (m_sRemoveButtons.get(index).getBounds().x + m_sRemoveButtons.get(index).getBounds().width) - x;
            final int height =
                    (m_sRemoveButtons.get(index).getBounds().y + m_sRemoveButtons.get(index).getBounds().height) - y;
            if (m_sConfigurationButtons.get(index).isContentAreaFilled()) {
                clearView();
            }
            removeUIControls(index);
            updateLayout();
            // Initiate a repaint on the area of the removed
            // columns.
            // It is a workaround which seems to be a bug in the
            // swing
            // repainting which occurs when the last row is deleted
            // when
            // more than two rows are displayed.
            m_sListComponent.repaint(x, y, width, height);
        }
    }

    private class UpButtonListener implements ActionListener {
        /** {@inheritDoc} */
        @Override
        public void actionPerformed(final ActionEvent e) {
            final JButton button = (JButton)e.getSource();
            final int index = m_sMoveUpButtons.indexOf(button);
            moveUIControls(index, index - 1);
            updateLayout();
        }
    }

    private class ViewButtonListener implements ActionListener {
        /** {@inheritDoc} */
        @Override
        public void actionPerformed(final ActionEvent e) {
            final JButton button = (JButton)e.getSource();
            clearView();
            updateView(m_sConfigurationButtons.indexOf(button));
            getComponentPanel().revalidate();
        }
    }

    private final ActionListener m_downButtonListener;

    private JComboBox m_jcbConfigurationType;

    private final Map<String, Class<SerializableConfiguration<T>>> m_pool;

    private final JPanel m_previewConfigPanel;

    private final ActionListener m_removeButtonListener;

    private final List<JButton> m_sConfigurationButtons;

    private final List<SerializableConfiguration<T>> m_sConfigurations;

    private JComponent m_sFillComponent;

    private JComponent m_sListComponent;

    private final List<JButton> m_sMoveDownButtons;

    private final List<JButton> m_sMoveUpButtons;

    private final List<JButton> m_sRemoveButtons;

    private final ActionListener m_upButtonListener;

    private final ActionListener m_viewButtonListener;

    public DialogComponentSerializableConfiguration(final SettingsModelSerializableObjects<SerializableSetting<T>> settingsModel,
                                                    final Map<String, Class<SerializableConfiguration<T>>> pool) {
        super(settingsModel);
        m_pool = pool;

        m_sConfigurationButtons = new ArrayList<JButton>();
        m_sMoveUpButtons = new ArrayList<JButton>();
        m_sMoveDownButtons = new ArrayList<JButton>();
        m_sRemoveButtons = new ArrayList<JButton>();
        m_sConfigurations = new ArrayList<SerializableConfiguration<T>>();

        m_viewButtonListener = new ViewButtonListener();
        m_upButtonListener = new UpButtonListener();
        m_downButtonListener = new DownButtonListener();
        m_removeButtonListener = new RemoveButtonListener();

        m_previewConfigPanel = new JPanel(new BorderLayout());

        final JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        jsp.setDividerLocation(240);
        jsp.setLeftComponent(createConfigurationListPanel());

        jsp.setRightComponent(m_previewConfigPanel);

        final JPanel main = getComponentPanel();
        main.setLayout(new BorderLayout());
        main.add(jsp, BorderLayout.CENTER);
        main.setPreferredSize(new Dimension(800, 600));

        getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });
        updateComponent();
    }

    private void addOrUpdate(final JComponent parent, final JComponent component, final GridBagConstraints c) {
        final List<Component> components = Arrays.asList(parent.getComponents());
        if (components.contains(component)) {
            ((GridBagLayout)parent.getLayout()).setConstraints(component, c);
        } else {
            parent.add(component, c);
        }
    }

    private void addUIControls(final int index, final SerializableConfiguration<T> conf) {
        m_sConfigurationButtons.add(index, new JButton(conf.getName()));
        m_sConfigurationButtons.get(index).setContentAreaFilled(false);
        m_sConfigurationButtons.get(index).addActionListener(m_viewButtonListener);
        m_sMoveUpButtons.add(index, new BasicArrowButton(SwingConstants.NORTH));
        if (m_sMoveUpButtons.size() == 1) {
            m_sMoveUpButtons.get(index).setEnabled(false);
        }
        m_sMoveUpButtons.get(index).addActionListener(m_upButtonListener);
        m_sMoveDownButtons.add(index, new BasicArrowButton(SwingConstants.SOUTH));
        if (m_sMoveDownButtons.size() > 1) {
            m_sMoveDownButtons.get(index - 1).setEnabled(true);
        }
        m_sMoveDownButtons.get(index).setEnabled(false);
        m_sMoveDownButtons.get(index).addActionListener(m_downButtonListener);
        m_sRemoveButtons.add(index, new JButton("-"));
        m_sRemoveButtons.get(index).addActionListener(m_removeButtonListener);
        m_sConfigurations.add(index, conf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) {
        //
    }

    private void clearView() {
        for (int i = 0; i < m_sConfigurationButtons.size(); i++) {
            m_sConfigurationButtons.get(i).setContentAreaFilled(false);
        }
        m_previewConfigPanel.removeAll();
    }

    private JComponent createConfigurationListPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridx = 0;
        gbc.gridy = 0;
        m_jcbConfigurationType = new JComboBox();
        final DefaultComboBoxModel jcbModel = new DefaultComboBoxModel();
        m_jcbConfigurationType.setModel(jcbModel);
        for (final String name : m_pool.keySet()) {
            jcbModel.addElement(name);
        }
        p.add(m_jcbConfigurationType, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0;
        final JButton add = new JButton("+");
        add.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                SerializableConfiguration<T> conf;
                try {
                    conf = m_pool.get(m_jcbConfigurationType.getSelectedItem()).newInstance();
                    addUIControls(m_sConfigurations.size(), conf);
                    updateLayout();
                    m_sConfigurationButtons.get(m_sConfigurationButtons.size() - 1).doClick();
                } catch (final InstantiationException e1) {
                    e1.printStackTrace();
                } catch (final IllegalAccessException e1) {
                    e1.printStackTrace();
                }
            }
        });
        p.add(add, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        m_sListComponent = new JPanel(new GridBagLayout());
        m_sListComponent.setBackground(Color.WHITE);
        p.add(new JScrollPane(m_sListComponent), gbc);
        m_sFillComponent = new JPanel();
        m_sFillComponent.setBackground(Color.WHITE);
        return p;
    }

    @SuppressWarnings("unchecked")
    private SettingsModelSerializableObjects<SerializableSetting<T>> getMyModel() {
        return (SettingsModelSerializableObjects<SerializableSetting<T>>)getModel();
    }

    private void moveUIControls(final int from, final int to) {
        m_sConfigurationButtons.add(to, m_sConfigurationButtons.remove(from));
        m_sConfigurations.add(to, m_sConfigurations.remove(from));
    }

    private void removeAllUIControls() {
        for (int i = m_sConfigurationButtons.size() - 1; i >= 0; i--) {
            removeUIControls(i);
        }
    }

    private void removeUIControls(final int index) {
        m_sListComponent.remove(m_sConfigurationButtons.remove(index));
        m_sListComponent.remove(m_sMoveUpButtons.remove(index));
        if ((index == 0) && (m_sMoveUpButtons.size() > 0)) {
            m_sMoveUpButtons.get(0).setEnabled(false);
        }
        m_sListComponent.remove(m_sMoveDownButtons.remove(index));
        if ((m_sMoveDownButtons.size() == index) && (m_sMoveDownButtons.size() > 0)) {
            m_sMoveDownButtons.get(index - 1).setEnabled(false);
        }
        m_sListComponent.remove(m_sRemoveButtons.remove(index));
        m_sConfigurations.remove(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final SettingsModelSerializableObjects<SerializableSetting<T>> model = getMyModel();
        final List<SerializableSetting<T>> settings = model.getObjects();
        // look for enabled button
        int enabledIndex = -1;
        for (int index = 0; index < m_sConfigurationButtons.size(); index++) {
            if (m_sConfigurationButtons.get(index).isContentAreaFilled()) {
                enabledIndex = index;
                break;
            }
        }
        removeAllUIControls();
        for (final SerializableSetting<T> s : settings) {
            addUIControls(m_sConfigurationButtons.size(), s.createConfiguration());
        }
        updateLayout();
        setEnabledComponents(getModel().isEnabled());
        getComponentPanel().repaint();
        clearView();
        // re-enable last selected
        if ((enabledIndex != -1) && (enabledIndex < m_sConfigurationButtons.size())) {
            updateView(enabledIndex);
        }

        getComponentPanel().revalidate();
    }

    private void updateLayout() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridy = 0;
        for (int i = 0; i < m_sConfigurationButtons.size(); i++) {

            gbc.gridx = 0;
            gbc.weightx = 0;
            addOrUpdate(m_sListComponent, m_sMoveUpButtons.get(i), gbc);
            gbc.gridx++;
            gbc.weightx = 0;
            addOrUpdate(m_sListComponent, m_sMoveDownButtons.get(i), gbc);

            gbc.gridx++;
            gbc.weightx = 1;
            addOrUpdate(m_sListComponent, m_sConfigurationButtons.get(i), gbc);

            gbc.gridx++;
            gbc.weightx = 0;
            addOrUpdate(m_sListComponent, m_sRemoveButtons.get(i), gbc);
            gbc.gridy++;
        }
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        gbc.weighty = 1;
        addOrUpdate(m_sListComponent, m_sFillComponent, gbc);
        m_sListComponent.revalidate();
    }

    /**
     * Transfers the current value from the component into the model.
     * 
     * @throws InvalidSettingsException if a selection is required and no item is selected
     */
    private void updateModel() throws InvalidSettingsException {
        final List<SerializableSetting<T>> settings = new ArrayList<SerializableSetting<T>>();
        for (final SerializableConfiguration<T> conf : m_sConfigurations) {
            settings.add(conf.getSetting());
        }
        final SettingsModelSerializableObjects<SerializableSetting<T>> model = getMyModel();
        model.setObjects(settings);
    }

    private void updateView(final int index) {
        for (int i = 0; i < m_sConfigurationButtons.size(); i++) {
            m_sConfigurationButtons.get(i).setContentAreaFilled(false);
        }
        final SerializableConfiguration<T> conf = m_sConfigurations.get(index);
        m_sConfigurationButtons.get(index).setContentAreaFilled(true);

        m_previewConfigPanel.add(conf.getPreviewPanel(), BorderLayout.CENTER);
        m_previewConfigPanel.add(conf.getConfigurationPanel(), BorderLayout.PAGE_END);

        m_previewConfigPanel.repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        for (final SerializableConfiguration<T> conf : m_sConfigurations) {
            conf.validate();
        }
        updateModel();
    }
}
