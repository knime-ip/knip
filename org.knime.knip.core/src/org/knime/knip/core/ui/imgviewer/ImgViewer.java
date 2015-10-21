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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;

import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelingWithMetadataChgEvent;

import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imagej.space.DefaultCalibratedSpace;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * A TableCellViewPane providing another view on image objects. It allows to browser through the individual
 * planes/dimensions, enhance contrast, etc.
 *
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgViewer extends JPanel implements ViewerComponentContainer {

    /* def */
    private static final long serialVersionUID = 1L;

    // Panels of this viewer

    private JSplitPane m_background;

    protected JPanel m_centerPanel;

    protected JPanel m_infoPanel;

    private JComponent m_menuComponent;

    // Counter to keep track of added components

    private int m_currentSlot;

    // Buttons available in this viewer

    private JToggleButton m_bottomQuickViewButton;

    private JButton m_overviewButton;

    /** keep the option panel-references to load and save configurations */
    protected List<ViewerComponent> m_viewerComponents;

    /** EventService of the viewer, unique for each viewer. */
    protected EventService m_eventService;

    public ImgViewer() {
        this(new EventService());

    }

    /**
     * @param nodeModel
     */
    public ImgViewer(final EventService eventService) {

        m_eventService = eventService;
        m_viewerComponents = new ArrayList<ViewerComponent>();

        m_background = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        setLayout(new BorderLayout());

        // Left side - the actual view

        JPanel view = createViewPanel();

        // Right panel, i.e. the menus

        m_menuComponent = createMenu();

        JScrollPane sp = new JScrollPane(m_menuComponent);
        sp.setMinimumSize(new Dimension(300, sp.getMinimumSize().height));
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        m_background.setLeftComponent(view);
        m_background.setRightComponent(sp);
        m_background.setResizeWeight(1);
        m_background.setDividerSize(5);

        add(m_background);

    }

    protected JComponent createMenu() {
        JPanel menu = new JPanel();

        menu.setLayout(new GridBagLayout());
        menu.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        return menu;
    }

    public void setMenu(final JComponent menu) {
        m_menuComponent = menu;
    }

    /**
     * @param leftPanel
     */
    protected JPanel createViewPanel() {

        JPanel viewPanel = new JPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout gbl = new GridBagLayout();
        viewPanel.setLayout(gbl);

        // Infopanel
        gbc.anchor = GridBagConstraints.CENTER;

        gbc.gridx = 1;
        gbc.gridy = 1;

        gbc.gridheight = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        m_infoPanel = new JPanel();
        m_infoPanel.setLayout(new BorderLayout());
        viewPanel.add(m_infoPanel, gbc);

        // The imageview

        gbc.gridx = 0;
        gbc.gridy = 2;

        gbc.gridheight = 4;
        gbc.gridwidth = 4;

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;

        m_centerPanel = new JPanel();
        m_centerPanel.setLayout(new BorderLayout());
        viewPanel.add(m_centerPanel, gbc);

        return viewPanel;
    }

    /**
     * Adds the panel
     */
    @Override
    public void addViewerComponent(final ViewerComponent panel) {
        addViewerComponent(panel, true);

    }

    /**
     * Adds the {@link ViewerComponent} to the {@link ImgViewer}
     *
     * @param panel {@link ViewerComponent} to be set
     *
     * @param setEventService indicates weather the {@link EventService} of the {@link ImgViewer} shall be set to the
     *            {@link ViewerComponent}
     *
     */
    public void addViewerComponent(final ViewerComponent panel, final boolean setEventService,
                                   final boolean dynamicSize) {

        if (setEventService) {
            panel.setEventService(m_eventService);
        }

        m_viewerComponents.add(panel);

        switch (panel.getPosition()) {
            case CENTER:
                m_centerPanel.add(panel);
                break;
            case INFO: // CONTROL
                m_infoPanel.add(panel);
                break;
            case ADDITIONAL: // ADDITIONAL
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(5, 0, 5, 0);
                gbc.weightx = 1;
                if (dynamicSize) {
                    gbc.weighty = 1;
                    gbc.fill = GridBagConstraints.BOTH;
                } else {
                    gbc.weighty = 0;
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                }
                gbc.anchor = GridBagConstraints.CENTER;
                gbc.gridx = 0;
                gbc.gridy = m_currentSlot++;

                gbc.gridheight = 1;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                m_menuComponent.add(panel, gbc);

                break;
            default: // hidden

        }

    }

    public void addViewerComponent(final ViewerComponent panel, final boolean setEventService) {
        addViewerComponent(panel, setEventService, false);
    }

    /**
     * Ensures a valid layout. Should be called after all components are added to the Position.ADDITIONAL slot.
     */
    public void doneAdding() {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = m_currentSlot;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        Component panel = Box.createVerticalGlue();
        m_menuComponent.add(panel, gbc);
    }

    /**
     * @return the event service used in this particular viewer (e.g. to subscribe other listeners)
     */
    public EventService getEventService() {
        return m_eventService;
    }

    /**
     * TODO
     *
     * @param labeling
     * @param metadata
     */
    public <L> void setLabeling(final RandomAccessibleInterval<LabelingType<L>> labeling,
                                final LabelingMetadata metadata) {
        // make sure that at least two dimensions exist
        RandomAccessibleInterval<LabelingType<L>> labeling2d = labeling;
        LabelingMetadata resMetadata = metadata;
        if (labeling2d.numDimensions() <= 1) {
            labeling2d = Views.addDimension(labeling2d, 0, 0);
            resMetadata = new DefaultLabelingMetadata(new DefaultCalibratedSpace(2), resMetadata, resMetadata,
                    resMetadata.getLabelingColorTable());
        }

        m_eventService.publish(new LabelingWithMetadataChgEvent(labeling2d, resMetadata));
        m_eventService.publish(new ImgRedrawEvent());
    }

    /**
     * TODO
     */
    public <T extends RealType<T>> void setImg(final ImgPlus<T> img) {

        // make sure that at least two dimensions exist
        Img<T> img2d = img.getImg();
        ImgPlusMetadata meta = img;
        if (img.numDimensions() <= 1) {
            img2d = new ImgView<T>(Views.addDimension(img, 0, 0), img.factory());
            meta = new DefaultImgMetadata(2);
        }

        m_eventService.publish(new ImgWithMetadataChgEvent<T>(img2d, meta));
        m_eventService.publish(new ImgRedrawEvent());

    }
}
