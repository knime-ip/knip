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
package org.knime.knip.base.nodes.seg.waehlby;

import java.util.Iterator;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.logic.BitType;
import net.imglib2.view.Views;

import org.knime.knip.core.data.algebra.ExtendedPolygon;

/**
 *
 * @author Jonathan Hale (University of Konstanz)
 */
@SuppressWarnings("deprecation") //TODO: Remove
public class ContourExtractionOp implements UnaryOperation<RandomAccessibleInterval<BitType>, ExtendedPolygon> {

    /**
     * Basically Neighborhood with iteration order
     *
     * @author squareys
     */
    abstract class CodePattern implements Iterable<long[]> {
        final private int m_size;
        final private long[][] m_offsets; //offsets from the middle pixel

        public CodePattern (final int dimensions) {
            m_offsets = createCodeOffsets(dimensions);
            m_size = m_offsets.length;
        }

        protected abstract long[][] createCodeOffsets(int dims);

        class CodeIterator implements Iterator<long[]> {

            private final CodePattern m_code;
            private int m_curIndex;

            public CodeIterator(final CodePattern code) {
                m_code = code;
            }

            public CodeIterator(final CodePattern code, final int startIndex) {
                m_code = code;

                m_curIndex = startIndex;
                if (code.getLength() > m_curIndex) {
                    // if the start index is larger than the highest possible index,
                    // we set it to the highest possible index, resulting into
                    // hasNext() returning false from the beginning.
                    m_curIndex = code.getLength();
                }
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean hasNext() {
                return m_curIndex == m_code.getLength();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public long[] next() {
                return m_code.getOffset(m_curIndex++);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<long[]> iterator() {
            return new CodeIterator(this);
        }

        public int getLength() {
            return m_size;
        }

        public long[] getOffset(final int index) {
            return m_offsets[index];
        }
    }

    /**
     * Basically 4 connected Neighborhood
     *
     * @author squareys
     */
    class CrackCode extends CodePattern {

        /**
         * @param dimensions
         */
        public CrackCode(final int dimensions) {
            super(dimensions);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected long[][] createCodeOffsets(final int dims) {
            int neighSize = dims << 1;
            long[][] offsets = new long[neighSize][dims];

            for (int n = 0; n < neighSize; ++n) {
                //TODO: risky!
                long[] fastAccess = offsets[n];
                for (int d = 0; d < dims; ++d) {
                    fastAccess[d] = 1; //TODO: Stopped here.
                }
            }

            return offsets;
        }

    }

    /**
     * Basically 8 connected Neighborhood
     */
    class ChainCode extends CodePattern {

        /**
         * @param dimensions
         */
        public ChainCode(final int dimensions) {
            super(dimensions);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected long[][] createCodeOffsets(final int dims) {
            return null;
        }

    }
    /**
     *
     */
    public ContourExtractionOp() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtendedPolygon compute(final RandomAccessibleInterval<BitType> input, final ExtendedPolygon output) {

        RandomAccess<BitType> raInput = input.randomAccess();

        Cursor<BitType> cInput = Views.iterable(input).cursor();

        while(cInput.hasNext()) {
            BitType c = cInput.next();

            //TODO: Check if c has been processed yet


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
