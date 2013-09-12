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
package org.knime.knip.core.ui.imgviewer.panels.transfunc;

import java.util.Arrays;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class PlaneSelectionTFCDataProvider<T extends RealType<T>, I extends RandomAccessibleInterval<T>> extends
        AbstractTFCDataProvider<T, Integer> {

    /** generated serial id. */
    private static final long serialVersionUID = -3481919617165973916L;

    private long[] m_pos = new long[0];

    private final int[] m_indices = new int[]{0, 1};

    private Interval m_src = new FinalInterval(new long[2], new long[2]);

    private Interval m_histogramInterval = new FinalInterval(new long[2], new long[2]);

    public PlaneSelectionTFCDataProvider() {
        this(new TransferFunctionControlPanel());
    }

    public PlaneSelectionTFCDataProvider(final TransferFunctionControlPanel panel) {
        super(panel);
    }

    @EventListener
    public void onPlaneSelectionChg(final PlaneSelectionEvent event) {

        // update the values
        m_indices[0] = event.getPlaneDimIndex1();
        m_indices[1] = event.getPlaneDimIndex2();

        m_pos = event.getPlanePos();
        final Integer key = hash(m_pos, event.getDimIndices(), m_src);
        m_histogramInterval = event.getInterval(m_src);

        super.setMementoToTFC(key);
    }

    @Override
    protected final Interval currentHistogramInterval() {
        return m_histogramInterval;
    }

    @Override
    protected final Integer updateKey(final Interval src) {
        m_src = src;

        if (m_src.numDimensions() != m_pos.length) {
            m_pos = new long[src.numDimensions()];
            Arrays.fill(m_pos, 0);

            final long[] min = new long[src.numDimensions()];
            final long[] max = new long[src.numDimensions()];

            Arrays.fill(min, 0);
            Arrays.fill(max, 0);

            max[m_indices[0]] = m_src.dimension(m_indices[0]) - 1;
            max[m_indices[1]] = m_src.dimension(m_indices[1]) - 1;

            m_histogramInterval = new FinalInterval(min, max);
        }

        return hash(m_pos, m_indices, src);
    }

    @EventListener
    public void resetChild(final ViewClosedEvent e) {
        m_histogramInterval = null;
        m_src = null;
    }

    private int hash(final long[] pos, final int[] indices, final Interval src) {

        // set the two indices to values that can not occur in
        // normal settings
        pos[indices[0]] = -1000;
        pos[indices[1]] = -1000;

        // create the hash code
        int hash = 31;

        for (final long i : pos) {
            hash = (hash * 31) + (int)i;
        }

        hash += 31 * src.hashCode();

        return hash;
    }
}
