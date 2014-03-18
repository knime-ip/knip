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
 * Created on Dec 13, 2013 by squareys
 */
package org.knime.knip.base.nodes.seg.waehlby;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.combiner.Combiner;
import net.imglib2.combiner.read.CombinedRandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.converter.read.ConvertedRandomAccessible;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.ops.img.BinaryOperationAssignment;
import net.imglib2.ops.img.UnaryOperationBasedConverter;
import net.imglib2.ops.operation.BinaryOperation;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.real.unary.RealUnaryOperation;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 *
 * @author squareys
 */
public class WaehlbyUtils {
    /**
     * Function to map a BinaryOperation onto an Image (?)
     *
     * @param op
     * @return
     */
    public static <X, Y, Z> BinaryOperation<IterableInterval<X>, IterableInterval<Y>, IterableInterval<Z>>
            map(final BinaryOperation<X, Y, Z> op) {
        return new BinaryOperationAssignment<X, Y, Z>(op);
    }

    /**
     * @param img the img to invert
     * @param type Type of values of img
     * @return ConvertedRandomAccessible with SignedRealInvert converter
     */
    public static <T extends RealType<T>> ConvertedRandomAccessible<T, T> invertImg(final RandomAccessible<T> img,
                                                                                    final T type) {
        return new ConvertedRandomAccessible<T, T>(img, new UnaryOperationBasedConverter<T, T>(
                new SignedRealInvert<T, T>()), type);
    }

    public static <T extends RealType<T>> IntervalView<T> invertImg(final RandomAccessibleInterval<T> rai,
                                                                                    final T type) {
        return Views.interval(new ConvertedRandomAccessible<T, T>(rai, new UnaryOperationBasedConverter<T, T>(
                new SignedRealInvert<T, T>()), type), rai);
    }

    /**
     * @param img
     * @return
     */
    public static ConvertedRandomAccessible<BitType, BitType> invertBitImg(final RandomAccessible<BitType> img) {
        return new ConvertedRandomAccessible<BitType, BitType>(img, new UnaryOperationBasedConverter<BitType, BitType>(
                new BitInvert()), new BitType());
    }

    /**
     * Inverter for signed RealTypes
     *
     * @author Christian Dietz (University of Konstanz)
     * @param <I>
     * @param <O>
     */
    public static class SignedRealInvert<I extends RealType<I>, O extends RealType<O>> implements
            RealUnaryOperation<I, O> {

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

    /**
     *
     * @author squareys
     */
    public static class BitInvert implements UnaryOperation<BitType, BitType> {

        @Override
        public BitType compute(final BitType b, final BitType output) {
            output.set(!b.get());
            return output;
        }

        @Override
        public BitInvert copy() {
            return new BitInvert();
        }

    }

    /**
     * Condition of the IfTheElseCombiner
     *
     * @author Jonathan Hale (University of Konstanz)
     */
    public static abstract class IfThenElse<X extends Type<X>, Y extends Type<Y>, Z extends Type<Z>> {
        /**
         * Performs a test on a and b.
         *
         * @param a
         * @param b
         * @param out
         * @return
         */
        public abstract Z test(X a, Y b, Z out);
    }

    /**
     * Conditional Binary Operation
     *
     * Takes two Types as input and tests them with an {@link IfThenElse}.
     *
     * @author Jonathan Hale (University of Konstanz)
     * @param <X> {@link Type} of first parameter
     * @param <Y> {@link Type} of second parameter
     * @param <Z> {@link Type} of result
     */
    public static class ConditionalCombiner<X extends Type<X>, Y extends Type<Y>, Z extends Type<Z>> implements
            Combiner<X, Y, Z> {

        private IfThenElse<X, Y, Z> m_condition;

        /**
         * @param condition
         */
        public ConditionalCombiner(final IfThenElse<X, Y, Z> condition) {
            m_condition = condition;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void combine(final X inputA, final Y inputB, final Z output) {
            m_condition.test(inputA, inputB, output);
        }
    }

    public static class MaskCombiner<Z extends RealType<Z>> implements Combiner<Z, BitType, Z> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void combine(final Z inputA, final BitType inputB, final Z output) {
                output.setReal(inputB.get() ? inputA.getRealDouble(): inputA.getMinValue());
        }
    }

    /**
     * Mask Operation
     *
     * @author Jonathan Hale (University of Konstanz)
     */
    public static class MaskOp<X extends Type<X>> implements BinaryOperation<X, BitType, X> {

        private X m_bg;

        /**
         * Constructor
         *
         * @param bg Background value
         */
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

    /**
     * Converts a Labeling to a Binary Image.
     *
     * @author Jonathan Hale (University of Konstanz)
     */
    public static class LabelingToBitConverter<T extends LabelingType<?>> implements Converter<T, BitType> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void convert(final T input, final BitType output) {
            output.set(!input.getLabeling().isEmpty());
        }

    }

    /**
     * Converts a Labeling to a Binary Image.
     *
     * @author Jonathan Hale (University of Konstanz)
     */
    public static class WatershedToBitConverter<T extends LabelingType<?>> implements Converter<T, BitType> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void convert(final T input, final BitType output) {
            output.set(input.getLabeling().get(0).equals("Watershed"));
        }

    }

    /**
     * @param labeling
     * @return
     */
    public static <L extends LabelingType<?>> ConvertedRandomAccessible<L, BitType>
            convertWatershedsToBit(final RandomAccessible<L> labeling) {
        return new ConvertedRandomAccessible<L, BitType>(labeling, new WatershedToBitConverter<L>(), new BitType());
    }

    /**
     * @param labeling
     * @return
     */
    public static <L extends LabelingType<?>> ConvertedRandomAccessible<L, BitType>
            convertLabelingToBit(final RandomAccessible<L> labeling) {
        return new ConvertedRandomAccessible<L, BitType>(labeling, new LabelingToBitConverter<L>(), new BitType());
    }

    /**
     * Combine two images based on a condition
     *
     * @param a
     * @param b
     * @param condition
     * @param type
     * @return a {@link CombinedRandomAccessible} of the two input RandomAccessibles combined with a ConditionalCombiner
     */
    public static <X extends Type<X>, Y extends RealType<Y>, Z extends RealType<Z>>
            CombinedRandomAccessible<Z, BitType, Z> combineConditionedMasked(final RandomAccessible<X> a,
                                                                             final RandomAccessible<Y> b,
                                                                             final IfThenElse<X, Y, Z> condition,
                                                                             final RandomAccessible<BitType> mask,
                                                                             final Z type) {
        return new CombinedRandomAccessible<Z, BitType, Z>(new CombinedRandomAccessible<X, Y, Z>(a, b,
                new ConditionalCombiner<X, Y, Z>(condition), type), mask, new MaskCombiner<Z>(), type);
    }

    public static <X extends Type<X>, Y extends RealType<Y>, Z extends RealType<Z>> CombinedRandomAccessible<X, Y, Z>
            combineConditioned(final RandomAccessible<X> a, final RandomAccessible<Y> b,
                               final IfThenElse<X, Y, Z> condition, final Z type) {
        return new CombinedRandomAccessible<X, Y, Z>(a, b, new ConditionalCombiner<X, Y, Z>(condition), type);
    }

    /**
     * @param img
     * @param mask
     * @param type
     * @return
     */
    public static <T extends RealType<T>> CombinedRandomAccessible<T, BitType, BitType>
            makeFgBgMask(final RandomAccessible<T> img, final RandomAccessible<BitType> mask) {
        return combineConditioned(img, mask, new IfThenElse<T, BitType, BitType>() {
            @Override
            public BitType test(final T a, final BitType b, final BitType output) {
                if (!b.get() && a.getRealDouble() == a.getMinValue()) {
                    output.setZero();
                } else {
                    output.setOne();
                }

                return output;
            }
        }, new BitType());
    }

    /**
     * @param source
     * @param mask
     * @return
     */
    public static CombinedRandomAccessible<BitType, BitType, BitType>
            refineLabelingMask(final RandomAccessible<BitType> source, final RandomAccessible<BitType> mask) {
        return new CombinedRandomAccessible<BitType, BitType, BitType>(source, mask,
                new Combiner<BitType, BitType, BitType>() {
                    @Override
                    public void combine(final BitType inputA, final BitType inputB, final BitType output) {
                        output.set((!inputB.get()) ? inputA.get() : !inputB.get());
                    }
                }, new BitType());
    }

    /**
     * @param shedLabel
     * @param watershedResult
     * @param inLabMasked
     */
    public static void
            split(final String shedLabel, final Labeling<String> watershedResult, final RandomAccessibleInterval<BitType> inLabMasked) {
        Cursor<LabelingType<String>> cursor = watershedResult.cursor();
        RandomAccess<BitType> ra = inLabMasked.randomAccess();

        while (cursor.hasNext()) {
            LabelingType<String> type = cursor.next();

            ra.setPosition(cursor);

            if (type.getLabeling().contains(shedLabel) || !ra.get().get()) {
                type.setLabeling(type.getMapping().emptyList());
            }
        }

    }
}
