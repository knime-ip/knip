/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2014
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
 * Created on Jun 24, 2014 by dietzc
 */
package org.knime.knip.scijava.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.ConfigRO;
import org.knime.knip.scijava.SciJavaGateway;
import org.scijava.module.ModuleInfo;

/**
 * 
 * @author dietzc
 */
public class ModuleNodeSetFactory implements NodeSetFactory {

	@Override
	public Collection<String> getNodeFactoryIds() {
		final List<ModuleInfo> moduleInfos = SciJavaGateway.getInstance()
				.getSupportedModules();
		final List<String> moduleIds = new ArrayList<String>(moduleInfos.size());
		for (final ModuleInfo info : moduleInfos) {
			moduleIds.add(info.getDelegateClassName());
		}

		return moduleIds;
	}

	@Override
	public Class<? extends NodeFactory<? extends NodeModel>> getNodeFactory(
			final String id) {
		return ModuleNodeFactory.class;
	}

	@Override
	public String getCategoryPath(final String id) {
		return "/opencv";
	}

	@Override
	public String getAfterID(final String id) {
		return "";
	}

	@Override
	public ConfigRO getAdditionalSettings(final String id) {
		final NodeSettings settings = new NodeSettings("scijava-factory");
		settings.addString(ModuleNodeFactory.MODULE_CLASS_KEY, id);
		return settings;
	}
}
