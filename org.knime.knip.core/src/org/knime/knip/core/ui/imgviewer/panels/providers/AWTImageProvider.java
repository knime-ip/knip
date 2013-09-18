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
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
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
import net.imglib2.ops.operation.real.unary.Convert;
import net.imglib2.ops.operation.real.unary.Convert.TypeConversionTypes;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.knime.knip.core.awt.ImageRenderer;
import org.knime.knip.core.awt.RendererFactory;
import org.knime.knip.core.awt.Transparency;
import org.knime.knip.core.data.LRUCache;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.events.AWTImageChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.events.RendererSelectionChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ResetCacheEvent;
import org.knime.knip.core.ui.imgviewer.events.SetCachingEvent;
import org.knime.knip.core.ui.imgviewer.events.TransparencyPanelValueChgEvent;
import org.knime.knip.core.ui.imgviewer.panels.HiddenViewerComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes {@link AWTImageChgEvent}.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class AWTImageProvider extends HiddenViewerComponent {

    private static final long serialVersionUID = 1L;

    private final static Logger LOGGER = LoggerFactory.getLogger(AWTImageProvider.class);

    private EventService m_eventService;

    /*
     * {@link LRUCache} managing the cache of the rendered {@link
     * BufferedImage}
     */
    private LRUCache<Integer, SoftReference<Image>> m_awtImageCache;

    /* Indicates whether caching is active or not */
    private boolean m_isCachingActive = false;

    /* the cache (stores images for faster access */
    private int m_cache;

    /**
     * {@link PlaneSelectionEvent} indicating the current plane coordinates in the {@link Img} which will be rendered
     */
    private PlaneSelectionEvent m_sel;

    /**
     * {@link Renderer} rendering the {@link Img}
     */
    private ImageRenderer<?> m_renderer;

    private Integer m_transparency = 128;

    private RenderUnit[] m_renderUnits;

    private GraphicsConfiguration m_graphicsConfig;



    /**
     * Constructor
     *
     * @param cacheSize The number of {@link BufferedImage}s beeing cached using the {@link LRUCache}. A cache size < 2
     *            indicates, that caching is inactive
     */
    public AWTImageProvider(final int cacheSize, final RenderUnit... renderUnits) {
        if (cacheSize > 1) {
            m_awtImageCache = new LRUCache<Integer, SoftReference<Image>>(cacheSize);
        }
        m_cache = cacheSize;
        m_isCachingActive = cacheSize > 1;
        m_renderUnits = renderUnits;
        m_graphicsConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
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

    /**
     * stores current plane selection for fitDim checks if a new source is selected.
     * @param sel
     */
    @EventListener
    public void onPlaneSelectionUpdate(final PlaneSelectionEvent sel) {
        m_sel = sel;
    }

    /**
     * stores current renderer to test it against new sources.
     * @param e
     */
    @EventListener
    public void onRendererUpdate(final RendererSelectionChgEvent e) {
        m_renderer = e.getRenderer();
    }

    @EventListener
    public void onUpdate(final TransparencyPanelValueChgEvent e) {
        m_transparency = e.getTransparency();
    }

    /**
     * Creates a new suitable {@link ImageRenderer} if the existing one doesn't fit the new source. Creates a new
     * {@link PlaneSelectionEvent} if numDimensions of the existing {@link PlaneSelectionEvent} doesn't fit with new
     * data.
     */
    @EventListener
    public void onUpdated(final IntervalWithMetadataChgEvent<?> e) {
        final long[] dims = new long[e.getRandomAccessibleInterval().numDimensions()];
        e.getRandomAccessibleInterval().dimensions(dims);

        if ((m_sel == null) || !isInsideDims(m_sel.getPlanePos(), dims)) {
            m_sel = new PlaneSelectionEvent(0, 1, new long[e.getRandomAccessibleInterval().numDimensions()]);
        }

        final ImageRenderer<?>[] renderers = RendererFactory.createSuitableRenderer(e.getRandomAccessibleInterval());
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
                m_renderer = renderers[0];
            }
        } else {
            m_renderer = renderers[0];
        }
    }

    /*
     * Renders and caches the image according to it's hashcode
     */
    private void renderAndCacheImg() {
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

    private int generateHashCode() {
        int hash = 31;
        for (RenderUnit ru : m_renderUnits) {
            hash += ru.generateHashCode();
            hash *= 31;
        }

        return hash;
    }

    private Image createImage() {
        //blend images together
        Image img = m_renderUnits[0].createImage();
        Image joinedImg = m_graphicsConfig.createCompatibleImage(img.getWidth(null), img.getHeight(null), java.awt.Transparency.TRANSLUCENT);
        Graphics g = joinedImg.getGraphics();
        g.drawImage(img, 0, 0, null);

        for (int i = 1; i < m_renderUnits.length; i++) {
            g.drawImage(Transparency.makeColorTransparent(m_renderUnits[i].createImage(), Color.WHITE, m_transparency), 0, 0, null);
        }

        return joinedImg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
        eventService.subscribe(this);
        for (RenderUnit ru : m_renderUnits) {
            ru.setEventService(m_eventService);
        }
    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        out.writeBoolean(m_isCachingActive);
        out.writeInt(m_cache);
        for (RenderUnit ru : m_renderUnits) {
            ru.saveAdditionalConfigurations(out);
        }
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_isCachingActive = in.readBoolean();
        m_cache = in.readInt();
        for (RenderUnit ru : m_renderUnits) {
            ru.loadAdditionalConfigurations(in);
        }
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
}
