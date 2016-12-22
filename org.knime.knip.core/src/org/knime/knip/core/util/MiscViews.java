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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.core.util;

import java.util.ArrayList;
import java.util.List;

import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;

import net.imagej.ImgPlus;
import net.imagej.axis.DefaultTypedAxis;
import net.imagej.axis.TypedAxis;
import net.imagej.space.DefaultTypedSpace;
import net.imagej.space.TypedSpace;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.util.MetadataUtil;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.Type;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * TODO Auto-generated
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class MiscViews {

    /**
     * removes dimensions of size 1 if any.
     *
     * @param ret
     * @return
     */
    public static <T extends Type<T>> ImgPlus<T> cleanImgPlus(final ImgPlus<T> ret) {

        final List<Integer> oneSizedDims = getOneSizeDims(ret);
        if (oneSizedDims.size() == 0) {
            return ret;
        }

        final ImgPlus<T> imgPlusView =
                new ImgPlus<T>(ImgView.wrap(SubsetOperations.subsetview(ret.getImg(), ret.getImg()), ret.factory()));
        MetadataUtil.copyAndCleanImgPlusMetadata(ret, ret, imgPlusView);

        final long[] oldMin = Intervals.minAsLongArray(imgPlusView);
        final long[] newMin = new long[oldMin.length];

        boolean hasNonZeroMin = false;
        int offset = 0;
        for (int i = 0; i < oldMin.length; i++) {
            if (oneSizedDims.contains(i)) {
                offset++;
            } else {
                newMin[i - offset] = oldMin[i];
                if (newMin[i - offset] != 0) {
                    hasNonZeroMin = true;
                }
            }
        }

        IntervalView<T> translated = Views.translate(imgPlusView.getImg(), newMin);

        if (hasNonZeroMin) {
            ImgPlus<T> img = ImgPlus.wrap(ImgView.wrap(translated, imgPlusView.factory()), imgPlusView);
            img.setSource(ret.getSource());
            return img;
        } else {
            return ImgPlus.wrap(imgPlusView, imgPlusView);
        }
    }

    /**
     * {@link RandomAccessibleInterval} with same sice as target is returned
     *
     * @param src {@link RandomAccessibleInterval} to be adjusted
     * @param target {@link Interval} describing the resulting sizes
     * @return Adjusted {@link RandomAccessibleInterval}
     */
    public static <T> RandomAccessibleInterval<T>
           synchronizeDimensionality(RandomAccessibleInterval<T> src, final TypedSpace<? extends TypedAxis> srcSpace,
                                     final Interval target, final TypedSpace<? extends TypedAxis> targetSpace) {

        // must hold, if not: most likely an implementation error
        assert ((srcSpace.numDimensions() == src.numDimensions())
                && (target.numDimensions() == targetSpace.numDimensions()));

        // Check direction of conversion
        if (Intervals.equals(src, target) && spaceEquals(srcSpace, targetSpace)) {
            return src;
        }

        // Extend
        src = Views.interval(Views.extendBorder(src), src);

        // Init result vars
        RandomAccessibleInterval<T> res = src;
        final TypedSpace<TypedAxis> resSpace = new DefaultTypedSpace(target.numDimensions());

        // 1. Step remove axis from source which can't be found in
        // target
        final TypedAxis[] dispensable = getDeltaAxisTypes(targetSpace, srcSpace);
        for (int d = dispensable.length - 1; d >= 0; --d) {
            final int idx = srcSpace.dimensionIndex(dispensable[d].type());
            res = Views.hyperSlice(res, idx, 0);
        }

        int i = 0;
        outer: for (int d = 0; d < srcSpace.numDimensions(); d++) {
            for (final TypedAxis typedAxis : dispensable) {
                if (d == srcSpace.dimensionIndex(typedAxis.type())) {
                    continue outer;
                }
            }

            resSpace.setAxis(srcSpace.axis(d), i++);
        }

        // 2. Add Axis which are available in target but not in source
        final TypedAxis[] missing = getDeltaAxisTypes(srcSpace, targetSpace);

        // Dimensions are added and resSpace is synchronized with res
        i = srcSpace.numDimensions() - dispensable.length;
        for (final TypedAxis typedAxis : missing) {
            final int idx = targetSpace.dimensionIndex(typedAxis.type());
            res = Views.addDimension(res, target.min(idx), target.max(idx));
            resSpace.setAxis(new DefaultTypedAxis(typedAxis.type()), i++);
        }

        // res should have the same size, but with different metadata
        assert (res.numDimensions() == targetSpace.numDimensions());

        // 3. Permutate axis if necessary
        RandomAccessible<T> resRndAccessible = res;
        for (int d = 0; d < res.numDimensions(); d++) {
            final int srcIdx = resSpace.dimensionIndex(targetSpace.axis(d).type());

            if (srcIdx != d) {
                resRndAccessible = Views.permute(resRndAccessible, srcIdx, d);

                // also permutate calibrated space
                final TypedAxis tmp = resSpace.axis(d);
                resSpace.setAxis(targetSpace.axis(d), d);
                resSpace.setAxis(tmp, srcIdx);
            }
        }

        return Views.interval(resRndAccessible, target);
    }

    /**
     * {@link RandomAccessibleInterval} with same sice as target is returned
     *
     * @param src {@link RandomAccessibleInterval} to be adjusted
     * @param target {@link Interval} describing the resulting sizes
     * @return Adjusted {@link RandomAccessibleInterval}
     */
    public static <T> RandomAccessibleInterval<T> synchronizeDimensionality(final RandomAccessibleInterval<T> src,
                                                                            final Interval target) {
        IntervalView<T> res = Views.interval(Views.extendBorder(src), src);

        // Check direction of conversion
        if (Intervals.equals(src, target)) {
            return res;
        }

        // adjust dimensions
        if (res.numDimensions() < target.numDimensions()) {
            for (int d = res.numDimensions(); d < target.numDimensions(); d++) {
                res = Views.addDimension(res, target.min(d), target.max(d));
            }
        } else {
            for (int d = res.numDimensions() - 1; d >= target.numDimensions(); --d) {
                res = Views.hyperSlice(res, d, 0);
            }
        }

        final long[] resDims = new long[res.numDimensions()];
        res.dimensions(resDims);

        return Views.interval(res, target);

    }

    /**
     * {@link RandomAccessibleInterval} with same sice as target is returned
     *
     * @param src {@link RandomAccessibleInterval} to be adjusted
     * @param target {@link Interval} describing the resulting sizes
     * @return Adjusted {@link RandomAccessibleInterval}
     */
    public static Interval synchronizeDimensionality(final Interval src, final Interval target) {
        Interval res = new FinalInterval(src);

        // Check direction of conversion
        if (Intervals.equals(src, target)) {
            return res;
        }

        // adjust dimensions

        long[] mins = new long[target.numDimensions()];
        long[] maxs = new long[target.numDimensions()];

        if (target.numDimensions() >= res.numDimensions()) {
            for (int d = 0; d < res.numDimensions(); d++) {
                mins[d] = (res.min(d) <= target.min(d)) ? res.min(d) : target.min(d);
                maxs[d] = (res.max(d) >= target.max(d)) ? res.max(d) : target.max(d);
            }
            for (int d = res.numDimensions(); d < target.numDimensions(); d++) {
                mins[d] = target.min(d);
                maxs[d] = target.max(d);
            }
        } else {
            for (int d = 0; d < target.numDimensions(); d++) {
                mins[d] = (res.min(d) <= target.min(d)) ? res.min(d) : target.min(d);
                maxs[d] = (res.max(d) >= target.max(d)) ? res.max(d) : target.max(d);
            }
        }
        res = new FinalInterval(mins, maxs);

        return res;

    }

    public static <T, F extends RandomAccessibleInterval<T>> RandomAccessibleInterval<T>
           synchronizeDimensionality(final F src, final Interval target, final OutOfBoundsFactory<T, F> factory) {
        IntervalView<T> res = Views.interval(Views.extend(src, factory), src);

        // Check direction of conversion
        if (Intervals.equals(src, target)) {
            return res;
        }

        // adjust dimensions
        if (res.numDimensions() < target.numDimensions()) {
            for (int d = res.numDimensions(); d < target.numDimensions(); d++) {
                res = Views.addDimension(res, target.min(d), target.max(d));
            }
        } else {
            for (int d = res.numDimensions() - 1; d >= target.numDimensions(); --d) {
                res = Views.hyperSlice(res, d, 0);
            }
        }

        final long[] resDims = new long[res.numDimensions()];
        res.dimensions(resDims);

        return Views.interval(res, target);

    }

    public static PlaneSelectionEvent adjustPlaneSelection(final PlaneSelectionEvent e, final Interval target) {
        final long[] pos = new long[target.numDimensions()];
        if (target.numDimensions() > e.numDimensions()) {
            for (int d = 0; d < e.numDimensions(); d++) {
                pos[d] = e.getPlanePosAt(d);
            }
        } else {
            for (int d = 0; d < target.numDimensions(); d++) {
                pos[d] = e.getPlanePosAt(d);
            }
        }

        int dim1 = 0, dim2 = 0;

        if (e.getPlaneDimIndex1() < target.numDimensions()) {
            dim1 = e.getPlaneDimIndex1();
        }
        if (e.getPlaneDimIndex2() < target.numDimensions()) {
            dim2 = e.getPlaneDimIndex2();
        }

        return new PlaneSelectionEvent(dim1, dim2, pos);
    }

    private static boolean spaceEquals(final TypedSpace<? extends TypedAxis> srcSpace,
                                       final TypedSpace<? extends TypedAxis> targetSpace) {

        if (srcSpace.numDimensions() != targetSpace.numDimensions()) {
            return false;
        }

        for (int d = 0; d < srcSpace.numDimensions(); d++) {
            if (!srcSpace.axis(d).equals(targetSpace.axis(d))) {
                return false;
            }
        }
        return true;
    }

    /*
     * Calculate the delta axis which are missing in the smaller space. >
     * From the smallest index of axistype to the biggest
     */
    private synchronized static TypedAxis[] getDeltaAxisTypes(final TypedSpace<? extends TypedAxis> sourceSpace,
                                                              final TypedSpace<? extends TypedAxis> targetSpace) {

        final List<TypedAxis> delta = new ArrayList<TypedAxis>();
        for (int d = 0; d < targetSpace.numDimensions(); d++) {
            final TypedAxis axis = targetSpace.axis(d);
            if (sourceSpace.dimensionIndex(axis.type()) == -1) {
                delta.add(axis);
            }
        }
        return delta.toArray(new TypedAxis[delta.size()]);
    }

    // determine whether an interval has dimensions of size 1
    private static List<Integer> getOneSizeDims(final Interval i) {
        final List<Integer> dims = new ArrayList<>();

        for (int d = 0; d < i.numDimensions(); d++) {
            if (i.dimension(d) == 1) {
                dims.add(d);
            }
        }
        return dims;
    }
}
