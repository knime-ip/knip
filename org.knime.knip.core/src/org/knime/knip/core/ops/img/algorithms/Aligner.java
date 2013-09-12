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
package org.knime.knip.core.ops.img.algorithms;

import java.util.Arrays;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.legacy.fft.PhaseCorrelation;
import net.imglib2.algorithm.legacy.fft.PhaseCorrelationPeak;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.BinaryObjectFactory;
import net.imglib2.ops.operation.BinaryOutputOperation;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.img.unary.ImgCopyOperation;
import net.imglib2.ops.operation.imgplus.unary.ImgPlusCrop;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.type.numeric.RealType;

/**
 * Image projection.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author tcriess, University of Konstanz
 */
public class Aligner<T extends RealType<T>, V extends RealType<V>> implements
        BinaryOutputOperation<Img<T>, Img<V>, Img<T>> {

    public final static int MIN_DIMS = 3;

    public final static int MAX_DIMS = 5;

    private int[] m_selectedDims;

    private int m_alignDim;

    private Interval m_iv;

    private int[] m_xind;

    private int[] m_yind;

    private int[] m_gxind;

    private int[] m_gyind;

    private long[] m_offset;

    private long[] m_size;

    private SIZEMODES m_sizemode;

    private ALIGNMODES m_alignmode;

    public enum SIZEMODES {
        NOTHING, CROP, EXTEND
    };

    public enum ALIGNMODES {
        FIRST, LAST, PAIRWISE, STEPWISE
    };

    private int m_stepsize;

    private int m_minPixOverlap;

    public Aligner(final int[] selectedDims, final int alignDim, final Interval iv, final SIZEMODES sizemode,
                   final ALIGNMODES alignmode, final int stepsize, final int minPixOverlap) {
        m_selectedDims = selectedDims.clone();
        m_alignDim = alignDim;
        m_sizemode = sizemode;
        m_alignmode = alignmode;
        m_iv = iv;
        m_stepsize = stepsize;
        m_minPixOverlap = minPixOverlap;
    }

    public Aligner(final int[] selectedDims, final int alignDim, final Interval iv) {
        this(selectedDims, alignDim, iv, SIZEMODES.CROP, ALIGNMODES.FIRST, 1, 1);
    }

    /**
     * {@inheritDoc}
     */
    private Img<T> createType(final Img<T> src, final Img<V> srcFiltered) {
        computeShifts(srcFiltered);
        if (m_sizemode != SIZEMODES.NOTHING) {
            return createType(src, srcFiltered, m_size);
        } else {
            final long dims[] = new long[srcFiltered.numDimensions()];
            srcFiltered.dimensions(dims);
            return createType(src, srcFiltered, dims);
        }
    }

    private void computeShifts(final Img<V> src) {
        final Img<V> imgPlus = src;
        final int[] selectedDims1 = m_selectedDims;
        final int[] selectedDims2 = new int[1];
        selectedDims2[0] = m_alignDim;
        final int selectedDim2 = selectedDims2[0];

        final long tmin = imgPlus.min(selectedDim2);
        final long tmax = imgPlus.max(selectedDim2);

        final long[] ipmin = new long[imgPlus.numDimensions()];
        final long[] ipmax = new long[imgPlus.numDimensions()];
        imgPlus.min(ipmin);
        imgPlus.max(ipmax);
        final long[] spmin = new long[imgPlus.numDimensions()];
        final long[] spmax = new long[imgPlus.numDimensions()];
        imgPlus.min(spmin);
        imgPlus.max(spmax);

        final int[] allDims = new int[imgPlus.numDimensions()];
        for (int i = 0; i < imgPlus.numDimensions(); i++) {
            allDims[i] = i;
        }
        final int[] remainingDims = new int[imgPlus.numDimensions() - selectedDims1.length - selectedDims2.length];
        for (int i = 0; i < selectedDims1.length; i++) {
            allDims[selectedDims1[i]] = -1;
        }
        for (int i = 0; i < selectedDims2.length; i++) {
            allDims[selectedDims2[i]] = -1;
        }
        int j = 0;
        for (int i = 0; i < imgPlus.numDimensions(); i++) {
            if (allDims[i] > -1) {
                remainingDims[j] = i;
                j++;
            }
        }
        // use the imglib2-algorithms

        // collect the subimages along the alignment axis
        @SuppressWarnings("unchecked")
        final Img<V>[] sis = new ImgView[(int)((ipmax[selectedDim2] - ipmin[selectedDim2]) + 1)];

        Img<V> tmpsis;

        for (long t = tmin; t <= tmax; t++) {
            ipmin[selectedDim2] = t;
            ipmax[selectedDim2] = t;
            for (int i = 0; i < remainingDims.length; i++) {
                ipmin[remainingDims[i]] = m_iv.min(remainingDims[i]);
                ipmax[remainingDims[i]] = m_iv.min(remainingDims[i]);
            }
            Interval i = new FinalInterval(ipmin, ipmax);

            tmpsis = new ImgView<V>(SubsetOperations.subsetview(src, i), imgPlus.factory());
            final long[] tmpmin = new long[tmpsis.numDimensions()];
            final long[] tmpmax = new long[tmpsis.numDimensions()];
            tmpsis.min(tmpmin);
            tmpsis.max(tmpmax);
            i = new FinalInterval(tmpmin, tmpmax);

            // TODO: Each time there is a new tmpsis result created.
            // can't this be directlny pushed to res?
            // ggf. hier copy anlegen
            sis[(int)(t - tmin)] = new ImgView<V>(SubsetOperations.subsetview(tmpsis, i), tmpsis.factory());
        }

        // Crop data
        m_offset = new long[imgPlus.numDimensions()];
        m_size = new long[imgPlus.numDimensions()];
        final long[] origsize = new long[imgPlus.numDimensions()];

        src.dimensions(m_size);
        src.dimensions(origsize);
        for (int i = 0; i < imgPlus.numDimensions(); i++) {
            m_offset[i] = 0;
        }
        // end crop data

        m_xind = new int[(int)((spmax[selectedDim2] - spmin[selectedDim2]) + 1)];
        m_yind = new int[(int)((spmax[selectedDim2] - spmin[selectedDim2]) + 1)];

        m_gxind = new int[(int)((spmax[selectedDim2] - spmin[selectedDim2]) + 1)];
        m_gyind = new int[(int)((spmax[selectedDim2] - spmin[selectedDim2]) + 1)];

        m_xind[0] = 0;
        m_yind[0] = 0;

        long tstart = tmin + 1;
        long tend = tmax;
        if (m_alignmode == ALIGNMODES.LAST) {
            tstart = tmin;
            tend = tmax - 1;
        }

        PhaseCorrelation<V, V> p;
        long ref;
        final long[][] cache = new long[(int)tend + 1][(int)((spmax[selectedDim2] - spmin[selectedDim2]) + 1)];

        for (long t = tstart; t <= tend; t++) {

            if (m_alignmode == ALIGNMODES.FIRST) {
                ref = 0;
            } else if (m_alignmode == ALIGNMODES.LAST) {
                ref = tmax;
            } else if (m_alignmode == ALIGNMODES.PAIRWISE) {
                ref = t - 1;
            } else { // ALIGNMODES.STEPWISE
                ref = ((long)Math.ceil(t / (double)m_stepsize) * m_stepsize) - m_stepsize;
                if (ref < tstart) {
                    ref = tstart;
                }
            }

            p = new PhaseCorrelation<V, V>(sis[(int)ref], sis[(int)t]);

            p.setMinimalPixelOverlap(m_minPixOverlap);

            if (p.process()) { // success
                final PhaseCorrelationPeak pe = p.getShift();
                final long[] pos = pe.getPosition();
                if (m_alignmode == ALIGNMODES.STEPWISE) {
                    cache[(int)t] = pos;

                    pos[0] += cache[(int)ref][0];
                    pos[1] += cache[(int)ref][1];
                } else if (m_alignmode == ALIGNMODES.PAIRWISE) {
                    // accumulate the shifts
                    pos[0] -= m_xind[(int)(t - tmin - 1)];
                    pos[1] -= m_yind[(int)(t - tmin - 1)];
                }

                m_xind[(int)(t - tmin)] = (int)-pos[0];
                m_yind[(int)(t - tmin)] = (int)-pos[1];

                if (m_sizemode == SIZEMODES.CROP) {
                    // crop data
                    if (pos[0] < 0) {
                        // move to the left
                        m_size[selectedDims1[0]] =
                                Math.min(origsize[selectedDims1[0]] + pos[0], m_size[selectedDims1[0]]);
                    } else {
                        // move to the right
                        m_offset[selectedDims1[0]] = Math.max(m_offset[selectedDims1[0]], pos[0]);
                    }
                    if (pos[1] < 0) {
                        // move up
                        m_size[selectedDims1[1]] =
                                Math.min(origsize[selectedDims1[1]] + pos[1], m_size[selectedDims1[1]]);
                    } else {
                        // move down
                        m_offset[selectedDims1[1]] = Math.max(m_offset[selectedDims1[1]], pos[1]);
                    }
                    // end crop data
                } else if (m_sizemode == SIZEMODES.EXTEND) {
                    // extend data
                    m_size[selectedDims1[0]] = Math.max(origsize[selectedDims1[0]] + pos[0], m_size[selectedDims1[0]]);
                    m_offset[selectedDims1[0]] = Math.min(m_offset[selectedDims1[0]], pos[0]);

                    m_size[selectedDims1[1]] = Math.max(origsize[selectedDims1[1]] + pos[1], m_size[selectedDims1[1]]);
                    m_offset[selectedDims1[1]] = Math.min(m_offset[selectedDims1[1]], pos[1]);
                    // end extend data
                }
            }
        }

        for (long t = tmin; t <= tmax; t++) {
            m_gxind[(int)(t - tmin)] = m_xind[(int)(t - tmin)];
            m_gyind[(int)(t - tmin)] = m_yind[(int)(t - tmin)];
        }

        if (m_sizemode == SIZEMODES.CROP) {
            // crop data
            // adjust size
            for (int i = 0; i < imgPlus.numDimensions(); i++) {
                m_size[i] -= m_offset[i];
            }
            // end crop data
        } else if (m_sizemode == SIZEMODES.EXTEND) {
            for (int i = 0; i < imgPlus.numDimensions(); i++) {
                m_size[i] -= m_offset[i];
            }
            // adjust the shift for extension (also for the first
            // image)
            for (long t = tmin; t <= tmax; t++) {
                m_xind[(int)(t - tmin)] = -(int)m_offset[selectedDims1[0]] - m_xind[(int)(t - tmin)];
                m_yind[(int)(t - tmin)] = -(int)m_offset[selectedDims1[1]] - m_yind[(int)(t - tmin)];
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Img<T> compute(final Img<T> srcIn, final Img<V> srcFiltered, final Img<T> res) {
        final Img<T> imgPlus = srcIn;
        final int[] selectedDims1 = m_selectedDims;
        final int[] selectedDims2 = new int[1];
        selectedDims2[0] = m_alignDim;

        Img<T> res2;
        if ((m_sizemode == SIZEMODES.CROP) || (m_sizemode == SIZEMODES.NOTHING)) {
            res2 = srcIn.copy();
        } else {
            final long[] pos = new long[srcIn.numDimensions()];
            res2 = res.copy();
            final Cursor<T> c = srcIn.localizingCursor();
            final RandomAccess<T> ra = res2.randomAccess();
            while (c.hasNext()) {
                c.fwd();
                c.localize(pos);
                ra.setPosition(pos);
                final double v = c.get().getRealDouble();
                ra.get().setReal(v);
            }
        }

        // this is the dimension along which the alignment is done
        final int selectedDim2 = selectedDims2[0];

        final long tmin = imgPlus.min(selectedDim2);
        final long tmax = imgPlus.max(selectedDim2);

        final long[] ipmin = new long[imgPlus.numDimensions()];
        final long[] ipmax = new long[imgPlus.numDimensions()];
        res2.min(ipmin);
        res2.max(ipmax);
        final long[] spmin = new long[imgPlus.numDimensions()];
        final long[] spmax = new long[imgPlus.numDimensions()];
        res2.min(spmin);
        res2.max(spmax);

        final int[] allDims = new int[imgPlus.numDimensions()];
        for (int i = 0; i < imgPlus.numDimensions(); i++) {
            allDims[i] = i;
        }
        final int[] remainingDims = new int[imgPlus.numDimensions() - selectedDims1.length - selectedDims2.length];
        for (int i = 0; i < selectedDims1.length; i++) {
            allDims[selectedDims1[i]] = -1;
        }
        for (int i = 0; i < selectedDims2.length; i++) {
            allDims[selectedDims2[i]] = -1;
        }
        int j = 0;
        for (int i = 0; i < imgPlus.numDimensions(); i++) {
            if (allDims[i] > -1) {
                remainingDims[j] = i;
                j++;
            }
        }

        long tstart = tmin + 1;
        if ((m_sizemode == SIZEMODES.EXTEND) || (m_alignmode == ALIGNMODES.LAST)) {
            // also adjust the first image if extending
            tstart = tmin;
        }
        long tend = tmax;
        if ((m_alignmode == ALIGNMODES.LAST) && (!(m_sizemode == SIZEMODES.EXTEND))) {
            tend = tmax - 1;
        }
        // TODO: maybe something else here
        // set the remaining dimensions to their minimum value.
        for (int i = 0; i < remainingDims.length; i++) {
            ipmin[remainingDims[i]] = spmin[remainingDims[i]];
            ipmax[remainingDims[i]] = spmin[remainingDims[i]];
        }
        if (remainingDims.length > 0) {
            alignRemainingDims((int)tstart, (int)tend, res2, selectedDims1, selectedDim2, remainingDims, (int)tmin,
                               (int)tmax, ipmin, ipmax, spmin, spmax);
        } else {

            alignPlane((int)tstart, (int)tend, res2, selectedDims1, selectedDim2, remainingDims, (int)tmin, (int)tmax,
                       ipmin, ipmax, spmin, spmax);
        }

        if (m_sizemode == SIZEMODES.CROP) {
            // Crop the resulting image...
            final ImgPlus<T> iplusres = new ImgPlus<T>(res);
            final ImgPlus<T> iplus = new ImgPlus<T>(res2);
            new ImgPlusCrop<T>(m_offset, m_size).compute(iplus, iplusres);
        } else {
            new ImgCopyOperation<T>().compute(res2, res);
        }
        return res;
    }

    void alignRemainingDims(final int tstart, final int tend, final Img<T> res2, final int[] selectedDims1,
                            final int selectedDim2, final int[] remainingDims, final int tmin, final int tmax,
                            final long[] ipmin, final long[] ipmax, final long[] spmin, final long[] spmax) {
        if (remainingDims.length == 1) {
            for (int rdplane = (int)spmin[remainingDims[0]]; rdplane <= spmax[remainingDims[0]]; rdplane++) {
                ipmin[remainingDims[0]] = rdplane;
                ipmax[remainingDims[0]] = rdplane;
                alignPlane(tstart, tend, res2, selectedDims1, selectedDim2, remainingDims, tmin, tmax, ipmin, ipmax,
                           spmin, spmax);
            }
        } else {
            for (int rdplane = (int)spmin[remainingDims[0]]; rdplane <= spmax[remainingDims[0]]; rdplane++) {
                ipmin[remainingDims[0]] = rdplane;
                ipmax[remainingDims[0]] = rdplane;
                alignRemainingDims(tstart, tend, res2, selectedDims1, selectedDim2,
                                   Arrays.copyOfRange(remainingDims, 1, remainingDims.length), tmin, tmax, ipmin,
                                   ipmax, spmin, spmax);
            }
        }
    }

    void alignPlane(final int tstart, final int tend, final Img<T> res2, final int[] selectedDims1,
                    final int selectedDim2, final int[] remainingDims, final int tmin, final int tmax,
                    final long[] ipmin, final long[] ipmax, final long[] spmin, final long[] spmax) {

        // now: align the planes
        for (long t = tstart; t <= tend; t++) {
            ipmin[selectedDim2] = t;
            ipmax[selectedDim2] = t;

            final Interval i = new FinalInterval(ipmin, ipmax);
            final ImgView<T> si = new ImgView<T>(SubsetOperations.subsetview(res2, i), res2.factory());
            final RandomAccess<T> c = si.randomAccess();
            final int[] pos = new int[2];
            pos[0] = (int)spmin[selectedDims1[0]];
            pos[1] = (int)spmin[selectedDims1[1]];
            c.setPosition(pos);
            if (m_sizemode == SIZEMODES.EXTEND) {
                for (int x = (int)spmax[selectedDims1[0]]; x >= (int)spmin[selectedDims1[0]]; x--) {
                    for (int y = (int)spmax[selectedDims1[1]]; y >= (int)spmin[selectedDims1[1]]; y--) {
                        if (((x - m_xind[(int)(t - tmin)]) >= spmin[selectedDims1[0]])
                                && ((x - m_xind[(int)(t - tmin)]) <= spmax[selectedDims1[0]])
                                && ((y - m_yind[(int)(t - tmin)]) >= spmin[selectedDims1[1]])
                                && ((y - m_yind[(int)(t - tmin)]) <= spmax[selectedDims1[1]])) {
                            c.setPosition(x - m_xind[(int)(t - tmin)], 0);
                            c.setPosition(y - m_yind[(int)(t - tmin)], 1);
                            final double v = c.get().getRealDouble();
                            c.setPosition(x, 0);
                            c.setPosition(y, 1);
                            c.get().setReal(v);
                        } else {
                            c.setPosition(x, 0);
                            c.setPosition(y, 1);
                            c.get().setReal(0);
                        }
                    }
                }
            } else {
                if ((m_gxind[(int)(t - tmin)] >= 0) && (m_gyind[(int)(t - tmin)] >= 0)) { // move
                    // left
                    // and
                    // up
                    for (int x = (int)spmin[selectedDims1[0]]; x <= (int)spmax[selectedDims1[0]]; x++) {
                        for (int y = (int)spmin[selectedDims1[1]]; y <= (int)spmax[selectedDims1[1]]; y++) {
                            if (((x + m_xind[(int)(t - tmin)]) <= spmax[selectedDims1[0]])
                                    && ((y + m_yind[(int)(t - tmin)]) <= spmax[selectedDims1[1]])) {
                                c.setPosition(x + m_xind[(int)(t - tmin)], 0);
                                c.setPosition(y + m_yind[(int)(t - tmin)], 1);
                                final double v = c.get().getRealDouble();
                                c.setPosition(x, 0);
                                c.setPosition(y, 1);
                                c.get().setReal(v);
                            } else {
                                c.setPosition(x, 0);
                                c.setPosition(y, 1);
                                c.get().setReal(0);
                            }
                        }
                    }
                } else {
                    if ((m_gxind[(int)(t - tmin)] < 0) && (m_gyind[(int)(t - tmin)] >= 0)) { // move
                        // right
                        // and
                        // up
                        for (int x = (int)spmax[selectedDims1[0]]; x >= (int)spmin[selectedDims1[0]]; x--) {
                            for (int y = (int)spmin[selectedDims1[1]]; y <= (int)spmax[selectedDims1[1]]; y++) {
                                if (((x + m_xind[(int)(t - tmin)]) >= spmin[selectedDims1[0]])
                                        && ((y + m_yind[(int)(t - tmin)]) <= spmax[selectedDims1[1]])) {
                                    c.setPosition(x + m_xind[(int)(t - tmin)], 0);
                                    c.setPosition(y + m_yind[(int)(t - tmin)], 1);
                                    final double v = c.get().getRealDouble();
                                    c.setPosition(x, 0);
                                    c.setPosition(y, 1);
                                    c.get().setReal(v);
                                } else {
                                    c.setPosition(x, 0);
                                    c.setPosition(y, 1);
                                    c.get().setReal(0);
                                }
                            }
                        }
                    } else {
                        if ((m_gxind[(int)(t - tmin)] >= 0) && (m_gyind[(int)(t - tmin)] < 0)) { // move
                            // left
                            // and
                            // down
                            for (int x = (int)spmin[selectedDims1[0]]; x <= (int)spmax[selectedDims1[0]]; x++) {
                                for (int y = (int)spmax[selectedDims1[1]]; y >= (int)spmin[selectedDims1[1]]; y--) {
                                    if (((x + m_xind[(int)(t - tmin)]) <= spmax[selectedDims1[0]])
                                            && ((y + m_yind[(int)(t - tmin)]) >= spmin[selectedDims1[1]])) {
                                        c.setPosition(x + m_xind[(int)(t - tmin)], 0);
                                        c.setPosition(y + m_yind[(int)(t - tmin)], 1);
                                        final double v = c.get().getRealDouble();
                                        c.setPosition(x, 0);
                                        c.setPosition(y, 1);
                                        c.get().setReal(v);
                                    } else {
                                        c.setPosition(x, 0);
                                        c.setPosition(y, 1);
                                        c.get().setReal(0);
                                    }
                                }
                            }
                        } else { // xind[(int)(t-tmin)]<0
                            // &&
                            // yind[(int)(t-tmin)]<0
                            // // move right and
                            // down
                            for (int x = (int)spmax[selectedDims1[0]]; x >= (int)spmin[selectedDims1[0]]; x--) {
                                for (int y = (int)spmax[selectedDims1[1]]; y >= (int)spmin[selectedDims1[1]]; y--) {
                                    if (((x + m_xind[(int)(t - tmin)]) >= spmin[selectedDims1[0]])
                                            && ((y + m_yind[(int)(t - tmin)]) >= spmin[selectedDims1[1]])) {
                                        c.setPosition(x + m_xind[(int)(t - tmin)], 0);
                                        c.setPosition(y + m_yind[(int)(t - tmin)], 1);
                                        final double v = c.get().getRealDouble();
                                        c.setPosition(x, 0);
                                        c.setPosition(y, 1);
                                        c.get().setReal(v);
                                    } else {
                                        c.setPosition(x, 0);
                                        c.setPosition(y, 1);
                                        c.get().setReal(0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @param dims
     */
    public Img<T> createType(final Img<T> img, final Img<V> srcFiltered, final long[] dims) {
        final ImgFactory<T> fac = img.factory();
        return fac.create(dims, img.firstElement().createVariable());
    }

    @Override
    public BinaryOutputOperation<Img<T>, Img<V>, Img<T>> copy() {
        return new Aligner<T, V>(m_selectedDims, m_alignDim, m_iv, m_sizemode, m_alignmode, m_stepsize, m_minPixOverlap);
    }

    @Override
    public BinaryObjectFactory<Img<T>, Img<V>, Img<T>> bufferFactory() {
        return new BinaryObjectFactory<Img<T>, Img<V>, Img<T>>() {

            @Override
            public Img<T> instantiate(final Img<T> inputA, final Img<V> inputB) {
                return createType(inputA, inputB);
            }
        };
    }

}
