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
package org.knime.knip.io.nodes.imgwriter2;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.core.util.MinimaUtils;

import io.scif.FormatException;
import net.imagej.ImgPlus;
import net.imglib2.type.numeric.RealType;

/**
 *
 *
 * @param <T>
 *            image type
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 *
 */
public class ImgWriter2NodeModel<T extends RealType<T>> extends NodeModel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(ImgWriter2NodeModel.class);

	/*
	 * SETTING MODELS
	 */

	private final SettingsModelString m_imgColumn = ImgWriter2SettingsModels.createImgColumnModel();

	private final SettingsModelColumnName m_filenameColumn = ImgWriter2SettingsModels.createFileNameColumnModel();

	/*
	 * Custom filename options.
	 */
	private final SettingsModelBoolean m_customNameOption = ImgWriter2SettingsModels.createUseCustomFileNameModel();

	private final SettingsModelString m_customFileName = ImgWriter2SettingsModels.createCustomFileNameModel();

	/*
	 * Output options.
	 */
	private final SettingsModelBoolean m_overwrite = ImgWriter2SettingsModels.createOverwriteModel();

	private final SettingsModelBoolean m_forceMkdir = ImgWriter2SettingsModels.createForceDirCreationModel();

	private final SettingsModelString m_directory = ImgWriter2SettingsModels.createDirectoryModel();

	private SettingsModelBoolean m_useAbsolutePaths = ImgWriter2SettingsModels.createAbsolutePathsModel();

	private final SettingsModelString m_format = ImgWriter2SettingsModels.createFormatModel();

	private final SettingsModelString m_compression = ImgWriter2SettingsModels.createCompressionModel();

	private final SettingsModelInteger m_frameRate = ImgWriter2SettingsModels.createFrameRateModel();

	private SettingsModelBoolean m_writeSequentially = ImgWriter2SettingsModels.createWriteSequentiallyModel();

	/*
	 * MAPPING SETTINGS MODELS.
	 */
	private final SettingsModelString m_tMapping = ImgWriter2SettingsModels.createTimeMappingModel();

	private final SettingsModelString m_cMapping = ImgWriter2SettingsModels.createChannelMappingModel();

	private final SettingsModelString m_zMapping = ImgWriter2SettingsModels.createZMappingModel();

	private final Collection<SettingsModel> m_settingsCollection;

	/**
	 * One input one output.
	 *
	 */
	public ImgWriter2NodeModel() {
		super(1, 0);
		// for state consistency:
		m_customFileName.setEnabled(false);

		m_settingsCollection = new ArrayList<SettingsModel>();
		m_settingsCollection.add(m_directory);
		m_settingsCollection.add(m_filenameColumn);
		m_settingsCollection.add(m_imgColumn);
		m_settingsCollection.add(m_format);
		m_settingsCollection.add(m_compression);
		m_settingsCollection.add(m_overwrite);
		m_settingsCollection.add(m_zMapping);
		m_settingsCollection.add(m_cMapping);
		m_settingsCollection.add(m_tMapping);
		m_settingsCollection.add(m_frameRate);
		m_settingsCollection.add(m_forceMkdir);
		m_settingsCollection.add(m_customNameOption);
		m_settingsCollection.add(m_customFileName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		// check if dimension mapping is valid
		checkDimensionMapping();

		// check if configured filename column is still available
		String imgNamColumn = m_filenameColumn.getStringValue();

		// not a newly created node
		if (imgNamColumn != null && !imgNamColumn.equals("") && !inSpecs[0].containsName(imgNamColumn)) {
			throw new InvalidSettingsException(
					"The configured Filename column: '" + imgNamColumn + "' is no longer avaiable!");
		}

		// check img column
		String imgColumn = m_imgColumn.getStringValue();
		if (imgColumn.equals("")) { // newly created node
			if ((NodeUtils.autoOptionalColumnSelection(inSpecs[0], m_imgColumn, ImgPlusValue.class)) >= 0) {
				setWarningMessage("Auto-configure Image Column: " + m_imgColumn.getStringValue());
			} else {
				throw new InvalidSettingsException("No Image column avaiable!");
			}
		} else if (!inSpecs[0].containsName(imgColumn)) {
			throw new InvalidSettingsException(
					"The configured image column: '" + imgColumn + "' is no longer avaiable!");
		}
		return null;
	}

	/**
	 * Checks that the dimension mappings, making sure no dimensions are mapped to
	 * the same label.
	 *
	 * @throws InvalidSettingsException
	 *             when dimensions are mapped to the same labels.
	 */
	private void checkDimensionMapping() throws InvalidSettingsException {
		if (m_zMapping.getStringValue().equals(m_cMapping.getStringValue())
				|| m_zMapping.getStringValue().equals(m_tMapping.getStringValue())
				|| m_cMapping.getStringValue().equals(m_tMapping.getStringValue())) {
			throw new InvalidSettingsException("Dimensions must not be mapped to the same label!");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		run(new DataTableRowInput(inData[0]), exec, inData[0].size(), inData[0].getDataTableSpec());

		return null;

	}

	protected void run(final RowInput inData, final ExecutionContext exec, final long size, final DataTableSpec spec)
			throws InvalidSettingsException, IOException, URISyntaxException, MalformedURLException, FormatException,
			CanceledExecutionException, InvalidPathException, InterruptedException {
		checkDimensionMapping();

		String directory = null;
		// only initialize output directory when needed
		if (!m_useAbsolutePaths.getBooleanValue()) {
			Path folderPath = createOutputPath();
			directory = folderPath.toString();
			if (!directory.endsWith("/")) { // fix directory path
				directory += "/";
			}
		}

		// loop constants
		final boolean overwrite = m_overwrite.getBooleanValue();
		final boolean useCustomName = m_customNameOption.getBooleanValue();
		final boolean useAbsolutPaths = m_useAbsolutePaths.getBooleanValue();
		final boolean forceMkdir = m_forceMkdir.getBooleanValue();
		final String customName = m_customFileName.getStringValue();
		final int nameColIndex = spec.findColumnIndex(m_filenameColumn.getStringValue());
		final int imgColIndex = spec.findColumnIndex(m_imgColumn.getStringValue());
		final String format = m_format.getStringValue();
		final String compression = m_compression.getStringValue();

		/* File name number magic */
		// get number of digits needed for padding
		final int digits;
		if (size == -1) {
			// very magic
			digits = (int) 7;
		} else {
			digits = (int) Math.log10(size) + 1;
		}
		final String digitStringFormat = "%0" + digits + "d";
		int imgCount = 0;

		// loop variables
		ImgPlus<T> img;
		String outfile;
		boolean error = false;

		final ImgWriter2 writer = new ImgWriter2().setWriteSequantially(m_writeSequentially.getBooleanValue())
				.setFramesPerSecond(m_frameRate.getIntValue());

		DataRow row;
		while ((row = inData.poll()) != null) {

			// set the filename
			if (useAbsolutPaths) {
				if (nameColIndex == -1) {
					outfile = row.getKey().getString();
				} else {
					outfile = ((StringValue) row.getCell(nameColIndex)).getStringValue();
				}
			} else if (useCustomName) {
				outfile = directory + customName + String.format(digitStringFormat, imgCount);
			} else if (nameColIndex == -1) {
				outfile = directory + row.getKey().getString();
			} else { // file name column configured
				try {
					outfile = directory + ((StringValue) row.getCell(nameColIndex)).getStringValue();
				} catch (ClassCastException e1) {
					throw new IllegalArgumentException("Missing value in the filename column in row: " + row.getKey());
				}
			}
			outfile += "." + writer.getSuffix(format);

			// handle file location
			final File f = FileUtil.resolveToPath(FileUtil.toURL(outfile)).toFile();

			if (f.exists()) {
				if (overwrite) {
					LOGGER.warn("The file " + outfile + " already exists and will be OVERWRITTEN.");
					f.delete();

				} else {
					throw new InvalidSettingsException(
							"The file " + outfile + " exists and must not be overwritten due to user settings.");
				}
				// filename contained path to non existent directory
			} else if (!f.getParentFile().exists()) {
				if (forceMkdir) {
					LOGGER.info("Creating directory: " + f.getParentFile().getPath());
					FileUtils.forceMkdir(f.getParentFile());
				} else {
					throw new InvalidSettingsException("Output directory " + f.getParentFile().getPath()
							+ " doesn't exist, you can force the creation" + " in the node settings.");
				}
			}

			try {
				img = MinimaUtils.getZeroMinImgPlus(((ImgPlusValue<T>) row.getCell(imgColIndex)).getImgPlus());
			} catch (ClassCastException e) {
				throw new IllegalArgumentException("Missing value in the img column in row: " + row.getKey());
			}

			// create dimensions mapping
			final int[] map = createDimMapping(img);

			try {
				writer.writeImage(img, FileUtil.resolveToPath(FileUtil.toURL(outfile)).toString(), format, compression,
						map);

			} catch (final FormatException | IOException e) {
				LOGGER.error("Error while writing image " + outfile + " : " + e.getMessage(), e);
				error = true;
			} catch (final UnsupportedOperationException e) {
				LOGGER.error("Error while writing image " + outfile + " : "
						+ "Check the filename for illegal characters! " + e.getMessage(), e);
				error = true;
			}

			exec.setProgress((double) imgCount / size);
			exec.checkCanceled();
			imgCount++;
		}

		if (error) {
			setWarningMessage("Some errors occured during the writing process!");
		}
	}

	/**
	 * Creates and checks the output folder.
	 *
	 * @return the Path of the output folder.
	 * @throws InvalidSettingsException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	private Path createOutputPath()
			throws InvalidSettingsException, IOException, URISyntaxException, MalformedURLException {
		// check and locate target folder
		try {
			CheckUtils.checkDestinationDirectory(m_directory.getStringValue());
		} catch (InvalidSettingsException e) {
			// allow creation of nonexistent directories if set in the options.
			if (e.getMessage().endsWith("does not exist")) {
				if (!m_forceMkdir.getBooleanValue()) { // option not set
					throw new InvalidSettingsException("Output directory doesn't exist, "
							+ "you can force the creation in the" + " node settings.");
				}
			} else {
				// don't hide other exceptions
				throw e;
			}
		}
		Path folderPath = FileUtil.resolveToPath(FileUtil.toURL(m_directory.getStringValue()));
		if (folderPath == null) {
			throw new InvalidSettingsException("Could not locate:" + m_directory.getStringValue()
					+ ", are you trying to write to an non local directory?");
		}

		// handle target folder
		final File folderFile = folderPath.toFile();
		if (!folderFile.exists()) { // create nonexistent folders must be set to
									// reach this point!.
			try {
				LOGGER.info("Creating directory: " + m_directory.getStringValue());
				FileUtils.forceMkdir(folderFile);
			} catch (final IOException e1) {
				LOGGER.error("Selected Path " + folderPath + " is not a directory");
				throw new IOException("Directory unreachable or file exists with the same name");
			}
		}
		return folderPath;
	}

	/**
	 * Creates the dimension mapping for the given Img.
	 *
	 * @param img
	 *            the image
	 * @return the dimension mapping
	 */
	private int[] createDimMapping(ImgPlus<T> img) {
		final int[] map = new int[] { -1, -1, -1 };
		for (int d = 2; d < img.numDimensions(); d++) {
			if (img.axis(d).type().getLabel().equals(m_zMapping.getStringValue())) {
				map[0] = d - 2;
			}
			if (img.axis(d).type().getLabel().equals(m_cMapping.getStringValue())) {
				map[1] = d - 2;
			}
			if (img.axis(d).type().getLabel().equals(m_tMapping.getStringValue())) {
				map[2] = d - 2;
			}
		}
		return map;
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
	protected void reset() {
		//
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
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		for (final SettingsModel sm : m_settingsCollection) {
			sm.saveSettingsTo(settings);
		}
		try {
			// new in 1.4.0
			m_useAbsolutePaths.saveSettingsTo(settings);
		} catch (Exception e) {
			//
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
		try {
			// new in 1.4.0
			m_useAbsolutePaths.validateSettings(settings);
		} catch (Exception e) {
			//
		}
	}

	@Override
	public StreamableOperator createStreamableOperator(PartitionInfo partitionInfo, PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		return new StreamableOperator() {

			@Override
			public void runFinal(PortInput[] inputs, PortOutput[] outputs, ExecutionContext exec) throws Exception {
				run(((RowInput) inputs[0]), exec, -1, (DataTableSpec) inSpecs[0]);
			}
		};
	}

	@Override
	public InputPortRole[] getInputPortRoles() {
		return new InputPortRole[] { InputPortRole.DISTRIBUTED_STREAMABLE };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		for (final SettingsModel sm : m_settingsCollection) {
			sm.loadSettingsFrom(settings);
		}
		try {
			// new in 1.4.0
			m_useAbsolutePaths.loadSettingsFrom(settings);
		} catch (Exception e) {
			//
		}
	}

}
