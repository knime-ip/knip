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
package org.knime.knip.core.awt;

import java.awt.Graphics;
import java.util.Set;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.display.ScreenImage;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;
import net.imglib2.type.Type;

import org.knime.knip.core.awt.labelingcolortable.LabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableRenderer;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter.Operator;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class BoundingBoxRandomColorLabelRenderer<L extends Comparable<L> & Type<L>> extends BoundingBoxLabelRenderer<L>
        implements LabelingColorTableRenderer {

    private final ColorLabelingRenderer<L> m_labelRenderer;

    private RandomAccessibleInterval<LabelingType<L>> m_source;

    private int m_dimX;

    private int m_dimY;

    private long[] m_planePos;

    private LabelingColorTable m_mapping;

    public BoundingBoxRandomColorLabelRenderer() {
        m_labelRenderer = new ColorLabelingRenderer<L>();
    }

    @Override
    public ScreenImage render(final RandomAccessibleInterval<LabelingType<L>> source, final int dimX, final int dimY,
                              final long[] planePos) {
        m_dimX = dimX;
        m_dimY = dimY;
        m_source = source;
        m_planePos = planePos.clone();

        return super.render(source, dimX, dimY, planePos);
    }

    @Override
    protected ScreenImage createCanvas(final int width, final int height) {

        final ScreenImage ret = new ARGBScreenImage(width, height);
        m_labelRenderer.setLabelingColorTable(m_mapping);
        final ScreenImage labelRendererResult = m_labelRenderer.render(m_source, m_dimX, m_dimY, m_planePos);
        final Graphics g = ret.image().getGraphics();
        g.drawImage(labelRendererResult.image(), 0, 0, width, height, null);

        return ret;
    }

    @Override
    public String toString() {
        return "Bounding Box Color Labeling Renderer";
    }

    @Override
    public void setHilitedLabels(final Set<String> hilitedLabels) {
        super.setHilitedLabels(hilitedLabels);
        m_labelRenderer.setHilitedLabels(hilitedLabels);
    }

    @Override
    public void setActiveLabels(final Set<String> activeLabels) {
        super.setActiveLabels(activeLabels);
        m_labelRenderer.setActiveLabels(activeLabels);
    }

    @Override
    public void setHiliteMode(final boolean isHiliteMode) {
        super.setHiliteMode(isHiliteMode);
        m_labelRenderer.setHiliteMode(isHiliteMode);
    }

    @Override
    public void setLabelMapping(final LabelingMapping<L> labelMapping) {
        super.setLabelMapping(labelMapping);
        m_labelRenderer.setLabelMapping(labelMapping);
    }

    @Override
    public void setOperator(final Operator operator) {
        super.setOperator(operator);
        m_labelRenderer.setOperator(operator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLabelingColorTable(final LabelingColorTable mapping) {
        m_mapping = mapping;
    }
}
