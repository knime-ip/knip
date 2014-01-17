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
import net.imglib2.RandomAccessible;
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
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

import org.knime.knip.base.nodes.io.kernel.structuring.SphereSetting;
import org.knime.knip.base.nodes.seg.waehlby.WaelbyUtils.IfThenElse;
import org.knime.knip.base.nodes.seg.waehlby.WaelbyUtils.LabelingToBitConverter;
import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.ops.img.IterableIntervalNormalize;
import org.knime.knip.core.ops.labeling.WatershedWithThreshold;

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
     * @param segType
     */
    public WaehlbySplitterOp(final SEG_TYPE segType) {
        super();
        m_segType = segType;

        m_gaussSize = 4;
    }

    private long[] getDimensions(final RandomAccessibleInterval<T> img) {
        long[] array = new long[img.numDimensions()];

        for (int i = 0; i < img.numDimensions(); i++) {
            array[i] = img.dimension(i);
        }

        return array;
    }

    private ArrayImgFactory<FloatType> m_floatFactory = new ArrayImgFactory<FloatType>();

    /**
     * {@inheritDoc}
     */
    @Override
    public Labeling<Integer> compute(final Labeling<L> inLab, final RandomAccessibleInterval<T> img,
                                     final Labeling<Integer> outLab) {

        int maxima_size = 10; // TODO: some random number still

        Img<FloatType> imgAlice = m_floatFactory.create(img, new FloatType());
        Img<FloatType> imgBob   = m_floatFactory.create(img, new FloatType());


        RandomAccessibleInterval<FloatType> imgAliceExt = Views.interval(Views.extendBorder(imgAlice), img);
        RandomAccessibleInterval<FloatType> imgBobExt   = Views.interval(Views.extendBorder(imgBob), img);

        //Labeling converted to BitType
        RandomAccessibleInterval<BitType> inLabMasked =
                Views.interval(new ConvertedRandomAccessible<LabelingType<L>, BitType>(inLab,
                        new LabelingToBitConverter<LabelingType<L>>(), new BitType()), inLab);

        // TODO: REMEMBER... this is cool: inLab.getIterableRegionOfInterest(inLab.getLabels());

        if (m_segType == SEG_TYPE.SHAPE_BASED_SEGMENTATION) {

            /* Start with distance transform */
            new DistanceMap<BitType>().compute(inLabMasked, imgBobExt); //TODO: Formerly <T> ?

            /* Gaussian smoothing */

            try {
                Gauss3.gauss(m_gaussSize, imgBobExt, imgAliceExt);
            } catch (IncompatibleTypeException e) {
                System.out.println("Incompatible Type Exception in Gauss.");
            }
        } else {
            /* Gaussian smoothing */
            try {
                Gauss3.gauss(m_gaussSize, Views.extendBorder(img), imgAliceExt);
            } catch (IncompatibleTypeException e) {
                // TODO Auto-generated catch block
            }
        }
        /* Disc dilation */
        new DilateGray<FloatType>(new SphereSetting(img.numDimensions(), maxima_size).get()[0],
                new OutOfBoundsBorderFactory()).compute(imgAliceExt, imgBobExt);

        //debugImage(imgBob, "After Dilate");

        /* Combine Images */
        //(S) Combine, if src1 < src2, else set as background
        CombinedRandomAccessible<FloatType, FloatType, FloatType> combined =
                WaelbyUtils.combineConditioned(imgAliceExt, imgBobExt, new IfThenElse<FloatType, FloatType, FloatType>() {
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
        ConvertedRandomAccessible<FloatType, FloatType> inverted = WaelbyUtils.invertImg(combined, new FloatType());

//        Img<BitType> imgChris = new ArrayImgFactory<BitType>().create(img, new BitType());
//        new MaximumFinderOp<T>(20, 0).compute(img, imgChris); //Why img? Cause it's faster...

        long[][] structuringElement = AbstractRegionGrowing.get4ConStructuringElement(img.numDimensions()); /* TODO: Cecog uses 8con */

        final CCA<FloatType> cca = new CCA<FloatType>(structuringElement, new FloatType());

        Labeling<Integer> seeds = inLab.<Integer> factory().create(inLab);
        new NativeImgLabeling<Integer, ShortType>(new ArrayImgFactory<ShortType>().create(getDimensions(img),
                                                                                          new ShortType()));
        cca.compute(Views.interval(combined, img), seeds);

        /* Seeded Watershed */
        WatershedWithThreshold<FloatType, Integer> watershed = new WatershedWithThreshold<FloatType, Integer>();
        watershed.setSeeds(seeds);
        watershed.setOutputLabeling(outLab);
        watershed.setIntensityImage(imgAliceExt);
        watershed.setThreshold(2);
        watershed.process();

        //        transformImageIf(srcImageRange(labels),
        //                         maskImage(img_bin),
        //                         destImage(img_bin),
        //                         ifThenElse(
        //                                 Arg1() == Param(background),
        //                                 Param(background),
        //                                 Param(foreground))
        //                         );

        CombinedRandomAccessible<BitType, BitType, BitType> maskBgFg =
                WaelbyUtils.refineLabelingMask(WaelbyUtils.convertLabelingToBit(outLab), inLabMasked);

        debugImage(WaelbyUtils.convertLabelingToBit(outLab), img, "After Watershed");
        debugImage(maskBgFg, img, "maskBgFg");


        //        Labeling<L> l = null;
        //        for(L label : l.getLabels()){
        //            IterableRegionOfInterest iterableRegionOfInterest = l.getIterableRegionOfInterest(label);
        //            iterableRegionOfInterest.getIterableIntervalOverROI(null).cursor();
        //            iterableRegionOfInterest.getIterableIntervalOverROI(null).cursor();
        //
        //        }

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
     * @param img
     * @param string
     */
    private void debugImage(final Img<FloatType> img, final String string) {
        FloatType min = new FloatType(0);
        FloatType max = new FloatType((float)min.getMaxValue());
        min.set((float)max.getMinValue());

        Img<FloatType> myImg = m_floatFactory.create(img, new FloatType());

        IterableIntervalNormalize<FloatType> norm =
                new IterableIntervalNormalize<FloatType>(0.0, new FloatType(), new ValuePair<FloatType, FloatType>(min,
                        max), true);
        norm.compute(img, myImg);
        AWTImageTools.showInFrame(myImg, string);
    }

    private void debugImage(final RandomAccessible<BitType> img, final Interval interval, final String string) {
        ArrayImgFactory<BitType> bitFactory = new ArrayImgFactory<BitType>();
        AWTImageTools.showInFrame(new ImgView<BitType>(Views.interval(img, interval), bitFactory), string);
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
