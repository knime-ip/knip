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

import java.util.Iterator;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.LabelingView;

import org.knime.core.data.DataRow;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.core.data.img.LabelingMetadata;

/**
 * Compares two labelings
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <L>
 */
public class LabelingComparatorNodeModel<L extends Comparable<L>> extends
        ComparatorNodeModel<LabelingValue<L>, LabelingValue<L>> {

    @Override
    protected void compare(final DataRow row, final LabelingValue<L> vin1, final LabelingValue<L> vin2) {

        final Labeling<L> labeling1 = vin1.getLabeling();
        final Labeling<L> labeling2 = vin2.getLabeling();

        final LabelingMetadata meta1 = vin1.getLabelingMetadata();
        final LabelingMetadata meta2 = vin2.getLabelingMetadata();

        if (labeling1.factory().getClass() != labeling2.factory().getClass()) {
            throw new IllegalStateException("Factory of the images are not the same! " + row.getKey().toString());
        }

        if (labeling1.numDimensions() != labeling2.numDimensions()) {
            throw new IllegalStateException("Number of dimensions of images is not the same! "
                    + row.getKey().toString());
        }

        for (int d = 0; d < labeling1.numDimensions(); d++) {
            if (labeling1.dimension(d) != labeling2.dimension(d)) {
                throw new IllegalStateException("Dimension " + d + " is not the same in the compared images!"
                        + row.getKey().toString());
            }
        }

        if (!meta1.getName().equalsIgnoreCase(meta2.getName())) {
            throw new IllegalStateException("Names of labelings are not the same! " + row.getKey().toString());
        }

        if (!meta1.getSource().equalsIgnoreCase(meta2.getSource())) {
            throw new IllegalStateException("Sources of labelings are not the same! " + row.getKey().toString());
        }

        for (int d = 0; d < labeling1.numDimensions(); d++) {
            if (!meta1.axis(d).generalEquation().equalsIgnoreCase(meta2.axis(d).generalEquation())) {
                throw new IllegalStateException("GeneralEquation of CalibratedAxis " + d
                        + " is not the same in the compared labelings!" + row.getKey().toString());
            }
        }

        for (int d = 0; d < labeling1.numDimensions(); d++) {
            if (!meta1.axis(d).type().getLabel().equalsIgnoreCase(meta2.axis(d).type().getLabel())) {
                throw new IllegalStateException("Label of Axis " + d + " is not the same in the compared labelings!"
                        + row.getKey().toString());
            }
        }

        final Cursor<LabelingType<L>> c1;
        final Cursor<LabelingType<L>> c2;

        if (labeling1 instanceof LabelingView || labeling2 instanceof LabelingView) {
            c1 = new LabelingView<L>(labeling1, null).cursor();
            c2 = new LabelingView<L>(labeling2, null).cursor();
        } else {
            c1 = labeling1.cursor();
            c2 = labeling2.cursor();
        }

        while (c1.hasNext()) {
            c1.fwd();
            c2.fwd();

            final List<L> list1 = c1.get().getLabeling();
            final List<L> list2 = c2.get().getLabeling();

            if (list1.size() == list2.size()) {
                final Iterator<L> iter1 = list1.iterator();
                final Iterator<L> iter2 = list2.iterator();

                while (iter1.hasNext()) {
                    final L t1 = iter1.next();
                    final L t2 = iter2.next();

                    if (!t1.equals(t2)) {
                        throw new IllegalStateException("Two labelings are not the same" + row.getKey().toString());
                    }
                }
            } else {
                throw new IllegalStateException("Two labelings are not the same" + row.getKey().toString());
            }
        }
    }
}
