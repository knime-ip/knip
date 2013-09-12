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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.NormalizationParametersChgEvent;

/**
 * Settings to enhance the contrast of an image.
 * 
 * Publishes {@link NormalizationParametersChgEvent}.
 * 
 * @param <T>
 * @param <I>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgNormalizationPanel<T extends RealType<T>, I extends Img<T>> extends ViewerComponent {

    // 0..400 with steps of 1 <=> (0..50 with steps of 0.125) * 8
    private static final int SATURATION_SLIDER_MAX = 400;

    private static final float SATURATION_SLIDER_FACTOR = 8;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /* The saturation slider going in steps of 0.125 from 0 to 50 */
    private final JSlider m_saturationSlider;

    /* CheckBox indicating weather the image should be normalized or not */
    private final JCheckBox m_normalize;

    /* EventService to publish events */
    private EventService m_eventService;

    private final JLabel m_sat;

    /**
     * Constructor creating the GUI.
     */
    public ImgNormalizationPanel() {
        this(0, false);
    }

    /**
     * Creates {@link ImgNormalizationPanel} with the given default value for normalization.
     * 
     * @param saturation the default saturation
     * 
     * 
     * @param normalize whether normalization should be enabled
     */
    public ImgNormalizationPanel(final double sat, final boolean normalize) {
        super("Normalize", false);

        setMaximumSize(new Dimension(250, getMaximumSize().height));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        m_normalize = new JCheckBox("Normalize");
        m_normalize.setSelected(normalize);

        m_normalize.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_saturationSlider.setEnabled(m_normalize.isSelected());
                m_sat.setEnabled(m_normalize.isSelected());
                m_eventService.publish(new NormalizationParametersChgEvent(
                        (m_saturationSlider.getValue() / SATURATION_SLIDER_FACTOR), m_normalize.isSelected()));
                m_eventService.publish(new ImgRedrawEvent());
            }
        });
        final JLabel saturation = new JLabel("Saturation (%):");
        m_sat = new JLabel("             " + sat + "%");
        m_sat.setEnabled(false);
        add(m_normalize);
        add(saturation);
        // DO NOT CHANGE THE SLIDER W
        m_saturationSlider = new JSlider(0, SATURATION_SLIDER_MAX);
        m_saturationSlider.setValue((int)(sat * SATURATION_SLIDER_FACTOR));
        m_saturationSlider.setEnabled(false);
        m_saturationSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_eventService.publish(new NormalizationParametersChgEvent(m_saturationSlider.getValue()
                        / SATURATION_SLIDER_FACTOR, m_normalize.isSelected()));
                m_eventService.publish(new ImgRedrawEvent());
                final float percent = m_saturationSlider.getValue() / SATURATION_SLIDER_FACTOR;
                m_sat.setText("             " + percent + "%");
            }
        });

        add(m_saturationSlider);
        add(m_sat);
        add(Box.createVerticalGlue());
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
        // inform everybody about our settings.
        eventService.publish(new NormalizationParametersChgEvent(m_saturationSlider.getValue(), m_normalize
                .isSelected()));
    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        out.writeInt(m_saturationSlider.getValue());
        out.writeBoolean(m_normalize.isSelected());
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException {
        m_saturationSlider.setValue(in.readInt());
        m_normalize.setSelected(in.readBoolean());
    }

}
