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
 *
 * @author Jonathan Hale (University of Konstanz)
 */
@SuppressWarnings("deprecation") //TODO: Remove
public class MooreContourExtractionOp implements UnaryOperation<RandomAccessibleInterval<BitType>, List<ExtendedPolygon>> {

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

        int[] lastPos = new int[2];

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
            return m_curOffset != m_startIndex;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public T next() {
            m_ra.localize(lastPos);
            m_ra.move(CLOCKWISE_OFFSETS[m_curOffset]);
            m_curOffset = (m_curOffset + 1) % 8; //<=> (m_curOffset+1) % 8
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

            int[] nowPos = new int[2];
            m_ra.localize(nowPos);

            if (nowPos[0] != lastPos[0] || nowPos[1] != lastPos[1]) {
                System.out.println("ASSERT! Bad Backtrack."); //TODO: Remove, was for debugging
                return;
            }

            //find out, where to continue: //TODO: Optimize!
            if (back[0] > 0) {
                m_curOffset = 4;
            } else if (back[0] < 0){
                m_curOffset = 0;
            }

            if (back[1] > 0) {
                m_curOffset = 6;
            } else if (back[1] < 0){
                m_curOffset = 2;
            }

            m_startIndex = (m_curOffset + 7) % 8;
        }

        public int getIndex() {
            return m_curOffset;
        }
    }

    final boolean m_jacobs;
    /**
     * MooreContourExtractionOp
     * Performs Moore Contour Extraction
     *
     * @param useJacobsCriteria - Set this flag to use "Jacobs stopping criteria"
     */
    public MooreContourExtractionOp(final boolean useJacobsCriteria) {
        m_jacobs = useJacobsCriteria;
        //TODO: Toggle for crack/chain code output, or Polygon to code conversion.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExtendedPolygon> compute(final RandomAccessibleInterval<BitType> input, final List<ExtendedPolygon> output) {

        RandomAccess<BitType> raInput = input.randomAccess();
        Cursor<BitType> cInput = Views.flatIterable(input).cursor();
        ClockwiseMooreNeighborhoodIterator<BitType> cNeigh = new ClockwiseMooreNeighborhoodIterator<BitType>(raInput);

        int[] position = new int[2];
        int[] startPos = new int[2];

        AlgorithmVisualizer<BitType> vis = new AlgorithmVisualizer<BitType>(input);
        vis.setPosition(position);

        while(cInput.hasNext()) {
            BitType s = cInput.next();

            //we are looking for a black pixel
            if (!s.get()) {
                ExtendedPolygon polygon = new ExtendedPolygon();
                output.add(polygon);

                cInput.localize(startPos);
                raInput.setPosition(startPos);

                //add to polygon
                polygon.addPoint(startPos[0], startPos[1]);

                // backtrack:
                raInput.move(new int[]{-1, 0}); //manual moving back one on x-axis

                BitType c = null;

                while (cNeigh.hasNext()) {
                    c = cNeigh.next();
                    raInput.localize(position);
                    if (!c.get()) {
                        if (startPos[0] == position[0] && startPos[1] == position[1]) {
                            //startPoint was found.

                            if (m_jacobs) {
                                //jacobs stopping criteria
                                if ((cNeigh.getIndex() - 2) < 2) {
                                    //if index is 2 or 3, we entered the pixel
                                    //by moving {1, 0}, therefor in the same way.
                                    break;
                                } //else criteria not fullfilled, continue.
                            } else {
                                break;
                            }
                        }
                        //add found point to polygon
                        polygon.addPoint(position[0], position[1]);
                        cNeigh.backtrack();
                    }

                    vis.update();
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                    }
                }
                break;
            }

        }

        return output;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOperation<RandomAccessibleInterval<BitType>, List<ExtendedPolygon>> copy() {
        return new MooreContourExtractionOp(m_jacobs);
    }

}
