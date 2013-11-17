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
 */
package org.knime.knip.base.nodes.proc.ucm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.AbstractRegionGrowing;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;

/**
 * NodeModel for UltraMetric ContourMaps
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 *
 * @param <T>
 * @param <L>
 */
public class UCMNodeModel<T extends RealType<T>, L extends Comparable<L>> extends
        TwoValuesToCellNodeModel<LabelingValue<L>, ImgPlusValue<T>, ImgPlusCell<FloatType>> {

    static SettingsModelInteger createMaxFacesAmountModel() {
        return new SettingsModelInteger("max_faces_num", 9999);
    }

    static SettingsModelInteger createMaxFacePercentModel() {
        return new SettingsModelInteger("max_face_percent", 100);
    }

    static SettingsModelInteger createMinEdgeWeightModel() {
        return new SettingsModelInteger("min_edge_weight", 0);
    }

    static SettingsModelString createBoundaryLabelModel() {
        return new SettingsModelString("boundary_label", "Boundary");
    }

    private SettingsModelInteger m_maxNumFaces = createMaxFacesAmountModel();

    private SettingsModelInteger m_maxFacePercent = createMaxFacePercentModel();

    private SettingsModelInteger m_minEdgeWeight = createMinEdgeWeightModel();

    private SettingsModelString m_boundaryLabel = createBoundaryLabelModel();

    private ImgPlusCellFactory m_imgCellFactory;

    // all Edges, by weight
    List<UCMEdge> m_edges = new ArrayList<UCMEdge>();

    List<UCMEdge> m_resultEdges = new ArrayList<UCMEdge>();

    // all Faces
    HashMap<String, UCMFace> m_Faces = new HashMap<String, UCMFace>();

    // short-time integer
    int tempInt;

    @Override
    protected void prepareExecute(final ExecutionContext exec) {
        m_imgCellFactory = new ImgPlusCellFactory(exec);
    }

    @Override
    protected ImgPlusCell<FloatType> compute(final LabelingValue<L> cellValue, final ImgPlusValue<T> img)
            throws Exception {
        // resulting picture
        Img<FloatType> result = new ArrayImgFactory<FloatType>().create(cellValue.getLabeling(), new FloatType());

        RandomAccess<FloatType> resultAccess = result.randomAccess();

        RandomAccess<T> imgAccess = img.getImgPlus().randomAccess();

        // | Create Faces
        m_Faces.clear();
        for (L label : cellValue.getLabeling().getLabels()) {
            m_Faces.put(label.toString(), new UCMFace(label.toString()));
        }

        // | Create Edges
        m_edges.clear();

        // labeling of the picture
        Labeling<L> lab = cellValue.getLabeling();
        // random access cursor with extended borders
        Cursor<LabelingType<L>> labCur = lab.localizingCursor();
        RandomAccess<LabelingType<L>> labAccess = Views.extendValue(lab, null).randomAccess();

        // the 8 neighbors
        long[][] strucElement = AbstractRegionGrowing.get8ConStructuringElement(lab.numDimensions());

        // the 16 neighbors
        long[][] ring16 = ring16dim2();
        if (lab.numDimensions() != 2) {
            posRing(ring16, lab.numDimensions(), 2);
        }

        // temporary list of labels of neighboring faces
        HashSet<String> tempLabels = null;

        // for all pixels
        while (labCur.hasNext()) {
            labCur.fwd();
            // if pixel is part of a boundary
            if (labCur.get().getLabeling().get(0).toString().equals(m_boundaryLabel.getStringValue())) {
                tempLabels = new HashSet<String>();
                // iterate neighborhood to collect the faces
                for (int s = 0; s < strucElement.length; s++) {
                    for (int d = 0; d < lab.numDimensions(); d++) {
                        // the neighboring pixel
                        labAccess.setPosition(labCur.getLongPosition(d) + strucElement[s][d], d);
                        // if not outside the image
                        if (labAccess.get() != null) {
                            L label = labAccess.get().getLabeling().get(0);
                            if (!label.toString().equals(m_boundaryLabel.getStringValue())) {
                                // add face as neighbor
                                tempLabels.add(label.toString());
                            }
                        }
                    }
                }

                // special case: not two faces in direct neighborhood
                if (tempLabels.size() < 2) {
                    for (int s = 0; s < ring16.length; s++) {
                        for (int d = 0; d < lab.numDimensions(); d++) {
                            // the neighboring pixel
                            try {
                                labAccess.setPosition(labCur.getLongPosition(d) + ring16[s][d], d);
                            } catch (Exception e) {
                                // extremly rare, but ugly!
                                continue;
                            }
                            // if not outside the image
                            if (labAccess.get() != null) {
                                String label = labAccess.get().getLabeling().get(0).toString();
                                if (!label.toString().equals(m_boundaryLabel.getStringValue())) {
                                    // add face as neighbor
                                    tempLabels.add(label);
                                }
                            }
                        }
                    }

                }

                // add pixel to edge(s)
                HashMap<String, UCMEdge> tempEdgeMap = null;
                UCMEdge tempEdge = null;
                for (String firstLabel : tempLabels) {
                    tempEdgeMap = m_Faces.get(firstLabel).getEdges();
                    for (String secondLabel : tempLabels) {
                        if (firstLabel.compareTo(secondLabel) >= 0) {
                            continue;
                        }
                        int[] pos = new int[labCur.numDimensions()];
                        labCur.localize(pos);
                        // check if Edge exists
                        tempEdge = tempEdgeMap.get(secondLabel);
                        if (tempEdge == null) {
                            UCMFace faceA = m_Faces.get(firstLabel);
                            UCMFace faceB = m_Faces.get(secondLabel);
                            // create the edge
                            tempEdge = new UCMEdge();
                            tempEdge.getFaces().add(faceA);
                            tempEdge.getFaces().add(faceB);
                            m_edges.add(tempEdge);
                            // add to faces
                            faceA.addEdge(secondLabel, tempEdge);
                            faceB.addEdge(firstLabel, tempEdge);
                        }

                        try {
                            imgAccess.setPosition(pos);
                            tempEdge.addPixel(pos, imgAccess.get().getRealDouble());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        // | sort according to edgeweights
        Collections.sort(m_edges);

        double maxM_FacesSize = m_Faces.size();
        double facesWished = m_maxNumFaces.getIntValue();
        if (facesWished < 1) {
            facesWished = 1;
        }
        double maxFacePercent = ((double)m_maxFacePercent.getIntValue()) / 100;
        if (maxFacePercent < 0.01) {
            maxFacePercent = 0.01;
        }
        double minEdgeWeight = m_minEdgeWeight.getIntValue();
        boolean drawAllowed = false;
        double tempDouble = 0;
        // double weightMax = Double.MIN_VALUE;
        // double weightMin = Double.MAX_VALUE;

        // | Merge regions & draw edges
        while (m_Faces.size() > 1 && m_edges.size() > 0) {
            // get the next Edge
            UCMEdge c_edge = m_edges.get(0);

            // System.out.print("Faces: "+m_Faces.size()+" Edges: "+
            // m_edges.size() + " ");
            // testEFs(c_edge);

            tempDouble = c_edge.getWeight();
            // merge neighboring regions

            mergeFaces(c_edge);

            // check draw conditions
            if (!drawAllowed) {
                if (m_Faces.size() <= facesWished && tempDouble >= minEdgeWeight
                        && m_Faces.size() / maxM_FacesSize <= maxFacePercent) {
                    drawAllowed = true;
                }
            }

            // draw edge
            if (drawAllowed) {
                for (int[] pos : c_edge.getPixels()) {
                    resultAccess.setPosition(pos);
                    resultAccess.get().setReal(tempDouble);
                }
            }

            // Alternative, mit Normierung
            // tempDouble = c_edge.getWeight();
            // if (tempDouble > weightMax)
            // weightMax = tempDouble;
            // if (tempDouble < weightMin)
            // weightMin = tempDouble;
            // m_resultEdges.add(c_edge);
        }
        // weightMax = weightMax - weightMin;
        // for (UCMEdge edge : m_resultEdges) {
        // if (edge.getWeight() == 0)
        // continue;
        // tempDouble = ((edge.getWeight() - weightMin) / weightMax)
        // * Integer.MAX_VALUE;
        // // System.out.println(edge.getWeight()+" "+tempDouble); //temp
        // for (int[] pos : edge.getPixels()) {
        // resultAccess.setPosition(pos);
        // resultAccess.get().setReal(tempDouble);
        // }
        // }

        return m_imgCellFactory.createCell(result, img.getImgPlus());
    }

    /**
     * merges the two faces of an given edge
     *
     * @param mergingEdge
     * @return
     */
    private UCMFace mergeFaces(final UCMEdge mergingEdge) {
        // Faces
        UCMFace faceA = null;
        UCMFace faceB = null;
        UCMFace thirdFace;
        // Edges
        UCMEdge manipulatedEdge;
        UCMEdge existingEdge;
        // temporary Variables
        Iterator<UCMFace> tempFaceIterator;

        // temp check
        if (mergingEdge.getFaces().size() != 2) {
            System.out.print("ugly: ");
            testEFs(mergingEdge);
        }

        // get faces
        tempFaceIterator = mergingEdge.getFaces().iterator();
        faceA = tempFaceIterator.next();
        faceB = tempFaceIterator.next();

        // decide which face stays
        if (faceA.getEdges().size() < faceB.getEdges().size()) {
            thirdFace = faceA;
            faceA = faceB;
            faceB = thirdFace;
        }

        // remove connecting edge
        faceA.getEdges().remove(faceB.getLabel());
        faceB.getEdges().remove(faceA.getLabel());
        m_edges.remove(mergingEdge);

        // visit every connected face and shift corresponding Edge
        Iterator<String> faceLabelIterator = faceB.getEdges().keySet().iterator();
        String conFaceLabel;
        while (faceLabelIterator.hasNext()) {
            // the label of the neighbor
            conFaceLabel = faceLabelIterator.next();
            // get connecting Edge
            manipulatedEdge = faceB.getEdges().get(conFaceLabel);
            // get connected Face
            thirdFace = m_Faces.get(conFaceLabel);

            // remove connection to vanishing face
            thirdFace.getEdges().remove(faceB.getLabel());

            // edge to staying face?
            existingEdge = thirdFace.getEdges().get(faceA.getLabel());
            if (existingEdge == null) {

                // change faces of shifted Edge
                manipulatedEdge.getFaces().remove(faceB);
                manipulatedEdge.getFaces().add(faceA);

                // add edge to faces
                faceA.getEdges().put(conFaceLabel, manipulatedEdge);
                thirdFace.getEdges().put(faceA.getLabel(), manipulatedEdge);

            } else {

                // remove from general list
                m_edges.remove(manipulatedEdge);

                // merge into existing edge
                existingEdge.mergeEdge(manipulatedEdge);
                Collections.sort(m_edges);

            }
        }
        // remove swallowed face
        m_Faces.remove(faceB.getLabel());

        return faceA;
    }

    /**
     * prints the labels of the faces of the given edge "System.out.prinln(edge.faces.label)"
     *
     * @param edge
     */
    // @SuppressWarnings("unused")
    private void testEFs(final UCMEdge edge) {
        System.out.print("[");
        Iterator<UCMFace> iterator = edge.getFaces().iterator();
        while (iterator.hasNext()) {
            System.out.print(iterator.next().getLabel() + "");
            if (iterator.hasNext()) {
                System.out.print("|");
            }
        }
        System.out.println("]");
    }

    /**
     * Ring around a Pixel with distance 2 in a two-dimensional space
     *
     * @return list of positions
     */
    public static long[][] ring16dim2() {

        long[][] faul = new long[16][2];
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
    public static void posRing(long[][] wantsPositions, final int dimensions, final int distance) {

        int ringLength = (int)(Math.pow(distance * 2 + 1, dimensions) - Math.pow(distance * 2 - 1, dimensions));
        int posMaxi = (int)Math.pow(distance * 4 + 1, dimensions);
        long tempLong;
        wantsPositions = new long[ringLength][dimensions];
        long[][] possiblePosition = new long[posMaxi][dimensions];
        long[] numlock = new long[dimensions];

        // create ore
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

    @Override
    protected void addSettingsModels(final List<SettingsModel> settingsModels) {
        settingsModels.add(m_maxNumFaces);
        settingsModels.add(m_maxFacePercent);
        settingsModels.add(m_minEdgeWeight);
        settingsModels.add(m_boundaryLabel);
    }
}
