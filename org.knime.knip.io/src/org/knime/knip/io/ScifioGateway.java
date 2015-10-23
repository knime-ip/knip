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
package org.knime.knip.io;

import java.util.HashSet;
import java.util.Set;

import org.knime.scijava.core.ResourceAwareClassLoader;
import org.scijava.Context;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.PluginIndex;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;

import io.scif.SCIFIO;
import io.scif.SCIFIOService;
import io.scif.ome.services.OMEXMLService;
import io.scif.services.FormatService;

/**
 * Encapsulates the scifio instance as singleton.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class ScifioGateway {

	private static ScifioGateway m_instance;

	/** the scifio instance. */
	private final SCIFIO m_scifio;

	/**
	 * load supported formats and create the SCIFIO instance.
	 */
	private ScifioGateway() {
		// set log level
		System.setProperty("scijava.log.level", "error");

		// add old IFormatReaders
		// addIFormatReaders();

		// required classes
		HashSet<Class<? extends Service>> classes = new HashSet<>();
		classes.add(SciJavaService.class);
		classes.add(SCIFIOService.class);
		classes.add(OMEXMLService.class);

		// create a scifio context with required Scifio and Scijava Services
		m_scifio = new SCIFIO(new Context(classes, new PluginIndex(
				new DefaultPluginFinder(new ResourceAwareClassLoader(getClass()
						.getClassLoader(), getClass())))));
	}

	private static synchronized ScifioGateway getInstance() {
		if (m_instance == null) {
			m_instance = new ScifioGateway();
		}
		return m_instance;
	}

	/**
	 * @return the single SCIFIO instance
	 */
	public static SCIFIO getSCIFIO() {
		return getInstance().m_scifio;
	}

	// TODO: This should be handled on SCIFIO side
	public static String[] getAllSuffixes() {
		final String[] allFormats = format().getSuffixes();

		// avoid incompatible suffixes
		Set<String> validSuffixes = new HashSet<>();
		for (final String format : allFormats) {
			if (format != null && format.length() > 0) {
				validSuffixes.add(format);
			}
		}

		return validSuffixes.toArray(new String[validSuffixes.size()]);
	}

	public static FormatService format() {
		return getInstance().m_scifio.format();
	}
}
