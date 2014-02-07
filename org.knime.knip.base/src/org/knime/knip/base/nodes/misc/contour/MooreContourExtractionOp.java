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
 * Created on Jan 31, 2014 by squareys
 */
package org.knime.knip.base.nodes.misc.contour;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.view.Views;

import org.knime.knip.core.data.algebra.ExtendedPolygon;

/**
 *
 * @author Jonathan Hale (University of Konstanz)
 */
@SuppressWarnings("deprecation") //TODO: Remove
public class MooreContourExtractionOp implements UnaryOperation<RandomAccessibleInterval<BitType>, ExtendedPolygon> {

    class ClockwiseMooreNeighborhoodIterator<T extends Type<T>> implements java.util.Iterator<T> {
        RandomAccess<T> m_ra;

        final int[][] CLOCKWISE_OFFSETS = {
                { 0, -1},
                { 1,  0},
                { 1,  0},
                { 0,  1},
                { 0,  1},
                {-1,  0},
                {-1,  0},
                { 0, -1}
        };

        final int[][] CCLOCKWISE_OFFSETS = {
                { 0,  1},
                { 0,  1},
                {-1,  0},
                {-1,  0},
                { 0, -1},
                { 0, -1},
                { 1,  0},
                { 1,  0}
        };

        final short NUM_OFFSETS = 8;

        int m_curOffset = 0;
        int m_startIndex= 7;

        public ClockwiseMooreNeighborhoodIterator(final RandomAccess<T> ra) {
            m_ra = ra;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            // we will continue circling around
            return m_curOffset == m_startIndex;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public T next() {
            m_ra.move(CLOCKWISE_OFFSETS[m_curOffset]);
            m_curOffset = (m_curOffset + 1) & 7; //<=> (m_curOffset+1) % 8
            return m_ra.get();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void backtrack() {
            int[] back = CCLOCKWISE_OFFSETS[m_curOffset];
            m_ra.move(back); //undo last move

            //find out, where to continue: //TODO: Optimize!
            if (back[0] > 0) {
                m_curOffset = 4;
            } else {
                m_curOffset = 0;
            }

            if (back[1] > 0) {
                m_curOffset = 6;
            } else {
                m_curOffset = 2;
            }
        }

        public int getIndex() {
            return m_curOffset;
        }
    }
    /**
     *
     */
    public MooreContourExtractionOp() {
        //TODO: Toggle for Jacob's stopping criterion
        //TODO: Toggle for crack/chain code output, or Polygon to code conversion.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtendedPolygon compute(final RandomAccessibleInterval<BitType> input, final ExtendedPolygon output) {

        RandomAccess<BitType> raInput = input.randomAccess();
        IterableInterval<BitType> iterInput = Views.flatIterable(input);
        Cursor<BitType> cInput = iterInput.cursor();
        ClockwiseMooreNeighborhoodIterator<BitType> cNeigh = new ClockwiseMooreNeighborhoodIterator<BitType>(raInput);

        output.reset(); //clear the polygon, just in case.

        int[] position = new int[2];

        while(cInput.hasNext()) {
            BitType s = cInput.next();

            //we are looking for a black pixel
            if (s.get()) {
                cInput.localize(position);
                raInput.setPosition(position);

                //add to polygon
                output.addPoint(position[0], position[1]); //TODO: Dirty casting!

                BitType curBoundaryPixel = s;

                // backtrack:
                raInput.move(new int[]{-1, 0}); //manual moving back one on x-axis

                //risky, but code should always have more than 0 offsets
                BitType c = cNeigh.next();

                //TODO: Jackob's stopping criteria must go somewhere here.
                while (cNeigh.hasNext() && c == s) {
                        if (c.get()) {
                            //add to polygon
                            raInput.localize(position);
                            output.addPoint(position[0], position[1]);
                            curBoundaryPixel = c;
                            cNeigh.backtrack();
                        }

                        c = cNeigh.next();
                }
            }

        }

        return output;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOperation<RandomAccessibleInterval<BitType>, ExtendedPolygon> copy() {
        return new MooreContourExtractionOp();
    }

}
