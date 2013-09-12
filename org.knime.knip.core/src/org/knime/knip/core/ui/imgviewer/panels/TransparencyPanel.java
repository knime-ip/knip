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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.TransparencyPanelValueChgEvent;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class TransparencyPanel extends ViewerComponent {

    private static final long serialVersionUID = 1L;

    private EventService m_eventService;

    private JSlider m_slider;

    private JLabel m_sliderValue;

    private final boolean m_showLabel;

    public TransparencyPanel() {
        super("Transparency", false);
        m_showLabel = false;
        construct();
    }

    public TransparencyPanel(final boolean isBorderHidden) {
        super("Transparency", isBorderHidden);
        m_showLabel = isBorderHidden;
        construct();
    }

    private void construct() {
        setMinimumSize(new Dimension(180, 40));
        setPreferredSize(new Dimension(180, 40));

        m_sliderValue = new JLabel("128");
        m_slider = new JSlider(SwingConstants.HORIZONTAL, 0, 255, 128);
        m_slider.setPreferredSize(new Dimension(130, 17));
        m_slider.setMaximumSize(new Dimension(180, m_slider.getMaximumSize().height));
        m_slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_eventService.publish(new TransparencyPanelValueChgEvent(m_slider.getValue()));
                m_eventService.publish(new ImgRedrawEvent());
                m_sliderValue.setText("" + m_slider.getValue());
            }
        });

        addComponents();
    }

    private void addComponents() {
        setLayout(new GridBagLayout());

        final GridBagConstraints gc = new GridBagConstraints();
        int x = 0;
        int y = 0;

        // all
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        // label row
        if (m_showLabel) {
            gc.weightx = 1.0;
            gc.gridx = x;
            gc.gridwidth = 2;
            gc.gridy = y;
            gc.insets = new Insets(0, 5, 5, 0);
            add(new JLabel("Transparency"), gc);

            y++;
            gc.gridwidth = 1;

        }

        // content row
        gc.weightx = 0.0;
        gc.gridx = x;
        gc.gridy = y;
        gc.insets = new Insets(0, 5, 0, 0);
        add(m_slider, gc);

        x++;
        gc.insets = new Insets(0, 10, 0, 0);
        gc.weightx = 1.0;
        gc.gridx = x;
        gc.gridy = y;
        add(m_sliderValue, gc);
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

    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        out.writeInt(m_slider.getValue());

    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_slider.setValue(in.readInt());
    }
}
