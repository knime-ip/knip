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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.knime.knip.core.ui.imgviewer.ColorDispenser;

/**
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author muethingc
 */
public class HistogramPainter implements MouseMotionListener {

    private static final Color DEFAULT = Color.BLACK;

    private static final Color HILITE = Color.orange;

    private static final Color HILITE_BACKGROUND = Color.lightGray;

    private static final Color BACKGROUND = Color.white;

    private static final int MIN_HEIGHT = 50;

    private class Bin {
        private final Color m_colorSelection = ColorDispenser.INSTANCE.next();

        private final Color m_colorDefault;

        private final Rectangle m_rectangleLog;

        private final Rectangle m_rectangleLinear;

        private final Rectangle m_rectangleSelection;

        private final long m_count;

        private final double[] m_values;

        private Color m_color;

        public Bin(final Rectangle recLog, final Rectangle recLin, final Rectangle recSelection, final long c,
                   final double[] val, final Color col) {
            m_values = val.clone();
            m_rectangleLog = recLog;
            m_rectangleLinear = recLin;
            m_rectangleSelection = recSelection;
            m_count = c;
            m_color = col;
            m_colorDefault = col;
        }

        public Bin(final Rectangle recLog, final Rectangle recLin, final Rectangle recSelection, final long c,
                   final double[] val) {
            this(recLog, recLin, recSelection, c, val, DEFAULT);
        }

        public void hilite() {
            m_color = HILITE;
        }

        public void unhilite() {
            m_color = m_colorDefault;
        }
    }

    /**
     * The scale the histogram is drawn with.
     */
    public enum Scale {
        @SuppressWarnings("javadoc")
        LOG("log"), @SuppressWarnings("javadoc")
        LINEAR("linear");

        private final String m_name;

        private Scale(final String name) {
            m_name = name;
        }

        @Override
        public String toString() {
            return m_name;
        }
    }

    // used for determining how to draw the data
    private long m_max = Integer.MIN_VALUE;

    //
    private long m_min = Integer.MAX_VALUE;

    //
    private double m_maxLog;

    //
    private double m_minLog;

    // the data to draw
    private Histogram m_histogram = null;

    // Stores the current scale to draw in
    private Scale m_scale;

    // the extra info to paint close to the cursor
    private String m_msg = "";

    // list of listeners
    private final EventListenerList m_listener = new EventListenerList();

    // the area being drawn onto
    private Rectangle m_paintArea = new Rectangle(10, 10);

    // all bins we are drawing
    private final List<Bin> m_bins = new ArrayList<Bin>();

    // the backbuffer for selection detection
    private BufferedImage m_backBuf = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);

    /**
     * Set up a new histogram that will be painted using log scale.
     * 
     * @param hist the hist to draw
     */
    public HistogramPainter(final Histogram hist) {
        this(hist, Scale.LOG);
    }

    /**
     * Sets up a new histogram that will initally not draw anthing and be set to log scale.
     */
    public HistogramPainter() {
        this(null);
    }

    /**
     * Set up a new histogram that will be drawn into with the given scale.
     * 
     * @param hist the hist to draw
     * @param scale the scale to use
     */
    public HistogramPainter(final Histogram hist, final Scale scale) {
        setHistogram(hist);
        m_scale = scale;
    }

    /**
     * Set the histogram to display a new data set.
     * 
     * @param hist
     */
    public final void setHistogram(final Histogram hist) {
        m_histogram = hist;

        if (m_histogram == null) {
            return;
        }

        findMinMax();

        calcBins();
    }

    private final void calcBins() {
        assert m_paintArea != null;

        m_bins.clear();

        if (m_histogram == null) {
            return;
        }

        final int height = m_paintArea.height;

        final float pixelWidth = calcPixelSize(m_paintArea.width, m_histogram.size());
        float pos = 0f;

        for (int i = 0; i < m_histogram.size(); i++) {
            final int left = (int)pos;
            pos += pixelWidth;
            final int right = (int)pos;

            final int[] h = calculateBarDrawHeight(m_histogram.count(i), height);

            final Rectangle log = new Rectangle(left, height - h[0], right - left, h[0]);

            final Rectangle linear = new Rectangle(left, height - h[1], right - left, h[1]);

            final Rectangle selection = new Rectangle(left, 0, right - left, height);

            m_bins.add(new Bin(log, linear, selection, m_histogram.count(i), m_histogram.values(i)));
        }
    }

    private final void findMinMax() {
        assert m_histogram != null;

        m_max = Long.MIN_VALUE;
        m_min = Long.MAX_VALUE;

        for (final Long v : m_histogram) {
            m_max = v > m_max ? v : m_max;
            m_min = v < m_min ? v : m_min;
        }

        m_maxLog = Math.log(m_max);
        m_minLog = Math.log(m_min);

        if (m_minLog == Double.NEGATIVE_INFINITY) {
            m_minLog = 0;
        }
    }

    private float calcPixelSize(final int width, final int bins) {
        return (float)width / (float)(bins);
    }

    /**
     * Paint this histogram using the given Graphics2D object.
     * 
     * @see javax.swing.JComponent#paintComponent(Graphics)
     * @param g2 the Graphics2D object to use for drawing
     */
    public final void paint(final Graphics2D g2) {

        final Rectangle newArea = g2.getClipBounds();

        if (!newArea.equals(m_paintArea)) {
            m_paintArea = newArea;
            calcBins();

            m_backBuf = new BufferedImage(m_paintArea.width, m_paintArea.height, BufferedImage.TYPE_INT_ARGB);
        }

        // paint the background
        g2.setColor(BACKGROUND);
        g2.fill(m_paintArea);

        if (m_histogram != null) {
            paintHistogram(g2, false);
        } else {
            g2.setFont(new Font(g2.getFont().getFontName(), Font.BOLD, 20));
            g2.drawString("No data present", 20, m_paintArea.height / 2);
        }

        repaintBackBuffer();
    }

    private void repaintBackBuffer() {
        assert m_backBuf.getHeight() == m_paintArea.height;
        assert m_backBuf.getWidth() == m_paintArea.width;

        final Graphics2D g2 = m_backBuf.createGraphics();
        g2.setColor(ColorDispenser.BACKGROUND_COLOR);
        g2.fill(m_paintArea);

        paintHistogram(g2, true);
    }

    private void paintHistogram(final Graphics2D g2, final boolean selection) {

        for (final Bin bin : m_bins) {
            if (selection) {
                g2.setColor(bin.m_colorSelection);
                g2.fill(bin.m_rectangleSelection);
            } else {
                if (bin.m_color.equals(HILITE)) {
                    final GradientPaint gp =
                            new GradientPaint(0, m_paintArea.height, HILITE_BACKGROUND, 0, 0, BACKGROUND);
                    g2.setPaint(gp);
                    g2.fill(bin.m_rectangleSelection);
                }

                g2.setPaint(bin.m_color);

                if (m_scale == Scale.LINEAR) {
                    g2.fill(bin.m_rectangleLinear);
                } else {
                    g2.fill(bin.m_rectangleLog);
                }

            }
        }
    }

    /**
     * Calculate the height to which the bar with the given value should be drawn.
     * 
     * @param val the value
     * @return the height to draw to in int
     */
    private int[] calculateBarDrawHeight(final double val, final int height) {

        final int[] res = new int[2];

        final double max = m_maxLog - m_minLog;
        double log = Math.log(val);

        if (log == Double.NEGATIVE_INFINITY) {
            log = 0;
        }

        // Normalize to log scale
        final double l = (log - m_minLog) / max;
        res[0] = (int)(l * height);

        final double frac = val / m_max;
        res[1] = (int)(frac * height);

        return res;
    }

    /**
     * Sets the scale used to display the histogram.
     * 
     * Note: To acutally see the changes, the calling class has to issue a repaint() itself.
     * 
     * @param scale the scale
     */
    public final void setScale(final Scale scale) {
        m_scale = scale;
    }

    @Override
    public void mouseDragged(final MouseEvent arg0) {
        // ignore
    }

    @Override
    public void mouseMoved(final MouseEvent event) {

        final Color c = new Color(m_backBuf.getRGB(event.getX(), event.getY()));

        final StringBuilder sb = new StringBuilder();
        final Formatter formatter = new Formatter(sb);

        for (final Bin bin : m_bins) {
            if (bin.m_colorSelection.equals(c)) {
                formatter.format("Count %10d from %15.5g to %15.5g", bin.m_count, bin.m_values[0], bin.m_values[1]);
                bin.hilite();
            } else {
                bin.unhilite();
            }
        }

        m_msg = sb.toString();

        fireChangeEvent();
    }

    public final String getMessage() {
        return m_msg;
    }

    private void fireChangeEvent() {
        for (final ChangeListener l : m_listener.getListeners(ChangeListener.class)) {
            l.stateChanged(new ChangeEvent(this));
        }
    }

    public final void addChangeListener(final ChangeListener l) {
        m_listener.add(ChangeListener.class, l);
    }

    public final void removeChangeListener(final ChangeListener l) {
        m_listener.remove(ChangeListener.class, l);
    }

    public final Dimension getMinimumSize() {

        int width;
        if (m_histogram == null) {
            width = 100;
        } else {
            width = m_histogram.size();
        }
        return new Dimension(width, MIN_HEIGHT);
    }

    public final Dimension getPreferredSize() {
        return getMinimumSize();
    }
}
