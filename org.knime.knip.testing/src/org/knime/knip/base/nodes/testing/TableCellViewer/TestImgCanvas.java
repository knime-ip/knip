/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2014
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
 * Created on 04.03.2014 by Andreas
 */
package org.knime.knip.base.nodes.testing.TableCellViewer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.Type;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.imgviewer.ImgCanvas;
import org.knime.knip.core.ui.imgviewer.events.MinimapOffsetChgEvent;

/**
 * This class is a small extension of the {@link ImgCanvas} class, listening to an additional event-type.
 *
 * @author Andreas Burger, University of Konstanz
 * @param <T>
 */
public class TestImgCanvas<T extends Type<T>, I extends RandomAccessibleInterval<T>> extends
        ImgCanvas<T, I> {

    int m_xOffset = 0;
    int m_yOffset = 0;

    /**
     *
     */
    private static final long serialVersionUID = -3376351028351404404L;

    /**
     * Scrolls the image so the rectangle gets visible.
     *
     * @param rect
     */
    @Override
    @EventListener
    public void onMinimapOffsetChanged(final MinimapOffsetChgEvent e) {
        m_currentRectangle = m_imageCanvas.getVisibleRect();
        m_xOffset = (int)(e.getOffest()[0] * m_factors[0]);
        m_yOffset = (int)(e.getOffest()[1] * m_factors[1]);
        m_currentRectangle.x = m_xOffset;
        m_currentRectangle.y = m_yOffset;
        updateImageCanvas();
    }

    /**
     * Called whenever a TestCompleteEvent arrives. This method causes the TestImgCanvas to emit a
     * {@link TestImageEvent} containing the currently displayed image.
     *
     * @param e The received event.
     */
    @EventListener
    public void onTestComplete(final TestCompleteEvent e) {

        int width = m_imageCanvas.getWidth();
        int height = m_imageCanvas.getHeight();
        // Image size needs to be checked/limited.
        if (width == 0) {
            width = 1;
        }
        if(width > 200) {
            width = 200;
        }

        if (height == 0) {
            height = 1;
        }
        if(height > 200) {
            height = 200;
        }
        final BufferedImage currImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        //Note: Workaround, does not seem to work with currImage.getGraphics();
        Graphics2D g = currImage.createGraphics();
        // Actual calculation not viable due to SWT
        g.translate(-m_xOffset, -m_yOffset);
        m_imageCanvas.paint(g);

        m_eventService.publish(new TestImageEvent(currImage));
    }

}
