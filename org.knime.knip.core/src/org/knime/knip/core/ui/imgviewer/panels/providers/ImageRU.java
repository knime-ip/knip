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
 * ---------------------------------------------------------------------
 *
 * Created on 18.09.2013 by zinsmaie
 */
package org.knime.knip.core.ui.imgviewer.panels.providers;

import java.awt.Image;
import java.util.Arrays;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.ColorTable;
import net.imglib2.display.projectors.screenimages.ScreenImage;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.Real2GreyRenderer;
import org.knime.knip.core.awt.lookup.LookupTable;
import org.knime.knip.core.awt.parametersupport.RendererWithColorTable;
import org.knime.knip.core.awt.parametersupport.RendererWithLookupTable;
import org.knime.knip.core.awt.parametersupport.RendererWithNormalization;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorResetEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgAndLabelingChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.NormalizationParametersChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;
import org.knime.knip.core.ui.imgviewer.panels.transfunc.LookupTableChgEvent;

/*
 * This class could be split into three following the one class one responsibility paradigm. However its not a complex
 * class and this way its closer to the original implementation of the AWTImageProvider descendants.
 *
 * TODO split it after a migration phase if the new implementation stays
 */
/**
 * Combined image renderer. Supports basic rendering of pure images with grey, color, color table renderers as well as
 * pure grey rendering (e.g. as background for labels) and rendering using transfer functions.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * 
 * @param <T> number based type of the rendered image
 */
public class ImageRU<T extends RealType<T>> extends AbstractDefaultRU<T> {

    /**
     * A simple class that can be injected in the converter so that we will always get some result.
     */
    private class SimpleTable implements LookupTable<T, ARGBType> {
        @Override
        public final ARGBType lookup(final T value) {
            return new ARGBType(1);
        }

        /**
         * {@inheritDoc} all instances of SimpleTable have identical hashCodes
         */
        @Override
        public int hashCode() {
            return 1;
        }
    }

    /** Identifying hashCode of the last rendered image. */
    private int m_hashOfLastRendering;

    /** caches the last rendered image. */
    private Image m_lastImage;

    /**
     * true if all images should be rendered using a greyScale renderer (independent of the renderer selection).
     */
    private final boolean m_enforceGreyScale;

    /** used for all grey rendering mode. */
    private Real2GreyRenderer<T> m_greyRenderer = new Real2GreyRenderer<T>();

    // event members

    private LookupTable<T, ARGBType> m_lookupTable = new SimpleTable();

    private NormalizationParametersChgEvent m_normalizationParameters = new NormalizationParametersChgEvent(0, false);

    private ColorTable[] m_colorTables = new ColorTable[]{};

    private RandomAccessibleInterval<T> m_src;

    /** default constructor that creates a renderer selection dependent image {@link RenderUnit}. */
    public ImageRU() {
        this(false);
    }

    /**
     * Creates a {@link RenderUnit} for image data.
     * 
     * @param enforceGreyScale false => depends on renderer selection <br>
     *            true => always uses a {@link Real2GreyRenderer}
     */
    public ImageRU(final boolean enforceGreyScale) {
        m_enforceGreyScale = enforceGreyScale;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Image createImage() {
        if (m_lastImage != null && m_hashOfLastRendering == generateHashCode()) {
            return m_lastImage;
        }

        //+ allows normalization - breaks type safety
        @SuppressWarnings("rawtypes")
        RandomAccessibleInterval convertedSrc = AWTImageProvider.convertIfDouble(m_src);
        final double[] normParams =
                m_normalizationParameters.getNormalizationParameters(convertedSrc, m_planeSelection);

        //set parameters of the renderer
        if (m_renderer instanceof RendererWithNormalization) {
            ((RendererWithNormalization)m_renderer).setNormalizationParameters(normParams[0], normParams[1]);
        }

        if (m_renderer instanceof RendererWithLookupTable) {
            ((RendererWithLookupTable<T, ARGBType>)m_renderer).setLookupTable(m_lookupTable);
        }

        if (m_renderer instanceof RendererWithColorTable) {
            ((RendererWithColorTable)m_renderer).setColorTables(m_colorTables);
        }

        final ScreenImage ret;
        if (!m_enforceGreyScale) {
            ret =
                    m_renderer.render(convertedSrc, m_planeSelection.getPlaneDimIndex1(),
                                      m_planeSelection.getPlaneDimIndex2(), m_planeSelection.getPlanePos());
        } else {
            m_greyRenderer.setNormalizationParameters(normParams[0], normParams[1]);
            ret =
                    m_greyRenderer.render(convertedSrc, m_planeSelection.getPlaneDimIndex1(),
                                          m_planeSelection.getPlaneDimIndex2(), m_planeSelection.getPlanePos());
        }

        m_lastImage = ret.image();
        m_hashOfLastRendering = generateHashCode();

        return AWTImageTools.makeBuffered(ret.image());
    }

    @Override
    public int generateHashCode() {
        int hash = super.generateHashCode();
        if (isActive()) {
            hash += m_normalizationParameters.hashCode();
            hash *= 31;
            hash += m_src.hashCode();
            hash *= 31;
            hash += m_lookupTable.hashCode();
            hash *= 31;
            hash += Arrays.hashCode(m_colorTables);
            hash *= 31;
        }
        return hash;
    }

    @Override
    public boolean isActive() {
        return (m_src != null);
    }

    //event handling

    /**
     * stores normalization parameters into a member.
     * 
     * @param normalizationParameters saturation ... used for rendering.
     */
    @EventListener
    public void onUpdated(final NormalizationParametersChgEvent normalizationParameters) {
        m_normalizationParameters = normalizationParameters;
    }

    /**
     * {@link EventListener} for {@link LookupTableChgEvent}. The {@link LookupTable} is stored in a member and is used
     * for transfer function handling.
     * 
     * @param event holds a lookup table
     */
    @EventListener
    public void onLookupTableChgEvent(final LookupTableChgEvent<T, ARGBType> event) {
        m_lookupTable = event.getTable();
    }

    /**
     * changes the stored image (for rendering) additionally updates the color table member.
     * 
     * @param e image and MetaData
     */
    @EventListener
    public void onImageUpdated(final ImgWithMetadataChgEvent<T> e) {
        m_src = e.getRandomAccessibleInterval();

        final int size = e.getImgMetaData().getColorTableCount();
        m_colorTables = new ColorTable[size];

        for (int i = 0; i < size; i++) {
            m_colorTables[i] = e.getImgMetaData().getColorTable(i);
        }
    }

    /**
     * changes the stored image (for rendering).
     * 
     * @param e contains a image.
     */
    @EventListener
    public void onImageUpdated(final ImgAndLabelingChgEvent<T, ?> e) {
        m_src = e.getRandomAccessibleInterval();
    }

    /**
     * set all members that could hold expensive references to null or resets them to allow storage clean ups.
     * 
     * @param event marker event
     */
    @EventListener
    public void onClose2(final ViewClosedEvent event) {
        m_lastImage = null;
        m_src = null;
        m_greyRenderer = new Real2GreyRenderer<T>();
        m_lookupTable = new SimpleTable();
        m_normalizationParameters = new NormalizationParametersChgEvent(0, false);
        m_colorTables = new ColorTable[]{};
    }

    /**
     * @param event {@link #onClose2()}
     */
    @EventListener
    public void onAnnotatorReset(final AnnotatorResetEvent event) {
        //we need this because annotators are currently placed in dialogs. Unlike views dialogs
        //are not recreated on reopening. Therefore annotators can't use the ViewClosedEvent that
        //destroys some of the ViewerComponents (on a view they would be recreated).
        //=> RenderUnits listen to AnnotatorResetEvents as well
        onClose2(new ViewClosedEvent());
    }

}
