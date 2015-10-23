/*
 * ------------------------------------------------------------------------
 *
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
package org.knime.knip.features;

import java.util.Arrays;
import java.util.List;

import org.knime.scijava.core.ResourceAwareClassLoader;
import org.scijava.Context;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.PluginIndex;
import org.scijava.service.Service;
import org.scijava.ui.UIService;
import org.scijava.widget.WidgetService;

import net.imagej.ops.OpService;

/**
 * Encapsulates the {@link OpService} instance as singleton.
 *
 * @author Daniel Seebacher, University of Konstanz.
 */
public final class FeaturesGateway {

	private static FeaturesGateway m_instance;

	private static FeatureService fs;

	private final Context context;

	private FeaturesGateway() {
		// set log level
		System.setProperty("scijava.log.level", "error");

		// required classes
		final List<Class<? extends Service>> classes = Arrays.asList(OpService.class, FeatureService.class,
				WidgetService.class, UIService.class);

		this.context = new Context(classes, new PluginIndex(
				new DefaultPluginFinder(new ResourceAwareClassLoader(getClass().getClassLoader(), getClass()))));

		this.context.inject(this);
	}

	public static FeaturesGateway getInstance() {
		if (m_instance == null) {
			m_instance = new FeaturesGateway();
		}

		return m_instance;
	}

	public Context getContext() {
		return context;
	}

	public static FeatureService fs() {
		if (fs == null) {
			fs = getInstance().context.getService(FeatureService.class);
		}
		return fs;
	}

}