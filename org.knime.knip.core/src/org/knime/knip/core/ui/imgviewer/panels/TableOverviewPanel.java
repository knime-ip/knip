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
 * Created on Jul 16, 2015 by pop210958
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
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.TableOverviewDisableEvent;
import org.knime.knip.core.ui.imgviewer.events.TablePositionEvent;
import org.knime.knip.core.ui.imgviewer.panels.ViewerScrollEvent.Direction;

/**
 *
 * @author pop210958
 */
public class TableOverviewPanel extends ViewerComponent {

    private EventService m_eventService;

    private int m_x = 0;

    private int m_y = 0;

    private Box m_northPanel;

    private Box m_eastPanel;

    private Box m_southPanel;

    private Box m_westPanel;

    private JButton m_northButton;

    private JButton m_eastButton;

    private JButton m_southButton;

    private JButton m_westButton;

    private int m_height;

    private int m_width;

    private JLabel m_coordLabel;

    private JLabel m_colLabel;

    private JLabel m_rowLabel;

    /**
     * @param title
     * @param isBorderHidden
     */
    public TableOverviewPanel() {
        super("", true);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        //        JPanel comp = new JPanel();
        //        comp.setLayout(new BorderLayout());

        setUpPanels();

        Box columnBox = new Box(BoxLayout.X_AXIS);

        m_colLabel = new JLabel("Column (0/0): ");
        columnBox.add(m_colLabel);
        columnBox.add(Box.createVerticalStrut(20));
        columnBox.add(Box.createHorizontalStrut(5));
        columnBox.add(m_westPanel);
        columnBox.add(Box.createHorizontalStrut(5));
        columnBox.add(m_eastPanel);

        add(columnBox);
        add(Box.createHorizontalStrut(20));

        Box rowBox = new Box(BoxLayout.X_AXIS);
        rowBox.add(Box.createVerticalStrut(20));
        m_rowLabel = new JLabel("Row (0/0): ");
        rowBox.add(m_rowLabel);
        rowBox.add(Box.createHorizontalStrut(5));
        rowBox.add(m_northPanel);
        rowBox.add(Box.createHorizontalStrut(5));
        rowBox.add(m_southPanel);

        add(rowBox);

        //        add(Box.createHorizontalStrut(20));
        //
        //        Box coordBox = new Box(BoxLayout.X_AXIS);
        //        m_coordLabel = new JLabel("(" + m_x + "," + m_y + ")");
        //        coordBox.add(m_coordLabel);
        //
        //        add(coordBox);

        setMaximumSize(new Dimension(100, 200));
        //        setMinimumSize(new Dimension(200, 200));
        //        setPreferredSize(new Dimension(250, 200));
        validate();
    }

    public TableOverviewPanel(final Boolean... isActive) {
        this();
        for (int i = 0; i < 4; ++i) {
            if (isActive.length > i && !isActive[i]) {
                if (i == 0) {
                    m_northPanel.removeAll();
                }
                if (i == 1) {
                    m_eastPanel.removeAll();
                }
                if (i == 2) {
                    m_westPanel.removeAll();
                }
                if (i == 3) {
                    m_southPanel.removeAll();
                }
            }
        }
    }

    private void setUpPanels() {

        m_northPanel = Box.createHorizontalBox();

        m_northButton = new BasicArrowButton(SwingConstants.NORTH) {

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(20, 20);
            }
        };
        m_northButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_eventService.publish(new ViewerScrollEvent(Direction.NORTH));

            }
        });
        m_northButton.setAlignmentY(0.5f);
        m_northPanel.add(m_northButton);

        m_southPanel = Box.createHorizontalBox();

        m_southButton = new BasicArrowButton(SwingConstants.SOUTH) {

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(20, 20);
            }
        };
        m_southButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_eventService.publish(new ViewerScrollEvent(Direction.SOUTH));

            }
        });

        m_southButton.setAlignmentY(0.5f);
        m_southPanel.add(m_southButton);

        m_eastPanel = Box.createVerticalBox();
        m_eastButton = new BasicArrowButton(SwingConstants.EAST) {

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(20, 20);
            }
        };
        m_eastButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_eventService.publish(new ViewerScrollEvent(Direction.EAST));

            }
        });
        m_eastButton.setAlignmentX(0.5f);
        m_eastPanel.add(m_eastButton);

        m_westPanel = Box.createVerticalBox();
        m_westButton = new BasicArrowButton(SwingConstants.WEST) {

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(20, 20);
            }
        };
        m_westButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_eventService.publish(new ViewerScrollEvent(Direction.WEST));

            }
        });
        m_westButton.setAlignmentX(0.5f);
        m_westPanel.add(m_westButton);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
        m_eventService.subscribe(this);

    }

    @EventListener
    public void onTablePositionEvent(final TablePositionEvent e) {
        if (e.getwidth() != -1 || e.getx() != -1) {
            if (e.getheight() != -1 || e.gety() != -1) {
                setButtonStatus(true, true);
            } else {
                setButtonStatus(true, false);
            }
        } else {
            if (e.getheight() != -1 || e.gety() != -1) {
                setButtonStatus(false, true);
            } else {
                setButtonStatus(false, false);
            }
        }
        m_height = e.getheight();
        m_width = e.getwidth();
        m_x = e.getx();
        m_y = e.gety();

        if (m_x != -1 || m_width != -1) {
            m_colLabel.setText("Column (" + m_x + "/" + m_width + ")");
        }
        if (m_y != -1 || m_height != -1) {
            m_rowLabel.setText("Row (" + m_y + "/" + m_height + ")");
            //        m_coordLabel.setText("(" + m_x + "," + m_y + ")");
        }
    }

    @EventListener
    public void onDisableEvent(final TableOverviewDisableEvent e) {
        if (!e.getColStatus()) {
            m_colLabel.setText("");
        }
        if (!e.getRowStatus()) {
            m_rowLabel.setText("");
        }
        setButtonStatus(e.getColStatus(), e.getRowStatus());

    }

    private void setButtonStatus(final boolean columnStatus, final boolean rowStatus) {
        m_northButton.setEnabled(rowStatus);
        m_eastButton.setEnabled(columnStatus);
        m_westButton.setEnabled(columnStatus);
        m_southButton.setEnabled(rowStatus);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position getPosition() {
        // TODO Auto-generated method stub
        return Position.ADDITIONAL;
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
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        // TODO Auto-generated method stub

    }

}
