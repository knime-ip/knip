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
package org.knime.knip.core.ops.labeling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.Cursor;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;
import net.imglib2.ops.operation.UnaryOperation;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class MakeStringLabeling<L extends Comparable<L>> implements UnaryOperation<Labeling<L>, Labeling<String>> {

    @Override
    public Labeling<String> compute(final Labeling<L> op, final Labeling<String> res) {

        // String based Labeling is generated
        final long[] dims = new long[op.numDimensions()];
        op.dimensions(dims);

        final LabelingMapping<L> srcMapping = op.firstElement().getMapping();

        int size = 0;
        try {
            for (size = 0; size < Integer.MAX_VALUE; size++) {
                srcMapping.listAtIndex(size);
            }
        } catch (final IndexOutOfBoundsException e) {
            //
        }

        final LabelingMapping<String> resMapping = res.firstElement().getMapping();
        final Map<List<L>, List<String>> map = new HashMap<List<L>, List<String>>();
        for (int i = 0; i < size; i++) {
            final List<L> from = srcMapping.listAtIndex(i);
            final List<String> to = new ArrayList<String>(from.size());
            for (final L type : from) {
                to.add(type.toString());
            }

            map.put(from, resMapping.intern(to));
        }

        final Cursor<LabelingType<L>> inCursor = op.cursor();
        final Cursor<LabelingType<String>> resCursor = res.cursor();

        while (inCursor.hasNext()) {
            inCursor.fwd();
            resCursor.fwd();
            resCursor.get().setLabeling(map.get(inCursor.get().getLabeling()));
        }

        return res;
    }

    @Override
    public UnaryOperation<Labeling<L>, Labeling<String>> copy() {
        return new MakeStringLabeling<L>();
    }
}
