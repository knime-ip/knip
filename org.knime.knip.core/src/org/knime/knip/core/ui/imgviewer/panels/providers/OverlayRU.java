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

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

import net.imglib2.labeling.LabelingType;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorResetEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorImgAndOverlayChgEvent;
import org.knime.knip.core.ui.imgviewer.events.OverlayChgEvent;
import org.knime.knip.core.ui.imgviewer.events.TransparencyPanelValueChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;
import org.knime.knip.core.ui.imgviewer.overlay.Overlay;

/**
 * Renders the overlay primitives that affect the currently selected plane in a 2D image on top of a background image.
 * The background image is included to allow live updates during drawing without the need to blend images together.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @param <L>
 * @param <T>
 */
public class OverlayRU<L extends Comparable<L>> extends AbstractDefaultRU<LabelingType<L>> {

    /** context that allows to create graphics that fit the environment OS .. */
    private final GraphicsConfiguration m_graphicsConfig = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice().getDefaultConfiguration();

    /**
     * stores a canvas to allow faster writing if the required size doesn't change between two {@link #createImage}
     * calls.
     */
    private BufferedImage m_tmpCanvas;

    /** the background renderer is used to create the image beneath the overlay primitives. */
    private RenderUnit m_backgroundRenderer;

    // event members

    private Overlay<L> m_overlay;

    private long[] m_srcDims;

    private int m_transparency = 128;

    /**
     * @param ru {@link RenderUnit} for background rendering. Overlay images are applied on top of it.
     */
    public OverlayRU(final RenderUnit ru) {
        m_backgroundRenderer = ru;
    }

    @Override
    public Image createImage() {
        long width = m_srcDims[m_planeSelection.getPlaneDimIndex1()];
        long height = m_srcDims[m_planeSelection.getPlaneDimIndex2()];

        if ((m_tmpCanvas == null) || (m_tmpCanvas.getWidth() != width) || (m_tmpCanvas.getHeight() != height)) {
            m_tmpCanvas = m_graphicsConfig.createCompatibleImage((int)width, (int)height, Transparency.TRANSLUCENT);
        }
        Graphics g = m_tmpCanvas.getGraphics();
        g.drawImage(m_backgroundRenderer.createImage(), 0, 0, null);

        m_overlay.renderBufferedImage(g, m_planeSelection.getDimIndices(), m_planeSelection.getPlanePos(),
                                      m_transparency);

        return m_tmpCanvas;
    }

    @Override
    public int generateHashCode() {
        int hash = super.generateHashCode();
        if (isActive()) {
            hash += m_overlay.hashCode();
            hash *= 31;
            hash += m_backgroundRenderer.generateHashCode();
            hash *= 31;
        }
        return hash;
    }

    @Override
    public boolean isActive() {
        return (m_overlay != null && m_backgroundRenderer.isActive());
    }

    //event handling

    /**
     * @param e update the overlay member.
     */
    @EventListener
    public void onUpdated(final OverlayChgEvent e) {
        m_overlay = e.getOverlay();
    }

    /**
     * @param e update overlay member and readout dims of the annotated image.
     */
    @EventListener
    public void onUpdated(final AnnotatorImgAndOverlayChgEvent e) {
        m_overlay = e.getOverlay();
        m_srcDims = new long[e.getImg().numDimensions()];
        e.getImg().dimensions(m_srcDims);
    }

    /**
     * The transparency value determines how the overlay is rendered on top of the image.
     *
     * @param e transparency value used for blending
     */
    @EventListener
    public void onUpdate(final TransparencyPanelValueChgEvent e) {
        m_transparency = e.getTransparency();
    }

    /**
     * set all members that could hold expensive references to null or resets them to allow storage clean ups.
     *
     * @param event marker event
     */
    @EventListener
    public void onClose2(final ViewClosedEvent event) {
        m_tmpCanvas = null;
        m_overlay = null;
    }

    /**
     * @param event  {@link #onClose2()}
     */
    @EventListener
    public void onAnnotatorReset(final AnnotatorResetEvent event) {
        //we need this because annotators are currently placed in dialogs. Unlike views dialogs
        //are not recreated on reopening. Therefore annotators can't use the ViewClosedEvent that
        //destroys some of the ViewerComponents (on a view they would be recreated).
        //=> RenderUnits listen to AnnotatorResetEvents as well
        onClose2(new ViewClosedEvent());
    }

    //standard methods

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService service) {
        super.setEventService(service);
        m_backgroundRenderer.setEventService(service);
    }

}
