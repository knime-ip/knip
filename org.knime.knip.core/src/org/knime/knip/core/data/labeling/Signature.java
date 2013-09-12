/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.knime.knip.core.data.labeling;

import java.awt.Polygon;
import java.util.Arrays;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RealRandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.sampler.special.ConstantRandomAccessible;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.knip.core.algorithm.InplaceFFT;
import org.knime.knip.core.data.algebra.Complex;
import org.knime.knip.core.data.algebra.ExtendedPolygon;
import org.slf4j.LoggerFactory;

/**
 * Represents a signature (i.e. a line in the polar space) of a polygon in the cartesian space.
 * 
 * 
 * @author hornm
 * 
 */
public class Signature {

    /* the signature itself */
    private int[] m_sign;

    /* the original width of the (polar) image */
    private final long m_width;

    /* the actual length */
    private final long m_length;

    /* the center of the signature */
    private long[] m_center = new long[]{0, 0};

    /* the score from the signature derivation */
    private double m_score;

    /**
     * Builds a signature from an (polar) image.
     * 
     * @param interval the source image
     * @param maxVariance the maximum the signature will vary in the x-direction (corresponds to the radius)
     */
    public Signature(final IterableInterval<? extends RealType<?>> interval, final int maxVariance) {
        m_width = interval.dimension(0);
        m_length = interval.dimension(1);
        m_sign = extractMaxLine(calcWeights(interval), maxVariance);

    }

    /**
     * Builds a signature from an (polar) image.
     * 
     * @param interval
     * 
     * @param polarImage the source image
     * @param historyLength the maximal depth to be looked back for the signature retrieval
     * @param maxVariance the maximum the signature will vary in the x-direction (corresponds to the radius)
     */
    public Signature(final IterableInterval<? extends RealType<?>> interval, final int historyLength,
                     final int maxVariance) {

        m_sign = extractLine(calcWeights(interval), maxVariance, historyLength);
        m_width = interval.dimension(0);
        m_length = interval.dimension(1);
    }

    /**
     * Retrieves a signature from a bit mask. The first occurrence of the change from ON_VALUE to OFF_VALUE will used
     * for the individual signature positions.
     * 
     * @param mask
     * @param maskPosition
     * @param signatureLength
     * @param maxVariance
     * 
     */

    public Signature(final Img<BitType> mask, final long[] maskPosition, final int signatureLength) {

        int tmpx, tmpy, pos;
        m_length = signatureLength;
        m_sign = new int[signatureLength];
        m_center = maskPosition.clone();
        final double step = (2 * Math.PI) / signatureLength;

        // calc the centroid
        double centerX = 0;
        double centerY = 0;

        int count = 0;

        final Cursor<BitType> c = mask.localizingCursor();
        while (c.hasNext()) {
            c.fwd();
            if (c.get().get()) {
                centerX += c.getIntPosition(0);
                centerY += c.getIntPosition(1);
                count++;
            }
        }
        centerX = Math.round(centerX / count);
        centerY = Math.round(centerY / count);

        final RandomAccess<BitType> ra =
                Views.extend(mask, new OutOfBoundsConstantValueFactory<BitType, Img<BitType>>(new BitType(false)))
                        .randomAccess();
        final int rimWidth =
                (int)Math.round(Math.sqrt(Math.pow(mask.dimension(0), 2) + Math.pow(mask.dimension(1), 2)));
        m_width = rimWidth;
        // retrieve the signature from the mask, where the BinaryImage
        // changes
        // from ON_VALUE to OFF_VALUE
        for (double a = 0; Math.round(a / step) < signatureLength; a += step) {
            for (int r = 0; r < rimWidth; r++) {

                tmpx = (int)Math.round(r * Math.sin(a));
                tmpy = (int)Math.round(r * Math.cos(a));
                pos = (int)Math.round(a / step);

                tmpx = (int)centerX + tmpx;
                tmpy = (int)centerY + tmpy;

                if ((tmpx >= (mask.dimension(0) - 1)) || (tmpy >= (mask.dimension(1) - 1)) || (tmpx <= 0)
                        || (tmpy <= 0)) {
                    ra.setPosition(tmpx, 0);
                    ra.setPosition(tmpy, 1);
                    if (!ra.get().get()) {
                        m_sign[pos] = r;
                        r = rimWidth;
                    }
                    continue;
                }
            }
        }

        m_center = new long[]{maskPosition[0] + (int)centerX, maskPosition[1] + (int)centerY};
    }

    /**
     * Retrieves a signature from a bit mask. The first occurrence of the change from ON_VALUE to OFF_VALUE will used
     * for the individual signature positions.
     * 
     * @param mask
     * @param maskPosition
     * @param signatureLength
     * @param maxVariance
     * 
     */

    public Signature(final IterableRegionOfInterest roi, final long[] maskPosition, final int signatureLength) {

        int tmpx, tmpy, pos;
        m_length = signatureLength;
        m_sign = new int[signatureLength];
        m_center = maskPosition.clone();
        final double step = (2 * Math.PI) / signatureLength;

        // calc the centroid
        double centerX = 0;
        double centerY = 0;

        int count = 0;

        final IterableInterval<BitType> ii =
                roi.getIterableIntervalOverROI(new ConstantRandomAccessible<BitType>(new BitType(), roi.numDimensions()));

        final Cursor<BitType> c = ii.localizingCursor();
        while (c.hasNext()) {
            c.fwd();
            if (c.get().get()) {
                centerX += c.getIntPosition(0);
                centerY += c.getIntPosition(1);
                count++;
            }
        }
        centerX = Math.round(centerX / count);
        centerY = Math.round(centerY / count);

        final int rimWidth =
                (int)Math.round(Math.sqrt(Math.pow(ii.dimension(0) / 2, 2) + Math.pow(ii.dimension(1) / 2, 2)));

        final RealRandomAccess<BitType> ra = roi.realRandomAccess();
        m_width = rimWidth;
        // retrieve the signature from the mask, where the BinaryImage
        // changes
        // from ON_VALUE to OFF_VALUE
        for (double a = 0; Math.round(a / step) < signatureLength; a += step) {
            for (int r = 0; r < rimWidth; r++) {

                tmpx = (int)Math.round(r * Math.sin(a));
                tmpy = (int)Math.round(r * Math.cos(a));
                pos = (int)Math.round(a / step);

                tmpx = (int)centerX + tmpx;
                tmpy = (int)centerY + tmpy;

                if ((tmpx >= (ii.dimension(0) - 1)) || (tmpy >= (ii.dimension(1) - 1)) || (tmpx <= 0) || (tmpy <= 0)) {
                    ra.setPosition(tmpx, 0);
                    ra.setPosition(tmpy, 1);
                    if (!ra.get().get()) {
                        m_sign[pos] = r;
                        r = rimWidth;
                    }
                    continue;
                }
            }
        }

        m_center = new long[]{maskPosition[0] + (int)centerX, maskPosition[1] + (int)centerY};
    }

    /**
     * Creates a new signature.
     * 
     * @param sign the 'radial' positions for each angle (polar space). A copy will be made.
     * @param width the width of the signature, just for informal use
     */
    public Signature(final int[] sign, final int width) {
        m_sign = sign.clone();
        m_width = width;
        m_length = sign.length;
    }

    /**
     * The position (corresponds to the radius) of given index (corresponds to the angle).
     * 
     * @param index
     * @return
     */

    public int getPosAt(final int index) {
        return m_sign[index];
    }

    /**
     * The number of pixels in its length.
     * 
     * @return
     */

    public int length() {
        return m_sign.length;
    }

    /**
     * The score of this signature. The sum of the weights (from the polar image) normalized by the signature length.
     * 
     * @return a value between 0.0 and 1.0
     */
    public double getScore() {
        return m_score;
    }

    /**
     * The centre of the signature.
     * 
     * @return the centre as a 2-dim array. No copy is made!
     */
    public long[] getCentre() {
        return m_center;
    }

    /**
     * Sets a new centre for the signature. No copy is made!
     * 
     * @param center the new centre coordinates (2d)
     */
    public void setCentre(final long[] center) {
        m_center = center.clone();
    }

    /**
     * The width of the signature. It's simply the width of the polar image, where the signature stems from.
     * 
     * @return the width
     */
    public long getWidth() {
        return m_width;
    }

    /**
     * The area of the signature, i.e. the number of pixels on the left-hand side. Might differ a little bit from the
     * area of the contour in the Cartesian space.
     * 
     * @return
     */
    public int getArea() {
        int numCellPix = 0;
        for (int k = 0; k < length(); k++) {
            numCellPix += m_sign[k];
        }
        return numCellPix;
    }

    /*
     * Helper for the centralise method.
     */
    private void calcCenter() {

        // calc bounding box center or centroid

        double x, y;
        double oldx = -1;
        double oldy = -1;

        double maxX = 0;
        double maxY = 0;

        double minX = 10000;
        double minY = 10000;

        for (int i = 0; i < m_sign.length; i++) {

            // cartesian coordinates
            x = (m_sign[i] * Math.sin(((double)i / (double)m_sign.length) * 2 * Math.PI)) + m_center[0];
            y = ((m_sign[i] * Math.cos(((double)i / (double)m_sign.length) * 2 * Math.PI)) + m_center[1]);

            if ((Math.round(x) != oldx) || (Math.round(y) != oldy)) {

                oldx = Math.round(x);
                oldy = Math.round(y);

            } else {
                i++;
            }

            maxX = Math.max(maxX, x);
            minX = Math.min(minX, x);

            maxY = Math.max(maxY, y);
            minY = Math.min(minY, y);

        }

        // bounding box center
        x = (maxX + minX) / 2;
        y = (maxY + minY) / 2;

        // set new center
        m_center[0] = (int)Math.round(x);
        m_center[1] = (int)Math.round(y);

    }

    /**
     * Centralizes the signature according to the center point.
     * 
     */
    public void centralize() {

        // transform signature to cartesian coordinates and calc new
        // centroid
        // distance

        final long oldX = m_center[0];
        final long oldY = m_center[1];

        calcCenter();

        double x, y;
        double magn;
        double ang;
        final int[] tmp = new int[m_sign.length];

        for (int i = 0; i < m_sign.length; i++) {

            // cartesian coordinates
            x = ((m_sign[i] * Math.sin(((double)i / (double)m_sign.length) * 2 * Math.PI)) + oldX);

            y = ((m_sign[i] * Math.cos(((double)i / (double)m_sign.length) * 2 * Math.PI)) + oldY);

            magn = Math.sqrt(Math.pow(x - m_center[0], 2) + Math.pow(y - m_center[1], 2));
            // new centroid distance
            ang = Math.atan2((x - m_center[0]), (y - m_center[1]));
            if (ang < 0) {
                ang += 2 * Math.PI;
            }
            final int test = (int)Math.round((ang / (2 * Math.PI)) * ((double)m_sign.length - 1));

            // allocate some of the following array positions to
            // "interpolate"
            // missing values
            for (int j = 0; j < 5; j++) {
                tmp[(test + j) % m_sign.length] = (int)Math.round(magn);
            }

        }
        m_sign = tmp;
    }

    /**
     * Compares to signatures.
     * 
     * @return true, if the two signatures are exactly the same
     */
    @Override
    public boolean equals(final Object arg0) {
        if (arg0 instanceof Signature) {

            final Signature s = (Signature)arg0;
            if (s.length() != m_sign.length) {
                return false;
            }
            int diff = 0;
            for (int i = 0; i < m_sign.length; i++) {
                diff += Math.abs(m_sign[i] - s.getPosAt(i));
            }

            return diff < m_sign.length;
        } else {
            return false;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(m_sign);
    }

    /**
     * Transforms the signature to the corresponding {@link Polygon} in the Cartesian space.
     * 
     * @return the new contour
     */

    public ExtendedPolygon createPolygon() {

        int tmpx, tmpy;
        int r;
        final ExtendedPolygon rc = new ExtendedPolygon();
        for (int i = 0; i < m_sign.length; i++) {

            r = m_sign[i];
            tmpx = (int)Math.round(r * Math.cos(((double)i / (double)m_sign.length) * 2 * Math.PI));
            tmpy = -(int)Math.round(r * Math.sin(((double)i / (double)m_sign.length) * 2 * Math.PI));
            rc.addPoint((int)m_center[0] + tmpx, (int)m_center[1] + tmpy);
        }
        rc.setCenter(m_center);

        return rc;
    }

    /**
     * Transforms the signature to the corresponding {@link Polygon} in the Cartesian space.
     * 
     * @param offset an offset for the polygon points
     * 
     * @return the new contour
     */

    public ExtendedPolygon createPolygon(final int offset) {

        int tmpx, tmpy;
        int r;
        final ExtendedPolygon rc = new ExtendedPolygon();
        for (int i = 0; i < m_sign.length; i++) {

            r = m_sign[i] + offset;
            tmpx = (int)Math.round(r * Math.cos(((double)i / (double)m_sign.length) * 2 * Math.PI));
            tmpy = -(int)Math.round(r * Math.sin(((double)i / (double)m_sign.length) * 2 * Math.PI));
            rc.addPoint((int)m_center[0] + tmpx, (int)m_center[1] + tmpy);
        }
        rc.setCenter(m_center);

        return rc;
    }

    /**
     * Creates an image (width x length) from the signature, where all pixels from the signature are on, the rest off.
     * 
     * @return the image
     */
    public Img<BitType> createImage() {
        final Img<BitType> res = new ArrayImgFactory<BitType>().create(new long[]{m_width, m_length}, new BitType());
        final RandomAccess<BitType> c =
                Views.extend(res, new OutOfBoundsConstantValueFactory<BitType, Img<BitType>>(new BitType(false)))
                        .randomAccess();
        for (int i = 0; i < m_length; i++) {
            c.setPosition(m_sign[i], 0);
            c.setPosition(i, 1);
            c.get().set(true);
        }
        return res;
    }

    public Point getCartCoords(final int x, final int y) {

        final int tmpx =
                (int)Math.round(x * Math.cos(((double)y / (double)m_sign.length) * 2 * Math.PI)) + (int)m_center[0];
        final int tmpy =
                -(int)Math.round(x * Math.sin(((double)y / (double)m_sign.length) * 2 * Math.PI)) + (int)m_center[1];

        return new Point(new long[]{tmpx, tmpy});
    }

    // ---- Some helper methods to extract the signature from polar images
    // --//

    /**
     * Smoothes the signature by cutting the desired frequencies. This operation requires the signature to have a length
     * of power of 2! If not, an exception will be thrown.
     * 
     * @param cutoff the frequencies which should be kept
     */
    public void lowPassFilter(final int cutoff) {

        final Complex[] g = Complex.makeComplexVector(m_sign);
        final int N = g.length;

        // fast fourier transform
        final Complex[] G = InplaceFFT.fft(g);

        // delete frequencies
        for (int i = cutoff; i < (N - cutoff); i++) {
            G[i] = new Complex(0, 0);
        }
        // inverse fast fourier transformation
        final Complex[] res = InplaceFFT.ifft(G);
        for (int i = 0; i < res.length; i++) {
            m_sign[i] = (int)Math.round(res[i].re());
        }
    }

    /*
     * Retrieves the weights matrix from an (polar) image.
     */
    protected double[][] calcWeights(final IterableInterval<? extends RealType<?>> interval) {
        final double[][] weights = new double[(int)interval.dimension(1)][(int)interval.dimension(0)];
        final Cursor<? extends RealType<?>> c = interval.cursor();
        final RealType<?> val = c.get();
        final double max = (val.getMaxValue() - val.getMinValue());
        while (c.hasNext()) {
            c.fwd();
            weights[c.getIntPosition(1)][c.getIntPosition(0)] = (val.getRealDouble() - val.getMinValue()) / max;

        }
        return weights;
    }

    /*
     * Helper for the constructor to extract the Signature-Line by dynamic
     * programming. The polar image is considered as a graph and we are
     * searching for the "best CLOSED path" (in this case longest path). In
     * each step the best of the available parents will be choosen, his
     * weighted added to the current node and the path direction stored. ...
     */

    private int[] extractMaxLine(final double[][] weights, final int maxLineVariance) {

        final double[][] scores = new double[weights.length][weights[0].length];
        scores[0] = weights[0].clone();

        final int[][] dirs = new int[weights.length][weights[0].length];

        // calc the shortest pathes
        double max;
        int dir = 0;

        for (int i = 1; i < weights.length; i++) {
            for (int j = 0; j < weights[0].length; j++) {

                // the maximum within the max line variance
                max = scores[i - 1][j];
                dir = 0;

                for (int s = -maxLineVariance; s <= maxLineVariance; s++) {
                    if ((s == 0) || ((j + s) < 0) || ((j + s) > (weights[0].length - 1))) {
                        continue;
                    }
                    if (scores[i - 1][j + s] > max) {
                        max = scores[i - 1][j + s];
                        dir = s;
                    }
                }
                scores[i][j] = max + weights[i][j];
                dirs[i][j] = dir;
            }
        }

        // backtrack the best path two times wich in the most cases
        // results in a
        // closed contour. Else, directly find the maximal closed path.
        final int[] res = new int[weights.length];
        dir = dirs[0].length / 2;
        for (int count = 0; count < 2; count++) {

            res[res.length - 1] = dir;
            for (int i = dirs.length - 2; i >= 0; i--) {
                dir += dirs[i + 1][dir];
                res[i] = dir;
            }
            if (Math.abs(res[0] - res[res.length - 1]) <= maxLineVariance) {
                break;
            }
        }
        m_score = scores[scores.length - 1][res[res.length - 1]] / scores.length;

        // if the found best path isn't closed, backtrack all possible
        // leaves
        // until a closed path was found

        if (Math.abs(res[0] - res[res.length - 1]) > maxLineVariance) {
            // collect the leaves
            max = 0;
            final IndexedDouble[] leaves = new IndexedDouble[scores[0].length];
            for (int i = 0; i < weights[0].length; i++) {
                leaves[i] = new IndexedDouble(scores[scores.length - 1][i], i);
            }

            Arrays.sort(leaves);

            // backtrack to retrieve the path from the best leaf as
            // long as a
            // closed path was found
            for (int l = scores[0].length - 1; l >= 0; l--) {
                dir = leaves[l].getIndex();
                res[dirs.length - 1] = dir;
                for (int i = dirs.length - 2; i >= 0; i--) {
                    dir += dirs[i + 1][dir];
                    res[i] = dir;

                }
                m_score = leaves[l].getVal() / scores.length;
                if (Math.abs(res[0] - res[res.length - 1]) <= maxLineVariance) {
                    LoggerFactory.getLogger(Signature.class).debug("alternative backtrack: " + l);
                    break;
                }
            }
        }
        return res;
    }

    /*
     * Method, which helps to extract the signature from a polar image with
     * a arbitrary depth m_historyLength
     */

    private int[] extractLine(final double[][] weights, final int maxLineVariance, final int historyLength) {

        Path[] pathes = new Path[weights[0].length];
        final Path[] pathes2 = new Path[weights[0].length];

        double[] scores;

        for (int i = 0; i < pathes.length; i++) {
            pathes[i] = new Path(weights, historyLength, weights[0][i], i);
        }

        double max;
        int pos;

        // closeness
        double maxPathScore = 0;

        final int iterations = 2; // number of iterations of the whole polar
        // image,
        // minimum number is 2

        for (int i = 1; i < (iterations * weights.length); i++) {
            scores = getScores(pathes);
            maxPathScore = 0;
            for (int j = 0; j < weights[0].length; j++) {

                // the maximum within the max line variance
                max = scores[j];
                pos = j;
                for (int s = -maxLineVariance; s <= maxLineVariance; s++) {
                    if (((j + s) < 0) || ((j + s) > (weights[0].length - 1))) {
                        continue;
                    }

                    if (scores[j + s] > max) {
                        max = scores[j + s];
                        pos = j + s;
                    }

                }

                pathes2[j] = pathes[pos].clone();
                pathes2[j].addPos(i, j);

                if (pathes2[j].m_globalScore > maxPathScore) {
                    maxPathScore = pathes2[j].m_globalScore;
                }

            }
            pathes = pathes2.clone();

        }

        Arrays.sort(pathes);

        // find maximal closed path
        int i;
        for (i = 0; i < pathes.length; i++) {
            if (Math.abs(pathes[i].m_pos[weights.length - 1] - pathes[i].m_pos[0]) < (maxLineVariance * 2)) {
                break;
            }

        }
        if (i == (pathes.length - 1)) {
            m_score = 0;
        } else {
            m_score = pathes[i].m_globalScore / weights.length;
        }
        return pathes[i].m_pos;
    }

    /*
     * Hepler to associate a double value with an index. Here: to keep the
     * original index of a double list after sorting.
     */
    private static class IndexedDouble implements Comparable<IndexedDouble> {

        private double m_val;

        private int m_index;

        public IndexedDouble(final double val, final int index) {
            m_val = val;
            m_index = index;
        }

        public double getVal() {
            return m_val;
        }

        public int getIndex() {
            return m_index;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof IndexedDouble) {
                return Math.abs(m_val - ((IndexedDouble)obj).m_val) < 0.0000001;
            } else {
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return new Double(m_val).hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final IndexedDouble val2) {
            return Double.compare(m_val, m_val);
        }
    }

    /*
     * Object represents a path though the plane of a polar image of a
     * specific length
     */
    private static class Path implements Comparable<Path>, Cloneable {

        private int[] m_pos;

        private int m_h;

        private double m_score;

        private double m_globalScore;

        private final double[][] m_weights;

        Path(final double[][] weights, final int historyLength, final double score, final int index) {
            m_pos = new int[weights.length];
            m_h = historyLength;
            m_score = score;
            m_globalScore = score;
            m_pos[0] = index;
            m_weights = weights.clone();
        }

        void addPos(int index, final int pos) {

            if (index >= m_weights.length) {
                m_globalScore -=
                        m_weights[(index - m_weights.length) % m_weights.length][m_pos[(index - m_weights.length)
                                % m_weights.length]];
            }

            if (index >= m_h) {
                m_score -=
                        m_weights[((index * 10) - m_h) % m_weights.length][m_pos[((index * 10) - m_h)
                                % m_weights.length]];
            }

            index = index % m_weights.length;
            m_pos[index] = pos;
            m_score += m_weights[index][pos];
            m_globalScore += m_weights[index][pos];
        }

        @Override
        public int compareTo(final Path o) {
            return Double.compare(o.m_globalScore, m_globalScore);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof Path) {
                return Math.abs(m_globalScore - ((Path)obj).m_globalScore) < 0.0000001;
            } else {
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return new Double(m_globalScore).hashCode();
        }

        @Override
        public Path clone() {
            final Path p = new Path(m_weights, m_h, m_score, m_pos[0]);
            p.m_h = this.m_h;
            p.m_pos = this.m_pos.clone();
            p.m_score = this.m_score;
            p.m_globalScore = this.m_globalScore;

            return p;
        }
    }

    /*
     * Gets the scores from the given pathes
     */
    private double[] getScores(final Path[] pathes) {
        final double[] res = new double[pathes.length];
        for (int i = 0; i < pathes.length; i++) {
            res[i] = pathes[i].m_score;
        }
        return res;
    }
}
