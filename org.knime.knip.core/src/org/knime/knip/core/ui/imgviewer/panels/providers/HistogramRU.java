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
 * Created on 19.09.2013 by zinsmaie
 */
package org.knime.knip.core.ui.imgviewer.panels.providers;

import java.awt.Image;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.iterableinterval.unary.MakeHistogram;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.events.HistogramChgEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;

/**
 * Generates the image of a histogram from a source image.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <T>
 */
public class HistogramRU<T extends RealType<T>> implements RenderUnit {

    //event members

    private EventService m_eventService;

    private final int m_histHeight;

    private PlaneSelectionEvent m_planeSelection;

    private RandomAccessibleInterval<T> m_src;

    /**
     * @param histHeight sets the default value for the height of the created histograms
     */
    public HistogramRU(final int histHeight) {
        m_histHeight = histHeight;
    }

    @Override
    public Image createImage() {
        final Histogram1d<T> hist =
                Operations.compute(new MakeHistogram<T>(),
                                   Views.iterable(SubsetOperations.subsetview(m_src,
                                                                              m_planeSelection.getInterval(m_src))));
        m_eventService.publish(new HistogramChgEvent(hist));
        return AWTImageTools.drawHistogram(hist.toLongArray(), m_histHeight);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int generateHashCode() {
        int hash = 31;
        hash += m_planeSelection.hashCode();
        hash *= 31;
        hash += m_src.hashCode();
        hash *= 31;

        return hash;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    //event handling

    /**
     * @param sel updates the member for currently selected planes
     */
    @EventListener
    public void onPlaneSelectionUpdate(final PlaneSelectionEvent sel) {
        m_planeSelection = sel;
    }

    /**
     * @param e updates the source member (source image is used to calculate the histogram)
     */
    @EventListener
    public void onUpdated(final IntervalWithMetadataChgEvent<T> e) {
        m_src = e.getRandomAccessibleInterval();
    }

    /**
     * set all members that could hold expensive references to null to allow storage clean ups.
     * @param event marker event
     */
    @EventListener
    public void onClose(final ViewClosedEvent event) {
        m_src = null;
    }

    //standard methods

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService service) {
        service.subscribe(this);
        m_eventService = service;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalConfigurations(final ObjectOutput out) throws IOException {
        //nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalConfigurations(final ObjectInput in) throws IOException, ClassNotFoundException {
        //nothing to do here
    }
}
