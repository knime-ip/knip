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

import java.awt.Color;
import java.awt.Image;
import java.util.Set;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.screenimage.awt.AWTScreenImage;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;

import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableRenderer;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableUtils;
import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.awt.parametersupport.RendererWithHilite;
import org.knime.knip.core.awt.parametersupport.RendererWithLabels;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorResetEvent;
import org.knime.knip.core.ui.imgviewer.events.HilitedLabelsChgEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelColoringChangeEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelOptionsChangeEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelPanelIsHiliteModeEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelPanelVisibleLabelsChgEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelingWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter.Operator;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;

/*
 * This class could be likely split into multiple following the one class one responsibility paradigm. However
 * its closer to the original implementation of the AWTImageProvider descendants.
 *
 * TODO split it after a migration phase if the new implementation stays
 */
/**
 * Combined label renderer. Supports label rendering, with filters, with hilite, with color tables with/without strings
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <L> labeling based type of the rendered source
 */
public class LabelingRU<L extends Comparable<L>> extends AbstractDefaultRU<LabelingType<L>> {

    /** Identifying hashCode of the last rendered image. */
    private int m_hashOfLastRendering;

    /** caches the last rendered labeling as {@link Image}. */
    private Image m_lastImage;

    // event members

    private RandomAccessibleInterval<LabelingType<L>> m_src;

    private LabelingColorTable m_labelingColorMapping;

    private LabelingMapping<L> m_labelMapping;

    private Set<String> m_activeLabels;

    private Operator m_operator;

    private int m_colorMapGeneration = RandomMissingColorHandler.getGeneration();

    private Color m_boundingBoxColor = LabelingColorTableUtils.getBoundingBoxColor();

    private boolean m_withLabelStrings = false;

    /* hilite */

    private Set<String> m_hilitedLabels;

    private boolean m_isHiliteMode = false;

    @Override
    public Image createImage() {
        if (m_lastImage != null && m_hashOfLastRendering == generateHashCode()) {
            return m_lastImage;
        }

        if (m_renderer instanceof RendererWithLabels) {
            @SuppressWarnings("unchecked")
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

        if (m_renderer instanceof LabelingColorTableRenderer) {
            final LabelingColorTableRenderer r = (LabelingColorTableRenderer)m_renderer;
            r.setLabelingColorTable(m_labelingColorMapping);
        }

        final AWTScreenImage ret =
                m_renderer.render(m_src, m_planeSelection.getPlaneDimIndex1(), m_planeSelection.getPlaneDimIndex2(),
                                  m_planeSelection.getPlanePos());

        m_hashOfLastRendering = generateHashCode();
        m_lastImage = ret.image();

        return AWTImageTools.makeBuffered(ret.image());
    }

    @Override
    public int generateHashCode() {
        int hash = super.generateHashCode();
        if (isActive()) {
            ////////
            hash += m_src.hashCode();
            hash *= 31;
            hash += m_labelingColorMapping.hashCode();
            hash *= 31;
            hash += m_labelMapping.hashCode();
            hash *= 31;
            ////////
            if (m_activeLabels != null) {
                hash += m_activeLabels.hashCode();
                hash *= 31;
                hash += m_operator.ordinal();
                hash *= 31;
            }
            ////////
            hash += m_boundingBoxColor.hashCode();
            hash *= 31;
            hash += m_colorMapGeneration;
            hash *= 31;
            ////////
            if (m_withLabelStrings) {
                hash += 1;
            } else {
                hash += 2;
            }
            hash *= 31;
            /////////
            if (m_isHiliteMode) {
                hash = (hash * 31) + 1;
            }
            if ((m_hilitedLabels != null)) {
                hash = (hash * 31) + m_hilitedLabels.hashCode();
            }
            /////////
        }
        return hash;
    }

    @Override
    public boolean isActive() {
        return (m_src != null);
    }

    //event handling

    /**
     * @param e updates the stored labeling that is the source of the created image. Also updates related member
     *            variables.
     */
    @EventListener
    public void onUpdated(final LabelingWithMetadataChgEvent<L> e) {
        m_src = e.getRandomAccessibleInterval();
        m_labelMapping = e.getIterableInterval().firstElement().getMapping();
        m_labelingColorMapping =
                LabelingColorTableUtils.extendLabelingColorTable(e.getLabelingMetaData().getLabelingColorTable(),
                                                                 new RandomMissingColorHandler());
    }

    /**
     * @param e updates the stored colorMap and boundingBoxColor
     */
    @EventListener
    public void onLabelColoringChangeEvent(final LabelColoringChangeEvent e) {
        m_colorMapGeneration = e.getColorMapNr();
        m_boundingBoxColor = e.getBoundingBoxColor();
    }

    /**
     * @param e switches rendering with label string names on/off
     */
    @EventListener
    public void onLabelOptionsChangeEvent(final LabelOptionsChangeEvent e) {
        m_withLabelStrings = e.getRenderWithLabelStrings();
    }

    /**
     * stores active labels and filter operators in members.
     *
     * @param e issued if labels get filtered.
     */
    @EventListener
    public void onUpdate(final LabelPanelVisibleLabelsChgEvent e) {
        m_activeLabels = e.getLabels();
        m_operator = e.getOperator();
    }

    /**
     * stores hilited labels in a member.
     *
     * @param e contains all hilited labels.
     */
    @EventListener
    public void onUpdated(final HilitedLabelsChgEvent e) {
        m_hilitedLabels = e.getHilitedLabels();
    }

    @EventListener
    public void onUpdated(final LabelPanelIsHiliteModeEvent e) {
        m_isHiliteMode = e.isHiliteMode();
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
        m_labelingColorMapping = null;
        m_labelMapping = null;
        m_activeLabels = null;
        m_operator = null;
        m_colorMapGeneration = RandomMissingColorHandler.getGeneration();
        m_boundingBoxColor = LabelingColorTableUtils.getBoundingBoxColor();
        m_hilitedLabels = null;
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

    // standard methods

}
