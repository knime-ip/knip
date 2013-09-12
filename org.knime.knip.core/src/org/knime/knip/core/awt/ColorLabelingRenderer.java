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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.display.projectors.Abstract2DProjector;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;
import net.imglib2.type.numeric.ARGBType;

import org.apache.mahout.math.map.OpenIntIntHashMap;
import org.knime.knip.core.awt.converter.LabelingTypeARGBConverter;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableRenderer;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableUtils;
import org.knime.knip.core.awt.parametersupport.RendererWithHilite;
import org.knime.knip.core.awt.parametersupport.RendererWithLabels;
import org.knime.knip.core.awt.specializedrendering.Projector2D;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter.Operator;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ColorLabelingRenderer<L extends Comparable<L>> extends ProjectingRenderer<LabelingType<L>> implements
        RendererWithLabels<L>, RendererWithHilite, LabelingColorTableRenderer {

    private static int WHITE_RGB = Color.WHITE.getRGB();

    private LabelingTypeARGBConverter<L> m_converter;

    private Operator m_operator;

    private Set<String> m_activeLabels;

    private LabelingMapping<L> m_labelMapping;

    private Set<String> m_hilitedLabels;

    private boolean m_isHiliteMode;

    private boolean m_rebuildRequired;

    private LabelingColorTable m_colorMapping;

    public ColorLabelingRenderer() {
        m_rebuildRequired = true;
    }

    @Override
    public void setOperator(final Operator operator) {
        m_operator = operator;
        m_rebuildRequired = true;
    }

    @Override
    public void setActiveLabels(final Set<String> activeLabels) {
        m_activeLabels = activeLabels;
        m_rebuildRequired = true;
    }

    @Override
    public void setLabelMapping(final LabelingMapping<L> labelMapping) {
        m_labelMapping = labelMapping;
        m_rebuildRequired = true;
    }

    @Override
    public void setHilitedLabels(final Set<String> hilitedLabels) {
        m_hilitedLabels = hilitedLabels;
        m_rebuildRequired = true;
    }

    @Override
    public void setHiliteMode(final boolean isHiliteMode) {
        m_isHiliteMode = isHiliteMode;
        m_rebuildRequired = true;
    }

    @Override
    public String toString() {
        return "Random Color Labeling Renderer";
    }

    @Override
    protected Abstract2DProjector<LabelingType<L>, ARGBType>
            getProjector(final int dimX, final int dimY, final RandomAccessibleInterval<LabelingType<L>> source,
                         final ARGBScreenImage target) {

        if (m_rebuildRequired) {
            m_rebuildRequired = false;
            rebuildLabelConverter();
        }

        return new Projector2D<LabelingType<L>, ARGBType>(dimX, dimY, source, target, m_converter);
    }

    // create the converter

    private void rebuildLabelConverter() {
        m_rebuildRequired = false;
        final int labelListIndexSize = m_labelMapping.numLists();
        final OpenIntIntHashMap colorTable = new OpenIntIntHashMap();

        for (int i = 0; i < labelListIndexSize; i++) {

            final int color =
                    getColorForLabeling(m_activeLabels, m_operator, m_hilitedLabels, m_isHiliteMode,
                                        m_labelMapping.listAtIndex(i));
            colorTable.put(i, color);
        }

        m_converter = new LabelingTypeARGBConverter<L>(colorTable);
    }

    private int getColorForLabeling(final Set<String> activeLabels, final Operator op, final Set<String> hilitedLabels,
                                    final boolean isHiliteMode, final List<L> labeling) {

        if (labeling.size() == 0) {
            return WHITE_RGB;
        }

        // standard case no filtering / highlighting
        if ((activeLabels == null) && (hilitedLabels == null) && !isHiliteMode) {
            return LabelingColorTableUtils.<L> getAverageColor(m_colorMapping, labeling);
        }

        List<L> filteredLabels;
        if (activeLabels != null) {
            filteredLabels = intersection(activeLabels, op, labeling);
        } else {
            filteredLabels = labeling; // do not filter
        }

        if (filteredLabels.size() == 0) {
            return WHITE_RGB;
        } else {
            // highlight if necessary
            if (checkHilite(filteredLabels, hilitedLabels)) {
                return LabelingColorTableUtils.HILITED_RGB;
            } else {
                return isHiliteMode ? LabelingColorTableUtils.NOTSELECTED_RGB : LabelingColorTableUtils
                        .<L> getAverageColor(m_colorMapping, labeling);
            }
        }
    }

    private boolean checkHilite(final List<L> labeling, final Set<String> hilitedLabels) {
        if ((hilitedLabels != null) && (hilitedLabels.size() > 0)) {
            for (int i = 0; i < labeling.size(); i++) {
                if (hilitedLabels.contains(labeling.get(i).toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<L> intersection(final Set<String> activeLabels, final Operator op, final List<L> labeling) {

        final List<L> intersected = new ArrayList<L>(4);

        if (op == Operator.OR) {
            for (int i = 0; i < labeling.size(); i++) {
                if (activeLabels.contains(labeling.get(i).toString())) {
                    intersected.add(labeling.get(i));
                }
            }
        } else if (op == Operator.AND) {
            if (labeling.containsAll(activeLabels)) {
                intersected.addAll(labeling);
            }
        } else if (op == Operator.XOR) {
            boolean addedOne = false;
            for (int i = 0; i < labeling.size(); i++) {
                if (activeLabels.contains(labeling.get(i).toString())) {

                    if (!addedOne) {
                        intersected.add(labeling.get(i));
                        addedOne = true;
                    } else {
                        // only 0,1 or 1,0 should result
                        // in a XOR labeling
                        intersected.clear();
                        break;
                    }
                }
            }
        }

        return intersected;
    }

    @Override
    public void setRenderingWithLabelStrings(final boolean withNumbers) {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLabelingColorTable(final LabelingColorTable mapping) {
        m_rebuildRequired = true;
        m_colorMapping = mapping;
    }

}
