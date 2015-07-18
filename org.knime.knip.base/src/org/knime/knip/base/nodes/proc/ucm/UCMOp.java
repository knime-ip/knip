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
 * Created on 28.11.2013 by Tim-Oliver Buchholz
 */
package org.knime.knip.base.nodes.proc.ucm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.BinaryOperation;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.AbstractRegionGrowing;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import org.knime.knip.core.KNIPGateway;

/**
 * Operation encapsulating functionality of UltraMetricContourMaps UCMs are a extraction system that combines several
 * types of low-level image information into a generic notion of segmentation scale. This system constructs a
 * hierarchical representation of the image boundaries called Ultrametric Contour Map (UCM). Thresholding an UCM at
 * level k provides by definition a set of closed curves, the boundaries of the segmentation at scale k (from
 * http://www.cs.berkeley.edu/~arbelaez/UCM.html).
 *
 * @author Jan Dirk Verbeek (University of Konstanz)
 * @author Martin Horn (University of Konstanz)
 * @author Tim-Oliver Buchholz (University of Konstanz)
 * @author Christian Dietz (University of Konstanz)
 *
 * @param <L>
 * @param <T>
 */
public class UCMOp<L extends Comparable<L>, T extends RealType<T>>
        implements
        BinaryOperation<RandomAccessibleInterval<LabelingType<L>>, RandomAccessibleInterval<T>, RandomAccessibleInterval<FloatType>> {

    private final int maxNumFaces;

    private final double maxFacePercent;

    private final int minBoundaryWeight;

    private final String boundaryLabel;

    /**
     * Default Constructor
     *
     * TODO
     *
     * @param paramMaxNumFaces
     * @param paramMaxFacePercent
     * @param paramMinBoundaryWeight
     * @param paramBoundaryLabel name of label which is boundary between some other Labels
     */
    public UCMOp(final int paramMaxNumFaces, final double paramMaxFacePercent, final int paramMinBoundaryWeight,
                 final String paramBoundaryLabel) {
        this.maxNumFaces = paramMaxNumFaces;
        this.maxFacePercent = paramMaxFacePercent;
        this.minBoundaryWeight = paramMinBoundaryWeight;
        this.boundaryLabel = paramBoundaryLabel;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RandomAccessibleInterval<FloatType> compute(final RandomAccessibleInterval<LabelingType<L>> labeling,
                                                       final RandomAccessibleInterval<T> inImg,
                                                       final RandomAccessibleInterval<FloatType> result) {

        // check for dimensions
        if (labeling.numDimensions() != inImg.numDimensions() && inImg.numDimensions() != result.numDimensions()) {
            throw new IllegalArgumentException("Dimensions do not match.");
        }

        // check for dimensions
        if (labeling.numDimensions() != 2) {
            throw new IllegalArgumentException("Only two dimension are supported.");
        }

        // Create containers for faces and boundaries
        final HashMap<String, UCMFace> faces = new HashMap<String, UCMFace>();
        final ArrayList<UCMBoundary> boundaries = new ArrayList<UCMBoundary>();

        // access on img
        final RandomAccess<FloatType> resultAccess = result.randomAccess();
        final RandomAccess<T> imgAccess = inImg.randomAccess();

        // | Create Faces
        final LabelRegions<L> regions = KNIPGateway.regions().regions(labeling);
        for (L label : regions.getExistingLabels()) {
            faces.put(label.toString(), new UCMFace(label.toString()));
        }

        // random access cursor with extended borders
        final Cursor<LabelingType<L>> labCur = Views.iterable(labeling).localizingCursor();

        final LabelingType<L> empty = Util.getTypeFromInterval(labeling).createVariable();
        empty.clear();
        final RandomAccess<LabelingType<L>> labAccess = Views.extendValue(labeling, empty).randomAccess();

        // the 8 neighbors
        final long[][] strucElement = AbstractRegionGrowing.get8ConStructuringElement(labeling.numDimensions());

        // the 16 neighbors
        final long[][] ring16 = ring16dim2();
        if (labeling.numDimensions() != 2) {
            posRing(ring16, labeling.numDimensions(), 2);
        }

        // temporary list of labels of neighboring faces
        HashSet<String> tempLabels = null;

        // for all pixels
        while (labCur.hasNext()) {
            labCur.fwd();
            // if pixel is part of a boundary
            if (labCur.get().iterator().next().toString().equals(boundaryLabel)) {
                tempLabels = new HashSet<String>();
                // iterate neighborhood to collect the faces
                for (int s = 0; s < strucElement.length; s++) {
                    for (int d = 0; d < labeling.numDimensions(); d++) {
                        // the neighboring pixel
                        labAccess.setPosition(labCur.getLongPosition(d) + strucElement[s][d], d);
                        // if not outside the image
                        if (labAccess.get().size() != 0) {
                            L label = labAccess.get().iterator().next();
                            if (!label.toString().equals(boundaryLabel)) {
                                // add face as neighbor
                                tempLabels.add(label.toString());
                            }
                        }
                    }
                }

                // special case: not two faces in direct neighborhood
                if (tempLabels.size() < 2) {
                    for (int s = 0; s < ring16.length; s++) {
                        for (int d = 0; d < labeling.numDimensions(); d++) {
                            // the neighboring pixel
                            try {
                                labAccess.setPosition(labCur.getLongPosition(d) + ring16[s][d], d);
                            } catch (Exception e) {
                                // extremly rare, but ugly!
                                continue;
                            }
                            // if not outside the image
                            if (labAccess.get().size() != 0) {
                                String label = labAccess.get().iterator().next().toString();
                                if (!label.toString().equals(boundaryLabel)) {
                                    // add face as neighbor
                                    tempLabels.add(label);
                                }
                            }
                        }
                    }

                }

                // add pixel to boundary(ies)
                HashMap<String, UCMBoundary> tempBoundaryMap = null;
                UCMBoundary tempBoundary = null;
                for (String firstLabel : tempLabels) {
                    tempBoundaryMap = faces.get(firstLabel).getBoundaries();
                    for (String secondLabel : tempLabels) {
                        if (firstLabel.compareTo(secondLabel) >= 0) {
                            continue;
                        }
                        int[] pos = new int[labCur.numDimensions()];
                        labCur.localize(pos);
                        // check if boundary exists
                        tempBoundary = tempBoundaryMap.get(secondLabel);
                        if (tempBoundary == null) {
                            UCMFace faceA = faces.get(firstLabel);
                            UCMFace faceB = faces.get(secondLabel);
                            // create the boundary
                            tempBoundary = new UCMBoundary();
                            tempBoundary.getFaces().add(faceA);
                            tempBoundary.getFaces().add(faceB);
                            boundaries.add(tempBoundary);
                            // add to faces
                            faceA.addBoundary(secondLabel, tempBoundary);
                            faceB.addBoundary(firstLabel, tempBoundary);
                        }

                        try {
                            imgAccess.setPosition(pos);
                            tempBoundary.addPixel(pos, imgAccess.get().getRealDouble());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        // | sort according to boundary weights
        Collections.sort(boundaries);

        final double maxFacesSize = faces.size();
        double facesWished = maxNumFaces;
        if (facesWished < 1) {
            facesWished = 1;
        }

        double calcedMaxFacePercent = maxFacePercent / 100;
        if (calcedMaxFacePercent < 0.01) {
            calcedMaxFacePercent = 0.01;
        }
        boolean drawAllowed = false;
        double tempDouble = 0;

        // | Merge regions & draw boundaries
        while (faces.size() > 1 && boundaries.size() > 0) {
            // get the next boundary
            UCMBoundary boundary = boundaries.get(0);
            tempDouble = boundary.getWeight();

            // merge neighboring regions
            mergeFaces(boundary, faces, boundaries);

            // check draw conditions
            if (!drawAllowed) {
                if (faces.size() <= facesWished && tempDouble >= minBoundaryWeight
                        && faces.size() / maxFacesSize <= calcedMaxFacePercent) {
                    drawAllowed = true;
                }
            }

            // draw boundary
            if (drawAllowed) {
                for (int[] pos : boundary.getPixels()) {
                    resultAccess.setPosition(pos);
                    resultAccess.get().setReal(tempDouble);
                }
            }

        }

        return result;
    }

    /**
     * Ring around a Pixel with distance 2 in a two-dimensional space
     *
     * @return list of positions
     */
    private long[][] ring16dim2() {

        final long[][] faul = new long[16][2];
        // 00000
        // 0xxx0
        // 0xPx0
        // 0xxx0
        // 00000
        faul[0][0] = -2;
        faul[0][1] = -2;
        faul[1][0] = -2;
        faul[1][1] = -1;
        faul[2][0] = -2;
        faul[3][0] = -2;
        faul[3][1] = 1;
        faul[4][0] = -2;
        faul[4][1] = 2;
        // -
        faul[5][0] = -1;
        faul[5][1] = -2;
        faul[6][1] = -2;
        faul[7][0] = 1;
        faul[7][1] = -2;
        // -
        faul[8][0] = -1;
        faul[8][1] = 2;
        faul[9][1] = 2;
        faul[10][0] = 1;
        faul[10][1] = 2;
        // -
        faul[11][0] = 2;
        faul[11][1] = -2;
        faul[12][0] = 2;
        faul[12][1] = -1;
        faul[13][0] = 2;
        faul[14][0] = 2;
        faul[14][1] = 1;
        faul[15][0] = 2;
        faul[15][1] = 2;
        return faul;
    }

    /**
     * returns a long[][] containing all points which differ by a certain distance from a central point in one
     * dimension, but not more than it in any other dimension
     *
     * @param wantsPositions
     * @param dimensions
     * @param distance
     */
    private void posRing(long[][] wantsPositions, final int dimensions, final int distance) {

        int ringLength = (int)(Math.pow(distance * 2 + 1, dimensions) - Math.pow(distance * 2 - 1, dimensions));
        int posMaxi = (int)Math.pow(distance * 4 + 1, dimensions);
        long tempLong;
        wantsPositions = new long[ringLength][dimensions];
        final long[][] possiblePosition = new long[posMaxi][dimensions];
        final long[] numlock = new long[dimensions];

        // create core
        for (int i = 0; i < numlock.length; i++) {
            numlock[i] = -distance;
        }
        while (numlock[0] <= distance) {
            for (int dim = 0; dim < dimensions; dim++) {
                possiblePosition[posMaxi][dim] = numlock[dim];
            }
            posMaxi--;
            numlock[dimensions - 1] += 1;
            for (int dim = dimensions - 1; dim > 0; dim--) {
                if (numlock[dim] > distance) {
                    numlock[dim] = -distance;
                    numlock[dim - 1] += 1;
                }
            }
        }

        // collect diamond
        for (int i = 0; i < possiblePosition.length; i++) {
            tempLong = 0;
            for (int dim = 0; dim < dimensions; dim++) {
                tempLong += Math.abs(possiblePosition[i][dim]);
            }
            if (tempLong == distance * dimensions) {
                for (int dim = 0; dim < dimensions; dim++) {
                    wantsPositions[ringLength][dim] = possiblePosition[i][dim];
                }
                ringLength--;
            }
        }

        // crush to cube
        for (int i = 0; i < wantsPositions.length; i++) {
            for (int dim = 0; dim < dimensions; dim++) {
                tempLong = wantsPositions[i][dim];
                if (tempLong < -distance) {
                    wantsPositions[i][dim] = -distance;
                }
                if (tempLong > distance) {
                    wantsPositions[i][dim] = distance;
                }
            }
        }
    }

    /**
     * merges the two faces of an given boundary
     *
     * @param mergingBoundary
     * @param faces
     * @param boundaries
     * @return
     */
    private UCMFace mergeFaces(final UCMBoundary mergingBoundary, final HashMap<String, UCMFace> faces,
                               final ArrayList<UCMBoundary> boundaries) {
        // Faces
        UCMFace faceA = null;
        UCMFace faceB = null;
        UCMFace thirdFace;

        // Boundaries
        UCMBoundary manipulatedBoundary;
        UCMBoundary existingBoundary;
        // temporary Variables
        Iterator<UCMFace> tempFaceIterator;

        // get faces
        tempFaceIterator = mergingBoundary.getFaces().iterator();
        faceA = tempFaceIterator.next();
        faceB = tempFaceIterator.next();

        // decide which face stays
        if (faceA.getBoundaries().size() < faceB.getBoundaries().size()) {
            thirdFace = faceA;
            faceA = faceB;
            faceB = thirdFace;
        }

        // remove connecting boundary
        faceA.getBoundaries().remove(faceB.getLabel());
        faceB.getBoundaries().remove(faceA.getLabel());
        boundaries.remove(mergingBoundary);

        // visit every connected face and shift corresponding boundary
        Iterator<String> faceLabelIterator = faceB.getBoundaries().keySet().iterator();
        String conFaceLabel;
        while (faceLabelIterator.hasNext()) {
            // the label of the neighbor
            conFaceLabel = faceLabelIterator.next();
            // get connecting boundary
            manipulatedBoundary = faceB.getBoundaries().get(conFaceLabel);
            // get connected Face
            thirdFace = faces.get(conFaceLabel);

            // remove connection to vanishing face
            thirdFace.getBoundaries().remove(faceB.getLabel());

            // boundary to staying face?
            existingBoundary = thirdFace.getBoundaries().get(faceA.getLabel());
            if (existingBoundary == null) {

                // change faces of shifted boundary
                manipulatedBoundary.getFaces().remove(faceB);
                manipulatedBoundary.getFaces().add(faceA);

                // add boundary to faces
                faceA.getBoundaries().put(conFaceLabel, manipulatedBoundary);
                thirdFace.getBoundaries().put(faceA.getLabel(), manipulatedBoundary);

            } else {

                // remove from general list
                boundaries.remove(manipulatedBoundary);

                // merge into existing boundary
                existingBoundary.mergeBoundary(manipulatedBoundary);
                Collections.sort(boundaries);

            }
        }
        // remove swallowed face
        faces.remove(faceB.getLabel());

        return faceA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
            BinaryOperation<RandomAccessibleInterval<LabelingType<L>>, RandomAccessibleInterval<T>, RandomAccessibleInterval<FloatType>>
            copy() {
        return new UCMOp<L, T>(maxNumFaces, maxFacePercent, minBoundaryWeight, boundaryLabel);
    }
}
