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

import net.imagej.Sourced;
import net.imagej.axis.CalibratedAxis;
import net.imagej.space.CalibratedSpace;
import net.imagej.space.TypedSpace;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.Type;
import net.imglib2.view.Views;

import org.knime.knip.core.ui.event.KNIPEvent;
import org.scijava.Named;

/**
 * {@link Interval} with assigned {@link Sourced}, {@link Named}, {@link TypedSpace} metadata.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <T>
 */
public abstract class IntervalWithMetadataChgEvent<T extends Type<T>, DATA extends RandomAccessibleInterval<T>> implements KNIPEvent {

    private final DATA m_interval;

    private final Named m_name;

    private final CalibratedSpace<? extends CalibratedAxis> m_tspace;

    private final Sourced m_source;

    /**
     * Constructor
     *
     * @param interval interval which triggered this event
     * @param name name of the interval
     * @param source source of the interval
     * @param tspace typed space of the interval
     */
    public IntervalWithMetadataChgEvent(final DATA interval, final Named name,
                                        final Sourced source, final CalibratedSpace<? extends CalibratedAxis> tspace) {
        m_interval = interval;
        m_name = name;
        m_source = source;
        m_tspace = tspace;
    }

    @Override
    public ExecutionPriority getExecutionOrder() {
        return ExecutionPriority.NORMAL;
    }

    /**
     * implements object equality {@inheritDoc}
     */
    @Override
    public <E extends KNIPEvent> boolean isRedundant(final E thatEvent) {
        return this.equals(thatEvent);
    }

    /**
     * @return the interval
     */
    public RandomAccessibleInterval<T> getRandomAccessibleInterval() {
        return m_interval;
    }

    /**
     * @return the interval in its concrete format
     */
    public DATA getData() {
        return m_interval;
    }

    /**
     * @return the {@link IterableInterval} which triggered this event
     */
    public IterableInterval<T> getIterableInterval() {
        return Views.iterable(m_interval);
    }

    /**
     * @return the name
     */
    public Named getName() {
        return m_name;
    }

    /**
     * @return the source
     */
    public Sourced getSource() {
        return m_source;
    }

    /**
     * @return the axes
     */
    public CalibratedSpace<? extends CalibratedAxis> getTypedSpace() {
        return m_tspace;
    }

    /**
     * @return true, if {@link TypedSpace} is a {@link CalibratedSpace}
     */
    public boolean isCalibratedSpaceAvailable() {
        return m_tspace instanceof CalibratedSpace;
    }

}
