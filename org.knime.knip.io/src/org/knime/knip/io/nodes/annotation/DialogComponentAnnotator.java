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
package org.knime.knip.io.nodes.annotation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;

import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.apache.xmlbeans.impl.util.Base64;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorLabelPanel;
import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorManager;
import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorResetEvent;
import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorToolbar;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;
import org.knime.knip.core.ui.imgviewer.panels.ImgNormalizationPanel;
import org.knime.knip.core.ui.imgviewer.panels.MinimapPanel;
import org.knime.knip.core.ui.imgviewer.panels.PlaneSelectionPanel;
import org.knime.knip.core.ui.imgviewer.panels.RendererSelectionPanel;
import org.knime.knip.core.ui.imgviewer.panels.TransparencyPanel;
import org.knime.knip.core.ui.imgviewer.panels.infobars.ImgViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.providers.OverlayBufferedImageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class DialogComponentAnnotator<T extends RealType<T> & NativeType<T>>
		extends DialogComponent {

	/* Logger */
	private final Logger logger = LoggerFactory
			.getLogger(DialogComponentAnnotator.class);

	/* Img viewer in which the img/labeling is displayed */
	private AnnotatorImgViewer m_annotator;

	/* AnnotatorManager to manage the annotations and annotation tools */
	private AnnotatorManager<T> m_manager;

	/**
	 * Constructor
	 * 
	 * @param model
	 *            SettingsModel to load previous made annotations
	 * 
	 */
	public DialogComponentAnnotator(final SettingsModelAnnotator<String> model) {
		super(model);
		createAnnotator();

	}

	/**
	 * Constructor
	 * 
	 * @param model
	 *            SettingsModel to load previous made annotations
	 * @param initialClasses
	 *            init classes
	 * 
	 */
	public DialogComponentAnnotator(final SettingsModelAnnotator<String> model,
			final String... initialClasses) {
		super(model);
		createAnnotator(initialClasses);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
			throws NotConfigurableException {
		// Empty Block
	}

	public void close() {
		m_annotator.getEventService().publish(new ViewClosedEvent());
	}

	/* Annotator is constructed from various ViewerComponents */
	private void createAnnotator(final String... defaultLabels) {
		m_manager = new AnnotatorManager<T>();

		m_annotator = new AnnotatorImgViewer();

		m_annotator
				.addViewerComponent(new OverlayBufferedImageProvider<T, String>());
		m_annotator.addViewerComponent(m_manager);

		m_annotator.addViewerComponent(new AnnotatorFilePanel<T>());
		m_annotator.addViewerComponent(new AnnotatorLabelPanel(defaultLabels));
		m_annotator
				.addViewerComponent(AnnotatorToolbar.createStandardToolbar());
		m_annotator.addViewerComponent(new MinimapPanel());
		m_annotator.addViewerComponent(new ImgNormalizationPanel<T, Img<T>>());
		m_annotator.addViewerComponent(new PlaneSelectionPanel<T, Img<T>>());
		m_annotator.addViewerComponent(new RendererSelectionPanel<T>());
		m_annotator.addViewerComponent(new TransparencyPanel());
		m_annotator.addViewerComponent(new ImgViewInfoPanel<T>());
		m_annotator.addViewerComponent(new AnnotatorImgCanvas<T>());
		updateComponent();

		getComponentPanel().setLayout(
				new BoxLayout(getComponentPanel(), BoxLayout.X_AXIS));
		getComponentPanel().add(m_annotator);
		// m_annotator.setParent(canvas);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setEnabledComponents(final boolean enabled) {
		// Empty Block
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setToolTipText(final String text) {
		// Nothing to do here
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void updateComponent() {
		try {
			m_annotator.reset();
			m_annotator
					.setComponentConfiguration(((SettingsModelAnnotator<String>) getModel())
							.getBase64CodedImgViewer());
		} catch (final IOException e) {
			logger.error("IOError while loading annotator");
			e.printStackTrace();
		} catch (final ClassNotFoundException e) {
			logger.error("ClassCast exception while loading annotator");
			e.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		try {
			((SettingsModelAnnotator<String>) getModel())
					.setOverlayMapAndComponents(
							m_annotator.getComponentConfiguration(),
							m_manager.getOverlayMap());

		} catch (final IOException e) {
			logger.error("Error while saving Annotations", e);
		}
	}

	private class AnnotatorImgViewer extends ImgViewer {
		public void reset() {
			m_eventService.publish(new AnnotatorResetEvent());
		}

		/**
		 * Save the current settings/state of an ImgViewer to a base64 coded
		 * String
		 * 
		 * @return base64 coded String of the current settings/state of the
		 *         viewer
		 * @throws IOException
		 */
		public String getComponentConfiguration() throws IOException {
			String res = "";
			try {
				final ByteArrayOutputStream totalBytes = new ByteArrayOutputStream();
				final ObjectOutputStream totalOut = new ObjectOutputStream(
						totalBytes);

				totalOut.writeInt(m_viewerComponents.size());

				ByteArrayOutputStream componentBytes;
				ObjectOutput componentOutput;
				for (final ViewerComponent c : m_viewerComponents) {

					componentBytes = new ByteArrayOutputStream();
					componentOutput = new ObjectOutputStream(componentBytes);
					c.saveComponentConfiguration(componentOutput);
					componentOutput.close();
					totalOut.writeUTF(c.getClass().getSimpleName());

					totalOut.writeInt(componentBytes.size());
					totalOut.write(componentBytes.toByteArray());
					totalOut.flush();

				}
				totalOut.close();
				res = new String(new Base64().encode(totalBytes.toByteArray()));

			} catch (final IOException e) {
				e.printStackTrace();
			}

			return res;

		}

		/**
		 * Loading settings of the viewer from a base64Coded String produced by
		 * {@link ImgViewer#getComponentConfiguration()}
		 * 
		 * @param base64coded
		 *            the base64 representation of the viewer state
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		public void setComponentConfiguration(final String base64coded)
				throws IOException, ClassNotFoundException {
			final Map<String, ObjectInput> configMap = new HashMap<String, ObjectInput>();

			if (base64coded.equals("")) {
				return;
			}

			try {
				final byte[] bytes = new Base64()
						.decode(base64coded.getBytes());
				final ObjectInputStream totalIn = new ObjectInputStream(
						new ByteArrayInputStream(bytes));

				final int num = totalIn.readInt();

				String title = null;
				byte[] buf;
				for (int i = 0; i < num; i++) {
					title = totalIn.readUTF();
					final int len = totalIn.readInt();
					buf = new byte[len];

					for (int b = 0; b < len; b++) {
						buf[b] = totalIn.readByte();
					}
					configMap.put(title, new ObjectInputStream(
							new ByteArrayInputStream(buf)));
				}

				totalIn.close();
			} catch (final IOException e) {
				LoggerFactory.getLogger(ImgViewer.class).error("error", e);
				return;
			}

			for (final ViewerComponent c : m_viewerComponents) {
				final ObjectInput oi = configMap.get(c.getClass()
						.getSimpleName());
				if (oi != null) {
					c.loadComponentConfiguration(oi);
				}
			}
		}

	}

}
