package org.knime.knip.core.ui.imgviewer.panels;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.plaf.metal.MetalToolTipUI;

/**
 * This class represents a ToolTip able to show images.
 *
 * @author Andreas Burger, University of Konstanz
 */
class ImageToolTip extends JToolTip {

    public ImageToolTip(final Image img) {
        setUI(new ImageToolTipUI(img));
        this.setToolTipText("");
    }
}

class ImageToolTipUI extends MetalToolTipUI {

    Image m_img;

    public ImageToolTipUI(final Image img)
    {
        m_img = img;
    }

    @Override
    public void paint(final Graphics g, final JComponent c) {
        g.drawImage(m_img, 1, 1, c);
    }

    @Override
    public Dimension getPreferredSize(final JComponent c) {
        return new Dimension(m_img.getWidth(null), m_img.getHeight(null));
    }
}