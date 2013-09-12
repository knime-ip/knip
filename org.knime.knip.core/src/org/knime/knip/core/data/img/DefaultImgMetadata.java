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
package org.knime.knip.core.data.img;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.display.ColorTable;
import net.imglib2.meta.Axes;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.DefaultCalibratedAxis;
import net.imglib2.meta.ImageMetadata;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.meta.Named;
import net.imglib2.meta.Sourced;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public final class DefaultImgMetadata extends AbstractGeneralMetadata implements ImgPlusMetadata {

    private final ImageMetadata m_imageMetadata;

    /**
     * @param numDims
     */
    public DefaultImgMetadata(final int numDims) {
        super(numDims);
        m_imageMetadata = new DefaultImageMetadata();
    }

    /**
     * @param cs
     * @param named
     * @param source
     * @param imageMetadata
     */
    public DefaultImgMetadata(final ImgPlusMetadata imgMetadata) {
        super(imgMetadata, imgMetadata, imgMetadata);
        m_imageMetadata = imgMetadata;

    }

    /**
     * @param cs
     * @param named, defaultSource
     * @param sourced
     * @param imageMetadata
     */
    public DefaultImgMetadata(final CalibratedSpace<CalibratedAxis> cs, final Named named, final Sourced sourced,
                              final ImageMetadata imageMetadata) {
        super(cs, named, sourced);
        m_imageMetadata = imageMetadata;
    }

    /**
     * @param axes
     * @return
     */
    private static List<CalibratedAxis> createCalibratedAxis(final String[] axes) {
        List<CalibratedAxis> list = new ArrayList<CalibratedAxis>();
        for (int s = 0; s < axes.length; s++) {
            list.add(new DefaultCalibratedAxis(Axes.get(axes[s])));
        }
        return list;
    }

    @Override
    public int getValidBits() {
        return m_imageMetadata.getValidBits();
    }

    @Override
    public void setValidBits(final int bits) {
        m_imageMetadata.setValidBits(bits);
    }

    @Override
    public double getChannelMinimum(final int c) {
        return m_imageMetadata.getChannelMinimum(c);
    }

    @Override
    public void setChannelMinimum(final int c, final double min) {
        m_imageMetadata.setChannelMinimum(c, min);
    }

    @Override
    public double getChannelMaximum(final int c) {
        return m_imageMetadata.getChannelMaximum(c);
    }

    @Override
    public void setChannelMaximum(final int c, final double max) {
        m_imageMetadata.setChannelMaximum(c, max);
    }

    @Override
    public int getCompositeChannelCount() {
        return m_imageMetadata.getCompositeChannelCount();
    }

    @Override
    public void setCompositeChannelCount(final int count) {
        m_imageMetadata.setCompositeChannelCount(count);
    }

    @Override
    public void initializeColorTables(final int count) {
        m_imageMetadata.initializeColorTables(count);
    }

    @Override
    public int getColorTableCount() {
        return m_imageMetadata.getColorTableCount();
    }

    @Override
    public ColorTable getColorTable(final int no) {
        return m_imageMetadata.getColorTable(no);
    }

    @Override
    public void setColorTable(final ColorTable colorTable, final int no) {
        m_imageMetadata.setColorTable(colorTable, no);
    }
}
