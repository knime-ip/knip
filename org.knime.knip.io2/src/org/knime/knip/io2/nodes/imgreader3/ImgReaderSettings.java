package org.knime.knip.io2.nodes.imgreader3;

import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;

import io.scif.img.cell.SCIFIOCellImgFactory;

import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleRange;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection2;
import org.knime.knip.core.util.EnumUtils;

/**
 * Settings for the Image Reader nodes
 */
public class ImgReaderSettings {

	/**
	 * Enum that stores the supported image factories
	 *
	 */
	public enum ImgFactoryMode {
	// FIXME update to typed versions of constructors
	ARRAY_IMG("Array Image Factory", new ArrayImgFactory()), PLANAR_IMG("Planar Image Factory", new PlanarImgFactory()),
	CELL_IMG("Cell Image Factory", new SCIFIOCellImgFactory());

		private final String m_name;
		private final ImgFactory m_factory;

		private ImgFactoryMode(final String name, final ImgFactory factory) {
			m_name = name;
			m_factory = factory;
		}

		@Override
		public String toString() {
			return m_name;
		}

		public ImgFactory getFactory() {
			return m_factory;
		}

		public static ImgFactory getFactoryFromName(final String factoryName) {
			return EnumUtils.valueForName(factoryName, ImgFactoryMode.values()).m_factory;
		}
	}

	public static SettingsModelBoolean createCheckFileFormatModel() {
		return new SettingsModelBoolean("Check File format for each file", true);
	}

	public static SettingsModelBoolean createIsGroupFilesModel() {
		return new SettingsModelBoolean("Group Files", true);
	}

	public static SettingsModelBoolean createAppendOMEXMLColModel() {
		return new SettingsModelBoolean("Append OME-XML column", false);
	}

	/**
	 * @return Model for the settings holding selected image planes.
	 */
	public static final SettingsModelSubsetSelection2 createPlaneSelectionModel() {
		return new SettingsModelSubsetSelection2("Plane selection");
	}

	/**
	 * @return Model to store whether all series should be read
	 */
	public static final SettingsModelBoolean createReadAllSeriesModel() {
		return new SettingsModelBoolean("Read all series", true);
	}

	public static SettingsModelDoubleRange createSeriesSelectionRangeModel() {
		return new SettingsModelDoubleRange("Series range selection", 0, Short.MAX_VALUE);
	}

	/**
	 * @return Model to store the factory used to create the images
	 */
	public static SettingsModelString createImgFactoryModel() {
		return new SettingsModelString("Image Factory", ImgFactoryMode.ARRAY_IMG.toString());
	}

	/**
	 * @return Model to store the metadata mode.
	 */
	public static SettingsModelString createMetaDataModeModel() {
		return new SettingsModelString("Metadata Mode", MetadataMode.NO_METADATA.toString());
	}

	/**
	 * @return Model to store whether to read all meta data or not.
	 */
	public static SettingsModelBoolean createReadAllMetaDataModel() {
		return new SettingsModelBoolean("Read all metadata", false);
	}

	public static SettingsModelColumnName createFileURIColumnModel() {
		return new SettingsModelColumnName("File URI column", "");
	}

	public static SettingsModelString createColumnCreationModeModel() {
		return new SettingsModelColumnName("Column creation mode", ColumnCreationMode.APPEND.toString());
	}

	public static SettingsModelString createColumnSuffixNodeModel() {
		return new SettingsModelString("Column suffix ", "_read");
	}

	public static SettingsModelBoolean createAppendSeriesNumberModel() {
		return new SettingsModelBoolean("Append series number", false);
	}
}
