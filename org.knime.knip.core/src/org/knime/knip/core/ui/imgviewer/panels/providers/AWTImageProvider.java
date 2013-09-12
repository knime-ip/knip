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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.ref.SoftReference;

import javax.swing.Renderer;

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.ops.operation.real.unary.Convert;
import net.imglib2.ops.operation.real.unary.Convert.TypeConversionTypes;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.ImageRenderer;
import org.knime.knip.core.awt.RendererFactory;
import org.knime.knip.core.data.LRUCache;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.events.AWTImageChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.NormalizationParametersChgEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.events.RendererSelectionChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ResetCacheEvent;
import org.knime.knip.core.ui.imgviewer.events.SetCachingEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;
import org.knime.knip.core.ui.imgviewer.panels.HiddenViewerComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 * Publishes {@link AWTImageChgEvent}.
 * 
 * @param <T> The Type of the {@link Img} object
 * @param <I> The {@link Img} class which will be converted to a {@link BufferedImage}
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class AWTImageProvider<T extends Type<T>> extends HiddenViewerComponent {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * {@link Logger}
     */
    protected final static Logger LOGGER = LoggerFactory.getLogger(AWTImageProvider.class);

    /**
     * {@link Img} rendered as {@link BufferedImage}
     */
    protected RandomAccessibleInterval<T> m_src;

    /**
     * {@link PlaneSelectionEvent} indicating the current plane coordinates in the {@link Img} which will be rendered
     */
    protected PlaneSelectionEvent m_sel;

    /**
     * {@link Renderer} rendering the {@link Img}
     */
    protected ImageRenderer m_renderer;

    /**
     * {@link EventService}
     */
    protected EventService m_eventService;

    /*
     * {@link LRUCache} managing the cache of the rendered {@link
     * BufferedImage}
     */
    private LRUCache<Integer, SoftReference<Image>> m_awtImageCache;

    /* Indicates weather caching is active or not */
    private boolean m_isCachingActive = false;

    /* */
    private int m_cache;

    public AWTImageProvider() {
        // for serialization
    }

    /**
     * Constructor
     * 
     * @param cacheSize The number of {@link BufferedImage}s beeing cached using the {@link LRUCache}. A cache size < 2
     *            indicates, that caching is inactive
     */
    public AWTImageProvider(final int cacheSize) {

        if (cacheSize > 1) {
            m_awtImageCache = new LRUCache<Integer, SoftReference<Image>>(cacheSize);
        }

        m_cache = cacheSize;
        m_isCachingActive = cacheSize > 1;
    }

    /**
     * Renders the buffered image according to the parameters of {@link PlaneSelectionEvent},
     * {@link NormalizationParametersChgEvent}, {@link Img} and {@link ImgRenderer}
     * 
     * @return the rendererd {@link Image}
     */
    protected abstract Image createImage();

    /**
     * {@link EventListener} for {@link PlaneSelectionEvent} events The {@link PlaneSelectionEvent} of the
     * {@link AWTImageTools} will be updated
     * 
     * Renders and caches the image
     * 
     * @param img {@link Img} to render
     * @param sel {@link PlaneSelectionEvent}
     */
    @EventListener
    public void onPlaneSelectionUpdate(final PlaneSelectionEvent sel) {
        m_sel = sel;
    }

    /**
     * 
     * Renders and caches the image
     * 
     * @param renderer {@link ImgRenderer} which will be used to render the {@link BufferedImage}
     * 
     */
    @EventListener
    public void onRendererUpdate(final RendererSelectionChgEvent e) {
        m_renderer = e.getRenderer();
    }

    /**
     * {@link EventListener} for {@link Img} and it's {@link CalibratedSpace} {@link Img} and it's
     * {@link CalibratedSpace} metadata will be updated.
     * 
     * Creates a new suiteable {@link ImgRenderer} if the existing one doesn't fit with new {@link Img} Creates a new
     * {@link PlaneSelectionEvent} if numDimensions of the existing {@link PlaneSelectionEvent} doesn't fit with new
     * {@link Img}
     * 
     * Renders and caches the image
     * 
     * @param img The {@link Img} to render. May also be a Labeling.
     * @param axes The axes of the img, currently not used
     * @param name The name of the img
     */
    @EventListener
    public void onUpdated(final IntervalWithMetadataChgEvent<T> e) {
        m_src = e.getRandomAccessibleInterval();

        final long[] dims = new long[e.getRandomAccessibleInterval().numDimensions()];
        e.getRandomAccessibleInterval().dimensions(dims);

        if ((m_sel == null) || !isInsideDims(m_sel.getPlanePos(), dims)) {
            m_sel = new PlaneSelectionEvent(0, 1, new long[e.getRandomAccessibleInterval().numDimensions()]);
        }

        final ImageRenderer[] renderers = RendererFactory.createSuitableRenderer(m_src);
        if (m_renderer != null) {
            boolean contained = false;
            for (final ImageRenderer renderer : renderers) {
                if (m_renderer.toString().equals(renderer.toString())) {
                    m_renderer = renderer;
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                m_renderer = renderers[0];
            }
        } else {
            m_renderer = renderers[0];
        }
    }

    /**
     * Resets the image cache.
     * 
     * @param e
     */
    @EventListener
    public void onResetCache(final ResetCacheEvent e) {
        if (m_isCachingActive) {
            m_awtImageCache.clear();
            LOGGER.debug("Image cache cleared.");
        }
    }

    /**
     * Turns of the caching, e.g. the TransferFunctionRenderer creates different images all the time, it is not possible
     * to store all of them.
     * 
     * @param e
     */
    @EventListener
    public void onSetCaching(final SetCachingEvent e) {
        m_isCachingActive = e.caching();
    }

    /**
     * triggers an actual redraw of the image. If a parameter changes the providers and additional components can first
     * react to the parameter change event before the image is redrawn after the subsequent ImgRedrawEvent. Therefore
     * chained parameters and parameter changes that trigger further changes are possible.
     * 
     * @param e
     */
    @EventListener
    public void onRedrawImage(final ImgRedrawEvent e) {
        renderAndCacheImg();
    }

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

    /**
     * Generates a hashcode according to the parameters of {@link PlaneSelectionEvent},
     * {@link NormalizationParametersChgEvent}, {@link Img} and {@link ImgRenderer}. Override this method to add
     * provider specific hashcode types.
     * 
     * HashCode is generated as hash = hash*31 + object.hashCode().
     * 
     * @return HashCode
     */
    protected int generateHashCode() {
        int hash = 31 + m_src.hashCode();
        hash *= 31;
        hash += m_sel.hashCode();
        hash *= 31;
        hash += m_renderer.getClass().hashCode();
        hash *= 31;
        hash += m_renderer.toString().hashCode(); //if the user information differs
                                                  //re-rendering is most likely necessary
        return hash;
    }

    /*
     * Renders and caches the image according to it's hashcode
     */
    private void renderAndCacheImg() {

        if (m_src == null) {
            return;
        }
        Image awtImage = null;
        if (m_isCachingActive) {

            final int hash = generateHashCode();
            final SoftReference<Image> ref = m_awtImageCache.get(hash);
            if (ref != null) {
                awtImage = ref.get();
            }

            if (awtImage == null) {
                awtImage = createImage();

                m_awtImageCache.put(hash, new SoftReference<Image>(awtImage));
                LOGGER.info("Caching Image ... (" + m_awtImageCache.usedEntries() + ")");
            } else {
                LOGGER.info("Image from Cache ... (" + m_awtImageCache.usedEntries() + ")");
            }

        } else {
            awtImage = createImage();
        }

        m_eventService.publish(new AWTImageChgEvent(awtImage));
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
        out.writeBoolean(m_isCachingActive);
        out.writeInt(m_cache);
        out.writeUTF(m_renderer.getClass().getName());
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_isCachingActive = in.readBoolean();
        m_cache = in.readInt();

        try {
            m_renderer = (ImageRenderer)Class.forName(in.readUTF()).newInstance();
        } catch (final InstantiationException e) {
            e.printStackTrace();
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    /**
     * {@inheritDoc}
     */
    @EventListener
    public void reset(final ViewClosedEvent e) {
        m_src = null;
        m_sel = null;

    }

    @SuppressWarnings("unchecked")
    public RandomAccessibleInterval<? extends RealType<?>>
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
}
