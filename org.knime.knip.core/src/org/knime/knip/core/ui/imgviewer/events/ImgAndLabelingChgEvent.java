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

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.labeling.Labeling;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.Named;
import net.imglib2.meta.Sourced;
import net.imglib2.type.Type;
import net.imglib2.view.Views;

import org.knime.knip.core.ui.event.KNIPEvent;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgAndLabelingChgEvent<T extends Type<T>, L extends Comparable<L>> implements KNIPEvent {

    private final Labeling<L> m_labeling;

    private final RandomAccessibleInterval<T> m_img;

    private final Named m_name;

    private final CalibratedSpace m_cspace;

    private final Sourced m_source;

    /**
     * @param interval
     * @param name
     * @param cspace
     */
    public ImgAndLabelingChgEvent(final RandomAccessibleInterval<T> img, final Labeling<L> labeling, final Named name,
                                  final Sourced source, final CalibratedSpace cspace) {
        m_img = img;
        m_name = name;
        m_source = source;
        m_cspace = cspace;
        m_labeling = labeling;

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
        return m_img;
    }

    public IterableInterval<T> getIterableInterval() {
        return Views.iterable(m_img);
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
    public CalibratedSpace getCalibratedSpace() {
        return m_cspace;
    }

    /**
     * @return the labeling
     */
    public Labeling<L> getLabeling() {
        return m_labeling;
    }

}
