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
package org.knime.knip.core.ui.imgviewer.annotator.tools;

import java.util.ArrayList;
import java.util.List;

import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorTool;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerMouseEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.overlay.Overlay;
import org.knime.knip.core.ui.imgviewer.overlay.OverlayElement2D;
import org.knime.knip.core.ui.imgviewer.overlay.OverlayElementStatus;
import org.knime.knip.core.ui.imgviewer.overlay.elements.AbstractPolygonOverlayElement;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class AnnotatorSelectionTool extends AnnotatorTool<OverlayElement2D<String>> {

    private final List<OverlayElement2D<String>> m_elements;

    private int m_selectedIndex = -1;

    public AnnotatorSelectionTool() {
        super("Selection", "tool-select.png");
        m_elements = new ArrayList<OverlayElement2D<String>>();

    }

    private void clearSelectedElements() {
        for (final OverlayElement2D<String> element : m_elements) {
            element.setStatus(OverlayElementStatus.IDLE);
        }
        m_elements.clear();
    }

    @Override
    public void fireFocusLost(final Overlay<String> overlay) {
        m_selectedIndex = -1;
        if (setCurrentOverlayElement(null, null)) {
            fireStateChanged();
        }

        tryToFireStateChanged(overlay);
    }

    @Override
    public void setLabelsCurrentElements(final Overlay<String> overlay, final String[] selectedLabels) {
        if (!m_elements.isEmpty()) {
            for (final OverlayElement2D<String> element : m_elements) {
                element.setLabels(selectedLabels);
            }
            overlay.fireOverlayChanged();
        }
    }

    @Override
    public void onMouseDoubleClickLeft(final ImgViewerMouseEvent e,
                                       final OverlayElement2D<String> currentOverlayElement,
                                       final PlaneSelectionEvent selection, final Overlay<String> overlay,
                                       final String... labels) {
        // Nothing to do here

    }

    @Override
    public void onMousePressedLeft(final ImgViewerMouseEvent e, final OverlayElement2D<String> currentOverlayElement,
                                   final PlaneSelectionEvent selection, final Overlay<String> overlay,
                                   final String... labels) {
        final List<OverlayElement2D<String>> elements =
                overlay.getElementsByPosition(selection.getPlanePos(e.getPosX(), e.getPosY()),
                                              selection.getDimIndices());

        if (!elements.isEmpty()) {

            if (!e.isControlDown()) {

                if (elements.get(0) != currentOverlayElement) {
                    clearSelectedElements();

                    m_elements.add(elements.get(0));

                    if (setCurrentOverlayElement(OverlayElementStatus.ACTIVE, m_elements.get(0))) {
                        fireStateChanged();
                    }
                } else if (currentOverlayElement instanceof AbstractPolygonOverlayElement) {
                    m_selectedIndex =
                            ((AbstractPolygonOverlayElement<String>)currentOverlayElement).getPointIndexByPosition(e
                                    .getPosX(), e.getPosY(), 3);
                }

            } else {
                m_elements.add(elements.get(0));
                elements.get(0).setStatus(OverlayElementStatus.ACTIVE);
                fireStateChanged();
            }

        } else {
            clearSelectedElements();
            if (setCurrentOverlayElement(null, null)) {
                fireStateChanged();
            }
        }

    }

    @Override
    public void onMouseReleasedLeft(final ImgViewerMouseEvent e, final OverlayElement2D<String> currentOverlayElement,
                                    final PlaneSelectionEvent selection, final Overlay<String> overlay,
                                    final String... labels) {
        // Nothing to do here
    }

    @Override
    public void onMouseDraggedLeft(final ImgViewerMouseEvent e, final OverlayElement2D<String> currentOverlayElement,
                                   final PlaneSelectionEvent selection, final Overlay<String> overlay,
                                   final String... labels) {

        if (!m_elements.isEmpty()) {
            final long[] pos = selection.getPlanePos(e.getPosX(), e.getPosY()).clone();

            for (int d = 0; d < pos.length; d++) {
                pos[d] -= getDragPoint()[d];
            }

            if (currentOverlayElement != null) {
                if ((m_selectedIndex == -1)) {
                    currentOverlayElement.translate(pos);
                } else {
                    ((AbstractPolygonOverlayElement<String>)currentOverlayElement)
                            .translate(m_selectedIndex, pos[selection.getPlaneDimIndex1()],
                                       pos[selection.getPlaneDimIndex2()]);
                }
            }
            fireStateChanged();
        }

    }

    @Override
    public void onMouseDoubleClickRight(final ImgViewerMouseEvent e,
                                        final OverlayElement2D<String> currentOverlayElement,
                                        final PlaneSelectionEvent selection, final Overlay<String> overlay,
                                        final String... labels) {
        m_selectedIndex = -1;
    }

    @Override
    public void onMousePressedRight(final ImgViewerMouseEvent e, final OverlayElement2D<String> currentOverlayElement,
                                    final PlaneSelectionEvent selection, final Overlay<String> overlay,
                                    final String... labels) {
        m_selectedIndex = -1;

    }

    @Override
    public void onMouseReleasedRight(final ImgViewerMouseEvent e, final OverlayElement2D<String> currentOverlayElement,
                                     final PlaneSelectionEvent selection, final Overlay<String> overlay,
                                     final String... labels) {
        m_selectedIndex = -1;
        overlay.removeAll(m_elements);
        if (setCurrentOverlayElement(OverlayElementStatus.IDLE, null)) {
            fireStateChanged();
        }

    }

    @Override
    public void onMouseDraggedRight(final ImgViewerMouseEvent e, final OverlayElement2D<String> currentOverlayElement,
                                    final PlaneSelectionEvent selection, final Overlay<String> overlay,
                                    final String... labels) {
        m_selectedIndex = -1;
    }

    @Override
    public void onMouseDoubleClickMid(final ImgViewerMouseEvent e,
                                      final OverlayElement2D<String> currentOverlayElement,
                                      final PlaneSelectionEvent selection, final Overlay<String> overlay,
                                      final String... labels) {
        // Nothing to do here
    }

    @Override
    public void onMousePressedMid(final ImgViewerMouseEvent e, final OverlayElement2D<String> currentOverlayElement,
                                  final PlaneSelectionEvent selection, final Overlay<String> overlay,
                                  final String... labels) {
        // Nothing to do here
    }

    @Override
    public void onMouseReleasedMid(final ImgViewerMouseEvent e, final OverlayElement2D<String> currentOverlayElement,
                                   final PlaneSelectionEvent selection, final Overlay<String> overlay,
                                   final String... labels) {

    }

    @Override
    public void onMouseDraggedMid(final ImgViewerMouseEvent e, final OverlayElement2D<String> currentOverlayElement,
                                  final PlaneSelectionEvent selection, final Overlay<String> overlay,
                                  final String... labels) {
        // Nothing to do here
    }
}
