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
package org.knime.knip.io.nodes.imgwriter;

import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.img.ImgPlusValue;

/**
 * Dialog for the Image Writer.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class ImgWriterNodeDialog extends DefaultNodeSettingsPane {

	private final DialogComponentStringSelection m_compression;

	private final DialogComponentColumnNameSelection m_filenameColumn;

	private final DialogComponentStringSelection m_formats;

	// private DialogComponentColumnNameSelection m_omexmlColumn;

	private final ImgWriter m_writer;

	/**
	 * Dialog with Column Selection.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public ImgWriterNodeDialog() {
		super();

		// image column selection
		createNewGroup("Image column to save:");
		addDialogComponent(new DialogComponentColumnNameSelection(
				new SettingsModelString(ImgWriterNodeModel.CFG_IMG_COLUMN_KEY,
						""), "", 0, ImgPlusValue.class));
		closeCurrentGroup();

		// directory selection
		addDialogComponent(new DialogComponentFileChooser(
				new SettingsModelString(ImgWriterNodeModel.CFG_DIRECTORY_KEY,
						""), "imagewriterdirhistory", JFileChooser.OPEN_DIALOG,
				true));

		// filename column selection
		createNewGroup("File names:");
		final SettingsModelString fcol = new SettingsModelString(
				ImgWriterNodeModel.CFG_FILENAME_COLUMN_KEY, "");
		fcol.setEnabled(false);
		m_filenameColumn = new DialogComponentColumnNameSelection(fcol,
				"Column:", 0, false, true, StringValue.class);
		addDialogComponent(m_filenameColumn);
		addDialogComponent(new DialogComponentLabel(
				"Select '<none>' to use the rowID as filename."));
		closeCurrentGroup();

		// format and compression selection
		m_writer = new ImgWriter();
		m_formats = new DialogComponentStringSelection(new SettingsModelString(
				ImgWriterNodeModel.CFG_FORMAT_KEY, m_writer.getWriters()[0]),
				"File format:", Arrays.asList(m_writer.getWriters()));
		m_formats.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				onFormatSelectionChanged();
			}
		});
		String[] compr = m_writer
				.getCompressionTypes(((SettingsModelString) m_formats
						.getModel()).getStringValue());
		if (compr == null) {
			compr = new String[] { "" };
		}
		m_compression = new DialogComponentStringSelection(
				new SettingsModelString(ImgWriterNodeModel.CFG_COMPRESSION_KEY,
						compr[0]), "Compression type", Arrays.asList(compr));
		if (!compr[0].isEmpty()) {
			m_compression.getModel().setEnabled(true);
		} else {
			m_compression.getModel().setEnabled(false);
		}

		// m_omexmlColumn =
		// new DialogComponentColumnNameSelection(new
		// SettingsModelString(
		// ImageWriterNodeModel.OME_XML_COLUMN, ""),
		// "OME-XML Metadata column:", 0, false, true,
		// StringValue.class);
		createNewGroup("Format selection:");
		addDialogComponent(m_formats);
		addDialogComponent(m_compression);
		// addDialogComponent(m_omexmlColumn);
		closeCurrentGroup();

		// additional writer options
		createNewGroup("Writer options:");
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				ImgWriterNodeModel.CFG_OVERWRITE_KEY, false),
				"Overwrite existing files?"));
		closeCurrentGroup();

		createNewTab("Dimension Mapping");
		final String[] labels = KNIMEKNIPPlugin.parseDimensionLabels();
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(ImgWriterNodeModel.CFG_Z_DIM_MAPPING,
						"Z"), "Z label", Arrays.asList(labels)));
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(ImgWriterNodeModel.CFG_C_DIM_MAPPING,
						"Channel"), "Channel label (max. 3 channels used)",
				Arrays.asList(labels)));
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(ImgWriterNodeModel.CFG_T_DIM_MAPPING,
						"Time"), "Time label", Arrays.asList(labels)));

		createNewTab("More Writer Options");
		addDialogComponent(new DialogComponentNumber(new SettingsModelInteger(
				ImgWriterNodeModel.CFG_FRAMERATE_KEY, 10),
				"Frames per second (if applicable)", 1));

	}

	/* called, when another format was selected */
	private void onFormatSelectionChanged() {
		final String[] compr = m_writer
				.getCompressionTypes(((SettingsModelString) m_formats
						.getModel()).getStringValue());

		if (compr != null) {
			m_compression.replaceListItems(Arrays.asList(compr), compr[0]);
			m_compression.getModel().setEnabled(true);
		} else {
			m_compression.getModel().setEnabled(false);
		}

	}

}
