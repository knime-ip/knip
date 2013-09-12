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
import java.awt.Image;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import net.imglib2.display.ScreenImage;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;

import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableRenderer;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableUtils;
import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.awt.parametersupport.RendererWithLabels;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.imgviewer.events.AWTImageChgEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelColoringChangeEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelOptionsChangeEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelPanelVisibleLabelsChgEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelingWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter.Operator;

/**
 * Creates an awt image from a plane selection, labeling and labeling renderer. Propagates {@link AWTImageChgEvent}.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelingBufferedImageProvider<L extends Comparable<L>> extends AWTImageProvider<LabelingType<L>> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private int m_colorMapGeneration = RandomMissingColorHandler.getGeneration();

    private Color m_boundingBoxColor = LabelingColorTableUtils.getBoundingBoxColor();

    protected Set<String> m_activeLabels;

    protected Operator m_operator;

    protected LabelingMapping<L> m_labelMapping;

    protected boolean m_withLabelStrings = false;

    protected LabelingColorTable m_labelingColorMapping;

    /**
     * TODO
     */
    public LabelingBufferedImageProvider(final int cacheSize) {
        super(cacheSize);
    }

    @Override
    @EventListener
    public void onUpdated(final IntervalWithMetadataChgEvent<LabelingType<L>> e) {
        m_labelMapping = e.getIterableInterval().firstElement().getMapping();
        m_labelingColorMapping =
                LabelingColorTableUtils.extendLabelingColorTable(((LabelingWithMetadataChgEvent)e)
                        .getLabelingMetaData().getLabelingColorTable(), new RandomMissingColorHandler());

        super.onUpdated(e);
    }

    @EventListener
    public void onLabelColoringChangeEvent(final LabelColoringChangeEvent e) {
        m_colorMapGeneration = e.getColorMapNr();
        m_boundingBoxColor = e.getBoundingBoxColor();
    }

    @EventListener
    public void onLabelOptionsChangeEvent(final LabelOptionsChangeEvent e) {
        m_withLabelStrings = e.getRenderWithLabelStrings();
    }

    @Override
    protected int generateHashCode() {

        int hash = super.generateHashCode();

        if (m_activeLabels != null) {
            hash *= 31;
            hash += m_activeLabels.hashCode();
            hash *= 31;
            hash += m_operator.ordinal();
        }

        hash *= 31;
        hash += m_boundingBoxColor.hashCode();
        hash *= 31;
        hash += m_colorMapGeneration;
        hash *= 31;
        if (m_withLabelStrings) {
            hash += 1;
        } else {
            hash += 2;
        }

        return hash;
    }

    @Override
    protected Image createImage() {
        if (m_renderer instanceof RendererWithLabels) {
            final RendererWithLabels<L> r = (RendererWithLabels<L>)m_renderer;
            r.setActiveLabels(m_activeLabels);
            r.setOperator(m_operator);
            r.setLabelMapping(m_labelMapping);
            r.setRenderingWithLabelStrings(m_withLabelStrings);
        }

        if (m_renderer instanceof LabelingColorTableRenderer) {
            final LabelingColorTableRenderer r = (LabelingColorTableRenderer)m_renderer;
            r.setLabelingColorTable(m_labelingColorMapping);
        }

        final ScreenImage ret =
                m_renderer.render(m_src, m_sel.getPlaneDimIndex1(), m_sel.getPlaneDimIndex2(), m_sel.getPlanePos());

        return AWTImageTools.makeBuffered(ret.image());
    }

    @EventListener
    public void onUpdate(final LabelPanelVisibleLabelsChgEvent e) {
        m_activeLabels = e.getLabels();
        m_operator = e.getOperator();
    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        super.saveComponentConfiguration(out);
        out.writeObject(m_activeLabels);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.loadComponentConfiguration(in);
        m_activeLabels = (Set<String>)in.readObject();
    }
}
