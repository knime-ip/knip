/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2015
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
 * Created on Sep 15, 2015 by pop210958
 */
package org.knime.knip.core.ui.imgviewer;

import java.util.LinkedList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.event.KNIPEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
//import org.knime.knip.core.ui.imgviewer.events.ResetCacheEvent;
import org.knime.knip.core.ui.imgviewer.panels.CombinedRUControlPanel;
import org.knime.knip.core.ui.imgviewer.panels.CombinedRURenderEvent;
import org.knime.knip.core.ui.imgviewer.panels.CombinedRUSynchEvent;
import org.knime.knip.core.ui.imgviewer.panels.MinimapPanel;
import org.knime.knip.core.ui.imgviewer.panels.providers.AWTImageProvider;
import org.knime.knip.core.ui.imgviewer.panels.providers.CombinedRU;
import org.knime.knip.core.ui.imgviewer.panels.providers.ImageRU;
import org.knime.knip.core.ui.imgviewer.panels.providers.LabelingRU;
import org.knime.knip.core.ui.imgviewer.panels.providers.RenderUnit;

/**
 *
 * @author Andreas Burger, University of Konstanz
 */
public class CombinedImgViewer extends ImgViewer {

    protected CombinedRU m_ru;

    protected AWTImageProvider m_provider;

    protected JTabbedPane m_tabbedMenu;

    private int m_cachesize;

    private List<EventService> m_eventServices = new LinkedList<EventService>();

    private EventService m_prevService;

    private boolean m_sync = false;

    private int m_imgCounter = 1;

    private int m_labCounter = 1;

    public CombinedImgViewer(final int cacheSize) {
        super();

        m_cachesize = cacheSize;
        m_eventService.subscribe(this);
        m_ru = new CombinedRU();
        m_ru.setEventService(m_eventService);

        m_provider = new AWTImageProvider(cacheSize, m_ru);
        m_provider.setEventService(m_eventService);
        m_ru.setStackedRendering(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JComponent createMenu() {

        Box menuPanel = new Box(BoxLayout.Y_AXIS);

        menuPanel.add(Box.createVerticalStrut(10));

        MinimapPanel minimap = new MinimapPanel();

        minimap.setEventService(getEventService());

        menuPanel.add(minimap);

        menuPanel.add(Box.createVerticalStrut(5));

        CombinedRUControlPanel c = new CombinedRUControlPanel();

        c.setEventService(getEventService());
        menuPanel.add(c);

        m_tabbedMenu = new JTabbedPane();

        menuPanel.add(m_tabbedMenu);

        menuPanel.add(Box.createVerticalStrut(10));

        menuPanel.add(Box.createVerticalGlue());

        return menuPanel;
    }

    public void addRU(final RenderUnit ru) {
        EventService e = new EventService();
        m_prevService = e;
        e.subscribe(m_ru);
        e.subscribe(m_provider);
        e.subscribe(this);
        m_ru.add(ru);
        m_eventServices.add(e);
        ru.setEventService(e);
        if (ru instanceof ImageRU) {
            m_tabbedMenu.add(ViewerMenuFactory.getCombinedImgViewerImgMenu(e), "Img " + m_imgCounter++);
        }
        if (ru instanceof LabelingRU) {
            m_tabbedMenu.add(ViewerMenuFactory.getCombinedImgViewerLabelingMenu(e), "Labeling " + m_labCounter++);
        }
    }

    public void clear() {

        m_imgCounter = 1;

        m_labCounter = 1;

        m_eventServices.clear();

        m_ru.clear();

        m_tabbedMenu.removeAll();
    }

    public void redraw() {

        broadcast(new ImgRedrawEvent());
        m_eventService.publish(new ImgRedrawEvent());
    }

    public void broadcast(final KNIPEvent e) {
        for (EventService es : m_eventServices) {
            es.publish(e);
        }
    }

    public void publish(final KNIPEvent e) {
        getEventService().publish(e);
    }

    public void publishToPrev(final KNIPEvent e) {
        m_prevService.publish(e);
    }

    public void setStackedRendering() {
        m_ru.setStackedRendering(true);
    }

    @EventListener
    public void onEvent(final PlaneSelectionEvent e) {
        if (m_sync) {
            broadcast(e);
        }
    }

    @EventListener
    public void onCombinedRUSynchChange(final CombinedRUSynchEvent e) {
        m_sync = e.getSyncStatus();
    }

    @EventListener
    public void onCombinedRURenderChange(final CombinedRURenderEvent e) {
        m_ru.invalidateCache();
        m_ru.setStackedRendering(!e.getCombineStatus());
        redraw();
    }

    @Override
    public void addViewerComponent(final ViewerComponent panel, final boolean setEventService) {

        if (setEventService) {
            panel.setEventService(getEventService());
        }

        m_viewerComponents.add(panel);

        switch (panel.getPosition()) {
            case CENTER:
                m_centerPanel.add(panel);
                break;
            case INFO: // CONTROL
                m_infoPanel.add(panel);
                break;
            default: // hidden

        }

    }

}
