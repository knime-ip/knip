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
package org.knime.knip.core.ops.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.imglib2.Cursor;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.UnaryOutputOperation;

import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter;

/**
 * Dependencies of labels to each other are computed. e.g. if one LabelingType contains two labels A and B, then A has
 * reflexive relation to B.
 * 
 * The node can be used in two modes:
 * 
 * a. Intersection mode: A must appear at least once together with B two have a relation to B.
 * 
 * b. Complete mode: A must always appear with B two have a relation to B.
 * 
 * Two filters are helping to reduce the amount of labels, for which the relations are computed. 1. The left one filters
 * the labels on the left left side of the relation 2. The right one filters the labels on the right side of the
 * relation. Both filters use the given Rules {@link RulebasedLabelFilter}.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelingDependency<L extends Comparable<L>> implements UnaryOutputOperation<Labeling<L>, Map<L, List<L>>> {

    private final RulebasedLabelFilter<L> m_leftFilter;

    private final RulebasedLabelFilter<L> m_rightFilter;

    private final boolean m_intersectionMode;

    /**
     * @param leftFilter
     * @param rightFilter
     * @param intersectionMode
     */
    public LabelingDependency(final RulebasedLabelFilter<L> leftFilter, final RulebasedLabelFilter<L> rightFilter,
                              final boolean intersectionMode) {
        m_leftFilter = leftFilter;
        m_rightFilter = rightFilter;
        m_intersectionMode = intersectionMode;
    }

    @Override
    public Map<L, List<L>> compute(final Labeling<L> op, final Map<L, List<L>> r) {

        final HashMap<L, HashMap<L, Integer>> labelMap = new HashMap<L, HashMap<L, Integer>>();
        final HashMap<L, Integer> sizeMap = new HashMap<L, Integer>();

        final Cursor<LabelingType<L>> cursor = op.cursor();

        while (cursor.hasNext()) {
            cursor.fwd();

            if (cursor.get().getLabeling().isEmpty()) {
                continue;
            }

            for (final L outerL : m_leftFilter.filterLabeling(cursor.get().getLabeling())) {

                if (!labelMap.containsKey(outerL)) {
                    labelMap.put(outerL, new HashMap<L, Integer>());
                    sizeMap.put(outerL, 0);
                }

                for (final L innerL : m_rightFilter.filterLabeling(cursor.get().getLabeling())) {
                    if (outerL.equals(innerL)) {
                        continue;
                    }

                    if (!labelMap.get(outerL).containsKey(innerL)) {
                        labelMap.get(outerL).put(innerL, 0);
                    }

                    labelMap.get(outerL).put(innerL, labelMap.get(outerL).get(innerL) + 1);
                }

                if (!m_intersectionMode) {
                    sizeMap.put(outerL, sizeMap.get(outerL) + 1);
                }
            }
        }

        for (final Entry<L, HashMap<L, Integer>> outerEntry : labelMap.entrySet()) {
            final List<L> members = new ArrayList<L>();
            if (sizeMap.get(outerEntry.getKey()) > 0) {
                final HashMap<L, Integer> hashMap = outerEntry.getValue();
                for (final Entry<L, Integer> entry : hashMap.entrySet()) {
                    if (entry.getValue().equals(sizeMap.get(outerEntry.getKey()))) {
                        members.add(entry.getKey());
                    }
                }

            } else {
                for (final L groupMember : outerEntry.getValue().keySet()) {
                    members.add(groupMember);
                }
            }

            if ((members.size() > 0) || (m_rightFilter.getRules().size() == 0)) {
                r.put(outerEntry.getKey(), members);
            }
        }
        return r;

    }

    @Override
    public UnaryOutputOperation<Labeling<L>, Map<L, List<L>>> copy() {
        return new LabelingDependency<L>(m_leftFilter.copy(), m_leftFilter.copy(), m_intersectionMode);
    }

    @Override
    public UnaryObjectFactory<Labeling<L>, Map<L, List<L>>> bufferFactory() {
        return new UnaryObjectFactory<Labeling<L>, Map<L, List<L>>>() {

            @Override
            public Map<L, List<L>> instantiate(final Labeling<L> a) {
                return new HashMap<L, List<L>>();
            }
        };
    }
}
