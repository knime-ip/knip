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
package org.knime.knip.scijava.dialog;

import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.scijava.SciJavaGateway;
import org.scijava.module.ModuleException;
import org.scijava.ui.swing.widget.SwingInputHarvester;
import org.scijava.widget.WidgetModel;

/**
 * {@link DialogComponent} that contains the components of an ImageJ parameter dialog. The components are automatically
 * 'harvested' using ImageJ2 methods and are positioned together to reproduce the ImageJ dialog. This allows the usage
 * of the ImageJ parameter dialog to configure the basic input parameters inside the configuration dialog of KNIME
 * nodes. To handle data persistence and communication between the node dialog and the node model
 * {@link SettingsModelModuleDialog} is used.
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DialogComponentModule extends DialogComponent {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DialogComponentModule.class);

    /** contains the harvested components. */
    private final ExtendedInputPanel m_inputPanel;

    /**
     * creates a DialogComponent that encapsulates an ImageJ2 parameter dialog to allow the configration of basic input
     * parameters in the KNIME node dialog.
     *
     * @param model associated settings model to connect the node dialog to the node model
     * @param harvesterModule the module that should be 'harvested' to create the dialog
     */
    public DialogComponentModule(final SettingsModelModuleDialog model, final HarvesterModuleWrapper harvesterModule) {
        super(model);

        m_inputPanel = new ExtendedInputPanel();
        final SwingInputHarvester harvester = new SwingInputHarvester();
        harvester.setContext(SciJavaGateway.getInstance().getContext());

        try {
            harvester.buildPanel(m_inputPanel, harvesterModule);
        } catch (final ModuleException e) {
            LOGGER.error("ImageJ plugin creation for plugin " + harvesterModule.getDelegateObject()
                    + " failed during the ImageJ dialog creation.");
            e.printStackTrace();
        }

        // create module configuration panel, if possible
        getComponentPanel().add(m_inputPanel.getComponent());

        updateComponent();
    }

    @Override
    protected void updateComponent() {
        // update the component, e.i. selecting the right module, if available
        // and setting the item configurations
        final SettingsModelModuleDialog model = (SettingsModelModuleDialog)getModel();

        // load all handled values from the model and set the WidgetModel values
        // accordingly
        final Map<String, Object> valueMap = model.getItemValues(m_inputPanel.getWidgetModels().keySet());
        for (final String itemName : valueMap.keySet()) {
            m_inputPanel.getWidgetModels().get(itemName).setValue(valueMap.get(itemName));
        }
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        // update model
        for (final WidgetModel model : m_inputPanel.getWidgetModels().values()) {
            ((SettingsModelModuleDialog)getModel()).setItemValue(model.getItem().getName(), model.getValue());
        }
    }

    /**
     * not implemented
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {

    }

    /**
     * not implemented
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {

    }

    @Override
    public void setToolTipText(final String text) {
        //
    }

    /**
     * @return true if no widgets have been added to the dialog
     */
    public boolean isEmpty() {
        return m_inputPanel.getWidgetCount() == 0;
    }

}
