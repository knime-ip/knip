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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.knip.base.nodes.proc.ucm;

import java.util.HashMap;
import java.util.HashSet;

/**
 * UltraMetricContourMap face
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 *
 */
public class UCMFace {
    private final String m_label;

    // outer boundaries
    private HashMap<String, UCMBoundary> m_boundaries;

    // label of the original regions
    private HashSet<String> m_regions;

    /**
     *
     * @param label of the face
     */
    public UCMFace(final String label) {
        m_label = label;
        m_boundaries = new HashMap<String, UCMBoundary>();
        m_regions = new HashSet<String>();
        m_regions.add(label);
    }

    /**
     *
     * @return the boundaries which form the outer boundaries of the regions
     */
    public HashMap<String, UCMBoundary> getBoundaries() {
        return m_boundaries;
    }

    /**
     * adds an Edge to the face
     *
     * @param i Label of second Face
     * @param newBoundary
     */
    public void addBoundary(final String i, final UCMBoundary newBoundary) {
        m_boundaries.put(i, newBoundary);
    }

    /**
     *
     * @return the labels of the original regions which where merged into this face
     */
    public HashSet<String> getRegions() {
        return m_regions;
    }

    /**
     * @return the label of the face
     */
    public String getLabel() {
        return m_label;
    }

    @Override
    public boolean equals(final Object obj) {
        return m_label == ((UCMFace)obj).m_label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}