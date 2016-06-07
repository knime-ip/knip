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
package org.knime.knip.core.ui.imgviewer.panels.infobars;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerMouseEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerMouseMovedEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerRectChgEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;

import net.imagej.axis.TypedAxis;
import net.imagej.space.TypedSpace;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.Type;
import net.imglib2.view.Views;

/**
 *
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class ViewInfoPanel<T extends Type<T>> extends ViewerComponent {

    private static final long serialVersionUID = 1L;

    private StringBuffer m_infoBuffer;

    private JLabel m_mouseInfoLabel;

    private JLabel m_imageInfoLabel;

    private JLabel m_rectLabel;

    private JLabel m_typeLabel;

    private TypedSpace<? extends TypedAxis> m_imgAxes;

    private long[] m_pos;

    private ImgViewerMouseEvent m_currentCoords;

    private long[] m_dims;

    private final JPanel m_mouseInfoPanel;

    private final JPanel m_imageInfoPanel;

    private final JPanel m_rectInfoPanel;

    private final JPanel m_typeInfoPanel;

    private IterableInterval<T> m_iterable;

    protected RandomAccessibleInterval<T> m_randomAccessible;

    protected RandomAccess<T> m_rndAccess;

    protected PlaneSelectionEvent m_sel;

    protected Rectangle m_visibleRect;

    public ViewInfoPanel() {
        super("Image Info", false);
        m_infoBuffer = new StringBuffer();

        m_mouseInfoLabel = new JLabel();
        m_imageInfoLabel = new JLabel();
        m_rectLabel = new JLabel();
        m_typeLabel = new JLabel();

        m_mouseInfoPanel = new JPanel(new BorderLayout());
        m_imageInfoPanel = new JPanel(new BorderLayout());
        m_rectInfoPanel = new JPanel(new BorderLayout());
        m_typeInfoPanel = new JPanel(new BorderLayout());

        m_mouseInfoPanel.add(m_mouseInfoLabel, BorderLayout.LINE_START);
        m_imageInfoPanel.add(m_imageInfoLabel, BorderLayout.LINE_START);
        m_rectInfoPanel.add(m_rectLabel, BorderLayout.LINE_START);
        m_typeInfoPanel.add(m_typeLabel, BorderLayout.LINE_START);

        setLayout(new GridLayout(2, 2));
        add(m_mouseInfoLabel);
        add(m_imageInfoLabel);
        add(m_rectLabel);
        add(m_typeInfoPanel);
    }

    /**
     * @param buffer
     * @param img
     * @param axes
     * @param rndAccess
     * @param coords
     * @return
     */
    protected abstract String updateMouseLabel(StringBuffer buffer, Interval img, TypedSpace<? extends TypedAxis> axes,
                                               RandomAccess<T> rndAccess, long[] coords);

    /**
     * @param buffer
     * @param img
     * @param rndAccess
     * @param imgName
     * @return
     */
    protected abstract String updateImageLabel(StringBuffer buffer, Interval img, RandomAccess<T> rndAccess,
                                               String imgName);

    @EventListener
    public void onClose(final ViewClosedEvent ev) {
        m_randomAccessible = null;
        m_dims = null;
        m_mouseInfoLabel = null;
        m_imageInfoLabel = null;
        m_imgAxes = null;
        m_infoBuffer = null;
        m_pos = null;
        m_rndAccess = null;
        m_iterable = null;

    }

    /**
     * @param name
     */
    @EventListener
    public void onImgChanged(final IntervalWithMetadataChgEvent<T, ?> e) {
        m_randomAccessible = Views.zeroMin(e.getRandomAccessibleInterval());
        m_iterable = Views.iterable(m_randomAccessible);

        m_dims = new long[m_randomAccessible.numDimensions()];

        m_randomAccessible.dimensions(m_dims);
        m_imgAxes = e.getTypedSpace();

        final T val = m_iterable.firstElement().createVariable();
        m_rndAccess = Views.extendValue(m_randomAccessible, val).randomAccess();

        if ((m_sel == null) || (m_sel.numDimensions() != e.getRandomAccessibleInterval().numDimensions())) {
            onPlaneSelectionChanged(new PlaneSelectionEvent(0, 1, new long[e.getRandomAccessibleInterval()
                    .numDimensions()]));
        }

        //TODO instead of testing for null to indicate that something went wrong make sure the method is never
        //called with wrong parameters
        String text = updateImageLabel(m_infoBuffer, m_randomAccessible, m_rndAccess, e.getName().getName());
        if (text != null) {
            m_imageInfoLabel.setText(text);
        }
        m_typeLabel.setText("Type: " + m_rndAccess.get().getClass().getSimpleName());

    }

    @EventListener
    public void onPlaneSelectionChanged(final PlaneSelectionEvent e) {
        m_sel = e;
        m_pos = m_sel.getPlanePos().clone();

        if ((m_currentCoords == null)
                || !m_currentCoords.isInsideImgView(m_dims[m_sel.getPlaneDimIndex1()],
                                                    m_dims[m_sel.getPlaneDimIndex2()])) {
            m_pos[m_sel.getPlaneDimIndex1()] = -1;
            m_pos[m_sel.getPlaneDimIndex2()] = -1;
        }

        //TODO instead of testing for null to indicate that something went wrong make sure the method is never
        //called with wrong parameters
        String text = updateMouseLabel(m_infoBuffer, m_randomAccessible, m_imgAxes, m_rndAccess, m_pos);
        if (text != null) {
            m_mouseInfoLabel.setText(text);
        }

        m_infoBuffer.setLength(0);
    }

    @EventListener
    public void onMouseMoved(final ImgViewerMouseMovedEvent e) {
        m_currentCoords = e;
        if (e.isInsideImgView(m_dims[m_sel.getPlaneDimIndex1()], m_dims[m_sel.getPlaneDimIndex2()])) {
            m_pos = m_sel.getPlanePos(m_currentCoords.getPosX(), m_currentCoords.getPosY());

            //TODO instead of testing for null to indicate that something went wrong make sure the method is never
            //called with wrong parameters
            String text = updateMouseLabel(m_infoBuffer, m_randomAccessible, m_imgAxes, m_rndAccess, m_pos);
            if (text != null) {
                m_mouseInfoLabel.setText(text);
            }

            m_infoBuffer.setLength(0);
        }
    }

    @EventListener
    public void onRectChanged(final ImgViewerRectChgEvent e) {
        m_visibleRect = e.getRectangle();
        m_rectLabel.setText(updateZoomLabel());
    }

    protected String updateZoomLabel() {
        if(m_visibleRect == null || m_pos == null) {
            return "";
        }
        if(m_visibleRect.getWidth() <= 0) {
            return "";
        }
        String res = "";
        String dims = "";
        long[] maxSize = new long[2];
        res += "Visible Rectangle[";

        int index = m_sel.getPlaneDimIndex1();
        dims += (m_imgAxes != null ? m_imgAxes.axis(index).type().getLabel() : "");
        maxSize[0] = m_randomAccessible.dimension(index);
        index = m_sel.getPlaneDimIndex2();
        dims += (m_imgAxes != null ? m_imgAxes.axis(index).type().getLabel() : "");
        maxSize[1] = m_randomAccessible.dimension(index);

        res = res + dims + "]: ";
        res +=
                "(" + (m_visibleRect.x+1) + "," + (m_visibleRect.y+1) + ") - (" + (int)(Math.min((m_visibleRect.x +1 + m_visibleRect.getWidth()), maxSize[0]))
                        + ", " + (int)(Math.min((m_visibleRect.y +1 + m_visibleRect.getHeight()), maxSize[1])) + ")";
        return res;
    }

    @Override
    public Position getPosition() {
        return Position.INFO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {
        eventService.subscribe(this);
    }

    /**
     * {@inheritDoc}
     */
    @EventListener
    public void reset(final ViewClosedEvent e) {
        m_iterable = null;
    }

    /**
     * sets the mouse and image info labels. This method is intended to be used if a subclass reacts to additional
     * events ... that should change the labels.
     *
     * @param mouseInfoText
     * @param imageInfoText
     */
    protected void manualTextUpdate(final String mouseInfoText, final String imageInfoText) {
        m_mouseInfoLabel.setText(mouseInfoText);
        m_imageInfoLabel.setText(imageInfoText);
    }

}
