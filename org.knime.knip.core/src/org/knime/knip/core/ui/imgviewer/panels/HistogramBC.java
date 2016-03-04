/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2016
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
 * Created on Jan 4, 2016 by oole
 */
package org.knime.knip.core.ui.imgviewer.panels;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AnnotationChangeListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import net.imagej.widget.HistogramBundle;

/**
 * Calculates Slope over Histogram of given HistogramBundle. Inspired by IJ2 HistogramWidget
 *
 * @author <a href="mailto:ole.c.ostergaard@gmail.com">Ole Ostergaard</a>
 */
public class HistogramBC {

    private HistogramBundle bundle;

    private ChartPanel chartPanel;

    /**
     * @return chartPanel
     */
    public ChartPanel getChartPanel() {
        return chartPanel;
    }

    /**
     *
     * @param setBundle
     */
    public HistogramBC(final HistogramBundle setBundle) {
        bundle = setBundle;

        chartPanel = makeChartPanel(bundle);
        bundle.setHasChanges(false);
    }

    /**
     *
     * @param b
     * @return
     */
    private ChartPanel makeChartPanel(final HistogramBundle b) {
        final JFreeChart chart = getChart(null, b);
        final ChartPanel panel = new ChartPanel(chart);
        panel.setPopupMenu(null);
        panel.setDomainZoomable(false);
        panel.setRangeZoomable(false);
        final int xSize = b.getPreferredSizeX() / 2;
        final int ySize = b.getPreferredSizeY() / 2;
        panel.setPreferredSize(new java.awt.Dimension(xSize, ySize));
        panel.setMinimumSize(new java.awt.Dimension(xSize, ySize));
        return panel;
    }

    /**
     * Returns a JFreeChart containing data from the provided histogram.
     *
     * @param title
     * @param bund
     * @return
     */
    private JFreeChart getChart(final String title, final HistogramBundle bund) {
        List<XYSeries> series = new ArrayList<>();
        for (int h = 0; h < bund.getHistogramCount(); h++) {
            final XYSeries xys = new XYSeries("histo" + h);
            final long total = bund.getHistogram(h).getBinCount();
            for (long i = 0; i < total; i++) {
                xys.add(i, bund.getHistogram(h).frequency(i));
            }
            series.add(xys);
        }
        final JFreeChart chart = createChart(title, series);
        if (bund.getMinBin() != -1) {
            chart.getXYPlot().addDomainMarker(new ValueMarker(bund.getMinBin(), Color.black, new BasicStroke(1)));
        }
        if (bund.getMaxBin() != -1) {
            chart.getXYPlot().addDomainMarker(new ValueMarker(bund.getMaxBin(), Color.black, new BasicStroke(1)));
        }
        if (displaySlopeLine(bund)) {
            chart.getXYPlot().addAnnotation(slopeLine());
        }

        // set to knime gray
        chart.setBackgroundPaint(new Color(238, 238, 238));
        return chart;
    }

    private JFreeChart createChart(final String title, final List<XYSeries> series) {
        final XYSeriesCollection data = new XYSeriesCollection();
        for (XYSeries xys : series) {
            data.addSeries(xys);
        }
        final JFreeChart chart = ChartFactory.createXYBarChart(title, null, false, null, data, PlotOrientation.VERTICAL,
                                                               false, true, false);
        setTheme(chart);
        // chart.getXYPlot().setForegroundAlpha(0.50f);
        return chart;
    }

    private final void setTheme(final JFreeChart chart) {
        final XYPlot plot = (XYPlot)chart.getPlot();
        final XYBarRenderer r = (XYBarRenderer)plot.getRenderer();
        final StandardXYBarPainter bp = new StandardXYBarPainter();
        r.setBarPainter(bp);
        // set Bar Color
        r.setSeriesPaint(0, Color.gray);
        r.setSeriesOutlinePaint(0, Color.gray);
        r.setShadowVisible(false);
        r.setDrawBarOutline(true);
        setBackgroundDefault(chart);
        final NumberAxis rangeAxis = (NumberAxis)plot.getRangeAxis();

        // rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setTickLabelsVisible(false);
        rangeAxis.setTickMarksVisible(false);
        final NumberAxis domainAxis = (NumberAxis)plot.getDomainAxis();
        domainAxis.setTickLabelsVisible(false);
        domainAxis.setTickMarksVisible(false);
    }

    private final void setBackgroundDefault(final JFreeChart chart) {
        final BasicStroke gridStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
                new float[]{2.0f, 1.0f}, 0.0f);
        final XYPlot plot = (XYPlot)chart.getPlot();
        plot.setRangeGridlineStroke(gridStroke);
        plot.setDomainGridlineStroke(gridStroke);
        // Background of Histogram inside border
        //plot.setBackgroundPaint(new Color(235,235,235));
        plot.setBackgroundPaint(Color.white);
        // change from white to gray
        plot.setRangeGridlinePaint(Color.gray);
        plot.setDomainGridlinePaint(Color.gray);
        // set lines invisible
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setOutlineVisible(true);
        plot.getDomainAxis().setAxisLineVisible(false);
        plot.getRangeAxis().setAxisLineVisible(false);
        plot.getDomainAxis().setLabelPaint(Color.gray);
        plot.getRangeAxis().setLabelPaint(Color.gray);
        plot.getDomainAxis().setTickLabelPaint(Color.gray);
        plot.getRangeAxis().setTickLabelPaint(Color.gray);
        final TextTitle title = chart.getTitle();
        if (title != null) {
            title.setPaint(Color.black);
        }
    }

    private boolean displaySlopeLine(final HistogramBundle bund) {
        if (Double.isNaN(bund.getDataMin())) {
            return false;
        }
        if (Double.isNaN(bund.getDataMax())) {
            return false;
        }
        if (Double.isNaN(bund.getTheoreticalMin())) {
            return false;
        }
        if (Double.isNaN(bund.getTheoreticalMax())) {
            return false;
        }
        return true;
    }

    private XYAnnotation slopeLine() {
        return new XYAnnotation() {

            private double x1, y1, x2, y2;

            @Override
            public void removeChangeListener(final AnnotationChangeListener listener) {
                // ignore
            }

            @Override
            public void addChangeListener(final AnnotationChangeListener listener) {
                // ignore
            }

            @Override
            public void draw(final Graphics2D g2, final XYPlot plot, final Rectangle2D dataArea,
                             final ValueAxis domainAxis, final ValueAxis rangeAxis, final int rendererIndex,
                             final PlotRenderingInfo info) {
                calcLineCoords(dataArea);
                drawLine(g2);
            }

            private void drawLine(final Graphics2D g2) {
                final Color origColor = g2.getColor();
                g2.setColor(Color.black);
                g2.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
                g2.setColor(Color.lightGray);
                g2.drawLine((int)x1, 0,(int) x1, 192);
                g2.drawLine((int)x2, 0,(int) x2, 192);
                g2.setColor(origColor);
            }

            @SuppressWarnings("synthetic-access")
            private void calcLineCoords(final Rectangle2D rect) {
                // offset necessary since chart is not drawn on whole rectangle
                int offset = 12;
                final double x = rect.getMinX()+offset;
                final double y = rect.getMinY();
                final double w = rect.getWidth()-2*offset;
                final double h = rect.getHeight();
                final double min = bundle.getTheoreticalMin();
                final double max = bundle.getTheoreticalMax();
                final double defaultMin = bundle.getDataMin();
                final double defaultMax = bundle.getDataMax();
                final double scale = w / (defaultMax - defaultMin);
                double slope = 0.0;
                if (max != min) {
                    slope = h / (max - min);
                }
                if (min >= defaultMin) {
                    x1 = scale * (min - defaultMin);
                    y1 = h;
                } else {
                    x1 = 0;
                    if (max > min) {
                        y1 = h - ((defaultMin - min) * slope);
                    } else {
                        y1 = h;
                    }
                }
                if (max <= defaultMax) {
                    x2 = (scale * (max - defaultMin));
                    y2 = 0;
                } else {
                    x2 = w;
                    if (max > min) {
                        y2 = h - ((defaultMax - min) * slope);
                    } else {
                        y2 = 0;
                    }
                }
                x1 += x;
                x2 += x;
                y1 += y;
                y2 += y;
            }
        };
    }

    /**
     * Refreshes the JFreeChart based on newBundle.
     *
     * @param newBundle
     */
    public void refreshChart(final HistogramBundle newBundle) {
        final ChartPanel newChartPanel = makeChartPanel(newBundle);
        final JFreeChart chart = newChartPanel.getChart();
        chartPanel.setChart(chart);
        chartPanel.setDomainZoomable(false);
        chartPanel.setRangeZoomable(false);
    }
}