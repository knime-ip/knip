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

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Implementation of DefaultNodeSettingsPane for DataAware Dialogs Note that the loadSettingsFrom /
 * loadAdditionalSettingsFrom methods have changed to suit the needs for DataAwareNodeDialogs
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DataAwareDefaultNodeSettingsPane extends DataAwareNodeDialogPane {

    /* The tabs' names. */
    private static final String TAB_TITLE = "Options";

    private final List<DialogComponent> m_dialogComponents;

    private JPanel m_compositePanel;

    private JPanel m_currentPanel;

    private String m_defaultTabTitle = TAB_TITLE;

    private Box m_currentBox;

    private boolean m_horizontal = false;

    /**
     * Constructor for DefaultNodeDialogPane.
     */
    public DataAwareDefaultNodeSettingsPane() {
        super();
        m_dialogComponents = new ArrayList<DialogComponent>();
        createNewPanels();
        super.addTab(m_defaultTabTitle, m_compositePanel);
    }

    private void createNewPanels() {
        m_compositePanel = new JPanel();
        m_compositePanel.setLayout(new BoxLayout(m_compositePanel, BoxLayout.Y_AXIS));
        m_currentPanel = m_compositePanel;
        m_currentBox = createBox(m_horizontal);
        m_currentPanel.add(m_currentBox);
    }

    /**
     * Sets the title of the default tab that is created and used until you call {@link #createNewTab}.
     *
     * @param tabTitle the new title of the first tab. Can't be null or empty.
     * @throws IllegalArgumentException if the title is already used by another tab, or if the specified title is null
     *             or empty.
     */
    public void setDefaultTabTitle(final String tabTitle) {
        if ((tabTitle == null) || (tabTitle.length() == 0)) {
            throw new IllegalArgumentException("The title of a tab can't be " + "null or empty.");
        }
        if (tabTitle.equals(m_defaultTabTitle)) {
            return;
        }
        // check if we already have a tab with the new title
        if (super.getTab(tabTitle) != null) {
            throw new IllegalArgumentException("A tab with the specified new" + " name (" + tabTitle
                    + ") already exists.");
        }
        super.renameTab(m_defaultTabTitle, tabTitle);
        m_defaultTabTitle = tabTitle;
    }

    /**
     * Creates a new tab in the dialog. All components added from now on are placed in that new tab. After creating a
     * new tab the previous tab is no longer accessible. If a tab with the same name was created before an Exception is
     * thrown. The new panel in the new tab has no group set (i.e. has no border). The new tab is placed at the
     * specified position (or at the right most position, if the index is too big).
     *
     * @param tabTitle the title of the new tab to use from now on. Can't be null or empty.
     * @param index the index to place the new tab at. Can't be negative.
     * @throws IllegalArgumentException if you specify a title that is already been used by another tab. Or if the
     *             specified title is null or empty.
     * @see #setDefaultTabTitle(String)
     */
    public void createNewTabAt(final String tabTitle, final int index) {
        if ((tabTitle == null) || (tabTitle.length() == 0)) {
            throw new IllegalArgumentException("The title of a tab can't be " + "null nor empty.");
        }
        // check if we already have a tab with the new title
        if (super.getTab(tabTitle) != null) {
            throw new IllegalArgumentException("A tab with the specified new" + " name (" + tabTitle
                    + ") already exists.");
        }
        createNewPanels();
        super.addTabAt(index, tabTitle, m_compositePanel,false);
    }

    /**
     * Creates a new tab in the dialog. All components added from now on are placed in that new tab. After creating a
     * new tab the previous tab is no longer accessible. If a tab with the same name was created before an Exception is
     * thrown. The new panel in the new tab has no group set (i.e. has no border). The tab is placed at the right most
     * position.
     *
     * @param tabTitle the title of the new tab to use from now on. Can't be null or empty.
     * @throws IllegalArgumentException if you specify a title that is already been used by another tab. Or if the
     *             specified title is null or empty.
     * @see #setDefaultTabTitle(String)
     */
    public void createNewTab(final String tabTitle) {
        createNewTabAt(tabTitle, Integer.MAX_VALUE);
    }

    /**
     * Brings the specified tab to front and shows its components.
     *
     * @param tabTitle the title of the tab to select. If the specified title doesn't exist, this method does nothing.
     */
    public void selectTab(final String tabTitle) {
        setSelected(tabTitle);
    }

    /**
     * Creates a new dialog component group and closes the current one. From now on the dialog components added with the
     * addDialogComponent method are added to the current group. The group is a bordered and titled panel.
     *
     * @param title - the title of the new group.
     */
    public void createNewGroup(final String title) {
        checkForEmptyBox();
        m_currentPanel = createSubPanel(title);
        m_currentBox = createBox(m_horizontal);
        m_currentPanel.add(m_currentBox);
    }

    private JPanel createSubPanel(final String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title));
        m_compositePanel.add(panel);
        return panel;
    }

    /**
     * Closes the current group. Further added dialog components are added to the default panel outside any border.
     *
     */
    public void closeCurrentGroup() {
        checkForEmptyBox();
        if (m_currentPanel.getComponentCount() == 0) {
            m_compositePanel.remove(m_currentPanel);
        }
        m_currentPanel = m_compositePanel;
        m_currentBox = createBox(m_horizontal);
        m_currentPanel.add(m_currentBox);
    }

    /**
     * Add a new DialogComponent to the underlying dialog. It will automatically be added in the dialog and saved/loaded
     * from/to the config.
     *
     * @param diaC component to be added
     */
    public void addDialogComponent(final DialogComponent diaC) {
        m_dialogComponents.add(diaC);
        m_currentBox.add(diaC.getComponentPanel());
        addGlue(m_currentBox, m_horizontal);
    }

    /**
     * Changes the orientation the components get placed in the dialog.
     *
     * @param horizontal <code>true</code> if the next components should be placed next to each other or
     *            <code>false</code> if the next components should be placed below each other.
     */
    public void setHorizontalPlacement(final boolean horizontal) {
        if (m_horizontal != horizontal) {
            m_horizontal = horizontal;
            checkForEmptyBox();
            m_currentBox = createBox(m_horizontal);
            m_currentPanel.add(m_currentBox);
        }
    }

    /**
     * Save settings of all registered <code>DialogComponents</code> into the configuration object.
     *
     * @param settings the <code>NodeSettings</code> to write into
     * @throws InvalidSettingsException if the user has entered wrong values
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        for (DialogComponent comp : m_dialogComponents) {
            comp.saveSettingsTo(settings);
        }

        saveAdditionalSettingsTo(settings);
    }

    /**
     * Load settings for all registered components.
     *
     * @param settings the <code>NodeSettings</code> to read from
     * @param input data at the input port
     * @throws NotConfigurableException if the node can currently not be configured
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObject[] input)
            throws NotConfigurableException {
        assert settings != null;
        assert input != null;

        //extract specs from input data
        PortObjectSpec[] specs = new PortObjectSpec[input.length];
        for (int i = 0; i < specs.length; i++) {
            specs[i] = input[i].getSpec();
        }

        for (DialogComponent comp : m_dialogComponents) {
            comp.loadSettingsFrom(settings, specs);
        }

        loadAdditionalSettingsFrom(settings, input);
    }

    /**
     * Override hook to load additional settings.
     *
     * @param settings The settings of the node
     * @param input The input data.
     * @throws NotConfigurableException If not configurable
     */
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObject[] input)
            throws NotConfigurableException {
    }

    /**
     * This method can be overridden to save additional settings to the given settings object.
     *
     * @param settings the <code>NodeSettings</code> to write into
     * @throws InvalidSettingsException if the user has entered wrong values
     */
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        assert settings != null;
    }

    private void checkForEmptyBox() {
        if (m_currentBox.getComponentCount() == 0) {
            m_currentPanel.remove(m_currentBox);
        }
    }

    /**
     * @param currentBox
     * @param horizontal
     */
    private static void addGlue(final Box box, final boolean horizontal) {
        if (horizontal) {
            box.add(Box.createVerticalGlue());
        } else {
            box.add(Box.createHorizontalGlue());
        }
    }

    /**
     * @param horizontal <code>true</code> if the layout is horizontal
     * @return the box
     */
    private static Box createBox(final boolean horizontal) {
        final Box box;
        if (horizontal) {
            box = new Box(BoxLayout.X_AXIS);
            box.add(Box.createVerticalGlue());
        } else {
            box = new Box(BoxLayout.Y_AXIS);
            box.add(Box.createHorizontalGlue());
        }
        return box;
    }
}
