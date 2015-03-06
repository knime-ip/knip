/*
 * #%L
 * SCIFIO library for reading and converting scientific file formats.
 * %%
 * Copyright (C) 2011 - 2014 Board of Regents of the University of
 * Wisconsin-Madison
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package org.knime.knip.io.formats;

import io.scif.AbstractChecker;
import io.scif.AbstractFormat;
import io.scif.AbstractMetadata;
import io.scif.AbstractParser;
import io.scif.BufferedImagePlane;
import io.scif.Format;
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.config.SCIFIOConfig;
import io.scif.formats.TIFFFormat;
import io.scif.gui.AWTImageTools;
import io.scif.gui.BufferedImageReader;
import io.scif.io.RandomAccessInputStream;
import io.scif.services.FormatService;
import io.scif.util.FormatTools;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;

import net.imagej.axis.Axes;

import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;

/**
 * TiffJAIReader is a file format reader for TIFF images. It uses the Java
 * Advanced Imaging library (javax.media.jai) to read the data. Much of this
 * code was adapted from <a href=
 * "http://java.sun.com/products/java-media/jai/forDevelopers/samples/MultiPageRead.java"
 * >this example</a>.
 */
@Plugin(type = Format.class, priority = Priority.VERY_HIGH_PRIORITY + 1)
public class TIFFJAIFormat extends AbstractFormat {

	@Parameter
	private FormatService formatService;

	// -- Format API methods --

	@Override
	public String getFormatName() {
		return "Tagged Image File Format";
	}

	// -- AbstractFormat Methods --

	@Override
	protected String[] makeSuffixArray() {
		return formatService.getFormatFromClass(TIFFFormat.class).getSuffixes();
	}

	// -- Nested classes --

	/**
	 * @author Mark Hiner
	 */
	public static class Metadata extends AbstractMetadata {

		// -- Fields --

		private ImageDecoder dec;

		private int numPages;

		// -- TIFFJAIMetadata getters and setters --

		public ImageDecoder getImageDecoder() {
			return dec;
		}

		public void setImageDecoder(final ImageDecoder dec) {
			this.dec = dec;
		}

		public int getNumPages() {
			return numPages;
		}

		public void setNumPages(final int numPages) {
			this.numPages = numPages;
		}

		// -- Metadata API Methods --

		@Override
		public void populateImageMetadata() {
			createImageMetadata(1);
			final ImageMetadata m = get(0);

			// decode first image plane
			BufferedImage img = null;
			try {
				img = openBufferedImage(this, 0);
			} catch (final FormatException e) {
				log().error("Invalid image stream", e);
				return;
			} catch (IOException e) {
				e.printStackTrace();
			}

			// m.setAxisLength(Axes.CHANNEL,
			// img.getSampleModel().getNumBands());
			m.setAxisLength(Axes.X, img.getWidth());
			m.setAxisLength(Axes.Y, img.getHeight());
			// m.setAxisLength(Axes.TIME, numPages);
			m.setPlanarAxisCount(2);

			m.setPixelType(AWTImageTools.getPixelType(img));
			m.setLittleEndian(false);
			m.setMetadataComplete(true);
			m.setIndexed(false);
			m.setFalseColor(false);
		}
	}

	/**
	 * @author Mark Hiner
	 */
	public static class Parser extends AbstractParser<Metadata> {

		// -- Parser API Methods --

		@Override
		protected void typedParse(final RandomAccessInputStream stream,
				final Metadata meta, final SCIFIOConfig config)
				throws IOException, FormatException {

			// create tiff decoder
			ImageDecoder dec = ImageCodec.createImageDecoder("tiff", stream,
					null);
			RenderedImage img = dec.decodeAsRenderedImage();

			int numPages = dec.getNumPages();

			if (numPages < 0) {
				throw new FormatException("Invalid page count: " + numPages);
			}

			meta.setImageDecoder(dec);
			meta.setNumPages(numPages);
		}
	}

	public static class Checker extends AbstractChecker {
		@Override
		public boolean suffixSufficient() {
			return true;
		}
	}

	/**
	 * @author Mark Hiner
	 */
	public static class Reader extends BufferedImageReader<Metadata> {

		// -- AbstractReader API Methods --

		@Override
		protected String[] createDomainArray() {
			return new String[] { FormatTools.GRAPHICS_DOMAIN };
		}

		// -- Reader API methods --

		@Override
		public BufferedImagePlane openPlane(final int imageIndex,
				final long planeIndex, final BufferedImagePlane plane,
				final long[] planeMin, final long[] planeMax,
				final SCIFIOConfig config) throws FormatException, IOException {
			FormatTools.checkPlaneForReading(getMetadata(), imageIndex,
					planeIndex, -1, planeMin, planeMax);
			final BufferedImage img = openBufferedImage(getMetadata(),
					planeIndex);
			plane.setData(AWTImageTools.getSubimage(img,
					getMetadata().get(imageIndex).isLittleEndian(), planeMin,
					planeMax));
			return plane;
		}
	}

	// -- Helper methods --

	/**
	 * Obtains a BufferedImage from the given data source using JAI.
	 * 
	 * @throws IOException
	 */
	protected static BufferedImage openBufferedImage(final Metadata meta,
			final long planeIndex) throws FormatException, IOException {

		RenderedImage img = meta.getImageDecoder().decodeAsRenderedImage(
				(int) planeIndex);
		return AWTImageTools.convertRenderedImage(img);
	}
}
