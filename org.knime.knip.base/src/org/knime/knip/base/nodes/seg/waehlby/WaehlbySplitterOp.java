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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.knime.knip.base.nodes.io.kernel.structuring.SphereSetting;
import org.knime.knip.base.nodes.proc.maxfinder.MaximumFinderOp;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.data.algebra.ExtendedPolygon;
import org.knime.knip.core.ops.img.IterableIntervalNormalize;
import org.knime.knip.core.ops.labeling.WatershedWithSheds;
import org.knime.knip.core.util.Triple;

import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.RectangleShape.NeighborhoodsAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.converter.read.ConvertedRandomAccessible;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.ops.img.UnaryOperationBasedConverter;
import net.imglib2.ops.operation.BinaryObjectFactory;
import net.imglib2.ops.operation.BinaryOutputOperation;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.DistanceMap;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.DilateGray;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.AbstractRegionGrowing;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.CCA;
import net.imglib2.ops.operation.real.unary.RealUnaryOperation;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * WaehlbySplitterOp
 *
 * Performs split and merge operation similar to Wählby's method presented in "Combining intensity, edfge and shape
 * information for 2D and 3D segmentation of cell nuclei in tissue selections", 2004 and also very similar to the method
 * of cellcognizer (cellcognition.org)
 *
 * @author Jonathan Hale (University of Konstanz)
 *
 * @param <L> Type of the input {@link Labeling}<L>
 * @param <T> Type of the input {@link RandomAccessibleInterval}<T>
 */
public class WaehlbySplitterOp<L extends Comparable<L>, T extends RealType<T>> implements
        BinaryOutputOperation<RandomAccessibleInterval<LabelingType<L>>, RandomAccessibleInterval<T>, RandomAccessibleInterval<LabelingType<String>>> {

    /**
     * Segmentation type enum
     *
     * @author Jonathan Hale (University of Konstanz)
     */
    public enum SEG_TYPE {
        /**
         * Shape based segmentation
         */
        SHAPE_BASED_SEGMENTATION, //
        /**
         * Segmentation without distance transformation
         */
        OTHER_SEGMENTATION
    }

    /* type of segmentation to be performed */
    private SEG_TYPE m_segType;

    /* size of the gaussian blur */
    private final int m_gaussSize;

    /* minimal Size of Objects */
    private final int m_minMergeSize;

    /* minimal allowed object distance */
    private final int m_seedDistanceThreshold;

    /* connected component ananlysis op */
    private final CCA<BitType> m_cca;

    /* watershed op */
    private WatershedWithSheds<FloatType, Integer> m_watershed;

    /* 3x3 rectangle shape cursor which skips the center point */
    private final static RectangleShape RECTANGLE_SHAPE = new RectangleShape(1, true); //"true" skips center point

    /**
     * Constructor for WaehlbySplitter operation.
     *
     * @param segType {@link SEG_TYPE} Type of segmentation to be used
     * @param seedDistanceThreshold minimal distance between seeds for watershed
     * @param mergeSizeThreshold maximal size at which segmented objects will start being merged.
     * @param gaussSize sigma for the gaussian blur
     */
    public WaehlbySplitterOp(final SEG_TYPE segType, final int seedDistanceThreshold, final int mergeSizeThreshold,
                             final int gaussSize) {
        this(segType, seedDistanceThreshold, mergeSizeThreshold, gaussSize,
                new CCA<BitType>(AbstractRegionGrowing.get8ConStructuringElement(2), new BitType()),
                new WatershedWithSheds<FloatType, Integer>(AbstractRegionGrowing.get8ConStructuringElement(2)));
    }

    /**
     * Constructor for WaehlbySplitter operation.
     *
     * @param segType {@link SEG_TYPE} Type of segmentation to be used
     * @param seedDistanceThreshold minimal distance between seeds for watershed
     * @param mergeSizeThreshold maximal size at which segmented objects will start being merged.
     * @param gaussSize sigma for the gaussian blur
     */
    protected WaehlbySplitterOp(final SEG_TYPE segType, final int seedDistanceThreshold, final int mergeSizeThreshold,
                                final int gaussSize, final CCA<BitType> cca,
                                final WatershedWithSheds<FloatType, Integer> watershed) {
        super();
        m_segType = segType;
        m_cca = cca;
        m_watershed = watershed;
        m_seedDistanceThreshold = seedDistanceThreshold;
        m_gaussSize = gaussSize;
        m_minMergeSize = mergeSizeThreshold;
    }

    /* image factory used in this op TODO: Should use the img factory of the input img */
    private final ArrayImgFactory<FloatType> m_floatFactory = new ArrayImgFactory<FloatType>();

    @Override
    public RandomAccessibleInterval<LabelingType<String>>
           compute(final RandomAccessibleInterval<LabelingType<L>> inLab, final RandomAccessibleInterval<T> img,
                   final RandomAccessibleInterval<LabelingType<String>> outLab) {

        /* interval of the image extended by 1 in every direction */
        final FinalInterval extendedSize = extendBorderByOne(img);
        /* interval of size of img offset by 1 in dimensions 0 and 1. */
        final FinalInterval cutOffBorder = cutOffBorder(img);

        final Img<FloatType> imgAlice = m_floatFactory.create(extendedSize, new FloatType());
        final Img<FloatType> imgBob = m_floatFactory.create(extendedSize, new FloatType());

        final RandomAccessibleInterval<FloatType> imgAliceExt =
                Views.interval(Views.extendBorder(imgAlice), extendedSize);
        final RandomAccessibleInterval<FloatType> imgBobExt = Views.interval(Views.extendBorder(imgBob), extendedSize);

        // labeling converted to BitType (background where empty label, foreground where any label)
        final RandomAccessibleInterval<BitType> inLabMasked =
                Views.interval(new ConvertedRandomAccessible<LabelingType<L>, BitType>(
                        Views.translate(Views.extendValue(inLab, getEmptyLabel(inLab)), 1, 1),
                        new LabelingToBitConverter<LabelingType<L>>(), new BitType()), extendedSize);

        final double[] sigmas = new double[inLab.numDimensions()];
        Arrays.fill(sigmas, m_gaussSize);

        if (m_segType == SEG_TYPE.SHAPE_BASED_SEGMENTATION) {
            new DistanceMap<BitType>().compute(inLabMasked, imgBobExt);
            try {
                Gauss3.gauss(sigmas, imgBobExt, imgAliceExt, 1);
            } catch (final IncompatibleTypeException e) {
            }
        } else {
            try {
                Gauss3.gauss(sigmas, Views.extendBorder(img), imgAliceExt, 1);
            } catch (final IncompatibleTypeException e) {
            }
        }

        // disc dilation TODO: one could expose the '3' as setting
        new DilateGray<FloatType>(new SphereSetting(2, 3).get()[0],
                new OutOfBoundsBorderFactory<FloatType, RandomAccessibleInterval<FloatType>>()).compute(imgAliceExt,
                                                                                                        imgBob);
        final ImgLabeling<Integer, ShortType> seeds = new ImgLabeling<Integer, ShortType>(
                new ArrayImgFactory<ShortType>().create(extendedSize, new ShortType()));

        final Img<BitType> imgChris = new ArrayImgFactory<BitType>().create(extendedSize, new BitType());
        new MaximumFinderOp<FloatType>(0, 0).compute(imgBob, imgChris);

        m_cca.compute(imgChris, seeds);

        RandomAccessibleInterval<LabelingType<String>> watershedResult = new ImgLabeling<String, ShortType>(
                new ArrayImgFactory<ShortType>().create(extendedSize, new ShortType()));

        /* seeded watershed */
        m_watershed.compute(invertImg(imgAlice, new FloatType()), seeds, watershedResult);
        watershedResult = Views.interval(watershedResult, cutOffBorder); // cut off border pixels

        /* use the sheds from the watershed to split the labeled objects.
           If we had kept the border pixels, this would later result in the
           labels at the border being removed  */
        split("Watershed", watershedResult, inLabMasked);
        watershedResult = Views.zeroMin(watershedResult);

        final MooreContourExtractionOp contourExtraction = new MooreContourExtractionOp(true);
        final ArrayList<LabeledObject> objects = new ArrayList<LabeledObject>();

        LabelRegions<String> regions = KNIPGateway.regions().regions(watershedResult);
        final ArrayList<String> labels = new ArrayList<String>(regions.getExistingLabels());
        Collections.sort(labels, (o1, o2) -> o1.compareTo(o2) * -1);

        /* get some more information about the objects. Contour, bounding box etc */
        for (final String label : labels) {

            final IterableInterval<LabelingType<String>> intervalOverSrc =
                    Regions.sample(regions.getLabelRegion(label), watershedResult);

            /* create individual images for every object */
            final ExtendedPolygon poly = new ExtendedPolygon();

            /* compute contour polygon */
            contourExtraction.compute(regions.getLabelRegion(label), poly);

            objects.add(new LabeledObject(poly, label, intervalOverSrc));
        }

        final Cursor<LabelingType<String>> outC = Views.iterable(outLab).cursor();
        final Cursor<LabelingType<String>> inC = Views.iterable(watershedResult).cursor();

        while (inC.hasNext()) {
            outC.next().addAll(inC.next());
        }

        final ArrayList<Triple<LabeledObject, LabeledObject, String>> mergedList =
                new ArrayList<Triple<LabeledObject, LabeledObject, String>>();

        { /* scope for finding object pairs */
            final ArrayList<int[]> points = new ArrayList<int[]>();

            boolean found = false;

            final NeighborhoodsAccessible<LabelingType<String>> raNeighWatershed =
                    RECTANGLE_SHAPE.neighborhoodsRandomAccessibleSafe(Views.interval(
                                                                                     Views.extendValue(watershedResult,
                                                                                                       Util.getTypeFromInterval(watershedResult)
                                                                                                               .createVariable()),
                                                                                     watershedResult));

            /* find two objects that are closer than rSize */
            for (int i = 0; i < objects.size(); ++i) {
                for (int j = i + 1; j < objects.size(); ++j) {

                    final LabeledObject iObj = objects.get(i);
                    final LabeledObject jObj = objects.get(j);

                    final ExtendedPolygon iPoly = iObj.getContour();
                    final ExtendedPolygon jPoly = jObj.getContour();

                    final long[] jCenter = jPoly.getCenter();
                    final long[] iCenter = iPoly.getCenter();

                    final double diffX =
                            (iCenter[0] + iPoly.getBounds2D().getX()) - (jCenter[0] + jPoly.getBounds2D().getX());
                    final double diffY =
                            (iCenter[1] + iPoly.getBounds2D().getY()) - (jCenter[1] + jPoly.getBounds2D().getY());

                    if ((diffX * diffX + diffY * diffY) < m_seedDistanceThreshold) {
                        found = false; //reset flag

                        // find two points close to each other
                        for (final int[] iPoint : iPoly) {
                            if (found) {
                                break;
                            }

                            for (final int[] jPoint : jPoly) {
                                if (distanceSq(iPoint, jPoint) < 4) {
                                    found = true;

                                    points.add(iPoint);
                                    points.add(jPoint);
                                }
                            }
                        }

                        if (found) {
                            final String ijLabel = objects.get(j).getLabel();

                            final Cursor<LabelingType<String>> curs =
                                    iObj.getIterableIntervalOverWatershedResult().cursor();

                            final RandomAccess<LabelingType<String>> raWatershed = watershedResult.randomAccess();

                            // overwrite i to have label of j
                            while (curs.hasNext()) {
                                final LabelingType<String> next = curs.next();
                                next.clear();
                                next.add(ijLabel);
                            }

                            // set label of all points
                            for (final int[] point : points) {
                                raWatershed.setPosition(point);
                                raWatershed.get().clear();
                                raWatershed.get().add(ijLabel);
                            }

                            // fill remaining gap for both points
                            for (final int[] point : points) {
                                remainingGapFill(raNeighWatershed.randomAccess(watershedResult), point, ijLabel);
                            }

                            mergedList.add(new Triple<LabeledObject, LabeledObject, String>(iObj, jObj, ijLabel));

                            // transitive dependencies need to be resolved
                            final HashSet<Triple<LabeledObject, LabeledObject, String>> toRemove =
                                    new HashSet<Triple<LabeledObject, LabeledObject, String>>();
                            for (final Triple<LabeledObject, LabeledObject, String> triple : mergedList) {
                                if (triple.getThird().equals(iObj.m_label)) {
                                    toRemove.add(triple);
                                }
                            }

                            mergedList.removeAll(toRemove);
                            points.clear();
                        }
                    }
                }
            }
        } /* end scope for object finding */

        final RandomAccess<LabelingType<String>> raOut = outLab.randomAccess();

        // decide whether to merge objects using circularity and size
        for (final Triple<LabeledObject, LabeledObject, String> triple : mergedList) {
            final String label = triple.getThird();

            final LabelRegion<String> roi = regions.getLabelRegion(label);

            final IterableInterval<LabelingType<String>> intervalOverSrc = Regions.sample(roi, watershedResult);

            final ExtendedPolygon poly = new ExtendedPolygon();

            // compute contour polygon
            contourExtraction.compute(roi, poly);

            final long iSize = triple.getFirst().getIterableIntervalOverWatershedResult().size();
            final long jSize = triple.getSecond().getIterableIntervalOverWatershedResult().size();

            // decide whether to actually merge the objects
            final double iCircularity = featureCircularity(triple.getFirst().getPerimeter(), iSize);
            final double jCircularity = featureCircularity(triple.getSecond().getPerimeter(), jSize);
            final double ijCircularity = featureCircularity(poly.length(), intervalOverSrc.size());

            final Cursor<LabelingType<String>> curs = intervalOverSrc.cursor();
            if (!(iCircularity < ijCircularity || jCircularity < ijCircularity) || iSize < m_minMergeSize
                    || jSize < m_minMergeSize) {
                curs.reset();

                while (curs.hasNext()) {
                    curs.fwd();
                    raOut.setPosition(curs);
                    raOut.get().clear();
                    raOut.get().add(label);
                }
            }
        }

        return watershedResult;

    }

    /**
     * Create an instance of the labeling type contained in <code>inLab</code> and clear it, resulting in an empty
     * label.
     *
     * @param inLab labeling to get an empty label from.
     * @return the empty label
     */
    private LabelingType<L> getEmptyLabel(final RandomAccessibleInterval<LabelingType<L>> inLab) {
        final RandomAccess<LabelingType<L>> ra = inLab.randomAccess();
        ra.fwd(1);
        final LabelingType<L> labelingType = ra.get();
        final LabelingType<L> empty = labelingType.copy();
        empty.clear();
        return empty;
    }

    /**
     * Extend the given interval by 1 in every dimensions (assuming 2 dims).
     *
     * @param i interval to extend
     * @return the extended interval
     */
    private FinalInterval extendBorderByOne(final Interval i) {
        final long[] dims = Intervals.dimensionsAsLongArray(i);
        for (int d = 0; d < dims.length; d++) {
            dims[d] += 2;
        }
        return new FinalInterval(dims);
    }

    /**
     * Create an interval which has the same dimensions as the target Dimensions, but offset by 1 in dimensions 0 and 1
     * to cut off a 1 pixel border.
     *
     * @param target size of the result interval.
     * @return the created interval.
     */
    private FinalInterval cutOffBorder(final Dimensions target) {
        final long[] dims = new long[2];
        target.dimensions(dims);
        return new FinalInterval(new long[]{1, 1}, dims);
    }

    /**
     * Display the given image for debugging.
     *
     * @param img the image to display
     * @param title title of the window to display in
     */
    private void showImage(final Img<FloatType> img, final String title) {

        final IterableIntervalNormalize<FloatType> normalize = new IterableIntervalNormalize<FloatType>(0,
                new FloatType(), new ValuePair<FloatType, FloatType>(new FloatType(0), new FloatType(1)), false);

        Img<FloatType> normalized = img.factory().create(img, new FloatType());
        normalize.compute(img, normalized);

        AWTImageTools.showInFrame(normalized, title);
    }

    private final double featureCircularity(final double perimeter, final double roisize) {
        return perimeter / (2.0 * Math.sqrt(Math.PI * roisize));
    }

    private final void remainingGapFill(final RandomAccess<Neighborhood<LabelingType<String>>> ra, final int[] point,
                                        final String label) {
        ra.setPosition(point);
        final Cursor<LabelingType<String>> cNeigh = ra.get().cursor().copyCursor();

        int numNonij;
        boolean leq2;

        while (cNeigh.hasNext()) {
            final LabelingType<String> p = cNeigh.next();

            if (!p.contains(label)) {
                numNonij = 0;
                leq2 = true;

                ra.setPosition(cNeigh);
                final Cursor<LabelingType<String>> curs = ra.get().cursor();

                while (curs.hasNext()) {
                    if (!curs.next().contains(label)) {
                        ++numNonij;

                        if (numNonij > 2) {
                            leq2 = false;
                            break;
                        }
                    }
                }

                if (leq2) {
                    p.clear();
                    p.add(label);
                }
            }
        }
    }

    private final int distanceSq(final int[] iPoint, final int[] jPoint) {
        final int a = jPoint[0] - iPoint[0];
        final int b = jPoint[1] - iPoint[1];
        return (a * a + b * b);
    }

    @Override
    public BinaryObjectFactory<RandomAccessibleInterval<LabelingType<L>>, RandomAccessibleInterval<T>, RandomAccessibleInterval<LabelingType<String>>>
           bufferFactory() {
        return (lab, in) -> KNIPGateway.ops().create().imgLabeling(lab);
    }

    @Override
    public BinaryOutputOperation<RandomAccessibleInterval<LabelingType<L>>, RandomAccessibleInterval<T>, RandomAccessibleInterval<LabelingType<String>>>
           copy() {
        return new WaehlbySplitterOp<L, T>(m_segType, m_seedDistanceThreshold, m_minMergeSize, m_gaussSize,
                (CCA<BitType>)m_cca.copy(), (WatershedWithSheds<FloatType, Integer>)m_watershed.copy());
    }

    /**
     * Helper class containing a contour polygon, label and topleft point of bounding box of a labeled object
     *
     * @author Jonathan Hale (University of Konstanz)
     */
    private class LabeledObject {
        final ExtendedPolygon m_contour;

        final String m_label;

        final IterableInterval<LabelingType<String>> m_overSrc;

        public LabeledObject(final ExtendedPolygon c, final String l,
                             final IterableInterval<LabelingType<String>> oSrc) {

            m_contour = c;
            m_label = l;
            m_overSrc = oSrc;
        }

        public final ExtendedPolygon getContour() {
            return m_contour;
        }

        public final int getPerimeter() {
            return m_contour.length();
        }

        public final String getLabel() {
            return m_label;
        }

        public final IterableInterval<LabelingType<String>> getIterableIntervalOverWatershedResult() {
            return m_overSrc;
        }
    }

    private <RT extends RealType<RT>> IntervalView<RT> invertImg(final RandomAccessibleInterval<RT> rai,
                                                                 final RT type) {
        return Views.interval(
                              new ConvertedRandomAccessible<RT, RT>(rai,
                                      new UnaryOperationBasedConverter<RT, RT>(new SignedRealInvert<RT, RT>()), type),
                              rai);
    }

    /**
     * Inverter for signed RealTypes
     *
     * @author Christian Dietz (University of Konstanz)
     * @param <I>
     * @param <O>
     */
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

    private class LabelingToBitConverter<LT extends LabelingType<?>> implements Converter<LT, BitType> {

        @Override
        public void convert(final LT input, final BitType output) {
            output.set(!input.isEmpty());
        }

    }

    private void split(final String shedLabel, final RandomAccessibleInterval<LabelingType<String>> watershedResult,
                       final RandomAccessibleInterval<BitType> inLabMasked) {
        final Cursor<LabelingType<String>> cursor = Views.iterable(watershedResult).cursor();
        final RandomAccess<BitType> ra = inLabMasked.randomAccess();

        while (cursor.hasNext()) {
            final LabelingType<String> type = cursor.next();

            ra.setPosition(cursor);

            if (type.contains(shedLabel) || !ra.get().get()) {
                type.clear();
            }
        }

    }
}
