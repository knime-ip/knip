package org.knime.knip.core.ui.imgviewer;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;

import org.knime.knip.core.ui.event.EventService;

public class ExpandingPanel extends ViewerComponent {

    private JComponent m_header;

    private ViewerComponent m_content;

    private boolean isExpanded = false;

    public ExpandingPanel(final String name, final ViewerComponent content) {
        this(name, content, false);

    }

    public ExpandingPanel(final String name, final ViewerComponent content, final boolean startExpanded) {
        super("", true);

        isExpanded = startExpanded;
        int direction = isExpanded ? SwingConstants.SOUTH : SwingConstants.EAST;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.NORTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.weighty = 0;

        m_header = Box.createHorizontalBox();
        final BasicArrowButton arrow = new BasicArrowButton(direction) {

            /**
             * {@inheritDoc}
             */
            @Override
            public Dimension getMaximumSize() {
                // TODO Auto-generated method stub
                return new Dimension(16, 16);
            }

        };
        //	m_header.setLayout(new BoxLayout(m_header, BoxLayout.X_AXIS));
        //		m_header.add(Box.createHorizontalGlue());

        m_header.add(getDividerPanel());
        m_header.add(Box.createHorizontalStrut(2));
        m_header.add(new JLabel(name, SwingConstants.CENTER));
        m_header.add(Box.createHorizontalStrut(10));
        m_header.add(arrow);

        m_header.add(Box.createHorizontalStrut(2));
        m_header.add(getDividerPanel());

        //m_header.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        m_header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 15));
        //		m_header.setMinimumSize(new Dimension(100, 15));
        add(m_header, gbc);
        MouseListener ml = new MouseListener() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseEntered(final MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseExited(final MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mousePressed(final MouseEvent e) {
                m_content.setVisible(!m_content.isVisible());
                isExpanded = !isExpanded;
                if (isExpanded) {
                    arrow.setDirection(SwingConstants.SOUTH);
                } else {
                    arrow.setDirection(SwingConstants.EAST);
                }
                ExpandingPanel.super.validate();
                ExpandingPanel.super.repaint();

            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                // TODO Auto-generated method stub

            }
        };
        m_header.addMouseListener(ml);
        arrow.addMouseListener(ml);

        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.weighty = 1;

        m_content = content;
        m_content.setVisible(isExpanded);
        add(content, gbc);
        setVisible(true);

    }

    /**
     * @return
     */
    private JPanel getDividerPanel() {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(sep, gbc);
        return p;
    }

    @Override
    public Dimension getMaximumSize() {
        if (isExpanded) {
            int h = m_content.getMaximumSize().height + m_header.getMaximumSize().height + 10;
            int w = Integer.MAX_VALUE;
            return new Dimension(w, h);
        } else {
            return m_header.getMaximumSize();
        }
    }

    @Override
    public Dimension getMinimumSize() {
        if (isExpanded) {
            int h = m_content.getMinimumSize().height + m_header.getMinimumSize().height + 10;
            int w = getPreferredSize().width;
            return new Dimension(w, h);
        } else {
            return m_header.getMinimumSize();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (isExpanded) {
            int h = m_content.getPreferredSize().height + m_header.getPreferredSize().height + 10;
            int w = getLayout().preferredLayoutSize(this).width;//super.getPreferredSize().width;
            return new Dimension(w, h);
        } else {
            return m_header.getPreferredSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {
        m_content.setEventService(eventService);

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
        m_content.saveComponentConfiguration(out);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_content.loadComponentConfiguration(in);

    }

}
