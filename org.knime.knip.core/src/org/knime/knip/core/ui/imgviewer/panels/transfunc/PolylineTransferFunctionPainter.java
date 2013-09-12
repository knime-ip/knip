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
package org.knime.knip.core.ui.imgviewer.panels.transfunc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.EventListenerList;

import org.knime.knip.core.ui.imgviewer.ColorDispenser;
import org.knime.knip.core.ui.imgviewer.InputUtil;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author muethingc
 */
public class PolylineTransferFunctionPainter implements TransferFunctionPainter {

    /**
     * Simple class to use if nothing is selected, so we don't need to null test on m_selected all the time.
     */
    private class AlwaysUnequal {

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(final Object o) {
            return false;
        }
    }

    /**
     * Simple class that represents a line segment.
     */
    private class LineSegment {
        private final PolylineTransferFunction.Point m_p0;

        private final PolylineTransferFunction.Point m_p1;

        public LineSegment(final PolylineTransferFunction.Point point0, final PolylineTransferFunction.Point point1) {
            m_p0 = point0;
            m_p1 = point1;
        }

        @Override
        public int hashCode() {
            final int hash = m_p0.hashCode();
            return (hash * 31) + m_p1.hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof LineSegment)) {
                return false;
            } else {
                final LineSegment l = (LineSegment)o;
                boolean eq = true;

                eq = m_p0.equals(l.m_p0) ? eq & true : eq & false;
                eq = m_p1.equals(l.m_p1) ? eq & true : eq & false;

                return eq;
            }
        }
    }

    private static final int MOVE_HORIZONTAL = MouseEvent.SHIFT_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;

    private static final int MOVE_VERTICAL = InputUtil.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;

    private static final int ADD_POINT = MouseEvent.BUTTON1_DOWN_MASK;

    /* Some values that control how the lines look in the end */
    private final int m_pointSIZE = 10;

    private final int m_lineSIZE = 5;

    private final int m_selectionThickness = 2;

    private final EventListenerList m_listener = new EventListenerList();

    /* Stuff used for doing the backbuffer selection */
    private final ColorDispenser m_dispenser = ColorDispenser.INSTANCE;

    private final Map<Object, Color> m_backBufColors = new HashMap<Object, Color>();

    private final Map<Color, Object> m_colorToObject = new HashMap<Color, Object>();

    private final PolylineTransferFunction m_func;

    /* the area we are painting on */
    private Rectangle m_paintArea = new Rectangle(10, 10);

    /* the color we should paint in */
    private final Color m_color;

    /* some static colors that are always the same */
    private static final Color COLOR_BORDOR = Color.darkGray;

    private static final Color COLOR_HILITE = Color.lightGray;

    /* The cursor we use when highlighted, moving and otherwise */
    private static final Cursor CURSOR_MOVE = new Cursor(Cursor.CROSSHAIR_CURSOR);

    private static final Cursor CURSOR_DEFAULT = new Cursor(Cursor.DEFAULT_CURSOR);

    /* stores the info what object is currently selected */
    private Object m_selected = new AlwaysUnequal();

    /* the last point that the mouse was over */
    private int m_oldX = 0;

    private int m_oldY = 0;

    public PolylineTransferFunctionPainter(final PolylineTransferFunction func, final Color color) {
        if (func == null) {
            throw new NullPointerException();
        }
        if (color == null) {
            throw new NullPointerException();
        }

        m_func = func;
        m_color = color;
    }

    @Override
    public void paint(final Graphics2D g2) {
        paint(g2, false);
    }

    @Override
    public void paintForSelection(final Graphics2D g2) {
        paint(g2, true);
    }

    private void paint(final Graphics2D g2, final boolean backBufSel) {
        assert g2 != null;

        final Rectangle area = g2.getClipBounds();

        if (area == null) {
            return;
        }

        m_paintArea = area;

        if (!backBufSel) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        // first paint the lines so that the points will be on top
        paintLines(g2, backBufSel);
        paintPoints(g2, backBufSel);
    }

    @Override
    public void mouseClicked(final MouseEvent event) {

        if ((m_selected instanceof PolylineTransferFunction.Point) && (event.getButton() == MouseEvent.BUTTON3)) {

            m_colorToObject.remove(m_backBufColors.get(m_selected));
            m_backBufColors.remove(m_selected);
            m_func.removePoint((PolylineTransferFunction.Point)m_selected);

            fireTransferFunctionChgEvent(false);
        }
    }

    @Override
    public void mousePressed(final MouseEvent event) {

        if ((m_selected instanceof LineSegment) && (event.getModifiersEx() == ADD_POINT)) {
            final double x = getXPointCoordinate(event.getX());
            final double y = getYPointCoordinate(event.getY());

            final PolylineTransferFunction.Point p = m_func.addPoint(x, y);
            m_selected = p;

            fireTransferFunctionChgEvent(false);
        }

        storeCoord(event);
    }

    private boolean moveLineSegmentHorizontal(final LineSegment line, final MouseEvent event) {

        final int x0 = (event.getX() - m_oldX) + getXPanelCoordinate(line.m_p0.getX());

        final int x1 = (event.getX() - m_oldX) + getXPanelCoordinate(line.m_p1.getX());

        // make sure the ramp cannot be changed when hitting the borders
        // so that the gradient can not be altered
        // also do not move if one of the points is fixed
        if ((x0 >= 0) && (x1 <= m_paintArea.getWidth()) && !line.m_p0.getFixed() && !line.m_p1.getFixed()) {
            moveControlPointX(line.m_p0, x0);
            moveControlPointX(line.m_p1, x1);
            return true;
        }

        return false;
    }

    private boolean moveLineSegmentVertical(final LineSegment line, final MouseEvent event) {

        final int y0 = (event.getY() - m_oldY) + getYPanelCoordinate(line.m_p0.getY());

        final int y1 = (event.getY() - m_oldY) + getYPanelCoordinate(line.m_p1.getY());

        // make sure the ramp cannot be changed when hitting the borders
        // so that the gradient can not be altered
        if ((y0 >= 0) && (y0 <= m_paintArea.getHeight()) && (y1 >= 0) && (y1 <= m_paintArea.getHeight())) {
            moveControlPointY(line.m_p0, y0);
            moveControlPointY(line.m_p1, y1);
            return true;
        }

        return false;
    }

    @Override
    public void mouseDragged(final MouseEvent event) {

        boolean emitEvent = false;

        if (m_selected instanceof LineSegment) {

            final LineSegment line = (LineSegment)m_selected;

            if (event.getModifiersEx() == MOVE_HORIZONTAL) {
                emitEvent = moveLineSegmentHorizontal(line, event);
            }

            if (event.getModifiersEx() == MOVE_VERTICAL) {
                emitEvent = moveLineSegmentVertical(line, event);
            }
        }

        if (m_selected instanceof PolylineTransferFunction.Point) {

            moveControlPoint((PolylineTransferFunction.Point)m_selected, event.getX(), event.getY());
            emitEvent = true;
        }

        storeCoord(event);

        if (emitEvent) {
            fireTransferFunctionChgEvent(true);
        }
    }

    @Override
    public void clearHilite() {
        m_selected = new AlwaysUnequal();
    }

    @Override
    public boolean checkForHilite(final Color color) {
        if (color == null) {
            throw new NullPointerException();
        }

        if (m_colorToObject.containsKey(color)) {
            m_selected = m_colorToObject.get(color);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public Cursor getCursor() {
        if (m_selected instanceof AlwaysUnequal) {
            return CURSOR_DEFAULT;
        } else {
            return CURSOR_MOVE;
        }
    }

    /**
     * Paint all lines of the transfer function.<br>
     * 
     * @param g2 the graphics object to use
     * @param backBufSel wheter to paint for backbuffer selection or not
     */
    private void paintLines(final Graphics2D g2, final boolean backBufSel) {
        assert m_func != null;
        assert m_selected != null;

        final List<PolylineTransferFunction.Point> list = m_func.getPoints();

        PolylineTransferFunction.Point p0 = null;
        PolylineTransferFunction.Point p1 = list.get(0);

        for (int i = 1; i < list.size(); i++) {
            p0 = p1;
            p1 = list.get(i);

            final int x0 = getXPanelCoordinate(p0.getX());
            final int y0 = getYPanelCoordinate(p0.getY());

            final int x1 = getXPanelCoordinate(p1.getX());
            final int y1 = getYPanelCoordinate(p1.getY());

            final LineSegment line = new LineSegment(p0, p1);
            Color c;

            if (backBufSel) {
                c = getBackBufColor(line);
            } else {
                c = m_selected.equals(line) ? COLOR_HILITE : COLOR_BORDOR;
            }

            // paint the outline
            g2.setStroke(new BasicStroke(m_lineSIZE + m_selectionThickness));
            g2.setColor(c);
            g2.drawLine(x0, y0, x1, y1);

            // paint the actual line
            if (!backBufSel) {
                g2.setStroke(new BasicStroke(m_lineSIZE));
                g2.setColor(m_color);
                g2.drawLine(x0, y0, x1, y1);
            }

        }
    }

    /**
     * Paint all control points of the transfer function.<br>
     * 
     * @param g2 the graphics object to use
     * @param backBufSel wheter to paint for backbuffer selection or not
     */
    private void paintPoints(final Graphics2D g2, final boolean backBufSel) {
        assert m_func != null;
        assert m_selected != null;

        // paint the points
        for (final PolylineTransferFunction.Point p : m_func.getPoints()) {

            Color outer;
            Color inner;

            if (backBufSel) {
                outer = getBackBufColor(p);
                inner = outer;
            } else {
                outer = m_selected == p ? COLOR_HILITE : COLOR_BORDOR;
                inner = m_color;
            }

            paintPoint(g2, p, outer, inner);
        }
    }

    /**
     * This method actually paints a point.<br>
     * 
     * @param g2 graphics2d
     * @param p the point to paint
     * @param outer color to use for the highlighting/border painting
     * @param inner color to use for the acutal point
     */
    private void paintPoint(final Graphics2D g2, final PolylineTransferFunction.Point p, final Color outer,
                            final Color inner) {

        // correct the frac to fit into the coordinate space of the
        // panel
        final int x = getXPanelCoordinate(p.getX());
        final int y = getYPanelCoordinate(p.getY());

        final int half = m_pointSIZE / 2;

        Shape shape;

        if (p.getFixed()) {
            shape = new Rectangle(x - half, y - half, m_pointSIZE, m_pointSIZE);
        } else {
            shape = new Ellipse2D.Float(x - half, y - half, m_pointSIZE, m_pointSIZE);
        }

        // first fill the shape
        g2.setColor(inner);
        g2.fill(shape);

        // then draw the outline
        g2.setStroke(new BasicStroke(m_selectionThickness));
        g2.setColor(outer);
        g2.draw(shape);
    }

    /**
     * Get the position of the point in the Panel.
     * 
     * @param frac the corresponding x Fraction
     */
    private int getXPanelCoordinate(final double frac) {
        return ((int)(frac * m_paintArea.getWidth()));
    }

    /**
     * Get the position of the point in the Panel.
     * 
     * @param frac the corresponding y Fraction
     */
    private int getYPanelCoordinate(final double frac) {
        return ((int)((1.0 - frac) * m_paintArea.height));
    }

    /**
     * Get the fractional position of the given point relative in the panel.
     * 
     * @param x the x value
     */
    private double getXPointCoordinate(final int x) {
        return x / m_paintArea.getWidth();
    }

    /**
     * Get the fractional position of the given point relative in the panel.
     * 
     * @param y the y value
     */
    private double getYPointCoordinate(final int y) {
        return 1.0 - (y / m_paintArea.getHeight());
    }

    private void storeCoord(final MouseEvent event) {
        m_oldX = event.getX();
        m_oldY = event.getY();
    }

    /**
     * Get the color for this object, and if none exists yet assign one.
     */
    private Color getBackBufColor(final Object object) {
        assert object != null;

        Color c = m_backBufColors.get(object);

        if (c == null) {
            c = m_dispenser.next();
            m_backBufColors.put(object, c);
            m_colorToObject.put(c, object);
        }

        return c;
    }

    /**
     * Move a control point to a new position.
     * 
     * @param point the point to move
     * @param x the x value
     * @param y the y value
     */
    private void moveControlPoint(final PolylineTransferFunction.Point point, final int x, final int y) {
        final double xp = getXPointCoordinate(x);
        final double yp = getYPointCoordinate(y);
        m_func.movePoint(point, xp, yp);
    }

    /**
     * Move a control point to a new x-position, but keep the y-position.
     * 
     * @param point the point to move
     * @param x the x value
     */
    private void moveControlPointX(final PolylineTransferFunction.Point point, final int x) {
        final double xp = getXPointCoordinate(x);
        final double yp = point.getY();
        m_func.movePoint(point, xp, yp);
    }

    /**
     * Move a control point to a new y-position, but keep the x-position.
     * 
     * @param point the point to move
     * @param y the y value
     */
    private void moveControlPointY(final PolylineTransferFunction.Point point, final int y) {
        final double xp = point.getX();
        final double yp = getYPointCoordinate(y);
        m_func.movePoint(point, xp, yp);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.knime.knip.core.ui.transfunc.TransferFunctionPainter#
     * addTransferFunctionChgListener
     * (org.knime.knip.core.ui.transfunc.TransferFunctionChgListener)
     */
    @Override
    public void addTransferFunctionChgListener(final TransferFunctionChgListener l) {
        m_listener.add(TransferFunctionChgListener.class, l);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.knime.knip.core.ui.transfunc.TransferFunctionPainter#
     * removeTransferFunctionChgListener
     * (org.knime.knip.core.ui.transfunc.TransferFunctionChgListener)
     */
    @Override
    public void removeTransferFunctionChgListener(final TransferFunctionChgListener l) {
        m_listener.remove(TransferFunctionChgListener.class, l);
    }

    private void fireTransferFunctionChgEvent(final boolean adjusting) {
        for (final TransferFunctionChgListener l : m_listener.getListeners(TransferFunctionChgListener.class)) {
            l.transferFunctionChg(new TransferFunctionChgEvent(this, m_func, adjusting));
        }
    }

    @Override
    public void mouseEntered(final MouseEvent e) {
        // just ignore
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        // just ignore
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        // just ignore
    }

    @Override
    public void mouseMoved(final MouseEvent arg0) {
        // just ignore
    }
}
