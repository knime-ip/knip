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
package org.knime.knip.base.nodes.util.slicelooper;

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
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;

/**
 * Utils for slice loop node
 *
 * @author Andreas Graumann, University of Konstanz
 * @author Christian Dietz, Univesity of Konstanz
 */
public class SliceIteratorUtils {

    /**
     * Copies a RandomAccessibleInterval in another one
     *
     * @param input
     * @param output
     */
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

    /**
     *
     * @param in
     * @param i
     * @param fac
     * @return
     */
    static <T extends RealType<T> & NativeType<T>> Img<T> getImgPlusAsView(final Img<T> in, final Interval i,
                                                                           final ImgFactory<T> fac) {
        return new ImgView<T>(SubsetOperations.subsetview(in, i), fac);
    }

    /**
     *
     * @param in
     * @param i
     * @param fac
     * @return
     */
    static <L extends Comparable<L>> Labeling<L> getLabelingAsView(final Labeling<L> in, final Interval i,
                                                                   final LabelingFactory<L> fac) {
        return new LabelingView<L>(SubsetOperations.subsetview(in, i), fac);
    }

    /**
     *
     * @param inSpec
     * @param columns
     * @param logger
     * @return DataTableSpec
     */
    static DataTableSpec createResSpec(final DataTableSpec inSpec, final SettingsModelFilterString columns,
                                       final NodeLogger logger) {

        // store names and types of all valid columns
        ArrayList<String> names = new ArrayList<String>();
        ArrayList<DataType> types = new ArrayList<DataType>();

        // loop over all selected and valid columns
        for (Integer i : getSelectedAndValidIdx(inSpec, columns, logger)) {
            DataColumnSpec colSpec = inSpec.getColumnSpec(i);
            DataType colType = colSpec.getType();
            names.add(colSpec.getName());
            types.add(colType);
        }

        return new DataTableSpec(names.toArray(new String[names.size()]), types.toArray(new DataType[types.size()]));
    }


    /**
     *
     * @param inSpec
     * @param columns
     * @param logger
     * @return
     */
    static Integer[] getSelectedAndValidIdx(final DataTableSpec inSpec, final SettingsModelFilterString columns,
                                            final NodeLogger logger) {
        // Store all indexes of selected and valid columsn
        ArrayList<Integer> selectedIdx = new ArrayList<Integer>();

        // We have a column selection
        if (columns != null) {
            for (String col : columns.getIncludeList()) {
                final int colIdx = inSpec.findColumnIndex(col);
                DataColumnSpec colSpec = inSpec.getColumnSpec(colIdx);
                DataType colType = colSpec.getType();

                // only ImgPlus and Lablings are allowed
                if (colType.isCompatible(ImgPlusValue.class) || colType.isCompatible(LabelingValue.class)) {
                    selectedIdx.add(colIdx);
                } else {
                    logger.warn("Waning: Column " + colSpec.getName()
                            + " will be ignored! Only ImgPlusValue and LabelingValue are used!");
                }
            }
        }
        // the end node don't have a column selection, just look for valid columns
        else {
            int numCol = inSpec.getNumColumns();
            for (int i = 0; i < numCol; i++) {
                DataColumnSpec colSpec = inSpec.getColumnSpec(i);
                DataType colType = colSpec.getType();
                if (colType.isCompatible(ImgPlusValue.class) || colType.isCompatible(LabelingValue.class)) {
                    selectedIdx.add(i);
                }
                else {
                    logger.warn("Waning: Column " + colSpec.getName()
                            + " will be ignored! Only ImgPlusValue and LabelingValue are used!");
                }
            }
        }

        return selectedIdx.toArray(new Integer[selectedIdx.size()]);
    }

}
