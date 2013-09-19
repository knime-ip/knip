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
 * ---------------------------------------------------------------------
 *
 * Created on Sep 13, 2013 by squareys
 */
package org.knime.knip.base.nodes.seg.waehlby;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss.Gauss;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.ops.operation.BinaryObjectFactory;
import net.imglib2.ops.operation.BinaryOutputOperation;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.DistanceMap;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class WaehlbySplitterOp<L extends Comparable<L>, T extends RealType<T>> implements
        BinaryOutputOperation<Labeling<L>, RandomAccessibleInterval<T>, Labeling<L>> {

    /**
     * Segmentation type enum
     *
     * @author squareys
     */
    public enum SEG_TYPE {
        /**
         * Shape based segmentation.
         */
        SHAPE_BASED_SEGMENTATION
    }

    private SEG_TYPE m_segType;

    protected int m_gaussSize;

    /**
     * Contructor for WaehlbySplitter operation.
     *
     * @param segtype
     */
    public WaehlbySplitterOp(final SEG_TYPE segtype) {
        super();
        m_segType = segtype;

        m_gaussSize = 10;
    }

    private long[] getDimensions (final RandomAccessibleInterval<T> img) {
        long[] array = new long[img.numDimensions()];

        for(int i = 0; i < img.numDimensions(); i++) {
            array[i] = img.dimension(i);
        }

        return array;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Labeling<L> compute(final Labeling<L> inLab, final RandomAccessibleInterval<T> img, final Labeling<L> outLab) {

//        Labeling<L> seeds = new LabelingFactory().create(inLab);
        Img<FloatType> result = new ArrayImgFactory<FloatType>().create(getDimensions(img), new FloatType());

        if(m_segType == SEG_TYPE.SHAPE_BASED_SEGMENTATION){
            Img<FloatType> fimg = /* I need something here */ null;

            /* Start with distance transform */
            new DistanceMap<T, RandomAccessibleInterval<T>, Img<FloatType>>().compute(img, result);
            /* Gaussian smoothing */
            Gauss.toFloat(m_gaussSize, result);
        } else {
            /* Gaussian smoothing */
            Gauss.toFloat(m_gaussSize, result);
        }

        /* Disc dilation */

        /* Combine Images */
        //(S) Combine, if src1 < src2, else set as background

        /* Label the found Minima */

        /* Seeded Watershed */

        // Some "transformImageIf"

        /* Object Merge */

        /* weird complex algorithm with CrackContourCirculation */

        /* Merge objects */

        /* transform Images if ... */

        /* hole filling */

        /* Copy image for some reason */

        //...
        return outLab;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryObjectFactory<Labeling<L>, RandomAccessibleInterval<T>, Labeling<L>> bufferFactory() {
        return new BinaryObjectFactory<Labeling<L>, RandomAccessibleInterval<T>, Labeling<L>>() {

            @Override
            public Labeling<L> instantiate(final Labeling<L> lab, final RandomAccessibleInterval<T> in) {
                return lab.<L> factory().create(lab);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryOutputOperation<Labeling<L>, RandomAccessibleInterval<T>, Labeling<L>> copy() {
        return new WaehlbySplitterOp<L, T>(WaehlbySplitterOp.SEG_TYPE.SHAPE_BASED_SEGMENTATION);
    }

}
