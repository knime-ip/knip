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
package org.knime.knip.io.nodes.imgreader;

import java.util.ArrayList;
import java.util.Collection;

import loci.formats.IFormatReader;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class IFormatReaderExtPointManager {

	/**
	 * The attribute of the iformatreader view extension point pointing to the
	 * factory class
	 */
	public static final String EXT_POINT_ATTR_DF = "IFormatReader";

	/** The id of the IFormatReaderExtPoint extension point. */
	public static final String EXT_POINT_ID = "org.knime.knip.base.IFormatReader";

	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(IFormatReaderExtPointManager.class);

	public static Collection<IFormatReader> getIFormatReaders() {

		final Collection<IFormatReader> readers = new ArrayList<IFormatReader>();

		try {
			final IExtensionRegistry registry = Platform.getExtensionRegistry();
			final IExtensionPoint point = registry
					.getExtensionPoint(EXT_POINT_ID);
			if (point == null) {
				LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
				throw new IllegalStateException("ACTIVATION ERROR: "
						+ " --> Invalid extension point: " + EXT_POINT_ID);
			}
			for (final IConfigurationElement elem : point
					.getConfigurationElements()) {
				final String operator = elem.getAttribute(EXT_POINT_ATTR_DF);
				final String decl = elem.getDeclaringExtension()
						.getUniqueIdentifier();

				if ((operator == null) || operator.isEmpty()) {
					LOGGER.error("The extension '" + decl
							+ "' doesn't provide the required attribute '"
							+ EXT_POINT_ATTR_DF + "'");
					LOGGER.error("Extension " + decl + " ignored.");
					continue;
				}

				try {
					final IFormatReader reader = (IFormatReader) elem
							.createExecutableExtension(EXT_POINT_ATTR_DF);
					readers.add(reader);
				} catch (final Exception t) {
					LOGGER.error("Problems during initialization of "
							+ "IFormatReaderExtPoint View (with id '"
							+ operator + "'.)");
					if (decl != null) {
						LOGGER.error("Extension " + decl + " ignored.", t);
					}
				}
			}
		} catch (final Exception e) {
			LOGGER.error("Exception while registering "
					+ "IFormatReader extensions");
		}

		return readers;

	}

	private IFormatReaderExtPointManager() {
		// utility class
	}

}
