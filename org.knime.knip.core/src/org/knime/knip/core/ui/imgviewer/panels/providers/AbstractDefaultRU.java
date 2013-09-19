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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.imglib2.img.Img;
import net.imglib2.type.Type;

import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.ImageRenderer;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.events.RendererSelectionChgEvent;

/**
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class AbstractDefaultRU<T extends Type<T>> implements RenderUnit {

    /**
     * {@link PlaneSelectionEvent} indicating the current plane coordinates in the {@link Img} which will be rendered
     */
    protected PlaneSelectionEvent m_sel;

    protected ImageRenderer<T> m_renderer;

    protected EventService m_eventService;

    /**
     * {@link EventListener} for {@link PlaneSelectionEvent} events The {@link PlaneSelectionEvent} of the
     * {@link AWTImageTools} will be updated
     *
     * Renders and caches the image
     *
     * @param img {@link Img} to render
     * @param sel {@link PlaneSelectionEvent}
     */
    @EventListener
    public void onPlaneSelectionUpdate(final PlaneSelectionEvent sel) {
        m_sel = sel;
    }

    /**
     *
     * Renders and caches the image
     *
     * @param renderer {@link ImgRenderer} which will be used to render the {@link BufferedImage}
     *
     */
    @EventListener
    public void onRendererUpdate(final RendererSelectionChgEvent e) {
        m_renderer = e.getRenderer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService service) {
        m_eventService = service;
        service.subscribe(this);
    }

    /**
     * Generates a hashcode according to the parameters of {@link PlaneSelectionEvent}, and {@link ImgRenderer}.
     *
     * HashCode is generated as hash = 31 + object.hashCode()*31 + object.hashCode()*31 ...
     *
     * @return HashCode
     */
    @Override
    public int generateHashCode() {
        int hash = 31;
        hash += m_sel.hashCode();
        hash *= 31;
        hash += m_renderer.getClass().hashCode();
        hash *= 31;
        hash += m_renderer.toString().hashCode(); //if the user information differs
        hash *= 31; //re-rendering is most likely necessary

        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalConfigurations(final ObjectOutput out) throws IOException {
        //TODO should the selected renderer be saved?
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalConfigurations(final ObjectInput in) throws IOException, ClassNotFoundException {
        //TODO should the selected renderer be saved?
    }
}
