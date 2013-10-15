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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortType;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection;

/**
 * This Node reads images.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class ImgReaderNodeModel<T extends RealType<T> & NativeType<T>> extends
		NodeModel implements BufferedDataTableHolder {

	public static class CombinedIterable<E> implements Iterable<E> {

		private final Iterable<E>[] m_iterables;

		public CombinedIterable(final Iterable<E>... iterables) {
			m_iterables = iterables;

		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public Iterator<E> iterator() {
			return new Iterator<E>() {

				private int m_currentIt;

				private final Iterator<E>[] m_iterators;

				{
					m_currentIt = 0;
					m_iterators = new Iterator[m_iterables.length];
					for (int i = 0; i < m_iterators.length; i++) {
						m_iterators[i] = m_iterables[i].iterator();
					}
				}

				@Override
				public boolean hasNext() {
					if (m_iterators[m_currentIt].hasNext()) {
						return true;
					} else if ((m_currentIt + 1) == m_iterators.length) {
						return false;
					} else {
						m_currentIt++;
						return hasNext();
					}

				}

				@Override
				public E next() {
					return m_iterators[m_currentIt].next();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();

				}

			};
		}
	}

	/**
	 * Key to store the check file format option.
	 */
	public static final String CFG_CHECK_FILE_FORMAT = "check_file_format";

	/**
	 * Key to store if the complete path should be used as row key
	 */
	public static final String CFG_COMPLETE_PATH_ROWKEY = "complete_path_rowkey";

	/**
	 * Key to store the directory history.
	 */
	public static final String CFG_DIR_HISTORY = "imagereader_dirhistory";

	/**
	 * Key for the settings holding the file list.
	 */
	public static final String CFG_FILE_LIST = "file_list";

	/**
	 * Key to store the selected column in the optional input table
	 */
	public static final String CFG_FILENAME_COLUMN = "filename_column";

	/**
	 * Key for the settings holding information if group files modus is wanted
	 */
	public static final String CFG_GROUP_FILES = "group_files";

	/**
	 * Key to store the OME_XML-metadata column option.
	 */
	public static final String CFG_OME_XML_METADATA_COLUMN = "xmlcolumns";

	/**
	 * Key to store the factory used to create the images
	 */
	public static final String CFG_IMG_FACTORY = "img_factory";

	/**
	 * Key to store whether all series should be read
	 */
	public static final String CFG_READ_ALL_SERIES = "read_all_series";

	/**
	 * Key to store the selected series
	 */
	public static final String CFG_SERIES_SELECTION = "series_selection";

	/*
	 * Settings
	 */

	/**
	 * Key for the settings holding selected image planes.
	 */
	public static final String CFG_PLANE_SLECTION = "plane_selection";

	/**
	 * The image out port of the Node.
	 */
	public static final int IMAGEOUTPORT = 0;

	/**
	 * The meta data out port of the Node.
	 */
	public static final int METADATAOUTPORT = 1;

	/**
	 * The available factory types available for selection.
	 */
	public static final String[] IMG_FACTORIES = new String[] {
			"Array Image Factory", "Planar Image Factory", "Cell Image Factory" };

	private final SettingsModelBoolean m_checkFileFormat = new SettingsModelBoolean(
			CFG_CHECK_FILE_FORMAT, true);

	private final SettingsModelBoolean m_completePathRowKey = new SettingsModelBoolean(
			CFG_COMPLETE_PATH_ROWKEY, false);

	/* data table for the table cell viewer */
	private BufferedDataTable m_data;

	private final SettingsModelString m_filenameCol = new SettingsModelString(
			CFG_FILENAME_COLUMN, "");

	/*
	 * Collection of all settings.
	 */

	private final SettingsModelStringArray m_files = new SettingsModelStringArray(
			CFG_FILE_LIST, new String[] {});

	// New in 1.0.2
	private final SettingsModelBoolean m_isGroupFiles = new SettingsModelBoolean(
			CFG_GROUP_FILES, true);

	private final SettingsModelBoolean m_omexmlCol = new SettingsModelBoolean(
			CFG_OME_XML_METADATA_COLUMN, false);

	private final SettingsModelSubsetSelection m_planeSelect = new SettingsModelSubsetSelection(
			CFG_PLANE_SLECTION);

	// new in 1.1
	private final SettingsModelString m_imgFactory = new SettingsModelString(
			CFG_IMG_FACTORY, IMG_FACTORIES[0]);

	private SettingsModelBoolean m_readAllSeries = new SettingsModelBoolean(
			CFG_READ_ALL_SERIES, true);

	private final SettingsModelIntegerBounded m_seriesSelection = new SettingsModelIntegerBounded(
			CFG_SERIES_SELECTION, 0, 0, 1000);

	private final Collection<SettingsModel> m_settingsCollection;

	/**
	 * Initializes the ImageReader
	 */
	public ImgReaderNodeModel() {
		super(new PortType[] { new PortType(BufferedDataTable.class, true) },
				new PortType[] { BufferedDataTable.TYPE });
		m_settingsCollection = new ArrayList<SettingsModel>();
		m_settingsCollection.add(m_files);
		m_settingsCollection.add(m_planeSelect);
		m_settingsCollection.add(m_omexmlCol);
		m_settingsCollection.add(m_filenameCol);
		m_settingsCollection.add(m_completePathRowKey);
		m_settingsCollection.add(m_checkFileFormat);

		// FIXME: should actually not be necessary to disable an dialog
		// component, when the node is added the first time? right?
		m_seriesSelection.setEnabled(false);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		final ReadFileImgTable<T> tab = new ReadFileImgTable<T>(
				m_omexmlCol.getBooleanValue());
		// tab.setDimLabelProperty(m_planeSelect.getDimLabelsAsString());
		return new DataTableSpec[] { tab.getDataTableSpec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		final String[] fnames = m_files.getStringArrayValue();

		// String[] metaDataColumns =
		// m_metadatakeys.getStringArrayValue();

		// table with images from the dialog
		Iterable<String> tableImgList = null;
		Iterable<String> dialogImgList = null;

		if (inData[0] != null) {
			final int colIdx = inData[0].getDataTableSpec().findColumnIndex(
					m_filenameCol.getStringValue());
			if (colIdx >= 0) {
				tableImgList = new Iterable<String>() {
					@Override
					public Iterator<String> iterator() {

						final Iterator<DataRow> rowIt = inData[0].iterator();

						return new Iterator<String>() {
							@Override
							public boolean hasNext() {
								return rowIt.hasNext();
							}

							@Override
							public String next() {
								return ((StringValue) rowIt.next().getCell(
										colIdx)).getStringValue();
							}

							@Override
							public void remove() {
								throw new UnsupportedOperationException();
							}
						};
					}
				};

			}

		}
		if (fnames.length > 0) {
			dialogImgList = Arrays.asList(fnames);
		}

		Iterable<String> imgIt = null;
		int numImages;
		if ((tableImgList != null) && (dialogImgList != null)) {
			imgIt = new CombinedIterable<String>(dialogImgList, tableImgList);
			numImages = fnames.length + inData[0].getRowCount();
		} else if (tableImgList != null) {
			imgIt = tableImgList;
			numImages = inData[0].getRowCount();
		} else {
			imgIt = dialogImgList;
			numImages = fnames.length;
		}

		// create ImgFactory
		ImgFactory<T> imgFac;
		if (m_imgFactory.getStringValue().equals(IMG_FACTORIES[1])) {
			imgFac = new PlanarImgFactory<T>();
		} else if (m_imgFactory.getStringValue().equals(IMG_FACTORIES[2])) {
			// TODO: what is the appropriate cell size?
			imgFac = new CellImgFactory<T>();
		} else {
			imgFac = new ArrayImgFactory<T>();
		}

		// series selection
		int seriesSelection;
		if (m_readAllSeries.getBooleanValue()) {
			seriesSelection = -1;
		} else {
			seriesSelection = m_seriesSelection.getIntValue();
		}

		// create data table
		final ReadFileImgTable<T> dt = new ReadFileImgTable<T>(exec, imgIt,
				numImages, m_planeSelect, m_omexmlCol.getBooleanValue(),
				m_checkFileFormat.getBooleanValue(),
				m_completePathRowKey.getBooleanValue(),
				m_isGroupFiles.getBooleanValue(), seriesSelection, imgFac);

		// dt.setDimLabelProperty(m_planeSelect.getDimLabelsAsString());
		final BufferedDataTable[] out = new BufferedDataTable[] { exec
				.createBufferedDataTable(dt, exec) };

		if (dt.hasAnErrorOccured()) {
			setWarningMessage("Some errors occured opening images or image planes!");
		} else if (!dt.usedDifferentReaders()
				&& m_checkFileFormat.getBooleanValue()
				&& out[0].getRowCount() > 1) {
			// used only one reader and had more than one image
			setWarningMessage("All read files had the same format. To reduce read time uncheck \"Additional Options -> Check file format for each file\".");
		}

		// data table for the table cell viewer
		m_data = out[0];

		return out;
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
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		m_data = null;
		// m_filenameCol.setStringValue(null);
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
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		for (final SettingsModel sm : m_settingsCollection) {
			sm.loadSettingsFrom(settings);
		}

		try {
			// group file setting new in 1.0.2
			m_isGroupFiles.loadSettingsFrom(settings);

			// factory selection new in 1.1
			m_imgFactory.loadSettingsFrom(settings);
			m_readAllSeries.loadSettingsFrom(settings);
			m_seriesSelection.loadSettingsFrom(settings);
		} catch (final Exception e) {
			// nothing to handle
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
		// group file setting new in 1.0.2
		m_isGroupFiles.saveSettingsTo(settings);

		// factory selection new in 1.1
		m_imgFactory.saveSettingsTo(settings);
		m_readAllSeries.saveSettingsTo(settings);
		m_seriesSelection.saveSettingsTo(settings);

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

		try {
			// group file setting new in 1.0.2
			m_isGroupFiles.validateSettings(settings);

			// factory selection new in 1.1
			m_imgFactory.validateSettings(settings);
			m_readAllSeries.validateSettings(settings);
			m_seriesSelection.validateSettings(settings);
		} catch (final Exception e) {
			// nothing to handle
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
