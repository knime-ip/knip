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
 * Created on 01.12.2015 by oole
 */
package org.knime.knip.core.ui.imgviewer.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.BrightnessContrastChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgAndLabelingChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.panels.providers.AWTImageProvider;

import net.imagej.ops.OpService;
import net.imagej.ops.Ops.Stats.MinMax;
import net.imagej.ops.cached.CachedOpEnvironment;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imagej.widget.HistogramBundle;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.histogram.BinMapper1d;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.img.Img;
import net.imglib2.ops.operation.real.unary.Normalize;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

/**
 * Adjust Brightness and Contrast of an image.
 *
 * @author <a href="mailto:ole.c.ostergaard@gmail.com">Ole Ostergaard</a>
 * @param <T>
 * @param <I>
 */
public class BrightnessContrastPanel<T extends RealType<T>, I extends Img<T>> extends ViewerComponent {

    /* slider values */
    private static final int SLIDER_MIN = 0;

    private static final int SLIDER_MAX = 100;

    private static final long serialVersionUID = 1L;

    /* sliders */
    private JSlider m_minimumSlider;

    private JSlider m_maximumSlider;

    private JSlider m_brightnessSlider;

    private JSlider m_contrastSlider;

    /* buttons */
    private JButton m_automaticSaturationButton;

    private JButton m_resetButton;

    /* checkbox */
    private JCheckBox m_autoSelect;

    private JCheckBox m_planeSelect;

    /* labels */
    private JLabel m_min;

    private JLabel m_max;

    private JLabel m_bright;

    private JLabel m_contrast;

    private JLabel w_minLabel;

    private JLabel w_maxLabel;

    /* initial min max values */
    private double initialMin;

    private double initialMax;

    /* data element min max values */
    private double elementMin;

    private double elementMax;

    /* min max used for normalization */
    private double normMin;

    private double normMax;

    /* working values */
    private double w_min;

    private double w_max;

    private double w_brightness;

    private double w_contrast;

    private double w_factor;

    /* drawing check */
    private boolean isDrawn = false;

    /* adjust check */
    private boolean isAdjusting = false;

    /* plane selected */
    private boolean planeSelected = false;

    /* auto selected */
    private boolean autoSelect = false;

    /* eventservice to publish events */
    private EventService m_eventService;

    /* image and selected plane */
    private RandomAccessibleInterval<T> img;

    /* image copy for the combined view */
    private RandomAccessibleInterval<T> unmodImg;

    private IterableInterval<T> imgIt;

    private RandomAccessibleInterval<T> planeSelection;

    private long[] planeSelectionPos;

    private int[] planeSelectionIndices;

    private int bitDepth;

    private T element;

    /* ops */
    OpService ops = KNIPGateway.ops();

    /* histogram */
    private HistogramBC m_histoWidget;

    private HistogramBundle m_bundle;

    private UnaryFunctionOp<IterableInterval, Pair> minMax;

    /**
     * Empty constructor prior to image update
     */
    public BrightnessContrastPanel() {
        super("", true);
        setLayout(new GridBagLayout());

        final CachedOpEnvironment cachedOpEnvironment = new CachedOpEnvironment(KNIPGateway.ops());
        minMax = Functions.unary(cachedOpEnvironment, MinMax.class, Pair.class, IterableInterval.class);
    }

    /**
     * Draw interface.
     *
     */
    public void draw() {
        // build panel
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 2;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(m_histoWidget.getChartPanel(), c);

        GridBagConstraints c1 = new GridBagConstraints();
        c1.gridy = 1;
        c1.gridwidth = 2;
        c1.fill = GridBagConstraints.HORIZONTAL;
        JPanel minMaxPanel = new JPanel();
        minMaxPanel.setLayout(new BoxLayout(minMaxPanel, BoxLayout.LINE_AXIS));
        w_minLabel = new JLabel(Integer.toString((int)w_min));
        minMaxPanel.add(w_minLabel);
        minMaxPanel.add(Box.createHorizontalGlue());
        w_maxLabel = new JLabel(Integer.toString((int)w_max));
        minMaxPanel.add(w_maxLabel);
        add(minMaxPanel, c1);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridy = 2;
        c2.gridx = 0;
        c2.anchor = GridBagConstraints.LINE_START;
        m_min = new JLabel("Minimum");
        add(m_min, c2);

        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.gridy = 2;
        c2.gridx = 1;
        m_minimumSlider = new JSlider(SLIDER_MIN, SLIDER_MAX, SLIDER_MIN);
        m_minimumSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (!isAdjusting) {
                    isAdjusting = true;
                    w_min = elementMin + m_minimumSlider.getValue() * (elementMax - elementMin)
                            / ((SLIDER_MAX - SLIDER_MIN) - 1.0);
                    if (w_min >= w_max) {
                        w_max = w_min;
                        m_maximumSlider.setValue((int)((w_max - elementMin) * ((SLIDER_MAX - SLIDER_MIN) - 1.0)
                                / (elementMax - elementMin)));
                    }

                    // update min label
                    updateLabelMinMax();

                    setBrightnessContrast();

                    publishFactor();
                    isAdjusting = false;
                }

            }
        });
        add(m_minimumSlider, c2);

        GridBagConstraints c3 = new GridBagConstraints();
        c3.gridy = 3;
        c3.gridx = 0;
        c3.anchor = GridBagConstraints.LINE_START;
        m_max = new JLabel("Maximum");
        add(m_max, c3);

        c3.fill = GridBagConstraints.HORIZONTAL;
        c3.gridy = 3;
        c3.gridx = 1;
        m_maximumSlider = new JSlider(SLIDER_MIN, SLIDER_MAX, SLIDER_MAX);
        m_maximumSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (!isAdjusting) {
                    isAdjusting = true;
                    w_max = elementMin + m_maximumSlider.getValue() * (elementMax - elementMin)
                            / ((SLIDER_MAX - SLIDER_MIN) - 1.0);
                    if (w_min >= w_max) {
                        w_min = w_max;
                        m_minimumSlider.setValue((int)((w_min - elementMin) * ((SLIDER_MAX - SLIDER_MIN) - 1.0)
                                / (elementMax - elementMin)));
                    }

                    // update max label
                    updateLabelMinMax();

                    setBrightnessContrast();

                    publishFactor();
                    isAdjusting = false;
                }
            }
        });
        add(m_maximumSlider, c3);

        GridBagConstraints c4 = new GridBagConstraints();
        c4.gridy = 4;
        c4.gridx = 0;
        c4.anchor = GridBagConstraints.LINE_START;
        m_bright = new JLabel("Brightness");
        add(m_bright, c4);

        c4.fill = GridBagConstraints.HORIZONTAL;
        c4.gridy = 4;
        c4.gridx = 1;
        m_brightnessSlider = new JSlider(SLIDER_MIN, SLIDER_MAX, (SLIDER_MAX - SLIDER_MIN) / 2);
        m_brightnessSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (!isAdjusting) {
                    isAdjusting = true;
                    w_brightness = m_brightnessSlider.getValue();

                    brightnessSetMinMax();
                    // update min,max label
                    updateLabelMinMax();

                    publishFactor();
                    isAdjusting = false;
                }
            }
        });
        add(m_brightnessSlider, c4);

        GridBagConstraints c5 = new GridBagConstraints();
        c5.gridy = 5;
        c5.gridx = 0;
        c5.anchor = GridBagConstraints.LINE_START;
        m_contrast = new JLabel("Contrast");
        add(m_contrast, c5);

        c5.fill = GridBagConstraints.HORIZONTAL;
        c5.gridy = 5;
        c5.gridx = 1;
        m_contrastSlider = new JSlider(SLIDER_MIN, SLIDER_MAX, (SLIDER_MAX - SLIDER_MIN) / 2);
        m_contrastSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (!isAdjusting) {
                    isAdjusting = true;
                    w_contrast = m_contrastSlider.getValue();

                    contrastSetMinMax();
                    // update min,max label
                    updateLabelMinMax();

                    publishFactor();
                    isAdjusting = false;
                }
            }
        });
        add(m_contrastSlider, c5);

        GridBagConstraints c6 = new GridBagConstraints();
        c6.gridy = 6;
        c6.gridwidth = 2;
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        m_autoSelect = new JCheckBox("Auto");
        m_autoSelect.setSelected(autoSelect);
        m_autoSelect.setToolTipText("Always do automatic adjustment.");
        m_autoSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_resetButton.setEnabled(!m_autoSelect.isSelected());
                m_automaticSaturationButton.setEnabled(!m_autoSelect.isSelected());
                if (!isAdjusting) {
                    isAdjusting = true;
                    autoSelect = m_autoSelect.isSelected();
                    if (autoSelect) {
                        autoAdjust();
                        publishFactor();
                    } else {
                        w_min = initialMin;
                        w_max = initialMax;
                        updateLabelMinMax();
                        updateSliderMinMax();
                        setBrightnessContrast();
                        publishFactor();
                    }
                    isAdjusting = false;
                }
            }
        });
        buttonPanel.add(m_autoSelect);

        m_automaticSaturationButton = new JButton("Auto");
        m_automaticSaturationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (!isAdjusting) {
                    isAdjusting = true;
                    autoAdjust();

                    publishFactor();
                    isAdjusting = false;
                }
            }
        });
        m_automaticSaturationButton.setEnabled(!m_autoSelect.isSelected());
        buttonPanel.add(m_automaticSaturationButton);

        m_resetButton = new JButton("Reset");
        m_resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (!isAdjusting) {
                    isAdjusting = true;
                    if (autoSelect) {
                        m_autoSelect.setSelected(autoSelect);
                        autoAdjust();
                    } else {
                        w_min = initialMin;
                        w_max = initialMax;
                    }
                    updateLabelMinMax();
                    updateSliderMinMax();
                    setBrightnessContrast();

                    publishFactor();
                    isAdjusting = false;
                }
            }
        });
        m_resetButton.setEnabled(!m_autoSelect.isSelected());
        buttonPanel.add(m_resetButton);

        m_planeSelect = new JCheckBox("Per plane");
        m_planeSelect.setSelected(planeSelected);
        m_planeSelect.setToolTipText("Planewise?");
        m_planeSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (!isAdjusting) {
                    isAdjusting = true;
                    planeSelected = m_planeSelect.isSelected();
                    if (planeSelected && planeSelection == null) {
                        getSelectedPlane();
                    }
                    computeInitialMinMax();
                    if (bitDepth == 16 || bitDepth == 32) {
                        elementMin = initialMin;
                        elementMax = initialMax;
                    }
                    w_min = elementMin;
                    w_max = elementMax;
                    setBrightnessContrast();
                    m_bundle.setDataMinMax(elementMin, elementMax);
                    m_bundle.setTheoreticalMinMax(elementMin, elementMax);
                    m_histoWidget.refreshChart(m_bundle);

                    if (autoSelect) {
                        autoAdjust();
                    }
                    publishFactor();
                    isAdjusting = false;
                }
            }
        });
        buttonPanel.add(m_planeSelect);

        add(buttonPanel, c6);
        isDrawn = true;
    }

    /**
     * Get plane selection of the actual instance.
     */
    private void getSelectedPlane() {
        // get interval

        final long[] min = planeSelectionPos.clone();
        final long[] max = planeSelectionPos.clone();

        min[planeSelectionIndices[0]] = img.min(planeSelectionIndices[0]);
        min[planeSelectionIndices[1]] = img.min(planeSelectionIndices[1]);

        max[planeSelectionIndices[0]] = img.max(planeSelectionIndices[0]);
        max[planeSelectionIndices[1]] = img.max(planeSelectionIndices[1]);

        FinalInterval interval = new FinalInterval(min, max);
        planeSelection = (RandomAccessibleInterval<T>)Views.iterable(Views.interval(img, interval));
    }

    /**
     * Set min and max given the actual brightness
     */
    protected void brightnessSetMinMax() {

        final double center = elementMin
                + (elementMax - elementMin) * (((SLIDER_MAX - SLIDER_MIN) - w_brightness) / (SLIDER_MAX - SLIDER_MIN));
        final double width = w_max - w_min;
        w_min = center - width / 2.0;
        w_max = center + width / 2.0;
        if ((int)w_min == (int)w_max) {
            w_min = (int)w_min - 1;
        }

        updateSliderMinMax();

        m_bundle.setTheoreticalMinMax(w_min, w_max);
        m_histoWidget.refreshChart(m_bundle);
    }

    /**
     * Set min and max given the actual contrast
     */
    protected void contrastSetMinMax() {
        final double slope;
        final double center = w_min + (w_max - w_min) / 2.0;

        final double range = elementMax - elementMin;

        final double mid = (SLIDER_MAX - SLIDER_MIN) / 2;
        if (w_contrast <= mid) {
            slope = w_contrast / mid;
        } else {
            slope = mid / ((SLIDER_MAX - SLIDER_MIN) - w_contrast);
        }
        if (slope > 0.0) {
            w_min = center - (0.5 * range) / slope;
            w_max = center + (0.5 * range) / slope;
            if ((int)w_min == (int)w_max) {
                w_min = (int)w_min - 1;
            }
        }

        updateSliderMinMax();

        m_bundle.setTheoreticalMinMax(w_min, w_max);
        m_histoWidget.refreshChart(m_bundle);
    }

    /**
     * Compute brightness and contrast given the actual min and max.
     */
    protected void setBrightnessContrast() {
        final double level = w_min + (w_max - w_min) / 2.0;

        final double normalizedLevel = 1.0 - (level - elementMin) / (elementMax - elementMin);
        w_brightness = (int)(normalizedLevel * (SLIDER_MAX - SLIDER_MIN));

        final double mid = (SLIDER_MAX - SLIDER_MIN) / 2;
        double c = ((elementMax - elementMin) / (w_max - w_min)) * mid;
        if (c > mid) {
            c = (SLIDER_MAX - SLIDER_MIN) - ((w_max - w_min) / (elementMax - elementMin)) * mid;
        }
        w_contrast = (int)c;

        if (isDrawn) {
            m_brightnessSlider.setValue((int)w_brightness);
            m_contrastSlider.setValue((int)w_contrast);
            m_bundle.setTheoreticalMinMax(w_min, w_max);
            m_histoWidget.refreshChart(m_bundle);
        }
    }

    /**
     * Automatically adjust contrast for actual instance.
     */
    protected void autoAdjust() {
        isAdjusting = true;
        Iterable<T> iterable = null;
        if (planeSelected) {
            iterable = Views.iterable(planeSelection);
        } else {
            iterable = imgIt;
        }

        double lper = ops.stats().percentile(iterable, 5).getRealDouble();
        double uper = ops.stats().percentile(iterable, 95).getRealDouble();

        w_min = lper;
        w_max = uper;

        updateSliderMinMax();
        updateLabelMinMax();
        setBrightnessContrast();
        isAdjusting = false;
    }

    /**
     * Compute the datatype's min and max.
     */
    private void computeDataMinMax() {
        Pair<RealType, RealType> minMaxRes = minMax.compute1(imgIt);
        initialMin = minMaxRes.getA().getRealDouble();
        initialMax = minMaxRes.getB().getRealDouble();
        if (bitDepth == 1) {
            elementMin = element.getMinValue();
            elementMax = element.getMaxValue();
            normMin = elementMin;
            normMax = elementMax;
            autoSelect = false;
        } else {
            // use initialMin/Max as boundaries
            normMin = element.getMinValue();
            normMax = element.getMaxValue();
            elementMin = initialMin;
            elementMax = initialMax;
        }
        w_min = initialMin;
        w_max = initialMax;
        if (isDrawn) {
            updateSliderMinMax();
            updateLabelMinMax();
        }
        setBrightnessContrast();
        createNewHistogram();

        m_bundle.setDataMinMax(elementMin, elementMax);
        m_bundle.setTheoreticalMinMax(elementMin, elementMax);
    }

    /**
     * Compute the instances min and max.
     */
    private void computeInitialMinMax() {
        createNewHistogram();

        if (isDrawn) {
            updateSliderMinMax();
            updateLabelMinMax();
        }
        setBrightnessContrast();
    }

    /**
     * Create a new histogram for the actual instance.
     */
    private void createNewHistogram() {
        Iterable<T> iterable = null;

        if (planeSelected) {
            IterableInterval<T> planeSelIt = (IterableInterval<T>)planeSelection;
            Pair<RealType, RealType> minMaxRes = minMax.compute1(planeSelIt);
            initialMin = minMaxRes.getA().getRealDouble();
            initialMax = minMaxRes.getB().getRealDouble();
            iterable = planeSelIt;
        } else {
            Pair<RealType, RealType> minMaxRes = minMax.compute1(imgIt);
            initialMin = minMaxRes.getA().getRealDouble();
            initialMax = minMaxRes.getB().getRealDouble();
            iterable = imgIt;
        }

        BinMapper1d<T> mapper = new Real1dBinMapper<T>(initialMin, initialMax, 256, false);
        Histogram1d<T> histogram = new Histogram1d<T>(iterable, mapper);
        if (m_bundle == null) {
            m_bundle = new HistogramBundle(histogram);
        } else {
            m_bundle.setHistogram(0, histogram);
        }
    }

    /**
     * Update the min and max slider's value.
     */
    private void updateSliderMinMax() {
        m_minimumSlider
                .setValue((int)((w_min - elementMin) * ((SLIDER_MAX - SLIDER_MIN) - 1.0) / (elementMax - elementMin)));
        m_maximumSlider
                .setValue((int)((w_max - elementMin) * ((SLIDER_MAX - SLIDER_MIN) - 1.0) / (elementMax - elementMin)));
    }

    /**
     * Update the min and max label's value.
     */
    private void updateLabelMinMax() {
        w_minLabel.setText(Integer.toString((int)w_min));
        w_maxLabel.setText(Integer.toString((int)w_max));
    }

    /**
     * Publish the normalization values and redraw the image.
     */
    private void publishFactor() {
        w_factor = Normalize.normalizationFactor(w_min, w_max, normMin, normMax);
        m_eventService.publish(new BrightnessContrastChgEvent(w_factor, w_min));
        m_eventService.publish(new ImgRedrawEvent());
    }

    /**
     * Listen to image changes.
     *
     * @param event
     */
    @EventListener
    public void onImgUpdated(final ImgWithMetadataChgEvent<T> event) {
        RandomAccessibleInterval convertedImg = AWTImageProvider.convertIfDouble(event.getRandomAccessibleInterval());
        img = convertedImg;
        imgIt = Views.iterable(convertedImg);
        element = img.randomAccess().get().createVariable();
        bitDepth = element.getBitsPerPixel();
        if (imgIt != null && planeSelection != null) {
            if (!isDrawn) {
                // analyze data
                computeDataMinMax();
                m_histoWidget = new HistogramBC(m_bundle);

                //computeInitialMinMax();
                draw();
                if (autoSelect) {
                    autoAdjust();
                }
                publishFactor();

            } else if (planeSelected) {
                isAdjusting = true;
                computeDataMinMax();
                if (autoSelect) {
                    autoAdjust();
                }
                isAdjusting = false;
            } else {
                isAdjusting = true;
                computeDataMinMax();
                m_histoWidget.refreshChart(m_bundle);
                //computeInitialMinMax();
                if (autoSelect) {
                    autoAdjust();
                }
                publishFactor();
                isAdjusting = false;
            }
        }
    }

    /**
     * Listen to selected Plane change.
     *
     * @param event
     */
    @EventListener
    public void onPlaneUpdated(final PlaneSelectionEvent event) {
        planeSelectionPos = event.getPlanePos();
        planeSelectionIndices = event.getDimIndices();
        if (imgIt != null) {
            try {
                planeSelection =
                        (RandomAccessibleInterval<T>)Views.iterable(Views.interval(img, event.getInterval(img)));
            } catch (AssertionError e) {

            }
            if (planeSelected) {
                isAdjusting = true;
                computeInitialMinMax();

                int bitDepth = imgIt.firstElement().getBitsPerPixel();
                if (bitDepth == 16 || bitDepth == 32 || bitDepth == 64) {
                    elementMin = initialMin;
                    elementMax = initialMax;
                }

                w_min = initialMin;
                w_max = initialMax;
                createNewHistogram();

                if (!isDrawn) {
                    computeDataMinMax();
                    m_histoWidget = new HistogramBC(m_bundle);
                    draw();
                }

                updateSliderMinMax();

                setBrightnessContrast();

                m_bundle.setDataMinMax(elementMin, elementMax);
                m_bundle.setTheoreticalMinMax(elementMin, elementMax);
                m_histoWidget.refreshChart(m_bundle);
                if (autoSelect) {
                    autoAdjust();
                }
                publishFactor();
                isAdjusting = false;
            }
        }
    }

    /**
     * Listen to image and labeling changes.
     *
     * @param event
     */
    @EventListener
    public void onImgAndLabelingUpdated(final ImgAndLabelingChgEvent<T, ?> event) {
        RandomAccessibleInterval convertedImg = AWTImageProvider.convertIfDouble(event.getRandomAccessibleInterval());
        img = convertedImg;
        imgIt = Views.iterable(img);
        element = img.randomAccess().get().createVariable();
        bitDepth = element.getBitsPerPixel();
        if (imgIt != null && planeSelection != null) {
            if (!isDrawn) {
                // analyze data
                computeDataMinMax();
                m_histoWidget = new HistogramBC(m_bundle);
                //computeInitialMinMax();

                draw();
                isDrawn = !isDrawn;
                if (autoSelect) {
                    autoAdjust();
                }
                publishFactor();

            } else {
                isAdjusting = true;
                planeSelected = false;
                m_planeSelect.setSelected(planeSelected);
                computeDataMinMax();
                m_histoWidget.refreshChart(m_bundle);

                //computeInitialMinMax();
                if (autoSelect) {
                    autoAdjust();
                }
                publishFactor();
                isAdjusting = false;
            }
        }
    }

    @EventListener
    public void onCombinedRUSynchChange(final CombinedRUSynchEvent e) {
        boolean isSynched = e.getSyncStatus();
        if (isSynched) {
            unmodImg = img;
            T val = img.randomAccess().get().createVariable();
            val.setReal(val.getMinValue());
            img = Views.interval(Views.extendValue(img, val), img);
        } else {
            if (unmodImg != null) {
                img = unmodImg;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
        eventService.subscribe(this);

        eventService.publish(new BrightnessContrastChgEvent(w_factor, w_min));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position getPosition() {
        return Position.ADDITIONAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        out.writeInt(m_minimumSlider.getValue());
        out.writeInt(m_maximumSlider.getValue());
        out.writeInt(m_brightnessSlider.getValue());
        out.writeInt(m_contrastSlider.getValue());
        out.writeBoolean(m_planeSelect.isSelected());
        out.writeBoolean(planeSelected);
        out.writeBoolean(autoSelect);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_minimumSlider.setValue(in.readInt());
        m_maximumSlider.setValue(in.readInt());
        m_brightnessSlider.setValue(in.readInt());
        m_contrastSlider.setValue(in.readInt());
        m_planeSelect.setSelected(in.readBoolean());
        planeSelected = in.readBoolean();
        autoSelect = in.readBoolean();
    }
}