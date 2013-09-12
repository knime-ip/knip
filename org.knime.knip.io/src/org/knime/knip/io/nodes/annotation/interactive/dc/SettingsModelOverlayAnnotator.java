package org.knime.knip.io.nodes.annotation.interactive.dc;

/*
 * ------------------------------------------------------------------------
 *
 * Copyright (C) 2003 - 2010 University of Konstanz, Germany and KNIME GmbH,
 * Konstanz, Germany Website: http://www.knime.org; Email: contact@knime.org
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License, Version 3, as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 *
 * KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 * Hence, KNIME and ECLIPSE are both independent programs and are not
 * derived from each other. Should, however, the interpretation of the GNU
 * GPL Version 3 ("License") under any applicable laws result in KNIME and
 * ECLIPSE being a combined program, KNIME GMBH herewith grants you the
 * additional permission to use and propagate KNIME together with ECLIPSE
 * with only the license terms in place for ECLIPSE applying to ECLIPSE and
 * the GNU GPL Version 3 applying for KNIME, provided the license terms of
 * ECLIPSE themselves allow for the respective use and propagation of
 * ECLIPSE together with KNIME.
 *
 * Additional permission relating to nodes for KNIME that extend the Node
 * Extension (and in particular that are based on subclasses of NodeModel,
 * NodeDialog, and NodeView) and that only interoperate with KNIME through
 * standard APIs ("Nodes"): Nodes are deemed to be separate and independent
 * programs and to not be covered works. Notwithstanding anything to the
 * contrary in the License, the License does not apply to Nodes, you are not
 * required to license Nodes under the License, and you are granted a
 * license to prepare and propagate Nodes, in each case even if such Nodes
 * are propagated with or for interoperation with KNIME. The owner of a Node
 * may freely choose the license terms applicable to such Node, including
 * when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History 31 Aug 2010 (hornm): created
 */

/**
 *
 * @author dietzc, hornm, schoenenbergerf University of Konstanz
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.xmlbeans.impl.util.Base64;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.core.ui.imgviewer.overlay.Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SettingsModel for the annotator. Stores a annotations for an image as
 * overlays. The overlays are associated to the image via the unique image
 * source.
 * 
 * @author dietzc, hornm, zinsmaie
 * 
 * @param <L>
 */
public class SettingsModelOverlayAnnotator<L extends Comparable<L>> extends
		SettingsModel {

	/* Logger */
	private final Logger LOGGER = LoggerFactory
			.getLogger(SettingsModelOverlayAnnotator.class);

	private final String m_configName;

	private HashMap<String, Overlay<L>> m_labelingMap = new HashMap<String, Overlay<L>>();

	public SettingsModelOverlayAnnotator(String configName) {
		m_configName = configName;
	}

	public void setOverlayMap(HashMap<String, Overlay<L>> map) {
		m_labelingMap = map;
	}

	public Map<String, Overlay<L>> getOverlayMap() {
		return m_labelingMap;
	}

	//
	// helpers
	//

	private void saveSettings(NodeSettingsWO settings) {
		// save the labeling hashmap
		try {
			settings.addInt("numOverlayEntries", m_labelingMap.size());

			// save drawings
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(baos);

			for (Entry<String, Overlay<L>> entry : m_labelingMap.entrySet()) {
				// write key
				out.writeObject(entry.getKey());
				// write value
				out.writeObject(entry.getValue());
			}
			out.flush();

			settings.addString("labeling",
					new String(Base64.encode(baos.toByteArray())));
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadSettings(NodeSettingsRO settings) {
		// load the labeling hashmap
		try {
			int numOverlays = settings.getInt("numOverlayEntries");

			// load drawings
			m_labelingMap = new HashMap<String, Overlay<L>>();

			ByteArrayInputStream bais = new ByteArrayInputStream(
					Base64.decode(settings.getString("labeling").getBytes()));
			ObjectInputStream in = new ObjectInputStream(bais);

			for (int i = 0; i < numOverlays; i++) {
				// read key
				String key = (String) in.readObject();
				// read value
				Overlay<L> value = (Overlay<L>) in.readObject();

				m_labelingMap.put(key, value);
			}
			in.close();

		} catch (IOException e) {
			LOGGER.error("IOError while loading annotator", e);
		} catch (ClassNotFoundException e) {
			LOGGER.error("ClassNotFound while loading annotator", e);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//
	// standard methods
	//

	@Override
	protected void loadSettingsForDialog(NodeSettingsRO settings,
			PortObjectSpec[] specs) throws NotConfigurableException {
		loadSettings(settings);
	}

	@Override
	protected void saveSettingsForDialog(NodeSettingsWO settings)
			throws InvalidSettingsException {
		saveSettings(settings);
	}

	@Override
	protected void loadSettingsForModel(NodeSettingsRO settings)
			throws InvalidSettingsException {
		loadSettings(settings);
	}

	@Override
	protected void saveSettingsForModel(NodeSettingsWO settings) {
		saveSettings(settings);
	}

	@Override
	protected <T extends SettingsModel> T createClone() {
		SettingsModelOverlayAnnotator<L> clone = new SettingsModelOverlayAnnotator<L>(
				m_configName);
		clone.setOverlayMap((HashMap<String, Overlay<L>>) m_labelingMap.clone());
		return (T) clone;
	}

	@Override
	protected String getModelTypeID() {
		return "SMID_overlayannotation";
	}

	@Override
	protected String getConfigName() {
		return m_configName;
	}

	@Override
	public String toString() {
		return m_configName;
	}

	@Override
	protected void validateSettingsForModel(NodeSettingsRO settings)
			throws InvalidSettingsException {
		// Nothing to do here
	}
}
