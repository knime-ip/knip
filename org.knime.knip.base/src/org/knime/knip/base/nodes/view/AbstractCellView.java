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
 * Created on May 29, 2015 by pop210958
 */
package org.knime.knip.base.nodes.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;

import org.knime.core.node.tableview.TableView;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.event.EventServiceClient;
import org.knime.knip.core.ui.event.KNIPEvent;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.events.BackgroundColorChangedEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.panels.TableOverviewPanel;
import org.knime.knip.core.ui.imgviewer.panels.ViewerControlEvent;
import org.knime.knip.core.ui.imgviewer.panels.ViewerScrollEvent;

/**
 *
 * @author pop210958
 */
public abstract class AbstractCellView extends JPanel implements EventServiceClient {

    protected JPanel m_tablePanel;

    protected JPanel m_cellPanel;

    protected final TableView m_tableView;

    private boolean m_isBottomVisible = false;

    private boolean m_isLeftVisible = false;

    protected JSplitPane m_verticalSplit;

    protected EventService m_eventService;

    protected Set<EventService> m_contentServices;

    protected TableOverviewPanel m_navPanel;

    protected Color m_bgColour = Color.black;

    private JToggleButton m_bottomQuickViewButton;

    public AbstractCellView(final TableView tableView) {

        setLayout(new BorderLayout());

        m_tableView = tableView;

        m_contentServices = new HashSet<EventService>();

        m_verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        add(m_verticalSplit, BorderLayout.CENTER);

        m_verticalSplit.setTopComponent(createViewPanel());

        m_tablePanel = new JPanel(new BorderLayout());
        //        m_verticalSplit.setBo

    }

    private JPanel createViewPanel() {
        JPanel viewPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.gridheight = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;

        m_cellPanel = new JPanel(new BorderLayout());

        viewPanel.add(m_cellPanel, gbc);

        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 1;

        viewPanel.add(createNavBar(), gbc);

        return viewPanel;
    }

    private JComponent createNavBar() {
        Box navbar = new Box(BoxLayout.X_AXIS);

        navbar.add(Box.createRigidArea(new Dimension(10, 40)));

        Box firstPanel = new Box(BoxLayout.X_AXIS);

        JButton overviewButton =
                new JButton("Back to Table");
        overviewButton.setMnemonic(KeyEvent.VK_B);
        firstPanel.add(overviewButton);
        //        overviewButtonPanel.add(Box.createHorizontalGlue());

        overviewButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_eventService.publish(new ViewerControlEvent());
                m_bottomQuickViewButton.setSelected(false);

            }

        });

        firstPanel.add(Box.createHorizontalStrut(20));
        JLabel expandLabel = new JLabel("Expand Table View ");
        firstPanel.add(expandLabel);
        Box quickViewButtonPanel = new Box(BoxLayout.X_AXIS);
        //      py  quickViewButtonPanel.add(Box.createHorizontalGlue());
        ImageIcon i = new ImageIcon(this.getClass().getResource("/icons/tableup.png"));
        ImageIcon i2 = new ImageIcon(this.getClass().getResource("/icons/tabledown.png"));

        m_bottomQuickViewButton =
                new JToggleButton(new ImageIcon(i.getImage().getScaledInstance(32, 16, java.awt.Image.SCALE_SMOOTH)));
        m_bottomQuickViewButton
                .setSelectedIcon(new ImageIcon(i2.getImage().getScaledInstance(32, 16, java.awt.Image.SCALE_SMOOTH)));
        m_bottomQuickViewButton.setMnemonic(KeyEvent.VK_Q);

        quickViewButtonPanel.add(m_bottomQuickViewButton);
        //        quickViewButtonPanel.add(Box.createHorizontalGlue());
        firstPanel.add(quickViewButtonPanel);

        firstPanel.add(Box.createHorizontalStrut(20));

        Box colourButtonPanel = new Box(BoxLayout.X_AXIS);

        JLabel colourLabel = new JLabel("Background Color ");
        firstPanel.add(colourLabel);

        JButton colourButton  =
                new JButton(new ColorIcon(m_bgColour));

        colourButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                Color c = JColorChooser.showDialog(m_cellPanel, "Select Background Color", m_bgColour);
                if(c != null){
                    m_bgColour = c;
                    colourButton.setIcon(new ColorIcon(c));
                    broadcastEvent(new BackgroundColorChangedEvent(c));
                }



            }
        });


        colourButtonPanel.add(colourButton);
        //        quickViewButtonPanel.add(Box.createHorizontalGlue());
        firstPanel.add(colourButtonPanel);

        navbar.add(firstPanel);

        navbar.add(Box.createHorizontalGlue());

        m_bottomQuickViewButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                setTableViewVisible(!isTableViewVisible());
            }

        });

        m_navPanel = new TableOverviewPanel();
        navbar.add(m_navPanel);
        navbar.add(Box.createHorizontalGlue());

        Box thirdPanel = new Box(BoxLayout.X_AXIS);
        thirdPanel.add(Box.createHorizontalGlue());

        navbar.add(thirdPanel);

        return navbar;
    }

    /**
     * Sets the visibility of the TableView given by its location in the View.
     *
     * @param isVisible - boolean specifying the desired visibility state
     * @param d The location of the table whose visibility will be set
     */
    public void setTableViewVisible(final boolean isVisible) {
        m_tablePanel.add(m_tableView, BorderLayout.CENTER);
        if (isVisible) {
            m_isLeftVisible = !isVisible;
            showTableView();
        } else {
            closeTableView();
        }
        m_isBottomVisible = isVisible;

        validate();
    }

    private void closeTableView() {

        if (m_isBottomVisible) {
            m_verticalSplit.remove(2);
        }

    }

    private void showTableView() {
        m_verticalSplit.setBottomComponent(m_tablePanel);
        m_verticalSplit.setDividerLocation(this.getHeight() - (m_tableView.getColumnHeaderViewHeight()
                + m_tableView.getRowHeight() + m_verticalSplit.getDividerSize() + 4));
    }

    /**
     * Sets the visibility of both TableViews.
     *
     * @param isVisible - boolean specifying the desired visibility state
     * @param d The location of the table whose visibility will be set
     */
    public void hideTableView() {
        closeTableView();
        m_isBottomVisible = false;

        validate();
    }

    public void scrollTablesToIndex(final int i, final int j) {
        m_tableView.getContentTable().changeSelection(i, j, false, false);

    }

    public boolean isTableViewVisible() {
        return m_isBottomVisible;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
        m_navPanel.setEventService(eventService);

    }

    public void subscribe(final Component c) {
        if (c instanceof ImgViewer) {
            EventService es = ((ImgViewer)c).getEventService();
            m_contentServices.add(es);
        }
    }

    public void broadcastEvent(final KNIPEvent e) {
        for (EventService es : m_contentServices) {
            es.publish(e);
        }
    }

    //TODO: Inheritance

    @EventListener
    public void onPlaneSelectionChanged(final PlaneSelectionEvent e) {
        m_eventService.publish(e);
    }

    @EventListener
    public void onViewerImgChange(final ViewerScrollEvent e) {
        if(m_eventService != null) {
            m_eventService.publish(new ViewerScrollEvent(e));
        }
    }

    @EventListener
    public void onViewerOverviewToggle(final ViewerControlEvent e) {
        if(m_eventService != null) {
            m_eventService.publish(e);
        }
    }

    private static class ColorIcon implements Icon
    {
        private final int size = 16;

        private final Color color;

        public ColorIcon( final Color color )
        {
            this.color = color;
        }

        @Override
        public void paintIcon( final Component c, final Graphics g, final int x, final int y )
        {
            final Graphics2D g2d = ( Graphics2D ) g;
            g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
            g2d.setColor( color );
            g2d.fillOval( x, y, size, size );
        }

        @Override
        public int getIconWidth()
        {
            return size;
        }

        @Override
        public int getIconHeight()
        {
            return size;
        }
    }



}