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

import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape.NeighborhoodsAccessible;
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
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

import org.knime.knip.base.nodes.io.kernel.structuring.SphereSetting;
import org.knime.knip.base.nodes.misc.contour.MooreContourExtractionOp;
import org.knime.knip.base.nodes.proc.maxfinder.MaximumFinderOp;
import org.knime.knip.base.nodes.seg.waehlby.WaehlbyUtils.LabelingToBitConverter;
import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.data.algebra.ExtendedPolygon;
import org.knime.knip.core.ops.img.IterableIntervalNormalize;
import org.knime.knip.core.ops.labeling.LabelingCleaner;
import org.knime.knip.core.ops.labeling.WatershedWithSheds;

/**
 *
 * @author Jonathan Hale (University of Konstanz)
 *
 * @param <L>
 * @param <T>
 */
public class WaehlbySplitterOp<L extends Comparable<L>, T extends RealType<T>> implements
        BinaryOutputOperation<Labeling<L>, RandomAccessibleInterval<T>, Labeling<String>> {

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

    private int m_gaussSize;

    private int m_maximaSize;

    private int m_rsize;

    private int m_minMergeSize;

    /**
     * Contructor for WaehlbySplitter operation.
     *
     * @param segType
     */
    public WaehlbySplitterOp(final SEG_TYPE segType) {
        super();
        m_rsize = 2;

        m_segType = segType;

        m_gaussSize = 1;
        m_maximaSize = 5;
        m_minMergeSize = 8;
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
    public Labeling<String> compute(final Labeling<L> inLab, final RandomAccessibleInterval<T> img,
                                     final Labeling<String> outLab) {

        Img<FloatType> imgAlice = m_floatFactory.create(img, new FloatType());
        Img<FloatType> imgBob = m_floatFactory.create(img, new FloatType());

        RandomAccessibleInterval<FloatType> imgAliceExt = Views.interval(Views.extendBorder(imgAlice), img);
        RandomAccessibleInterval<FloatType> imgBobExt = Views.interval(Views.extendBorder(imgBob), img);

        //Labeling converted to BitType
        RandomAccessibleInterval<BitType> inLabMasked =
                Views.interval(new ConvertedRandomAccessible<LabelingType<L>, BitType>(inLab,
                        new LabelingToBitConverter<LabelingType<L>>(), new BitType()), inLab);

        if (m_segType == SEG_TYPE.SHAPE_BASED_SEGMENTATION) {
            /*c distance transform */
//            debugImage(inLabMasked, inLabMasked, "Distancemap Input", new BitType());
            new DistanceMap<BitType>().compute(Views.interval(inLabMasked, img), imgBobExt);

            debugImage(imgBob, "After Distance Map");
            try {
                /* Gaussian smoothing */
                Gauss3.gauss(m_gaussSize, imgBobExt, imgAliceExt);
            } catch (IncompatibleTypeException e) {
            }
        } else {
            try {
                /* Gaussian smoothing */
                Gauss3.gauss(m_gaussSize, Views.extendBorder(img), imgAliceExt);
            } catch (IncompatibleTypeException e) {
            }
        }

        debugImage(imgAlice, "After Blur (and Distancemap)");

        /* Disc dilation */
        new DilateGray<FloatType>(new SphereSetting(img.numDimensions(), m_maximaSize).get()[0],
                new OutOfBoundsBorderFactory<FloatType, RandomAccessibleInterval<FloatType>>()).compute(imgAliceExt,
                                                                                                        imgBobExt);
//        debugImage(imgBob, "After Dilate");

        /* Combine Images */
        // if src1 < src2, set as background else set as src2
//        CombinedRandomAccessible<FloatType, BitType, FloatType> combined =
//                WaehlbyUtils.combineConditionedMasked(imgAliceExt, WaehlbyUtils.invertImg(imgBobExt, new FloatType()),
//                                                     new IfThenElse<FloatType, FloatType, FloatType>() {
//                                                         @Override
//                                                         public FloatType test(final FloatType a, final FloatType b,
//                                                                               final FloatType out) {
//                                                             if (a.compareTo(b) < 0) {
//                                                                 out.setReal(a.getMinValue()); //background
//                                                             } else {
//                                                                 out.setReal(b.getRealDouble());
//                                                             }
//
//                                                             return out;
//                                                         }
//
//                                                     }, inLabMasked, new FloatType());

//        debugImage(combined, img, "Combined", new FloatType());

        // label
        long[][] structuringElement = AbstractRegionGrowing.get8ConStructuringElement(img.numDimensions()); /* TODO: Cecog uses 8con */

//        final CCA<FloatType> cca = new CCA<FloatType>(structuringElement, new FloatType());
//        NativeImgLabeling<Integer, ShortType> seeds =
//                new NativeImgLabeling<Integer, ShortType>(new ArrayImgFactory<ShortType>().create(getDimensions(img),
//                                                                                                  new ShortType()));
//
//        cca.compute(Views.interval(WaehlbyUtils.invertImg(combined, new FloatType()), img), seeds);

        final CCA<BitType> cca = new CCA<BitType>(structuringElement, new BitType());
        NativeImgLabeling<Integer, ShortType> seeds = new NativeImgLabeling<Integer, ShortType>(new ArrayImgFactory<ShortType>().create(getDimensions(img),
                                                                                        new ShortType()));

        Img<BitType> imgChris = new ArrayImgFactory<BitType>().create(img, new BitType());
        new MaximumFinderOp<FloatType>(0, 0).compute(imgBob, imgChris);

        debugImage(imgChris, "Maximum Finder");
        cca.compute(imgChris, seeds);

//        AWTImageTools.showInFrame(seeds, "Seeds");
        Labeling<String> watershedResult =
                new NativeImgLabeling<String, ShortType>(new ArrayImgFactory<ShortType>().create(getDimensions(img),
                                                                                                 new ShortType()));
        /* Seeded Watershed */
        WatershedWithSheds<FloatType, Integer> watershed =
                new WatershedWithSheds<FloatType, Integer>(structuringElement);
        watershed.compute(WaehlbyUtils.invertImg(imgAlice, new FloatType()), seeds, watershedResult);
//                watershed.compute(Views.interval(WaelbyUtils.invertImg(imgAlice, new FloatType()), img), seeds, watershedResult);
//        debugImage(imgAlice, "Intensity Image");
//        WatershedWithThreshold<FloatType, Integer> watershed = new WatershedWithThreshold<FloatType, Integer>();
//        watershed.setThreshold(-.2);
//        watershed.setIntensityImage(WaelbyUtils.invertImg(imgBob, new FloatType()));
//        watershed.setSeeds(seeds);
//        watershed.setOutputLabeling(watershedResult);
        //        debugImage(imgAlice, "Alice");
        //watershed.compute(imgAlice, seeds, watershedResult);
//        watershed.process();

//        transformImageIf(srcImageRange(labels),
//                         maskImage(img_bin),
//                         destImage(img_bin),
//                         ifThenElse(
//                                 Arg1() == Param(background),
//                                 Param(background),
//                                 Param(foreground))
//                         );

//        AWTImageTools.showInFrame(watershedResult, "Watershed Result");

        WaehlbyUtils.split("Watershed", watershedResult, inLabMasked);

//        AWTImageTools.showInFrame(watershedResult, "Watershed Split");

        new LabelingCleaner().compute(watershedResult, watershedResult);

        MooreContourExtractionOp contourExtraction = new MooreContourExtractionOp(false);
        ArrayImgFactory<BitType> bitFactory = new ArrayImgFactory<BitType>();
        ArrayList<LabeledObject> objects = new ArrayList<LabeledObject>();

        RandomAccessible<BitType> src = WaehlbyUtils.convertWatershedsToBit(watershedResult);

        for (String label : watershedResult.getLabels()) {
            IterableRegionOfInterest iROI = watershedResult.getIterableRegionOfInterest(label);

            IterableInterval<BitType> intervalOverSrc = iROI.getIterableIntervalOverROI(src);

            Img<BitType> objImage = bitFactory.create(intervalOverSrc, new BitType());
            final long[] offset = new long[2];
            intervalOverSrc.min(offset);
            offset[0] *= -1;
            offset[1] *= -1;

            RandomAccess<BitType> ra = objImage.randomAccess();
            Cursor<BitType> curs = intervalOverSrc.cursor();

            curs.fwd();
            while (curs.hasNext()) {
                ra.setPosition(curs);
                ra.move(offset);
                ra.get().set(curs.next());
            }

            ExtendedPolygon poly = new ExtendedPolygon();
            contourExtraction.compute(objImage, poly);

            long[] min = new long[2], max = new long[2];
            watershedResult.getExtents(label, min, max);

            objects.add(new LabeledObject(poly, label, min, max));
        }

        /* Object Merge */
        new LabelingCleaner().compute(watershedResult, outLab);

        ArrayList<int[]> points = new ArrayList<int[]>();

        int squaredRSize = m_rsize; //this is a bug from the cecog code. Seems to work better, though ;P

        boolean found = false;
        for (int i = 0; i < objects.size(); ++i) {
            for (int j = i + 1; j < objects.size(); ++j) {
                ExtendedPolygon iPoly = objects.get(i).getContour();
                ExtendedPolygon jPoly = objects.get(j).getContour();

                long[] jCenter = jPoly.getCenter();
                long[] iCenter = iPoly.getCenter();

                double[] diff = new double[]{
                        (iCenter[0] + iPoly.getBounds2D().getX()) - (jCenter[0] + jPoly.getBounds2D().getX()),
                        (iCenter[1] + iPoly.getBounds2D().getY()) - (jCenter[1] + jPoly.getBounds2D().getY())
                };

                if ((diff[0] * diff[0] + diff[1] * diff[1]) < squaredRSize) {
                    for (int[] iPoint : iPoly) {
                        if (found) {
                            break;
                        }

                        for (int[] jPoint : jPoly) {
                            if (distanceSq(iPoint, jPoint) < 4) {
                                found = true;

                                points.add(iPoint);
                                points.add(jPoint);
                            }
                        }
                    }

                    if (found) {
                        RectangleShape shape = new RectangleShape(1, true); //"true" skips middle point

                        NeighborhoodsAccessible<LabelingType<String>> raNeighOut = shape.neighborhoods(Views.interval(Views.extendValue(outLab, new LabelingType<String>()), outLab));

                        RandomAccess<LabelingType<String>> raOut = outLab.randomAccess();

                        String ijLabel = objects.get(j).getLabel();

                        //overwrite i to have label of j
                        Cursor<LabelingType<String>> curs = watershedResult.getIterableRegionOfInterest(objects.get(i).getLabel()).getIterableIntervalOverROI(watershedResult).cursor();

                        while (curs.hasNext()) {
                            curs.fwd();

                            raOut.setPosition(curs);
                            raOut.get().setLabel(ijLabel);
                        }

                        //set label of both pointes
                        for(int[] point : points) {
                            raOut.setPosition(point);
                            raOut.get().setLabel(ijLabel);
                        }

                        //fill remaining gap for both points
                        for (int[] point : points) {
                            remainingGapFill(raNeighOut.randomAccess(), point, ijLabel);
                        }
                    }
                }
            }
        }

        new LabelingCleaner().compute(watershedResult, outLab);

        /* transform Images if ... */

        /* hole filling */

        /* Copy image for some reason */

        //...

        return outLab;
        /* end*/
    }

    /**
     * @param point
     * @param ijLabel
     */
    private final void remainingGapFill(final RandomAccess<Neighborhood<LabelingType<String>>> ra, final int[] point, final String label) {
        ra.setPosition(point);

        Cursor<LabelingType<String>> cNeigh = ra.get().cursor();

        LabelingType<String> p;
        while (cNeigh.hasNext())  {
            p = cNeigh.next();
            if (!p.getLabeling().contains(label)) {
                int numNonij = 0;
                ra.setPosition(cNeigh);
                Cursor<LabelingType<String>> cNeighInner = ra.get().cursor();

                while(cNeighInner.hasNext()){
                    if (cNeighInner.next().getLabeling().contains(label)) {
                        ++numNonij;
                    }
                }

                if (numNonij <= 2) {
                    p.setLabel(label);
                }
            }
        }
    }

    /**
     * @param iPoint
     * @param jPoint
     * @return
     */
    private int distanceSq(final int[] iPoint, final int[] jPoint) {
        final int a = jPoint[0] - iPoint[0];
        final int b = jPoint[1] - iPoint[1];
        return (a*a + b*b);
    }

    /**
     * @param iPoint
     * @param jPoint
     * @return
     */
    private double distance(final int[] iPoint, final int[] jPoint) {
        final int a = jPoint[0] - iPoint[0];
        final int b = jPoint[1] - iPoint[1];
        return Math.sqrt(a*a + b*b);
    }

    /**
     * @param img
     * @param string
     */
    private <T extends RealType<T>> void debugImage(final Img<T> img, final String string) {
        T min = img.firstElement().createVariable();
        T max = min.createVariable();

        max.setReal((float)min.getMaxValue());
        min.setReal((float)max.getMinValue());

        Img<T> myImg = null;
        try {
            myImg = m_floatFactory.imgFactory(max).create(img, max);
        } catch (IncompatibleTypeException e) {
            System.out.println("Debug image threw incompatible type exception...");
        }

        /* this code does not change the contents of img. tested! */
        IterableIntervalNormalize<T> norm =
                new IterableIntervalNormalize<T>(0.0, max, new ValuePair<T, T>(min, max), true);
        norm.compute(img, myImg);
        norm.compute(img, img);
        AWTImageTools.showInFrame(myImg, string);
    }

    private <T extends RealType<T>> void debugImage(final RandomAccessible<T> img, final Interval interval, final String string, final T type) {
        try {
            debugImage(new ImgView<T>(Views.interval(img, interval), m_floatFactory.imgFactory(type)), string);
        } catch (IncompatibleTypeException e) {
            // TODO Auto-generated catch block
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryObjectFactory<Labeling<L>, RandomAccessibleInterval<T>, Labeling<String>> bufferFactory() {
        return new BinaryObjectFactory<Labeling<L>, RandomAccessibleInterval<T>, Labeling<String>>() {

            @Override
            public Labeling<String> instantiate(final Labeling<L> lab, final RandomAccessibleInterval<T> in) {
                return lab.<String> factory().create(lab);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryOutputOperation<Labeling<L>, RandomAccessibleInterval<T>, Labeling<String>> copy() {
        return new WaehlbySplitterOp<L, T>(WaehlbySplitterOp.SEG_TYPE.SHAPE_BASED_SEGMENTATION);
    }

    private class LabeledObject {
        ExtendedPolygon m_contour;

        String m_label;

        long[] m_topleft;

        long[] m_botright;

        public LabeledObject(final ExtendedPolygon c, final String l, final long[] tl, final long[] br) {
            m_contour = c;
            m_label = l;
            m_topleft = tl;
            m_botright = br;
        }

        public ExtendedPolygon getContour() {
            return m_contour;
        }

        public long[] getTopLeft() {
            return m_topleft;
        }

        public long[] getBottomRight() {
            return m_botright;
        }

        public long[] getCenter() {
            return new long[]{(m_botright[0] + m_topleft[0]) >> 1, (m_botright[1] + m_topleft[1]) >> 1};
        }

        public String getLabel() {
            return m_label;
        }
    }
}
