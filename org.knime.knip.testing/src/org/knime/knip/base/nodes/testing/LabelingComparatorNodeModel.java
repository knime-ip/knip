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
package org.knime.knip.base.nodes.testing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;

import org.knime.knip.base.data.labeling.LabelingValue;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelingComparatorNodeModel<L extends Comparable<L>> extends
        ComparatorNodeModel<LabelingValue<L>, LabelingValue<L>> {

    @Override
    protected void compare(final LabelingValue<L> vin1, final LabelingValue<L> vin2) {
        final Labeling<L> labeling1 = vin1.getLabelingCopy();
        final Labeling<L> labeling2 = vin2.getLabelingCopy();

        final Cursor<LabelingType<L>> c1 = labeling1.cursor();
        final Cursor<LabelingType<L>> c2 = labeling2.cursor();

        final HashMap<L, L> labelingMapping = new HashMap<L, L>();

        while (c1.hasNext()) {
            c1.fwd();
            c2.fwd();

            final List<L> list1 = c1.get().getLabeling();
            final List<L> list2 = c2.get().getLabeling();

            boolean eq = true;
            if (list1.size() == list2.size()) {
                final Iterator<L> iter1 = list1.iterator();
                final Iterator<L> iter2 = list2.iterator();

                while (iter1.hasNext()) {
                    final L t1 = iter1.next();
                    final L t2 = iter2.next();

                    if (labelingMapping.containsKey(t1)) {
                        // we have already a hypothesis
                        // what the labeling is
                        if (!labelingMapping.get(t1).equals(t2)) {
                            // oh oh error
                            eq = false;
                            break;
                        }
                    } else {
                        labelingMapping.put(t1, t2);
                    }
                }
            } else {
                eq = false;
            }

            if (!eq) {
                throw new IllegalStateException("Two images are not the same");
            }
        }
    }

}
