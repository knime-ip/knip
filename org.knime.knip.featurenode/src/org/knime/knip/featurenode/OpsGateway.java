/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (coffee) 2003 - 2013
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
  ---------------------------------------------------------------------
 *
 */
package org.knime.knip.featurenode;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleService;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginIndex;
import org.scijava.plugin.PluginService;
import org.scijava.service.Service;
import org.scijava.ui.UIService;
import org.scijava.widget.WidgetService;

import net.imagej.ops.OpMatchingService;
import net.imagej.ops.OpService;

/**
 * Encapsulates the {@link OpService} instance as singleton.
 *
 * @author Daniel Seebacher, University of Konstanz.
 */
public final class OpsGateway {

	private static OpsGateway m_instance;

	private final Context context;

	@Parameter
	private OpService ops;

	@Parameter
	private PluginService pls;

	@Parameter
	private CommandService cs;
	
	@Parameter
	private ModuleService ms;

	/* Sets up a SciJava context with {@link OpService}. */
	private OpsGateway() {
		// set log level
		System.setProperty("scijava.log.level", "error");

		// required classes
		final List<Class<? extends Service>> classes = Arrays.asList(
				OpService.class, OpMatchingService.class, WidgetService.class,
				UIService.class, ModuleService.class, CommandService.class);

		this.context = new Context(classes,
				new PluginIndex(
						new DefaultPluginFinder(new ResourceAwareClassLoader(
								getClass().getClassLoader()))));

		this.context.inject(this);
	}

	private static OpsGateway getInstance() {
		if (m_instance == null) {
			m_instance = new OpsGateway();
		}

		return m_instance;
	}

	public static Context getContext() {
		return getInstance().context;
	}

	public static OpService getOpService() {
		return getInstance().ops;
	}

	public static PluginService getPluginService() {
		return getInstance().pls;
	}

	public static CommandService getCommandService() {
		return getInstance().cs;
	}
	
	public static ModuleService getModuleService() {
		return getInstance().ms;
	}

	class ResourceAwareClassLoader extends ClassLoader {

		final ArrayList<URL> urls = new ArrayList<URL>();

		public ResourceAwareClassLoader(final ClassLoader cl) {
			super(cl);

			final String requireBundle = FrameworkUtil.getBundle(getClass())
					.getHeaders().get(Constants.REQUIRE_BUNDLE);
			try {
				final ManifestElement[] elements = ManifestElement
						.parseHeader(Constants.BUNDLE_CLASSPATH, requireBundle);
				for (final ManifestElement manifestElement : elements) {
					final Bundle bundle = org.eclipse.core.runtime.Platform
							.getBundle(manifestElement.getValue());

					Enumeration<URL> resources;
					try {
						resources = bundle.getResources(
								"META-INF/json/org.scijava.plugin.Plugin");
					} catch (final IOException e) {
						continue;
					}

					if (resources == null) {
						continue;
					}

					while (resources.hasMoreElements()) {
						final URL resource = resources.nextElement();
						// we want to avoid transitive resolving of dependencies
						final String host = resource.getHost();
						if (bundle.getBundleId() == Long.valueOf(
								host.substring(0, host.indexOf(".")))) {
							this.urls.add(resource);
						}
					}

				}
			} catch (final BundleException e) {
				e.printStackTrace();

			}
		}

		@Override
		public Enumeration<URL> getResources(final String name)
				throws IOException {
			if (!name.startsWith("META-INF/json")) {
				return Collections.emptyEnumeration();
			}
			this.urls.add(super.getResources(name).nextElement());
			return Collections.enumeration(this.urls);
		}
	}
}