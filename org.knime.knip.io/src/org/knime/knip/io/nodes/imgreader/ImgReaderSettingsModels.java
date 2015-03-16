package org.knime.knip.io.nodes.imgreader;

import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection;

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
	 *@return Model to store whether all series should be read
	 */
	public static final SettingsModelBoolean createReadAllSeriesModel() {
		return new SettingsModelBoolean("read_all_series", true);
	}

	/**
	 *@return Key to store the selected series
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

}
