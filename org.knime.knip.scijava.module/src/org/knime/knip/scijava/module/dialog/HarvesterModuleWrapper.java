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
package org.knime.knip.scijava.module.dialog;

import java.util.HashSet;
import java.util.Map;

import org.knime.knip.scijava.module.adapters.ModuleAdapter;
import org.knime.knip.scijava.module.adapters.ModuleItemAdapter;
import org.scijava.module.Module;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;

public class HarvesterModuleWrapper implements Module {

	private final Module m_module;

	private final HashSet<String> m_notHarvested;

	/**
	 * @param module
	 *            the module that should be wrapped for secure harvesting
	 */
	public HarvesterModuleWrapper(final Module module,
			final ModuleAdapter adapter) {
		m_module = module;
		m_notHarvested = new HashSet<String>();

		// ignore all items that cannot be part of the input panel
		for (final ModuleItem<?> item : m_module.getInfo().inputs()) {

			final Map<ModuleItem<?>, ModuleItemAdapter<?>> requiredFields = adapter
					.getInputAdapters();

			if (requiredFields.get(item) != null) {
				m_notHarvested.add(item.getName());
			}
		}
	}

	@Override
	public void run() {
		// not supported in harvester mode
	}

	@Override
	public void preview() {
		// not supported in harvester mode
	}

	@Override
	public void cancel() {
		// not supported in harvester mode
	}

	@Override
	public void initialize() {
		// not supported in harvester mode
	}

	@Override
	public ModuleInfo getInfo() {
		return m_module.getInfo();
	}

	@Override
	public Object getDelegateObject() {
		return m_module.getDelegateObject();
	}

	@Override
	public Object getInput(final String name) {
		return m_module.getInput(name);
	}

	@Override
	public Object getOutput(final String name) {
		return m_module.getOutput(name);
	}

	@Override
	public Map<String, Object> getInputs() {
		return m_module.getInputs();
	}

	@Override
	public Map<String, Object> getOutputs() {
		return m_module.getOutputs();
	}

	@Override
	public void setInput(final String name, final Object value) {
		m_module.setInput(name, value);
	}

	@Override
	public void setOutput(final String name, final Object value) {
		m_module.setOutput(name, value);
	}

	@Override
	public void setInputs(final Map<String, Object> inputs) {
		// not supported in harvester mode
	}

	@Override
	public void setOutputs(final Map<String, Object> outputs) {
		// not supported in harvester mode
	}

	@Override
	public boolean isResolved(final String name) {
		if (m_notHarvested.contains(name)) {
			return true;
		} else {
			return m_module.isResolved(name);
		}
	}

	@Override
	public void setResolved(final String name, final boolean resolved) {
		// not supported in harvester mode
	}

}
