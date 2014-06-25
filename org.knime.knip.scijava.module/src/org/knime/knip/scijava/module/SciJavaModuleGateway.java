package org.knime.knip.scijava.module;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

<<<<<<< HEAD:org.knime.knip.scijava.module/src/org/knime/knip/scijava/module/SciJavaModuleGateway.java
import org.knime.knip.scijava.module.services.AdapterService;
=======
import org.eclipse.core.runtime.internal.adaptor.ContextFinder;
import org.knime.knip.scijava.services.AdapterService;
>>>>>>> e9bf1d6a3639bb500217314c1c51b6e65f849afd:org.knime.knip.scijava/src/org/knime/knip/scijava/SciJavaGateway.java
import org.scijava.Context;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.PluginIndex;
import org.scijava.util.ColorRGB;

public class SciJavaModuleGateway {

	private static SciJavaModuleGateway instance;

	private AdapterService as;

	private ModuleService ms;

	private final List<ModuleInfo> m_supportedModulesInfos;

	private HashMap<String, ModuleInfo> m_delegateClassName2ModuleInfo;

	private Context context;

	/**
	 * all types that can be supported as input type for an ImageJ plugin as a
	 * {@link org.knime.knip.imagej2.core.imagejdialog panel} derived from the
	 * ImageJ dialog.
	 */
	public static final Class<?>[] SUPPORTED_SCIJAVA_DIALOG_TYPES = {
			Number.class, byte.class, double.class, float.class, int.class,
			long.class, short.class, String.class, Character.class, char.class,
			Boolean.class, boolean.class, File.class, ColorRGB.class };

	private SciJavaModuleGateway() {

		context = new Context(new PluginIndex(new DefaultPluginFinder(new ContextFinder(getClass().getClassLoader()))));
		as = context.getService(AdapterService.class);
		ms = context.getService(ModuleService.class);

		// get list of modules, and filter them to those acceptable to
		// KNIME/KNIP
		final List<ModuleInfo> moduleInfos = ms.getModules();
		m_supportedModulesInfos = findSupportedModules(moduleInfos);
		m_delegateClassName2ModuleInfo = new HashMap<String, ModuleInfo>(
				m_supportedModulesInfos.size());
		for (final ModuleInfo info : m_supportedModulesInfos) {
			m_delegateClassName2ModuleInfo.put(info.getDelegateClassName(),
					info);
		}

	}

	private List<ModuleInfo> findSupportedModules(
			final List<ModuleInfo> moduleInfos) {
		
		final List<ModuleInfo> supportedModules = new ArrayList<ModuleInfo>();

		for (final ModuleInfo info : moduleInfos) {
			if (as.getAdapter(info) != null) {
				supportedModules.add(info);
			}

		}

		return supportedModules;
	}

	public ModuleInfo getModuleInfo(final String moduleInfoDelegateClassName) {
		return m_delegateClassName2ModuleInfo.get(moduleInfoDelegateClassName);
	}

	public AdapterService getAdapterService() {
		return as;
	}

	public static SciJavaModuleGateway getInstance() {
		if (instance == null)
			instance = new SciJavaModuleGateway();
		return instance;
	}

	public Context getContext() {
		return context;
	}

	public ModuleService getModuleService() {
		return ms;
	}

	/**
	 * tests a type against the internal list of ImageJ dialog input types.
	 * 
	 * @param type
	 *            the type to test
	 * @return true if this type can be handled by the ImageJ dialog
	 */
	public static synchronized boolean isSciJavaDialogInputType(
			final Class<?> type) {
		boolean ret = false;
		for (final Class<?> c : SUPPORTED_SCIJAVA_DIALOG_TYPES) {
			if (c.isAssignableFrom(type)) {
				ret = true;
			}
		}
		return ret;
	}

	public List<ModuleInfo> getSupportedModules() {
		return m_supportedModulesInfos;
	}

}
