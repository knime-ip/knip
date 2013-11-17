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

import java.util.HashSet;
import java.util.Iterator;

/**
 * UltraMetricContourMap Edge
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 *
 */
public class UCMEdge implements Comparable<UCMEdge> {
    // connected Faces, maximal 2
    private HashSet<UCMFace> m_faces;

    // weight of the Edge
    private double m_weight;

    // pixels forming the edge
    private HashSet<int[]> m_pixels;

    /**
     * Constructor
     */
    public UCMEdge() {
        m_faces = new HashSet<UCMFace>();
        m_weight = -1;
        m_pixels = new HashSet<int[]>();
    }

    /**
     * (obsolete?)
     *
     * @param faces
     * @param weight
     * @param pixels
     */
    // UCMEdge(HashSet<UCMFace> faces, HashSet<int[]> pixels, double weight) {
    // m_faces = faces;
    // m_weight = weight;
    // m_pixels = pixels;
    // }

    /**
     * returns the requested UCMFace, or null (if not a neighbor) - rather slow!
     *
     * @param label
     * @return UCM for this label
     */
    public UCMFace hasUCMFace(final int label) {
        Iterator<UCMFace> m_iterator = m_faces.iterator();
        UCMFace tempFace;
        while (m_iterator.hasNext()) {
            tempFace = m_iterator.next();
            if (tempFace.getLabel().equals(label)) {
                return tempFace;
            }
        }
        return null;
    }

    /**
     * adds a Pixel with its weight
     *
     * @param newPixel
     * @param weight
     */
    public void addPixel(final int[] newPixel, final double weight) {
        m_pixels.add(newPixel);
        // int size = m_pixels.size();
        m_weight += weight;
        // m_weight = (size / (size + 1)) * m_weight + weight / (size + 1);
    }

    /**
     *
     * @return returns the weight of the edge
     */
    public double getWeight() {
        return m_weight / m_pixels.size();
    }

    /**
     *
     * @return faces of the Edge
     */
    public HashSet<UCMFace> getFaces() {
        return m_faces;

    }

    /**
     *
     * @return returns the position of the points forming the edge
     */
    public HashSet<int[]> getPixels() {
        return m_pixels;
    }

    /**
     * adds pixel and weight of an edge
     *
     * @param edge
     */
    public void mergeEdge(final UCMEdge edge) {
        m_pixels.addAll(edge.m_pixels);
        // int sizeA = m_pixels.size();
        // int sizeB = edge.m_pixels.size();
        // int sum = sizeA + sizeB;
        m_weight += edge.m_weight;

        // m_weight = (sizeA/sum)*this.m_weight + (sizeB/sum)*edge.m_weight;
    }

    @Override
    public int compareTo(final UCMEdge other) {
        return Double.compare(this.getWeight(), other.getWeight());
    }
}
