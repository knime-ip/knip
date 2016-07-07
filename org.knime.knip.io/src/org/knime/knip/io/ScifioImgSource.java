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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.knime.core.node.NodeLogger;
import org.knime.knip.base.exceptions.KNIPRuntimeException;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.util.MiscViews;
import org.scijava.Context;

import io.scif.Format;
import io.scif.FormatException;
import io.scif.Metadata;
import io.scif.Parser;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.config.SCIFIOConfig;
import io.scif.filters.ChannelFiller;
import io.scif.filters.PlaneSeparator;
import io.scif.filters.ReaderFilter;
import io.scif.gui.AWTImageTools;
import io.scif.img.IO;
import io.scif.img.ImageRegion;
import io.scif.img.ImgOpener;
import io.scif.img.ImgUtilityService;
import io.scif.img.Range;
import io.scif.img.SCIFIOImgPlus;
import io.scif.ome.OMEMetadata;
import io.scif.refs.RefManagerService;
import loci.formats.FormatHandler;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.TypedAxis;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import ome.xml.model.OMEModelImpl;
import ome.xml.model.enums.handlers.DetectorTypeEnumHandler;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class ScifioImgSource implements ImgSource {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(ScifioImgSource.class);

	/* ID of the source */
	private static final String SOURCE_ID = "Scifio Image Source";

	private UnclosableReaderFilter m_reader;

	/* The currently used file by the reader */
	private String m_currentFile;

	private final ImgOpener m_imgOpener;

	@SuppressWarnings("rawtypes")
	private final ImgFactory m_imgFactory;

	private ImgUtilityService m_imgUtilsService;

	private boolean m_checkFileFormat;

	private SCIFIOConfig m_scifioConfig;

	/*
	 * helps do decide if the checkFileFormat option could have been set to
	 * false.
	 */
	private boolean m_usedDifferentReaders;

	public ScifioImgSource() {
		this(true);
	}

	@SuppressWarnings("rawtypes")
	public ScifioImgSource(final boolean checkFileFormat) {
		this(new ArrayImgFactory(), checkFileFormat, true);
	}

	/**
	 * Creates a new ScifioImgSource.
	 * 
	 * @param imgFactory
	 *            the image factory to be used
	 * @param checkFileFormat
	 *            if for each new file to be read a new reader should be created
	 *            or the old one can be re-used
	 * @param isGroupFiles
	 *            if a file points to a collection of files
	 * 
	 */
	public ScifioImgSource(@SuppressWarnings("rawtypes") final ImgFactory imgFactory, final boolean checkFileFormat,
			final boolean isGroupFiles) {
		this(imgFactory, checkFileFormat, new SCIFIOConfig().groupableSetGroupFiles(isGroupFiles));
	}

	/**
	 * Creates a new ScifioImgSource.
	 * 
	 * @param imgFactory
	 *            the image factory to be used
	 * @param checkFileFormat
	 *            if for each new file to be read a new reader should be created
	 *            or the old one can be re-used
	 * @param config
	 *            additional scifio-specific settings
	 * 
	 * 
	 */
	public ScifioImgSource(@SuppressWarnings("rawtypes") final ImgFactory imgFactory, final boolean checkFileFormat,
			final SCIFIOConfig config) {
		m_scifioConfig = config;
		m_checkFileFormat = checkFileFormat;
		m_imgOpener = new ImgOpener(ScifioGateway.getSCIFIO().getContext());
		m_imgFactory = imgFactory;
		m_usedDifferentReaders = false;

		// TODO Workaround to suppress BioFormats info message flooding
		org.apache.log4j.Logger.getLogger(OMEModelImpl.class).setLevel(Level.ERROR);
		org.apache.log4j.Logger.getLogger(FormatHandler.class).setLevel(Level.ERROR);
		org.apache.log4j.Logger.getLogger(DetectorTypeEnumHandler.class).setLevel(Level.ERROR);
	}

	@Override
	public void close() {
		if (m_reader != null) {
			try {
				m_reader.closeNow();
			} catch (final IOException e) {
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
	 * @return omexml metadata string
	 * @throws Exception
	 */
	public String getOMEXMLMetadata(final String imgRef) throws Exception {
		final Metadata meta = getReader(imgRef).getMetadata();
		final OMEMetadata omexml = new OMEMetadata(ScifioGateway.getSCIFIO().getContext());
		try {
			ScifioGateway.getSCIFIO().translator().translate(meta, omexml, false);
		} catch (Exception e) {
			throw new IOException("Could not read the OMEXML-metadata! : " + e);
		}
		final String xml = omexml.getRoot().dumpXML();
		return xml;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public ImgPlus<RealType> getImg(final String imgRef, final int currentSeries) throws Exception {
		return getImg(imgRef, currentSeries, null);
	}

	// TODO: Use new SCIFIO API
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ImgPlus<RealType> getImg(final String imgRef, final int currentSeries,
			final Pair<TypedAxis, long[]>[] axisSelectionConstraints) throws Exception {

		final Level oldLevel = Logger.getRootLogger().getLevel();

		// FIXME: WORKAROUND till bug 3391 is fixed (support for
		// SCIFIOImgPlus)
		if (m_imgFactory instanceof CellImgFactory) {

			LOGGER.warn(
					"Cell images are not fully supported, yet. Images are read but potential subset selection is ignored.");

			// copy scifio img plus to an ordinary cell img

			final SCIFIOImgPlus<RealType> scifio = (SCIFIOImgPlus<RealType>) IO.open(imgRef);
			final Img<RealType> tmp = new CellImgFactory().create(scifio, scifio.firstElement().createVariable());
			final Cursor<RealType> inCur = scifio.cursor();
			final RandomAccess<RealType> outRA = tmp.randomAccess();
			while (inCur.hasNext()) {
				inCur.fwd();
				outRA.setPosition(inCur);
				outRA.get().set(inCur.get());
			}
			final ImgPlus<RealType> res = new ImgPlus<RealType>(tmp, scifio);
			// register
			final Context ctx = m_imgOpener.getContext();
			final RefManagerService refManagerService = ctx.getService(RefManagerService.class);
			refManagerService.manage(res, ctx);
			return res;
		}

		final SCIFIOConfig options = new SCIFIOConfig();
		options.imgOpenerSetComputeMinMax(false);
		options.imgOpenerSetIndex(currentSeries);
		// boolean withCropping = false;

		if (axisSelectionConstraints != null && axisSelectionConstraints.length > 0) {

			// withCropping = true;
			// TODO: Still WRONG WRONG WRONG only 5d support?
			final Range[] ranges = new Range[axisSelectionConstraints.length];
			final AxisType[] axes = new AxisType[axisSelectionConstraints.length];
			for (int i = 0; i < ranges.length; i++) {
				ranges[i] = new Range(axisSelectionConstraints[i].getB());
				axes[i] = axisSelectionConstraints[i].getA().type();
			}

			options.imgOpenerSetRegion(new ImageRegion(axes, ranges));
		}

		final ImgPlus<RealType> ret = MiscViews.cleanImgPlus(
				m_imgOpener.openImg(getReader(imgRef), getPixelType(imgRef, currentSeries), m_imgFactory, options));

		Logger.getRootLogger().setLevel(oldLevel);
		return ret;
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> ImgPlus<T> getTypedImg(final String imgRef, final int currentSeries,
			T type) throws Exception {
		return getTypedImg(imgRef, currentSeries, null, type);
	}

	// TODO: Use new SCIFIO API
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <T extends RealType<T> & NativeType<T>> ImgPlus<T> getTypedImg(final String imgRef, final int currentSeries,
			final Pair<TypedAxis, long[]>[] axisSelectionConstraints, T type) throws Exception {

		final Level oldLevel = Logger.getRootLogger().getLevel();

		// FIXME: WORKAROUND till bug 3391 is fixed (support for
		// SCIFIOImgPlus)
		if (m_imgFactory instanceof CellImgFactory) {

			LOGGER.warn(
					"Cell images are not fully supported, yet. Images are read but potential subset selection is ignored.");

			// copy scifio img plus to an ordinary cell img

			final SCIFIOImgPlus<T> scifio = (SCIFIOImgPlus<T>) IO.openImg(imgRef, type);
			final Img<T> tmp = new CellImgFactory().create(scifio, type);
			final Cursor<T> inCur = scifio.cursor();
			final RandomAccess<T> outRA = tmp.randomAccess();
			while (inCur.hasNext()) {
				inCur.fwd();
				outRA.setPosition(inCur);
				outRA.get().set(inCur.get());
			}
			final ImgPlus<T> res = new ImgPlus<T>(tmp, scifio);
			// register
			final Context ctx = m_imgOpener.getContext();
			final RefManagerService refManagerService = ctx.getService(RefManagerService.class);
			refManagerService.manage(res, ctx);
			return res;
		}

		final SCIFIOConfig options = new SCIFIOConfig();
		options.imgOpenerSetComputeMinMax(false);
		options.imgOpenerSetIndex(currentSeries);
		// boolean withCropping = false;

		if (axisSelectionConstraints != null && axisSelectionConstraints.length > 0) {

			// withCropping = true;
			// TODO: Still WRONG WRONG WRONG only 5d support?
			final Range[] ranges = new Range[axisSelectionConstraints.length];
			final AxisType[] axes = new AxisType[axisSelectionConstraints.length];
			for (int i = 0; i < ranges.length; i++) {
				ranges[i] = new Range(axisSelectionConstraints[i].getB());
				axes[i] = axisSelectionConstraints[i].getA().type();
			}

			options.imgOpenerSetRegion(new ImageRegion(axes, ranges));
		}

		final ImgPlus<T> ret = MiscViews
				.cleanImgPlus(m_imgOpener.openImg(getReader(imgRef), type, m_imgFactory, options));

		Logger.getRootLogger().setLevel(oldLevel);
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BufferedImage getThumbnail(final String imgRef, final int planeNo) throws Exception {
		final Reader r = getReader(imgRef);
		final int sizeX = (int) r.getMetadata().get(0).getThumbSizeX();
		final int sizeY = (int) r.getMetadata().get(0).getThumbSizeY();

		// image index / plane index
		final Plane pl = r.openThumbPlane(0, 0);

		return AWTImageTools.makeImage(pl.getBytes(), sizeX, sizeY,
				NativeTypes.getPixelType(getPixelType(imgRef, 0)).isSigned());
	}

	// META DATA

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<CalibratedAxis> getAxes(final String imgRef, final int currentSeries) throws Exception {
		return getReader(imgRef).getMetadata().get(currentSeries).getAxes();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long[] getDimensions(final String imgRef, final int currentSeries) throws Exception {
		final long[] tmp = getReader(imgRef).getMetadata().get(currentSeries).getAxesLengths();
		final long[] ret = new long[tmp.length];

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
	@SuppressWarnings("rawtypes")
	@Override
	public RealType getPixelType(final String imgRef, final int currentSeries) throws IOException, FormatException {

		if (m_imgUtilsService == null) {
			m_imgUtilsService = ScifioGateway.getSCIFIO().getContext().getService(ImgUtilityService.class);
		}

		final RealType type = m_imgUtilsService
				.makeType(getReader(imgRef).getMetadata().get(currentSeries).getPixelType());
		return type;
	}

	// -- private helper methods --

	private UnclosableReaderFilter getReader(final String imgRef) throws FormatException, IOException {
		if (m_reader == null || (!m_currentFile.equals(imgRef) && m_checkFileFormat)) {

			final Format format = ScifioGateway.getSCIFIO().format().getFormat(imgRef,
					new SCIFIOConfig().checkerSetOpen(true));

			final UnclosableReaderFilter r = new UnclosableReaderFilter(format.createReader());

			final Parser p = format.createParser();

			r.setMetadata(p.parse(imgRef, m_scifioConfig));

			// check if the current file really contains images
			if (r.getMetadata().getImageCount() == 0) {
				throw new KNIPRuntimeException("No images available in file " + imgRef);
			}

			// without the "separate"-stuff the images will not be split
			// correctly for some types. This fixes the bug if, for instance,
			// only Channel 1 is desired and Channel 0 was returned every time.
			r.enable(ChannelFiller.class);
			r.enable(PlaneSeparator.class).separate(axesToSplit(r));

			if (m_reader != null && !(m_reader.getFormat().getClass().equals(r.getFormat().getClass()))) {
				// more than one reader (class) has been used
				m_usedDifferentReaders = true;
			}
			if (m_reader != null) {
				m_reader.closeNow();
			}
			m_reader = r;
		} else if (!m_currentFile.equals(imgRef) && !m_checkFileFormat) {
			// re-use the last reader, set the new image reference (i.e. id) and
			// parse the metadata
			m_reader.closeNow();

			final Parser p = m_reader.getFormat().createParser();

			m_reader.setMetadata(p.parse(imgRef, m_scifioConfig));

			// TODO maybe this is a bug and we shouldn't have to do this.
			m_reader.enable(ChannelFiller.class);
			m_reader.enable(PlaneSeparator.class).separate(axesToSplit(m_reader));
		}

		// sets the file the reader currently points to
		m_currentFile = imgRef;
		return m_reader;
	}

	/*
	 * Returns a list of all AxisTypes that should be split out. This is a list
	 * of all non-X,Y planar axes. Always tries to split {@link Axes#CHANNEL}.
	 * 
	 * Code taken from ImgOpener!
	 */
	private AxisType[] axesToSplit(final ReaderFilter r) {
		final Set<AxisType> axes = new HashSet<AxisType>();
		final Metadata meta = r.getTail().getMetadata();
		// Split any non-X,Y axis
		for (final CalibratedAxis t : meta.get(0).getAxesPlanar()) {
			final AxisType type = t.type();
			if (!(type == Axes.X || type == Axes.Y)) {
				axes.add(type);
			}
		}
		// Ensure channel is attempted to be split
		axes.add(Axes.CHANNEL);
		return axes.toArray(new AxisType[axes.size()]);
	}

	/* Helper class to prevent a reader from being closed. */
	private class UnclosableReaderFilter extends ReaderFilter {

		public UnclosableReaderFilter(final Reader r) {
			super(r);
		}

		@Override
		public void close() throws IOException {
			// do nothing here to prevent the reader from being closed
		}

		@Override
		public void close(final boolean fileOnly) throws IOException {
			// do nothing here to prevent the reader from being closed
		}

		/*
		 * Closes the underlying reader, called by the
		 * ScifioImgsource.close()-method.
		 */
		private void closeNow() throws IOException {
			super.close(false);
		}

	}

}
