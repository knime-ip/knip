/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2014
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
 * Created on 29.04.2014 by Christian Dietz
 */
package org.knime.knip.base.nodes.loops.slicelooper;

import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingFactory;
import net.imglib2.labeling.LabelingView;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;

/**
 *
 * @author Christian Dietz
 */
public class SliceLooperUtils {

    static <T extends Type<T>> void copy(final RandomAccessibleInterval<T> input,
                                         final RandomAccessibleInterval<T> output) {

        final Cursor<T> c1 = Views.flatIterable(input).cursor();
        final Cursor<T> c2 = Views.flatIterable(output).cursor();

        while (c1.hasNext()) {
            c1.fwd();
            c2.fwd();
            c2.get().set(c1.get());
        }
    }

    static <T extends RealType<T> & NativeType<T>> Img<T> getImgPlusAsView(final Img<T> in, final Interval i,
                                                                           final ImgFactory<T> fac) {
        return new ImgView<T>(SubsetOperations.subsetview(in, i), fac);
    }

    static <L extends Comparable<L>> Labeling<L> getLabelingAsView(final Labeling<L> in, final Interval i,
                                                                   final LabelingFactory<L> fac) {
        return new LabelingView<L>(SubsetOperations.subsetview(in, i), fac);
    }

    /**
     * @return
     */
    static DataTableSpec createResSpec(final NodeLogger logger, final DataTableSpec inSpec) {
        int numCol = inSpec.getNumColumns();

        ArrayList<String> names = new ArrayList<String>();
        ArrayList<DataType> types = new ArrayList<DataType>();

        for (int i = 0; i < numCol; i++) {
            DataColumnSpec colSpec = inSpec.getColumnSpec(i);

            DataType colType = colSpec.getType();
            if (!colType.isCompatible(ImgPlusValue.class) && !colType.isCompatible(LabelingValue.class)) {
                logger.warn("Waning: Column " + colSpec.getName()
                        + " will be ignored! Only ImgPlusValue and LabelingValue are used!");
            } else {
                names.add(colSpec.getName());
                types.add(colType);
            }
        }

        return new DataTableSpec(names.toArray(new String[names.size()]), types.toArray(new DataType[types.size()]));
    }

}
