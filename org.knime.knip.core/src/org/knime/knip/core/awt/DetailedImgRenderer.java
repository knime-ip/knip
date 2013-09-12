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
import java.awt.Dimension;
import java.awt.Graphics;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.display.ScreenImage;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.meta.Named;
import net.imglib2.meta.Sourced;
import net.imglib2.meta.TypedAxis;
import net.imglib2.meta.TypedSpace;
import net.imglib2.type.Type;

/**
 * 
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DetailedImgRenderer<T extends Type<T>> implements ImageRenderer<T> {

    /* for source images below that size, no details will be shown */
    private static final Dimension MIN_SIZE = new Dimension(150, 150);

    private final ProjectingRenderer<T> m_projectingRenderer;

    private Sourced m_imgSource;

    private Named m_imgName;

    private TypedSpace<? extends TypedAxis> m_axes;

    private int m_height;

    public DetailedImgRenderer(final ProjectingRenderer<T> projectingRenderer) {
        m_projectingRenderer = projectingRenderer;
    }

    public void setHeight(final int height) {
        m_height = height;
    }

    public void setMetaData(final ImgPlusMetadata meta) {
        m_imgSource = meta;
        m_imgName = meta;
        m_axes = meta;
    }

    public ImageRenderer<T> getUnderlyingRenderer() {
        return m_projectingRenderer;
    }

    @Override
    public ScreenImage render(final RandomAccessibleInterval<T> source, final int dimX, final int dimY,
                              final long[] planePos) {

        final long[] orgDims = new long[planePos.length];
        source.dimensions(orgDims);

        // create information string
        final StringBuffer sb = new StringBuffer();

        for (int i = 0; i < planePos.length; i++) {
            if (m_axes != null) {
                sb.append("Size " + m_axes.axis(i).type().getLabel() + "=" + orgDims[i] + "\n");
            } else {
                sb.append("Size " + i + "=" + orgDims[i] + "\n");
            }
        }

        sb.append("Pixel Type=" + source.randomAccess().get().getClass().getSimpleName() + "\n");

        sb.append("Image Type=" + source.getClass().getSimpleName() + "\n");

        if (m_imgName != null) {
            sb.append("Image Name=" + m_imgName.getName() + "\n");
        }

        if (m_imgSource != null) {
            sb.append("Image Source=" + m_imgSource.getSource());
        }
        final int lineHeight = 15;
        final int posX = 10;
        final String[] tmp = sb.toString().split("\n");

        // render image and created information string
        final ScreenImage res = m_projectingRenderer.render(source, dimX, dimY, planePos);

        final int width = (int)(orgDims[dimX] * ((double)m_height / orgDims[dimY]));

        if ((width < MIN_SIZE.width) || (m_height < MIN_SIZE.height)) {
            // scale render without text
            final ScreenImage scaledRes = new ARGBScreenImage(width, m_height);
            final Graphics g = scaledRes.image().getGraphics();
            g.drawImage(res.image(), 0, 0, width, m_height, null);

            return scaledRes;
        } else {
            // scale render with text
            final ScreenImage composedRes = new ARGBScreenImage(width, m_height);
            final Graphics g = composedRes.image().getGraphics();
            g.drawImage(res.image(), 0, 0, width, m_height, null);
            g.setXORMode(Color.black);

            for (int i = 0; i < tmp.length; i++) {
                g.drawString(tmp[i], posX, composedRes.image().getHeight(null) - ((tmp.length - i) * lineHeight));
            }
            return composedRes;
        }
    }

    @Override
    public String toString() {
        return m_projectingRenderer.toString() + " (detailed)";
    }
}
