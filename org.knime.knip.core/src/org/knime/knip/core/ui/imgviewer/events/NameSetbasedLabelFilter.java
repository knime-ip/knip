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
package org.knime.knip.core.ui.imgviewer.events;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.knime.knip.core.data.labeling.LabelFilter;
import org.knime.knip.core.ui.event.KNIPEvent;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author dietyc
 */
public class NameSetbasedLabelFilter<L extends Comparable<L>> implements LabelFilter<L>, KNIPEvent {

    private HashSet<String> m_filterSet;

    private boolean m_includeMatches;

    /**
     * Default constructor
     */
    public NameSetbasedLabelFilter() {
    }

    /**
     * @param includeMatches
     */
    public NameSetbasedLabelFilter(final boolean includeMatches) {
        m_filterSet = new HashSet<String>();
        m_includeMatches = includeMatches;
    }

    /**
     * @param filterSet
     * @param includeMatches
     */
    public NameSetbasedLabelFilter(final HashSet<String> filterSet, final boolean includeMatches) {
        m_filterSet = filterSet;
        m_includeMatches = includeMatches;
    }

    /**
     * @param filter
     */
    public void addFilter(final String filter) {
        m_filterSet.add(filter);
    }

    /**
     * @param filterSet
     */
    public void setFilterSet(final HashSet<String> filterSet) {
        m_filterSet = filterSet;
    }

    /**
     * @return
     */
    public int sizeOfFilterSet() {
        return m_filterSet.size();
    }

    @Override
    public ExecutionPriority getExecutionOrder() {
        return ExecutionPriority.NORMAL;
    }

    @Override
    public <E extends KNIPEvent> boolean isRedundant(final E thatEvent) {
        return this.equals(thatEvent);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(m_filterSet);
        out.writeBoolean(m_includeMatches);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_filterSet = (HashSet<String>)in.readObject();
        m_includeMatches = in.readBoolean();
    }

    @Override
    public Collection<L> filterLabeling(final Collection<L> labels) {
        final Collection<L> ret = new LinkedList<L>();

        if (m_includeMatches) {
            for (final L label : labels) {
                if (m_filterSet.contains(labels.toString())) {
                    ret.add(label);
                }
            }
        } else {
            for (final L label : labels) {
                if (!m_filterSet.contains(label.toString())) {
                    ret.add(label);
                }
            }
        }

        return ret;
    }

    @Override
    public void clear() {
        m_filterSet.clear();
    }

}
