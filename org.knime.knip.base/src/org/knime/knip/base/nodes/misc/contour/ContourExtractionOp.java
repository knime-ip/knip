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
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.logic.BitType;
import net.imglib2.view.Views;

import org.knime.knip.base.nodes.misc.contour.CodePattern.CodeIteratorOnRandomAccess;
import org.knime.knip.core.data.algebra.ExtendedPolygon;

/**
 *
 * @author Jonathan Hale (University of Konstanz)
 */
@SuppressWarnings("deprecation") //TODO: Remove
public class ContourExtractionOp implements UnaryOperation<RandomAccessibleInterval<BitType>, ExtendedPolygon> {

    /**
     * Basically 4 connected Neighborhood
     *
     * @author Jonathan Hale (University of Konstanz)
     */
    class CrackCode extends CodePattern {
        /**
         */
        public CrackCode() {
            super();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected long[][] getCodeOffsets() {
            return new long[][] {
                    { 1,  0},
                    {-1, -1},
                    {-1,  1},
                    { 1,  1}
            };
        }

    }

    /**
     * Basically 8 connected Neighborhood
     * with counter-clockwise Iteration order
     */
    class ChainCode extends CodePattern {

        /**
         *
         */
        public ChainCode() {
            super();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected long[][] getCodeOffsets() {
            return new long[][] {
                    { 1,  0},
                    { 0, -1},
                    {-1,  0},
                    {-1,  0},
                    { 0,  1},
                    { 0,  1},
                    { 1,  0},
                    { 1,  0}
            };
        }

    }

    private final CodePattern m_codePattern;

    /**
     *
     */
    public ContourExtractionOp() {
        m_codePattern = new ChainCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtendedPolygon compute(final RandomAccessibleInterval<BitType> input, final ExtendedPolygon output) {

        RandomAccess<BitType> raInput = input.randomAccess();
        Cursor<BitType> cInput = Views.iterable(input).cursor();
        CodeIteratorOnRandomAccess<BitType> cCode = m_codePattern.iteratorOnRandomAccess(raInput);

        output.reset(); //clear the polygon, just in case.

        long[] position = new long[2];

        while(cInput.hasNext()) {
            BitType s = cInput.next();

            //black pixel found?
            if (s.get()) {
                cInput.localize(position);
                raInput.setPosition(position);

                //add to polygon
                output.addPoint((int) position[0], (int) position[1]); //TODO: Dirty casting!

                cCode.reset();

                BitType curBoundaryPixel = s;

                //risky, but code should always have more than 0 offsets
                BitType c = cCode.next();
                while (cCode.hasNext() && c == s) {
                        if (c.get()) {
                            //add to polygon
                            raInput.localize(position);
                            output.addPoint((int) position[0], (int) position[1]); //TODO: Dirty casting!
                            curBoundaryPixel = c;
                        }

                        c = cCode.next();
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
        return new ContourExtractionOp();
    }

}
