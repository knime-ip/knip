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

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003, 2010
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   29 Jan 2010 (hornm): created
 */

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.panels.ViewerControlEvent;
import org.knime.knip.core.ui.imgviewer.panels.ViewerScrollEvent;
import org.knime.knip.core.ui.imgviewer.panels.ViewerScrollEvent.Direction;
import org.knime.knip.core.ui.imgviewer.panels.ViewerToggleEvent;

/**
 * A TableCellViewPane providing another view on image objects. It allows to browser through the individual
 * planes/dimensions, enhance contrast, etc.
 *
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class MissingImgViewer extends ImgViewer implements ViewerComponentContainer {

    /* def */
    private static final long serialVersionUID = 1L;

    // Panels of this viewer

    private JPanel m_background;

    private JLabel m_centerPanel;

    private JPanel m_bottomControl;

    private JPanel m_leftControl;

    private JPanel m_rightControl;

    private JPanel m_topControl;

    private JPanel m_infoPanel;

    private JPanel m_rightPanel;

    // Counter to keep track of added components

    private int m_currentSlot;

    // Buttons available in this viewer

    private JToggleButton m_bottomQuickViewButton;

    private JButton m_overviewButton;

    private ViewerComponent[] m_controls = new ViewerComponent[4];

    /** keep the option panel-references to load and save configurations */
    protected List<ViewerComponent> m_viewerComponents;

    /** EventService of the viewer, unique for each viewer. */
    protected EventService m_eventService;

    public MissingImgViewer() {
        this(new EventService());

    }

    public MissingImgViewer(final boolean simplified) {
        this(simplified, new EventService());

    }

    public MissingImgViewer(final EventService eventService) {
        this(false, eventService);
    }

    /**
     * @param nodeModel
     */
    public MissingImgViewer(final boolean simplified, final EventService eventService) {

        setLayout(new BorderLayout());

        m_eventService = eventService;
        m_viewerComponents = new ArrayList<ViewerComponent>();

        m_background = new JPanel();

        // Left side - the actual view

        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout gbl = new GridBagLayout();
        m_background.setLayout(gbl);


        gbc.anchor = GridBagConstraints.CENTER;

        gbc.gridx = 2;
        gbc.gridy = 1;

        gbc.gridheight = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;


        // The imageview

        gbc.gridx = 1;
        gbc.gridy = 2;

        gbc.gridheight = 4;
        gbc.gridwidth = 5;

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;


        ImageIcon bg = new ImageIcon(this.getClass().getResource("/icons/missingval.png"));
        m_centerPanel = new JLabel(bg, SwingConstants.CENTER);
        m_background.add(m_centerPanel, gbc);

        // Left & right control panel

        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 4;

        gbc.gridheight = 1;
        gbc.gridwidth = 1;

        gbc.fill = GridBagConstraints.NONE;

        gbc.weightx = 0;
        gbc.weighty = 1;

        m_leftControl = new JPanel();
        m_leftControl.setLayout(new BorderLayout());
        m_background.add(m_leftControl, gbc);

        BasicArrowButton l = new BasicArrowButton(SwingConstants.WEST);
        m_leftControl.add(l);

        l.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_eventService.publish(new ViewerScrollEvent(Direction.WEST));

            }

        });

        gbc.gridx = 6;
        gbc.gridy = 4;

        m_rightControl = new JPanel();
        m_rightControl.setLayout(new BorderLayout());
        m_background.add(m_rightControl, gbc);

        BasicArrowButton r = new BasicArrowButton(SwingConstants.EAST);
        m_rightControl.add(r);

        r.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_eventService.publish(new ViewerScrollEvent(Direction.EAST));

            }

        });

        // Top & botton control panel

        gbc.gridx = 3;
        gbc.gridy = 6;

        gbc.weightx = 1;
        gbc.weighty = 0;

        m_bottomControl = new JPanel();
        m_bottomControl.setLayout(new BorderLayout());
        m_background.add(m_bottomControl, gbc);

        BasicArrowButton b = new BasicArrowButton(SwingConstants.SOUTH);
        m_bottomControl.add(b);

        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_eventService.publish(new ViewerScrollEvent(Direction.SOUTH));

            }

        });

        gbc.gridx = 3;
        gbc.gridy = 0;

        m_topControl = new JPanel();
        m_topControl.setLayout(new BorderLayout());
        m_background.add(m_topControl, gbc);

        BasicArrowButton t = new BasicArrowButton(SwingConstants.NORTH);
        m_topControl.add(t);

        t.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_eventService.publish(new ViewerScrollEvent(Direction.NORTH));

            }

        });

        // Overview and quickview panel

        gbc.gridx = 0;
        gbc.gridy = 6;

        gbc.weightx = 0;
        gbc.weighty = 0;

        ImageIcon i = new ImageIcon(this.getClass().getResource("/icons/tableup.png"));
        ImageIcon i2 = new ImageIcon(this.getClass().getResource("/icons/tabledown.png"));

        m_bottomQuickViewButton =
                new JToggleButton(new ImageIcon(i.getImage().getScaledInstance(32, 16, java.awt.Image.SCALE_SMOOTH)));
        m_bottomQuickViewButton
                .setSelectedIcon(new ImageIcon(i2.getImage().getScaledInstance(32, 16, java.awt.Image.SCALE_SMOOTH)));
        m_bottomQuickViewButton.setMnemonic(KeyEvent.VK_Q);

        m_background.add(m_bottomQuickViewButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 0;

        m_bottomQuickViewButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_eventService.publish(new ViewerToggleEvent());
            }

        });

        JPanel overviewButtonPanel = new JPanel();
        overviewButtonPanel.setLayout(new BorderLayout());
        i = new ImageIcon(this.getClass().getResource("/icons/backarrow.png"));
        if (!simplified) {
            m_overviewButton =

            new JButton(new ImageIcon(i.getImage().getScaledInstance(32, 16, java.awt.Image.SCALE_SMOOTH)));
            m_overviewButton.setMnemonic(KeyEvent.VK_B);
            overviewButtonPanel.add(m_overviewButton, BorderLayout.CENTER);
            m_background.add(overviewButtonPanel, gbc);

            m_overviewButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    m_eventService.publish(new ViewerControlEvent());

                }

            });
        }

        add(m_background);

    }

    /**
     * Adds the panel
     */
    @Override
    public void addViewerComponent(final ViewerComponent panel) {
        addViewerComponent(panel, true);

    }

    /**
     * Adds the {@link ViewerComponent} to the {@link MissingImgViewer}
     *
     * @param panel {@link ViewerComponent} to be set
     *
     * @param setEventService indicates weather the {@link EventService} of the {@link MissingImgViewer} shall be set to the
     *            {@link ViewerComponent}
     *
     */
    @Override
    public void addViewerComponent(final ViewerComponent panel, final boolean setEventService) {

        if (setEventService) {
            panel.setEventService(m_eventService);
        }

        m_viewerComponents.add(panel);

        switch (panel.getPosition()) {
            case CENTER:
                m_centerPanel.add(panel);
                break;
            case SOUTH: // BTMCONTROL
                m_bottomControl.add(panel);
                m_controls[0] = panel;
                break;
            case WEST: // LFTCONTROL
                m_leftControl.add(panel);
                m_controls[1] = panel;
                break;
            case EAST:
                m_rightControl.add(panel);
                m_controls[2] = panel;
                break;
            case NORTH:
                m_topControl.add(panel);
                m_controls[3] = panel;
                break;
            case INFO: // CONTROL
                m_infoPanel.add(panel);
                break;
            case ADDITIONAL: // ADDITIONAL
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(5, 0, 5, 0);
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.anchor = GridBagConstraints.CENTER;
                gbc.gridx = 0;
                gbc.gridy = m_currentSlot++;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridheight = 1;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                m_rightPanel.add(panel, gbc);

                break;
            default: // hidden

        }

    }

    /**
     * Ensures a valid layout. Should be called after all components are added to the Position.ADDITIONAL slot.
     */
    @Override
    public void doneAdding() {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = m_currentSlot;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        Component panel = Box.createVerticalGlue();
        m_rightPanel.add(panel, gbc);
    }

    private void addControls() {

    }


    /**
     * @return the event service used in this particular viewer (e.g. to subscribe other listeners)
     */
    @Override
    public EventService getEventService() {
        return m_eventService;
    }

}
