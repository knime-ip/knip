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

import io.scif.FormatException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;

import org.apache.commons.io.FileUtils;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CloseableRowIterator;
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
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.NodeUtils;

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
public class ImgWriterNodeModel<T extends RealType<T>> extends NodeModel {

	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(ImgWriterNodeModel.class);

	/*
	 * SETTING MODELS
	 */

	private final SettingsModelString m_imgColumn = ImgWriterSettingsModels
			.createImgColumnModel();

	private final SettingsModelColumnName m_filenameColumn = ImgWriterSettingsModels
			.createFileNameColumnModel();

	/*
	 * Custom filename options.
	 */
	private final SettingsModelBoolean m_customNameOption = ImgWriterSettingsModels
			.createUseCustomFileNameModel();

	private final SettingsModelString m_customFileName = ImgWriterSettingsModels
			.createCustomFileNameModel();

	/*
	 * output options.
	 */
	private final SettingsModelBoolean m_overwrite = ImgWriterSettingsModels
			.createOverwriteModel();

	private final SettingsModelBoolean m_forceMkdir = ImgWriterSettingsModels
			.createForceDirCreationModel();

	private final SettingsModelString m_directory = ImgWriterSettingsModels
	.createDirectoryModel();

	private final SettingsModelString m_format = ImgWriterSettingsModels
			.createFormatModel();

	private final SettingsModelString m_compression = ImgWriterSettingsModels
			.createCompressionModel();

	private final SettingsModelInteger m_frameRate = ImgWriterSettingsModels
			.createFrameRateModel();

	/*
	 * MAPPING SETTINGS MODELS.
	 */
	private final SettingsModelString m_tMapping = ImgWriterSettingsModels
			.createTimeMappingModel();

	private final SettingsModelString m_cMapping = ImgWriterSettingsModels
			.createChannelMappingModel();

	private final SettingsModelString m_zMapping = ImgWriterSettingsModels
			.createZMappingModel();

	private final Collection<SettingsModel> m_settingsCollection;

	/**
	 * One input one output.
	 *
	 */
	public ImgWriterNodeModel() {
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
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		if (m_zMapping.getStringValue().equals(m_cMapping.getStringValue())
				|| m_zMapping.getStringValue().equals(
						m_tMapping.getStringValue())
						|| m_cMapping.getStringValue().equals(
								m_tMapping.getStringValue())) {
			throw new InvalidSettingsException(
					"Dimensions must not be mapped to the same label!");
		}

		final int colIndex = inSpecs[0].findColumnIndex(m_imgColumn
				.getStringValue());
		if (colIndex == -1) {
			if ((NodeUtils.autoOptionalColumnSelection(inSpecs[0], m_imgColumn,
					ImgPlusValue.class)) >= 0) {
				setWarningMessage("Auto-configure Image Column: "
						+ m_imgColumn.getStringValue());
			} else {
				throw new InvalidSettingsException("No column selected!");
			}
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		if (m_directory.getStringValue().equals("")) {
			throw new InvalidSettingsException("No output path selected");
		}

		final int imgColIndex = inData[0].getDataTableSpec().findColumnIndex(
				m_imgColumn.getStringValue());
		final String format = m_format.getStringValue();
		final String compression = m_compression.getStringValue();
		String directory = m_directory.getStringValue();
		if (!directory.endsWith("/")) {
			directory += "/";
		}

		final File tempFile = new File(directory);
		if (!tempFile.exists()) {
			if (m_forceMkdir.getBooleanValue()) {
				try {
					LOGGER.warn("Creating directory: " + directory);
					FileUtils.forceMkdir(new File(directory));
				} catch (final IOException e1) {
					LOGGER.error("Selected Path " + directory
							+ " is not a directory");
					throw new IOException(
							"Directory unreachable or file exists with the same name");
				}
			} else {
				throw new IOException(
						"Output directory doesn't exist, you can force the creation in the node settings.");
			}
		}

		final boolean overwrite = m_overwrite.getBooleanValue();
		final boolean useCustomName = m_customNameOption.getBooleanValue();
		final String customName = m_customFileName.getStringValue();

		/* File name number magic */
		// get number of digits needed for padding
		final int digits = (int) Math.log10(inData[0].getRowCount()) + 1;
		final String stringformat = "%0" + digits + "d";
		int imgCount = 0;

		final int nameColIndex = inData[0].getDataTableSpec().findColumnIndex(
				m_filenameColumn.getStringValue());

		final CloseableRowIterator it = inData[0].iterator();

		// loop variables
		ImgPlus<T> img;
		DataRow row;
		String outfile;
		boolean error = false;

		while (it.hasNext()) {
			row = it.next();
			final ImgWriter w = new ImgWriter();
			w.setFramesPerSecond(m_frameRate.getIntValue());

			if (useCustomName) {
				outfile = directory + customName
						+ String.format(stringformat, imgCount);
			} else if (nameColIndex == -1) {
				outfile = directory + row.getKey().getString();
			} else {
				outfile = directory
						+ ((StringValue) row.getCell(nameColIndex))
						.getStringValue();
			}
			outfile += "." + w.getSuffix(format);

			final File f = new File(outfile);
			if (f.exists()) {
				if (overwrite) {
					LOGGER.warn("The file " + outfile
							+ " already exits and will be OVERWRITTEN.");
					f.delete();

				} else {
					LOGGER.warn("The file " + outfile
							+ " already exits and will be SKIPPED.");
					continue;
				}
			}

			img = ((ImgPlusValue<T>) row.getCell(imgColIndex)).getImgPlus();

			// create dimensions mapping
			final int[] map = new int[] { -1, -1, -1 };
			for (int d = 2; d < img.numDimensions(); d++) {
				if (img.axis(d).type().getLabel()
						.equals(m_zMapping.getStringValue())) {
					map[0] = d - 2;
				}
				if (img.axis(d).type().getLabel()
						.equals(m_cMapping.getStringValue())) {
					map[1] = d - 2;
				}
				if (img.axis(d).type().getLabel()
						.equals(m_tMapping.getStringValue())) {
					map[2] = d - 2;
				}
			}

			try {

				w.writeImage(img, outfile, format, compression, map);

			} catch (final FormatException e) {
				LOGGER.error(
						"Error while writing image " + outfile + " : "
								+ e.getMessage(), e);
				error = true;
			} catch (final IOException e) {
				LOGGER.error(
						"Error while writing image " + outfile + " : "
								+ e.getMessage(), e);
				error = true;
			} catch (final UnsupportedOperationException e) {
				LOGGER.error(
						"Error while writing image " + outfile + " : "
								+ "Check the filename for illegal characters! "
								+ e.getMessage(), e);
				error = true;
			}

			exec.setProgress((double) imgCount / inData[0].getRowCount());
			exec.checkCanceled();
			imgCount++;
		}

		if (error) {
			setWarningMessage("Some errors occured during the writing process!");
		}

		return null;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		for (final SettingsModel sm : m_settingsCollection) {
			sm.loadSettingsFrom(settings);
		}
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
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
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
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		for (final SettingsModel sm : m_settingsCollection) {
			sm.validateSettings(settings);
		}
	}

}
