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
 * Created on 25.02.2014 by Andreas
 */
package org.knime.knip.base.nodes.testing.TableCellViewer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedList;
import java.util.List;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;

/**
 *
 * @author Andreas
 */
public class HiddenImageLogger extends ViewerComponent {

    private EventService m_eventService;

    private BufferedImage m_image;

    private List<BufferedImage> m_collectedImages;

    /**
     * @param title
     * @param isBorderHidden
     */
    public HiddenImageLogger(final String title, final boolean isBorderHidden) {
        super(title, isBorderHidden);
        m_collectedImages = new LinkedList<BufferedImage>();
    }

    public HiddenImageLogger()
    {
        this("", false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
        eventService.subscribe(this);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position getPosition() {
        // TODO Auto-generated method stub
        return Position.HIDDEN;
    }

    @EventListener
    public void onTestImageEvent(final TestImageEvent e) {
        // Keep track of most recent image
        m_collectedImages.add(e.getImage());
    }

//    @EventListener
//    public void onTestComplete(final TestCompleteEvent e) {
//        m_collectedImages.add(m_image);
//    }

    public List<BufferedImage> getImages()
    {
        return m_collectedImages;
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
