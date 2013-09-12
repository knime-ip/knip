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
package org.knime.knip.core.data.graphtheory;

/**
 * Class to represent pixel coherence as directed edges in the graph.
 * 
 * This implementation was <b>heavily</b> inspired by the implementation provided by Kolmogorov and Boykov: MAXFLOW
 * version 3.01.
 * 
 * From the README of the library:
 * 
 * This software library implements the maxflow algorithm described in
 * 
 * "An Experimental Comparison of Min-Cut/Max-Flow Algorithms for Energy Minimization in Vision." Yuri Boykov and
 * Vladimir Kolmogorov. In IEEE Transactions on Pattern Analysis and Machine Intelligence (PAMI), September 2004
 * 
 * This algorithm was developed by Yuri Boykov and Vladimir Kolmogorov at Siemens Corporate Research. To make it
 * available for public use, it was later reimplemented by Vladimir Kolmogorov based on open publications.
 * 
 * If you use this software for research purposes, you should cite the aforementioned paper in any resulting
 * publication.
 * 
 * @version 0.1
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Jan Funke <jan.funke@inf.tu-dresden.de>
 */
public class Edge {

    // node the edge points to
    private Node m_head;

    // nect edge with the same originating node
    private Edge m_next;

    // reverse arc
    private Edge m_sister;

    // residual capacity of this edge
    private float m_residualCapacity;

    // special edges
    public final static Edge TERMINAL = new Edge();

    public final static Edge ORPHAN = new Edge();

    public Edge() {

        m_head = null;
        m_next = null;
        m_sister = null;

        m_residualCapacity = 0;
    }

    /**
     * Gets the head, i.e., the node this edge points to for this edge.
     * 
     * @return The head, i.e., the node this edge points to.
     */
    public Node getHead() {
        return this.m_head;
    }

    /**
     * Sets the head for this edge.
     * 
     * @param head The head, i.e., the node this edge shall point to.
     */
    public void setHead(final Node head) {
        this.m_head = head;
    }

    /**
     * Sets the next edge for this edge.
     * 
     * @param next The next edge.
     */
    public void setNext(final Edge next) {
        this.m_next = next;
    }

    /**
     * Sets the sister for this instance.
     * 
     * @param sister The sister.
     */
    public void setSister(final Edge sister) {
        this.m_sister = sister;
    }

    /**
     * Sets the residual capacity for this instance.
     * 
     * @param residualCapacity The residual capacity.
     */
    public void setResidualCapacity(final float residualCapacity) {
        this.m_residualCapacity = residualCapacity;
    }

    /**
     * Gets the residual capacity for this instance.
     * 
     * @return The residualCapacity.
     */
    public float getResidualCapacity() {
        return this.m_residualCapacity;
    }

    /**
     * Gets the sister for this instance.
     * 
     * @return The sister.
     */
    public Edge getSister() {
        return this.m_sister;
    }

    /**
     * Gets the next for this instance.
     * 
     * @return The next.
     */
    public Edge getNext() {
        return this.m_next;
    }
}
