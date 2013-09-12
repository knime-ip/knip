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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.imglib2.type.Type;

import org.knime.knip.core.awt.ImageRenderer;
import org.knime.knip.core.awt.RendererFactory;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.RendererSelectionChgEvent;

/**
 * Allows the user to select a certain renderer.
 * 
 * Publishes {@link RendererSelectionChgEvent}
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class RendererSelectionPanel<T extends Type<T>> extends ViewerComponent {

    private static final long serialVersionUID = 1L;

    private JList m_rendList;

    private EventService m_eventService;

    private boolean m_blockEvent = false;

    public RendererSelectionPanel() {

        super("Renderering", false);

        // renderer selection
        setPreferredSize(new Dimension(200, getMinimumSize().height));
        setMaximumSize(new Dimension(250, getMaximumSize().height));
        setMinimumSize(new Dimension(100, getMinimumSize().height));
        setLayout(new BorderLayout());

        m_rendList = new JList();
        m_rendList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        m_rendList.setSelectedIndex(0);

        m_rendList.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(final ListSelectionEvent e) {
                if (e.getValueIsAdjusting() || m_blockEvent) {
                    return;
                } else {
                    m_eventService.publish(new RendererSelectionChgEvent((ImageRenderer)m_rendList.getSelectedValue()));
                    m_eventService.publish(new ImgRedrawEvent());
                }

            }
        });

        add(new JScrollPane(m_rendList), BorderLayout.CENTER);
    }

    /**
     * @param axes
     * @param name
     */
    @EventListener
    public void onIntervalUpdated(final IntervalWithMetadataChgEvent<T> e) {
        if (e instanceof ImgWithMetadataChgEvent) {
            // event already processed
            return;
        }
        final ImageRenderer<T>[] tmp = RendererFactory.createSuitableRenderer(e.getRandomAccessibleInterval());

        m_blockEvent = true;
        m_rendList.setListData(tmp);
        m_rendList.repaint();
        m_blockEvent = false;

    }

    @EventListener
    public void onImageUpdated(final ImgWithMetadataChgEvent<T> e) {
        final ImageRenderer<T>[] tmp =
                RendererFactory.createSuitableRenderer(e.getRandomAccessibleInterval(), e.getImgMetaData());

        m_blockEvent = true;
        m_rendList.setListData(tmp);
        m_rendList.repaint();
        m_blockEvent = false;
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
        out.writeInt(m_rendList.getSelectedIndex());
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_rendList.setSelectedIndex(in.readInt());
    }
}
