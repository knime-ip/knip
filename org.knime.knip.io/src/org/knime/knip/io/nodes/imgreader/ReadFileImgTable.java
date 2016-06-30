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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection;
import org.knime.knip.io.ScifioImgSource;

import io.scif.config.SCIFIOConfig;
import loci.formats.FormatException;
import net.imagej.ImgPlus;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.TypedAxis;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;

/**
 * Implements a <code>DataTable</code> that read image data from files.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class ReadFileImgTable<T extends NativeType<T> & RealType<T>> implements DataTable {

	/* Standard node logger */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(ReadFileImgTable.class);

	/* connection timeout if images are read from an url */
	private static final int CONNECTION_TIMEOUT = 2000;

	/* read timeout if images are read from an url */
	private static final int READ_TIMEOUT = 10000;

	private boolean m_completePathRowKey;

	/*
	 * Indicates weather an error occured in the current run or not
	 */
	private boolean m_error = false;

	/*
	 * Reference to the execution context to set the progress.
	 */
	private ExecutionContext m_exec;

	/*
	 * points to the files to be read
	 */
	private Iterable<String> m_fileList;

	/*
	 * The image factory to read the image files.
	 */
	private ScifioImgSource m_imgSource = null;

	/* Object that keeps scifio-specific settings */
	private SCIFIOConfig m_scifioConfig = null;

	/*
	 * Number of files to be read
	 */
	private long m_numberOfFiles;

	/*
	 * The option whether to add an ome-xml colmn or not
	 */
	private final boolean m_omexml;

	/*
	 * An array encoding the selected image planes in the 3-dimensional space
	 * (Z,C,T).
	 */
	private SettingsModelSubsetSelection m_sel;

	/*
	 * the selected series to be read
	 */
	private int m_selectedSeries = -1;

	/**
	 * Creates an new and empty ImageTable and is useful to get the table
	 * specification without actually knowing the content.
	 * 
	 * @param omexml
	 *            if true, a omexml column will be appended to the table
	 * 
	 */
	public ReadFileImgTable(final boolean omexml) {
		m_omexml = omexml;
	}

	/**
	 * Constructor for an ImageTable.
	 * 
	 * @param exec
	 *            the execution context (for the progress bar)
	 * @param fileList
	 *            the files to be opened
	 * @param numberOfFiles
	 *            the number of files
	 * @param sel
	 *            the subset selection
	 * @param omexml
	 *            if true, a ome-xml column will be appended to the table
	 * @param checkFileFormat
	 *            checks the file format newly for each single file (might be a
	 *            bit slower)
	 * @param completePathRowKey
	 *            if true, the complete path will be used as row key, else only
	 *            the file name
	 * @param isGroupFiles
	 *            if true all files which are referenced from the current file
	 *            will also be loaded (e.g. in flex files, files from one
	 *            experiment link each other in the header of the file format).
	 * @param selectedSeries
	 *            the series to be read, if an image file contains multiple
	 *            series, if -1 all series will be read
	 * @param imgFactory
	 *            the image factory used to create the individual images
	 * 
	 * 
	 */
	public ReadFileImgTable(final ExecutionContext exec, final Iterable<String> fileList, final long numberOfFiles,
			final SettingsModelSubsetSelection sel, final boolean omexml, final boolean checkFileFormat,
			final boolean completePathRowKey, final boolean isGroupFiles, final int selectedSeries,
			final ImgFactory<T> imgFactory) {

		m_completePathRowKey = completePathRowKey;
		m_fileList = fileList;
		m_numberOfFiles = numberOfFiles;
		m_sel = sel;
		m_exec = exec;
		m_omexml = omexml;
		m_selectedSeries = selectedSeries;
		m_scifioConfig = new SCIFIOConfig().groupableSetGroupFiles(isGroupFiles);
		m_imgSource = new ScifioImgSource(imgFactory, checkFileFormat, m_scifioConfig);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataTableSpec getDataTableSpec() {
		final int col = 0;

		DataColumnSpecCreator creator;
		final DataColumnSpec[] cspecs = new DataColumnSpec[1 + (m_omexml ? 1 : 0)];
		creator = new DataColumnSpecCreator("Image", ImgPlusCell.TYPE);
		cspecs[col] = creator.createSpec();

		if (m_omexml) {
			creator = new DataColumnSpecCreator("OME-XML Metadata", XMLCell.TYPE);
			cspecs[cspecs.length - 1] = creator.createSpec();
		}

		return new DataTableSpec(cspecs);
	}

	/**
	 * 
	 * @return true, if an error occurred while iterating through the filelist
	 *         to open the images.
	 */
	public boolean hasAnErrorOccured() {
		return m_error;
	}

	/**
	 * @return true if more than one reader class was necessary to open the
	 *         files
	 */
	public boolean usedDifferentReaders() {
		return m_imgSource.usedDifferentReaders();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RowIterator iterator() {

		if (m_fileList == null) {
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

		/* Cellfactory to create image cells */
		final ImgPlusCellFactory cellFactory = new ImgPlusCellFactory(m_exec);

		/*
		 * Iterate over the row
		 */
		return new RowIterator() {

			/* id of the current file */
			private String currentFile;

			/*
			 * base row key for the currently read file (possibly with the
			 * series id appended to it later on)
			 */
			private String rowKey;

			/* current image of the series contained in one file */
			private int currentSeries = m_selectedSeries == -1 ? 0 : m_selectedSeries;

			/* iterator over filelist */
			private final Iterator<String> fileIterator = m_fileList.iterator();

			/* the current iterator position */
			private int idx = 0;

			private int progressCount = 0;

			/* number of series */
			private int seriesCount = 0;

			{
				m_error = false;
			}

			@Override
			public boolean hasNext() {
				if (m_selectedSeries == -1 && (currentSeries + 1) < seriesCount) {
					return true;
				}
				return fileIterator.hasNext();
			}

			@SuppressWarnings("unchecked")
			@Override
			public DataRow next() {
				final Vector<DataCell> row = new Vector<DataCell>();

				final DataCell[] result = new DataCell[m_omexml ? 2 : 1];
				try {
					if ((currentSeries + 1) < seriesCount && m_selectedSeries == -1) {
						// if there still is a series left and ALL series are
						// selected to be read (m_selectedSeries!=-1)
						currentSeries++;
					} else {
						/* prepare next file name and the according row key */
						progressCount++;

						final String tmp = fileIterator.next().trim().replaceAll(" ", "%20");
						// final String tmp =
						// URLEncoder.encode(fileIterator.next().trim(),
						// "UTF-8");
						try {
							currentFile = ResolverUtil.resolveURItoLocalOrTempFile(new URL(tmp).toURI())
									.getAbsolutePath();
						} catch (final MalformedURLException | URISyntaxException e) {
							if (tmp.contains("knime://knime.workflow")) {
								currentFile = tmp.replaceAll("knime://knime.workflow",
										ResolverUtil
												.resolveURItoLocalOrTempFile(new URL("knime://knime.workflow").toURI())
												.getAbsolutePath());
							} else {
								currentFile = tmp;
							}

						}

						rowKey = currentFile;

						// download file and return new file path, if
						// 'currentFile' is an url
						String newLoc;
						if ((newLoc = downloadFileFromURL(currentFile)) != null) {
							rowKey = currentFile;
							currentFile = newLoc;
						} else {
							// if file wasn't downloaded from a url, re-set
							// row-key
							// check whether file exists
							File f = new File(currentFile);
							if (!f.exists()) {
								throw new KNIPException("Image " + rowKey + " doesn't exist!");
							}
							if (!m_completePathRowKey) {
								rowKey = f.getName();
							}

						}

						seriesCount = m_imgSource.getSeriesCount(currentFile);
						currentSeries = m_selectedSeries == -1 ? 0 : m_selectedSeries;

						idx++;
					}
					if (currentSeries >= seriesCount) {
						LOGGER.warn("Image file only contains " + seriesCount + " series, but series number "
								+ currentSeries + " selected. File skipped!");
					}

					List<CalibratedAxis> calibAxes = m_imgSource.getAxes(currentFile, currentSeries);

					final Pair<TypedAxis, long[]>[] axisSelectionConstraints = m_sel.createSelectionConstraints(
							m_imgSource.getDimensions(currentFile, currentSeries),
							calibAxes.toArray(new CalibratedAxis[calibAxes.size()]));

					// reads the ome xml metadata
					if (m_omexml) {
						result[result.length - 1] = XMLCellFactory.create(m_imgSource.getOMEXMLMetadata(currentFile));
					}

					// One _can_ be sure that if and only if
					// some dims are removed (as they are of
					// size 1) an optimized iterable interval
					// is created
					final ImgPlus<T> resImgPlus = (ImgPlus<T>) m_imgSource.getImg(currentFile, currentSeries,
							axisSelectionConstraints);

					result[0] = cellFactory.createCell(resImgPlus);

				} catch (final FormatException e) {
					LOGGER.warn("Format not supported for image " + rowKey + " (" + e.getMessage() + ")");
					m_error = true;

				} catch (final IOException e) {
					LOGGER.error(
							"An IO problem occured while opening the image " + rowKey + " (" + e.getMessage() + ")");
					m_error = true;
				} catch (final KNIPException e) {
					LOGGER.warn(e.getLocalizedMessage());
					m_error = true;
				} catch (final Exception e) {
					LOGGER.error(e);
					e.printStackTrace();
					m_error = true;
				}

				for (final DataCell cell : result) {
					if (cell == null) {
						row.add(DataType.getMissingCell());
					} else {
						row.add(cell);
					}
				}

				DataCell[] rowvalues = new DataCell[row.size()];
				rowvalues = row.toArray(rowvalues);
				m_exec.setProgress((double) progressCount / m_numberOfFiles);

				// add series count to row key, if the current file is a series
				// of images
				RowKey rk;
				if (seriesCount > 1) {
					rk = new RowKey(rowKey + "_" + currentSeries);
				} else {
					rk = new RowKey(rowKey);
				}
				return new DefaultRow(rk, rowvalues);

			}

		};

	}

	/**
	 * Allows on to specify additional settings that can not be passed via the
	 * constructor (e.g. scifio specific settings). They have to be passed
	 * immediately after the instantiation of the {@link ReadFileImgTable}
	 * -object, otherwise they might not be regarded.
	 * 
	 * @param key
	 * @param value
	 */
	public void set(String key, Object value) {
		m_scifioConfig.put(key, value);
	}

	/*
	 * Downloads a file specified by an url and saves it to the tmp directory,
	 * returns the respective file path, if its not an url, null will be
	 * returned. If the given url is a 'file-url' it returns the actual absolute
	 * path of the specified file.
	 */
	private String downloadFileFromURL(String s) throws KNIPException {
		// check if the given url really is an url
		try {
			URL url = new URL(s);

			// special handling if its a file url (just return the actual
			// complete path
			if (url.getProtocol().equalsIgnoreCase("file")) {
				return FileUtil.getFileFromURL(url).getAbsolutePath();
			}

			// try to extract the suffix from the file in the url (sometimes
			// necessary for the scifio readers to identify the right reader)
			String file = url.getFile();
			int idx = file.lastIndexOf(".");
			String suffix = file.substring(idx, Math.min(idx + 4, file.length() - 1));

			// create tmp file and copy data from url
			File tmpFile = FileUtil.createTempFile(FileUtil.getValidFileName(url.toString(), -1), suffix);
			FileUtils.copyURLToFile(url, tmpFile, CONNECTION_TIMEOUT, READ_TIMEOUT);
			return tmpFile.getAbsolutePath();
		} catch (MalformedURLException e) {
			return null;
		} catch (IOException e) {
			throw new KNIPException("Can't create temporary file to download image from URL (" + s + ").", e);
		}
	}
}
