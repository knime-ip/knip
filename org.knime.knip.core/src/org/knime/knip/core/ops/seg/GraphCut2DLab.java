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
package org.knime.knip.core.ops.seg;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.labeling.Labeling;
import net.imglib2.ops.operation.BinaryObjectFactory;
import net.imglib2.ops.operation.BinaryOutputOperation;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.iterableinterval.unary.MakeHistogram;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.sampler.special.ConstantRandomAccessible;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.IntervalIndexer;

import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter;

/**
 * GraphCut where the averge value for the sink and source are retrieved from a labeling.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class GraphCut2DLab<T extends RealType<T>, L extends Comparable<L>> implements
        BinaryOutputOperation<Img<T>, Labeling<L>, Img<BitType>> {

    private Set<long[]> m_sources;

    private Set<long[]> m_sinks;

    private double[] m_srcAvg;

    private double[] m_sinkAvg;

    private double m_lambda;

    private String m_bgLabel;

    private String m_fgLabel;

    private final int m_dimX;

    private final int m_dimY;

    private final int m_dimFeat;

    /**
     * @param factory factory of the source image
     * @param lambda
     * @param fgLabel foreground label (can contain wildcards)
     * @param bgLabel the background label (can contain wildcards
     * @param dimX the first dimensions
     * @param dimY the second dimensions
     * @param dimFeat the dimension containing the features (feature vector)
     */
    public GraphCut2DLab(final double lambda, final String fgLabel, final String bgLabel, final int dimX,
                         final int dimY, final int dimFeat) {
        m_dimX = dimX;
        m_dimY = dimY;
        m_dimFeat = dimFeat;
        m_bgLabel = RulebasedLabelFilter.formatRegExp(bgLabel);
        m_fgLabel = RulebasedLabelFilter.formatRegExp(fgLabel);
        m_lambda = lambda;
    }

    /**
     * @param factory factory of the source image
     * @param lambda
     * @param fgLabel foreground label (can contain wildcards)
     * @param bgLabel the background label (can contain wildcards
     * @param dimX the first dimensions
     * @param dimY the second dimensions
     */
    public GraphCut2DLab(final double lambda, final String fgLabel, final String bgLabel, final int dimX, final int dimY) {
        this(lambda, fgLabel, bgLabel, dimX, dimY, -1);
    }

    /**
     * {@inheritDoc}
     * 
     * @return
     */
    @Override
    public Img<BitType> compute(final Img<T> src, final Labeling<L> labeling, final Img<BitType> res) {

        boolean isBg, isFg;

        final int numFeat = m_dimFeat == -1 ? 1 : (int)src.dimension(m_dimFeat);

        m_sources = new TreeSet<long[]>(new LongArrayComparator());
        m_sinks = new TreeSet<long[]>(new LongArrayComparator());

        m_srcAvg = new double[numFeat];
        m_sinkAvg = new double[numFeat];

        double srcCount = 0;
        double sinkCount = 0;

        for (final L label : labeling.getLabels()) {
            isBg = RulebasedLabelFilter.isValid(label, m_bgLabel);
            isFg = RulebasedLabelFilter.isValid(label, m_fgLabel);

            if (!isBg && !isFg) {
                continue;
            }

            final Cursor<T> roiCursor =
                    labeling.getIterableRegionOfInterest(label)
                            .getIterableIntervalOverROI(new ConstantRandomAccessible<T>(src.firstElement(), labeling
                                                                .numDimensions())).localizingCursor();

            final RandomAccess<T> srcRA = src.randomAccess();

            while (roiCursor.hasNext()) {
                roiCursor.fwd();
                if (isFg) {
                    m_sources.add(new long[]{roiCursor.getLongPosition(m_dimX), roiCursor.getLongPosition(m_dimY)});
                    srcRA.setPosition(roiCursor.getLongPosition(m_dimX), m_dimX);
                    srcRA.setPosition(roiCursor.getLongPosition(m_dimY), m_dimY);
                    for (int i = 0; i < numFeat; i++) {
                        if (m_dimFeat != -1) {
                            srcRA.setPosition(i, m_dimFeat);
                        }
                        m_srcAvg[i] += srcRA.get().getRealDouble();
                    }

                    srcCount++;
                }
                if (isBg) {
                    m_sinks.add(new long[]{roiCursor.getLongPosition(m_dimX), roiCursor.getLongPosition(m_dimY)});
                    srcRA.setPosition(roiCursor.getLongPosition(m_dimX), m_dimX);
                    srcRA.setPosition(roiCursor.getLongPosition(m_dimY), m_dimY);
                    for (int i = 0; i < numFeat; i++) {
                        if (m_dimFeat != -1) {
                            srcRA.setPosition(i, m_dimFeat);
                        }
                        m_sinkAvg[i] += srcRA.get().getRealDouble();
                    }
                    sinkCount++;
                }

            }

        }

        if ((m_sinks.size() == 0) && (m_sources.size() == 0)) {
            throw new IllegalStateException("GraphCut needs sinks and sources");
        }

        for (int i = 0; i < m_srcAvg.length; i++) {
            m_srcAvg[i] /= srcCount;
            m_sinkAvg[i] /= sinkCount;
        }

        calculateGraphCut(src, res);
        return res;
    }

    /**
     * calculates the GraphCut on the Image Img
     * 
     * @param img The Image
     * @return the processed Image with black and white values for sink and Source
     */
    private void calculateGraphCut(final Img<T> src, final Img<BitType> res) {

        /*
         * Image has been normalized before. Therefore the lowest and
         * the highest possible values MUST exist. These can be used as
         * sources and sinks for the graphcut algorithm.
         */

        final int numFeat = m_dimFeat == -1 ? 1 : (int)src.dimension(m_dimFeat);

        /*
         * Calculate the 'camera noise' as the std. deviation of the
         * image's pixel values.
         */
        final float[] stdDev = new float[numFeat];

        final long[] min = new long[src.numDimensions()];
        final long[] max = new long[src.numDimensions()];
        src.min(min);
        src.max(max);
        final MakeHistogram<T> histOp = new MakeHistogram<T>();

        for (int i = 0; i < numFeat; i++) {

            long[] bins;
            if (m_dimFeat != -1) {
                min[m_dimFeat] = i;
                max[m_dimFeat] = i;
                final ImgView<T> subImg =
                        new ImgView<T>(SubsetOperations.subsetview(src, new FinalInterval(min, max)), src.factory());
                bins = histOp.compute(subImg, histOp.bufferFactory().instantiate(subImg)).toLongArray();
            } else {
                bins = histOp.compute(src, histOp.bufferFactory().instantiate(src)).toLongArray();
            }

            long noPixels = 0;
            long sumValues = 0;
            for (int j = 0; j < bins.length; ++j) {
                sumValues += j * bins[j];
                noPixels += bins[j];
            }
            final double mean = sumValues / (double)noPixels;
            long sum = 0;
            for (int j = 0; j < bins.length; ++j) {
                sum += bins[j] * (j - mean) * (j - mean);
            }
            stdDev[i] = (float)Math.sqrt(sum / (double)noPixels);
        }

        // for the neighbor nodes we need the slower ByDim Cursor
        final RandomAccess<T> srcRandomAcess = src.randomAccess();
        final Cursor<BitType> resCursor = res.localizingCursor();

        // get the number of nodes and number of edges from all
        // dimensions
        final long numNodes = src.size();
        final long numEdges = numNodes * (2 + 2);

        final org.knime.knip.core.algorithm.GraphCutAlgorithm graphCut =
                new org.knime.knip.core.algorithm.GraphCutAlgorithm((int)numNodes, (int)numEdges);

        /*
         * computing the edge weights and Computing the maximum weight
         * for the K-value (p. 108 "Interactive Graph Cuts")
         */
        float K_value = 0;

        // the neighbor position for looking at the adjacent nodes
        final long[] dims = new long[res.numDimensions()];
        res.dimensions(dims);
        final long[] cursorPos = new long[resCursor.numDimensions()];

        final double[] nodeValues = new double[numFeat];
        final double[] neighborValues = new double[numFeat];
        while (resCursor.hasNext()) {
            resCursor.fwd();

            // get the position of the cursor
            resCursor.localize(cursorPos);
            srcRandomAcess.setPosition(cursorPos[0], m_dimX);
            srcRandomAcess.setPosition(cursorPos[1], m_dimY);
            final int nodeID = listPosition(cursorPos, dims);
            getValues(nodeValues, srcRandomAcess);
            for (final Integer d : new int[]{m_dimX, m_dimY}) {

                // if we are not at the lower dimension bounds
                if ((srcRandomAcess.getIntPosition(d) - 1) >= 0) {

                    // get the intensity from the neighbor
                    // pixel
                    srcRandomAcess.bck(d);
                    cursorPos[0] = srcRandomAcess.getIntPosition(m_dimX);
                    cursorPos[1] = srcRandomAcess.getIntPosition(m_dimY);
                    final int neighborID = listPosition(cursorPos, dims);
                    getValues(neighborValues, srcRandomAcess);

                    srcRandomAcess.fwd(d);
                    float weight = 0;

                    for (int i = 0; i < nodeValues.length; i++) {
                        weight -= Math.pow(nodeValues[i] - neighborValues[i], 2) / (2 * stdDev[i] * stdDev[i]);
                    }
                    weight /= numFeat;

                    // save maximum value for K
                    K_value = Math.max(K_value, weight);

                    // assumption distance between nodes is
                    // 1
                    weight = (float)Math.exp(weight);

                    weight *= (1 - m_lambda);

                    graphCut.setEdgeWeight(nodeID, neighborID, weight);
                }
            }

        }

        // K has to be bigger than all weights in the graph ==> +1
        K_value = (K_value * dims.length) + 1;

        /*
         * computing the weights to source and sink using the K-value
         */

        resCursor.reset();
        while (resCursor.hasNext()) {
            resCursor.fwd();
            resCursor.localize(cursorPos);
            final int nodeID = listPosition(cursorPos, dims);

            srcRandomAcess.setPosition(cursorPos[0], m_dimX);
            srcRandomAcess.setPosition(cursorPos[1], m_dimY);

            getValues(nodeValues, srcRandomAcess);

            if (m_sinks.contains(cursorPos)) {
                // found sink at loc_cursor.getPosition()
                graphCut.setTerminalWeights(nodeID, 0, K_value);
            } else if (m_sources.contains(cursorPos)) {
                // found source at loc_cursor.getPosition()
                graphCut.setTerminalWeights(nodeID, K_value, 0);
            } else {

                float r_Source = 0;

                float r_Sink = 0;

                for (int i = 0; i < nodeValues.length; i++) {
                    r_Source += Math.abs(nodeValues[i] - m_srcAvg[i]);
                    r_Sink += Math.abs(nodeValues[i] - m_sinkAvg[i]);
                }
                r_Source = (float)-Math.log(1.0 / (r_Source / numFeat));
                r_Sink = (float)-Math.log(1.0 / (r_Sink / numFeat));

                r_Source *= m_lambda;
                r_Sink *= m_lambda;
                graphCut.setTerminalWeights(nodeID, r_Sink, r_Source);
            }

        }

        // compute the maximum flow i.e. the graph cut
        graphCut.computeMaximumFlow(false, null);

        resCursor.reset();

        // Set output image
        final long[] resPos = new long[resCursor.numDimensions()];
        while (resCursor.hasNext()) {
            resCursor.fwd();
            resCursor.localize(resPos);
            resCursor.get().set(graphCut.getTerminal(listPosition(resPos, dims))
                                        .equals(org.knime.knip.core.algorithm.GraphCutAlgorithm.Terminal.BACKGROUND));

        }

    }

    /**
     * Gives the position of the node in the list from the pixel position in the image.
     * 
     * @param imagePosition Coordinates of the pixel in x,y,z,... direction
     * @param dimensions overall image dimensions (width, height, depth,...)
     * @return the position of the node in the list
     */
    private int listPosition(final long[] imagePosition, final long[] dimensions) {
        return (int)IntervalIndexer.positionToIndex(imagePosition, dimensions);
    }

    private void getValues(final double[] res, final RandomAccess<T> ra) {
        for (int i = 0; i < res.length; i++) {
            if (m_dimFeat != -1) {
                ra.setPosition(i, m_dimFeat);
            }
            res[i] = ra.get().getRealDouble();
        }
    }

    private class LongArrayComparator implements Comparator<long[]> {

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(final long[] o1, final long[] o2) {
            if (o1.length != o2.length) {
                return o1.length - o2.length;
            }

            for (int i = 0; i < o1.length; i++) {
                if (o1[i] != o2[i]) {
                    return (int)(o1[i] - o2[i]);
                }
            }
            return 0;
        }

    }

    public Img<BitType> createEmptyOutput(final Img<T> src, final Labeling<L> src2, final long[] dims)
            throws IncompatibleTypeException {
        return src.factory().imgFactory(new BitType()).create(dims, new BitType());
    }

    public long[] resultDims(final Interval src, final Interval labeling) {
        if (m_dimFeat != -1) {

            // check dimensionality
            if ((src.numDimensions() <= m_dimX) || (src.numDimensions() <= m_dimY)
                    || (src.numDimensions() <= m_dimFeat)) {
                throw new IllegalArgumentException("Image doesn't provide the selected dimensions.");
            }
            if ((labeling.numDimensions() <= m_dimX) || (labeling.numDimensions() <= m_dimY)) {
                throw new IllegalArgumentException("Labeling doesn't provide the selected dimensions.");
            }

            if ((src.dimension(m_dimX) != labeling.dimension(m_dimX))
                    || (src.dimension(m_dimY) != labeling.dimension(m_dimY))) {
                throw new IllegalArgumentException(
                        "Image labeling must have the same dimensions size in the dimensions " + m_dimX + " and "
                                + m_dimY + ".");
            }

            return new long[]{src.dimension(m_dimX), src.dimension(m_dimY)};
        }

        // else: check dimensionality
        final long[] imgDim = new long[src.numDimensions()];
        final long[] labDim = new long[labeling.numDimensions()];
        src.dimensions(imgDim);
        labeling.dimensions(labDim);
        if (!Arrays.equals(imgDim, labDim)) {
            throw new IllegalArgumentException("Labeling and Image must have the same dimensions.");
        }

        final long[] dims = new long[src.numDimensions()];
        src.dimensions(dims);

        return dims;

    }

    @Override
    public BinaryOutputOperation<Img<T>, Labeling<L>, Img<BitType>> copy() {
        return new GraphCut2DLab<T, L>(m_lambda, m_fgLabel, m_bgLabel, m_dimX, m_dimY, m_dimFeat);
    }

    @Override
    public BinaryObjectFactory<Img<T>, Labeling<L>, Img<BitType>> bufferFactory() {
        return new BinaryObjectFactory<Img<T>, Labeling<L>, Img<BitType>>() {

            @Override
            public Img<BitType> instantiate(final Img<T> src, final Labeling<L> labeling) {
                try {
                    return createEmptyOutput(src, labeling, resultDims(src, labeling));
                } catch (final IncompatibleTypeException e) {
                    e.printStackTrace();
                }

                return null;
            }
        };
    }
}
