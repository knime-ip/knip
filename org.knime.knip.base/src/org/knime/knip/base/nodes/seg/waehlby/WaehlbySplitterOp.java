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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.labeling.Watershed;
import net.imglib2.combiner.read.CombinedRandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.converter.read.ConvertedRandomAccessible;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
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
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.AbstractRegionGrowing;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.CCA;
import net.imglib2.ops.operation.real.unary.RealUnaryOperation;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.knime.knip.core.ops.interval.MaximumFinder;
import org.knime.knip.core.util.ImgUtils;

//TODO: Make Integer more generic
public class WaehlbySplitterOp<T extends RealType<T>> implements
        BinaryOutputOperation<Labeling<Integer>, RandomAccessibleInterval<T>, Labeling<Integer>> {

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

    //TODO: Make Integer more generic
    /**
     * {@inheritDoc}
     */
    @Override
    public Labeling<Integer> compute(final Labeling<Integer> inLab, final RandomAccessibleInterval<T> img,
                                     final Labeling<Integer> outLab) {

        int maxima_size = 4;
        //        Labeling<LongType> outLabeling = new NativeImgLabeling<LongType, LongType>(new ArrayImgFactory<LongType>().create(getDimensions(img), new LongType()));
        Img<FloatType> tmp = new ArrayImgFactory<FloatType>().create(getDimensions(img), new FloatType());
        Img<FloatType> tmp2 = new ArrayImgFactory<FloatType>().create(getDimensions(img), new FloatType());

        if (m_segType == SEG_TYPE.SHAPE_BASED_SEGMENTATION) {

            /* Start with distance transform */
            new DistanceMap<T, RandomAccessibleInterval<T>, RandomAccessibleInterval<FloatType>>().compute(img, tmp2);
            /* Gaussian smoothing */
            try {
                Gauss3.gauss(m_gaussSize, tmp2, tmp);
            } catch (IncompatibleTypeException e) {
                // TODO Auto-generated catch block
            }
        } else {
            /* Gaussian smoothing */
            try {
                Gauss3.gauss(m_gaussSize, img, tmp);
            } catch (IncompatibleTypeException e) {
                // TODO Auto-generated catch block
            }
        }

        /* Disc dilation */
        new DilateGray<FloatType, Img<FloatType>>(createDiscStructuringElement(maxima_size)).compute(tmp, tmp2);

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

                }, new ConvertedRandomAccessible<LabelingType<Integer>, BitType>(inLab, new LabelingToMaskConverter(), new BitType()));


        /* Label the found Minima */
        ConvertedRandomAccessible<FloatType, FloatType> inverter =
                new ConvertedRandomAccessible<FloatType, FloatType>(combiner,
                        new UnaryOperationBasedConverter<FloatType, FloatType>(
                                new SignedRealInvert<FloatType, FloatType>()), new FloatType());

        Img<BitType> tmp3 = new ArrayImgFactory<BitType>().create(img, new BitType());
        new MaximumFinder<FloatType>(40, 0).compute(Views.interval(inverter, tmp2), tmp3);

        long[][] structuringElement = AbstractRegionGrowing.get4ConStructuringElement(img.numDimensions()); /* TODO: Cecog uses 8con */

        final CCA<BitType, Img<BitType>, Labeling<Integer>> cca =
                new CCA<BitType, Img<BitType>, Labeling<Integer>>(structuringElement, new BitType(false));

        Labeling<Integer> seeds = ImgUtils.createEmptyCopy(inLab);
        new NativeImgLabeling<Integer, ShortType>(new ArrayImgFactory<ShortType>().create(getDimensions(img),
                                                                                          new ShortType()));
        cca.compute(tmp3, seeds);

        /* Seeded Watershed */
        Watershed<FloatType, Integer> watershed = new Watershed<FloatType, Integer>();
        watershed.setSeeds(seeds);
        watershed.setOutputLabeling(outLab);
        watershed.setIntensityImage(tmp);
        watershed.process();

        // Some "transformImageIf"

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
     * @param maxima_size
     * @return
     */
    private long[][] createDiscStructuringElement(final int radius) {
        int s = radius << 2;
        BufferedImage strel = new BufferedImage(s, s, BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D g = strel.createGraphics();
        g.setColor(Color.BLACK);
        g.fillOval(0, 0, s, s);

        //convert image to array

        byte[] pixels = ((DataBufferByte) strel.getRaster().getDataBuffer()).getData();
        long[][] element = new long[s][s];

        if (pixels.length != s*s) {
            //that's not correct...
            return null;
        }

        int x = 0;
        int y = 0;

        for( int i = 0; i < pixels.length; i++, x++) {
            if(x >= s) {
                x = 0;
                y++;
            }

            element[x][y] = pixels[i];
        }

        return element;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryObjectFactory<Labeling<Integer>, RandomAccessibleInterval<T>, Labeling<Integer>> bufferFactory() {
        return new BinaryObjectFactory<Labeling<Integer>, RandomAccessibleInterval<T>, Labeling<Integer>>() {

            @Override
            public Labeling<Integer> instantiate(final Labeling<Integer> lab, final RandomAccessibleInterval<T> in) {
                return lab.<Integer> factory().create(lab);
            }
        };
    }

    private CombinedRandomAccessible<FloatType, BitType, FloatType>
            createMaskedIfThenElseCombiner(final RandomAccessible<FloatType> a, final RandomAccessible<FloatType> b,
                                           final IfThenElse<FloatType, FloatType, FloatType> cond,
                                           final RandomAccessible<BitType> mask) {
        return new CombinedRandomAccessible<FloatType, BitType, FloatType>(
                new CombinedRandomAccessible<FloatType, FloatType, FloatType>(a, b,
                        new BinaryOperationBasedCombiner<FloatType, FloatType, FloatType>(
                                new ConditionalCombineOp(cond)), new FloatType()),
                mask,
                new BinaryOperationBasedCombiner<FloatType, BitType, FloatType>(new MaskOp<FloatType>(new FloatType())),
                new FloatType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryOutputOperation<Labeling<Integer>, RandomAccessibleInterval<T>, Labeling<Integer>> copy() {
        return new WaehlbySplitterOp<T>(WaehlbySplitterOp.SEG_TYPE.SHAPE_BASED_SEGMENTATION);
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

    private class ConditionalCombineOp<X, Y, Z> implements BinaryOperation<X, Y, Z> {

        private IfThenElse<X, Y, Z> m_condition;

        public ConditionalCombineOp(final IfThenElse<X, Y, Z> condition) {
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
            return new ConditionalCombineOp<X, Y, Z>(m_condition);
        }

    }

    private class MaskOp<X> implements BinaryOperation<X, BitType, X> {

        private X m_bg;

        public MaskOp(final X bg) {
            m_bg = bg;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public X compute(final X inputA, final BitType inputB, X output) {
            if (inputB.get()) {
                output = inputA;
            } else {
                output = m_bg;
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
    private class LabelingToMaskConverter implements Converter <LabelingType<Integer>, BitType> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void convert(final LabelingType<Integer> input, BitType output) {
            if (input.getLabeling().isEmpty()) {
                output = new BitType(false);
            } else {
                output = new BitType(true);
            }

        }

    }


}
