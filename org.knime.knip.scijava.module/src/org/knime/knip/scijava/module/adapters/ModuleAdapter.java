package org.knime.knip.scijava.module.adapters;

import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;

public interface ModuleAdapter {

	DataTableSpec getOutSpec();

	Map<ModuleItem<?>, ModuleItemAdapter<?>> getInputAdapters();

	/**
	 * Configure the {@link Module}. This means that all inputs are properly
	 * set. 
	 * 
	 * @param spec
	 * @param row
	 * @param modelMap
	 */
	void configureModule(DataTableSpec spec, DataRow row,
			Map<ModuleItem<?>, SettingsModel> modelMap);

	List<DataCell[]> getModuleOutput(ExecutionContext context);

	Module getModule();
}
