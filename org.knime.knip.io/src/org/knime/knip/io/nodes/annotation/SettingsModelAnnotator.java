package org.knime.knip.io.nodes.annotation;

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

import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.overlay.Overlay;
import org.knime.knip.io.ImgSource;
import org.knime.knip.io.ImgSourcePool;
import org.knime.knip.io.ScifioImgSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SettingsModel for the annotator
 * 
 * @author dietzc, hornm
 * 
 * @param <L>
 */
public class SettingsModelAnnotator<L extends Comparable<L>> extends
		SettingsModel {

	public static <T extends RealType<T> & NativeType<T>> ImgPlus<T> loadImgPlus(
			final String key) throws Exception {
		final ImgSource src = ImgSourcePool
				.getImgSource(AnnotatorFilePanel.IMAGE_SOURCE_ID);
		@SuppressWarnings("unchecked")
		final ImgPlus<T> img = (ImgPlus<T>) src.getImg(key, 0);

		return img;
	}

	/* Logger */
	private final Logger logger = LoggerFactory
			.getLogger(SettingsModelAnnotator.class);

	/*
	 * The complete annotations as base64 coded string to store the result in a
	 * XML file
	 */
	private String m_base64Coded;

	/* Config name of the settings model */
	private final String m_configName;

	/* Map to store the overlays for different Img */
	private Map<String, Overlay<L>> m_overlayMap;

	/**
	 * @param configName
	 *            ConfigName of the SettingsModel
	 */
	public SettingsModelAnnotator(final String configName) {
		m_configName = configName;
		m_overlayMap = new HashMap<String, Overlay<L>>();
		m_base64Coded = "";
		if (ImgSourcePool.getImgSource(AnnotatorFilePanel.IMAGE_SOURCE_ID) == null) {
			ImgSourcePool.addImgSource(AnnotatorFilePanel.IMAGE_SOURCE_ID,
					new ScifioImgSource());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected <T extends SettingsModel> T createClone() {
		final SettingsModelAnnotator<L> clone = new SettingsModelAnnotator<L>(
				m_configName);
		clone.setOverlayMapAndComponents(m_base64Coded, m_overlayMap);
		return (T) clone;
	}

	/**
	 * @return Base64 String representation of the complete {@link ImgViewer}
	 *         (Annotator) and it's settings
	 */
	public String getBase64CodedImgViewer() {
		return m_base64Coded;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getConfigName() {
		return m_configName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getModelTypeID() {
		return "SMID_overlayselection";
	}

	/**
	 * 
	 * @return The overlay map containing the different {@link Overlay} for
	 *         different {@link Img}
	 */
	public Map<String, Overlay<L>> getOverlayMap() {
		return m_overlayMap;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsForDialog(final NodeSettingsRO settings,
			final PortObjectSpec[] specs) throws NotConfigurableException {
		try {
			loadSettingsForModel(settings);
		} catch (final InvalidSettingsException e) {
			logger.error("Error while loading settings for annotator dialog", e);
		}
	}

	/**
	 * Load the Base64 coded String representation of the annotator in the
	 * settingsmodel and deseralizes the {@link Overlay} for the different
	 * {@link Img}
	 * 
	 * {@inheritDoc}
	 * 
	 * @throws InvalidSettingsException
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void loadSettingsForModel(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		try {
			m_base64Coded = settings.getString("base64");
			m_overlayMap.clear();

			final ByteArrayInputStream bais = new ByteArrayInputStream(
					settings.getByteArray("overlays"));

			final ObjectInputStream in = new ObjectInputStream(bais);

			for (int i = 0; i < settings.getInt("numOverlayEntries"); i++) {
				m_overlayMap.put(in.readUTF(), (Overlay<L>) in.readObject());
			}

			in.close();
		} catch (final IOException e) {
			logger.error("IOError while loading annotator", e);
		} catch (final ClassNotFoundException e) {
			logger.error("ClassNotFound while loading annotator", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsForDialog(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		saveSettingsForModel(settings);
	}

	/**
	 * Adds the Base64 coded String representation of the annotator to the
	 * settingsmodel and writes the {@link Overlay} for the different
	 * {@link Img}
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsForModel(final NodeSettingsWO settings) {
		try {
			settings.addString("base64", m_base64Coded);
			settings.addInt("numOverlayEntries", m_overlayMap.size());

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final ObjectOutputStream out = new ObjectOutputStream(baos);
			for (final Entry<String, Overlay<L>> entry : getOverlayMap()
					.entrySet()) {
				out.writeUTF(entry.getKey());
				out.writeObject(entry.getValue());
			}

			// TODO: Faster
			settings.addByteArray("overlays", baos.toByteArray());

			out.close();
		} catch (final IOException e) {

			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @param base64Coded
	 *            the complete information about the annotation as base64coded
	 *            string
	 * @param overlayMap
	 *            Map to store the overlays for different {@link Img}
	 */
	public void setOverlayMapAndComponents(final String base64Coded,
			final Map<String, Overlay<L>> overlayMap) {
		m_overlayMap = overlayMap;
		m_base64Coded = base64Coded;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return m_configName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettingsForModel(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// Nothing to do here
	}

}
