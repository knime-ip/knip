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
package org.knime.knip.core.ui.imgviewer.annotator;

import java.util.ArrayList;
import java.util.Map;

import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorImgAndOverlayChgEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelEditEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsColResetEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsDelEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsSelChgEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsSetEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorToolChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerMouseDraggedEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerMousePressedEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerMouseReleasedEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.OverlayChgEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.overlay.Overlay;
import org.knime.knip.core.ui.imgviewer.overlay.OverlayElement2D;
import org.knime.knip.core.ui.imgviewer.panels.HiddenViewerComponent;

/**
 * Manages overlays and overlay elements ...
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Christian
 */
public abstract class AbstractAnnotatorManager<T extends RealType<T>> extends HiddenViewerComponent {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected String[] m_selectedLabels;

    protected PlaneSelectionEvent m_sel;

    /* Are not serialized or calculated from serzalization values */
    protected EventService m_eventService;

    protected Overlay<String> m_currentOverlay;

    protected AnnotatorTool<?> m_currentTool;

    public AbstractAnnotatorManager() {
        m_selectedLabels = new String[]{"Unknown"};
    }

    protected abstract Map<String, Overlay<String>> getOverlayMap();

    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
        eventService.subscribe(this);
    }

    @EventListener
    public void onLabelsColorReset(final AnnotatorLabelsColResetEvent e) {
        for (final String label : e.getLabels()) {
            RandomMissingColorHandler.resetColor(label);
        }

        m_eventService.publish(new OverlayChgEvent(m_currentOverlay));
    }

    @EventListener
    public void onSetClassLabels(final AnnotatorLabelsSetEvent e) {
        if (m_currentTool != null) {
            m_currentTool.setLabelsCurrentElements(m_currentOverlay, e.getLabels());
        }
    }

    @EventListener
    public void onSelectedLabelsChg(final AnnotatorLabelsSelChgEvent e) {
        m_selectedLabels = e.getLabels();
    }

    @EventListener
    public void onToolChange(final AnnotatorToolChgEvent e) {
        if (m_currentTool != null) {
            m_currentTool.fireFocusLost(m_currentOverlay);
        }

        m_currentTool = e.getTool();

    }

    @EventListener
    public void onLabelsDeleted(final AnnotatorLabelsDelEvent e) {
        ArrayList<OverlayElement2D<String>> m_removeList = new ArrayList<OverlayElement2D<String>>();

        for (final Overlay<String> overlay : getOverlayMap().values()) {
            for (final OverlayElement2D<String> element : overlay.getElements()) {
                for (final String label : e.getLabels()) {
                    element.getLabels().remove(label);
                }

                if (element.getLabels().size() == 0) {
                    m_removeList.add(element);
                }
            }

            overlay.removeAll(m_removeList);
            m_removeList.clear();
        }
        if (m_currentOverlay != null) {
            m_currentOverlay.fireOverlayChanged();
        }
    }

    /**
     * @param axes
     */
    @EventListener
    public void onUpdate(final IntervalWithMetadataChgEvent<T> e) {

        m_currentOverlay = getOverlayMap().get(e.getSource().getSource());

        if (m_currentOverlay == null) {
            m_currentOverlay = new Overlay<String>(e.getRandomAccessibleInterval());
            getOverlayMap().put(e.getSource().getSource(), m_currentOverlay);
            m_currentOverlay.setEventService(m_eventService);
        }

        final long[] dims = new long[e.getRandomAccessibleInterval().numDimensions()];
        e.getRandomAccessibleInterval().dimensions(dims);

        if ((m_sel == null) || !isInsideDims(m_sel.getPlanePos(), dims)) {
            m_sel = new PlaneSelectionEvent(0, 1, new long[e.getRandomAccessibleInterval().numDimensions()]);
        }

        m_eventService.publish(new AnnotatorImgAndOverlayChgEvent(e.getRandomAccessibleInterval(), m_currentOverlay));

        m_eventService.publish(new ImgRedrawEvent());
    }

    private boolean isInsideDims(final long[] planePos, final long[] dims) {
        if (planePos.length != dims.length) {
            return false;
        }

        for (int d = 0; d < planePos.length; d++) {
            if (planePos[d] >= dims[d]) {
                return false;
            }
        }

        return true;
    }

    @EventListener
    public void onUpdate(final PlaneSelectionEvent sel) {
        m_sel = sel;
    }

    @EventListener
    public void onLabelEdit(final AnnotatorLabelEditEvent e) {
        for (final Overlay<String> overlay : getOverlayMap().values()) {
            for (final OverlayElement2D<String> element : overlay.getElements()) {
                if (element.getLabels().remove(e.getOldLabel())) {
                    element.getLabels().add(e.getNewLabel());
                }
            }
        }
        onSelectedLabelsChg(new AnnotatorLabelsSelChgEvent(e.getNewLabel()));

        RandomMissingColorHandler.setColor(e.getNewLabel(), RandomMissingColorHandler.getLabelColor(e.getOldLabel()));
    }

    /*
     * Handling mouse events
     */

    @EventListener
    public void onMousePressed(final ImgViewerMousePressedEvent e) {

        if ((m_currentOverlay != null) && (m_currentTool != null)) {
            m_currentTool.onMousePressed(e, m_sel, m_currentOverlay, m_selectedLabels);
        }
    }

    @EventListener
    public void onMouseDragged(final ImgViewerMouseDraggedEvent e) {

        if ((m_currentOverlay != null) && (m_currentTool != null)) {
            m_currentTool.onMouseDragged(e, m_sel, m_currentOverlay, m_selectedLabels);
        }
    }

    @EventListener
    public void onMouseReleased(final ImgViewerMouseReleasedEvent e) {
        if ((m_currentOverlay != null) && (m_currentTool != null)) {
            if (e.getClickCount() > 1) {
                m_currentTool.onMouseDoubleClick(e, m_sel, m_currentOverlay, m_selectedLabels);
            } else {
                m_currentTool.onMouseReleased(e, m_sel, m_currentOverlay, m_selectedLabels);
            }

        }
    }

    /**
     * {@inheritDoc}
     */
    @EventListener
    public void reset(final AnnotatorResetEvent e) {
        m_currentOverlay = null;
        m_selectedLabels = new String[]{"Unknown"};
    }

}
