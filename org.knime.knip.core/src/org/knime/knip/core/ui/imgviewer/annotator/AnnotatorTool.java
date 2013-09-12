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

import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;

import org.knime.knip.core.ui.imgviewer.events.ImgViewerMouseEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.overlay.Overlay;
import org.knime.knip.core.ui.imgviewer.overlay.OverlayElement2D;
import org.knime.knip.core.ui.imgviewer.overlay.OverlayElementStatus;

/**
 * 
 * @param <O>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author dietyc
 */
public abstract class AnnotatorTool<O extends OverlayElement2D<String>> {

    private final String m_name;

    private final String m_iconPath;

    private long[] m_dragPoint;

    private boolean m_stateChanged;

    private O m_currentOverlayElement;

    /**
     * @param e
     * @param currentOverlayElement
     * @param selection
     * @param overlay
     * @param labels
     */
    public abstract void onMouseDoubleClickLeft(ImgViewerMouseEvent e, O currentOverlayElement,
                                                PlaneSelectionEvent selection, Overlay<String> overlay,
                                                String... labels);

    /**
     * @param e
     * @param currentOverlayElement
     * @param selection
     * @param overlay
     * @param labels
     */
    public abstract void onMousePressedLeft(ImgViewerMouseEvent e, O currentOverlayElement,
                                            PlaneSelectionEvent selection, Overlay<String> overlay, String... labels);

    /**
     * @param e
     * @param currentOverlayElement
     * @param selection
     * @param overlay
     * @param labels
     */
    public abstract void onMouseReleasedLeft(ImgViewerMouseEvent e, O currentOverlayElement,
                                             PlaneSelectionEvent selection, Overlay<String> overlay, String... labels);

    /**
     * @param e
     * @param currentOverlayElement
     * @param selection
     * @param overlay
     * @param labels
     */
    public abstract void onMouseDraggedLeft(ImgViewerMouseEvent e, O currentOverlayElement,
                                            PlaneSelectionEvent selection, Overlay<String> overlay, String... labels);

    /**
     * @param e
     * @param currentOverlayElement
     * @param selection
     * @param overlay
     * @param labels
     */
    public abstract void onMouseDoubleClickRight(ImgViewerMouseEvent e, O currentOverlayElement,
                                                 PlaneSelectionEvent selection, Overlay<String> overlay,
                                                 String... labels);

    /**
     * @param e
     * @param currentOverlayElement
     * @param selection
     * @param overlay
     * @param labels
     */
    public abstract void onMousePressedRight(ImgViewerMouseEvent e, O currentOverlayElement,
                                             PlaneSelectionEvent selection, Overlay<String> overlay, String... labels);

    /**
     * @param e
     * @param currentOverlayElement
     * @param selection
     * @param overlay
     * @param labels
     */
    public abstract void onMouseReleasedRight(ImgViewerMouseEvent e, O currentOverlayElement,
                                              PlaneSelectionEvent selection, Overlay<String> overlay, String... labels);

    /**
     * @param e
     * @param currentOverlayElement
     * @param selection
     * @param overlay
     * @param labels
     */
    public abstract void onMouseDraggedRight(ImgViewerMouseEvent e, O currentOverlayElement,
                                             PlaneSelectionEvent selection, Overlay<String> overlay, String... labels);

    /**
     * @param e
     * @param currentOverlayElement
     * @param selection
     * @param overlay
     * @param labels
     */
    public abstract void
            onMouseDoubleClickMid(ImgViewerMouseEvent e, O currentOverlayElement, PlaneSelectionEvent selection,
                                  Overlay<String> overlay, String... labels);

    /**
     * @param e
     * @param currentOverlayElement
     * @param selection
     * @param overlay
     * @param labels
     */
    public abstract void onMousePressedMid(ImgViewerMouseEvent e, O currentOverlayElement,
                                           PlaneSelectionEvent selection, Overlay<String> overlay, String... labels);

    /**
     * @param e
     * @param currentOverlayElement
     * @param selection
     * @param overlay
     * @param labels
     */
    public abstract void onMouseReleasedMid(ImgViewerMouseEvent e, O currentOverlayElement,
                                            PlaneSelectionEvent selection, Overlay<String> overlay, String... labels);

    /**
     * @param e
     * @param currentOverlayElement
     * @param selection
     * @param overlay
     * @param labels
     */
    public abstract void onMouseDraggedMid(ImgViewerMouseEvent e, O currentOverlayElement,
                                           PlaneSelectionEvent selection, Overlay<String> overlay, String... labels);

    public abstract void fireFocusLost(Overlay<String> overlay);

    /**
     * @param name
     * @param iconPath
     */
    public AnnotatorTool(final String name, final String iconPath) {
        m_name = name;
        m_iconPath = iconPath;
        m_stateChanged = false;
    }

    /**
     * @param e
     * @param selection
     * @param overlay
     * @param labels
     */
    public void onMouseDoubleClick(final ImgViewerMouseEvent e, final PlaneSelectionEvent selection,
                                   final Overlay<String> overlay, final String... labels) {
        if (!e.isInside()) {
            setCurrentOverlayElement(null, null);
            fireStateChanged();
        } else if (e.isLeftDown()) {
            onMouseDoubleClickLeft(e, m_currentOverlayElement, selection, overlay, labels);
        } else if (e.isRightDown()) {
            onMouseDoubleClickRight(e, m_currentOverlayElement, selection, overlay, labels);
        } else if (e.isMidDown()) {
            onMouseDoubleClickMid(e, m_currentOverlayElement, selection, overlay, labels);
        }

        tryToFireStateChanged(overlay);
    }

    /**
     * @param e
     * @param selection
     * @param overlay
     * @param labels
     */
    public void onMousePressed(final ImgViewerMouseEvent e, final PlaneSelectionEvent selection,
                               final Overlay<String> overlay, final String... labels) {
        m_dragPoint = selection.getPlanePos(e.getPosX(), e.getPosY());

        if (!e.isInside()) {
            setCurrentOverlayElement(null, null);
            fireStateChanged();
        } else if (e.isLeftDown()) {
            onMousePressedLeft(e, m_currentOverlayElement, selection, overlay, labels);
        } else if (e.isRightDown()) {
            onMousePressedRight(e, m_currentOverlayElement, selection, overlay, labels);
        } else if (e.isMidDown()) {
            onMousePressedMid(e, m_currentOverlayElement, selection, overlay, labels);
        }

        tryToFireStateChanged(overlay);
    }

    /**
     * @param e
     * @param selection
     * @param overlay
     * @param labels
     */
    public void onMouseReleased(final ImgViewerMouseEvent e, final PlaneSelectionEvent selection,
                                final Overlay<String> overlay, final String... labels) {

        if (!e.isInside()) {
            if ((m_currentOverlayElement != null)
                    && (m_currentOverlayElement.getStatus() != OverlayElementStatus.ACTIVE)) {
                m_currentOverlayElement.setStatus(OverlayElementStatus.ACTIVE);
                fireStateChanged();
            }
        } else if (e.isLeftDown()) {
            onMouseReleasedLeft(e, m_currentOverlayElement, selection, overlay, labels);
        } else if (e.isRightDown()) {
            onMouseReleasedRight(e, m_currentOverlayElement, selection, overlay, labels);
        } else if (e.isMidDown()) {
            onMouseReleasedMid(e, m_currentOverlayElement, selection, overlay, labels);
        }

        tryToFireStateChanged(overlay);

    }

    /**
     * @param e
     * @param selection
     * @param overlay
     * @param labels
     */
    public void onMouseDragged(final ImgViewerMouseEvent e, final PlaneSelectionEvent selection,
                               final Overlay<String> overlay, final String... labels) {
        if (!e.isInside()) {
            return;
        }

        if (e.isLeftDown()) {
            onMouseDraggedLeft(e, m_currentOverlayElement, selection, overlay, labels);
        } else if (e.isRightDown()) {
            onMouseDraggedRight(e, m_currentOverlayElement, selection, overlay, labels);
        } else if (e.isMidDown()) {
            onMouseDraggedMid(e, m_currentOverlayElement, selection, overlay, labels);
        }

        m_dragPoint = selection.getPlanePos(e.getPosX(), e.getPosY());

        tryToFireStateChanged(overlay);
    }

    /**
     * @param overlay
     */
    protected void tryToFireStateChanged(final Overlay<String> overlay) {
        if (m_stateChanged && (overlay != null)) {
            m_stateChanged = false;
            overlay.fireOverlayChanged();
        }
    }

    @SuppressWarnings("javadoc")
    public String getName() {
        return m_name;
    }

    /**
     * @return
     */
    public String getIconPath() {
        return m_iconPath;
    }

    @SuppressWarnings("javadoc")
    public final void setButtonIcon(final AbstractButton jb, final String path) {
        final URL icon =
                getClass().getClassLoader().getResource(getClass().getPackage().getName().replace('.', '/') + "/"
                                                                + path);
        jb.setHorizontalAlignment(SwingConstants.LEFT);
        if (icon != null) {
            jb.setIcon(new ImageIcon(icon));
        }
    }

    /**
     * @param status
     * @param os
     * @return
     */
    // Helpers
    protected boolean setCurrentOverlayElement(final OverlayElementStatus status, final O os) {
        if ((((m_currentOverlayElement == null) && (os == null)) || ((m_currentOverlayElement == os) && (m_currentOverlayElement
                .getStatus() == status)))) {
            return false;
        }

        if (os == null) {
            m_currentOverlayElement.setStatus(OverlayElementStatus.IDLE);
            m_currentOverlayElement = null;
        } else {
            os.setStatus(status == null ? os.getStatus() : status);

            if (m_currentOverlayElement != null) {
                m_currentOverlayElement.setStatus(OverlayElementStatus.IDLE);
            }
            m_currentOverlayElement = os;
        }

        return true;

    }

    @SuppressWarnings("javadoc")
    protected void fireStateChanged() {
        m_stateChanged = true;
    }

    @SuppressWarnings("javadoc")
    protected final long[] getDragPoint() {
        return m_dragPoint;
    }

    @SuppressWarnings("javadoc")
    protected final void setDragPoint(final long[] dragPoint) {
        m_dragPoint = dragPoint.clone();
    }

    @SuppressWarnings("javadoc")
    public void setLabelsCurrentElements(final Overlay<String> overlay, final String[] selectedLabels) {

        if (m_currentOverlayElement != null) {
            m_currentOverlayElement.setLabels(selectedLabels);
            overlay.fireOverlayChanged();
        }
    }

}
