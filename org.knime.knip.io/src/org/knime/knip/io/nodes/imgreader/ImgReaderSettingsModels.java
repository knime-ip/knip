/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2015
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

import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection;

/**
 * Settings Models for the Img Reader.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn"> Gabriel Einsdorf</a>
 */
class ImgReaderSettingsModels {

	/**
	 * The available factory types available for selection.
	 */
	public static final String[] IMG_FACTORIES = new String[] {
			"Array Image Factory", "Planar Image Factory", "Cell Image Factory" };

	/**
	 * Key to store the directory history.
	 */
	public static final String CFG_DIR_HISTORY = "imagereader_dirhistory";

	/**
	 * Key for the settings holding information if group files modus is wanted
	 */
	public static final String CFG_GROUP_FILES = "group_files";

	/**
	 * Enum that stores the metadata modes.
	 */
	public enum MetadataMode {
		NO_METADATA("No metadata"), APPEND_METADATA("Append a metadata column"), METADATA_ONLY(
				"Only metadata");

		private final String name;

		MetadataMode(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/*
	 * Settings
	 */

	/**
	 * @return Model to store the check file format option.
	 */
	public static final SettingsModelBoolean createCheckFileFormatModel() {
		return new SettingsModelBoolean("check_file_format", true);
	}

	/**
	 * @return Model to store if the complete path should be used as row key
	 */
	public static final SettingsModelBoolean createCompletePathRowKeyModel() {
		return new SettingsModelBoolean("complete_path_rowkey", false);
	}

	/**
	 * @return Model to store the selected column in the optional input table
	 */
	public static final SettingsModelString createFilenameColumnModel() {
		return new SettingsModelString("filename_column", "");
	}

	// New in 1.0.2
	public static final SettingsModelBoolean createIsGroupFilesModel() {
		return new SettingsModelBoolean("group_files", true);
	}

	/**
	 * @return Model to store the OME_XML-metadata column option.
	 */
	public static final SettingsModelBoolean createAppendOmexmlColModel() {
		return new SettingsModelBoolean("xmlcolumns", false);
	}

	/**
	 * @return Model for the settings holding selected image planes.
	 */
	public static final SettingsModelSubsetSelection createPlaneSelectionModel() {
		return new SettingsModelSubsetSelection("plane_selection");
	}

	/**
	 * @return Model to store whether all series should be read
	 */
	public static final SettingsModelBoolean createReadAllSeriesModel() {
		return new SettingsModelBoolean("read_all_series", true);
	}

	/**
	 * @return Key to store the selected series
	 */
	public static final SettingsModelIntegerBounded createSeriesSelectionModel() {
		return new SettingsModelIntegerBounded("series_selection", 0, 0, 1000);
	}

	/**
	 * @return Model to store the factory used to create the images
	 */
	public static SettingsModelString createImgFactoryModel() {
		return new SettingsModelString("img_factory", IMG_FACTORIES[0]);
	}

	/**
	 * @return Model for the settings holding the file list.
	 */
	public static SettingsModelStringArray createFileListModel() {
		return new SettingsModelStringArray("file_list", new String[] {});

	}

	/**
	 * @return Model to store the metadata mode.
	 */
	public static SettingsModelString createMetaDataModeModel() {
		return new SettingsModelString("metadata_mode",
				MetadataMode.NO_METADATA.toString());
	}

}
