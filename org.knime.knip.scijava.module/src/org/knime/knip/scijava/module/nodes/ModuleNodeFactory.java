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
package org.knime.knip.scijava.module.nodes;

import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.knip.scijava.module.SciJavaModuleGateway;
import org.knime.knip.scijava.module.adapters.ModuleAdapterFactory;
import org.knime.node2012.KnimeNodeDocument;
import org.knime.node2012.KnimeNodeDocument.KnimeNode;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleInfo;

/**
 * Node factory for ImageJ2 plugins. Contains internal settings that associate
 * an instance of IJNodeFactory with a specific ImageJ2 plugin, that is created
 * as KNIME node by the factory instance.
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class ModuleNodeFactory extends
		DynamicNodeFactory<ModuleNodeModel> {

	/**
	 * Key of Module Class
	 */
	public static final String MODULE_CLASS_KEY = "module_class_key";

	private ModuleAdapterFactory m_adapter;

	private ModuleInfo m_info;

	@Override
	public ModuleNodeModel createNodeModel() {
		return new ModuleNodeModel(m_info, m_adapter);
	}

	/**
	 * @return one if at least one return type of the ImageJ plugin can be
	 *         displayed by a {@link TableCellViewNodeView} else zero
	 */
	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	/**
	 * @return returns a new node view if {@link IJNodeFactory#getNrNodeViews()}
	 *         equals one. else returns null
	 * 
	 * 
	 *         {@inheritDoc}
	 * 
	 */
	@Override
	public NodeView<ModuleNodeModel> createNodeView(final int viewIndex,
			final ModuleNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		try {
			return new ModuleNodeDialog(m_info, m_adapter);
		} catch (final ModuleException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * loads additional factory settings that allow to load different
	 * IJNodeFactory instances for the different ImageJ plugins. Essentially an
	 * identifier is loaded that allows reloading of meta data that is specific
	 * to the associated ImageJ plugin of the IJNodeFactory
	 * 
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void loadAdditionalFactorySettings(final ConfigRO config)
			throws InvalidSettingsException {
		final String moduleClass = config.getString(MODULE_CLASS_KEY);

		m_info = SciJavaModuleGateway.getInstance().getModuleInfo(moduleClass);
		m_adapter = SciJavaModuleGateway.getInstance().getAdapterService()
				.getAdapter(m_info);

		super.loadAdditionalFactorySettings(config);
	}

	/**
	 * Automatically creates node descriptions based on the meta data that is
	 * provided with the ModuleInfo of an ImageJ2 plugin.<br>
	 * <br>
	 * Creates: <br>
	 * - (optional) a description for a table cell viewer if the output matches
	 * one of the predefined displayable types <br>
	 * - an icon from the plugin jar or the standard plugin icon<br>
	 * - a menu category from the plugin menu path - a description of the plugin
	 * - a basic static port description
	 * 
	 * {@inheritDoc}
	 * 
	 * @deprecated
	 * 
	 */
	@Deprecated
	@Override
	protected void addNodeDescription(final KnimeNodeDocument doc) {
		final KnimeNode node = doc.addNewKnimeNode();

		// TYPE
		node.setType(KnimeNode.Type.OTHER);

		// ICON
		final String path = "default_icon.png";
		node.setIcon(path);

		node.setName(m_info.getName());
	}

	/**
	 * saves additional factory settings such that different IJNodeFactory
	 * instances can be loaded for the different ImageJ Plugins. Essentially
	 * stores an identifier that allows reloading of the specific information
	 * that belongs to a specific Plugin.
	 * 
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void saveAdditionalFactorySettings(final ConfigWO config) {
		config.addString(MODULE_CLASS_KEY, m_info.getDelegateClassName());
		super.saveAdditionalFactorySettings(config);
	}
}
