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
package org.knime.knip.core.util;

/**
 * This class is a simple pair of objects.
 * 
 * @param <T> class of the first object
 * @param <M> class of the second object
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Thorsten Meinl, University of Konstanz
 */
public final class Triple<F, S, T> {
    private final F m_first;

    private final S m_second;

    private final T m_third;

    /**
     * Creates a new pair.
     * 
     * @param first the first object
     * @param second the second object
     */
    public Triple(final F first, final S second, final T third) {
        m_first = first;
        m_second = second;
        m_third = third;
    }

    /**
     * Returns the first object.
     * 
     * @return the first object
     */
    public F getFirst() {
        return m_first;
    }

    /**
     * Returns the second object.
     * 
     * @return the second object
     */
    public S getSecond() {
        return m_second;
    }

    /**
     * Returns the third object.
     * 
     * @return the third object
     */
    public T getThird() {
        return m_third;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof Triple)) {
            return false;
        }

        final Triple<?, ?, ?> p = (Triple<?, ?, ?>)o;
        if (!areEqual(m_first, p.m_first)) {
            return false;
        }
        if (!areEqual(m_second, p.m_second)) {
            return false;
        }
        if (!areEqual(m_third, p.m_third)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 17;
        hash = (hash * 31) + (m_first == null ? 0 : m_first.hashCode());
        hash = (hash * 31) + (m_second == null ? 0 : m_second.hashCode());
        hash = (hash * 31) + (m_third == null ? 0 : m_third.hashCode());
        return hash;
    }

    /**
     * Determines if both arguments are equal according to their equals method (assumed to be symmetric). This method
     * handles null arguments.
     * 
     * @param o1 First object for comparison, may be <code>null</code>.
     * @param o2 Second object for comparison, may be <code>null</code> .
     * @return If both arguments are equal (if either one is null, so must be the other one)
     */
    private static boolean areEqual(final Object o1, final Object o2) {
        if (o1 == o2) {
            // same object or both are null
            return true;
        }
        if (o1 != null) {
            return o1.equals(o2);
        } else {
            // o2 != null && o1 == null
            return false;
        }
    }
}
