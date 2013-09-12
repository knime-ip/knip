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

import io.scif.Format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.base.node.dialog.DialogComponentSubsetSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection;
import org.knime.knip.io.ScifioGateway;
import org.knime.knip.io.node.dialog.DialogComponentMultiFileChooser;

/**
 * Dialog for the ImageReader to select the files and choose some additional
 * options.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class ImgReaderNodeDialog extends DefaultNodeSettingsPane {

	public static final FileFilter FILEFILTER;

	static {
		// create file filter
		final List<String> suffices = new ArrayList<String>();

		final Set<Format> formats = ScifioGateway.getFORMATS();
		for (final Format f : formats) {
			for (final String s : f.getSuffixes()) {
				if ((s != null) && (s.length() > 0)) {
					suffices.add(s);
				}
			}
		}

		FILEFILTER = new FileNameExtensionFilter("BioFormats files",
				suffices.toArray(new String[suffices.size()]));

	}

	private final DialogComponentMultiFileChooser m_filechooser;

	private final SettingsModelString m_fileNamesColumn;

	@SuppressWarnings("unchecked")
	ImgReaderNodeDialog() {

		super();

		createNewGroup("");
		m_filechooser = new DialogComponentMultiFileChooser(
				new SettingsModelStringArray(ImgReaderNodeModel.CFG_FILE_LIST,
						new String[] {}), FILEFILTER,
				ImgReaderNodeModel.CFG_DIR_HISTORY);
		addDialogComponent(m_filechooser);
		closeCurrentGroup();

		createNewTab("Additional Options");

		createNewGroup("Output");
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				ImgReaderNodeModel.CFG_OME_XML_METADATA_COLUMN, false),
				"Append additional OME-XML-metadata column"));
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(ImgReaderNodeModel.CFG_IMG_FACTORY,
						ImgReaderNodeModel.IMG_FACTORIES[0]), "Image factory",
				ImgReaderNodeModel.IMG_FACTORIES));
		closeCurrentGroup();

		createNewGroup("File");
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				ImgReaderNodeModel.CFG_COMPLETE_PATH_ROWKEY, false),
				"Use complete file path as row key"));

		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				ImgReaderNodeModel.CFG_CHECK_FILE_FORMAT, true),
				"Check file format for each file (may be slower)"));
		closeCurrentGroup();

		createNewGroup("Optional Inport");
		m_fileNamesColumn = new SettingsModelString(
				ImgReaderNodeModel.CFG_FILENAME_COLUMN, "");
		addDialogComponent(new DialogComponentColumnNameSelection(
				m_fileNamesColumn, "File name column in optional table", 0,
				false, true, StringValue.class));
		closeCurrentGroup();

		createNewGroup("Series & Groups");
		final SettingsModelBoolean smReadAll = new SettingsModelBoolean(
				ImgReaderNodeModel.CFG_READ_ALL_SERIES, true);
		final SettingsModelIntegerBounded smSeriesIdx = new SettingsModelIntegerBounded(
				ImgReaderNodeModel.CFG_SERIES_SELECTION, 0, 0, 1000);
		addDialogComponent(new DialogComponentBoolean(smReadAll,
				"Read all series"));
		addDialogComponent(new DialogComponentNumber(smSeriesIdx,
				"Series index", 1));
		smReadAll.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				smSeriesIdx.setEnabled(!smReadAll.getBooleanValue());
			}
		});
		smSeriesIdx.setEnabled(!smReadAll.getBooleanValue());
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				ImgReaderNodeModel.CFG_GROUP_FILES, true), "Load group files?"));
		closeCurrentGroup();

		createNewTab("Subset Selection");
		createNewGroup("Image Subset Selection");
		addDialogComponent(new DialogComponentSubsetSelection(
				new SettingsModelSubsetSelection(
						ImgReaderNodeModel.CFG_PLANE_SLECTION), true, true,
				new int[] { 0, 1 }));
		closeCurrentGroup();

	}

	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
			final PortObjectSpec[] specs) throws NotConfigurableException {
		super.loadAdditionalSettingsFrom(settings, specs);
	}

	@Override
	public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		if ((m_filechooser.getSelectFiles().length == 0)
				&& (m_fileNamesColumn.getStringValue() == null)) {
			m_filechooser.getFileChooserPanel().onAdd();
		}
		// throw new InvalidSettingsException("No files selected");
		super.saveAdditionalSettingsTo(settings);
	}
}
