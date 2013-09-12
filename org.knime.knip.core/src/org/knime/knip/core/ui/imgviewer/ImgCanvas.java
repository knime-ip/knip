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
package org.knime.knip.core.ui.imgviewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.type.Type;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.events.AWTImageChgEvent;
import org.knime.knip.core.ui.imgviewer.events.CalibrationUpdateEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerMouseDraggedEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerMouseMovedEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerMousePressedEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerMouseReleasedEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerRectChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerTextMessageChgEvent;
import org.knime.knip.core.ui.imgviewer.events.MinimapOffsetChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewZoomfactorChgEvent;
import org.knime.knip.core.ui.imgviewer.panels.MinimapPanel;

/**
 * 
 * Panel to draw a BufferedImage.
 * 
 * Propagates {@link ImgViewerRectChgEvent}.
 * 
 * @param <T>
 * @param <I>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgCanvas<T extends Type<T>, I extends IterableInterval<T> & RandomAccessible<T>> extends ViewerComponent {

    private static BufferedImage TEXTMSGIMG = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);

    private static final long serialVersionUID = 1L;

    private final JPanel m_imageCanvas;

    private final JScrollPane m_imageScrollPane;

    protected BufferedImage m_image;

    /** the current zoom factor */
    private double m_zoomFactor;

    /**
     * current calibration scale factors for x and y dimension (in the displayed image)
     */
    private double[] m_scaleFactors;

    /**
     * current combined factor for x and y dimension (in the displayed image)
     */
    private double[] m_factors;

    /** old combined factor for x and y dimension (in the displayed image) */
    private double[] m_oldFactors;

    private boolean m_keyDraggingEnabled;

    private Point m_dragPoint;

    private Rectangle m_dragRect;

    private boolean m_horScrollbarMoved = false;

    private boolean m_verScrollbarMoved = false;

    private boolean m_blockMouseEvents;

    private boolean m_blockPanning = false;

    protected Rectangle m_currentRectangle;

    protected StringBuffer m_labelBuffer = new StringBuffer();

    protected EventService m_eventService;

    public ImgCanvas() {
        this("Image", false);
    }

    /**
     *
     */
    public ImgCanvas(final String name, final boolean isImageHidden) {
        super(name, isImageHidden);

        m_currentRectangle = new Rectangle();
        m_oldFactors = new double[]{1.0d, 1.0d};
        m_factors = new double[]{1.0d, 1.0d};
        m_scaleFactors = new double[]{1.0d, 1.0d};
        m_zoomFactor = 1.0d;

        m_imageCanvas = new JPanel() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void paint(final Graphics g) {
                super.paint(g);
                if (m_image == null) {
                    return;
                }
                g.drawImage(m_image, 0, 0, (int)(m_image.getWidth(null) * m_factors[0]),
                            (int)(m_image.getHeight(null) * m_factors[1]), null);
            }
        };

        m_imageCanvas.setBackground(Color.DARK_GRAY);
        // TODO discuss global key listener

        m_imageCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                m_dragPoint = e.getLocationOnScreen();
                m_dragRect = m_imageCanvas.getVisibleRect();
                fireImageCoordMousePressed(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                fireImageCoordMouseReleased(e);
            }

        });
        m_imageCanvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                if (m_keyDraggingEnabled || ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 1024)) {
                    if (!m_blockPanning) {
                        m_currentRectangle.setBounds(m_dragRect);
                        m_currentRectangle.translate((m_dragPoint.x - e.getXOnScreen()),
                                                     (m_dragPoint.y - e.getYOnScreen()));
                        m_imageCanvas.scrollRectToVisible(m_currentRectangle);
                    }
                }
                fireImageCoordMouseDragged(e);
            }

            @Override
            public void mouseMoved(final MouseEvent e) {
                fireImageCoordMouseMoved(e);
            }
        });
        m_imageCanvas.addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(final MouseWheelEvent e) {
                if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                    int direction = -1;
                    if (e.getWheelRotation() < 0) {
                        direction = 1;
                    }

                    final int oldValue = (int)(m_zoomFactor * 100.0);

                    final int change = (int)Math.sqrt(oldValue) * direction;
                    int newValue = oldValue + change;

                    if (newValue < MinimapPanel.ZOOM_MIN) {
                        newValue = MinimapPanel.ZOOM_MIN;
                    } else if (newValue > MinimapPanel.ZOOM_MAX) {
                        newValue = MinimapPanel.ZOOM_MAX;
                    }

                    m_eventService.publish(new ViewZoomfactorChgEvent(newValue / 100d));

                }
            }

        });
        m_imageScrollPane = new JScrollPane(m_imageCanvas);

        m_imageScrollPane.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(final AdjustmentEvent e) {
                if (m_verScrollbarMoved) {
                    m_verScrollbarMoved = false;
                    return;
                }
                m_horScrollbarMoved = true;
                handleScrollbarEvent();
            }
        });

        m_imageScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(final AdjustmentEvent e) {
                if (m_horScrollbarMoved) {
                    m_horScrollbarMoved = false;
                    return;
                }
                m_verScrollbarMoved = true;
                handleScrollbarEvent();
            }
        });

        setLayout(new GridBagLayout());
        final GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;

        add(m_imageScrollPane, gc);

        m_zoomFactor = 1;
        updateImageCanvas(false);

    }

    protected void blockPanning(final boolean block) {
        m_blockPanning = block;
    }

    private void handleScrollbarEvent() {
        if ((m_currentRectangle == null) || !m_currentRectangle.equals(m_imageCanvas.getVisibleRect())) {
            m_eventService.publish(new ImgViewerRectChgEvent(getVisibleImageRect()));
        }

    }

    private boolean isMouseEventBlocked() {
        return m_blockMouseEvents || (m_image == null);
    }

    /**
     * Returns the visible bounding box in the image coordinate space.
     * 
     * @return the visible bounding box.
     */
    public Rectangle getVisibleImageRect() {
        m_currentRectangle = m_imageCanvas.getVisibleRect();
        m_currentRectangle.x = (int)(m_currentRectangle.x / m_factors[0]);
        m_currentRectangle.y = (int)(m_currentRectangle.y / m_factors[1]);
        m_currentRectangle.width = (int)(m_currentRectangle.width / m_factors[0]);
        m_currentRectangle.height = (int)(m_currentRectangle.height / m_factors[1]);
        return m_currentRectangle;
    }

    private void fireImageCoordMousePressed(final MouseEvent e) {
        if (!isMouseEventBlocked()) {
            m_eventService
                    .publish(new ImgViewerMousePressedEvent(e, m_factors, m_image.getWidth(), m_image.getHeight()));
        }

    }

    private void fireImageCoordMouseReleased(final MouseEvent e) {
        if (!isMouseEventBlocked()) {
            m_eventService.publish(
            // TODO CHANGE HERE TO FACTORS
                    new ImgViewerMouseReleasedEvent(e, m_factors, m_image.getWidth(), m_image.getHeight()));
        }

    }

    private void fireImageCoordMouseDragged(final MouseEvent e) {
        if (!isMouseEventBlocked()) {
            m_eventService.publish(
            // TODO CHANGE HERE TO FACTORS
                    new ImgViewerMouseDraggedEvent(e, m_factors, m_image.getWidth(), m_image.getHeight()));
        }

    }

    private void fireImageCoordMouseMoved(final MouseEvent e) {
        if (!isMouseEventBlocked()) {
            // TODO CHANGE HERE TO FACTORS
            m_eventService.publish(new ImgViewerMouseMovedEvent(e, m_factors, m_image.getWidth(), m_image.getHeight()));
        }

    }

    @EventListener
    public void onZoomFactorChanged(final ViewZoomfactorChgEvent zoomEvent) {
        m_zoomFactor = zoomEvent.getZoomFactor();
        updateImageCanvas(false);
    }

    @EventListener
    public void onCalibrationUpdateEvent(final CalibrationUpdateEvent e) {
        m_scaleFactors =
                new double[]{e.getScaleFactors()[e.getSelectedDims()[0]], e.getScaleFactors()[e.getSelectedDims()[1]],};
        updateImageCanvas(false);
    }

    /**
     * Scrolls the image so the rectangle gets visible.
     * 
     * @param rect
     */
    @EventListener
    public void onMinimapOffsetChanged(final MinimapOffsetChgEvent e) {
        m_currentRectangle = m_imageCanvas.getVisibleRect();
        m_currentRectangle.x = (int)(e.getOffest()[0] * m_factors[0]);
        m_currentRectangle.y = (int)(e.getOffest()[1] * m_factors[1]);
        m_imageCanvas.scrollRectToVisible(m_currentRectangle);
        updateImageCanvas(false);
    }

    @EventListener
    public void onBufferedImageChanged(final AWTImageChgEvent e) {
        m_image = (BufferedImage)e.getImage();
        m_blockMouseEvents = false;

        updateImageCanvas(true);
    }

    public void updateImageCanvas(final boolean enforceRecalculation) {
        if (m_image == null) {
            return;
        }

        // calculate the new combined factor
        m_factors[0] = m_scaleFactors[0] * m_zoomFactor;
        m_factors[1] = m_scaleFactors[1] * m_zoomFactor;

        if (enforceRecalculation || (m_oldFactors[0] != m_factors[0]) || (m_oldFactors[1] != m_factors[1])) {

            // get old center of the image

            final Rectangle rect = m_imageCanvas.getVisibleRect();
            final double imgCenterX = rect.getCenterX() / m_oldFactors[0];
            final double imgCenterY = rect.getCenterY() / m_oldFactors[1];

            // enlarge canvas
            final Dimension d =
                    new Dimension((int)(m_image.getWidth(null) * m_factors[0]),
                            (int)(m_image.getHeight(null) * m_factors[1]));
            m_imageCanvas.setSize(d);
            m_imageCanvas.setPreferredSize(d);

            final double xCorrect = getVisibleImageRect().width / 2.0;
            final double yCorrect = getVisibleImageRect().height / 2.0;

            // apply old center
            m_imageScrollPane.getViewport().setViewPosition(new Point((int)(((imgCenterX - xCorrect) * m_factors[0])),
                                                                    (int)(((imgCenterY - yCorrect) * m_factors[1]))));

            m_oldFactors = m_factors.clone();

        }
        m_imageScrollPane.validate();
        m_imageScrollPane.repaint();
    }

    /**
     * An image with the message.
     * 
     * @param message
     */
    @EventListener
    public void onTextMessageChanged(final ImgViewerTextMessageChgEvent e) {

        final Graphics2D g = (Graphics2D)m_imageCanvas.getGraphics();
        if (g != null) {
            g.setBackground(Color.GRAY.darker());
            g.clearRect(0, 0, m_imageCanvas.getWidth(), m_imageCanvas.getHeight());
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            final int h = g.getFont().getSize();
            int y = h;
            g.setColor(Color.YELLOW);
            for (final String s : e.getMessage().split("\n")) {
                g.drawString(s, h, y);
                y += h;
            }

            m_blockMouseEvents = true;
            m_image = TEXTMSGIMG;
            updateImageCanvas(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position getPosition() {
        return Position.CENTER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
        eventService.subscribe(this);
    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
    }

    /**
     * @param event
     */
    @EventListener
    public void reset(final ViewClosedEvent event) {
        m_image = null;
    }

}
