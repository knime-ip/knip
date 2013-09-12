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

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.ops.operation.iterableinterval.unary.MakeHistogram;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.view.Views;

/**
 * GraphCut. The values of sink and source are specified directly.
 * 
 * @author hornm, dietzc, University of Konstanz
 */

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class GraphCut2D<T extends RealType<T>, I extends RandomAccessibleInterval<T>, O extends RandomAccessibleInterval<BitType>>
        implements UnaryOutputOperation<I, O> {

    private double[] m_srcVal;

    private double[] m_sinkVal;

    private double m_pottsWeight;

    private final int m_dimX;

    private final int m_dimY;

    private final int m_dimFeat;

    /**
     * @param factory factory of the source image
     * @param lambda
     * @param dimX the first dimensions
     * @param dimY the second dimensions
     * @param dimFeat the dimension containing the features (feature vector)
     */
    public GraphCut2D(final double pottsWeight, final int dimX, final int dimY, final int dimFeat,
                      final double[] srcVal, final double[] sinkVal) {
        m_dimX = dimX;
        m_dimY = dimY;
        m_dimFeat = dimFeat;
        m_pottsWeight = pottsWeight;
        m_srcVal = srcVal.clone();
        m_sinkVal = sinkVal.clone();
    }

    /**
     * @param lambda
     * @param fgLabel foreground label (can contain wildcards)
     * @param bgLabel the background label (can contain wildcards
     * @param dimX the first dimensions
     * @param dimY the second dimensions
     */
    public GraphCut2D(final double pottsWeight, final int dimX, final int dimY, final int dimFeat, final double srcVal,
                      final double sinkVal) {
        this(pottsWeight, dimX, dimY, dimFeat, new double[]{srcVal}, new double[]{sinkVal});
    }

    /**
     * @param lambda
     * @param fgLabel foreground label (can contain wildcards)
     * @param bgLabel the background label (can contain wildcards
     * @param dimX the first dimensions
     * @param dimY the second dimensions
     */
    public GraphCut2D(final double pottsWeight, final int dimX, final int dimY, final double srcVal,
                      final double sinkVal) {
        this(pottsWeight, dimX, dimY, -1, new double[]{srcVal}, new double[]{sinkVal});
    }

    /**
     * {@inheritDoc}
     * 
     * @return
     */
    @Override
    public O compute(final I src, final O res) {

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

        final MakeHistogram<T> hist = new MakeHistogram<T>();

        for (int i = 0; i < numFeat; i++) {

            long[] bins;
            if (m_dimFeat != -1) {
                min[m_dimFeat] = i;
                max[m_dimFeat] = i;

                bins =
                        Operations
                                .compute(hist,
                                         Views.iterable(SubsetOperations.subsetview(src, new FinalInterval(min, max))))
                                .toLongArray();
            } else {

                bins =
                        hist.compute(Views.iterable(src), hist.bufferFactory().instantiate(Views.iterable(src)))
                                .toLongArray();
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
        final Cursor<BitType> resCursor = Views.iterable(res).localizingCursor();

        // get the number of nodes and number of edges from all
        // dimensions
        final long numNodes = Views.iterable(src).size();
        final long numEdges = numNodes * (2 + 2);

        final org.knime.knip.core.algorithm.GraphCutAlgorithm graphCut =
                new org.knime.knip.core.algorithm.GraphCutAlgorithm((int)numNodes, (int)numEdges);

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

                    /*
                     * weight according to p.109 lower right
                     * in the paper (ad-hoc) function
                     */

                    // get the intensity from the neighbor
                    // pixel
                    srcRandomAcess.bck(d);
                    cursorPos[0] = srcRandomAcess.getIntPosition(m_dimX);
                    cursorPos[1] = srcRandomAcess.getIntPosition(m_dimY);
                    final int neighborID = listPosition(cursorPos, dims);
                    getValues(neighborValues, srcRandomAcess);

                    srcRandomAcess.fwd(d);

                    // setting all edge weights to
                    // pottsWeight
                    graphCut.setEdgeWeight(nodeID, neighborID, (float)m_pottsWeight);
                }
            }

        }

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

            float r_Source = 0;

            float r_Sink = 0;

            for (int i = 0; i < nodeValues.length; i++) {
                r_Source += Math.abs(nodeValues[i] - m_srcVal[i]);
                r_Sink += Math.abs(nodeValues[i] - m_sinkVal[i]);
            }
            graphCut.setTerminalWeights(nodeID, r_Source, r_Sink);

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
        return res;

    }

    /**
     * Gives the position of the node in the list from the pixel position in the image.
     * 
     * @param imagePosition Coordinates of the pixel in x,y,z,... direction
     * @param dimensions overall image dimensions (width, height, depth,...)
     * @return the position of the node in the list
     */
    private static synchronized int listPosition(final long[] imagePosition, final long[] dimensions) {
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

    public long[] resultDims(final Interval src) {
        return new long[]{src.dimension(m_dimX), src.dimension(m_dimY)};
    }

    @Override
    public UnaryOutputOperation<I, O> copy() {
        return new GraphCut2D<T, I, O>(m_pottsWeight, m_dimX, m_dimY, m_dimFeat, m_srcVal.clone(), m_sinkVal.clone());
    }

    @Override
    public UnaryObjectFactory<I, O> bufferFactory() {
        return new UnaryObjectFactory<I, O>() {

            @SuppressWarnings("unchecked")
            @Override
            public O instantiate(final I in) {
                if (m_dimFeat != -1) {
                    // check dimensionality
                    if ((m_sinkVal.length != in.dimension(m_dimFeat)) || (m_srcVal.length != in.dimension(m_dimFeat))) {
                        throw new IllegalArgumentException(
                                "Vectors of the source or sink values are not of the same size as the feature dimensions of the image.!");
                    }
                }

                try {
                    ImgFactory<BitType> factory;
                    if (in instanceof Img) {
                        factory = ((Img<T>)in).factory().imgFactory(new BitType());
                    } else {
                        factory = new ArrayImgFactory<BitType>();
                    }
                    return (O)factory.create(resultDims(in), new BitType());

                } catch (final IncompatibleTypeException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
