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
 * ---------------------------------------------------------------------
 *
 * Created on 18.09.2013 by zinsmaie
 */
package org.knime.knip.core.ui.imgviewer.panels.providers;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.imglib2.labeling.LabelingType;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorImgAndOverlayChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.OverlayChgEvent;
import org.knime.knip.core.ui.imgviewer.overlay.Overlay;

/**
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @param <L>
 */
public class OverlayRU<L extends Comparable<L>> extends AbstractDefaultRU<LabelingType<L>> {

    private static final long serialVersionUID = 1L;

    private Overlay<L> m_overlay;

    private long[] m_srcDims;

    private final GraphicsConfiguration m_graphicsConfig;

    private BufferedImage m_tmpCanvas;

    public OverlayRU() {
        m_graphicsConfig =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }

    @Override
    public Image createImage() {
        if (m_overlay == null) {
            return null;
        }

        long width = m_srcDims[m_sel.getPlaneDimIndex1()];
        long height = m_srcDims[m_sel.getPlaneDimIndex2()];

        if ((m_tmpCanvas == null) || (m_tmpCanvas.getWidth() != width) || (m_tmpCanvas.getHeight() != height)) {
            m_tmpCanvas = m_graphicsConfig.createCompatibleImage((int)width, (int)height, Transparency.TRANSLUCENT);
        }
        Graphics g = m_tmpCanvas.getGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, (int)width, (int)height);

        m_overlay.renderBufferedImage(m_tmpCanvas.getGraphics(), m_sel.getDimIndices(), m_sel.getPlanePos(), 255);

        return m_tmpCanvas;
    }

    @Override
    public int generateHashCode() {
        int hash = super.generateHashCode();
        if (m_overlay != null) {
            hash += m_overlay.hashCode();
            hash *= 31;
        }
        return hash;
    }

    @EventListener
    public void onUpdated(final OverlayChgEvent e) {
        m_overlay = e.getOverlay();
        m_eventService.publish(new ImgRedrawEvent());
    }

    @EventListener
    public void onUpdated(final AnnotatorImgAndOverlayChgEvent e) {
        m_srcDims = new long[e.getImg().numDimensions()];
        e.getImg().dimensions(m_srcDims);
        m_overlay = e.getOverlay();
    }

    @Override
    public void saveAdditionalConfigurations(final ObjectOutput out) throws IOException {
        super.saveAdditionalConfigurations(out);
        m_overlay.writeExternal(out);
    }

    @Override
    public void loadAdditionalConfigurations(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.loadAdditionalConfigurations(in);
        m_overlay = new Overlay<L>();
        m_overlay.readExternal(in);
    }
}
