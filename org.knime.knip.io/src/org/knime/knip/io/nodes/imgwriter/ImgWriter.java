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

import java.io.IOException;
import java.util.HashMap;

import loci.common.DataTools;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.ClassList;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.IFormatWriter;
import loci.formats.ImageWriter;
import loci.formats.MetadataTools;
import loci.formats.MissingLibraryException;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import net.imglib2.img.Img;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.sampler.special.OrthoSliceCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.PixelType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the functionality to write {@link Img}s using the <a href =
 * "http://loci.wisc.edu/bio-formats/">bioformats</a>-library.
 * 
 * 
 * @author hornm, University of Konstanz
 */
public class ImgWriter {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ImgWriter.class);

	/*
	 * List of the names of the available writers associated with specific
	 * formats.
	 */
	private String[] m_writers;

	/* map from the writer names to the actual writers */
	private HashMap<String, IFormatWriter> m_mapWriters = null;

	/* Frame rate to use when writing in frames per second, if applicable. */
	private int m_fps = 10;

	/**
	 * Creates an new writer. *
	 */
	public ImgWriter() {
	}

	/* helper to get the list of the supported writers */
	private void retrieveSupportedWriters() {

		if (m_mapWriters == null) {
			loci.formats.ImageWriter writer;
			final ClassList<IFormatWriter> defaultClasses = ImageWriter
					.getDefaultWriterClasses();
			writer = new loci.formats.ImageWriter(defaultClasses);
			final IFormatWriter[] writers = writer.getWriters();
			m_writers = new String[writers.length];
			m_mapWriters = new HashMap<String, IFormatWriter>();
			for (int i = 0; i < writers.length; i++) {

				m_writers[i] = writers[i].getFormat() + " ("
						+ writers[i].getSuffixes()[0] + ")";
				m_mapWriters.put(m_writers[i], writers[i]);

			}
		}
	}

	/**
	 * Returns the list of the possible compression types of the specific
	 * writer.
	 * 
	 * @param writer
	 *            the name of the writer
	 * @return the list of possible compressions, <code>null</code> if there are
	 *         no compression types
	 */

	public String[] getCompressionTypes(final String writer) {
		retrieveSupportedWriters();
		final IFormatWriter w = m_mapWriters.get(writer);
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
	 * @param writer
	 *            the writer
	 * @return the suffix, e.g. '.tif'
	 */
	public String getSuffix(final String writer) {
		retrieveSupportedWriters();
		final IFormatWriter w = m_mapWriters.get(writer);
		if (w == null) {
			return null;
		}
		return w.getSuffixes()[0];
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
			final String outfile, final String writer,
			final String compressionType, final int[] dimMapping)
			throws FormatException, IOException, MissingLibraryException,
			ServiceException, DependencyException {
		retrieveSupportedWriters();
		writeImage(img, outfile, m_mapWriters.get(writer), compressionType,
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
			final String outfile, final IFormatWriter writer,
			final String compressionType, final int[] dimMapping)
			throws FormatException, IOException, MissingLibraryException,
			ServiceException, DependencyException {

		// create metadata object with minimum required metadata
		// fields
		final ServiceFactory factory = new ServiceFactory();
		final OMEXMLService service = factory.getInstance(OMEXMLService.class);
		final IMetadata store = service.createOMEXMLMetadata();

		// retrieve the pixeltype
		PixelType ptype = null;
		int ftptype;
		final T val = img.firstElement().createVariable();
		if (val instanceof BitType) {
			ptype = PixelType.INT8;
			ftptype = FormatTools.INT8;
		} else if (val instanceof ByteType) {
			ptype = PixelType.INT8;
			ftptype = FormatTools.INT8;
		} else if (val instanceof UnsignedByteType) {
			ptype = PixelType.UINT8;
			ftptype = FormatTools.UINT8;
		} else if (val instanceof ShortType) {
			ptype = PixelType.INT16;
			ftptype = FormatTools.INT16;
		} else if (val instanceof UnsignedShortType) {
			ptype = PixelType.UINT16;
			ftptype = FormatTools.UINT16;
		} else if (val instanceof IntType) {
			ptype = PixelType.INT32;
			ftptype = FormatTools.INT32;
		} else if (val instanceof UnsignedIntType) {
			ptype = PixelType.UINT32;
			ftptype = FormatTools.UINT32;
		} else if (val instanceof FloatType) {
			ptype = PixelType.FLOAT;
			ftptype = FormatTools.FLOAT;
		} else if (val instanceof DoubleType) {
			ptype = PixelType.DOUBLE;
			ftptype = FormatTools.DOUBLE;
		} else {
			throw new FormatException("The given image format ("
					+ val.getClass().toString() + ") can't be writen!");
		}

		if (store == null) {
			throw new MissingLibraryException("OME-XML Java library not found.");
		}

		int[] map;
		if ((dimMapping == null) || (dimMapping.length != 3)) {
			map = new int[] { 2, 3, 4 };
		} else {
			map = dimMapping.clone();
		}

		final int numDim = img.numDimensions();
		final int sizeX = (int) img.dimension(0);
		final int sizeY = (int) img.dimension(1);
		final int sizeZ = ((img.numDimensions() > map[0]) && (map[0] != -1)) ? (int) img
				.dimension(2 + map[0]) : 1;
		int sizeC = (img.numDimensions() > map[1]) && (map[1] != -1) ? (int) img
				.dimension(2 + map[1]) : 1;
		if (sizeC > 3) {
			LOGGER.warn("Image has more than 3 channels. These channels will be ignored.");
			sizeC = 3;
		}
		final int sizeT = (img.numDimensions() > map[2]) && (map[2] != -1) ? (int) img
				.dimension(2 + map[2]) : 1;

		MetadataTools.populateMetadata(store, 0, outfile, false, "XYZCT",
				FormatTools.getPixelTypeString(ftptype), sizeX, sizeY, sizeZ,
				sizeC, sizeT, sizeC);

		if (img.numDimensions() > 5) {
			LOGGER.warn("Image has more than five dimension. These dimensions will be ignored.");
		}
		if ((sizeC > 1)
				&& !((val instanceof ByteType) || (val instanceof UnsignedByteType))) {
			throw new FormatException("RGB images must be of type byte!");
		}

		// write image plane to disk

		writer.setMetadataRetrieve(store);
		writer.setFramesPerSecond(m_fps);
		writer.setId(outfile);

		if ((compressionType != null) && (writer.getCompressionTypes() != null)) {
			writer.setCompression(compressionType);
		}

		if (!writer.isSupportedType(ftptype)) {
			final int[] supportedPTypes = writer.getPixelTypes();
			final StringBuffer types = new StringBuffer();
			for (int i = 0; i < supportedPTypes.length; i++) {
				types.append(FormatTools.getPixelTypeString(supportedPTypes[i])
						+ " ");
			}

			throw new FormatException(
					ptype.toString()
							+ " not supported by the selected image format. Supported are "
							+ types.toString() + ".");
		}

		// convert and save slices
		final boolean doStack = writer.canDoStacks();

		if (!doStack && ((sizeT > 1) || (sizeZ > 1))) {
			throw new FormatException(
					"Seleted format doesn't support image stacks.");
		}

		writer.setInterleaved(false);

		final boolean littleEndian = !writer.getMetadataRetrieve()
				.getPixelsBinDataBigEndian(0, 0).booleanValue();

		final IntervalIterator fakeCursor = new IntervalIterator(new int[] {
				sizeZ, sizeT });

		OrthoSliceCursor<T> c;

		final byte[][] planes = new byte[sizeC][];
		long[] pos;
		final int numSteps = sizeT * sizeZ;
		while (fakeCursor.hasNext()) {
			fakeCursor.fwd();

			// iterate through the channels
			for (int i = 0; i < sizeC; i++) {

				final long[] zctPos = new long[] {
						fakeCursor.getLongPosition(0), i,
						fakeCursor.getLongPosition(1) };

				// map xyzct pos to img dimensions
				switch (numDim) {
				case 2:
					pos = new long[] { 0, 0 };
					break;
				case 3:
					pos = new long[3];
					for (int j = 0; j < map.length; j++) {
						if ((map[j] != -1) && (map[j] < 3)) {
							pos[2 + map[j]] = zctPos[j];
							break;
						}
					}
					break;
				case 4:
					pos = new long[4];
					pos[2] = -1;
					for (int j = 0; j < map.length; j++) {
						if ((map[j] != -1) && (map[j] < 4)) {
							if (pos[2] == -1) {
								pos[2 + map[j]] = zctPos[j];
							} else {
								pos[2 + map[j]] = zctPos[j];
								break;
							}
						}
					}
					break;
				// five or more dimensions
				default:
					pos = new long[numDim];
					for (int j = 0; j < map.length; j++) {
						if (map[j] != -1) {
							pos[2 + map[j]] = zctPos[j];
						}
					}
					break;

				}

				c = new OrthoSliceCursor<T>(img, 0, 1, pos);

				if (val instanceof ByteType) {
					planes[i] = new byte[(int) (img.dimension(0) * img
							.dimension(1))];
					while (c.hasNext()) {
						c.fwd();
						planes[i][c.getIntPosition(0)
								+ ((int) img.dimension(0) * c.getIntPosition(1))] = ((ByteType) c
								.get()).get();
					}
					planes[i] = DataTools.makeSigned(planes[i]);

				} else if (val instanceof UnsignedByteType) {
					planes[i] = new byte[(int) (img.dimension(0) * img
							.dimension(1))];
					while (c.hasNext()) {
						c.fwd();
						planes[i][c.getIntPosition(0)
								+ ((int) img.dimension(0) * c.getIntPosition(1))] = (byte) (((UnsignedByteType) c
								.get()).get() - 128);
					}
					planes[i] = DataTools.makeSigned(planes[i]);
				} else if (val instanceof ShortType) {
					final short[] tmp = new short[(int) (img.dimension(0) * img
							.dimension(1))];
					while (c.hasNext()) {
						c.fwd();
						tmp[c.getIntPosition(0)
								+ ((int) img.dimension(0) * c.getIntPosition(1))] = ((ShortType) c
								.get()).get();
					}
					planes[i] = DataTools.shortsToBytes(tmp, littleEndian);

				} else if (val instanceof FloatType) {
					final float[] tmp = new float[(int) (img.dimension(0) * img
							.dimension(1))];
					while (c.hasNext()) {
						c.fwd();
						tmp[c.getIntPosition(0)
								+ ((int) img.dimension(0) * c.getIntPosition(1))] = ((FloatType) c
								.get()).get();
					}
					planes[i] = DataTools.floatsToBytes(tmp, littleEndian);

				} else {
					throw new FormatException(
							"Pixel type not supported by this format.");
				}

			}
			final int index = FormatTools.getIndex(
					DimensionOrder.XYZCT.toString(), sizeZ, 1, sizeT, numSteps,
					fakeCursor.getIntPosition(0), 0,
					fakeCursor.getIntPosition(1));

			// merge channel planes
			if ((sizeC > 1)
					&& ((val instanceof ByteType) || (val instanceof UnsignedByteType))) {
				final byte[] rgb = new byte[planes[0].length * sizeC];

				for (int j = 0; j < sizeC; j++) {
					System.arraycopy(planes[j], 0, rgb, planes[j].length * j,
							planes[j].length);
				}
				writer.saveBytes(index, rgb);

			} else {
				writer.saveBytes(index, planes[0]);
			}

		}

		writer.close();

	}

	public void setFramesPerSecond(final int fps) {
		m_fps = fps;
	}

	/**
	 * All operations to be done to close the image writer.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		//
	}

}
