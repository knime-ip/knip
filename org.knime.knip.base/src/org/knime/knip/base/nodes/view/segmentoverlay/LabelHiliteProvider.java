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
package org.knime.knip.base.nodes.view.segmentoverlay;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Set;

import net.imglib2.RandomAccess;
import net.imglib2.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.RowKey;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.events.HilitedLabelsChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgAndLabelingChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerMousePressedEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelPanelHiliteSelectionChgEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.panels.HiddenViewerComponent;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelHiliteProvider<L extends Comparable<L>, T extends RealType<T>> extends HiddenViewerComponent {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private EventService m_eventService;

    private Set<String> m_hilitedLabels;

    private HiLiteHandler m_hiliteHandler;

    private HiLiteListener m_hiliteListener;

    private RandomAccess<LabelingType<L>> m_labelingAccess;

    private String m_rowKey;

    private PlaneSelectionEvent m_sel;

    public LabelHiliteProvider() {
        m_hilitedLabels = new HashSet<String>();
    }

    public void clearHiliteListener() {
        m_hilitedLabels.clear();

        if (m_hiliteHandler != null) {
            m_hiliteHandler.removeHiLiteListener(m_hiliteListener);
        }
    }

    public void close() {
        clearHiliteListener();
    }

    private void createHiliteFromHandler(final HiLiteHandler hiliteHandler) {
        m_hilitedLabels.clear();
        if (hiliteHandler != null) {
            handleHiLiteEvent(hiliteHandler.getHiLitKeys());
        }
    }

    private void handleHiLiteEvent(final Set<RowKey> rowKeys) {
        for (final RowKey key : rowKeys) {
            m_hilitedLabels.add(key.getString());
        }

        m_eventService.publish(new HilitedLabelsChgEvent(m_hilitedLabels));
    }

    private void handleUnHiLiteEvent(final Set<RowKey> rowKeys) {
        for (final RowKey key : rowKeys) {
            m_hilitedLabels.remove(key.getString());
        }

        m_eventService.publish(new HilitedLabelsChgEvent(m_hilitedLabels));
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_hilitedLabels = (Set<String>)in.readObject();
        m_rowKey = in.readUTF();
    }

    public void onClose() {
        clearHiliteListener();
        m_hilitedLabels = null;
        m_labelingAccess = null;
    }

    /**
     * @param img
     * @param axes
     */
    @EventListener
    public void onLabelingUpdated(final ImgAndLabelingChgEvent<T, L> e) {
        m_rowKey = e.getName().getName();
        m_labelingAccess = e.getLabeling().randomAccess();

        createHiliteFromHandler(m_hiliteHandler);
    }

    @EventListener
    public void onLabelSelectionHilite(final LabelPanelHiliteSelectionChgEvent e) {
        final Set<RowKey> keys = new HashSet<RowKey>();

        for (final String s : e.getLabels()) {

            if (!m_hilitedLabels.contains(s)) {
                if (e.isHilite()) {
                    m_hilitedLabels.add(s);
                    keys.add(new RowKey(s));
                }
            } else if (!e.isHilite()) {
                m_hilitedLabels.remove(s);
                keys.add(new RowKey(s));
            }

        }

        if (e.isHilite()) {
            m_hiliteHandler.fireHiLiteEvent(keys);
        } else {
            m_hiliteHandler.fireUnHiLiteEvent(keys);
        }
    }

    @EventListener
    public void onMousePressed(final ImgViewerMousePressedEvent e) {
        if (e.isInside()) {

            if (m_sel == null) {
                m_sel = new PlaneSelectionEvent(0, 1, new long[m_labelingAccess.numDimensions()]);
            }

            m_labelingAccess.setPosition(e.getPosX(), m_sel.getPlaneDimIndex1());
            m_labelingAccess.setPosition(e.getPosY(), m_sel.getPlaneDimIndex2());

            boolean hilite = false;
            final Set<RowKey> keys = new HashSet<RowKey>();
            for (final L label : m_labelingAccess.get().getLabeling()) {
                final String l = label.toString();
                if (!m_hilitedLabels.contains(l)) {
                    m_hilitedLabels.add(l);
                    hilite = true;
                }

                keys.add(new RowKey(l));
            }

            if (hilite) {
                m_hiliteHandler.fireHiLiteEvent(keys);
            } else {
                m_hiliteHandler.fireUnHiLiteEvent(keys);
            }
        }
    }

    @EventListener
    public void onUpdate(final PlaneSelectionEvent sel) {
        m_sel = sel;

        if (m_labelingAccess != null) {
            m_labelingAccess.setPosition(sel.getPlanePos());
        }
    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        out.writeObject(m_hilitedLabels);
        out.writeUTF(m_rowKey);
    }

    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
        eventService.subscribe(this);
    }

    public void updateInHandler(final HiLiteHandler hiliteInHandler) {

        // nothing has changed, we come away easy
        if (hiliteInHandler == m_hiliteHandler) {
            return;
        }

        m_hilitedLabels.clear();
        if (m_hiliteHandler != null) {
            m_hiliteHandler.removeHiLiteListener(m_hiliteListener);
        }

        m_hiliteHandler = hiliteInHandler;
        createHiliteFromHandler(m_hiliteHandler);

        m_hiliteListener = new HiLiteListener() {
            @Override
            public void hiLite(final KeyEvent event) {
                final Set<RowKey> rowKeys = event.keys();
                handleHiLiteEvent(rowKeys);
                m_eventService.publish(new ImgRedrawEvent());
            }

            @Override
            public void unHiLite(final KeyEvent event) {
                final Set<RowKey> rowKeys = event.keys();
                handleUnHiLiteEvent(rowKeys);
                m_eventService.publish(new ImgRedrawEvent());
            }

            @Override
            public void unHiLiteAll(final KeyEvent event) {
                m_hilitedLabels.clear();
                m_eventService.publish(new HilitedLabelsChgEvent(m_hilitedLabels));
                m_eventService.publish(new ImgRedrawEvent());
            }
        };
        m_hiliteHandler.addHiLiteListener(m_hiliteListener);
    }

}
