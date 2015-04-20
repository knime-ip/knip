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
package org.knime.knip.base.data.ui;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.ui.imgviewer.ExpandingPanel;
import org.knime.knip.core.ui.imgviewer.ImgCanvas;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.ViewerComponent.Position;
import org.knime.knip.core.ui.imgviewer.ViewerComponents;
import org.knime.knip.core.ui.imgviewer.panels.ControlPanel;
import org.knime.knip.core.ui.imgviewer.panels.LabelOptionPanel;
import org.knime.knip.core.ui.imgviewer.panels.infobars.HistogramViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.infobars.ImgViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.infobars.LabelingViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.providers.AWTImageProvider;
import org.knime.knip.core.ui.imgviewer.panels.providers.HistogramRU;
import org.knime.knip.core.ui.imgviewer.panels.providers.ImageRU;
import org.knime.knip.core.ui.imgviewer.panels.providers.LabelingRU;

/**
 * TODO Auto-generated
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ViewerFactory {

    /**
     * Creates a ImgViewer showing the histogram of the given image.
     *
     * @param cacheSize
     * @param <T>
     *
     * @return {@link ImgViewer}
     */
    public static <T extends RealType<T>> ImgViewer createHistViewer(final int cacheSize) {
        final ImgViewer viewer = new ImgViewer();

        viewer.addViewerComponent(new AWTImageProvider(cacheSize, new HistogramRU<T>(512)));
        viewer.addViewerComponent(new HistogramViewInfoPanel<T, Img<T>>());
        viewer.addViewerComponent(new ImgCanvas<T, Img<T>>());

        viewer.addViewerComponent(new ControlPanel(Position.NORTH));
        viewer.addViewerComponent(new ControlPanel(Position.EAST));
        viewer.addViewerComponent(new ControlPanel(Position.WEST));
        viewer.addViewerComponent(new ControlPanel(Position.SOUTH));

        viewer.addViewerComponent(ViewerComponents.MINIMAP.createInstance());
       // viewer.addViewerComponent(ViewerComponents.PLANE_SELECTION.createInstance());
        viewer.addViewerComponent(new ExpandingPanel("Image Properties",ViewerComponents.IMAGE_PROPERTIES.createInstance()));

        viewer.doneAdding();

        return viewer;
    }

    /**
     * Creates a ImgViewer for {@link Img}s with a Minimap, Plane Selection, Renderer Selection, Image Normalization and
     * Image Properties Panel
     *
     * @param cacheSize
     *
     * @return {@link ImgViewer}
     */
    public static <T extends RealType<T> & NativeType<T>> ImgViewer createImgViewer(final int cacheSize) {

        final ImgViewer viewer = new ImgViewer();

        final AWTImageProvider realProvider = new AWTImageProvider(cacheSize, new ImageRU<T>(false));
        realProvider.setEventService(viewer.getEventService());

        viewer.addViewerComponent(new ImgViewInfoPanel<T>());
        viewer.addViewerComponent(new ImgCanvas<T, Img<T>>());

        viewer.addViewerComponent(new ControlPanel(Position.NORTH));
        viewer.addViewerComponent(new ControlPanel(Position.EAST));
        viewer.addViewerComponent(new ControlPanel(Position.WEST));
        viewer.addViewerComponent(new ControlPanel(Position.SOUTH));

        viewer.addViewerComponent(ViewerComponents.MINIMAP_PLANE_SELECTION.createInstance());
        viewer.addViewerComponent(new ExpandingPanel("Image Enhancement",ViewerComponents.IMAGE_ENHANCE.createInstance(), true));
        viewer.addViewerComponent(new ExpandingPanel("Renderer Selection",ViewerComponents.RENDERER_SELECTION.createInstance()));
        viewer.addViewerComponent(new ExpandingPanel("Image Properties",ViewerComponents.IMAGE_PROPERTIES.createInstance()));

        viewer.doneAdding();

        return viewer;

    }

    /**
     * @param cacheSize
     * @return {@link ImgViewer}
     */
    public static <L extends Comparable<L>> ImgViewer createLabelingViewer(final int cacheSize) {
        final ImgViewer viewer = new ImgViewer();

        new AWTImageProvider(cacheSize, new LabelingRU<L>()).setEventService(viewer.getEventService());

        viewer.addViewerComponent(new LabelingViewInfoPanel<L>());

        viewer.addViewerComponent(new ImgCanvas<LabelingType<L>, RandomAccessibleInterval<LabelingType<L>>>());


        viewer.addViewerComponent(new ControlPanel(Position.NORTH));
        viewer.addViewerComponent(new ControlPanel(Position.EAST));
        viewer.addViewerComponent(new ControlPanel(Position.WEST));
        viewer.addViewerComponent(new ControlPanel(Position.SOUTH));

        viewer.addViewerComponent(ViewerComponents.MINIMAP.createInstance());

       // viewer.addViewerComponent(ViewerComponents.PLANE_SELECTION.createInstance());

        viewer.addViewerComponent(new ExpandingPanel("Renderer Selection",ViewerComponents.RENDERER_SELECTION.createInstance()));

        viewer.addViewerComponent(new ExpandingPanel("Image Properties",ViewerComponents.IMAGE_PROPERTIES.createInstance()));

        viewer.addViewerComponent(new ExpandingPanel("Label Options",new LabelOptionPanel()));

        viewer.addViewerComponent(new ExpandingPanel("Label Filter",ViewerComponents.LABEL_FILTER.createInstance()));

        viewer.doneAdding();

        return viewer;
    }

    private ViewerFactory() {
        //
    }

}
