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
package org.knime.knip.core.awt.converter;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import org.knime.knip.core.awt.labelingcolortable.LabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableUtils;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter.Operator;

import gnu.trove.map.hash.TIntIntHashMap;
import net.imglib2.converter.Converter;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.ARGBType;

/**
 * TODO Auto-generated
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelingTypeARGBConverter<L> implements Converter<LabelingType<L>, ARGBType> {

    private static int WHITE_RGB = Color.WHITE.getRGB();

    private final TIntIntHashMap m_colorTable;

    private Set<String> m_hilitedLabels;

    private boolean m_isHiliteMode;

    private Operator m_operator;

    private Set<String> m_activeLabels;

    private LabelingColorTable m_colorMapping;

    public LabelingTypeARGBConverter(final LabelingColorTable colorMapping) {
        this.m_colorTable = new TIntIntHashMap();
        this.m_colorMapping = colorMapping;
    }

    public void setOperator(final Operator operator) {
        m_operator = operator;
    }

    public void setActiveLabels(final Set<String> activeLabels) {
        m_activeLabels = activeLabels;
    }

    public void setHilitedLabels(final Set<String> hilitedLabels) {
        m_hilitedLabels = hilitedLabels;
    }

    public void setHiliteMode(final boolean isHiliteMode) {
        m_isHiliteMode = isHiliteMode;
    }

    public void clear(){
        m_colorTable.clear();
    }

    @Override
    public void convert(final LabelingType<L> input, final ARGBType output) {

                if (!m_colorTable.contains(input.hashCode())) {
                    m_colorTable.put(input.hashCode(), getColorForLabeling(input));
                }
        output.set(m_colorTable.get(input.hashCode()));
    }

    private int getColorForLabeling(final Set<L> labeling) {

        if (labeling.size() == 0) {
            return WHITE_RGB;
        }

        // standard case no filtering / highlighting
        if ((m_activeLabels == null) && (m_hilitedLabels == null) && !m_isHiliteMode) {
            return LabelingColorTableUtils.<L> getAverageColor(m_colorMapping, labeling);
        }

        Set<L> filteredLabels;
        if (m_activeLabels != null) {
            filteredLabels = intersection(m_activeLabels, m_operator, labeling);
        } else {
            filteredLabels = labeling; // do not filter
        }

        if (filteredLabels.size() == 0) {
            return WHITE_RGB;
        } else {
            // highlight if necessary
            if (checkHilite(filteredLabels, m_hilitedLabels)) {
                return LabelingColorTableUtils.HILITED_RGB;
            } else {
                return m_isHiliteMode ? LabelingColorTableUtils.NOTSELECTED_RGB
                        : LabelingColorTableUtils.<L> getAverageColor(m_colorMapping, labeling);
            }
        }
    }

    private boolean checkHilite(final Set<L> labeling, final Set<String> hilitedLabels) {
        if ((hilitedLabels != null) && (hilitedLabels.size() > 0)) {
            for (final L label : labeling) {
                if (hilitedLabels.contains(label.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<L> intersection(final Set<String> activeLabels, final Operator op, final Set<L> labeling) {
        final Set<L> intersected = new HashSet<L>(4);

        if (op == Operator.OR) {
            for (final L label : labeling) {
                if (activeLabels.contains(label.toString())) {
                    intersected.add(label);
                }
            }
        } else if (op == Operator.AND) {

            if (labeling.containsAll(activeLabels)) {
                intersected.addAll(labeling);
            }
        } else if (op == Operator.XOR) {
            boolean addedOne = false;
            for (L label : labeling) {
                if (activeLabels.contains(label.toString())) {

                    if (!addedOne) {
                        intersected.add(label);
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

}
