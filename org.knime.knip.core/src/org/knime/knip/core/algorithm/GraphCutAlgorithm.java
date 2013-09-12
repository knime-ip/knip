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
package org.knime.knip.core.algorithm;

/**
 * Graph cut implementation for images.
 *
 * This implementation was <b>heavily</b> inspired by the implementation
 * provided by Kolmogorov and Boykov: MAXFLOW version 3.01.
 *
 * From the README of the library:
 *
 *	This software library implements the maxflow algorithm described in
 *
 *	"An Experimental Comparison of Min-Cut/Max-Flow Algorithms for Energy
 *	Minimization in Vision."
 *	Yuri Boykov and Vladimir Kolmogorov.
 *	In IEEE Transactions on Pattern Analysis and Machine Intelligence
 *	(PAMI),
 *	September 2004
 *
 *	This algorithm was developed by Yuri Boykov and Vladimir Kolmogorov
 *	at Siemens Corporate Research. To make it available for public
 *	use, it was later reimplemented by Vladimir Kolmogorov based on open
 *	publications.
 *
 *	If you use this software for research purposes, you should cite
 *	the aforementioned paper in any resulting publication.
 *
 * @version 0.1
 */

import java.util.LinkedList;
import java.util.List;

import org.knime.knip.core.data.graphtheory.Edge;
import org.knime.knip.core.data.graphtheory.Node;

/**
 * Class implementing the grach cut algorithm.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Jan Funke <jan.funke@inf.tu-dresden.de>
 */
public class GraphCutAlgorithm {
    /**
     * The two possible segments, represented as special terminal nodes in the graph.
     */
    public enum Terminal {

        FOREGROUND, // a.k.a. the source
        BACKGROUND; // a.k.a. the sink
    }

    // number of nodes
    private final int m_numNodes;

    // maximum number of edges
    private final int m_numEdges;

    // list of all nodes in the graph
    private final Node[] m_nodes;

    // list of all edges in the graph
    private final Edge[] m_edges;

    // internal counter for edge creation
    private int m_edgeNum;

    // the total flow in the whole graph
    private float m_totalFlow;

    // counter for the numbers of iterations to maxflow
    private int m_maxflowIteration;

    // Lists of active nodes: activeQueueFirst points to first
    // elements of the lists, activeQueueLast to the last ones.
    // In between, nodes are connected via reference to next node
    // in each node.
    private final Node[] m_activeQueueFirst;

    private final Node[] m_activeQueueLast;

    // list of orphans
    private final LinkedList<Node> m_orphans;

    // counter for iterations of main loop
    private int m_time;

    /**
     * Initialises the graph cut implementation and allocates the memory needed for the given number of nodes and edges.
     * 
     * @param numNodes The number of nodes that should be created.
     * @param numEdges The number of edges that you can add. A directed edge and its counterpart (i.e., the directed
     *            edge in the other direction) count as one edge.
     */
    public GraphCutAlgorithm(final int numNodes, final int numEdges) {

        this.m_numNodes = numNodes;
        this.m_numEdges = numEdges;

        this.m_nodes = new Node[numNodes];
        this.m_edges = new Edge[2 * numEdges];

        this.m_edgeNum = 0;

        this.m_totalFlow = 0;

        this.m_maxflowIteration = 0;

        this.m_activeQueueFirst = new Node[2];
        this.m_activeQueueLast = new Node[2];

        this.m_orphans = new LinkedList<Node>();

        assert (numNodes > 0) : "number of nodes must be > 0";

        for (int i = 0; i < numNodes; i++) {
            m_nodes[i] = new Node();
        }
    }

    /**
     * Set the affinity for one node to belong to the foreground (i.e., source) or background (i.e., sink).
     * 
     * @param nodeId The number of the node.
     * @param source The affinity of this node to the foreground (i.e., source)
     * @param sink The affinity of this node to the background (i.e., sink)
     */
    public void setTerminalWeights(final int nodeId, float source, float sink) {

        assert ((nodeId >= 0) && (nodeId < m_numNodes)) : "nodeID out of Bounds";

        final float delta = m_nodes[nodeId].getResidualCapacity();

        if (delta > 0) {
            source += delta;
        } else {
            sink -= delta;
        }

        m_totalFlow += (source < sink) ? source : sink;

        m_nodes[nodeId].setResidualCapacity(source - sink);
    }

    /**
     * Set the edge weight of an undirected edge between two nodes.
     * 
     * Please note that you cannot call any <tt>setEdgeWeight</tt> more often than the number of edges you specified at
     * the time of construction!
     * 
     * @param nodeId1 The first node.
     * @param nodeId2 The second node.
     * @param weight The weight (i.e., the cost) of the connecting edge.
     */
    public void setEdgeWeight(final int nodeId1, final int nodeId2, final float weight) {

        setEdgeWeight(nodeId1, nodeId2, weight, weight);
    }

    /**
     * Set the edge weight of a pair of directed edges between two nodes.
     * 
     * Please note that you cannot call any <tt>setEdgeWeight</tt> more often than the number of edges you specified at
     * the time of construction!
     * 
     * @param nodeId1 The first node.
     * @param nodeId2 The second node.
     * @param weight1to2 The weight (i.e., the cost) of the directed edge from node1 to node2.
     * @param weight2to1 The weight (i.e., the cost) of the directed edge from node2 to node1.
     */
    public void setEdgeWeight(final int nodeId1, final int nodeId2, final float weight1to2, final float weight2to1) {

        assert ((nodeId1 >= 0) && (nodeId1 < m_numNodes)) : "nodeID1 out of bounds";
        assert ((nodeId2 >= 0) && (nodeId2 < m_numNodes)) : "nodeID2 out of bounds";
        assert (nodeId1 != nodeId2) : "nodeID1 == nodeID is not allowed";
        assert (weight1to2 >= 0) : " weight have to be >= 0";
        assert (weight2to1 >= 0) : " weight have to be >= 0";
        assert (m_edgeNum < (m_numEdges - 2)) : "max number of edges(" + m_numEdges + ") reached: " + m_edgeNum;

        // create new edges
        final Edge edge = new Edge();
        m_edges[m_edgeNum] = edge;
        m_edgeNum++;
        final Edge reverseEdge = new Edge();
        m_edges[m_edgeNum] = reverseEdge;
        m_edgeNum++;

        // get the nodes
        final Node node1 = m_nodes[nodeId1];
        final Node node2 = m_nodes[nodeId2];

        // link edges
        edge.setSister(reverseEdge);
        reverseEdge.setSister(edge);

        // add node1 to edge
        edge.setNext(node1.getFirstOutgoing());
        node1.setFirstOutgoing(edge);

        // add node2 to reverseEdge
        reverseEdge.setNext(node2.getFirstOutgoing());
        node2.setFirstOutgoing(reverseEdge);

        // set targets of edges
        edge.setHead(node2);
        reverseEdge.setHead(node1);

        // set residual capacities
        edge.setResidualCapacity(weight1to2);
        reverseEdge.setResidualCapacity(weight2to1);
    }

    /**
     * Performs the actual max-flow/min-cut computation.
     * 
     * @param reuseTrees reuse trees of a previos call
     * @param changedNodes list of nodes that potentially changed their segmentation compared to a previous call, can be
     *            set to <tt>null</tt>
     */
    public float computeMaximumFlow(boolean reuseTrees, final List<Integer> changedNodes) {

        if (m_maxflowIteration == 0) {
            reuseTrees = false;
        }

        if (reuseTrees) {
            maxflowReuseTreesInit();
        } else {
            maxflowInit();
        }

        Node currentNode = null;
        Edge edge = null;

        // main loop
        while (true) {

            Node activeNode = currentNode;

            if (activeNode != null) {
                // remove active flag
                activeNode.setNext(null);
                if (activeNode.getParent() == null) {
                    activeNode = null;
                }
            }
            if (activeNode == null) {
                activeNode = getNextActiveNode();
                if (activeNode == null) {
                    // no more active nodes - we're done
                    // here
                    break;
                }
            }

            // groth
            if (!activeNode.isInSink()) {
                // grow source tree
                for (edge = activeNode.getFirstOutgoing(); edge != null; edge = edge.getNext()) {
                    if (edge.getResidualCapacity() != 0) {

                        final Node headNode = edge.getHead();

                        if (headNode.getParent() == null) {
                            // free node found, add
                            // to source tree
                            headNode.setInSink(false);
                            headNode.setParent(edge.getSister());
                            headNode.setTimestamp(activeNode.getTimestamp());
                            headNode.setDistance(activeNode.getDistance() + 1);
                            setNodeActive(headNode);
                            addToChangedList(headNode);

                        } else if (headNode.isInSink()) {
                            // node is not free and
                            // belongs to other tree
                            // - path
                            // via edge found
                            break;

                        } else if ((headNode.getTimestamp() <= activeNode.getTimestamp())
                                && (headNode.getDistance() > activeNode.getDistance())) {
                            // node is not free and
                            // belongs to our tree -
                            // try to
                            // shorten its distance
                            // to the source
                            headNode.setParent(edge.getSister());
                            headNode.setTimestamp(activeNode.getTimestamp());
                            headNode.setDistance(activeNode.getDistance() + 1);
                        }
                    }
                }
            } else {
                // activeNode is in sink, grow sink tree
                for (edge = activeNode.getFirstOutgoing(); edge != null; edge = edge.getNext()) {
                    if (edge.getSister().getResidualCapacity() != 0) {

                        final Node headNode = edge.getHead();

                        if (headNode.getParent() == null) {
                            // free node found, add
                            // to sink tree
                            headNode.setInSink(true);
                            headNode.setParent(edge.getSister());
                            headNode.setTimestamp(activeNode.getTimestamp());
                            headNode.setDistance(activeNode.getDistance() + 1);
                            setNodeActive(headNode);
                            addToChangedList(headNode);

                        } else if (!headNode.isInSink()) {
                            // node is not free and
                            // belongs to other tree
                            // - path
                            // via edge's sister
                            // found
                            edge = edge.getSister();
                            break;

                        } else if ((headNode.getTimestamp() <= activeNode.getTimestamp())
                                && (headNode.getDistance() > activeNode.getDistance())) {
                            // node is not free and
                            // belongs to our tree -
                            // try to
                            // shorten its distance
                            // to the sink
                            headNode.setParent(edge.getSister());
                            headNode.setTimestamp(activeNode.getTimestamp());
                            headNode.setDistance(activeNode.getDistance() + 1);
                        }
                    }
                }
            }

            m_time++;

            if (edge != null) {
                // we found a path via edge

                // set active flag
                activeNode.setNext(activeNode);
                currentNode = activeNode;

                // augmentation
                augment(edge);

                // adoption
                while (m_orphans.size() > 0) {
                    final Node orphan = m_orphans.poll();
                    if (orphan.isInSink()) {
                        processSinkOrphan(orphan);
                    } else {
                        processSourceOrphan(orphan);
                    }
                }
            } else {
                // no path found
                currentNode = null;
            }
        }

        m_maxflowIteration++;

        // create list of changed nodes
        if (changedNodes != null) {
            changedNodes.clear();
            for (int i = 0; i < m_nodes.length; i++) {
                if (m_nodes[i].isInChangedList()) {
                    changedNodes.add(i);
                }
            }
        }

        return m_totalFlow;
    }

    /**
     * Get the segmentation, i.e., the terminal node that is connected to the specified node. If there are several
     * min-cut solutions, free nodes are assigned to the background.
     * 
     * @param nodeId the node to check
     * @return Either <tt>Terminal.FOREGROUND</tt> or <tt>Terminal.BACKGROUND</tt>
     */
    public Terminal getTerminal(final int nodeId) {

        assert ((nodeId >= 0) && (nodeId < m_numNodes)) : "nodeID out of Bounds";

        final Node node = m_nodes[nodeId];

        if (node.getParent() != null) {
            return node.isInSink() ? Terminal.BACKGROUND : Terminal.FOREGROUND;
        }

        return Terminal.BACKGROUND;
    }

    /**
     * Gets the number of nodes in this graph.
     * 
     * @return The number of nodes
     */
    public int getNumNodes() {
        return this.m_numNodes;
    }

    /**
     * Gets the number of edges in this graph.
     * 
     * @return The number of edges.
     */
    public int getNumEdges() {
        return this.m_numEdges;
    }

    /**
     * Mark a node as being changed.
     * 
     * Use this method if the graph weights changed after a previous computation of the max-flow. The next computation
     * will be faster by just considering changed nodes.
     * 
     * A node has to be considered changed if any of its adjacent edges changed its weight.
     * 
     * @param nodeId The node that changed.
     */
    public void markNode(final int nodeId) {

        assert ((nodeId >= 0) && (nodeId < m_numNodes)) : "nodeID out of Bounds";

        final Node node = m_nodes[nodeId];

        if (node.getNext() == null) {
            if (m_activeQueueLast[1] != null) {
                m_activeQueueLast[1].setNext(node);
            } else {
                m_activeQueueFirst[1] = node;
            }

            m_activeQueueLast[1] = node;
            node.setNext(node);
        }

        node.setMarked(true);
    }

    /*
     * PRIVATE METHODS
     */

    /**
     * Marks a node as being active and adds it to second queue of active nodes.
     */
    private void setNodeActive(final Node node) {

        if (node.getNext() == null) {
            if (m_activeQueueLast[1] != null) {
                m_activeQueueLast[1].setNext(node);
            } else {
                m_activeQueueFirst[1] = node;
            }

            m_activeQueueLast[1] = node;
            node.setNext(node);
        }
    }

    /**
     * Gets the next active node, that is, the first node of the first queue of active nodes. If this queue is empty,
     * the second queue is used. Returns <tt>nyll</tt>, if no active node is left.
     */
    private Node getNextActiveNode() {

        Node node;

        while (true) {

            node = m_activeQueueFirst[0];

            if (node == null) {
                // queue 0 was empty, try other one
                node = m_activeQueueFirst[1];

                // swap queues
                m_activeQueueFirst[0] = m_activeQueueFirst[1];
                m_activeQueueLast[0] = m_activeQueueLast[1];
                m_activeQueueFirst[1] = null;
                m_activeQueueLast[1] = null;

                // if other queue was emtpy as well, return null
                if (node == null) {
                    return null;
                }
            }

            // remove current node from active list
            if (node.getNext() == node) {
                // this was the last one
                m_activeQueueFirst[0] = null;
                m_activeQueueLast[0] = null;
            } else {
                m_activeQueueFirst[0] = node.getNext();
            }

            // not in any list anymore
            node.setNext(null);

            // return only if it has a parent and is therefore
            // active
            if (node.getParent() != null) {
                return node;
            }
        }
    }

    /**
     * Mark a node as orphan and add it to the front of the queue.
     */
    private void addOrphanAtFront(final Node node) {

        node.setParent(Edge.ORPHAN);

        m_orphans.addFirst(node);
    }

    /**
     * Mark a node as orphan and add it to the back of the queue.
     */
    private void addOrphanAtBack(final Node node) {

        node.setParent(Edge.ORPHAN);

        m_orphans.addLast(node);
    }

    /**
     * Add a node to the list of potentially changed nodes.
     */
    private void addToChangedList(final Node node) {

        node.setInChangedList(true);
    }

    /**
     * Initialise the algorithm.
     * 
     * Only called if <tt>reuseTrees</tt> is false.
     */
    private void maxflowInit() {

        m_activeQueueFirst[0] = null;
        m_activeQueueLast[0] = null;
        m_activeQueueFirst[1] = null;
        m_activeQueueLast[1] = null;

        m_orphans.clear();

        m_time = 0;

        for (final Node node : m_nodes) {

            node.setNext(null);
            node.setMarked(false);
            node.setInChangedList(false);
            node.setTimestamp(m_time);

            if (node.getResidualCapacity() > 0) {
                // node is connected to source
                node.setInSink(false);
                node.setParent(Edge.TERMINAL);
                setNodeActive(node);
                node.setDistance(1);
            } else if (node.getResidualCapacity() < 0) {
                // node is connected to sink
                node.setInSink(true);
                node.setParent(Edge.TERMINAL);
                setNodeActive(node);
                node.setDistance(1);
            } else {
                node.setParent(null);
            }
        }
    }

    /**
     * Initialise the algorithm.
     * 
     * Only called if <tt>reuseTrees</tt> is true.
     */
    private void maxflowReuseTreesInit() {

        Node node1;
        Node node2;

        Node queueStart = m_activeQueueFirst[1];

        Edge edge;

        m_activeQueueFirst[0] = null;
        m_activeQueueLast[0] = null;
        m_activeQueueFirst[1] = null;
        m_activeQueueLast[1] = null;

        m_orphans.clear();

        m_time++;

        while ((node1 = queueStart) != null) {

            queueStart = node1.getNext();

            if (queueStart == node1) {
                queueStart = null;
            }

            node1.setNext(null);
            node1.setMarked(false);
            setNodeActive(node1);

            if (node1.getResidualCapacity() == 0) {
                if (node1.getParent() != null) {
                    addOrphanAtBack(node1);
                }
                continue;
            }

            if (node1.getResidualCapacity() > 0) {

                if ((node1.getParent() == null) || node1.isInSink()) {

                    node1.setInSink(false);
                    for (edge = node1.getFirstOutgoing(); edge != null; edge = edge.getNext()) {

                        node2 = edge.getHead();
                        if (!node2.isMarked()) {
                            if (node2.getParent() == edge.getSister()) {
                                addOrphanAtBack(node2);
                            }
                            if ((node2.getParent() != null) && node2.isInSink() && (edge.getResidualCapacity() > 0)) {
                                setNodeActive(node2);
                            }
                        }
                    }
                    addToChangedList(node1);
                }
            } else {

                if ((node1.getParent() == null) || !node1.isInSink()) {

                    node1.setInSink(true);
                    for (edge = node1.getFirstOutgoing(); edge != null; edge = edge.getNext()) {

                        node2 = edge.getHead();
                        if (!node2.isMarked()) {
                            if (node2.getParent() == edge.getSister()) {
                                addOrphanAtBack(node2);
                            }
                            if ((node2.getParent() != null) && !node2.isInSink()
                                    && (edge.getSister().getResidualCapacity() > 0)) {
                                setNodeActive(node2);
                            }
                        }
                    }
                    addToChangedList(node1);
                }
            }
            node1.setParent(Edge.TERMINAL);
            node1.setTimestamp(m_time);
            node1.setDistance(1);
        }

        // adoption
        while (m_orphans.size() > 0) {
            final Node orphan = m_orphans.poll();
            if (orphan.isInSink()) {
                processSinkOrphan(orphan);
            } else {
                processSourceOrphan(orphan);
            }
        }
    }

    /**
     * Perform the augmentation step of the graph cut algorithm.
     * 
     * This is done whenever a path between the source and the sink was found.
     */
    private void augment(final Edge middle) {

        Node node;
        Edge edge;

        float bottleneck;

        // 1. find bottleneck capacity

        // 1a - the source tree
        bottleneck = middle.getResidualCapacity();
        for (node = middle.getSister().getHead();; node = edge.getHead()) {

            edge = node.getParent();

            if (edge == Edge.TERMINAL) {
                break;
            }
            if (bottleneck > edge.getSister().getResidualCapacity()) {
                bottleneck = edge.getSister().getResidualCapacity();
            }
        }

        if (bottleneck > node.getResidualCapacity()) {
            bottleneck = node.getResidualCapacity();
        }

        // 1b - the sink tree
        for (node = middle.getHead();; node = edge.getHead()) {

            edge = node.getParent();

            if (edge == Edge.TERMINAL) {
                break;
            }
            if (bottleneck > edge.getResidualCapacity()) {
                bottleneck = edge.getResidualCapacity();
            }
        }
        if (bottleneck > -node.getResidualCapacity()) {
            bottleneck = -node.getResidualCapacity();
        }

        // 2. augmenting

        // 2a - the source tree
        middle.getSister().setResidualCapacity(middle.getSister().getResidualCapacity() + bottleneck);
        middle.setResidualCapacity(middle.getResidualCapacity() - bottleneck);
        for (node = middle.getSister().getHead();; node = edge.getHead()) {

            edge = node.getParent();

            if (edge == Edge.TERMINAL) {
                // end of path
                break;
            }
            edge.setResidualCapacity(edge.getResidualCapacity() + bottleneck);
            edge.getSister().setResidualCapacity(edge.getSister().getResidualCapacity() - bottleneck);
            if (edge.getSister().getResidualCapacity() == 0) {
                addOrphanAtFront(node);
            }
        }
        node.setResidualCapacity(node.getResidualCapacity() - bottleneck);
        if (node.getResidualCapacity() == 0) {
            addOrphanAtFront(node);
        }

        // 2b - the sink tree
        for (node = middle.getHead();; node = edge.getHead()) {

            edge = node.getParent();

            if (edge == Edge.TERMINAL) {
                // end of path
                break;
            }
            edge.getSister().setResidualCapacity(edge.getSister().getResidualCapacity() + bottleneck);
            edge.setResidualCapacity(edge.getResidualCapacity() - bottleneck);
            if (edge.getResidualCapacity() == 0) {
                addOrphanAtFront(node);
            }
        }
        node.setResidualCapacity(node.getResidualCapacity() + bottleneck);
        if (node.getResidualCapacity() == 0) {
            addOrphanAtFront(node);
        }

        m_totalFlow += bottleneck;
    }

    /**
     * Adopt an orphan.
     */
    private void processSourceOrphan(final Node orphan) {

        Edge bestEdge = null;
        int minDistance = Integer.MAX_VALUE;

        for (Edge orphanEdge = orphan.getFirstOutgoing(); orphanEdge != null; orphanEdge = orphanEdge.getNext()) {
            if (orphanEdge.getSister().getResidualCapacity() != 0) {

                Node node = orphanEdge.getHead();
                Edge parentEdge = node.getParent();

                if (!node.isInSink() && (parentEdge != null)) {

                    // check the origin of node
                    int distance = 0;
                    while (true) {

                        if (node.getTimestamp() == m_time) {
                            distance += node.getDistance();
                            break;
                        }
                        parentEdge = node.getParent();
                        distance++;
                        if (parentEdge == Edge.TERMINAL) {
                            node.setTimestamp(m_time);
                            node.setDistance(1);
                            break;
                        }
                        if (parentEdge == Edge.ORPHAN) {
                            distance = Integer.MAX_VALUE;
                            break;
                        }
                        // otherwise, proceed to the
                        // next node
                        node = parentEdge.getHead();
                    }
                    if (distance < Integer.MAX_VALUE) { // node
                        // originates
                        // from
                        // the source

                        if (distance < minDistance) {
                            bestEdge = orphanEdge;
                            minDistance = distance;
                        }
                        // set marks along the path
                        for (node = orphanEdge.getHead(); node.getTimestamp() != m_time; node =
                                node.getParent().getHead()) {

                            node.setTimestamp(m_time);
                            node.setDistance(distance);
                            distance--;
                        }
                    }
                }
            }
        }

        orphan.setParent(bestEdge);
        if (bestEdge != null) {
            orphan.setTimestamp(m_time);
            orphan.setDistance(minDistance + 1);
        } else {
            // no parent found
            addToChangedList(orphan);

            // process neighbors
            for (Edge orphanEdge = orphan.getFirstOutgoing(); orphanEdge != null; orphanEdge = orphanEdge.getNext()) {

                final Node node = orphanEdge.getHead();
                final Edge parentEdge = node.getParent();
                if (!node.isInSink() && (parentEdge != null)) {

                    if (orphanEdge.getSister().getResidualCapacity() != 0) {
                        setNodeActive(node);
                    }
                    if ((parentEdge != Edge.TERMINAL) && (parentEdge != Edge.ORPHAN)
                            && (parentEdge.getHead() == orphan)) {
                        addOrphanAtBack(node);
                    }
                }
            }
        }

    }

    /**
     * Adopt an orphan.
     */
    private void processSinkOrphan(final Node orphan) {

        Edge bestEdge = null;
        int minDistance = Integer.MAX_VALUE;

        for (Edge orphanEdge = orphan.getFirstOutgoing(); orphanEdge != null; orphanEdge = orphanEdge.getNext()) {
            if (orphanEdge.getResidualCapacity() != 0) {

                Node node = orphanEdge.getHead();
                Edge parentEdge = node.getParent();

                if (node.isInSink() && (parentEdge != null)) {

                    // check the origin of node
                    int distance = 0;
                    while (true) {

                        if (node.getTimestamp() == m_time) {
                            distance += node.getDistance();
                            break;
                        }
                        parentEdge = node.getParent();
                        distance++;
                        if (parentEdge == Edge.TERMINAL) {
                            node.setTimestamp(m_time);
                            node.setDistance(1);
                            break;
                        }
                        if (parentEdge == Edge.ORPHAN) {
                            distance = Integer.MAX_VALUE;
                            break;
                        }
                        // otherwise, proceed to the
                        // next node
                        node = parentEdge.getHead();
                    }
                    if (distance < Integer.MAX_VALUE) {
                        // node originates from the sink
                        if (distance < minDistance) {
                            bestEdge = orphanEdge;
                            minDistance = distance;
                        }
                        // set marks along the path
                        for (node = orphanEdge.getHead(); node.getTimestamp() != m_time; node =
                                node.getParent().getHead()) {

                            node.setTimestamp(m_time);
                            node.setDistance(distance);
                            distance--;
                        }
                    }
                }
            }
        }

        orphan.setParent(bestEdge);
        if (bestEdge != null) {
            orphan.setTimestamp(m_time);
            orphan.setDistance(minDistance + 1);
        } else {
            // no parent found
            addToChangedList(orphan);

            // process neighbors
            for (Edge orphanEdge = orphan.getFirstOutgoing(); orphanEdge != null; orphanEdge = orphanEdge.getNext()) {

                final Node node = orphanEdge.getHead();
                final Edge parentEdge = node.getParent();
                if (node.isInSink() && (parentEdge != null)) {

                    if (orphanEdge.getResidualCapacity() != 0) {
                        setNodeActive(node);
                    }
                    if ((parentEdge != Edge.TERMINAL) && (parentEdge != Edge.ORPHAN)
                            && (parentEdge.getHead() == orphan)) {
                        addOrphanAtBack(node);
                    }
                }
            }
        }
    }

    /**
     * returns the Edge weight between two nodes
     * 
     * @param nodeID1 first node
     * @param nodeID2 second node
     * @return the weight of the edge
     * 
     */
    public float getEdgeWeight(final int nodeID1, final int nodeID2) {
        final Node n1 = m_nodes[nodeID1];
        final Node n2 = m_nodes[nodeID2];
        Edge e;
        for (int i = 0; i < m_edgeNum; i++) {
            e = m_edges[i];
            if (e.getHead().equals(n1) && e.getSister().getHead().equals(n2)) {
                return e.getResidualCapacity();
            }
        }
        return 0;

    }
}
