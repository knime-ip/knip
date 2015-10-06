/**
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
package org.knime.knip.io.nodes.imgreader2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleRange;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection2;
import org.knime.knip.core.types.NativeTypes;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * This Node reads images.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 * @author <a href="mailto:gabriel.einsdorf@uni.kn"> Gabriel Einsdorf</a>
 * @author <a href="mailto:danielseebacher@t-online.de">Daniel Seebacher,
 *         University of Konstanz.</a>
 */
public abstract class AbstractImgReaderNodeModel<T extends RealType<T> & NativeType<T>> extends NodeModel
		implements BufferedDataTableHolder {

	/**
	 * The available factory types available for selection.
	 */
	public static final String[] IMG_FACTORIES = new String[] { "Array Image Factory", "Planar Image Factory",
			"Cell Image Factory" };

	public static final String[] PIXEL_TYPES = { NativeTypes.BITTYPE.toString(), NativeTypes.BYTETYPE.toString(),
			NativeTypes.DOUBLETYPE.toString(), NativeTypes.FLOATTYPE.toString(), NativeTypes.INTTYPE.toString(),
			NativeTypes.LONGTYPE.toString(), NativeTypes.SHORTTYPE.toString(), NativeTypes.UNSIGNEDSHORTTYPE.toString(),
			NativeTypes.UNSIGNED12BITTYPE.toString(), NativeTypes.UNSIGNEDINTTYPE.toString(),
			NativeTypes.UNSIGNEDBYTETYPE.toString(), "< Automatic >" };

	/* data table for the table cell viewer */
	protected BufferedDataTable m_data;

	/*
	 * **********************************************************************
	 * ************** SETTINGS MODELS CREATOR
	 **********************************************************************/
	/**
	 * @return Model to store the check file format option.
	 */
	public static final SettingsModelBoolean createCheckFileFormatModel() {
		return new SettingsModelBoolean("check_file_format", true);
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
	public static final SettingsModelSubsetSelection2 createPlaneSelectionModel() {
		return new SettingsModelSubsetSelection2("plane_selection");
	}

	/**
	 * @return Model to store whether all series should be read
	 */
	public static final SettingsModelBoolean createReadAllSeriesModel() {
		return new SettingsModelBoolean("read_all_series", true);
	}

	// /**
	// * @return Key to store the selected series
	// */
	// public static final SettingsModelIntegerBounded
	// createSeriesSelectionModel() {
	// return new SettingsModelIntegerBounded("series_selection", 0, 0, 1000);
	// }

	public static SettingsModelDoubleRange createSeriesSelectionRangeModel() {
		return new SettingsModelDoubleRange("series_range_selection", 0, Short.MAX_VALUE);
	}

	/**
	 * @return Model to store the factory used to create the images
	 */
	public static SettingsModelString createImgFactoryModel() {
		return new SettingsModelString("img_factory", IMG_FACTORIES[0]);
	}

	/**
	 * @return Model to store the metadata mode.
	 */
	public static SettingsModelString createMetaDataModeModel() {
		return new SettingsModelString("metadata_mode", MetadataMode.NO_METADATA.toString());
	}

	/**
	 * @return Model to store whether to read all meta data or not.
	 */
	public static SettingsModelBoolean createReadAllMetaDataModel() {
		return new SettingsModelBoolean("read_all_metadata", false);
	}

	public static SettingsModelString createPixelTypeModel() {
		return new SettingsModelString("m_pixeltype", "< Automatic >");
	}

	/*
	 * **********************************************************************
	 * ************** SETTINGS MODELS
	 **********************************************************************/
	protected final SettingsModelBoolean m_checkFileFormat = createCheckFileFormatModel();

	// New in 1.0.2
	protected final SettingsModelBoolean m_isGroupFiles = createIsGroupFilesModel();
	protected final SettingsModelSubsetSelection2 m_planeSelect = createPlaneSelectionModel();

	// new in 1.1
	protected final SettingsModelString m_imgFactory = createImgFactoryModel();
	protected final SettingsModelBoolean m_readAllSeries = createReadAllSeriesModel();
	// protected final SettingsModelIntegerBounded m_seriesSelection =
	// createSeriesSelectionModel();
	protected final SettingsModelDoubleRange m_seriesRangeSelection = createSeriesSelectionRangeModel();

	// new in 1.3
	protected final SettingsModelString m_metadataModeModel = createMetaDataModeModel();
	protected final SettingsModelBoolean m_readAllMetaDataModel = createReadAllMetaDataModel();

	protected final SettingsModelString m_pixelType = createPixelTypeModel();

	protected final List<SettingsModel> m_settingsCollection = new ArrayList<>();

	/**
	 * Initializes the ImageReader
	 */
	public AbstractImgReaderNodeModel(int numInPorts, int numOutPorts) {
		super(numInPorts, numOutPorts);

		m_settingsCollection.add(m_checkFileFormat);
		m_settingsCollection.add(m_isGroupFiles);
		m_settingsCollection.add(m_planeSelect);
		m_settingsCollection.add(m_imgFactory);
		m_settingsCollection.add(m_readAllSeries);
		// m_settingsCollection.add(m_seriesSelection);
		m_settingsCollection.add(m_seriesRangeSelection);
		m_settingsCollection.add(m_metadataModeModel);
		m_settingsCollection.add(m_readAllMetaDataModel);
		m_settingsCollection.add(m_pixelType);

		// m_seriesSelection.setEnabled(false);
		m_seriesRangeSelection.setEnabled(false);
	}

	protected void addSettingsModels(SettingsModel... additionalSettingModels) {
		for (SettingsModel sm : additionalSettingModels) {
			m_settingsCollection.add(sm);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BufferedDataTable[] getInternalTables() {
		return new BufferedDataTable[] { m_data };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		m_data = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		for (final SettingsModel sm : m_settingsCollection) {
			sm.loadSettingsFrom(settings);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		for (final SettingsModel sm : m_settingsCollection) {
			sm.saveSettingsTo(settings);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		for (final SettingsModel sm : m_settingsCollection) {
			sm.validateSettings(settings);
		}
	}

	// // Methods for the table cell view ////

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setInternalTables(final BufferedDataTable[] tables) {
		m_data = tables[0];
	}

}
