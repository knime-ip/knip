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
package org.knime.knip.core.ops.interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.knip.core.util.NeighborhoodUtils;

/**
 * Operation to Compute local maxima within a given image. Maxima computation can be done in any dimensionality desired.
 * 
 * @author Tino Klingebiel, University of Konstanz
 */

public class MaximumFinder<T extends RealType<T>> implements
        UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<BitType>> {

    private final double m_noise;

    private final double m_suppression;

    private long[][] m_strucEl;

    private ArrayList<AnalyticPoint> pList;

    public MaximumFinder(final double noise, final double suppression) {
        m_noise = noise;
        m_suppression = suppression;
    }

    /**
     * {@inheritDoc}
     */
    public RandomAccessibleInterval<BitType> compute(final RandomAccessibleInterval<T> input,
                                                     final RandomAccessibleInterval<BitType> output) {

        pList = new ArrayList<AnalyticPoint>();

        Cursor<T> cur = Views.iterable(input).localizingCursor();
        RandomAccess<T> ra = input.randomAccess(input);

        long[] dimensions = new long[input.numDimensions()];
        input.dimensions(dimensions);
        long[] dim = new long[input.numDimensions()];

        for (int i = 0; i < dim.length; ++i) {
            dim[i] = dimensions[i];
        }

        m_strucEl =
                NeighborhoodUtils.reworkStructuringElement(NeighborhoodUtils.get8ConStructuringElement(input
                        .numDimensions()));

        while (cur.hasNext()) {
            cur.fwd();
            ra.setPosition(cur);
            boolean candidate = true;
            double act = ra.get().getRealDouble();

            for (int i = 0; i < m_strucEl.length; ++i) {
                long[] offset = m_strucEl[i];
                ra.move(offset);

                if (isWithin(ra, dim)) {
                    if (ra.get().getRealDouble() > act) {
                        /*
                         * A candidate has no higher intensity pixels in his
                         * neighborhood. When reaching this point, this
                         * condition dows not hold and this is no candidate.
                         */
                        candidate = false;
                        break;
                    }
                }
            }

            if (candidate) {
                int[] coordinates = new int[dimensions.length];
                for (int i = 0; i < coordinates.length; ++i) {
                    coordinates[i] = cur.getIntPosition(i);
                }
                AnalyticPoint p = new AnalyticPoint(coordinates, cur.get().getRealDouble());
                pList.add(p);
            } else {
            }
        }

        /*
         * Analyze the maxima candidates. Find out wich ones are real local maxima.
         */
        analyzeCandidates(input, dimensions, output);
        return output;
    }

    /*
     * ToDo: Get rid of the ImgPlus usage. Status Image Type should not be
     * anything higher than ByteType
     */
    protected void analyzeCandidates(final RandomAccessibleInterval<T> input, final long[] dimensions,
                                     final RandomAccessibleInterval<BitType> output) {

        Collections.sort(pList);
        ImgPlus<T> img = (ImgPlus<T>)input;

        /*
         * Status structure to avoid running over pixels more than once when
         * computing plateau centers.
         */
        ImgPlus<BitType> status =
                new ImgPlus<BitType>(((ImgPlus<BitType>)output).factory().create(dimensions,
                                                                                 ((ImgPlus<BitType>)output)
                                                                                         .firstElement()));

        /*
         * Status structure to mark all previous reached pixels. SInce we stark with the
         * highest intensity pixels we know, when reaching a pixel marked as processed,
         * that we can sip the current operation, because the initial candidate can
         * not be a local max, because it is connected to a previous computed local max
         * or a region connected to one. Since we can only reach point in noise tolerance
         * our candidat eis now real max.
         */
        ImgPlus<BitType> processed =
                new ImgPlus<BitType>(((ImgPlus<BitType>)output).factory().create(dimensions,
                                                                                 ((ImgPlus<BitType>)output)
                                                                                         .firstElement()));

        RandomAccess<T> ra = img.randomAccess();
        RandomAccess<T> act = img.randomAccess();
        RandomAccess<BitType> ra2 = output.randomAccess();
        RandomAccess<BitType> raStat = status.randomAccess();
        RandomAccess<BitType> raProc = processed.randomAccess();
        Iterator<AnalyticPoint> it = pList.iterator();

        while (it.hasNext()) {
            boolean error = false;
            AnalyticPoint p = it.next();
            raProc.setPosition(p.getPosition());
            raStat.setPosition(p.getPosition());

            /*
             * The actual candidate was reached by previous steps.
             * Thus it is eigther a member of a plateau or connected
             * to a real max and thus no real local max. Ignore it.
             */
            if (raStat.get().get() || raProc.get().get()) {
                continue;
            }

            act.setPosition(p.getPosition());
            ra.setPosition(p.getPosition());

            long[] myPos = new long[dimensions.length];

            for (int i = 0; i < dimensions.length; ++i) {
                myPos[i] = act.getIntPosition(i);
            }

            for (long[] offset : m_strucEl) {
                ra.move(offset);
                if (isWithin(ra, dimensions)) {
                    if (Math.abs(act.get().getRealDouble() - ra.get().getRealDouble()) <= m_noise) {

                        int[] mpos = new int[dimensions.length];
                        for (int i = 0; i < dimensions.length; ++i) {
                            mpos[i] = ra.getIntPosition(i);
                        }

                        long[] myPosi =
                                analyzeneighborhood(img, status, processed, act, dimensions, act.get().getRealDouble());

                        if (myPosi != null) {
                            long[] newPos = new long[dimensions.length];

                            for (int i = 0; i < dimensions.length; ++i) {
                                newPos[i] = myPosi[i] / myPosi[dimensions.length];
                            }
                            double mindist = 10000;
                            int minPos = pList.indexOf(p);
                            for (int k = 0; k < pList.size(); ++k) {
                                double d = 0;
                                for (int i = 0; i < newPos.length; ++i) {
                                    d +=
                                            ((double)(newPos[i] - pList.get(k).getPosition()[i]))
                                                    * ((double)(newPos[i] - pList.get(k).getPosition()[i]));
                                }
                                if (d <= mindist) {
                                    minPos = k;
                                    mindist = d;
                                }
                            }
                            pList.get(minPos).setMax(true);
                            raProc.setPosition(pList.get(minPos).getPosition());
                            raProc.get().set(true);
                        }
                        /*
                         * When we reach this point the initial candidate is
                         * most likely not the real local max within the region.
                         * Mark it, so that it will not be set as a local max.
                         * The real max is set 3 lines above and could still
                         * be the candidate. If so, there will be no error.
                         */
                        error = true;
                        break;
                    }
                }

            }
            if (error) {
                raProc.setPosition(p.getPosition());
                raProc.get().set(true);
            } else {
                /*
                 * We did not reach the neighborhoofmethod, so there is
                 * no connected point within 8-con-neighborhood in noise tolerance.
                 * Since a candidate has no higher intensity pixels around all neighbors
                 * must haveintensitys smaller than actual - noise.
                 * Thus we have a real local max and can mark.
                 */
                p.setMax(true);
                raProc.setPosition(p.getPosition());
                raProc.get().set(true);
            }
        }

        /* Remove every Point from the List that  is no max.
         * Useful for all later operations on the list.
         */
        if (pList instanceof ArrayList<?>) {
            ArrayList<AnalyticPoint> cpList = (ArrayList<AnalyticPoint>)pList.clone();
            for (AnalyticPoint p : cpList) {
                if (!p.isMax()) {
                    pList.remove(p);
                }
            }
        } else {
            //If we get here we have done s.th. terribly wrong
        }

        if (m_suppression > 0) {
            doSuppression();
        }

        /*
         * Mark all single maximum points.
         */
        for (int i = 0; i < pList.size(); ++i) {
            if (pList.get(i).isMax()) {
                ra2.setPosition(pList.get(i).getPosition());
                ra2.get().setReal(255);
            }
        }

    }

    /**
     * Suppression of Max Points within a given radius.
     */
    protected void doSuppression() {

        Collections.sort(pList);

        /*Build a distance Matrix (To avoid running
         * over everything more often than we need to
         */

        int size = pList.size();
        for (int i = 0; i < size; ++i) {
            for (int j = i + 1; j < size; ++j) {
                if (pList.get(i).distanceTo(pList.get(j)) < m_suppression) {
                    pList.remove(j);
                    size--;
                    j--;
                }
            }
        }
    }

    /**
     * Analyze the Neighbor candidates. Find Plateaus, compute Center.
     * 
     * @param img - The Input Image.
     * @param status - The 3 dimensional Status Structure.
     * @param status - The 3 dimensional processing Status Structure.
     * @param ra - The Random access (Position) within the original image.
     * @param dimensions - Dimensions of the Image.
     * @return null when the random access was not on a real maximum or the computed real max.
     */
    protected long[] analyzeneighborhood(final ImgPlus<T> img, final ImgPlus<BitType> status,
                                         final ImgPlus<BitType> processed, final RandomAccess<T> ra,
                                         final long[] dimensions, final double initialIntensity) {

        long[] retVal = new long[dimensions.length + 1];
        RandomAccess<BitType> raStat = status.randomAccess();
        RandomAccess<BitType> raProc = processed.randomAccess();
        HashSet<int[]> next = new HashSet<int[]>();
        HashSet<int[]> all = new HashSet<int[]>();
        int[] mpos = new int[dimensions.length];

        for (int i = 0; i < dimensions.length; ++i) {
            retVal[i] = ra.getIntPosition(i);
            mpos[i] = ra.getIntPosition(i);
        }

        retVal[dimensions.length] = 1;
        next.add(mpos);
        all.add(mpos);

        while (next.size() > 0) {
            HashSet<int[]> newNext = new HashSet<int[]>();
            for (int[] act : next) {
                ra.setPosition(act);
                raStat.setPosition(act);
                raProc.setPosition(act);

                /*
                 * We reached a previous analyzed region.
                 * So this point belongs either to a local
                 * max, or to a region, that is neighbor
                 * to a local max. Thus this point can not
                 * be a local max.
                 */
                if (raProc.get().get()) {
                    for (int[] ac : all) {
                        raStat.setPosition(ac);
                        raStat.get().set(false);
                        raProc.setPosition(ac);
                        raProc.get().set(true);
                    }
                    return null;
                }
                raStat.get().set(true);
                for (long[] offset : m_strucEl) {
                    ra.move(offset);
                    if (isWithin(ra, dimensions)) {

                        raStat.setPosition(ra);
                        raProc.setPosition(ra);

                        if (ra.get().getRealDouble() >= initialIntensity - m_noise) {
                            if (raStat.get().get()) {
                                continue;
                            } else {
                                raStat.get().set(true);
                            }
                            if (Math.abs(ra.get().getRealDouble() - initialIntensity) <= m_noise) {
                                mpos = new int[dimensions.length];
                                for (int i = 0; i < dimensions.length; ++i) {
                                    retVal[i] += ra.getIntPosition(i);
                                    mpos[i] = ra.getIntPosition(i);
                                }
                                retVal[dimensions.length]++;
                                if (!all.contains(mpos)) {
                                    newNext.add(mpos);
                                    all.add(mpos);
                                }
                            } else {
                                /*
                                 * The point is higher that the candidate + noise
                                 * and was reached by traversing all neighbors
                                 * to the candidate that are within noisetolerance
                                 * thus the candidate is no real max.
                                 */
                                for (int[] ac : all) {
                                    raStat.setPosition(ac);
                                    raStat.get().set(false);
                                    raProc.setPosition(ac);
                                    raProc.get().set(true);
                                }
                                return null;
                            }
                        }
                    }
                }
            }
            next = newNext;
        }
        for (int[] act : all) {
            /*
             * Computing done. Mark all pixels traversed (pixels within
             * noisetolerance of the candidate and connected to it) as
             * processed.
             * When reaching a previous processed pixel in future steps
             * we know, that the initial candidate is no local max.
             */
            raStat.setPosition(act);
            raStat.get().set(false);
            raProc.setPosition(act);
            raProc.get().set(true);
        }
        return retVal;
    }

    /**
     * Check if a given Position is within our bounds.
     * 
     * @param ra - Random access on the Position.
     * @param dimensions - The dimensions of our given view.
     * @return - true if the Position is within bounds, false otherwise.
     */
    protected boolean isWithin(final RandomAccess<T> ra, final long[] dimensions) {
        boolean noskip = true;

        for (int i = 0; i < dimensions.length; ++i) {
            if (ra.getIntPosition(i) >= dimensions[i] || ra.getIntPosition(i) < 0) {
                noskip = false;
            }
        }
        return noskip;
    }

    /**
     * {@inheritDoc}
     */
    public UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<BitType>> copy() {
        return new MaximumFinder<T>(m_noise, m_suppression);
    }

}
