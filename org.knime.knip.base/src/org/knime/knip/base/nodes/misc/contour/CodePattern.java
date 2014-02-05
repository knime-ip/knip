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
 * Created on Feb 5, 2014 by squareys
 */
package org.knime.knip.base.nodes.misc.contour;

import java.util.Iterator;

import net.imglib2.RandomAccess;
import net.imglib2.type.Type;

/**
 * Basically Neighborhood with iteration order
 *
 * @author Jonathan Hale (University of Konstanz)
 */
abstract class CodePattern implements Iterable<long[]> {
    final private int m_size;
    final private long[][] m_offsets; //offsets from the middle pixel

    public CodePattern () {
        m_offsets = getCodeOffsets();
        m_size = m_offsets.length;
    }

    /**
     * Get Code Offsets in format:
     * long[][] {
     *  {offset from center pixel},
     *  {offset from first pixel},
     *  ...
     * }
     * @return the array of offsets
     */
    protected abstract long[][] getCodeOffsets();


    public class CodeIterator implements Iterator<long[]> {

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

        /**
         * reset - resets the iterator to the begining of the codePattern
         * Should improve performance vs. creating new Iterator every time.
         */
        public void reset() {
            m_curIndex = 0;
        }
    }

    public class CodeIteratorOnRandomAccess<T extends Type<T>> implements Iterator<T> {

        protected RandomAccess<T> m_randomAccess;
        protected final CodePattern m_code;
        protected int m_curIndex;

        public CodeIteratorOnRandomAccess(final CodePattern pattern, final RandomAccess<T> ra) {
              m_code = pattern;
              m_randomAccess = ra;
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
        public T next() {
            m_randomAccess.move(m_code.getOffset(m_curIndex++));
            return m_randomAccess.get();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * reset - resets the iterator to the beginning of the codePattern
         * Should improve performance vs. creating a new Iterator every time.
         *
         * Warning!: This does not reset the position on the RandomAccessible!
         */
        public void reset() {
            m_curIndex = 0;
        }
    }

    /**
     * LoopingCodeIteratorOnrandomAccess - automatically resetting CodeIteratorOnRandomAccess
     * WARNING!: Use with care: infinite Loop potential
     *
     * @author Jonathan Hale (University of Konstanz)
     */
    class LoopingCodeIteratorOnRandomAccess<T extends Type<T>> extends CodeIteratorOnRandomAccess<T> {

        /**
         * @param pattern
         * @param ra
         */
        public LoopingCodeIteratorOnRandomAccess(final CodePattern pattern, final RandomAccess<T> ra) {
            super(pattern, ra);
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public T next() {
            m_curIndex = (m_curIndex + 1) % m_code.getLength();
            m_randomAccess.move(m_code.getOffset(m_curIndex));
            return m_randomAccess.get();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<long[]> iterator() {
        return new CodeIterator(this);
    }

    public <T extends Type<T>> CodeIteratorOnRandomAccess<T> iteratorOnRandomAccess(final RandomAccess<T> ra) {
        return new CodeIteratorOnRandomAccess<T>(this, ra);
    }

    public int getLength() {
        return m_size;
    }

    public long[] getOffset(final int index) {
        return m_offsets[index];
    }
}