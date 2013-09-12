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
package org.knime.knip.base.nodes.view.imgparadjust;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SetThresholdPanel<T extends RealType<T>, I extends RandomAccessibleInterval<T> & IterableInterval<T>>
        extends ViewerComponent {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private EventService m_eventService;

    private final JSlider m_thresholdSlider;

    private final JLabel m_thresholdValue;

    private final JCheckBox m_useThreshold;

    public SetThresholdPanel() {
        super("Threshold", false);
        setMaximumSize(new Dimension(250, getMaximumSize().height));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        m_thresholdValue = new JLabel();

        m_thresholdSlider = new JSlider(0, 255, 125);
        m_thresholdSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_thresholdValue.setText("" + m_thresholdSlider.getValue());
                publishEvent();
            }
        });
        m_useThreshold = new JCheckBox("Set threshold");
        m_useThreshold.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_thresholdValue.setText("" + m_thresholdSlider.getValue());
                publishEvent();
            }
        });
        add(m_useThreshold);
        add(m_thresholdSlider);
        add(m_thresholdValue);

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
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        // TODO Auto-generated method stub

    }

    @EventListener
    public void onImgUpdated(final IntervalWithMetadataChgEvent<T> evt) {
        final T type = evt.getIterableInterval().firstElement().createVariable();
        m_thresholdSlider.setMinimum((int)type.getMinValue());
        m_thresholdSlider.setMaximum((int)type.getMaxValue());
        publishEvent();

    }

    private void publishEvent() {
        if (m_useThreshold.isSelected()) {
            m_eventService.publish(new ThresholdValChgEvent(m_thresholdSlider.getValue()));
            m_eventService.publish(new ImgRedrawEvent());
        } else {
            m_eventService.publish(new ThresholdValChgEvent(Double.NaN));
            m_eventService.publish(new ImgRedrawEvent());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
        m_eventService.subscribe(this);

    }

}
