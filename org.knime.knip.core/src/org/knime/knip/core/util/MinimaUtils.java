/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2016
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
 * Created on Mar 6, 2016 by dietzc
 */
package org.knime.knip.core.util;

import java.util.Arrays;

import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.labeling.Labeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * Utility functions mainly used to work with the minima of {@link ImgPlus}' or {@link Labeling}s. It mainly provides
 * convenience methods to translate imgs or labelings with respect to their minima.
 *
 * @author Christian Dietz, University of Konstanz
 */
public class MinimaUtils {

    /**
     * @param imgPlus
     * @return translated {@link ImgPlus}
     */
    public static <T extends RealType<T>> ImgPlus<T> getZeroMinImgPlus(final ImgPlus<T> imgPlus) {

        final long[] min = Intervals.minAsLongArray(imgPlus);
        final boolean hasNonZeroMin = hasNonZeroMin(min);

        if (hasNonZeroMin) {
            return new ImgPlus<T>(ImgView.wrap(Views.zeroMin(imgPlus.getImg()), imgPlus.factory()), imgPlus);
        } else {
            return imgPlus;
        }
    }

    /**
     * @param fromCell
     * @param res
     * @return translated {@link ImgPlus}
     */
    public static <T extends RealType<T>, V extends RealType<V>> ImgPlus<V>
           getTranslatedImgPlus(final ImgPlus<T> fromCell, final ImgPlus<V> res) {
        final long[] min = Intervals.minAsLongArray(fromCell);
        final boolean hasNonZeroMin = hasNonZeroMin(min);

        if (Arrays.equals(min, Intervals.minAsLongArray(res))) {
            return res;
        }

        if (hasNonZeroMin) {
            return new ImgPlus<V>(ImgView.wrap(Views.translate(res.getImg(), min), res.factory()), res);
        } else {
            return res;
        }
    }

    /**
     * @param labeling
     * @return translated labeling
     */
    public static <L> RandomAccessibleInterval<LabelingType<L>>
           getZeroMinLabeling(final RandomAccessibleInterval<LabelingType<L>> labeling) {

        final long[] min = Intervals.minAsLongArray(labeling);
        final boolean hasNonZeroMin = hasNonZeroMin(min);

        if (hasNonZeroMin) {
            return Views.zeroMin(labeling);
        } else {
            return labeling;
        }
    }

    /**
     * @param fromCell
     * @param res
     * @return translated labeling
     */
    public static <L, M> RandomAccessibleInterval<LabelingType<M>>
           getTranslatedLabeling(final Interval fromCell, final RandomAccessibleInterval<LabelingType<M>> res) {

        final long[] min = Intervals.minAsLongArray(fromCell);

        if (Arrays.equals(min, Intervals.minAsLongArray(res))) {
            return res;
        }

        final boolean hasNonZeroMin = hasNonZeroMin(min);

        if (hasNonZeroMin) {
            return Views.translate(res, min);
        } else {
            return res;
        }
    }

    /**
     * @param min
     * @return
     */
    private static boolean hasNonZeroMin(final long[] min) {
        boolean hasNonZeroMin = false;
        for (int i = 0; i < min.length; i++) {
            if (min[i] != 0) {
                hasNonZeroMin = true;
                break;
            }
        }

        return hasNonZeroMin;
    }
}
