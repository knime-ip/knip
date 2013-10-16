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
package org.knime.knip.io.nodes.fileref;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortType;
import org.knime.knip.io.ImgRefCell;
import org.knime.knip.io.ImgSource;
import org.knime.knip.io.ImgSourcePool;
import org.knime.knip.io.ScifioImgSource;
import org.knime.knip.io.nodes.imgreader.ImgReaderNodeModel.CombinedIterable;

/**
 * This Node reads images.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class ImageFileRefNodeModel extends NodeModel implements
		BufferedDataTableHolder {

	/**
	 * The table holding the references.
	 * 
	 * @author hornm, University of Konstanz
	 */
	private class ImageReferenceDataTable implements DataTable {

		private final ExecutionContext m_exec;

		private final int m_numberOfFiles;

		private final Iterable<String> m_refs;

		public ImageReferenceDataTable(final Iterable<String> refs,
				final int numberOfFiles, final ExecutionContext exec) {
			m_refs = refs;
			m_numberOfFiles = numberOfFiles;
			m_exec = exec;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public DataTableSpec getDataTableSpec() {
			final DataColumnSpecCreator creator = new DataColumnSpecCreator(
					"Image Reference", ImgRefCell.TYPE);
			return new DataTableSpec(creator.createSpec());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public RowIterator iterator() {
			if (m_refs == null) {
				return new RowIterator() {
					@Override
					public boolean hasNext() {
						return false;
					}

					@Override
					public DataRow next() {
						return null;
					}

				};
			}
			final Iterator<String> it = m_refs.iterator();

			return new RowIterator() {

				int pos = 0;

				@Override
				public boolean hasNext() {
					return it.hasNext();
				}

				@SuppressWarnings("rawtypes")
				@Override
				public DataRow next() {
					m_exec.setProgress(pos++ / (double) m_numberOfFiles);
					final String ref = it.next();
					return new DefaultRow(new File(ref).getName(),
							new ImgRefCell(
									ScifioImgSource.class.getSimpleName(), ref,
									m_generateThumbnails.getBooleanValue()));

				}

			};
		}
	}

	/**
	 * Key for the settings holding the file list.
	 */
	public static final String CFG_FILE_LIST = "file_list";

	/**
	 * Key to store the optional file name column
	 */
	public static final String CFG_FILENAME_COLUMN = "filename_column";

	/**
	 * Key to store the thumbnail option.
	 */
	public static final String CFG_GENERATE_THUMBNAILS = "generate_thumbnails";

	/**
	 * Key to store the OME_XML-metadata column option.
	 */
	public static final String CFG_OME_XML_METADATA_COLUMN = "xmlcolumns";

	/**
	 * The image out port of the Node.
	 */
	public static final int IMAGEOUTPORT = 0;

	/**
	 * The meta data out port of the Node.
	 */
	public static final int METADATAOUTPORT = 1;

	/*
	 * Collection of all settings.
	 */

	private BufferedDataTable m_data;

	private final SettingsModelString m_fileNameCol = new SettingsModelString(
			CFG_FILENAME_COLUMN, "");

	/*
	 * Settings for the file list.
	 */
	private final SettingsModelStringArray m_files = new SettingsModelStringArray(
			CFG_FILE_LIST, new String[] {});

	private final SettingsModelBoolean m_generateThumbnails = new SettingsModelBoolean(
			CFG_GENERATE_THUMBNAILS, false);

	/*
	 * The image-output-table DataSpec
	 */
	private DataTableSpec m_outspec;

	private final Collection<SettingsModel> m_settingsCollection;

	private ImgSource m_source;

	/**
	 * Initializes the ImageReader
	 */
	public ImageFileRefNodeModel() {
		super(new PortType[] { new PortType(BufferedDataTable.class, true) },
				new PortType[] { BufferedDataTable.TYPE });
		m_settingsCollection = new ArrayList<SettingsModel>();
		m_settingsCollection.add(m_files);
		m_settingsCollection.add(m_generateThumbnails);
		m_settingsCollection.add(m_fileNameCol);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		m_outspec = new ImageReferenceDataTable(null, 0, null)
				.getDataTableSpec();

		return new DataTableSpec[] { m_outspec };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		if (m_outspec == null) {
			final DataTableSpec[] inSpecs = new DataTableSpec[inData.length];
			for (int i = 0; i < inSpecs.length; i++) {
				inSpecs[i] = inData[i].getDataTableSpec();
			}
			configure(inSpecs);
		}
		if (m_source == null) {
			m_source = new ScifioImgSource();
		}
		final String[] fnames = m_files.getStringArrayValue();

		// adds the source to img source pool, to be
		// accessible/available in the
		// img reference cell
		ImgSourcePool.addImgSource(ScifioImgSource.class.getSimpleName(),
				m_source);

		// table with images from the dialog
		Iterable<String> tableImgList = null;
		Iterable<String> dialogImgList = null;

		if (inData[0] != null) {
			final int colIdx = inData[0].getDataTableSpec().findColumnIndex(
					m_fileNameCol.getStringValue());
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

		final ImageReferenceDataTable dt = new ImageReferenceDataTable(imgIt,
				numImages, exec);

		// dt.setDimLabelProperty(m_planeSelect.getDimLabelsAsString());
		final BufferedDataTable[] out = new BufferedDataTable[] { exec
				.createBufferedDataTable(dt, exec) };

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

		m_source = new ScifioImgSource();
		ImgSourcePool.addImgSource(ScifioImgSource.class.getSimpleName(),
				m_source);

		// File f = new File(nodeInternDir, "factory");
		// ModelContentRO model = ModelContent.loadFromXML(new
		// FileInputStream(f));
		// try {
		// // TODO: img factory selection
		// ObjectCache.addSource(new
		// FileImgSource(model.getString("facID"),
		// new ArrayImgFactory()));
		// } catch (InvalidSettingsException e) {
		// NodeLogger.getLogger(ImageFileRefNodeModel.class).error(e);
		// }

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
	public void reset() {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// ImageRepository.removeFactory(m_factoryID);
		// File f = new File(nodeInternDir, "factory");
		// ModelContent model = new ModelContent("FactoryModel");
		// model.addString("facID", m_source.getSourceID());
		// model.saveToXML(new FileOutputStream(f));
	}

	// // Methods for the table cell view ////

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
	public void setInternalTables(final BufferedDataTable[] tables) {
		m_data = tables[0];

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
