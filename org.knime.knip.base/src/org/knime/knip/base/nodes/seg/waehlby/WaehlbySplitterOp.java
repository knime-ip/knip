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


import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.combiner.read.CombinedRandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.converter.read.ConvertedRandomAccessible;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.ops.img.BinaryOperationAssignment;
import net.imglib2.ops.img.BinaryOperationBasedCombiner;
import net.imglib2.ops.img.UnaryOperationBasedConverter;
import net.imglib2.ops.operation.BinaryObjectFactory;
import net.imglib2.ops.operation.BinaryOperation;
import net.imglib2.ops.operation.BinaryOutputOperation;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.DistanceMap;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.DilateGray;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.StructuringElementCursor;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.AbstractRegionGrowing;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.CCA;
import net.imglib2.ops.operation.real.unary.RealUnaryOperation;
import net.imglib2.roi.BinaryMaskRegionOfInterest;
import net.imglib2.roi.EllipseRegionOfInterest;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.knime.knip.base.nodes.io.kernel.structuring.SphereSetting;
import org.knime.knip.core.ops.labeling.WatershedWithThreshold;

//TODO: Make Integer more generic
public class WaehlbySplitterOp<L extends Comparable<L>, T extends RealType<T>> implements
        BinaryOutputOperation<Labeling<L>, RandomAccessibleInterval<T>, Labeling<Integer>> {

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

    private long[] getDimensions(final RandomAccessibleInterval<T> img) {
        long[] array = new long[img.numDimensions()];

        for (int i = 0; i < img.numDimensions(); i++) {
            array[i] = img.dimension(i);
        }

        return array;
    }


    private ArrayImgFactory<FloatType> m_floatFactory = new ArrayImgFactory<FloatType>();
    private RandomAccessibleInterval<FloatType> createBorderedImg(final Interval interval) {
        Img<FloatType> img = m_floatFactory.create(interval, new FloatType());
        return Views.interval(Views.extendBorder(img), interval);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Labeling<Integer> compute(final Labeling<L> inLab, final RandomAccessibleInterval<T> img,
                                     final Labeling<Integer> outLab) {

        int maxima_size = 4;
        RandomAccessibleInterval<FloatType> tmp = createBorderedImg(img);
        RandomAccessibleInterval<FloatType> tmp2 = createBorderedImg(img);

        RandomAccessibleInterval<BitType> imgBin = createLabelingMask(inLab);

        if (m_segType == SEG_TYPE.SHAPE_BASED_SEGMENTATION) {

            /* Start with distance transform */
            new DistanceMap< T >().compute(imgBin, tmp2);
            /* Gaussian smoothing */
            try {
                Gauss3.gauss(m_gaussSize, tmp2, tmp);
            } catch (IncompatibleTypeException e) {
                System.out.println("Incompatible Type Exception in Gauss.");
            }
        } else {
            /* Gaussian smoothing */
            try {
                Gauss3.gauss(m_gaussSize, Views.extendBorder(img), tmp);
            } catch (IncompatibleTypeException e) {
                // TODO Auto-generated catch block
            }
        }

        /* Disc dilation */
        new DilateGray<FloatType>(new SphereSetting(img.numDimensions(), maxima_size).get()[0])
                .compute(new ImgView<FloatType>(Views.interval(Views.extendBorder(tmp), tmp), new ArrayImgFactory<FloatType>()), tmp2);

        RandomAccessible<BitType> mask =
                new ConvertedRandomAccessible<LabelingType<L>, BitType>(inLab,
                        new LabelingToMaskConverter<LabelingType<L>>(), new BitType());

        /* Combine Images */
        //(S) Combine, if src1 < src2, else set as background
        CombinedRandomAccessible<FloatType, BitType, FloatType> combiner =
                createMaskedIfThenElseCombiner(tmp, tmp2, new IfThenElse<FloatType, FloatType, FloatType>() {

                    @Override
                    public FloatType test(final FloatType a, final FloatType b) {
                        if (a.compareTo(b) < 0) {
                            return new FloatType(); //background
                        }

                        return b;
                    }

                }, mask);

        /* Label the found Minima */
        ConvertedRandomAccessible<FloatType, FloatType> inverter =
                new ConvertedRandomAccessible<FloatType, FloatType>(combiner,
                        new UnaryOperationBasedConverter<FloatType, FloatType>(
                                new SignedRealInvert<FloatType, FloatType>()), new FloatType());

        Img<BitType> tmp3 = new ArrayImgFactory<BitType>().create(img, new BitType());
        new MaximumFinder<FloatType>(40, 0).compute(Views.interval(inverter, tmp2), tmp3);

        long[][] structuringElement = AbstractRegionGrowing.get4ConStructuringElement(img.numDimensions()); /* TODO: Cecog uses 8con */

        final CCA<BitType> cca =
                new CCA<BitType>(structuringElement, new BitType(false));

        Labeling<Integer> seeds = inLab.<Integer> factory().create(inLab);
        new NativeImgLabeling<Integer, ShortType>(new ArrayImgFactory<ShortType>().create(getDimensions(img),
                                                                                          new ShortType()));
        cca.compute(tmp3, seeds);

        /* Seeded Watershed */
        WatershedWithThreshold<FloatType, Integer> watershed = new WatershedWithThreshold<FloatType, Integer>();
        watershed.setSeeds(seeds);
        watershed.setOutputLabeling(outLab);
        watershed.setIntensityImage(tmp);
        watershed.process();

        CombinedRandomAccessible<BitType, BitType, BitType> maskBgFg =
                new CombinedRandomAccessible<BitType, BitType, BitType>(
                        new ConvertedRandomAccessible<LabelingType<Integer>, BitType>(outLab,
                                new LabelingToMaskConverter<LabelingType<Integer>>(), new BitType()), mask,
                        new BinaryOperationBasedCombiner<BitType, BitType, BitType>(
                                new ConditionalBinaryOp<BitType, BitType, BitType>(
                                        new IfThenElse<BitType, BitType, BitType>() {
                                            @Override
                                            public BitType test(final BitType a, final BitType b) {
                                                boolean ret = true;

                                                ret = a.get() && b.get();

                                                return new BitType(ret);
                                            }
                                        })), new BitType());

        /* Object Merge */

        /* weird complex algorithm with CrackContourCirculation */

        /* Merge objects (Big part, since own algorithm) */

        /* transform Images if ... */

        /* hole filling */

        /* Copy image for some reason */

        //...
        return outLab;
    }

    /**
     * @return
     */
    private RandomAccessibleInterval<BitType> createLabelingMask(final Labeling<L> inLab) {
        return new BinaryMaskRegionOfInterest(new ConvertedRandomAccessible(inLab, new LabelingToMaskConverter()));
    }

    /**
     * @param maxima_size
     * @return
     */
    private Img<BitType> createDiscStructuringElement(final int radius) {
        int s = radius * 2;

        Img<BitType> strel = new ArrayImgFactory<BitType>().create(new long[]{s, s}, new BitType());

        EllipseRegionOfInterest roi = new EllipseRegionOfInterest(2);
        roi.setRadius(radius);

        Cursor<BitType> c = roi.getIterableIntervalOverROI(strel).cursor();

        while (c.hasNext()) {
            c.next().set(true);
        }

        return StructuringElementCursor.createElementFromImg(strel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryObjectFactory<Labeling<L>, RandomAccessibleInterval<T>, Labeling<Integer>> bufferFactory() {
        return new BinaryObjectFactory<Labeling<L>, RandomAccessibleInterval<T>, Labeling<Integer>>() {

            @Override
            public Labeling<Integer> instantiate(final Labeling<L> lab, final RandomAccessibleInterval<T> in) {
                return lab.<Integer> factory().create(lab);
            }
        };
    }

    private CombinedRandomAccessible<FloatType, BitType, FloatType>
            createMaskedIfThenElseCombiner(final RandomAccessible<FloatType> a, final RandomAccessible<FloatType> b,
                                           final IfThenElse<FloatType, FloatType, FloatType> cond,
                                           final RandomAccessible<BitType> mask) {
        return new CombinedRandomAccessible<FloatType, BitType, FloatType>(
                new CombinedRandomAccessible<FloatType, FloatType, FloatType>(
                        a,
                        b,
                        new BinaryOperationBasedCombiner<FloatType, FloatType, FloatType>(new ConditionalBinaryOp(cond)),
                        new FloatType()), mask, new BinaryOperationBasedCombiner<FloatType, BitType, FloatType>(
                        new MaskOp<FloatType>(new FloatType())), new FloatType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryOutputOperation<Labeling<L>, RandomAccessibleInterval<T>, Labeling<Integer>> copy() {
        return new WaehlbySplitterOp<L, T>(WaehlbySplitterOp.SEG_TYPE.SHAPE_BASED_SEGMENTATION);
    }

    /*
     * Mapper
     */
    private static <X, Y, Z> BinaryOperation<IterableInterval<X>, IterableInterval<Y>, IterableInterval<Z>>
            map(final BinaryOperation<X, Y, Z> op) {
        return new BinaryOperationAssignment<X, Y, Z>(op);
    }

    private class SignedRealInvert<I extends RealType<I>, O extends RealType<O>> implements RealUnaryOperation<I, O> {

        @Override
        public O compute(final I x, final O output) {
            final double value = x.getRealDouble() * -1.0;
            output.setReal(value);
            return output;
        }

        @Override
        public SignedRealInvert<I, O> copy() {
            return new SignedRealInvert<I, O>();
        }

    }

    private abstract class IfThenElse<X, Y, Z> {
        public abstract Z test(X a, Y b);
    }

    private class ConditionalBinaryOp<X, Y, Z> implements BinaryOperation<X, Y, Z> {

        private IfThenElse<X, Y, Z> m_condition;

        public ConditionalBinaryOp(final IfThenElse<X, Y, Z> condition) {
            m_condition = condition;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Z compute(final X inputA, final Y inputB, Z output) {
            output = m_condition.test(inputA, inputB);

            return output;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BinaryOperation<X, Y, Z> copy() {
            return new ConditionalBinaryOp<X, Y, Z>(m_condition);
        }
    }

    private class MaskOp<X extends Type<X>> implements BinaryOperation<X, BitType, X> {

        private X m_bg;

        public MaskOp(final X bg) {
            m_bg = bg;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public X compute(final X inputA, final BitType inputB, final X output) {
            if (inputB.get()) {
                output.set(inputA);
            } else {
                output.set(m_bg);
            }

            return output;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BinaryOperation<X, BitType, X> copy() {
            return new MaskOp<X>(m_bg);
        }

    }

    // TODO: Make Integer more generic
    private class LabelingToMaskConverter<T extends LabelingType<?>> implements Converter<T, BitType> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void convert(final T input, final BitType output) {
            output.set(!input.getLabeling().isEmpty());
        }

    }

}
