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
package org.knime.knip.core.ui.imgviewer.panels;

import java.awt.Adjustable;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.NumberFormat;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.KeyStroke;

import net.imglib2.Interval;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.TypedAxis;
import net.imglib2.meta.TypedSpace;
import net.imglib2.type.Type;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.CalibrationUpdateEvent;
import org.knime.knip.core.ui.imgviewer.events.ForcePlanePosEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;

/**
 * Allows the user to select a plane in a multdimensional space.
 * 
 * Publishes {@link PlaneSelectionEvent}
 * 
 * 
 * @param <T> image type
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
@SuppressWarnings("serial")
public class PlaneSelectionPanel<T extends Type<T>, I extends Interval> extends ViewerComponent {

    private final static int DEFAULT_X = 0;

    private final static int DEFAULT_Y = 1;

    private JScrollBar[] m_scrollBars;

    private JScrollBar m_totalSlider;

    private JCheckBox[] m_planeCheckBoxes;

    private JFormattedTextField[] m_coordinateTextFields;

    /* the plane dimension indices */
    private int m_dim1 = DEFAULT_X;

    private int m_dim2 = DEFAULT_Y;

    /* the steps to switch to the subsequent coordinate in one dimension */
    private int[] m_steps;

    /* the dimension sizes */
    private long[] m_dims;

    /* recognizes which dimension to alter next */
    private int m_alterDim;

    private TypedAxis[] m_axes;

    private EventService m_eventService;

    private boolean m_isAdjusting;

    private boolean m_useCalibration = false;

    private long[] m_oldCoordinates;

    private JCheckBox m_calibrationCheckbox;

    private TypedSpace<? extends TypedAxis> m_calibratedSpace;

    public PlaneSelectionPanel() {
        super("Plane selection", false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // empty panel that ensures a minimum width
        // setting the same thing on this has bad impacts on the
        // component height
        final JPanel wider = new JPanel();
        wider.setMaximumSize(new Dimension(200, wider.getMaximumSize().height));
        wider.setPreferredSize(new Dimension(200, wider.getPreferredSize().height));
        wider.setMinimumSize(new Dimension(200, wider.getMinimumSize().height));
        add(wider);

        setMaximumSize(new Dimension(200, getMaximumSize().height));

    }

    @EventListener
    public void onClose(final ViewClosedEvent e) {
        m_axes = null;
        m_dims = null;
        m_oldCoordinates = null;
        m_steps = null;
        m_calibratedSpace = null;
    }

    /**
     * @param dimX the first dimension index
     * @param dimY the second dimension index
     */
    private void setPlaneDimensionIndices(final int dimX, final int dimY) {
        m_isAdjusting = true;

        m_scrollBars[m_dim1].setEnabled(true);
        m_scrollBars[m_dim2].setEnabled(true);
        m_planeCheckBoxes[m_dim1].setSelected(false);
        m_planeCheckBoxes[m_dim2].setSelected(false);
        m_planeCheckBoxes[m_dim1].setEnabled(true);
        m_planeCheckBoxes[m_dim2].setEnabled(true);

        m_dim1 = dimX;
        m_dim2 = dimY;
        m_scrollBars[dimX].setEnabled(false);
        m_scrollBars[dimY].setEnabled(false);
        m_planeCheckBoxes[m_dim1].setSelected(true);
        m_planeCheckBoxes[m_dim2].setSelected(true);
        m_planeCheckBoxes[m_dim1].setEnabled(false);
        m_planeCheckBoxes[m_dim2].setEnabled(false);

        m_isAdjusting = false;

        // calculate the steps to step forward in the linear array
        boolean first = true;
        int lasti = 0;
        for (int i = 0; i < m_dims.length; i++) {
            if ((i == dimX) || (i == dimY)) {
                m_coordinateTextFields[i].setEnabled(false);
            } else {
                if (first) {
                    m_steps[i] = 1;
                    lasti = i;
                    first = false;
                } else {
                    m_steps[i] = m_steps[lasti] * (int)m_dims[lasti];
                    lasti = i;
                }
                m_coordinateTextFields[i].setEnabled(true);
            }
        }

        // maximum index
        int max = 1;
        for (int i = 0; i < m_dims.length; i++) {
            if ((i == dimX) || (i == dimY)) {
                continue;
            }
            max *= m_dims[i];
        }

        m_isAdjusting = true;

        m_totalSlider.setMinimum(0);
        m_totalSlider.setVisibleAmount(1);
        m_totalSlider.setValue(m_totalSlider.getValue() <= max ? m_totalSlider.getValue() : 1);
        m_totalSlider.setMaximum(max);
        m_totalSlider.setEnabled(max > 1);

        setEnabled(max > 1);
        m_isAdjusting = false;
    }

    // -- ChangeListener API methods --

    /** Handles slider events. */
    private void onSliderChanged(final int id) {

        if (m_isAdjusting) {
            return;
        }

        if (id == -1) {
            updateDimSliders();
        } else {
            updateTotalSlider();
        }

        // test if the slider positions changed
        boolean change = false;
        final long[] imgCoords = getImageCoordinate();
        if ((m_oldCoordinates != null) && (imgCoords.length == m_oldCoordinates.length)) {
            for (int i = 0; i < imgCoords.length; i++) {
                if (imgCoords[i] != m_oldCoordinates[i]) {
                    change = true;
                    break;
                }
            }
        } else {
            change = true;
        }

        m_oldCoordinates = imgCoords;

        if (change) {
            for (int i = 0; i < m_dims.length; i++) {
                m_coordinateTextFields[i].setValue(m_scrollBars[i].getValue() + 1);
            }

            m_eventService.publish(new PlaneSelectionEvent(Math.min(m_dim1, m_dim2), Math.max(m_dim2, m_dim1),
                    imgCoords));
            fireCalibrationEvent();
            m_eventService.publish(new ImgRedrawEvent());
        }
    }

    /**
     * 
     * @param e
     * @param id
     */
    private void onCheckBoxChange(final ItemEvent e, final int id) {

        if (m_isAdjusting) {
            return;
        }

        final int idx = Integer.parseInt(((JCheckBox)e.getSource()).getActionCommand());

        if (m_alterDim == 0) {
            setPlaneDimensionIndices(idx, m_dim2);
        } else {
            setPlaneDimensionIndices(m_dim1, idx);
        }
        m_alterDim = (m_alterDim + 1) % 2;
        m_eventService.publish(new PlaneSelectionEvent(Math.min(m_dim1, m_dim2), Math.max(m_dim2, m_dim1),
                getImageCoordinate()));
        fireCalibrationEvent();
        m_eventService.publish(new ImgRedrawEvent());

    }

    /* Gets the index of the currently displayed image. */
    private void updateTotalSlider() {
        // calc index
        int index = 0;
        for (int i = 0; i < m_steps.length; i++) {
            if ((i == m_dim1) || (i == m_dim2)) {
                continue;
            }
            index += m_steps[i] * (m_scrollBars[i].getValue());
        }
        // update index
        if (index >= 0) {
            m_isAdjusting = true;
            m_totalSlider.setValue(index);
            m_isAdjusting = false;
        }
    }

    private void updateDimSliders() {
        // calc logical coordinates
        final int[] coords = new int[m_scrollBars.length];
        int idx = m_totalSlider.getValue();

        for (int i = coords.length - 1; i > -1; i--) {
            if ((i == m_dim1) || (i == m_dim2)) {
                continue;
            }
            coords[i] = idx / m_steps[i];
            idx = idx % m_steps[i];
        }
        // update coordinates
        for (int i = 0; i < coords.length; i++) {
            if ((i == m_dim1) || (i == m_dim2)) {
                continue;
            }
            m_isAdjusting = true;
            m_scrollBars[i].setValue(coords[i]);
            m_isAdjusting = false;
        }
    }

    private void onCalibrationBoxChanged() {
        if (m_calibrationCheckbox.isSelected() != m_useCalibration) {
            m_useCalibration = m_calibrationCheckbox.isSelected();
            fireCalibrationEvent();
            m_eventService.publish(new ImgRedrawEvent());
        }
    }

    @SuppressWarnings("rawtypes")
    private void fireCalibrationEvent() {
        // calculate calibration values
        final double[] scaleFactors = new double[m_dims.length];
        for (int i = 0; i < scaleFactors.length; i++) {
            // = don't scale as default
            scaleFactors[i] = 1.0d;
        }

        if (m_useCalibration && (m_calibratedSpace != null) && m_calibratedSpace instanceof CalibratedSpace) {
            final double[] tmpFactors = new double[scaleFactors.length];
            ((CalibratedSpace)m_calibratedSpace).calibration(tmpFactors);

            double min = Double.MAX_VALUE;
            boolean foundAFactor = false;

            for (int i = 0; i < tmpFactors.length; i++) {
                // clean up
                if ((tmpFactors[i] > 0.0d) && !Double.isNaN(tmpFactors[i])) {
                    foundAFactor = true;
                } else {
                    tmpFactors[i] = 1.0d;
                }

                // get minimum
                if (tmpFactors[i] < min) {
                    min = tmpFactors[i];
                }
            }

            if (foundAFactor) {
                // normalize with min and scale
                final double norm = 1.0d / min;

                for (int i = 0; i < scaleFactors.length; i++) {
                    scaleFactors[i] = (tmpFactors[i] * norm);
                }

            }
        }

        m_eventService.publish(new CalibrationUpdateEvent(scaleFactors, new int[]{Math.min(m_dim1, m_dim2),
                Math.max(m_dim1, m_dim2)}));
    }

    /**
     * 
     * @return the coordinates of the currently selected image (a newly generated array)
     */
    protected long[] getImageCoordinate() {
        if (m_scrollBars == null) {
            return new long[m_dims.length];
        }
        final long[] res = new long[m_dims.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = m_scrollBars[i].getValue();
        }
        return res;
    }

    /**
     * @param name
     */
    @EventListener
    public void onImgUpdated(final IntervalWithMetadataChgEvent<T> e) {

        if (m_dims == null) {
            m_dims = new long[e.getTypedSpace().numDimensions()];
        }

        if ((m_axes == null) || (m_axes.length != e.getTypedSpace().numDimensions())) {
            m_axes = new TypedAxis[e.getTypedSpace().numDimensions()];
        }

        final long[] oldDims = m_dims.clone();
        final TypedAxis[] oldAxes = m_axes.clone();

        // local dims //axes labels
        for (int d = 0; d < e.getTypedSpace().numDimensions(); d++) {
            m_axes[d] = e.getTypedSpace().axis(d);
        }

        m_dims = new long[e.getRandomAccessibleInterval().numDimensions()];
        e.getRandomAccessibleInterval().dimensions(m_dims);

        if (!Arrays.equals(m_axes, oldAxes)) {
            // reset m_dim1, m_dim2 before calibration
            m_dim1 = DEFAULT_X;
            m_dim2 = DEFAULT_Y;
        }

        m_calibratedSpace = e.getTypedSpace();
        fireCalibrationEvent(); // update to new calibration values

        if (!Arrays.equals(oldDims, m_dims) || !Arrays.equals(m_axes, oldAxes)) {
            draw();

            for (int i = 0; i < m_dims.length; i++) {
                m_coordinateTextFields[i].setValue(m_scrollBars[i].getValue() + 1);
            }
        }

    }

    @EventListener
    public void onFocusLabel(final ForcePlanePosEvent e) {
        m_isAdjusting = true;
        for (int d = 0; d < e.getPosition().length; d++) {

            if ((d == m_dim1) || (d == m_dim2)) {
                m_scrollBars[d].setValue(0);
            } else {
                m_scrollBars[d].setValue((int)e.getPosition()[d]);
            }
        }
        updateTotalSlider();
        onSliderChanged(0);
    }

    private void draw() {

        if ((m_dims != null) && (m_axes != null)) {

            m_steps = new int[m_dims.length];

            removeAll();
            final JPanel nPanel = new JPanel();
            nPanel.setLayout(new BoxLayout(nPanel, BoxLayout.X_AXIS));
            add(nPanel);
            add(Box.createVerticalStrut(2));
            m_totalSlider = new JScrollBar(Adjustable.HORIZONTAL);
            Dimension dim = m_totalSlider.getPreferredSize();
            dim.width = 150;
            m_totalSlider.setPreferredSize(dim);
            m_totalSlider.addAdjustmentListener(new ChangeListenerWithId(-1));
            nPanel.add(new JLabel("N"));
            nPanel.add(Box.createHorizontalStrut(3));
            nPanel.add(m_totalSlider);

            // add key bindings to the JTextArea
            final int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
            final InputMap inMap = getInputMap(condition);
            final ActionMap actMap = getActionMap();

            inMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0), "FORWARD");
            inMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "BACKWARD");
            actMap.put("FORWARD", new ForwardBackwardAction("FORWARD", m_totalSlider, 1));
            actMap.put("BACKWARD", new ForwardBackwardAction("BACKWARD", m_totalSlider, 1));

            final JPanel dimPanel = new JPanel();
            dimPanel.setLayout(new BoxLayout(dimPanel, BoxLayout.Y_AXIS));
            add(dimPanel);

            m_scrollBars = new JScrollBar[m_dims.length];
            m_planeCheckBoxes = new JCheckBox[m_dims.length];
            m_coordinateTextFields = new JFormattedTextField[m_dims.length];

            JPanel sliderPanel;
            for (int i = 0; i < m_dims.length; i++) {
                sliderPanel = new JPanel();
                sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.X_AXIS));
                m_scrollBars[i] = new JScrollBar(Adjustable.HORIZONTAL);
                m_planeCheckBoxes[i] = new JCheckBox("", false);
                m_planeCheckBoxes[i].addItemListener(new ItemListenerWithId(i));
                m_planeCheckBoxes[i].setActionCommand(i + "");

                dim = m_scrollBars[i].getPreferredSize();
                dim.width = 150;
                m_scrollBars[i].setPreferredSize(dim);
                m_scrollBars[i].setValue(m_scrollBars[i].getValue() < m_dims[i] ? m_scrollBars[i].getValue() : 0);
                m_scrollBars[i].setMinimum(0);
                m_scrollBars[i].setMaximum((int)m_dims[i]);

                m_scrollBars[i].setEnabled(m_dims[i] > 1);
                m_scrollBars[i].setVisibleAmount(1);
                m_scrollBars[i].addAdjustmentListener(new ChangeListenerWithId(i));

                sliderPanel.add(m_axes != null ? (new JLabel(m_axes[i].type().getLabel())) : (new JLabel("" + i)));

                sliderPanel.add(Box.createHorizontalStrut(3));
                sliderPanel.add(m_scrollBars[i]);

                // add coordinate text fields
                final NumberFormat nf = NumberFormat.getInstance();
                nf.setGroupingUsed(false);
                final JFormattedTextField tmp = new JFormattedTextField(nf);
                tmp.setMinimumSize(new Dimension(40, tmp.getMinimumSize().height));
                tmp.setPreferredSize(new Dimension(40, tmp.getPreferredSize().height));
                tmp.setMaximumSize(new Dimension(40, tmp.getPreferredSize().height));

                final int index = i;
                tmp.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        textCoordinatesChanged(index);
                    }
                });

                tmp.addFocusListener(new FocusListener() {

                    @Override
                    public void focusLost(final FocusEvent arg0) {
                        textCoordinatesChanged(index);
                    }

                    @Override
                    public void focusGained(final FocusEvent e) {
                    }

                });

                m_coordinateTextFields[i] = tmp;
                sliderPanel.add(m_coordinateTextFields[i]);
                sliderPanel.add(m_planeCheckBoxes[i]);
                sliderPanel.add(Box.createHorizontalStrut(7));
                dimPanel.add(sliderPanel);

            }

            // add calibration checkbox
            m_calibrationCheckbox = new JCheckBox("use calibration");
            m_calibrationCheckbox.setSelected(m_useCalibration);
            m_calibrationCheckbox.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    onCalibrationBoxChanged();
                }
            });

            add(m_calibrationCheckbox);

            setPlaneDimensionIndices(m_dim1, m_dim2);

            m_eventService.publish(new PlaneSelectionEvent(Math.min(m_dim1, m_dim2), Math.max(m_dim2, m_dim1),
                    getImageCoordinate()));
            fireCalibrationEvent();
        }

        updateUI();
    }

    private void textCoordinatesChanged(final int fieldIndex) {
        final int value = Integer.valueOf(m_coordinateTextFields[fieldIndex].getText());
        if (value != (m_scrollBars[fieldIndex].getValue() + 1)) {
            if (value < m_scrollBars[fieldIndex].getMinimum()) {
                m_coordinateTextFields[fieldIndex].setText(String.valueOf(m_scrollBars[fieldIndex].getMinimum() + 1));
            } else if (value > m_scrollBars[fieldIndex].getMaximum()) {
                m_coordinateTextFields[fieldIndex].setText(String.valueOf(m_scrollBars[fieldIndex].getMaximum()));
            }
            // triggers also the necessary events
            // set value -1 because internal model starts with 0
            m_scrollBars[fieldIndex].setValue(value - 1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position getPosition() {
        return Position.SOUTH;
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
        // Nothing to do here
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        // Nothing to do here
    }

    private class ItemListenerWithId implements ItemListener {

        private final int m_id;

        public ItemListenerWithId(final int id) {
            m_id = id;
        }

        @Override
        public void itemStateChanged(final ItemEvent e) {
            onCheckBoxChange(e, m_id);

        }
    }

    private class ChangeListenerWithId implements AdjustmentListener {

        private final int m_id;

        public ChangeListenerWithId(final int id) {
            m_id = id;
        }

        @Override
        public void adjustmentValueChanged(final AdjustmentEvent e) {
            onSliderChanged(m_id);
        }

    }

    // Action for our key binding to perform when bound event occurs
    private class ForwardBackwardAction extends AbstractAction {
        private final JScrollBar m_slider;

        private final int m_scrollableIncrement;

        public ForwardBackwardAction(final String name, final JScrollBar slider, final int scrollableIncrement) {
            super(name);
            this.m_slider = slider;
            this.m_scrollableIncrement = scrollableIncrement;
        }

        @Override
        public void actionPerformed(final ActionEvent ae) {

            for (final JFormattedTextField field : m_coordinateTextFields) {
                if (field.hasFocus()) {
                    return;
                }
            }

            final String name = getValue(Action.NAME).toString();
            int value = m_slider.getValue();
            if (name.equals("FORWARD")) {
                value += m_scrollableIncrement;
                if (value >= m_totalSlider.getMaximum()) {
                    return;
                }

                m_slider.setValue(value);
            } else if (name.equals("BACKWARD")) {
                value -= m_scrollableIncrement;
                if (value < 0) {
                    return;
                }
                m_slider.setValue(value);
            }
        }
    }

}
