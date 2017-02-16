package org.knime.knip.core.ui.imgviewer.panels.providers;

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 */
import java.awt.Image;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.LRUCache;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.awt.ImageRenderer;
import org.knime.knip.core.awt.Real2ColorByLookupTableRenderer;
import org.knime.knip.core.awt.RendererFactory;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.events.AWTImageChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.events.RendererSelectionChgEvent;
import org.knime.knip.core.ui.imgviewer.events.SetCachingEvent;
import org.knime.knip.core.ui.imgviewer.panels.HiddenViewerComponent;

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.ops.operation.real.unary.Convert;
import net.imglib2.ops.operation.real.unary.Convert.TypeConversionTypes;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Handles the creation and caching of images for views. Publishes {@link AWTImageChgEvent} if the image changes.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class AWTImageProvider extends HiddenViewerComponent {

    private static final long serialVersionUID = 1L;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AWTImageProvider.class);

    /**
     * Converts DoubleType to FloatType and preserves other types. This can be used to ensure that images (after calling
     * the method) are FloatType or smaller and thus can be normalized. However type safety is broken!
     *
     *
     * @param src a RealType image
     * @return either the source image or if the source type is DoubleType a FloatType representation of the image to
     *         allow rendering.
     */
    @SuppressWarnings("unchecked")
    public static RandomAccessibleInterval<? extends RealType<?>>
           convertIfDouble(final RandomAccessibleInterval<? extends RealType<?>> src) {
        final IterableInterval<?> iterable = Views.iterable(src);

        if (iterable.firstElement() instanceof DoubleType) {
            final Convert<DoubleType, FloatType> convertOp =
                    new Convert<DoubleType, FloatType>(new DoubleType(), new FloatType(), TypeConversionTypes.DIRECT);

            return new ConvertedRandomAccessibleInterval<DoubleType, FloatType>(
                    (RandomAccessibleInterval<DoubleType>)src, convertOp, new FloatType());
        }

        return src;
    }

    //Event members

    private EventService m_eventService;

    private PlaneSelectionEvent m_planeSelection;

    private ImageRenderer<?> m_renderer;

    //

    /** Indicates whether caching is active or not. */
    private boolean m_isCachingActive = false;

    /** size of the {@link LRUCache}. */
    private int m_cacheSize;

    /** the render unit that is used to create the image result. */
    private RenderUnit m_renderUnit;

    /**
     * Creates a new AWTImageProviders that uses different parameters from the {@link EventService} to create images
     * using its associated {@link RenderUnit}.
     *
     * @param ru the {@link RenderUnit} that creates the images
     * @param cacheSize The number of {@link Image}s being cached using the {@link LRUCache}. A cache size < 2
     *            indicates, that caching is inactive
     */
    public AWTImageProvider(final int cacheSize, final RenderUnit ru) {
        m_cacheSize = cacheSize;
        m_isCachingActive = cacheSize > 1;
        m_renderUnit = ru;
    }

    /**
     * Retrieves images from the cache if possible (i.e. if the {@link #generateHashCode() hashCode} is in the
     * {@link #m_awtImageCache cache}) if not triggers rendering of a suitable image.
     */
    private void renderAndCacheImg() {
        Image awtImage = null;
        if (m_isCachingActive) {

            final int hash = (31 + m_renderUnit.generateHashCode());
            awtImage = (Image)KNIPGateway.cache().get(hash);

            if (awtImage == null) {
                awtImage = m_renderUnit.createImage();

                KNIPGateway.cache().put(hash, awtImage);
                LOGGER.info("Caching Image ...");
            } else {
                LOGGER.info("Image from Cache ...");
            }

        } else {
            awtImage = m_renderUnit.createImage();
        }

        m_eventService.publish(new AWTImageChgEvent(awtImage));
    }

    /**
     * Publishes a {@link RendererSelectionChgEvent} with a new suitable {@link ImageRenderer} if the existing one
     * doesn't fit the new source.<br>
     * Publishes a new {@link PlaneSelectionEvent} if numDimensions of the last {@link PlaneSelectionEvent} doesn't fit
     * with new source.
     *
     * @param e new selected Interval (Image, Labeling)
     */
    private void checkRendererAndPlaneSelection(final IntervalWithMetadataChgEvent<?, ?> e) {
        final long[] dims = new long[e.getRandomAccessibleInterval().numDimensions()];
        e.getRandomAccessibleInterval().dimensions(dims);

        if ((m_planeSelection == null) || !isInsideDims(m_planeSelection.getPlanePos(), dims)) {
            //publish if necessary
            m_eventService
                    .publish(new PlaneSelectionEvent(0, 1, new long[e.getRandomAccessibleInterval().numDimensions()]));
        }

        final ImageRenderer<?>[] renderers = RendererFactory.createSuitableRenderer(e.getRandomAccessibleInterval());
        ImageRenderer<?> newRenderer = null;
        if (m_renderer != null) {
            boolean contained = false;
            for (final ImageRenderer<?> renderer : renderers) {
                if (m_renderer.toString().equals(renderer.toString())) {
                    m_renderer = renderer;
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                newRenderer = renderers[0];
            }
        } else {
            newRenderer = renderers[0];
        }

        if (newRenderer != null) {
            //publish if necessary
            m_eventService.publish(new RendererSelectionChgEvent(newRenderer));
        }
    }

    /**
     * helper method that checks if a given plane position is within the ranges of the given dim array.
     *
     * @param planePos position to be checked
     * @param dims range that should include the position
     * @return true if planePos is inside dims
     */
    private boolean isInsideDims(final long[] planePos, final long[] dims) {
        if (planePos.length != dims.length) {
            return false;
        }
        for (int d = 0; d < planePos.length; d++) {
            if (planePos[d] >= dims[d]) {
                return false;
            }
        }
        return true;
    }

    // event handling

    /**
     * triggers an actual redraw of the image. If a parameter changes the providers and additional components can first
     * react to the parameter change event before the image is redrawn after the subsequent ImgRedrawEvent. Therefore
     * chained parameters and parameter changes that trigger further changes are possible.
     *
     * @param e marker event.
     */
    @EventListener
    public void onRedrawImage(final ImgRedrawEvent e) {
        renderAndCacheImg();
    }

    /**
     * Checks if currently active {@link ImageRenderer} and {@link PlaneSelectionEvent} fit the new image.<br>
     * <br>
     *
     * @param e new selected Interval (Image, Labeling)
     */
    @EventListener
    public void onUpdated(final IntervalWithMetadataChgEvent<?, ?> e) {
        checkRendererAndPlaneSelection(e);
    }

    /**
     * Turns of the caching. This is e.g. useful for the {@link Real2ColorByLookupTableRenderer} (for transfer
     * functions) creates different images all the time such that caching provides no benefits.
     *
     * @param e activates / deactivates caching
     */
    @EventListener
    public void onSetCachingStrategy(final SetCachingEvent e) {
        m_isCachingActive = e.caching();
    }

    /**
     * Stores the currently active plane selection into a member. This is used for fitDimension checks if a new source
     * is selected.
     *
     * @param sel currently active plane
     */
    @EventListener
    public void onPlaneSelectionUpdate(final PlaneSelectionEvent sel) {
        m_planeSelection = sel;
    }

    /**
     * stores current renderer in a member to allow testing against new sources.
     *
     * @param e selected renderer
     */
    @EventListener
    public void onRendererUpdate(final RendererSelectionChgEvent e) {
        m_renderer = e.getRenderer();
    }

    //HiddenViewerComponent methods

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
        eventService.subscribe(this);
        m_renderUnit.setEventService(eventService);
    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        out.writeBoolean(m_isCachingActive);
        out.writeInt(m_cacheSize);
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_isCachingActive = in.readBoolean();
        m_cacheSize = in.readInt();
    }

}
