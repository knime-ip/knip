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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.ScreenImage;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.Real2GreyRenderer;
import org.knime.knip.core.awt.Transparency;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableRenderer;
import org.knime.knip.core.awt.parametersupport.RendererWithHilite;
import org.knime.knip.core.awt.parametersupport.RendererWithLabels;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.imgviewer.events.HilitedLabelsChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgAndLabelingChgEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelColoringChangeEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelOptionsChangeEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelPanelIsHiliteModeEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelPanelVisibleLabelsChgEvent;
import org.knime.knip.core.ui.imgviewer.events.NormalizationParametersChgEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.events.RendererSelectionChgEvent;
import org.knime.knip.core.ui.imgviewer.events.TransparencyPanelValueChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;

/**
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class BufferedImageLabelingOverlayProvider<T extends RealType<T>, L extends Comparable<L>> extends
        LabelingBufferedImageProvider<L> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Real2GreyRenderer<T> m_greyRenderer;

    private RandomAccessibleInterval<T> m_img;

    private Integer m_transparency;

    private NormalizationParametersChgEvent m_normalizationParameters;

    private GraphicsConfiguration m_config;

    private BufferedImage m_bufLab;

    private BufferedImage m_bufImg;

    private BufferedImage m_rgb;

    private Set<String> m_hilitedLabels;

    private Graphics m_rgbGraphics;

    private boolean m_labChanged;

    private boolean m_imgChanged;

    private String m_rowKey = null;

    private boolean m_isHiliteMode = false;

    public BufferedImageLabelingOverlayProvider(final int cacheSize) {
        super(cacheSize);

        m_greyRenderer = new Real2GreyRenderer<T>();
        m_transparency = 128;
        m_normalizationParameters = new NormalizationParametersChgEvent(0, false);
        m_config = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }

    @Override
    protected int generateHashCode() {
        int hash = super.generateHashCode();
        hash = (hash * 31) + m_img.hashCode();
        hash = (hash * 31) + m_transparency;
        hash = (hash * 31) + m_normalizationParameters.hashCode();

        if (m_isHiliteMode) {
            hash = (hash * 31) + 1;
        }

        if ((m_rowKey != null) && (m_hilitedLabels != null)) {
            hash = (hash * 31) + m_rowKey.hashCode();
            hash = (hash * 31) + m_hilitedLabels.hashCode();
        }
        return hash;
    }

    @Override
    @EventListener
    public void onUpdate(final LabelPanelVisibleLabelsChgEvent e) {
        m_labChanged = true;
        super.onUpdate(e);
    }

    @Override
    @EventListener
    public void onPlaneSelectionUpdate(final PlaneSelectionEvent sel) {
        m_sel = sel;
        m_imgChanged = true;
        m_labChanged = true;
        super.onPlaneSelectionUpdate(sel);
    }

    @Override
    @EventListener
    public void onRendererUpdate(final RendererSelectionChgEvent e) {
        m_labChanged = true;
        super.onRendererUpdate(e);
    }

    @Override
    protected Image createImage() {
        if (m_imgChanged) {
            m_bufImg = renderImage();
            m_imgChanged = false;
        }

        if (m_labChanged) {
            m_bufLab = renderLabeling();
            m_labChanged = false;
        }

        return renderTogether(m_bufImg, m_bufLab);
    }

    @Override
    @EventListener
    public void onLabelColoringChangeEvent(final LabelColoringChangeEvent e) {
        super.onLabelColoringChangeEvent(e);
        m_labChanged = true;
    }

    @Override
    @EventListener
    public void onLabelOptionsChangeEvent(final LabelOptionsChangeEvent e) {
        super.onLabelOptionsChangeEvent(e);
        m_labChanged = true;
    }

    private BufferedImage renderImage() {
        //converted version is guaranteed to be ? extends RealType => getNormalization and render should work
        //TODO find a way to relax type constraints from R extends RealType  to RealType

        RandomAccessibleInterval convertedImg = convertIfDouble(m_img);

        final double[] normParams = m_normalizationParameters.getNormalizationParameters(convertedImg, m_sel);

        m_greyRenderer.setNormalizationParameters(normParams[0], normParams[1]);

        final ScreenImage ret =
                m_greyRenderer.render(convertedImg, m_sel.getPlaneDimIndex1(), m_sel.getPlaneDimIndex2(),
                                      m_sel.getPlanePos());

        return AWTImageTools.makeBuffered(ret.image());
    }

    private BufferedImage renderTogether(final BufferedImage img, final BufferedImage labeling) {
        m_rgb = m_config.createCompatibleImage(img.getWidth(), img.getHeight(), java.awt.Transparency.TRANSLUCENT);
        m_rgbGraphics = m_rgb.getGraphics();

        m_rgbGraphics.drawImage(img, 0, 0, null);
        m_rgbGraphics.drawImage(Transparency.makeColorTransparent(labeling, Color.WHITE, m_transparency), 0, 0, null);

        return m_rgb;
    }

    private BufferedImage renderLabeling() {
        if (m_renderer instanceof RendererWithLabels) {
            final RendererWithLabels<L> r = (RendererWithLabels<L>)m_renderer;
            r.setActiveLabels(m_activeLabels);
            r.setOperator(m_operator);
            r.setLabelMapping(m_labelMapping);
            r.setRenderingWithLabelStrings(m_withLabelStrings);
        }

        if ((m_renderer instanceof RendererWithHilite) && (m_hilitedLabels != null)) {
            final RendererWithHilite r = (RendererWithHilite)m_renderer;
            r.setHilitedLabels(m_hilitedLabels);
            r.setHiliteMode(m_isHiliteMode);
        }

        if ((m_renderer instanceof LabelingColorTableRenderer)) {
            final LabelingColorTableRenderer r = (LabelingColorTableRenderer)m_renderer;
            r.setLabelingColorTable(m_labelingColorMapping);
        }

        final ScreenImage ret =
                m_renderer.render(m_src, m_sel.getPlaneDimIndex1(), m_sel.getPlaneDimIndex2(), m_sel.getPlanePos());

        return AWTImageTools.makeBuffered(ret.image());
    }

    @EventListener
    public void onUpdate(final TransparencyPanelValueChgEvent e) {
        m_transparency = e.getTransparency();
        m_labChanged = true;
    }

    @EventListener
    public void onLabelingUpdated(final ImgAndLabelingChgEvent<T, L> e) {
        m_img = e.getRandomAccessibleInterval();
        m_rowKey = e.getName().getName();

        m_imgChanged = true;
        m_labChanged = true;

    }

    @EventListener
    public void onClose(final ViewClosedEvent event) {
        m_img = null;
        m_greyRenderer = null;
        m_src = null;
        m_normalizationParameters = null;
        m_config = null;
        m_bufLab = null;
        m_bufImg = null;

        m_rgb = null;

        m_hilitedLabels = null;

        m_rgbGraphics = null;
    }

    /**
     * {@link EventListener} for {@link NormalizationParametersChgEvent} events The
     * {@link NormalizationParametersChgEvent} of the {@link AWTImageTools} will be updated
     * 
     * @param normalizationParameters
     */
    @EventListener
    public void onUpdated(final NormalizationParametersChgEvent normalizationParameters) {
        m_normalizationParameters = normalizationParameters;
        m_imgChanged = true;
    }

    @EventListener
    public void onUpdated(final HilitedLabelsChgEvent e) {
        m_hilitedLabels = e.getHilitedLabels();
        m_labChanged = true;
    }

    @EventListener
    public void onUpdated(final LabelPanelIsHiliteModeEvent e) {
        m_isHiliteMode = e.isHiliteMode();
        m_labChanged = true;
    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        super.saveComponentConfiguration(out);
        out.writeInt(m_transparency);
        m_normalizationParameters.writeExternal(out);
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.loadComponentConfiguration(in);
        m_transparency = in.readInt();
        m_normalizationParameters = new NormalizationParametersChgEvent();
        m_normalizationParameters.readExternal(in);
    }

}
