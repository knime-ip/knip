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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorResetEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorFilelistChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerTextMessageChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgWithMetadataChgEvent;
import org.knime.knip.io.ImgSource;
import org.knime.knip.io.ImgSourcePool;
import org.knime.knip.io.ScifioImgSource;
import org.knime.knip.io.nodes.imgreader.FileChooserPanel;
import org.knime.knip.io.nodes.imgreader.ImgReaderNodeDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class AnnotatorFilePanel<T extends RealType<T> & NativeType<T>> extends
		ViewerComponent {

	/* */
	public static final String IMAGE_SOURCE_ID = "ANNOTATOR_IMAGE_SOURCE";

	/* */
	private static final long serialVersionUID = 1L;

	/* Logger */
	private final Logger logger = LoggerFactory
			.getLogger(AnnotatorFilePanel.class);

	/* */
	private FileAssociation[] m_allFiles;

	/* */
	private EventService m_eventService;

	/* */
	private JList m_fileList;

	/* */
	private boolean m_isAdjusting;

	public AnnotatorFilePanel() {
		super("Selected files", false);
		m_isAdjusting = false;
		m_fileList = new JList();
		m_fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_fileList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				if (m_isAdjusting) {
					return;
				}

				onListSelection(e);
			}
		});

		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(150, getPreferredSize().height));
		add(new JScrollPane(m_fileList), BorderLayout.CENTER);

		final JButton browse = new JButton("Browse ...");
		browse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				m_isAdjusting = true;
				final String[] files = FileChooserPanel.showFileChooserDialog(
						ImgReaderNodeDialog.FILEFILTER,
						FileAssociation.getPaths(m_allFiles));
				if (files != null) {

					m_allFiles = FileAssociation.createAssociations(files);
					m_fileList.setListData(m_allFiles);
					m_eventService
							.publish(new AnnotatorFilelistChgEvent(files));

					if (m_fileList != null) {
						m_fileList.setSelectedIndex(0);
						sendFileSelected();
					}

				}
				m_isAdjusting = false;
				updateUI();
			}
		});
		add(browse, BorderLayout.SOUTH);
	}

	@Override
	public Position getPosition() {
		return Position.WEST;
	}

	@Override
	public void loadComponentConfiguration(final ObjectInput in)
			throws IOException {

		final int num = in.readInt();
		m_allFiles = new FileAssociation[num];
		for (int i = 0; i < num; i++) {
			m_allFiles[i] = new FileAssociation(in.readUTF());
		}
		m_isAdjusting = true;
		m_fileList.setListData(m_allFiles);
		m_isAdjusting = false;
		m_fileList.updateUI();
	}

	private void onListSelection(final ListSelectionEvent e2) {
		if (e2.getValueIsAdjusting()) {
			return;
		}
		sendFileSelected();
	}

	@EventListener
	public void reset(final AnnotatorResetEvent e) {
		m_allFiles = new FileAssociation[0];
		m_isAdjusting = true;
		m_fileList.setListData(m_allFiles);
		m_isAdjusting = false;
	}

	@Override
	public void saveComponentConfiguration(final ObjectOutput out)
			throws IOException {
		out.writeInt(m_allFiles.length);
		for (int i = 0; i < m_allFiles.length; i++) {
			out.writeUTF(m_allFiles[i].m_path);
		}
	}

	private void sendFileSelected() {
		try {
			if (ImgSourcePool.getImgSource(IMAGE_SOURCE_ID) == null) {
				ImgSourcePool.addImgSource(IMAGE_SOURCE_ID,
						new ScifioImgSource());
			}
			final ImgSource source = ImgSourcePool
					.getImgSource(IMAGE_SOURCE_ID);

			final String ref = ((FileAssociation) m_fileList.getSelectedValue()).m_path;

			m_eventService.publish(new ImgViewerTextMessageChgEvent(
					"Load img ... " + ref));

			@SuppressWarnings("unchecked")
			final ImgPlus<T> imgPlus = (ImgPlus<T>) source.getImg(ref, 0);

			m_eventService.publish(new ImgWithMetadataChgEvent<T>(imgPlus,
					imgPlus));
		} catch (final IncompatibleTypeException e) {
			logger.warn("Failed to load image");
			m_eventService.publish(new ImgViewerTextMessageChgEvent(
					"Failed to load image"));
		} catch (final Exception e) {
			logger.warn("Failed to load image");
			e.printStackTrace();
		}

	}

	@Override
	public void setEventService(final EventService eventService) {
		m_eventService = eventService;
		eventService.subscribe(this);
	}

}

class FileAssociation {

	static FileAssociation[] createAssociations(final String[] filePaths) {
		final FileAssociation[] ret = new FileAssociation[filePaths.length];
		int i = 0;
		for (final String path : filePaths) {
			ret[i] = new FileAssociation(path);
			i++;
		}

		return ret;
	}

	static String[] getNames(final FileAssociation[] assocs) {
		final String[] names = new String[assocs.length];
		for (int i = 0; i < assocs.length; i++) {
			names[i] = assocs[i].m_name;
		}
		return names;
	}

	static String[] getPaths(final FileAssociation[] assocs) {
		final String[] paths = new String[assocs.length];
		for (int i = 0; i < assocs.length; i++) {
			paths[i] = assocs[i].m_path;
		}
		return paths;
	}

	String m_name;

	String m_path;

	FileAssociation(final String path) {
		this.m_path = path;
		m_name = new File(path).getName();
	}

	public String getPath() {
		return m_path;
	}

	@Override
	public String toString() {
		return m_name;
	}
}
