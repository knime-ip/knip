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

import net.imglib2.img.Img;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.ui.imgviewer.ImgCanvas;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.ViewerComponents;
import org.knime.knip.core.ui.imgviewer.events.SetCachingEvent;
import org.knime.knip.core.ui.imgviewer.panels.LabelOptionPanel;
import org.knime.knip.core.ui.imgviewer.panels.infobars.HistogramViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.infobars.ImgViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.infobars.LabelingViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.providers.BufferedImageProvider;
import org.knime.knip.core.ui.imgviewer.panels.providers.HistogramBufferedImageProvider;
import org.knime.knip.core.ui.imgviewer.panels.providers.LabelingBufferedImageProvider;
import org.knime.knip.core.ui.imgviewer.panels.transfunc.PlaneSelectionTFCDataProvider;
import org.knime.knip.core.ui.imgviewer.panels.transfunc.TransferFunctionControlPanel;

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
     * @param <T>
     * @return
     */
    public static <T extends RealType<T>> ImgViewer createHistViewer(final int cacheSize) {
        final ImgViewer viewer = new ImgViewer();

        viewer.addViewerComponent(new HistogramBufferedImageProvider<T>(cacheSize, 512));
        viewer.addViewerComponent(new HistogramViewInfoPanel<T, Img<T>>());
        viewer.addViewerComponent(new ImgCanvas<T, Img<T>>());
        viewer.addViewerComponent(ViewerComponents.MINIMAP.createInstance());
        viewer.addViewerComponent(ViewerComponents.PLANE_SELECTION.createInstance());
        viewer.addViewerComponent(ViewerComponents.IMAGE_PROPERTIES.createInstance());

        return viewer;
    }

    /**
     * Creates a ImgViewer for {@link Img}s with a Minimap, Plane Selection, Renderer Selection, Image Normalization and
     * Image Properties Panel
     * 
     * @return
     */
    public static <T extends RealType<T> & NativeType<T>> ImgViewer createImgViewer(final int cacheSize) {

        final ImgViewer viewer = new ImgViewer();

        final BufferedImageProvider<T> realProvider = new BufferedImageProvider<T>(cacheSize);
        realProvider.setEventService(viewer.getEventService());

        viewer.addViewerComponent(new ImgViewInfoPanel<T>());
        viewer.addViewerComponent(new ImgCanvas<T, Img<T>>());

        viewer.addViewerComponent(ViewerComponents.MINIMAP.createInstance());
        viewer.addViewerComponent(ViewerComponents.PLANE_SELECTION.createInstance());
        viewer.addViewerComponent(ViewerComponents.IMAGE_ENHANCE.createInstance());
        viewer.addViewerComponent(ViewerComponents.RENDERER_SELECTION.createInstance());
        viewer.addViewerComponent(ViewerComponents.IMAGE_PROPERTIES.createInstance());

        return viewer;

    }

    public static <L extends Comparable<L>> ImgViewer createLabelingViewer(final int cacheSize) {
        final ImgViewer viewer = new ImgViewer();

        new LabelingBufferedImageProvider<L>(cacheSize).setEventService(viewer.getEventService());

        viewer.addViewerComponent(new LabelingViewInfoPanel<L>());

        viewer.addViewerComponent(new ImgCanvas<LabelingType<L>, Labeling<L>>());

        viewer.addViewerComponent(ViewerComponents.MINIMAP.createInstance());

        viewer.addViewerComponent(ViewerComponents.PLANE_SELECTION.createInstance());

        viewer.addViewerComponent(ViewerComponents.RENDERER_SELECTION.createInstance());

        viewer.addViewerComponent(ViewerComponents.IMAGE_PROPERTIES.createInstance());

        viewer.addViewerComponent(new LabelOptionPanel());

        viewer.addViewerComponent(ViewerComponents.LABEL_FILTER.createInstance());

        return viewer;
    }

    /**
     * Creates a ImgViewer for {@link Img}s with a Minimap, Plane Selection, and TransferFunctionPanel
     * 
     * @return
     */
    public static <T extends RealType<T> & NativeType<T>> ImgViewer createTransferFunctionViewer(final int cacheSize) {

        final ImgViewer viewer = new ImgViewer();

        final BufferedImageProvider<T> realProvider = new BufferedImageProvider<T>(cacheSize);
        realProvider.setEventService(viewer.getEventService());
        realProvider.onSetCaching(new SetCachingEvent(false));

        viewer.addViewerComponent(new ImgViewInfoPanel<T>());
        viewer.addViewerComponent(new ImgCanvas<T, Img<T>>());

        viewer.addViewerComponent(ViewerComponents.MINIMAP.createInstance());
        viewer.addViewerComponent(ViewerComponents.PLANE_SELECTION.createInstance());

        viewer.addViewerComponent(new PlaneSelectionTFCDataProvider<T, Img<T>>(new TransferFunctionControlPanel()));

        return viewer;

    }

    private ViewerFactory() {
        //
    }

}
