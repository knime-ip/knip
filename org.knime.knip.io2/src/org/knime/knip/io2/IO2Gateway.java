package org.knime.knip.io2;

import io.scif.SCIFIO;

import java.util.stream.Collectors;

import net.imagej.ops.OpService;

import org.knime.knip.io2.extension.SciJavaPluginExtensionHandler;
import org.knime.scijava.core.ResourceAwareClassLoader;
import org.scijava.Context;
import org.scijava.io.handle.DataHandleService;
import org.scijava.io.location.LocationService;
import org.scijava.log.LogService;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginIndex;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.Service;

public class IO2Gateway {

	private static IO2Gateway m_instance;

	private static DataHandleService m_handles;
	private static LocationService m_loc;
	private static Context m_context;
	private static SCIFIO m_scifio;

	private IO2Gateway() {
		// set log level to warn to ignore plug-in loading log events
		System.setProperty(LogService.LOG_LEVEL_PROPERTY, "warn");
		// blacklist StderrLogService to prevent logging to stdout / stderr
		System.setProperty("scijava.plugin.blacklist", ".*StderrLogService");

		// create custom plugin index
		final PluginIndex pluginIndex = new PluginIndex(
				new DefaultPluginFinder(new ResourceAwareClassLoader(getClass().getClassLoader(), getClass())));
		// get plugins from extension point
		pluginIndex.addAll(SciJavaPluginExtensionHandler.getPlugins().stream().map(p -> {
			final Plugin a = p.getClass().getAnnotation(Plugin.class);
			return new PluginInfo<>(p.getClass().getName(), a.type(), a, p.getClass().getClassLoader());
		}).collect(Collectors.toList()));
		// create custom context with all collected plugins
		m_context = new Context(pluginIndex);
	}

	/**
	 * @return singleton instance of {@link IO2Gateway}
	 */
	public static synchronized IO2Gateway getInstance() {
		if (m_instance == null) {
			m_instance = new IO2Gateway();
		}
		return m_instance;
	}

	/**
	 * @return singleton instance of {@link DataHandleService}
	 */
	public static DataHandleService handles() {
		if (m_handles == null) {
			getInstance();
			m_handles = IO2Gateway.m_context.getService(DataHandleService.class);
		}
		return m_handles;
	}

	public static LocationService locations() {
		if (m_loc == null) {
			getInstance();
			m_loc = IO2Gateway.m_context.getService(LocationService.class);
		}
		return m_loc;
	}

	public static SCIFIO scifio() {
		if (m_scifio == null) {
			getInstance();
			m_scifio = new SCIFIO(IO2Gateway.m_context);
		}
		return m_scifio;
	}

	public static <S extends Service> S getService(final Class<S> c) {
		getInstance();
		return IO2Gateway.m_context.getService(c);
	}

	public static Context context() {
		getInstance();
		return IO2Gateway.m_context;
	}

	public static OpService ops() {
		getInstance();
		return IO2Gateway.m_context.getService(OpService.class);
	}
}
