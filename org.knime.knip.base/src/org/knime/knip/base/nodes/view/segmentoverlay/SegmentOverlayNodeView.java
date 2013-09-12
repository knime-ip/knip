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
package org.knime.knip.base.nodes.view.segmentoverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.ops.operation.subset.views.LabelingView;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;

import org.knime.core.data.DataCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableView;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.nodes.view.segmentoverlay.SegmentOverlayNodeModel.LabelTransformVariables;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.ui.imgviewer.ImgCanvas;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.ViewerComponents;
import org.knime.knip.core.ui.imgviewer.events.ImgAndLabelingChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelingWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;
import org.knime.knip.core.ui.imgviewer.panels.LabelFilterPanel;
import org.knime.knip.core.ui.imgviewer.panels.RendererSelectionPanel;
import org.knime.knip.core.ui.imgviewer.panels.TransparencyColorSelectionPanel;
import org.knime.knip.core.ui.imgviewer.panels.infobars.ImgLabelingViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.providers.BufferedImageLabelingOverlayProvider;
import org.knime.knip.core.util.MiscViews;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SegmentOverlayNodeView<T extends RealType<T>, L extends Comparable<L>, I extends IntegerType<I>> extends
        NodeView<SegmentOverlayNodeModel<T, L>> implements ListSelectionListener {

    private class ExtNativeImgLabeling<LL extends Comparable<LL>, II extends IntegerType<II>> extends
            NativeImgLabeling<LL, II> {
        /**
         * @param img
         */
        public ExtNativeImgLabeling(final Img<II> img, final LabelingMapping<LL> mapping) {
            super(img);
            super.mapping = mapping;
        }
    }

    /* A node logger */
    static NodeLogger LOGGER = NodeLogger.getLogger(SegmentOverlayNodeView.class);

    private LabelHiliteProvider<L, T> m_hiliteProvider;

    /* Image cell view pane */
    private ImgViewer m_imgView;

    /* Current row */
    private int m_row;

    /* The split pane for the view */
    private JSplitPane m_sp;

    /* Table for the images */
    private TableContentView m_tableContentView;

    /* The Table view */
    private TableView m_tableView;

    private final ExecutorService UPDATE_EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger m_counter = new AtomicInteger();

        @Override
        public Thread newThread(final Runnable r) {
            final Thread t = new Thread(r, "Segment Overlay Viewer-Updater-" + m_counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    /**
     * Constructor
     * 
     * @param model
     */
    public SegmentOverlayNodeView(final SegmentOverlayNodeModel<T, L> model) {
        super(model);
        m_sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        m_row = -1;
        m_tableContentView = new TableContentView();
        m_tableContentView.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_tableContentView.getSelectionModel().addListSelectionListener(this);
        m_tableContentView.getColumnModel().getSelectionModel().addListSelectionListener(this);
        m_tableView = new TableView(m_tableContentView);

        m_imgView = new ImgViewer();
        m_imgView.addViewerComponent(new BufferedImageLabelingOverlayProvider<T, L>(20));
        m_imgView.addViewerComponent(new ImgLabelingViewInfoPanel<T, L>());
        m_imgView.addViewerComponent(new ImgCanvas<T, Img<T>>());
        m_imgView.addViewerComponent(ViewerComponents.MINIMAP.createInstance());
        m_imgView.addViewerComponent(ViewerComponents.PLANE_SELECTION.createInstance());
        m_imgView.addViewerComponent(ViewerComponents.IMAGE_ENHANCE.createInstance());
        m_imgView.addViewerComponent(new RendererSelectionPanel<T>());

        m_imgView.addViewerComponent(new TransparencyColorSelectionPanel());
        m_imgView.addViewerComponent(new LabelFilterPanel<L>(
                getNodeModel().getInternalTables().length > SegmentOverlayNodeModel.PORT_SEG));

        if (getNodeModel().getInternalTables().length > SegmentOverlayNodeModel.PORT_SEG) {
            m_hiliteProvider = new LabelHiliteProvider<L, T>();
            m_imgView.addViewerComponent(m_hiliteProvider);
        } else {
            m_hiliteProvider = null;
        }

        m_sp.add(m_tableView);
        m_sp.add(m_imgView);

        setComponent(m_sp);

        m_sp.setDividerLocation(300);
        loadPortContent();

    }

    private void loadPortContent() {

        m_tableContentView.setModel(getNodeModel().getTableContentModel());

        // Scale to thumbnail size
        m_tableView.validate();
        m_tableView.repaint();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        m_tableContentView.setModel(getNodeModel().getTableContentModel());
        if (m_hiliteProvider != null) {
            final HiLiteHandler handler = getNodeModel().getInHiLiteHandler(getNodeModel().PORT_SEG);
            m_hiliteProvider.updateInHandler(handler);
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        UPDATE_EXECUTOR.shutdownNow();
        if (m_hiliteProvider != null) {
            m_hiliteProvider.onClose();
        }

        m_tableView.removeAll();
        m_hiliteProvider = null;
        m_tableContentView.removeAll();
        m_imgView.getEventService().publish(new ViewClosedEvent());
        m_imgView.removeAll();
        m_imgView = null;
        m_tableContentView = null;
        m_tableView = null;
        m_sp = null;
        m_row = -1;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // Scale to thumbnail size
        m_tableView.validate();
        m_tableView.repaint();
    }

    /**
     * Updates the ViewPane with the selected image and labeling
     * 
     * 
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void valueChanged(final ListSelectionEvent e) {

        final int row = m_tableContentView.getSelectionModel().getLeadSelectionIndex();

        if ((row == m_row) || e.getValueIsAdjusting()) {
            return;
        }

        m_row = row;

        try {
            RandomAccessibleInterval<T> img;
            LabelingMetadata metadata;
            Labeling<L> lab;
            DataCell currentLabelingCell;

            if (m_tableContentView.getModel().getColumnCount() == 2) {
                // Labeling and image
                final DataCell currentImgCell =
                        m_tableContentView.getContentModel().getValueAt(row, SegmentOverlayNodeModel.COL_IDX_IMAGE);

                currentLabelingCell =
                        m_tableContentView.getContentModel().getValueAt(row, SegmentOverlayNodeModel.COL_IDX_LABELING);

                img = ((ImgPlusValue<T>)currentImgCell).getImgPlus();
                metadata = ((LabelingValue<L>)currentLabelingCell).getLabelingMetadata();

                lab = ((LabelingValue<L>)currentLabelingCell).getLabeling();

            } else {
                // only a Labeling (creates an empty image of
                // ByteType)
                currentLabelingCell =
                        m_tableContentView.getContentModel()
                                .getValueAt(row, SegmentOverlayNodeModel.COL_IDX_SINGLE_LABELING);

                lab = ((LabelingValue<L>)currentLabelingCell).getLabeling();
                final long[] labMin = new long[lab.numDimensions()];
                final long[] labMax = new long[lab.numDimensions()];
                final long[] labDims = new long[lab.numDimensions()];

                lab.min(labMin);
                lab.max(labMax);
                lab.dimensions(labDims);

                final T max = (T)new ByteType();
                max.setReal(max.getMaxValue());
                img = MiscViews.constant(max, new FinalInterval(labMin, labMax));

                metadata = ((LabelingValue<L>)currentLabelingCell).getLabelingMetadata();

            }

            // Inputmap for transformation issues
            final Map<String, Object> transformationInputMap = new HashMap<String, Object>();
            transformationInputMap.put(LabelTransformVariables.LabelingName.toString(),
                                       ((LabelingValue<L>)currentLabelingCell).getLabelingMetadata().getName());
            transformationInputMap.put(LabelTransformVariables.LabelingSource.toString(),
                                       ((LabelingValue<L>)currentLabelingCell).getLabelingMetadata().getSource());
            transformationInputMap.put(LabelTransformVariables.ImgName.toString(), metadata.getName());
            transformationInputMap.put(LabelTransformVariables.ImgSource.toString(), metadata.getSource());
            transformationInputMap.put(LabelTransformVariables.RowID.toString(), m_tableContentView.getContentModel()
                    .getRowKey(row));

            if (lab instanceof NativeImgLabeling) {
                if (getNodeModel().isTransformationActive()) {
                    final Img<I> nativeImgLabeling = ((NativeImgLabeling<L, I>)lab).getStorageImg();

                    final LabelingMapping<String> newMapping =
                            new LabelingMapping<String>(nativeImgLabeling.firstElement().createVariable());
                    final LabelingMapping<L> oldMapping = lab.firstElement().getMapping();
                    for (int i = 0; i < oldMapping.numLists(); i++) {
                        final List<String> newList = new ArrayList<String>();
                        for (final L label : oldMapping.listAtIndex(i)) {
                            transformationInputMap.put(LabelTransformVariables.Label.toString(), label.toString());
                            newList.add(getNodeModel().getTransformer().transform(transformationInputMap));
                        }

                        newMapping.intern(newList);
                    }

                    // Unsafe cast but works
                    lab = (Labeling<L>)new ExtNativeImgLabeling<String, I>(nativeImgLabeling, newMapping);
                }
            } else {
                LOGGER.warn("Labeling Transformer settings don't have any effect, as since now  this is only available for NativeImgLabelings.");
            }

            if (getNodeModel().virtuallyAdjustImgs()) {
                lab =
                        new LabelingView<L>(MiscViews.synchronizeDimensionality(lab,
                                                                                ((LabelingValue<L>)currentLabelingCell)
                                                                                        .getLabelingMetadata(), img,
                                                                                metadata), lab.<L> factory());
            }
            m_imgView.getEventService().publish(new LabelingWithMetadataChgEvent<L>(lab,
                                                        ((LabelingValue<L>)currentLabelingCell).getLabelingMetadata()));

            m_imgView.getEventService().publish(new ImgAndLabelingChgEvent<T, L>(img, lab,
                                                        ((LabelingValue<L>)currentLabelingCell).getLabelingMetadata(),
                                                        metadata, metadata));
            m_imgView.getEventService().publish(new ImgRedrawEvent());
        } catch (final IndexOutOfBoundsException e2) {
            return;
        }

    }
}
