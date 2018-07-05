package org.knime.knip.io2.extension;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.scijava.plugin.SciJavaPlugin;

public class SciJavaPluginExtensionHandler {
	private static final NodeLogger LOGGER = NodeLogger.getLogger(SciJavaPluginExtensionHandler.class);

	private SciJavaPluginExtensionHandler() {
		// utility class
	}

	/** The id of the SciJavaPlugin extension point. */
	public static final String EXT_POINT_ID = "org.knime.knip.io2.SciJavaPlugin";

	/**
	 * The attribute of the scijava plugin extension point pointing to the plugin
	 * constructor
	 */
	public static final String EXT_POINT_ATTR_DF = "SciJavaPlugin";

	public static List<SciJavaPlugin> getPlugins() {

		List<SciJavaPlugin> plugins = new ArrayList<>();

		try {
			final IExtensionRegistry registry = Platform.getExtensionRegistry();
			final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
			if (point == null) {
				LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
				throw new IllegalStateException("ACTIVATION ERROR: " + " --> Invalid extension point: " + EXT_POINT_ID);
			}
			for (final IConfigurationElement elem : point.getConfigurationElements()) {
				final String operator = elem.getAttribute(EXT_POINT_ATTR_DF);
				final String decl = elem.getDeclaringExtension().getUniqueIdentifier();

				if (operator == null || operator.isEmpty()) {
					LOGGER.error("The extension '" + decl + "' doesn't provide the required attribute '"
							+ EXT_POINT_ATTR_DF + "'");
					LOGGER.error("Extension " + decl + " ignored.");
					continue;
				}

				try {
					plugins.add((SciJavaPlugin) elem.createExecutableExtension(EXT_POINT_ATTR_DF));
				} catch (final Throwable t) {
					LOGGER.error("Problems during initialization of SciJavaPluginExensionPoint (with id '" + operator
							+ "'.)");
					if (decl != null) {
						LOGGER.error("Extension " + decl + " ignored.", t);
					}
				}
			}
		} catch (final Exception e) {
			LOGGER.error("Exception while registering SciJavaPlugin extensions");
		}
		return plugins;
	}
}
