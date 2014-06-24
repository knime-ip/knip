package org.knime.knip.scijava.services;

import java.util.List;

import org.knime.knip.scijava.adapters.ModuleAdapterFactory;
import org.scijava.InstantiableException;
import org.scijava.module.ModuleInfo;
import org.scijava.plugin.AbstractPTService;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;

@Plugin(type = AdapterService.class)
public class DefaultAdapterService extends
		AbstractPTService<ModuleAdapterFactory> implements AdapterService {

	private List<PluginInfo<ModuleAdapterFactory>> plugins;

	@Override
	public void initialize() {
		plugins = getPlugins();
	}

	@Override
	public ModuleAdapterFactory getAdapter(final ModuleInfo moduleInfo) {

		try {
			for (final PluginInfo<ModuleAdapterFactory> info : plugins) {
				ModuleAdapterFactory adapter;
				if ((adapter = info.createInstance()).isCompatible(moduleInfo)) {
					return adapter;
				}
			}
		} catch (final InstantiableException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Class<ModuleAdapterFactory> getPluginType() {
		return ModuleAdapterFactory.class;
	}
}
