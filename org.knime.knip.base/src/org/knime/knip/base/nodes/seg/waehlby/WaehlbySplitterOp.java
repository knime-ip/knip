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

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.combiner.read.CombinedRandomAccessible;
import net.imglib2.converter.read.ConvertedRandomAccessible;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.ops.operation.BinaryObjectFactory;
import net.imglib2.ops.operation.BinaryOutputOperation;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.DistanceMap;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.DilateGray;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.AbstractRegionGrowing;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.CCA;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.knime.knip.base.nodes.io.kernel.structuring.SphereSetting;
import org.knime.knip.base.nodes.proc.maxfinder.MaximumFinderOp;
import org.knime.knip.base.nodes.seg.waehlby.WaelbyUtils.IfThenElse;
import org.knime.knip.base.nodes.seg.waehlby.WaelbyUtils.LabelingToBitConverter;
import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.ops.labeling.WatershedWithThreshold;
import org.knime.knip.core.util.MiscViews;

//TODO: Make Integer more generic, TODO: Why? It's the output
/**
 *
 * @author Jonathan Hale (University of Konstanz)
 */
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

    protected SEG_TYPE m_segType;

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

        //Labeling converted to BitType
        RandomAccessibleInterval<BitType> imgBin =
                Views.interval(new ConvertedRandomAccessible<LabelingType<L>, BitType>(inLab,
                        new LabelingToBitConverter<LabelingType<L>>(), new BitType()), inLab);

        // TODO: REMEMBER... this is cool: inLab.getIterableRegionOfInterest(inLab.getLabels());

        if (m_segType == SEG_TYPE.SHAPE_BASED_SEGMENTATION) {

            /* Start with distance transform */
            new DistanceMap<BitType>().compute(imgBin, tmp2); //TODO: Formerly <T> ?
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

//        Labeling<L> l = null;
//        for(L label : l.getLabels()){
//            IterableRegionOfInterest iterableRegionOfInterest = l.getIterableRegionOfInterest(label);
//            iterableRegionOfInterest.getIterableIntervalOverROI(null).cursor();
//            iterableRegionOfInterest.getIterableIntervalOverROI(null).cursor();
//
//        }


        /* Disc dilation */
        new DilateGray<FloatType>(
                new SphereSetting(img.numDimensions(), maxima_size).get()[0],
                new OutOfBoundsBorderFactory()
                )
                .compute(
                        new ImgView<FloatType>(Views.interval(Views.extendBorder(tmp), tmp), new ArrayImgFactory<FloatType>()),
                        tmp2);

        debugImage(tmp2, "After Dilate");


        /* Combine Images */
        //(S) Combine, if src1 < src2, else set as background
        CombinedRandomAccessible<FloatType, FloatType, FloatType> combined =
                WaelbyUtils.combineConditioned(tmp, tmp2, new IfThenElse<FloatType, FloatType, FloatType>() {
                    @Override
                    public FloatType test(final FloatType a, final FloatType b, final FloatType out) {
                        if (a.compareTo(b) < 0) {
                            out.set((float)out.getMinValue()); //background
                        } else {
                            out.set(b);
                        }

                        return out;
                    }

                }, new FloatType());


        /* Label the found Minima */
        ConvertedRandomAccessible<FloatType, FloatType> inverter = WaelbyUtils.invertImg(combined, new FloatType());


        Img<BitType> tmp3 = new ArrayImgFactory<BitType>().create(img, new BitType());

        new MaximumFinderOp<FloatType>(40, 0).compute(Views.interval(inverter, tmp2), tmp3);

        long[][] structuringElement = AbstractRegionGrowing.get4ConStructuringElement(img.numDimensions()); /* TODO: Cecog uses 8con */

        final CCA<BitType> cca = new CCA<BitType>(structuringElement, new BitType(false));

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

//        transformImageIf(srcImageRange(labels),
//                         maskImage(img_bin),
//                         destImage(img_bin),
//                         ifThenElse(
//                                 Arg1() == Param(background),
//                                 Param(background),
//                                 Param(foreground))
//                         );
//        CombinedRandomAccessible<BitType, BitType, BitType> maskBgFg =
//                WaelbyUtils.makeFgBgMask(outLab, new FloatType()); //TODO: paramters may be completely incorrect.

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
     * @param tmp2
     * @param string
     */
    private void debugImage(final RandomAccessibleInterval<FloatType> tmp2, final String string) {
        AWTImageTools.showInFrame(MiscViews.imgView(tmp2, null), "After Dilate");
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

    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryOutputOperation<Labeling<L>, RandomAccessibleInterval<T>, Labeling<Integer>> copy() {
        return new WaehlbySplitterOp<L, T>(WaehlbySplitterOp.SEG_TYPE.SHAPE_BASED_SEGMENTATION);
    }

}
