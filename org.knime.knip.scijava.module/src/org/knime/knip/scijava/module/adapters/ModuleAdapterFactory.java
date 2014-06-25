package org.knime.knip.scijava.module.adapters;

import org.scijava.module.Module;
import org.scijava.module.ModuleInfo;
import org.scijava.plugin.SciJavaPlugin;

public interface ModuleAdapterFactory extends SciJavaPlugin {

	ModuleAdapter createAdapter(Module module);

	boolean isCompatible(ModuleInfo info);
}
