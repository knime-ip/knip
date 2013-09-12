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
package org.knime.knip.base.nodes.misc.dimswap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.base.KNIMEKNIPPlugin;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author fschoenenberger, University of Konstanz
 */
public class DialogComponentDimensionSwappingSelect extends DialogComponent {

    private static final Color BLUE = new Color(160, 160, 255);

    private static final int ROUNDED = 8;

    private static final Color YELLOW = new Color(255, 255, 160);

    private final JPanel m_canvas;

    private final int[] m_dimlut;

    private int m_dragIndex;

    private Point m_dragPoint;

    private int m_dropIndex;

    private final JTextField[] m_jtfOffsets;

    private final JTextField[] m_jtfSizes;

    private int m_rectSize;

    private int m_width;

    public DialogComponentDimensionSwappingSelect(final SettingsModelDimensionSwappingSelect model) {
        super(model);
        m_dimlut = new int[model.getNumDimensions()];
        m_dragIndex = -1;

        getComponentPanel().setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;

        m_canvas = new JPanel() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void paint(final Graphics g) {
                super.paint(g);
                m_width = m_canvas.getWidth();
                m_rectSize = m_canvas.getHeight() / m_dimlut.length;
                final int tri = m_rectSize / 4;
                // AxisType[] dimLabels = KNIMEKNIPPlugin
                // .parseDimensionLabelsAsAxis();
                // TODO: use dim labels to map the respective
                // dimension index
                // here: swapping using the dimension index
                // directly
                final String[] dimLabels = new String[KNIMEKNIPPlugin.parseDimensionLabels().length];
                for (int i = 0; i < dimLabels.length; i++) {
                    dimLabels[i] = i + "         ";
                }

                for (int i = 0; i < m_dimlut.length; i++) {
                    // Left rectangle
                    g.setColor(YELLOW);
                    g.fillRoundRect(0, i * m_rectSize, m_rectSize, m_rectSize, ROUNDED, ROUNDED);
                    g.setColor(Color.BLACK);
                    g.drawRoundRect(0, i * m_rectSize, m_rectSize, m_rectSize, ROUNDED, ROUNDED);
                    g.drawLine(m_rectSize, ((i * m_rectSize) + (m_rectSize / 2)) - (tri / 2), m_rectSize + tri,
                               (i * m_rectSize) + (m_rectSize / 2));
                    g.drawLine(m_rectSize, (i * m_rectSize) + (m_rectSize / 2) + (tri / 2), m_rectSize + tri,
                               (i * m_rectSize) + (m_rectSize / 2));
                    g.drawString(dimLabels[i], 5, ((i + 1) * m_rectSize) - 5);
                    // Right rectangle
                    g.setColor(BLUE);
                    g.fillRoundRect(m_width - (2 * m_rectSize) - 1, i * m_rectSize, 2 * m_rectSize, m_rectSize,
                                    ROUNDED, ROUNDED);
                    g.setColor(Color.BLACK);
                    g.drawRoundRect(m_width - (2 * m_rectSize) - 1, i * m_rectSize, 2 * m_rectSize, m_rectSize,
                                    ROUNDED, ROUNDED);
                    g.drawLine(m_width - (2 * m_rectSize) - tri - 1, ((i * m_rectSize) + (m_rectSize / 2)) - (tri / 2),
                               m_width - (2 * m_rectSize) - 1, (i * m_rectSize) + (m_rectSize / 2));
                    g.drawLine(m_width - (2 * m_rectSize) - tri - 1, (i * m_rectSize) + (m_rectSize / 2) + (tri / 2),
                               m_width - (2 * m_rectSize) - 1, (i * m_rectSize) + (m_rectSize / 2));
                    g.drawLine(m_width - (2 * m_rectSize) - tri - 1, (i * m_rectSize) + (m_rectSize / 2) + (tri / 2),
                               m_width - (2 * m_rectSize) - tri - 1, ((i * m_rectSize) + (m_rectSize / 2)) - (tri / 2));
                }
                for (int i = 0; i < m_dimlut.length; i++) {
                    // Connection
                    if (i != m_dragIndex) {
                        g.drawLine(m_rectSize + tri, (i * m_rectSize) + (m_rectSize / 2), m_width - (2 * m_rectSize)
                                - tri - 1, (m_dimlut[i] * m_rectSize) + (m_rectSize / 2));
                    } else {
                        if (m_dropIndex != -1) {
                            g.drawLine(m_rectSize + tri, (i * m_rectSize) + (m_rectSize / 2), m_width
                                    - (2 * m_rectSize) - tri - 1, (m_dropIndex * m_rectSize) + (m_rectSize / 2));
                        } else {
                            g.drawLine(m_rectSize + tri, (i * m_rectSize) + (m_rectSize / 2), m_dragPoint.x,
                                       m_dragPoint.y);
                        }
                    }
                    // Label
                    g.drawString(dimLabels[i] + ">" + dimLabels[m_dimlut[i]], (m_width - (2 * m_rectSize)) + 5,
                                 ((m_dimlut[i] + 1) * m_rectSize) - 5);
                }
            }
        };
        m_canvas.setBackground(Color.WHITE);
        m_canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.getX() < m_rectSize) {
                    m_dragPoint = e.getPoint();
                    m_dragIndex = e.getY() / m_rectSize;
                    m_dropIndex = -1;
                    m_canvas.repaint();
                }
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                if (m_dragIndex > -1) {
                    m_dropIndex = e.getY() / m_rectSize;
                    final int freeIndex = m_dimlut[m_dragIndex];
                    for (int i = 0; i < m_dimlut.length; i++) {
                        if (m_dimlut[i] == m_dropIndex) {
                            m_dimlut[i] = freeIndex;
                            break;
                        }
                    }
                    m_dimlut[m_dragIndex] = m_dropIndex;
                    m_dragIndex = -1;
                    m_dropIndex = -1;
                    m_canvas.repaint();
                }
            }
        });
        m_canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                m_dragPoint = e.getPoint();
                if (e.getX() > (m_width - (2 * m_rectSize))) {
                    m_dropIndex = e.getY() / m_rectSize;
                } else {
                    m_dropIndex = -1;
                }
                m_canvas.repaint();
            }
        });
        final Dimension d = new Dimension(100, (model.getNumDimensions() * 20) + 1);
        m_canvas.setPreferredSize(d);
        m_canvas.setMaximumSize(d);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 5);
        getComponentPanel().add(m_canvas, gbc);

        JPanel jp = new JPanel(new GridLayout(1, 2, 0, 2));
        jp.add(new JLabel("Offset"));
        jp.add(new JLabel("Size"));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        getComponentPanel().add(jp, gbc);

        jp = new JPanel(new GridLayout(model.getNumDimensions(), 2, 0, 2));
        m_jtfOffsets = new JTextField[model.getNumDimensions()];
        m_jtfSizes = new JTextField[model.getNumDimensions()];
        for (int i = 0; i < model.getNumDimensions(); i++) {
            m_jtfOffsets[i] = new JTextField();
            jp.add(m_jtfOffsets[i]);
            m_jtfSizes[i] = new JTextField();
            m_jtfSizes[i].addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(final FocusEvent e) {
                    final JTextField jtf = (JTextField)e.getSource();
                    if (jtf.getText().equals("all")) {
                        jtf.setText("");
                    }
                    jtf.setForeground(Color.BLACK);
                }

                @Override
                public void focusLost(final FocusEvent e) {
                    final JTextField jtf = (JTextField)e.getSource();
                    try {
                        jtf.setText(String.valueOf(Integer.parseInt(jtf.getText())));
                        jtf.setForeground(Color.BLACK);
                    } catch (final NumberFormatException e1) {
                        jtf.setText("all");
                        jtf.setForeground(Color.GRAY);
                    }
                }
            });
            jp.add(m_jtfSizes[i]);
        }
        gbc.weightx = 1;
        gbc.gridx = 1;
        gbc.gridy = 1;
        getComponentPanel().add(jp, gbc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final SettingsModelDimensionSwappingSelect model = (SettingsModelDimensionSwappingSelect)getModel();
        for (int i = 0; i < model.getNumDimensions(); i++) {
            m_dimlut[i] = model.getFwdDimensionLookup(i);
            m_jtfOffsets[i].setText(String.valueOf(model.getOffset(i)));
            if (model.getSize(i) < 0) {
                m_jtfSizes[i].setText("all");
                m_jtfSizes[i].setForeground(Color.GRAY);
            } else {
                m_jtfSizes[i].setText(String.valueOf(model.getSize(i)));
                m_jtfSizes[i].setForeground(Color.BLACK);
            }
        }
        m_canvas.repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        final SettingsModelDimensionSwappingSelect model = (SettingsModelDimensionSwappingSelect)getModel();
        for (int i = 0; i < model.getNumDimensions(); i++) {
            model.setFwdDimensionLookup(m_dimlut[i], i);
            try {
                model.setOffset(Integer.parseInt(m_jtfOffsets[i].getText()), i);
            } catch (final NumberFormatException e) {
                m_jtfOffsets[i].setText("0");
                model.setOffset(0, i);
            }
            try {
                model.setSize(Integer.parseInt(m_jtfSizes[i].getText()), i);
                m_jtfSizes[i].setForeground(Color.BLACK);
            } catch (final NumberFormatException e) {
                m_jtfSizes[i].setText("all");
                m_jtfSizes[i].setForeground(Color.GRAY);
                model.setSize(-1, i);
            }
        }
    }
}
