package org.knime.knip.io.nodes.imgwriter2;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

final class ImgWriter2SettingsModels {

    /**
     * CONSTANTS
     */
    static final String DIRHISTORY_KEY = "imagewriterdirhistory";

    /**
     * SETTINGS MODELS
     */

    /**
     * @return Model to store the Img Column.
     */
    static SettingsModelString createImgColumnModel() {
        return new SettingsModelString("img_column_key", "");
    }

    /**
     * @return Model to store the directory settings.
     */
    static SettingsModelString createDirectoryModel() {
        return new SettingsModelString("directory_key", "") {
            @Override
            protected void validateSettingsForModel(NodeSettingsRO settings)
                    throws InvalidSettingsException {
                if (settings.getString("directory_key").equals("")) {
                    throw new InvalidSettingsException(
                            "Output directory must not be empty!");
                }
                super.validateSettingsForModel(settings);
            }
        };
    }

    /**
     * @return Model to store if non existing directories are created.
     */
    static SettingsModelBoolean createForceDirCreationModel() {
        return new SettingsModelBoolean("forceMKDIR_key", false);
    }

    /**
     * @return Model to store the column of the file names.
     */
    static SettingsModelColumnName createFileNameColumnModel() {
        return new SettingsModelColumnName("columnname_key", "");
    }

    /**
     * @return Model to store if a custom file name is used.
     */
    static SettingsModelBoolean createUseCustomFileNameModel() {
        return new SettingsModelBoolean("custom_filename_option_key", false);
    }

    /**
     * @return Model to store the custom file name.
     */
    static SettingsModelString createCustomFileNameModel() {
        return new SettingsModelString("custom_filename_key", "image_");
    }

    /**
     * @return Model to store the selected format.
     */
    static SettingsModelString createFormatModel() {
        return new SettingsModelString("format_key", "");
    }

    /**
     * @return Model to store the frame rate.
     */
    static SettingsModelInteger createFrameRateModel() {
        return new SettingsModelInteger("framerate_key", 10);
    }

    /**
     * @return Model to store the compression settings.
     */
    static SettingsModelString createCompressionModel() {
        return new SettingsModelString("compression_key", "");
    }

    /**
     * @return Model to store if existing files are overwritten.
     */
    static SettingsModelBoolean createOverwriteModel() {
        return new SettingsModelBoolean("overwrite_key", false);
    }

    /**
     * @return Model to store the Channel Mapping.
     */
    static SettingsModelString createChannelMappingModel() {
        return new SettingsModelString("c_dim_mapping_key", "Channel");
    }

    /**
     * @return Model to store the Z Mapping.
     */
    static SettingsModelString createZMappingModel() {
        return new SettingsModelString("z_dim_mapping_key", "Z");
    }

    /**
     * @return Model to store the Time Mapping.
     */
    static SettingsModelString createTimeMappingModel() {
        return new SettingsModelString("t_dim_mapping_key", "Time");
    }

    /**
     * @return Model to store the if the writers should write sequentially.
     */
	static SettingsModelBoolean createWriteSequentiallyModel() {
		return new SettingsModelBoolean("write_sequential_key", true);
	}
}
