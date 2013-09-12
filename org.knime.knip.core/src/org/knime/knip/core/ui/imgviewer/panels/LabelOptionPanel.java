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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableUtils;
import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelColoringChangeEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelOptionsChangeEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelOptionPanel extends ViewerComponent {

    private class OKAdapter implements ActionListener {
        @Override
        public void actionPerformed(final java.awt.event.ActionEvent evt) {
            final Color newColor = LabelOptionPanel.this.m_colorChooser.getColor();
            LabelingColorTableUtils.setBoundingBoxColor(newColor);

            m_eventService.publish(new LabelOptionsChangeEvent(true));

            m_eventService.publish(new LabelColoringChangeEvent(newColor, RandomMissingColorHandler.getGeneration()));
            m_eventService.publish(new ImgRedrawEvent());
        }
    }

    private static final long serialVersionUID = 1L;

    private EventService m_eventService;

    private JButton m_boundingBoxColor;

    private JButton m_resetColor;

    // ColorChooser Dialog
    private final JColorChooser m_colorChooser = new JColorChooser();

    private JDialog m_colorDialog;

    private final JCheckBox m_renderLabelString = new JCheckBox();

    private final OKAdapter m_adapter;

    /**
     * @param isBorderHidden
     */
    public LabelOptionPanel(final boolean isBorderHidden) {
        super("Color Options", isBorderHidden);
        this.m_adapter = new OKAdapter();

        construct();
    }

    @SuppressWarnings("javadoc")
    public LabelOptionPanel() {
        this(false);
    }

    private void construct() {
        setMinimumSize(new Dimension(240, 80));
        setPreferredSize(new Dimension(240, 80));
        setMaximumSize(new Dimension(240, this.getMaximumSize().height));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        // Buttons for changing BoundingBox color and reset color
        m_boundingBoxColor = new JButton(new ImageIcon(getClass().getResource("ColorIcon.png")));

        m_resetColor = new JButton(new ImageIcon(getClass().getResource("ColorIcon.png")));

        m_boundingBoxColor.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                if (m_colorDialog == null) {
                    m_colorDialog =
                            JColorChooser.createDialog(LabelOptionPanel.this, "Choose Bounding Box Color", false,
                                                       m_colorChooser, m_adapter, null);
                }
                m_colorDialog.setVisible(true);
            }
        });

        m_resetColor.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                RandomMissingColorHandler.resetColorMap();
                m_eventService.publish(new LabelColoringChangeEvent(LabelingColorTableUtils.getBoundingBoxColor(),
                        RandomMissingColorHandler.getGeneration()));
                m_eventService.publish(new ImgRedrawEvent());
            }
        });

        m_renderLabelString.setSelected(false);
        m_renderLabelString.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_eventService.publish(new LabelOptionsChangeEvent(m_renderLabelString.isSelected()));
                m_eventService.publish(new ImgRedrawEvent());
            }
        });

        add(createComponentPanel());
    }

    private JPanel createComponentPanel() {
        final JPanel ret = new JPanel();
        ret.setLayout(new GridBagLayout());

        final GridBagConstraints gc = new GridBagConstraints();
        int y = 0;

        // all
        gc.fill = GridBagConstraints.HORIZONTAL;

        // first col
        gc.anchor = GridBagConstraints.LINE_START;
        gc.weightx = 1.0;

        gc.gridy = y;
        gc.insets = new Insets(5, 5, 0, 5);
        ret.add(new JLabel("Set BoundingBox Color"), gc);

        y++;
        gc.gridy = y;
        gc.insets = new Insets(0, 5, 0, 5);
        ret.add(new JLabel("Change Random Label Colors"), gc);

        y++;
        gc.gridy = y;
        gc.insets = new Insets(5, 5, 0, 5);
        ret.add(new JLabel("Show Bounding Box Names"), gc);

        // 2nd col
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;

        y = 0;
        gc.gridy = y;
        gc.insets = new Insets(5, 5, 0, 5);
        ret.add(m_boundingBoxColor, gc);

        y++;
        gc.gridy = y;
        gc.insets = new Insets(0, 5, 0, 5);
        ret.add(m_resetColor, gc);

        y++;
        gc.gridy = y;
        gc.insets = new Insets(5, 5, 0, 5);
        ret.add(m_renderLabelString, gc);

        return ret;
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
        // color codings cannot be saved

    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        // color codings cannot be saved
    }

    @EventListener
    public void onClose(final ViewClosedEvent e) {
        if (m_colorDialog != null) {
            m_colorDialog.dispose();
            m_colorDialog = null;
        }
    }
}
