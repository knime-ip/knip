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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.knip.core.awt.Real2ColorByLookupTableRenderer;
import org.knime.knip.core.awt.lookup.LookupTable;
import org.knime.knip.core.awt.lookup.RealLookupTable;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.RendererSelectionChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;

/**
 * Class that wraps the panel and connects it to the knip event service.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class AbstractTFCDataProvider<T extends RealType<T>, KEY> extends ViewerComponent implements
        TransferFunctionControlDataProvider<T> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private class ActionAdapter implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            switch (e.getID()) {
                case TransferFunctionControlPanel.ID_APPLY:
                    fireTransferFunctionChgEvent();
                    break;
                case TransferFunctionControlPanel.ID_NORMALIZE:
                    fireTransferFunctionChgEvent();
                    break;
                case TransferFunctionControlPanel.ID_ONLYONE:
                    m_onlyOne = m_tfc.isOnlyOneFunc();
                    break;
                default:
                    throw new RuntimeException("No action implemented for id " + e.getID());
            }
        }
    }

    private static final int NUM_BINS = 250;

    protected EventService m_eventService;

    protected TransferFunctionControlPanel m_tfc;

    private RandomAccessibleInterval<T> m_src;

    private int m_numBins = NUM_BINS;

    private final Map<KEY, TransferFunctionControlPanel.Memento> m_mementos =
            new HashMap<KEY, TransferFunctionControlPanel.Memento>();

    private final Map<KEY, HistogramWithNormalization> m_histData = new HashMap<KEY, HistogramWithNormalization>();

    private boolean m_onlyOne = true;

    private TransferFunctionControlPanel.Memento m_currentMemento;

    private HistogramWithNormalization m_currentHistogram = new HistogramWithNormalization(new long[]{0, 1}, 0, 1);

    /**
     * Set up a new instance and wrap the passed panel.
     * 
     * @param panel the panel that should be wrapped
     */
    AbstractTFCDataProvider(final TransferFunctionControlPanel panel) {
        super("Transfer Function", false);

        if (panel == null) {
            throw new NullPointerException();
        }

        m_currentMemento = createStartingMemento(panel);

        m_tfc = panel;
        m_tfc.setState(m_currentMemento);
        m_tfc.setOnlyOneFunc(m_onlyOne);
        m_tfc.addActionListener(new ActionAdapter());
        m_tfc.setOnlyOneFunc(m_onlyOne);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(m_tfc);
    }

    /**
     * Use this to calculate a new histogram for a given interval on the current source data.
     */
    private HistogramWithNormalization calcNewHistogram(final Interval interval) {
        assert m_src != null;
        assert interval != null;

        // find min value
        final Cursor<T> cur = Views.iterable(SubsetOperations.subsetview(m_src, interval)).cursor();
        cur.fwd();
        final T sample = cur.get().createVariable();
        cur.reset();

        // create the histogram
        final Histogram1d<T> hist =
                new Histogram1d<T>(new Real1dBinMapper<T>(sample.getMinValue(), sample.getMaxValue(), m_numBins, false));
        while (cur.hasNext()) {
            cur.fwd();
            hist.increment(cur.get());
        }

        return new HistogramWithNormalization(hist.toLongArray(), sample.getMinValue(), sample.getMaxValue());
    }

    /**
     * This method is called everytime the src changes and must return the key that corresponds to the current settings.<br>
     * 
     * @return the key to store the first memento.
     */
    protected abstract KEY updateKey(final Interval src);

    protected abstract Interval currentHistogramInterval();

    /**
     * Use this if the concrete base class has intercepted an event that needs to set a new Memento.<br>
     * 
     * @param key the key to look up or to save the new memento under
     * @param interval the interval to use for calculating the histogram if the key is not yet saved in the map of
     *            mementos
     */
    protected final void setMementoToTFC(final KEY key) {

        TransferFunctionControlPanel.Memento newMemento;
        final HistogramWithNormalization hist = getHistogramData(key);

        if (m_onlyOne) {
            newMemento = m_tfc.createMemento(m_currentMemento, hist);
        } else {
            newMemento = m_mementos.get(key);

            if (newMemento == null) {
                newMemento = m_tfc.createMemento(hist);

                m_mementos.put(key, newMemento);
            }
        }

        m_currentMemento = newMemento;
        m_tfc.setState(m_currentMemento);
        m_currentHistogram = hist;

        fireTransferFunctionChgEvent();
    }

    private HistogramWithNormalization getHistogramData(final KEY key) {
        HistogramWithNormalization hist = m_histData.get(key);

        if (hist == null) {
            hist = calcNewHistogram(currentHistogramInterval());
            m_histData.put(key, hist);
        }

        return hist;
    }

    @EventListener
    public final void onImgUpdated(final IntervalWithMetadataChgEvent<T> event) {

        /*
         * because of the way the AWTImageProvider reacts to new images
         * (simply choosing a new Renderer from a list and keeping the
         * old one if the current renderer is also on the list) and the
         * fact that I cannot add my renderer to this list (the lookup
         * table renderer is only suitable if you have a source for a
         * lookup table, clearly not the case if only the simple image
         * enhance is used), I need to issue a renderer changed request
         * each time the image changes.
         *
         * Moreover this needs to be after the AWTImageProvider has
         * processed this request, so we need to do this after the
         * current AWTEvent has been finished processing.
         */
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                m_eventService.publish(new RendererSelectionChgEvent(new Real2ColorByLookupTableRenderer<T>()));
                m_eventService.publish(new ImgRedrawEvent());
            }
        });

        m_src = event.getRandomAccessibleInterval();
        setMementoToTFC(updateKey(m_src));
    }

    @EventListener
    public void onClose(final ViewClosedEvent event) {
        m_src = null;
    }

    @Override
    public final void setNumberBins(final int bins) {
        m_numBins = bins < 1 ? 1 : bins;
    }

    @Override
    public final void setEventService(final EventService service) {
        if (service == null) {
            m_eventService = new EventService();
        } else {
            m_eventService = service;
        }

        m_eventService.subscribe(this);
    }

    @Override
    public final TransferFunctionControlPanel getControl() {
        return m_tfc;
    }

    private List<TransferFunctionBundle> createStartingBundle() {
        final List<TransferFunctionBundle> bundles = new ArrayList<TransferFunctionBundle>();
        bundles.add(TransferFunctionBundle.newRGBBundle());
        bundles.add(TransferFunctionBundle.newGBundle());

        return bundles;
    }

    private TransferFunctionControlPanel.Memento createStartingMemento(final TransferFunctionControlPanel panel) {
        assert panel != null;

        return panel.createMemento(createStartingBundle(), null);
    }

    private void fireTransferFunctionChgEvent() {
        Histogram hist = m_currentHistogram;

        if (m_tfc.isNormalize()) {
            hist = m_currentHistogram.getNormalizedHistogram();
        }

        final LookupTable<T, ARGBType> table =
                new RealLookupTable<T>(hist.getMinValue(), hist.getMaxValue(), m_tfc.getCurrentBundle());
        m_eventService.publish(new LookupTableChgEvent<T, ARGBType>(table));
        m_eventService.publish(new ImgRedrawEvent());
    }

    @Override
    public Position getPosition() {
        return Position.SOUTH;
    }

    @EventListener
    public void reset(final ViewClosedEvent e) {
        m_src = null;
    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        // ignore
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        // ignore
    }

}
