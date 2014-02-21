/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2014
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
 * Created on Jan 31, 2014 by Jonathan Hale
 */
package org.knime.knip.base.nodes.misc.contour;

import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.view.Views;

import org.knime.knip.core.data.algebra.ExtendedPolygon;

/**
 * MooreContourExtractionOp
 *
 * Implementation of the Moore contour extraction algorithm.
 * Input: BitType RandomAccessible
 * Output: A list of Polygons representing the found contours. Warning! There will be alot of duplicates.
 *
 * @author Jonathan Hale (University of Konstanz)
 */
@SuppressWarnings("deprecation") //TODO: We need better Polygons. Remove this when they made it.
public class MooreContourExtractionOp implements UnaryOperation<RandomAccessibleInterval<BitType>, List<ExtendedPolygon>> {

    /**
     * ClockwiseMooreNeighborhoodIterator
     * Iterates clockwise through a 2D Moore Neighborhood (8 connected Neighborhood).
     *
     * @author Jonathan Hale (University of Konstanz)
     */
    final class ClockwiseMooreNeighborhoodIterator<T extends Type<T>> implements java.util.Iterator<T> {
        final private RandomAccess<T> m_ra;

        final private int[][] CLOCKWISE_OFFSETS = {
                { 0, -1},
                { 1,  0},
                { 1,  0},
                { 0,  1},
                { 0,  1},
                {-1,  0},
                {-1,  0},
                { 0, -1}
        };

        final private int[][] CCLOCKWISE_OFFSETS = {
                { 0,  1},
                { 0,  1},
                {-1,  0},
                {-1,  0},
                { 0, -1},
                { 0, -1},
                { 1,  0},
                { 1,  0}
        };

        //index of offset to be executed at next next() call.
        private int m_curOffset = 0;

        //startIndex basically tells the Cursor when it performed
        //every relative movement in CLOCKWISE_OFFSETS once. After
        //backtrack, this is reset to go through all 8 offsets again.
        private int m_startIndex = 7;

        public ClockwiseMooreNeighborhoodIterator(final RandomAccess<T> ra) {
            m_ra = ra;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final boolean hasNext() {
            return (m_curOffset != m_startIndex);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final T next() {
            m_ra.move(CLOCKWISE_OFFSETS[m_curOffset]);
            m_curOffset = (m_curOffset + 1) & 7; //<=> (m_curOffset+1) % 8
            return m_ra.get();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final void remove() {
            throw new UnsupportedOperationException();
        }

        public final void backtrack() {
            final int[] back = CCLOCKWISE_OFFSETS[m_curOffset];
            m_ra.move(back); //undo last move

            //find out, where to continue:
            if (back[0] == 0) {
                if (back[1] == 1) {
                    m_curOffset = 6;
                } else {
                    m_curOffset = 2;
                }
            } else {
                if (back[0] == 1) {
                    m_curOffset = 4;
                } else {
                    m_curOffset = 0;
                }
            }

            m_startIndex = (m_curOffset + 7) & 7; //set the Pixel to stop at
        }

        public final int getIndex() {
            return m_curOffset;
        }

        /**
         * Reset the current offset index. This does not influence the RandomAccess.
         */
        public final void reset() {
            m_curOffset = 0;
            m_startIndex = 7;
        }
    }

    final private boolean m_jacobs;
    private boolean m_findOnlyOne = false;

    /**
     * MooreContourExtractionOp
     * Performs Moore Contour Extraction
     *
     * @param useJacobsCriteria - Set this flag to use "Jacobs stopping criteria"
     * @param findOnlyOne - Set this flag to stop after one contour was extracted.
     */
    public MooreContourExtractionOp(final boolean useJacobsCriteria, final boolean findOnlyOne) {
        m_jacobs = useJacobsCriteria;
        setFindOnlyOne(findOnlyOne);
        //TODO: Toggle for crack/chain code output, or maybe Polygon to code conversion?
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExtendedPolygon> compute(final RandomAccessibleInterval<BitType> input, final List<ExtendedPolygon> output) {

        final RandomAccess<BitType> raInput = input.randomAccess();
        final Cursor<BitType> cInput = Views.flatIterable(input).cursor();
        final ClockwiseMooreNeighborhoodIterator<BitType> cNeigh = new ClockwiseMooreNeighborhoodIterator<BitType>(raInput);

        int[] position = new int[2];
        int[] startPos = new int[2];

        final int[] firstBacktrack = new int[]{-1, 0};

        ExtendedPolygon curPolygon = null;

        //flag to prevent obvious duplicate extraction
        boolean lastPixelWhite = true;

        while(cInput.hasNext()) {
            //we are looking for a black pixel
            if (!cInput.next().get() && lastPixelWhite) {
                lastPixelWhite = false;

                curPolygon = new ExtendedPolygon();

                raInput.setPosition(cInput);
                raInput.localize(startPos);

                //add to polygon
                curPolygon.addPoint(startPos[0], startPos[1]);

                //backtrack:
                raInput.move(firstBacktrack); //manual moving back one on x-axis

                cNeigh.reset();

                while (cNeigh.hasNext()) {
                    if (!cNeigh.next().get()) {
                        raInput.localize(position);
                        if (startPos[0] == position[0] && startPos[1] == position[1]) {
                            //startPoint was found.
                            if (m_jacobs) {
                                //jacobs stopping criteria
                                if ((cNeigh.getIndex() - 2) < 2) {
                                    //if index is 2 or 3, we entered the pixel
                                    //by moving {1, 0}, therefore in the same way.
                                    break;
                                } //else criteria not fulfilled, continue.
                            } else {
                                break;
                            }
                        }
                        //add found point to polygon
                        curPolygon.addPoint(position[0], position[1]);
                        cNeigh.backtrack();
                    }
                }

                output.add(curPolygon);

                if (m_findOnlyOne) {
                    break;
                }
            } else {
                lastPixelWhite = true;
            }

        }

        return output;
    }

    /**
     * Set whether to stop after one contour was found.
     *
     * Huge speedup, since otherwise contours are extracted
     * multiple times for each pixel its corresponding the shape.
     *
     * @param b
     */
    public void setFindOnlyOne(final boolean b) {
        m_findOnlyOne = b;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOperation<RandomAccessibleInterval<BitType>, List<ExtendedPolygon>> copy() {
        return new MooreContourExtractionOp(m_jacobs, m_findOnlyOne);
    }

}
