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
package org.knime.knip.core.awt;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.geom.Rectangle2D;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.knip.core.util.MiscViews;

/**
 * 
 * Methods to convert data (e.g. an imglib image or a histogram) to a java.awt.BufferedImage
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public final class AWTImageTools {

    private AWTImageTools() {
        // to hide the constructor of this utility class
    }

    /** Creates an image with the given DataBuffer. */
    public static BufferedImage
            constructImage(final int c, final int type, final int w, final int h, final boolean interleaved,
                           final boolean banded, final DataBuffer buffer) {
        if (c > 4) {
            throw new IllegalArgumentException("Cannot construct image with " + c + " channels");
        }

        SampleModel model;
        if ((c > 2) && (type == DataBuffer.TYPE_INT) && (buffer.getNumBanks() == 1)) {
            final int[] bitMasks = new int[c];
            for (int i = 0; i < c; i++) {
                bitMasks[i] = 0xff << ((c - i - 1) * 8);
            }
            model = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, w, h, bitMasks);
        } else if (banded) {
            model = new BandedSampleModel(type, w, h, c);
        } else if (interleaved) {
            final int[] bandOffsets = new int[c];
            for (int i = 0; i < c; i++) {
                bandOffsets[i] = i;
            }
            model = new PixelInterleavedSampleModel(type, w, h, c, c * w, bandOffsets);
        } else {
            final int[] bandOffsets = new int[c];
            for (int i = 0; i < c; i++) {
                bandOffsets[i] = i * w * h;
            }
            model = new ComponentSampleModel(type, w, h, 1, w, bandOffsets);
        }

        final WritableRaster raster = Raster.createWritableRaster(model, buffer, null);

        BufferedImage b = null;

        if ((c == 1) && (type == DataBuffer.TYPE_BYTE)) {
            b = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

            b.setData(raster);
        } else if ((c == 1) && (type == DataBuffer.TYPE_USHORT)) {
            b = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
            b.setData(raster);
        } else if ((c > 2) && (type == DataBuffer.TYPE_INT) && (buffer.getNumBanks() == 1)) {

            final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            final GraphicsDevice device = env.getDefaultScreenDevice();
            final GraphicsConfiguration config = device.getDefaultConfiguration();
            b = config.createCompatibleImage(w, h);
            b.setData(raster);
        }

        return b;
    }

    /**
     * Draws the histogram of the {@link ImagePlane} onto a {@link BufferedImage}.
     * 
     * @param ip
     * @param width the width of the image containing the histogram
     * @param height the height of the image containing the histogram
     * @return a java-image with the histogram painted on it
     */
    public static BufferedImage drawHistogram(final long[] hist, final int height) {

        long max = 0;
        for (int i = 0; i < hist.length; i++) {
            max = Math.max(max, hist[i]);
        }
        return drawHistogram(hist, hist.length, height, max, true);
    }

    /**
     * Draws the histogram of the {@link ImagePlane} onto a {@link BufferedImage}.
     * 
     * @param ip
     * @param width the width of the image containing the histogram
     * @param height the height of the image containing the histogram
     * @return a java-image with the histogram painted on it
     */
    public static BufferedImage drawHistogram(final long[] hist, final int width, final int height, final long max,
                                              final boolean log) {
        final int margin = 20;

        final BufferedImage histImg = new BufferedImage(width, height + margin, BufferedImage.TYPE_BYTE_GRAY);
        final Graphics g = histImg.getGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, width, height + margin);
        g.setColor(Color.black);
        final int binWidth = width / hist.length;
        final double heightScale = (double)height / max;
        final double heightScaleLog = height / Math.log(max);
        for (int i = 0; i < hist.length; i++) {

            if (log) {
                g.setColor(Color.GRAY);
                if (hist[i] > 0) {
                    g.fillRect(i * binWidth, (int)Math.round(height - (Math.log(hist[i]) * heightScaleLog)), binWidth,
                               height);
                }
            }

            g.setColor(Color.BLACK);
            g.fillRect(i * binWidth, (int)Math.round(height - (hist[i] * heightScale)), binWidth, height);

            if (hist.length <= 256) {
                g.setColor(new Color(i, i, i));
                g.drawLine(i, height + 5, i, (height + margin) - 1);
            }
        }

        return histImg;
    }

    // ---------------- Some other utility methods ------------------//

    /**
     * Creates a blank image with the given message painted on top (e.g., a loading or error message), matching the
     * given size.
     */
    public static BufferedImage makeTextBufferedImage(final String text, final int width, final int height,
                                                      final String lineDelimiter) {

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final String[] tokens = text.split(lineDelimiter);
        final Graphics2D g = image.createGraphics();
        g.fillRect(0, 0, width, height);
        final Rectangle2D.Float r = (Rectangle2D.Float)g.getFont().getStringBounds(tokens[0], g.getFontRenderContext());
        g.setColor(Color.black);
        for (int i = 0; i < tokens.length; i++) {
            g.drawString(tokens[i], 5, (i + 1) * r.height);
        }

        g.dispose();
        return image;
    }

    /**
     * Adds a subtitle to the given image.
     * 
     * @param source
     * @param txt
     * @return
     */
    public static BufferedImage makeSubtitledBufferedImage(final BufferedImage source, final String txt) {

        final Graphics g = source.getGraphics();
        final int w = SwingUtilities.computeStringWidth(g.getFontMetrics(), txt) + 5;
        g.setColor(Color.WHITE);
        g.fillRect(source.getWidth() - w, source.getHeight() - 16, source.getWidth(), source.getHeight());
        g.setColor(Color.black);
        g.drawString(txt, (source.getWidth() - w) + 4, source.getHeight() - 5);
        g.dispose();

        return source;
    }

    /**
     * Shows the first image plane in the dimension 0,1 in a JFrame.
     * 
     * @param ip the image plane
     * @param title a title for the frame
     */
    public static <T extends RealType<T>> JFrame showInFrame(final Img<T> img, final String title) {
        return showInFrame(img, title, 1);
    }

    /**
     * Shows first image plane in the dimension 0,1, scaled with the specified factor in a JFrame.
     * 
     * @param ip the image plane
     * @param factor the scaling factor
     */
    public static <T extends RealType<T>> JFrame showInFrame(final Img<T> img, final String title, final double factor) {
        return showInFrame(img, 0, 1, new long[img.numDimensions()], title, factor);
    }

    /**
     * Shows the selected ImagePlane in a JFrame.
     * 
     * @param img the image plane
     * @param factor the scaling factor
     */
    public static <T extends RealType<T>, I extends Img<T>> JFrame showInFrame(final I img, final int dimX,
                                                                               final int dimY, final long[] pos,
                                                                               String title, final double factor) {

        final int w = (int)img.dimension(dimX);
        final int h = (int)img.dimension(dimY);
        title = title + " (" + w + "x" + h + ")";

        final JLabel label = new JLabel();

        final JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setSize(w, h);
        frame.getContentPane().add(label);

        final Real2GreyRenderer<T> renderer = new Real2GreyRenderer<T>();

        final java.awt.Image awtImage = renderer.render(img, dimX, dimY, pos).image();
        label.setIcon(new ImageIcon(awtImage.getScaledInstance((int)Math.round(w * factor),
                                                               (int)Math.round(h * factor),
                                                               java.awt.Image.SCALE_DEFAULT)));

        frame.pack();
        frame.setVisible(true);
        return frame;
    }

    /**
     * Shows an AWT-image in a JFrame.
     * 
     * @param img the image to show.
     */

    public static void showInFrame(final java.awt.Image img) {
        showInFrame(img, "", 1);

    }

    public static void showInFrame(final java.awt.Image img, String title, final double factor) {

        final int w = img.getWidth(null);
        final int h = img.getHeight(null);
        title = title + " (" + w + "x" + h + ")";

        final JLabel label = new JLabel();

        final JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setSize(w, h);
        frame.getContentPane().add(label);

        label.setIcon(new ImageIcon(img.getScaledInstance((int)Math.round(w * factor), (int)Math.round(h * factor),
                                                          java.awt.Image.SCALE_DEFAULT)));

        frame.pack();
        frame.setVisible(true);

    }

    public static <T extends Type<T>> BufferedImage renderScaledStandardColorImg(RandomAccessibleInterval<T> img,
                                                                                 final ImageRenderer<T> renderer,
                                                                                 final double factor,
                                                                                 final long[] startPos) {

        int width;
        int height;

        FinalInterval interval;
        AffineGet transform;
        if (img.numDimensions() == 1) {
            width = Math.max(1, (int)(img.dimension(0) * factor));
            height = 1;

            transform = new AffineTransform2D();
            ((AffineTransform2D)transform).scale(factor);
            interval = new FinalInterval(new long[]{width, height});
            img = MiscViews.synchronizeDimensionality(img, interval);
        } else if (img.numDimensions() == 2) {
            width = Math.max(1, (int)(img.dimension(0) * factor));
            height = Math.max(1, (int)(img.dimension(1) * factor));
            transform = new AffineTransform2D();
            ((AffineTransform2D)transform).scale(factor);
            interval = new FinalInterval(new long[]{width, height});
        } else if (img.numDimensions() == 3) {
            width = Math.max(1, (int)(img.dimension(0) * factor));
            height = Math.max(1, (int)(img.dimension(1) * factor));
            transform = new AffineTransform3D();
            ((AffineTransform3D)transform).set(factor, 0.0, 0.0, 0.0, 0.0, factor, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);

            interval = new FinalInterval(new long[]{width, height, img.dimension(2)});

        } else {
            throw new IllegalArgumentException("Images with more than 3 dimensions are not supported!");
        }

        return AWTImageTools
                .makeBuffered(renderer
                        .render(Views.interval(RealViews.constantAffine(Views.interpolate(Views.extend(img,
                                                                                                       new OutOfBoundsBorderFactory<T, RandomAccessibleInterval<T>>()),
                                                                                          new NearestNeighborInterpolatorFactory<T>()),
                                                                        transform), interval), 0, 1, startPos).image());

    }

    //    code inside the block is from loci.formats.gui.AWTImageTools
    //    code is copied to avoid a dependency on the bioformats jars
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /** ImageObserver for working with AWT images. */
    protected static final Component OBS = new Container();

    //    code copied from loci.formats.gui.AWTImageTools
    /**
     * Creates a buffered image from the given AWT image object. If the AWT image is already a buffered image, no new
     * object is created.
     */
    public static BufferedImage makeBuffered(final Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage)image;
        }

        // TODO: better way to handle color model (don't just assume RGB)
        loadImage(image);
        BufferedImage img = new BufferedImage(image.getWidth(OBS), image.getHeight(OBS), BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.drawImage(image, 0, 0, OBS);
        g.dispose();
        return img;
    }

    //    code copied from loci.formats.gui.AWTImageTools
    /**
     * Creates a buffered image possessing the given color model, from the specified AWT image object. If the AWT image
     * is already a buffered image with the given color model, no new object is created.
     */
    public static BufferedImage makeBuffered(final Image image, final ColorModel cm) {
        if (cm == null) {
            return makeBuffered(image);
        }

        if (image instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage)image;
            if (cm.equals(bi.getColorModel())) {
                return bi;
            }
        }
        loadImage(image);
        int w = image.getWidth(OBS), h = image.getHeight(OBS);
        boolean alphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = cm.createCompatibleWritableRaster(w, h);
        BufferedImage result = new BufferedImage(cm, raster, alphaPremultiplied, null);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, OBS);
        g.dispose();
        return result;
    }

    //    code copied from loci.formats.gui.AWTImageTools
    /** Ensures the given AWT image is fully loaded. */
    public static boolean loadImage(final Image image) {
        if (image instanceof BufferedImage) {
            return true;
        }
        MediaTracker tracker = new MediaTracker(OBS);
        tracker.addImage(image, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException exc) {
            return false;
        }
        if (MediaTracker.COMPLETE != tracker.statusID(0, false)) {
            return false;
        }
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
}
