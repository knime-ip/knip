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

import io.scif.FormatException;
import io.scif.Metadata;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.filters.ChannelSeparator;
import io.scif.filters.ReaderFilter;
import io.scif.gui.AWTImageTools;
import io.scif.img.DimRange;
import io.scif.img.ImgOpener;
import io.scif.img.ImgOptions;
import io.scif.img.ImgUtilityService;
import io.scif.img.SubRegion;
import io.scif.ome.xml.meta.OMEMetadata;

import java.awt.image.BufferedImage;
import java.io.IOException;

import net.imglib2.Pair;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.TypedAxis;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.util.MiscViews;
import org.scijava.InstantiableException;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class ScifioImgSource implements ImgSource {

	/* ID of the source */
	private static final String SOURCE_ID = "Scifio Image Source";

	private Reader m_reader;

	private final ImgOpener m_imgOpener;

	private final ImgFactory m_imgFactory;

	private final boolean m_isGroupFiles;

	private ImgUtilityService m_imgUtilsService;

	private boolean m_checkFileFormat;

	/*
	 * helps do decide if the checkFileFormat option could have been set to
	 * false.
	 */
	private boolean m_usedDifferentReaders;

	public ScifioImgSource() {
		this(true);
	}

	public ScifioImgSource(boolean checkFileFormat) {
		this(new ArrayImgFactory(), checkFileFormat, true);
	}

	public ScifioImgSource(final ImgFactory imgFactory,
			boolean checkFileFormat, final boolean isGroupFiles) {
		m_isGroupFiles = isGroupFiles;
		m_checkFileFormat = checkFileFormat;
		m_imgOpener = new ImgOpener();
		m_imgFactory = imgFactory;
		m_usedDifferentReaders = false;
	}

	@Override
	public void close() {
		if (m_reader != null) {
			try {
				m_reader.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public String getSource(final String imgRef) throws Exception {
		return SOURCE_ID;
	}

	/**
	 * @param ref
	 * @return number of images contained in the specified file
	 * @throws Exception
	 */
	public int getSeriesCount(final String imgRef) throws Exception {
		return getReader(imgRef).getImageCount();
	}

	/**
	 * @param ref
	 * @return
	 * @throws Exception
	 */
	public String getOMEXMLMetadata(final String imgRef) throws Exception {
		Metadata meta = getReader(imgRef).getMetadata();
		OMEMetadata omexml = new OMEMetadata(ScifioGateway.getSCIFIO()
				.getContext());

		ScifioGateway.getSCIFIO().translator().translate(meta, omexml, true);
		String xml = omexml.getRoot().dumpXML();
		return xml;
	}

	@Override
	public ImgPlus<RealType> getImg(final String imgRef, final int currentSeries)
			throws Exception {
		return getImg(imgRef, currentSeries, null);
	}

	@Override
	public ImgPlus<RealType> getImg(final String imgRef,
			final int currentSeries,
			final Pair<TypedAxis, long[]>[] axisSelectionConstraints)
			throws Exception {
		ImgOptions options = new ImgOptions();
		options.setComputeMinMax(false);

		boolean withCropping = false;

		if (axisSelectionConstraints != null
				&& axisSelectionConstraints.length > 0) {

			withCropping = true;
			// WRONG WRONG WRONG only 5d support
			DimRange[] ranges = new DimRange[axisSelectionConstraints.length];
			AxisType[] axes = new AxisType[axisSelectionConstraints.length];
			for (int i = 0; i < ranges.length; i++) {
				ranges[i] = new DimRange(axisSelectionConstraints[i].getB());
				axes[i] = axisSelectionConstraints[i].getA().type();
			}

			options.setRegion(new SubRegion(axes, ranges));
		}

		// TODO remove calibration hack as soon as the returned imgplus has
		// calibration values
		ImgPlus<RealType> ret = m_imgOpener.openImg(getReader(imgRef),
				getPixelType(imgRef, currentSeries), m_imgFactory, options);
		double[] calib = getCalibration(imgRef, currentSeries);
		for (int d = 0; d < ret.numDimensions(); d++) {
			ret.setCalibration(calib[d], d);
		}

		if (withCropping) {
			ret = MiscViews.cleanImgPlus(ret);
		}

		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BufferedImage getThumbnail(final String imgRef, final int planeNo)
			throws Exception {
		Reader r = getReader(imgRef);
		int sizeX = r.getMetadata().getThumbSizeX(0);
		int sizeY = r.getMetadata().getThumbSizeY(0);

		// image index / plane index
		Plane pl = r.openThumbPlane(0, 0);

		return AWTImageTools.makeImage(pl.getBytes(), sizeX, sizeY, NativeTypes
				.getPixelType(getPixelType(imgRef, 0)).isSigned());
	}

	// META DATA

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CalibratedAxis[] getAxes(final String imgRef, final int currentSeries)
			throws Exception {
		return getReader(imgRef).getMetadata().getAxes(currentSeries);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double[] getCalibration(final String imgRef, final int currentSeries)
			throws Exception {

		Metadata meta = ScifioGateway.getSCIFIO().initializer()
				.parseMetadata(imgRef, true);

		// translate to ome metadata to get access to calibration values
		OMEMetadata omexml = new OMEMetadata(ScifioGateway.getSCIFIO()
				.getContext());
		ScifioGateway.getSCIFIO().translator().translate(meta, omexml, false);

		double[] calib = new double[getDimensions(imgRef, currentSeries).length];

		int i = 0;
		for (CalibratedAxis axes : omexml.getAxes(currentSeries)) {
			calib[i] = axes.calibration();
			i++;
		}

		return calib;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long[] getDimensions(final String imgRef, final int currentSeries)
			throws Exception {
		int[] tmp = getReader(imgRef).getMetadata().getAxesLengths(
				currentSeries);
		long[] ret = new long[tmp.length];

		for (int i = 0; i < tmp.length; i++) {
			ret[i] = tmp[i];
		}

		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName(final String imgRef) throws Exception {
		return getReader(imgRef).getMetadata().getDatasetName();
	}

	public boolean usedDifferentReaders() {
		return m_usedDifferentReaders;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws IOException
	 * @throws FormatException
	 */
	@Override
	public RealType getPixelType(final String imgRef, final int currentSeries)
			throws FormatException, IOException {

		if (m_imgUtilsService == null) {
			m_imgUtilsService = ScifioGateway.getSCIFIO().getContext()
					.getService(ImgUtilityService.class);
		}

		RealType type = m_imgUtilsService.makeType(getReader(imgRef)
				.getMetadata().getPixelType(currentSeries));
		return type;
	}

	private Reader getReader(final String imgRef) throws FormatException,
			IOException {
		if (m_reader == null
				|| (!m_reader.getCurrentFile().equals(imgRef) && m_checkFileFormat)) {
			ReaderFilter r = ScifioGateway.getSCIFIO().initializer()
					.initializeReader(imgRef, true);
			try {
				r.enable(ChannelSeparator.class);
			} catch (InstantiableException e) {
				throw new FormatException(e);
			}
			r.setGroupFiles(m_isGroupFiles);

			if (m_reader != null
					&& !(m_reader.getFormat().getClass().equals(r.getFormat()
							.getClass()))) {
				// more than one reader (class) has been used
				m_usedDifferentReaders = true;
			}
			m_reader = r;
		}

		if (!m_checkFileFormat) {
			m_reader.setSource(imgRef);
		}

		return m_reader;
	}

}
