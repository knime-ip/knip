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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.knime.knip.core.awt.ImageRenderer;
import org.knime.knip.core.awt.RendererFactory;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.ImageAndLabelingRendererEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.RendererSelectionChgEvent;
import org.knime.knip.core.ui.imgviewer.panels.providers.ImageRU;
import org.knime.knip.core.ui.imgviewer.panels.providers.LabelingRU;

import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.Type;

/**
 * Allows the user to select a certain renderer.
 *
 * Publishes {@link RendererSelectionChgEvent}
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DualRendererSelectionPanel<T extends Type<T>> extends ViewerComponent {

    private static final long serialVersionUID = 1L;

    private JComboBox m_rendListOne;

    private JComboBox m_rendListTwo;

    private ImageRU m_imageRU;

    private LabelingRU m_labelingRU;

    private EventService m_eventService;

    private boolean m_blockEvent = false;

    private ImageRenderer m_currentSelectionOne;

    private ImageRenderer m_currentSelectionTwo;

    public DualRendererSelectionPanel(final ImageRU imgRu, final LabelingRU labelRu) {

        //super("Rendering", false);
        super("", true);
        // renderer selection

        m_labelingRU = labelRu;
        m_imageRU = imgRu;

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;

        m_rendListOne = new JComboBox();
        m_rendListTwo = new JComboBox();

        //m_rendList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        //        m_rendList.addaddSelectionListener(new ListSelectionListener() {
        //
        //            @Override
        //            public void valueChanged(final ListSelectionEvent e) {
        //                if (e.getValueIsAdjusting() || m_blockEvent) {
        //                    return;
        //                } else {
        //                    m_eventService.publish(new RendererSelectionChgEvent((ImageRenderer)m_rendList.getSelectedValue()));
        //                    m_eventService.publish(new ImgRedrawEvent());
        //                }
        //
        //            }
        //        });

        m_rendListOne.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED || m_blockEvent) {
                    return;
                } else {
                    ImageRenderer selected = (ImageRenderer)m_rendListOne.getSelectedItem();
                    m_currentSelectionOne = selected;
                    m_imageRU.onRendererUpdate(new RendererSelectionChgEvent(selected));
                    m_imageRU.enableColourRendering();
                    m_eventService.publish(new ImgRedrawEvent());
                }

            }
        });

        m_rendListTwo.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED || m_blockEvent) {
                    return;
                } else {
                    ImageRenderer selected = (ImageRenderer)m_rendListTwo.getSelectedItem();
                    m_currentSelectionOne = selected;
                    m_labelingRU.onRendererUpdate(new RendererSelectionChgEvent(selected));
                    m_eventService.publish(new ImgRedrawEvent());
                }

            }
        });

        //     m_rendList.setSelectedIndex(0);

        add(new JLabel("Renderer 1: "), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        add(m_rendListOne, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 0, 0, 0);

        gbc.fill = GridBagConstraints.NONE;

        add(new JLabel("Renderer 2: "), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        add(m_rendListTwo, gbc);

        gbc.gridy = 2;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;

        add(Box.createVerticalGlue(), gbc);

        validate();
    }

    /**
     * @param axes
     * @param name
     */
    @EventListener
    public void onIntervalUpdated(final IntervalWithMetadataChgEvent<T, ?> e) {
        if (e instanceof ImgWithMetadataChgEvent) {
            // event already processed
            return;
        }
        final ImageRenderer<T>[] tmp = RendererFactory.createSuitableRenderer(e.getRandomAccessibleInterval());

        m_blockEvent = true;
        m_rendListTwo.removeAllItems();
        for (ImageRenderer<T> rend : tmp) {
            m_rendListTwo.addItem(rend);
        }
        m_rendListTwo.repaint();
        m_blockEvent = false;

    }

    //    @EventListener
    //    public void onImageUpdated(final ImgWithMetadataChgEvent<T> e) {
    //        final ImageRenderer<T>[] tmp =
    //                RendererFactory.createSuitableRenderer(e.getRandomAccessibleInterval(), e.getImgMetaData());
    //
    //        m_blockEvent = true;
    //        m_rendListOne.removeAllItems();
    //        for (ImageRenderer<T> rend : tmp) {
    //            m_rendListOne.addItem(rend);
    //        }
    //        if (m_currentSelectionOne != null
    //                && ((DefaultComboBoxModel)m_rendListOne.getModel()).getIndexOf(m_currentSelectionOne) != -1) {
    //            m_rendListOne.setSelectedItem(m_currentSelectionOne);
    //        } else {
    //            m_rendListOne.setSelectedIndex(0);
    //            m_currentSelectionOne = null;
    //        }
    //        m_rendListOne.repaint();
    //        m_blockEvent = false;
    //    }

    @EventListener
    public void onImageUpdated(final ImageAndLabelingRendererEvent<T, String> e) {
        final ImageRenderer<T>[] imgRenderers = RendererFactory.createSuitableRenderer(e.getImg());

        final ImageRenderer<LabelingType<String>>[] labelRenderers =
                RendererFactory.createSuitableRenderer(e.getLabeling());

        m_blockEvent = true;
        m_rendListOne.removeAllItems();
        for (ImageRenderer<T> rend : imgRenderers) {
            m_rendListOne.addItem(rend);
        }
        if (m_currentSelectionOne != null
                && ((DefaultComboBoxModel)m_rendListOne.getModel()).getIndexOf(m_currentSelectionOne) != -1) {
            m_rendListOne.setSelectedItem(m_currentSelectionOne);
        } else {
            m_rendListOne.setSelectedIndex(0);
            m_currentSelectionOne = null;
        }
        m_rendListOne.repaint();

        m_rendListTwo.removeAllItems();
        for (ImageRenderer<T> rend : imgRenderers) {
            m_rendListTwo.addItem(rend);
        }
        if (m_currentSelectionTwo != null
                && ((DefaultComboBoxModel)m_rendListTwo.getModel()).getIndexOf(m_currentSelectionTwo) != -1) {
            m_rendListTwo.setSelectedItem(m_currentSelectionTwo);
        } else {
            m_rendListTwo.setSelectedIndex(0);
            m_currentSelectionTwo = null;
        }
        m_rendListTwo.repaint();

        m_blockEvent = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position getPosition() {
        return Position.ADDITIONAL;
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
        out.writeInt(m_rendListOne.getSelectedIndex());
        out.writeInt(m_rendListTwo.getSelectedIndex());
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_rendListOne.setSelectedIndex(in.readInt());
        m_rendListTwo.setSelectedIndex(in.readInt());
    }
}
