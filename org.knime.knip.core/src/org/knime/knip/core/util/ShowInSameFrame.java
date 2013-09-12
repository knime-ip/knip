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
package org.knime.knip.core.util;

import java.awt.Toolkit;
import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

import net.imglib2.img.Img;
import net.imglib2.sampler.special.OrthoSliceCursor;
import net.imglib2.type.numeric.RealType;

/**
 * 
 * Helper class for debugging purposes allowing to show an image in one frame. Every time the image is updated, the
 * image is rendered newly.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ShowInSameFrame {

    private ImagePlaneProducer m_planeProd = null;

    /**
     * Shows the images (the first plane of dimensions 0 and 1) in the same frame. Only the first call of this method
     * creates a new window, subsequent calls will show the images in the created window. The first call also defines
     * the scaling factor and will be ignored in further calls.
     * 
     * @param img
     * @param scaleFactor
     */
    public <T extends RealType<T>> void show(final Img<T> img, final double scaleFactor) {
        if (m_planeProd != null) {
            if ((img.dimension(0) == m_planeProd.getImage().dimension(0))
                    && (img.dimension(1) == m_planeProd.getImage().dimension(1))) {
                m_planeProd.updateConsumers(img);
                return;
            }

            m_planeProd = null;
        }
        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        final JLabel label = new JLabel();
        frame.getContentPane().add(label);
        m_planeProd = new ImagePlaneProducer(img);
        final String title = " (" + img.dimension(0) + "x" + img.dimension(1) + ")";
        frame.setTitle(title);
        final java.awt.Image awtImage = Toolkit.getDefaultToolkit().createImage(m_planeProd);
        label.setIcon(new ImageIcon(awtImage.getScaledInstance((int)Math.round(img.dimension(0) * scaleFactor),
                                                               (int)Math.round(img.dimension(1) * scaleFactor),
                                                               java.awt.Image.SCALE_DEFAULT)));
        frame.setSize((int)img.dimension(0), (int)img.dimension(1));
        frame.pack();
        frame.setVisible(true);
    }

    public static class ImagePlaneProducer<T extends RealType<T>> implements ImageProducer {

        private final Set<ImageConsumer> m_consumers = new HashSet<ImageConsumer>();

        private Img<T> m_img;

        private final ColorModel m_cmodel;

        private byte[] m_plane;

        /**
         * @param img
         */
        public ImagePlaneProducer(final Img<T> img) {
            m_img = img;
            m_plane = new byte[(int)m_img.dimension(0) * (int)m_img.dimension(1)];
            m_cmodel = new SimpleColorModel();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addConsumer(final ImageConsumer ic) {
            if (ic == null) {
                // I don't know why this should happen - but it
                // does!!
                return;
            }
            m_consumers.add(ic);
            initConsumer(ic);
            updateConsumer(ic);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isConsumer(final ImageConsumer ic) {
            return m_consumers.contains(ic);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeConsumer(final ImageConsumer ic) {
            m_consumers.remove(ic);

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void requestTopDownLeftRightResend(final ImageConsumer ic) {
            startProduction(ic);

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void startProduction(final ImageConsumer ic) {
            addConsumer(ic);

        }

        /**
         * @param ip
         */
        public void updateConsumers(final Img<T> ip) {
            m_img = ip;
            m_plane = new byte[(int)m_img.dimension(0) * (int)m_img.dimension(1)];
            for (final ImageConsumer ic : m_consumers) {
                updateConsumer(ic);
            }
        }

        /* */
        private void initConsumer(final ImageConsumer ic) {
            ic.setDimensions((int)m_img.dimension(0), (int)m_img.dimension(1));
            ic.setColorModel(m_cmodel);
            final int hints = ImageConsumer.RANDOMPIXELORDER;
            ic.setHints(hints);
        }

        private void updateConsumer(final ImageConsumer ic) {
            if (m_img == null) {
                return;
            }

            try {
                // we send the first plane of the picture
                final OrthoSliceCursor<T> c = new OrthoSliceCursor<T>(m_img, 0, 1, new long[m_img.numDimensions()]);
                while (c.hasNext()) {
                    c.fwd();
                    m_plane[c.getIntPosition(0) + (c.getIntPosition(1) * (int)m_img.dimension(0))] =
                            (byte)Math.round(normRealType(c.get()) * 255);
                }
                ic.setPixels(0, 0, (int)m_img.dimension(0), (int)m_img.dimension(1), m_cmodel, m_plane, 0,
                             (int)m_img.dimension(0));

                if (isConsumer(ic)) {
                    ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
                }
            } catch (final Exception e) {
                if (isConsumer(ic)) {
                    ic.imageComplete(ImageConsumer.IMAGEERROR);
                }
            }

        }

        /**
         * @return the source {@link Img}
         */
        public Img<T> getImage() {
            return m_img;
        }

        private <T extends RealType<T>> double normRealType(final T type) {
            double value = (type.getRealDouble() - type.getMinValue()) / (type.getMaxValue() - type.getMinValue());

            if (value < 0) {
                value = 0;
            } else if (value > 1) {
                value = 1;
            }

            return value;
        }

    }

    private static class SimpleColorModel extends ColorModel {

        public SimpleColorModel() {
            super(8);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getAlpha(final int pixel) {
            return 255;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getBlue(final int pixel) {
            return pixel;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getGreen(final int pixel) {
            return pixel;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRed(final int pixel) {
            return pixel;
        }

    }

}
