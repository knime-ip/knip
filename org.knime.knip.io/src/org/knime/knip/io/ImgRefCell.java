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
package org.knime.knip.io;

import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import net.imglib2.display.ColorTable;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.DefaultCalibratedAxis;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.node.NodeLogger;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.img.ImgPlusValue;

/**
 * This cell only holds references (file, db, ...) to a specific image file and
 * loads it, when needed. It doesn't imports the data behind the reference to
 * the knime internal image representation.
 * 
 * 
 * Furthermore the cell keeps and serialises a thumbnail locally (together with
 * the knime data), if it was generated. If no thumbnail exists, it will be
 * retrieved from the according {@link ImgSource}.
 * 
 * @param <T>
 *            image type
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
@SuppressWarnings("serial")
public class ImgRefCell<T extends RealType<T> & NativeType<T>> extends
		BlobDataCell implements ImgRefValue, ImgPlusValue<T>, StringValue {

	/** Factory for (de-)serializing a ImageRefCell. */
	@SuppressWarnings("rawtypes")
	private static class ImageRefSerializer implements
			DataCellSerializer<ImgRefCell> {
		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public ImgRefCell deserialize(final DataCellDataInput input)
				throws IOException {
			final String sourceID = input.readUTF();
			final String imgRef = input.readUTF();

			final ImgRefCell res = new ImgRefCell(sourceID, imgRef);
			res.m_thumb = null;
			if (input.readByte() == 1) {
				// deserialize thumbnail
				res.m_originalDims = new long[input.readInt()];
				for (int i = 0; i < res.m_originalDims.length; i++) {
					res.m_originalDims[i] = input.readLong();
				}
				if (input instanceof InputStream) {
					res.m_thumb = ImageIO.read((InputStream) input);
				}

			}

			return res;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void serialize(final ImgRefCell cell,
				final DataCellDataOutput output) throws IOException {
			output.writeUTF(cell.m_sourceID);
			output.writeUTF(cell.m_imgRef);

			// flag, if the thumbnail is null
			if (cell.m_thumb == null) {
				output.write(0);
			} else {
				output.write(1);
				// serialize thumbnail
				output.writeInt(cell.m_originalDims.length);
				for (int i = 0; i < cell.m_originalDims.length; i++) {
					output.writeLong(cell.m_originalDims[i]);
				}

				if (output instanceof OutputStream) {
					ImageIO.write(cell.m_thumb, "bmp", (OutputStream) output);
				}

			}

		}
	}

	private static final String AXES_SUFFIX = "axes";

	private static final String CALIBRATION_SUFFIX = "cal";

	private static final String DIM_SUFFIX = "dim";

	private static final String IMG_SUFFIX = "img";

	private static NodeLogger LOGGER = NodeLogger.getLogger(ImgRefCell.class);

	private static final ImageRefSerializer SERIALIZER = new ImageRefSerializer();

	/**
	 * Convenience access member for
	 * <code>DataType.getType(StringCell.class)</code>.
	 * 
	 * @see DataType#getType(Class)
	 */
	public static final DataType TYPE = DataType.getType(ImgRefCell.class);

	/**
	 * Returns the factory to read/write DataCells of this class from/to a
	 * DataInput/DataOutput. This method is called via reflection.
	 * 
	 * @return A serializer for reading/writing cells of this kind.
	 * @see DataCell
	 */
	public static final ImageRefSerializer getCellSerializer() {
		return SERIALIZER;
	}

	/**
	 * Returns the preferred value class of this cell implementation. This
	 * method is called per reflection to determine which is the preferred
	 * renderer, comparator, etc.
	 * 
	 * @return ImageValue.class;
	 */
	public static final Class<? extends DataValue> getPreferredValueClass() {
		return ImgRefValue.class;
	}

	private String m_imgRef;

	private long[] m_originalDims;

	private String m_sourceID;

	private BufferedImage m_thumb;

	/**
	 * Creates a new reference to the image whereas the target is encoded in the
	 * string.
	 * 
	 * @param sourceID
	 * @param imgRef
	 * 
	 */
	public ImgRefCell(final String sourceID, final String imgRef) {
		this(sourceID, imgRef, false);

	}

	/**
	 * 
	 * Creates a new reference to the image whereas the target is encoded in the
	 * string.
	 * 
	 * @param sourceID
	 * @param imgRef
	 * @param generateThumbnail
	 *            if true a thumbnail will be generated on the creation of the
	 *            ImgRefCell, hence, the according source must be available
	 */
	public ImgRefCell(final String sourceID, final String imgRef,
			final boolean generateThumbnail) {
		m_sourceID = sourceID;
		m_imgRef = imgRef;
		if (generateThumbnail) {
			getThumbnail(null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean equalsDataCell(final DataCell dc) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long[] getDimensions() {
		if (m_originalDims != null) {
			return m_originalDims;
		}
		try {
			long[] dim = (long[]) ObjectCache.getCachedObject(m_sourceID,
					m_imgRef + DIM_SUFFIX);
			if (dim == null) {
				dim = ImgSourcePool.getImgSource(m_sourceID).getDimensions(
						m_imgRef, 0);
				ObjectCache.addObject(m_sourceID, m_imgRef + DIM_SUFFIX, dim);
			}
			return dim;
		} catch (final Exception e) {
			noAccessWarning(e);
			return new long[] { 100, 100 };
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getImageReference() {
		return m_imgRef;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	private Img<T> getImg() {
		final long[] dims = getDimensions();
		try {
			Img<T> img = (Img<T>) ObjectCache.getCachedObject(m_sourceID,
					m_imgRef + IMG_SUFFIX);
			if (img == null) {
				img = (Img<T>) ImgSourcePool.getImgSource(m_sourceID).getImg(
						m_imgRef, 0);
				ObjectCache.addObject(m_sourceID, m_imgRef + IMG_SUFFIX, img);
			}
			return img;
		} catch (final Exception e) {
			noAccessWarning(e);
			return (Img<T>) new ArrayImgFactory<ByteType>().create(dims,
					new ByteType());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	private Img<T> getImgCopy() {
		try {
			return getImg().copy();
		} catch (final Exception e) {
			noAccessWarning(e);
			final long[] dims = getDimensions();
			return (Img<T>) new ArrayImgFactory<ByteType>().create(dims,
					new ByteType());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ImgPlus<T> getImgPlus() {
		return new ImgPlus<T>(getImg(), getMetadata());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ImgPlus<T> getImgPlusCopy() {
		return new ImgPlus<T>(getImgCopy(), getMetadata());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ImgPlusMetadata getMetadata() {
		// default metadata

		double[] tmpCal;
		try {
			tmpCal = (double[]) ObjectCache.getCachedObject(m_sourceID,
					m_imgRef + CALIBRATION_SUFFIX);
			if (tmpCal == null) {
				tmpCal = ImgSourcePool.getImgSource(m_sourceID).getCalibration(
						m_imgRef, 0);
				ObjectCache.addObject(m_sourceID,
						m_imgRef + CALIBRATION_SUFFIX, tmpCal);
			}
		} catch (final Exception e) {
			noAccessWarning(e);
			tmpCal = new double[getDimensions().length];
			for (int i = 0; i < tmpCal.length; i++) {
				tmpCal[i] = 1;
			}
		}

		CalibratedAxis[] tmpAxes;
		try {
			tmpAxes = (CalibratedAxis[]) ObjectCache.getCachedObject(
					m_sourceID, m_imgRef + AXES_SUFFIX);
			if (tmpAxes == null) {
				tmpAxes = ImgSourcePool.getImgSource(m_sourceID).getAxes(
						m_imgRef, 0);
				ObjectCache.addObject(m_sourceID, m_imgRef + AXES_SUFFIX,
						tmpAxes);
			}
		} catch (final Exception e) {
			noAccessWarning(e);
			tmpAxes = new CalibratedAxis[getDimensions().length];
			for (int i = 0; i < tmpAxes.length; i++) {
				tmpAxes[i] = new DefaultCalibratedAxis(Axes.get("Unknown " + i));
			}
		}

		// setting everything to metadata
		final CalibratedAxis[] axes = new CalibratedAxis[tmpAxes.length];
		for (int i = 0; i < axes.length; i++) {
			axes[i] = tmpAxes[i];
		}

		final double[] calibration = tmpCal.clone();

		// TODO: Can be replaced by FinalMetadata?!
		return new ImgPlusMetadata() {

			@Override
			public void axes(final CalibratedAxis[] axes) {
				for (int i = 0; i < axes.length; i++) {
					axes[i] = axis(i);
				}
			}

			@Override
			public CalibratedAxis axis(final int d) {
				return axes[d];
			}

			@Override
			public void calibration(final double[] cal) {
				for (int d = 0; d < cal.length; d++) {
					cal[d] = calibration[d];
				}
			}

			@Override
			public void calibration(final float[] cal) {
				for (int d = 0; d < cal.length; d++) {
					cal[d] = (float) calibration[d];
				}
			}

			@Override
			public double calibration(final int d) {
				return calibration[d];
			}

			@Override
			public int dimensionIndex(final AxisType axisType) {
				for (int i = 0; i < axes.length; i++) {
					if (axisType.getLabel().equals(axes[i].type().getLabel())) {
						return i;
					}
				}
				return -1;
			}

			@Override
			public double getChannelMaximum(final int c) {
				return 0;
			}

			@Override
			public double getChannelMinimum(final int c) {
				return 0;
			}

			@Override
			public ColorTable getColorTable(final int no) {
				// Nothing to do here
				return null;
			}

			@Override
			public int getColorTableCount() {
				return 0;
			}

			@Override
			public int getCompositeChannelCount() {
				return 0;
			}

			@Override
			public String getName() {
				return m_imgRef;
			}

			@Override
			public String getSource() {
				return m_imgRef;
			}

			@Override
			public int getValidBits() {
				return 0;
			}

			@Override
			public void initializeColorTables(final int count) {
				// Nothing to do here
			}

			@Override
			public int numDimensions() {
				return axes.length;
			}

			@Override
			public void setAxis(final CalibratedAxis axis, final int d) {
				axes[d] = axis;
			}

			@Override
			public void setCalibration(final double cal, final int d) {
				calibration[d] = cal;
			}

			@Override
			public void setCalibration(final double[] cal) {
				for (int d = 0; d < cal.length; d++) {
					calibration[d] = (float) cal[d];
				}
			}

			@Override
			public void setCalibration(final float[] cal) {
				for (int d = 0; d < cal.length; d++) {
					calibration[d] = cal[d];
				}
			}

			@Override
			public void setChannelMaximum(final int c, final double max) {
				// Nothing to do here
			}

			@Override
			public void setChannelMinimum(final int c, final double min) {
				// Nothing to do here
			}

			@Override
			public void setColorTable(final ColorTable colorTable, final int no) {
				// Nothing to do here

			}

			@Override
			public void setCompositeChannelCount(final int count) {
				// Nothing to do here
			}

			@Override
			public void setName(final String name) {
				// Nothing to do here
			}

			@Override
			public void setSource(final String source) {
				// TODO: take only img name
				m_imgRef = source;
			}

			@Override
			public void setValidBits(final int bits) {
				// Nothing to do here
			}

			@Override
			public String unit(int d) {
				return axes[d].unit();
			}

			@Override
			public void setUnit(final String unit, final int d) {
				axes[d].setUnit(unit);
			}

		};

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long[] getMinimum() {
		return new long[getDimensions().length];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<T> getPixelType() {
		try {
			return (Class<T>) ImgSourcePool.getImgSource(m_sourceID)
					.getPixelType(m_imgRef, 0).getClass();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSource() {
		return m_sourceID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getStringValue() {
		final ImgSource fac = (ImgSource) ObjectCache.getCachedObject(
				m_sourceID, m_sourceID);
		String facDesc = "unknown source";
		if (fac != null) {
			try {
				facDesc = fac.getSource(m_sourceID);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}
		return m_imgRef + " (" + facDesc + ")";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Image getThumbnail(final RenderingHints renderingHints) {
		try {

			if (m_thumb != null) {
				return m_thumb;
			}
			m_thumb = ImgSourcePool.getImgSource(m_sourceID).getThumbnail(
					m_imgRef, KNIMEKNIPPlugin.getMaximumImageCellHeight());
			m_originalDims = getDimensions();
			return m_thumb;
		} catch (final Exception e) {
			noAccessWarning(e);
			return new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return m_imgRef.hashCode();
	}

	private void noAccessWarning(final Exception e) {
		LOGGER.warn("Can not access the referenced image object: " + e);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return m_imgRef;
	}
}
