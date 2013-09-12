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

import net.imglib2.display.ColorTable;
import net.imglib2.meta.ImageMetadata;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DefaultImageMetadata implements ImageMetadata {

    private int m_validBits;

    private final ArrayList<Double> m_channelMin;

    private final ArrayList<Double> m_channelMax;

    private int m_compositeChannelCount = 1;

    private final ArrayList<ColorTable> m_lut;

    public DefaultImageMetadata() {
        this.m_channelMin = new ArrayList<Double>();
        this.m_channelMax = new ArrayList<Double>();

        this.m_lut = new ArrayList<ColorTable>();
    }

    @Override
    public int getValidBits() {
        return m_validBits;
    }

    @Override
    public void setValidBits(final int bits) {
        m_validBits = bits;
    }

    @Override
    public double getChannelMinimum(final int c) {
        if ((c < 0) || (c >= m_channelMin.size())) {
            return Double.NaN;
        }
        final Double d = m_channelMin.get(c);
        return d == null ? Double.NaN : d;
    }

    @Override
    public void setChannelMinimum(final int c, final double min) {
        if (c < 0) {
            throw new IllegalArgumentException("Invalid channel: " + c);
        }
        if (c >= m_channelMin.size()) {
            m_channelMin.ensureCapacity(c + 1);
            for (int i = m_channelMin.size(); i <= c; i++) {
                m_channelMin.add(null);
            }
        }
        m_channelMin.set(c, min);
    }

    @Override
    public double getChannelMaximum(final int c) {
        if ((c < 0) || (c >= m_channelMax.size())) {
            return Double.NaN;
        }
        final Double d = m_channelMax.get(c);
        return d == null ? Double.NaN : d;
    }

    @Override
    public void setChannelMaximum(final int c, final double max) {
        if (c < 0) {
            throw new IllegalArgumentException("Invalid channel: " + c);
        }
        if (c >= m_channelMax.size()) {
            m_channelMax.ensureCapacity(c + 1);
            for (int i = m_channelMax.size(); i <= c; i++) {
                m_channelMax.add(null);
            }
        }
        m_channelMax.set(c, max);
    }

    @Override
    public int getCompositeChannelCount() {
        return m_compositeChannelCount;
    }

    @Override
    public void setCompositeChannelCount(final int value) {
        m_compositeChannelCount = value;
    }

    @Override
    public void initializeColorTables(final int count) {
        m_lut.ensureCapacity(count);
        m_lut.clear();
        for (int i = 0; i < count; i++) {
            m_lut.add(null);
        }
    }

    @Override
    public int getColorTableCount() {
        return m_lut.size();
    }

    @Override
    public ColorTable getColorTable(final int no) {
        return m_lut.get(no);
    }

    @Override
    public void setColorTable(final ColorTable colorTable, final int no) {
        m_lut.set(no, colorTable);
    }

}
