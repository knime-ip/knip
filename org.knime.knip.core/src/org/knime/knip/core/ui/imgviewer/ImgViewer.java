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
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import net.imglib2.labeling.Labeling;
import net.imglib2.meta.DefaultCalibratedSpace;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.subset.views.ImgPlusView;
import net.imglib2.ops.operation.subset.views.LabelingView;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelingWithMetadataChgEvent;

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

    /* Panels of the viewer accoding to the BorderLayout */
    private JPanel m_southPanels;

    private JPanel m_eastPanels;

    private JPanel m_westPanels;

    private Container m_northPanels;

    private JPanel m_centerPanels;

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
        // content pane
        setLayout(new BorderLayout());

        m_centerPanels = new JPanel();
        m_centerPanels.setLayout(new BoxLayout(m_centerPanels, BoxLayout.Y_AXIS));
        add(m_centerPanels, BorderLayout.CENTER);

        m_southPanels = new JPanel();
        m_southPanels.setLayout(new BoxLayout(m_southPanels, BoxLayout.X_AXIS));
        add(m_southPanels, BorderLayout.SOUTH);

        m_eastPanels = new JPanel();
        m_eastPanels.setLayout(new BoxLayout(m_eastPanels, BoxLayout.Y_AXIS));
        add(m_eastPanels, BorderLayout.EAST);

        m_westPanels = new JPanel();
        m_westPanels.setLayout(new BoxLayout(m_westPanels, BoxLayout.Y_AXIS));
        add(m_westPanels, BorderLayout.WEST);

        m_northPanels = new JPanel();
        m_northPanels.setLayout(new BoxLayout(m_northPanels, BoxLayout.X_AXIS));
        add(m_northPanels, BorderLayout.NORTH);

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
    public void addViewerComponent(final ViewerComponent panel, final boolean setEventService) {

        if (setEventService) {
            panel.setEventService(m_eventService);
        }

        m_viewerComponents.add(panel);

        switch (panel.getPosition()) {
            case CENTER:
                m_centerPanels.add(panel);
                break;
            case NORTH:
                m_northPanels.add(panel);
                break;
            case SOUTH:
                m_southPanels.add(panel);
                break;
            case EAST:
                m_eastPanels.add(panel);
                break;
            case WEST:
                m_westPanels.add(panel);
                break;
            default: // hidden

        }

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
    public <L extends Comparable<L>> void setLabeling(final Labeling<L> labeling, final LabelingMetadata metadata) {
        // make sure that at least two dimensions exist
        Labeling<L> labeling2d = labeling;
        LabelingMetadata resMetadata = metadata;
        if (labeling2d.numDimensions() <= 1) {
            labeling2d = new LabelingView<L>(Views.addDimension(labeling2d, 0, 0), labeling2d.<L> factory());
            resMetadata =
                    new DefaultLabelingMetadata(new DefaultCalibratedSpace(2), resMetadata, resMetadata,
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
        ImgPlus<T> img2d = img;
        if (img.numDimensions() <= 1) {
            img2d = new ImgPlusView<T>(Views.addDimension(img, 0, 0), img.factory());
        }

        m_eventService.publish(new ImgWithMetadataChgEvent<T>(img2d, img));
        m_eventService.publish(new ImgRedrawEvent());

    }
}
