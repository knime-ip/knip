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

import io.scif.Format;
import io.scif.FormatException;
import io.scif.SCIFIO;
import io.scif.Writer;
import io.scif.common.DataTools;
import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgIOException;
import io.scif.img.ImgSaver;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.io.ScifioGateway;
import org.scijava.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the functionality to write {@link Img}s using the <a href =
 * "http://loci.wisc.edu/software/scifio">scifio</a>-library.
 *
 *
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class ImgWriter {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ImgWriter.class);

	private static final boolean DEBUG = true; // switch for debug output

	/*
	 * List of the names of the available writers associated with specific
	 * formats.
	 */
	private String[] m_writers;

	/* map from the writer names to the actual writers */
	private HashMap<String, Format> m_mapFormats = null;

	/* Frame rate to use when writing in frames per second, if applicable. */
	private int m_fps = 10;

	/* Handles the writing on disk */
	private ImgSaver m_saver;

	/**
	 * Creates an new writer. *
	 */
	public ImgWriter() {
	}

	/*
	 * Provides access to SCifio methods
	 */
	private final SCIFIO SCIFIO = ScifioGateway.getSCIFIO();

	/* helper to get the list of the supported writers */
	private void retrieveSupportedWriters() {

		if (m_mapFormats == null) {
			final Context con = SCIFIO.getContext();
			m_saver = new ImgSaver(con);

			final Collection<Format> outFormats = SCIFIO.format()
					.getOutputFormats();

			m_writers = new String[outFormats.size()];
			m_mapFormats = new HashMap<String, Format>();

			int i = 0;
			final Iterator<Format> it = outFormats.iterator();
			while (it.hasNext()) {
				final Format f = it.next();
				m_writers[i] = f.getClass().getSimpleName()
						.replace("Format", "")
						+ " (" + f.getSuffixes()[0] + ")";
				m_mapFormats.put(m_writers[i], f);
				i++;
			}

		}
	}

	/**
	 * Returns the list of the possible compression types of the specific
	 * writer.
	 *
	 * @param format
	 *            the name of the writer
	 * @return the list of possible compressions, <code>null</code> if there are
	 *         no compression types
	 */

	public String[] getCompressionTypes(final String format) {
		retrieveSupportedWriters();
		Writer w = null;
		try {
			w = m_mapFormats.get(format).createWriter();
		} catch (final FormatException e) {
			e.printStackTrace();
		}
		if (w == null) {
			return null;
		}
		return w.getCompressionTypes();
	}

	/**
	 * @return the available writers as strings
	 */
	public String[] getWriters() {
		retrieveSupportedWriters();
		return m_writers;
	}

	/**
	 * Gets one suffix normally used to identify the format associated with the
	 * specific writer.
	 *
	 * @param format
	 *            the writer
	 * @return the suffix, e.g. '.tif'
	 * @throws FormatException
	 */
	public String getSuffix(final String format) throws FormatException {
		retrieveSupportedWriters();
		Writer w = null;
		try {
			w = m_mapFormats.get(format).createWriter();
		} catch (final io.scif.FormatException e) {
			throw new FormatException(
					"Could not create writer for selected format " + format
							+ "\n" + e.getMessage());
		} catch (final NullPointerException e) {
			throw new FormatException("No writer for selected format " + format);
		}
		if (w == null) {
			return null;
		}
		return w.getFormat().getSuffixes()[0];
	}

	/**
	 * Writes the image plane stack to the given file. The resulting image
	 * format is determined by the given writer.
	 *
	 * @param img
	 *            the image to be written
	 * @param <T>
	 *            the image type
	 *
	 * @param outfile
	 *            the absolute path of the file to write in
	 * @param writer
	 *            the writer
	 * @param compressionType
	 *            the compression type, if available, can be <code>null</code>.
	 * @param dimMapping
	 *            mapping of the image dimensions (without X and Y) to the
	 *            dimension order ZCT (array must be of length 3), If
	 *            <code>null</code> Z=dim2, C=dim3, T=dim4. If a mapping is -1 ,
	 *            that particular dimensions is assumed to be not existent.
	 * @throws IOException
	 * @throws FormatException
	 * @throws MissingLibraryException
	 * @throws ServiceException
	 * @throws DependencyException
	 */
	public <T extends RealType<T>> void writeImage(final Img<T> img,
			final String outfile, final String format,
			final String compressionType, final int[] dimMapping)
			throws FormatException, IOException  {

		retrieveSupportedWriters();
		writeImage(img, outfile, m_mapFormats.get(format), compressionType,
				dimMapping);

	}

	/**
	 * Writes the image plane stack to the given file. The resulting image
	 * format is determined by the given writer.
	 *
	 * @param img
	 *            the image to be written
	 * @param <T>
	 *            the image type
	 *
	 * @param outfile
	 *            the absolute path of the file to write in
	 * @param writer
	 *            the writer
	 * @param compressionType
	 *            the compression type, if available, can be <code>null</code>.
	 * @param dimMapping
	 *            mapping of the image dimensions (without X and Y) to the
	 *            dimension order ZCT (array must be of length 3), If
	 *            <code>null</code> Z=dim2, C=dim3, T=dim4. If a mapping is -1 ,
	 *            that particular dimensions is assumed to be not existent.
	 * @throws IOException
	 * @throws FormatException
	 * @throws MissingLibraryException
	 * @throws ServiceException
	 * @throws DependencyException
	 */
	public <T extends RealType<T>> void writeImage(final Img<T> img,
			final String outfile, final Format format,
			final String compressionType, final int[] dimMapping)
			throws FormatException, IOException  {

		if (DEBUG) {
			LOGGER.warn("File: " + outfile + " \n Type:"
					+ img.firstElement().getClass().getSimpleName()
					+ "format: " + format.getFormatName());
		}
		final SCIFIOConfig config = new SCIFIOConfig();

		int[] map;
		if ((dimMapping == null) || (dimMapping.length != 3)) {
			map = new int[] { 2, 3, 4 };
		} else {
			map = dimMapping.clone();
		}

		int sizeC = (img.numDimensions() > map[1]) && (map[1] != -1) ? (int) img
				.dimension(2 + map[1]) : 1;
		if (sizeC > 3) {
			LOGGER.warn("Image has more than 3 channels. These channels will be ignored.");
			sizeC = 3;
		}

		final int sizeT = (img.numDimensions() > map[2]) && (map[2] != -1) ? (int) img
				.dimension(2 + map[2]) : 1;

		final int sizeZ = ((img.numDimensions() > map[0]) && (map[0] != -1)) ? (int) img
				.dimension(2 + map[0]) : 1;

		if (img.numDimensions() > 5) {
			LOGGER.warn("Image has more than five dimension. These dimensions will be ignored.");
		}

		config.writerSetFramesPerSecond(m_fps);

		Writer tempWriter = null;
		try {
			tempWriter = format.createWriter();
		} catch (final FormatException e) {
			LOGGER.error(e.getMessage());
		}

		if ((compressionType != null)
				&& (tempWriter.getCompressionTypes() != null)) {
			config.writerSetCompression(compressionType);
		}
		final boolean doStack = tempWriter.canDoStacks();

		if (!doStack && ((sizeT > 1) || (sizeZ > 1))) {
			throw new FormatException(
					"Selected format  doesn't support image stacks.");
		}

		final ImgPlus<T> imp = ImgPlus.wrap(img);

		try {
			tempWriter.isSupportedType(
					SCIFIO.imgUtil().makeType(imp.firstElement()),
					compressionType);

			final int pixeltype = SCIFIO.imgUtil().makeType(imp.firstElement());
			if (!DataTools.containsValue(
					tempWriter.getPixelTypes(compressionType), pixeltype)) {
				throw new ImgIOException(compressionType);
			}

		} catch (final ImgIOException e1) {
			throw new FormatException("Pixeltype "
					+ imp.firstElement().getClass().getSimpleName()
					+ " can't be written in the selected Format.");
		}

		try {
			m_saver.saveImg(outfile, img, config);
		} catch (final ImgIOException e) {
			LOGGER.error("Skipped image: " + imp.getName() + " "
					+ e.getMessage().replace("io.scif.FormatException:", ""));
		} catch (final IncompatibleTypeException e) {
			LOGGER.error(e.getMessage());
		}
	}

	public void setFramesPerSecond(final int fps) {
		m_fps = fps;
	}
}
