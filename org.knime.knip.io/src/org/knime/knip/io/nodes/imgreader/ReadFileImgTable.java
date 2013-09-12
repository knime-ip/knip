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

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import loci.formats.FormatException;
import net.imglib2.Pair;
import net.imglib2.img.ImgFactory;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.TypedAxis;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

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
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection;
import org.knime.knip.io.ScifioImgSource;

/**
 * Implements a <code>DataTable</code> that read image data from files.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class ReadFileImgTable<T extends NativeType<T> & RealType<T>> implements
		DataTable {

	/**
	 * Core meta data keys.
	 */
	public static final String[] CORE_METADATA = new String[] { "SizeX",
			"SizeY", "SizeZ", "SizeT", "SizeC", "IsRGB", "PixelType",
			"LittleEndian", "DimensionsOrder", "IsInterleaved" };

	/** The row header prefix that is used if nothing else is specified. */
	public static final String STANDARD_ROW_PREFIX = "SAMPLE_";

	/** Suffix of image files. */
	public static final String SUFFIX = ".tif";

	/* Standard node logger */
	private final NodeLogger LOGGER = NodeLogger
			.getLogger(ReadFileImgTable.class);

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
	public ReadFileImgTable(final ExecutionContext exec,
			final Iterable<String> fileList, final long numberOfFiles,
			final SettingsModelSubsetSelection sel, final boolean omexml,
			final boolean checkFileFormat, final boolean completePathRowKey,
			final boolean isGroupFiles, final int selectedSeries,
			final ImgFactory<T> imgFactory) {

		m_completePathRowKey = completePathRowKey;
		m_fileList = fileList;
		m_numberOfFiles = numberOfFiles;
		m_sel = sel;
		m_exec = exec;
		m_omexml = omexml;
		m_selectedSeries = selectedSeries;
		m_imgSource = new ScifioImgSource(imgFactory, checkFileFormat,
				isGroupFiles);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataTableSpec getDataTableSpec() {
		final int col = 0;

		DataColumnSpecCreator creator;
		final DataColumnSpec[] cspecs = new DataColumnSpec[1 + (m_omexml ? 1
				: 0)];
		creator = new DataColumnSpecCreator("Image", ImgPlusCell.TYPE);
		cspecs[col] = creator.createSpec();

		if (m_omexml) {
			creator = new DataColumnSpecCreator("OME-XML Metadata",
					XMLCell.TYPE);
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

			/* current image of the series contained in one file */
			private int currentSeries = m_selectedSeries == -1 ? 0
					: m_selectedSeries;

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
				} else {
					return fileIterator.hasNext();
				}
			}

			@SuppressWarnings("unchecked")
			@Override
			public DataRow next() {
				String rowKey = null;
				final Vector<DataCell> row = new Vector<DataCell>();

				final DataCell[] result = new DataCell[m_omexml ? 2 : 1];
				try {
					progressCount++;
					if ((currentSeries + 1) < seriesCount
							&& m_selectedSeries == -1) {
						// if there still is a series left and ALL series are
						// selected to be read (m_selectedSeries!=-1)
						currentSeries++;
					} else {
						currentFile = fileIterator.next().trim();
						seriesCount = m_imgSource.getSeriesCount(currentFile);
						currentSeries = m_selectedSeries == -1 ? 0
								: m_selectedSeries;

						idx++;
					}
					if (currentSeries >= seriesCount) {
						LOGGER.warn("Image file only contains " + seriesCount
								+ " series, but series number " + currentSeries
								+ " selected. File skipped!");
					}

					CalibratedAxis[] calibAxes = m_imgSource.getAxes(
							currentFile, currentSeries);

					final Pair<TypedAxis, long[]>[] axisSelectionConstraints = m_sel
							.createSelectionConstraints(m_imgSource
									.getDimensions(currentFile, currentSeries),
									calibAxes);

					// One _can_ be sure that if and only if
					// some dims are removed (as they are of
					// size 1) a optimized iterable interval
					// is created
					final ImgPlus<T> resImgPlus = (ImgPlus<T>) m_imgSource
							.getImg(currentFile, currentSeries,
									axisSelectionConstraints);

					result[0] = cellFactory.createCell(resImgPlus);

					// set row key
					if (m_completePathRowKey) {
						rowKey = resImgPlus.getSource();
					} else {
						rowKey = resImgPlus.getName();
					}

					if (seriesCount > 1) {
						rowKey += "_" + currentSeries;
					}

					// reads the ome xml metadata
					if (m_omexml) {
						result[result.length - 1] = XMLCellFactory
								.create(m_imgSource
										.getOMEXMLMetadata(currentFile));
					}

				} catch (final FormatException e) {
					LOGGER.warn("Format not supported for file " + currentFile
							+ " (" + e.getMessage() + ")");
					m_error = true;

				} catch (final IOException e) {
					LOGGER.error("An IO problem occured while opening the file "
							+ currentFile + " (" + e.getMessage() + ")");
					m_error = true;
				} catch (final Exception e) {
					LOGGER.error(e);
					m_error = true;
				}

				for (final DataCell cell : result) {
					if (cell == null) {
						row.add(DataType.getMissingCell());
						rowKey = currentFile;
					} else {

						row.add(cell);
					}
				}

				DataCell[] rowvalues = new DataCell[row.size()];
				rowvalues = row.toArray(rowvalues);
				m_exec.setProgress((double) progressCount / m_numberOfFiles);

				return new DefaultRow(new RowKey(rowKey), rowvalues);

			}

		};

	}
}
