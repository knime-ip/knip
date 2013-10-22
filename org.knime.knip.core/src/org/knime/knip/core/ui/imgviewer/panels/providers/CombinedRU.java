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
 * Created on 20.09.2013 by zinsmaie
 */
package org.knime.knip.core.ui.imgviewer.panels.providers;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;

import org.knime.knip.core.awt.Transparency;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.events.TransparencyPanelValueChgEvent;

/**
 * Combines multiple {@link RenderUnit}s by blending their result images together. The color WHITE is treated as
 * transparent color.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class CombinedRU implements RenderUnit {

    /**
     * each rendering unit produces a {@link Image} and all {@link Image}s get blended for together to produce the final
     * outcome.
     */
    private RenderUnit[] m_renderUnits;

    /** a graphic context that fits the current environment (OS ...) and can be used to create images. */
    private GraphicsConfiguration m_graphicsConfig;

    /** Identifying hashCode of the last rendered image. */
    private int m_hashOfLastRendering;

    /** caches the last rendered image. */
    private Image m_lastImage;

    // event members

    private EventService m_eventService;

    private Integer m_transparency = 128;

    /**
     * Uses different parameters from the {@link EventService} to create images using its associated {@link RenderUnit}
     * s. The images are blended together and put in a cache for efficient image management.
     * 
     * @param renderUnits the created images of the renderUnits are blended together to create the result {@link Image}
     *            of the provider.
     */
    public CombinedRU(final RenderUnit... renderUnits) {
        m_renderUnits = renderUnits;
        m_graphicsConfig =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }

    /**
     * Creates a blended image, that consists of the result {@link Image}s of the active {@link RenderUnit}s. The parts
     * are joined using the transparency value from the last {@link TransparencyPanelValueChgEvent}.
     * 
     * @return blended {@link Image}
     */
    @Override
    public Image createImage() {
        if (m_hashOfLastRendering == generateHashCode() && m_lastImage != null) {
            return m_lastImage;
        }

        Image ret;
        int i = 0;

        //forward to the first active image
        while (i < m_renderUnits.length) {
            if (m_renderUnits[i].isActive()) {
                break;
            }
            i++;
        }

        if (i < m_renderUnits.length) {
            //at least one active image
            Image img = m_renderUnits[0].createImage();
            Image joinedImg =
                    m_graphicsConfig.createCompatibleImage(img.getWidth(null), img.getHeight(null),
                                                           java.awt.Transparency.TRANSLUCENT);
            Graphics g = joinedImg.getGraphics();
            g.drawImage(img, 0, 0, null);
            i++;

            //blend in the other active images
            while (i < m_renderUnits.length) {
                if (m_renderUnits[i].isActive()) {
                    g.drawImage(Transparency.makeColorTransparent(m_renderUnits[i].createImage(), Color.WHITE,
                                                                  m_transparency), 0, 0, null);
                }
                i++;
            }

            ret = joinedImg;
        } else {
            //no active renderer create dummy image
            ret = m_graphicsConfig.createCompatibleImage(100, 100);
        }

        m_lastImage = ret;
        m_hashOfLastRendering = generateHashCode();

        return ret;
    }

    /**
     * @return combined HashCode of all {@link RenderUnit}s. By contract that allows to identify images generated by
     *         {@link #createImage()} including all parameters that have influence on the creation.
     */
    @Override
    public int generateHashCode() {
        int hash = 0;
        hash += m_transparency;
        hash *= 31;
        for (RenderUnit ru : m_renderUnits) {
            hash += ru.generateHashCode();
            hash *= 31;
        }
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        boolean isActive = true;
        for (RenderUnit ru : m_renderUnits) {
            isActive = isActive & ru.isActive();
        }

        return isActive;
    }

    // event handling

    /**
     * The transparency value determines how the {@link Image}s of multiple {@link RenderUnit}s get blended together.
     * 
     * @param e transparency value used for blending
     */
    @EventListener
    public void onUpdate(final TransparencyPanelValueChgEvent e) {
        m_transparency = e.getTransparency();
    }

    //standard methods

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService service) {
        m_eventService = service;
        service.subscribe(this);
        for (RenderUnit ru : m_renderUnits) {
            ru.setEventService(m_eventService);
        }
    }

}
