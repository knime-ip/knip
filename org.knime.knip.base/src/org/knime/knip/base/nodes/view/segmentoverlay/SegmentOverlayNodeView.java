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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableView;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.nodes.view.PlainCellView;
import org.knime.knip.base.nodes.view.segmentoverlay.SegmentOverlayNodeModel.LabelTransformVariables;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ExpandingPanel;
import org.knime.knip.core.ui.imgviewer.ImgCanvas;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.ViewerComponents;
import org.knime.knip.core.ui.imgviewer.events.ImgAndLabelingChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelingWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.TableOverviewDisableEvent;
import org.knime.knip.core.ui.imgviewer.events.TablePositionEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;
import org.knime.knip.core.ui.imgviewer.panels.LabelFilterPanel;
import org.knime.knip.core.ui.imgviewer.panels.RendererSelectionPanel;
import org.knime.knip.core.ui.imgviewer.panels.TransparencyColorSelectionPanel;
import org.knime.knip.core.ui.imgviewer.panels.ViewerControlEvent;
import org.knime.knip.core.ui.imgviewer.panels.ViewerScrollEvent;
import org.knime.knip.core.ui.imgviewer.panels.ViewerScrollEvent.Direction;
import org.knime.knip.core.ui.imgviewer.panels.infobars.ImgLabelingViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.providers.AWTImageProvider;
import org.knime.knip.core.ui.imgviewer.panels.providers.CombinedRU;
import org.knime.knip.core.ui.imgviewer.panels.providers.ImageRU;
import org.knime.knip.core.ui.imgviewer.panels.providers.LabelingRU;
import org.knime.knip.core.util.MiscViews;

import net.imagej.ImgPlus;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.util.ConstantUtils;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

/**
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SegmentOverlayNodeView<T extends RealType<T>, L extends Comparable<L>, I extends IntegerType<I>>
        extends NodeView<SegmentOverlayNodeModel<T, L>> implements ListSelectionListener {

    /* A node logger */
    static NodeLogger LOGGER = NodeLogger.getLogger(SegmentOverlayNodeView.class);

    private LabelHiliteProvider<L, T> m_hiliteProvider;

    /* Image cell view pane */
    private ImgViewer m_imgView = null;

    private EventService m_eventService;

    /* Current row */
    private int m_row;

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

    private PlainCellView m_cellView;

    private Container m_tableViewPanel;

    private JLabel m_statusLabel;

    private static final String DEFAULT_STATUS_BAR =
            "Click on a cell or drag and select multiple cells to continue ...";

    /**
     * Constructor
     *
     * @param model
     */
    public SegmentOverlayNodeView(final SegmentOverlayNodeModel<T, L> model) {
        super(model);
        m_row = -1;

        initTableView();

        m_eventService = new EventService();
        m_eventService.subscribe(this);

        setComponent(m_tableViewPanel);

        loadPortContent();

    }

    private void loadPortContent() {

        m_tableContentView.setModel(getNodeModel().getTableContentModel());

        // Scale to thumbnail size
        m_tableView.validate();
        m_tableView.repaint();
    }

    /*Initializes the table view (left side of the split pane)*/
    private void initTableView() {
        m_tableContentView = new TableContentView();
        m_tableContentView.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_tableContentView.getSelectionModel().addListSelectionListener(this);
        m_tableContentView.getColumnModel().getSelectionModel().addListSelectionListener(this);
        m_tableView = new TableView(m_tableContentView);

        m_tableViewPanel = new JPanel(new BorderLayout());
        m_tableViewPanel.add(m_tableView, BorderLayout.CENTER);

        JPanel statusBar = new JPanel();
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        m_tableViewPanel.add(statusBar, BorderLayout.SOUTH);
        statusBar.setPreferredSize(new Dimension(m_tableViewPanel.getWidth(), 16));
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));
        m_statusLabel = new JLabel(DEFAULT_STATUS_BAR);
        m_statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusBar.add(m_statusLabel);
    }

    /* Initializes the img view (right side of the split pane)*/
    private void initImgView() {
        m_imgView = new ImgViewer();
        AWTImageProvider prov = new AWTImageProvider(20, new CombinedRU(new ImageRU<T>(true), new LabelingRU<L>()));
        prov.setEventService(m_imgView.getEventService());
        m_imgView.addViewerComponent(prov);

        m_imgView.addViewerComponent(new ImgLabelingViewInfoPanel<T, L>());
        m_imgView.addViewerComponent(new ImgCanvas<T, Img<T>>());
        m_imgView.addViewerComponent(ViewerComponents.MINIMAP_PLANE_SELECTION.createInstance());
        m_imgView.addViewerComponent(new ExpandingPanel("Brightness and Contrast",
                ViewerComponents.IMAGE_ENHANCE.createInstance(), true));
        m_imgView.addViewerComponent(new ExpandingPanel("Renderer Selection", new RendererSelectionPanel<T>(), true));

        m_imgView.addViewerComponent(new ExpandingPanel("Transparency", new TransparencyColorSelectionPanel(), true));
        m_imgView.addViewerComponent(
                                     new ExpandingPanel("Label Filter",
                                             new LabelFilterPanel<L>(getNodeModel()
                                                     .getInternalTables().length > SegmentOverlayNodeModel.PORT_SEG),
                                             true));

        m_imgView.doneAdding();

        if (getNodeModel().getInternalTables().length > SegmentOverlayNodeModel.PORT_SEG) {
            m_hiliteProvider = new LabelHiliteProvider<L, T>();
            m_imgView.addViewerComponent(m_hiliteProvider);
            getNodeModel();
            final HiLiteHandler handler = getNodeModel().getInHiLiteHandler(SegmentOverlayNodeModel.PORT_SEG);
            m_hiliteProvider.updateInHandler(handler);

        } else {
            m_hiliteProvider = null;
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        m_tableContentView.setModel(getNodeModel().getTableContentModel());
        m_imgView = null;
        //TODO
        //    m_sp.setRightComponent(new JPanel());
        if (m_hiliteProvider != null) {
            getNodeModel();
            final HiLiteHandler handler = getNodeModel().getInHiLiteHandler(SegmentOverlayNodeModel.PORT_SEG);
            m_hiliteProvider.updateInHandler(handler);
        }
        m_row = -1;
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
        if (m_imgView != null) {
            m_imgView.getEventService().publish(new ViewClosedEvent());
            m_imgView.removeAll();
        }
        m_imgView = null;
        m_tableContentView = null;
        m_tableView = null;
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
        initializeListeners();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void rowChanged(final int row) {

        //if the imgView isn't visible yet, add it to the spit pane -> happens when a cell is selected the first time
        if (m_imgView == null) {
            initImgView();
            m_cellView = new PlainCellView(m_tableView, m_imgView);
            m_cellView.setEventService(m_eventService);
        }

        m_row = row;

        try {

            boolean labelingOnly = (m_tableContentView.getModel().getColumnCount() == 1);

            RandomAccessibleInterval<T> underlyingInterval;
            String imgName = "";
            String imgSource = "";

            LabelingValue<L> currentLabelingCell;

            if (labelingOnly) {
                currentLabelingCell = (LabelingValue<L>)m_tableContentView.getContentModel()
                        .getValueAt(row, SegmentOverlayNodeModel.COL_IDX_SINGLE_LABELING);

                final T max = (T)new ByteType();
                max.setReal(max.getMaxValue());
                underlyingInterval = ConstantUtils
                        .constantRandomAccessibleInterval(max, currentLabelingCell.getDimensions().length,
                                                          new FinalInterval(currentLabelingCell.getLabeling()));
            } else {
                currentLabelingCell = (LabelingValue<L>)m_tableContentView.getContentModel()
                        .getValueAt(row, SegmentOverlayNodeModel.COL_IDX_LABELING);

                // Set image
                final DataCell currentImgCell =
                        m_tableContentView.getContentModel().getValueAt(row, SegmentOverlayNodeModel.COL_IDX_IMAGE);
                final ImgPlus<T> imgPlus = ((ImgPlusValue<T>)currentImgCell).getImgPlus();

                imgName = imgPlus.getName();
                imgSource = imgPlus.getSource();
                underlyingInterval = imgPlus;
            }

            // Update Labeling Mapping for Hiliting
            final RandomAccessibleInterval<LabelingType<L>> labeling = currentLabelingCell.getLabeling();
            LabelingMetadata labelingMetadata = currentLabelingCell.getLabelingMetadata();

            final Map<String, Object> transformationInputMap = new HashMap<String, Object>();
            transformationInputMap.put(LabelTransformVariables.LabelingName.toString(), labelingMetadata.getName());
            transformationInputMap.put(LabelTransformVariables.LabelingSource.toString(), labelingMetadata.getSource());
            transformationInputMap.put(LabelTransformVariables.ImgName.toString(), imgName);
            transformationInputMap.put(LabelTransformVariables.ImgSource.toString(), imgSource);
            transformationInputMap.put(LabelTransformVariables.RowID.toString(),
                                       m_tableContentView.getContentModel().getRowKey(row));

            RandomAccessibleInterval<LabelingType<String>> displayedLabeling = null;
            if (labeling instanceof ImgLabeling) {
                displayedLabeling = new ImgLabeling<>(((ImgLabeling)labeling).getIndexImg());

                new LabelingMappingAccess(Util.getTypeFromInterval(displayedLabeling).getMapping(),
                        Util.getTypeFromInterval(labeling).getMapping(), transformationInputMap, getNodeModel());
            } else {
                final LabelingType<String> type =
                        (LabelingType<String>)Util.getTypeFromInterval(labeling).createVariable();
                // access and set
                new LabelingMappingAccess(type.getMapping(), Util.getTypeFromInterval(labeling).getMapping(),
                        transformationInputMap, getNodeModel());

                new LabelingMappingAccess(Util.getTypeFromInterval(displayedLabeling).getMapping(),
                        Util.getTypeFromInterval(labeling).getMapping(), transformationInputMap, getNodeModel());

                // read only converter
                displayedLabeling =
                        Converters.convert(labeling, new Converter<LabelingType<L>, LabelingType<String>>() {

                            @Override
                            public void convert(final LabelingType<L> arg0, final LabelingType<String> arg1) {
                                arg1.clear();
                                for (final L label : arg0) {
                                    transformationInputMap.put(LabelTransformVariables.Label.toString(),
                                                               label.toString());
                                    arg1.add(getNodeModel().getTransformer().transform(transformationInputMap));
                                }
                            }
                        }, type);
            }

            // TODO here copy source labeling into new labeling

            if (!Intervals.equalDimensions(underlyingInterval, displayedLabeling)) {
                displayedLabeling = MiscViews.synchronizeDimensionality(displayedLabeling,
                                                                        currentLabelingCell.getLabelingMetadata(),
                                                                        underlyingInterval,

                                                                        (ImgPlus<T>)underlyingInterval);
                labelingMetadata = new DefaultLabelingMetadata((ImgPlus<T>)underlyingInterval, labelingMetadata,
                        labelingMetadata, labelingMetadata.getLabelingColorTable());

            }

            m_imgView.getEventService()
                    .publish(new LabelingWithMetadataChgEvent<String>(displayedLabeling, labelingMetadata));

            m_imgView.getEventService().publish(new ImgAndLabelingChgEvent<T, String>(underlyingInterval,
                    displayedLabeling, labelingMetadata, labelingMetadata, labelingMetadata));

            m_imgView.getEventService().publish(new ImgRedrawEvent());
            m_eventService.publish(new TableOverviewDisableEvent(false, true));
            m_eventService.publish(new TablePositionEvent(-1, m_tableContentView.getRowCount(), -1, m_row + 1, "",
                    m_tableContentView.getContentModel().getRowKey(m_row).toString()));

            if (getComponent() == m_tableViewPanel) {

                setComponent(m_cellView);
            }

        } catch (final IndexOutOfBoundsException e2) {
            return;
        }
    }

    /**
     * Updates the ViewPane with the selected image and labeling
     *
     *
     * {@inheritDoc}
     */
    @Override
    public void valueChanged(final ListSelectionEvent e) {

        final int row = m_tableContentView.getSelectionModel().getLeadSelectionIndex();

        if ((row == m_row) || e.getValueIsAdjusting()) {
            if (getComponent() == m_tableViewPanel && m_cellView != null) {

                setComponent(m_cellView);
            }
            return;
        }

        rowChanged(row);

    }

    /**
     * Creates the Listeners used in the displayed views.
     */
    private void initializeListeners() {
    }

    @EventListener
    public void onViewerScrollEvent(final ViewerScrollEvent e) {

        if (e.getDirection() == Direction.NORTH) {
            int r = m_row - 1;
            rowChanged(r);
        }
        if (e.getDirection() == Direction.SOUTH) {
            int r = m_row + 1;
            rowChanged(r);

        }

    }

    @EventListener
    public void onViewerOverviewToggle(final ViewerControlEvent e) {
        if (getComponent() == m_cellView) {
            if (m_cellView.isTableViewVisible()) {
                m_cellView.hideTableView();
            }
            m_tableContentView.clearSelection();
            setComponent(m_tableViewPanel);
        } else {
            // Should not happen.
        }
    }

    static class LabelingMappingAccess extends LabelingMapping.SerialisationAccess<String> {

        private LabelingMapping<String> mapping;

        /**
         * @param mapping
         */
        protected LabelingMappingAccess(final LabelingMapping<String> newMapping, final LabelingMapping<?> oldMapping,
                                        final Map<String, Object> map, final SegmentOverlayNodeModel model) {
            super(newMapping);

            final List<Set<String>> newLabels = new ArrayList<>();
            for (int i = 0; i < oldMapping.numSets(); i++) {
                final HashSet<String> labels = new HashSet<>();
                for (Object o : oldMapping.labelsAtIndex(i)) {
                    map.put(LabelTransformVariables.Label.toString(), o.toString());
                    labels.add(model.getTransformer().transform(map));
                }
                newLabels.add(labels);
            }

            super.setLabelSets(newLabels);
        }
    }

}
