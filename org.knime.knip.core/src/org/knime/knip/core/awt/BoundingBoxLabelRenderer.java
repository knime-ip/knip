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

import java.awt.Color;
import java.awt.Graphics;
import java.util.Set;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.display.ScreenImage;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.subset.views.LabelingView;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.type.Type;

import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableUtils;
import org.knime.knip.core.awt.parametersupport.RendererWithHilite;
import org.knime.knip.core.awt.parametersupport.RendererWithLabels;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter.Operator;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class BoundingBoxLabelRenderer<L extends Comparable<L> & Type<L>> implements ImageRenderer<LabelingType<L>>,
        RendererWithLabels<L>, RendererWithHilite {

    private final Color HILITED_RGB_COLOR = new Color(LabelingColorTableUtils.HILITED_RGB);

    //allows to bypass the segment color table
    private Color m_boxColor = null;

    private Color getBOX_RGB_COLOR() {
        if (m_boxColor != null) {
            return m_boxColor;
        } else {
            return LabelingColorTableUtils.getBoundingBoxColor();
        }
    }

    private Set<String> m_hilitedLabels;

    private Set<String> m_activeLabels;

    protected double m_scale = 1.0;

    protected boolean m_withLabelStrings = true;

    @Override
    public ScreenImage render(final RandomAccessibleInterval<LabelingType<L>> source, final int dimX, final int dimY,
                              final long[] planePos) {
        return render(dimX, dimY, planePos, source, m_activeLabels, m_scale, m_withLabelStrings);
    }

    private ScreenImage render(final int dimX, final int dimY, final long[] planePos,
                               final RandomAccessibleInterval<LabelingType<L>> labeling,
                               final Set<String> activeLabels, final double scale, final boolean withLabelString) {
        Labeling<L> subLab = null;
        if (labeling instanceof Labeling) {
            subLab = (Labeling<L>)labeling;
        } else {
            subLab = new LabelingView<L>(labeling, null);
        }

        if (subLab.numDimensions() > 2) {
            final long[] min = planePos.clone();
            final long[] max = planePos.clone();

            min[dimX] = 0;
            min[dimY] = 0;

            max[dimX] = subLab.max(dimX);
            max[dimY] = subLab.max(dimY);

            subLab =
                    new LabelingView<L>(SubsetOperations.subsetview(subLab, new FinalInterval(min, max)),
                            subLab.<L> factory());
        }

        final long[] dims = new long[subLab.numDimensions()];
        subLab.dimensions(dims);
        final int width = (int)Math.round(dims[dimX] * scale) + 1;
        final int height = (int)Math.round(dims[dimY] * scale) + 1;

        final ScreenImage res = createCanvas(width, height);
        final Graphics g = res.image().getGraphics();
        g.setColor(Color.black);

        for (final L label : subLab.getLabels()) {

            // test hilite
            if ((m_hilitedLabels != null) && m_hilitedLabels.contains(label)) {
                g.setColor(HILITED_RGB_COLOR);
            } else {
                g.setColor(getBOX_RGB_COLOR());
            }

            int X = 0;
            int Y = 1;
            if (dimX > dimY) {
                Y = 0;
                X = 1;
            }

            // test active labels (null = all active)
            if ((activeLabels == null) || activeLabels.contains(label)) {

                final IterableRegionOfInterest roi = subLab.getIterableRegionOfInterest(label);
                final Interval ii = roi.getIterableIntervalOverROI(subLab);
                g.drawRect((int)(ii.min(X) * scale), (int)(ii.min(Y) * scale), (int)((ii.dimension(X) - 1) * scale),
                           (int)((ii.dimension(Y) - 1) * scale));

                if (withLabelString) {
                    if (scale > .6) {

                        g.drawString(label.toString(), (int)((ii.min(X) + 1) * scale), (int)((ii.min(Y) + 10) * scale));
                    }
                }
            }
        }

        return res;
    }

    protected ScreenImage createCanvas(final int width, final int height) {
        final ScreenImage ret = new ARGBScreenImage(width, height);
        final Graphics g = ret.image().getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        return ret;
    }

    @Override
    public String toString() {
        return "Bounding Box Renderer";
    }

    public void setScale(final double scale) {
        m_scale = scale;
    }

    @Override
    public void setRenderingWithLabelStrings(final boolean withNumbers) {
        m_withLabelStrings = withNumbers;
    }

    @Override
    public void setHilitedLabels(final Set<String> hilitedLabels) {
        m_hilitedLabels = hilitedLabels;
    }

    @Override
    public void setActiveLabels(final Set<String> activeLabels) {
        m_activeLabels = activeLabels;
    }

    /**
     * explicitly defines a color for labelings and bounding boxes instead of using the color defined in the
     * SegmentColorTable. This is useful if e.g. the color has been defined during config time and should not depend the
     * SegmentColorTable.
     * 
     * 
     * @param bbColor a color for bounding boxes and labels or <code>null</code> to restore the default behavior (
     *            <code>SegmentColorTable.getBoundingBoxColor()</code>)
     */
    public void setBoxColor(final Color bbColor) {
        m_boxColor = bbColor;
    }

    @Override
    public void setHiliteMode(final boolean isHiliteMode) {
        // TODO: Nothing going on here
    }

    @Override
    public void setLabelMapping(final LabelingMapping<L> labelMapping) {
        // do nothing
    }

    @Override
    public void setOperator(final Operator operator) {
        // do nothing
    }
}
