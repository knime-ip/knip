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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ListSelectionModel;
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
import org.knime.knip.core.ui.imgviewer.ExpandingPanel;
import org.knime.knip.core.ui.imgviewer.ImgCanvas;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.ViewerComponents;
import org.knime.knip.core.ui.imgviewer.events.ImgAndLabelingChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelingWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;
import org.knime.knip.core.ui.imgviewer.panels.LabelFilterPanel;
import org.knime.knip.core.ui.imgviewer.panels.RendererSelectionPanel;
import org.knime.knip.core.ui.imgviewer.panels.TableOverviewPanel;
import org.knime.knip.core.ui.imgviewer.panels.TransparencyColorSelectionPanel;
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
public class SegmentOverlayNodeView<T extends RealType<T>, L extends Comparable<L>, I extends IntegerType<I>> extends
        NodeView<SegmentOverlayNodeModel<T, L>> implements ListSelectionListener {

    /* A node logger */
    static NodeLogger LOGGER = NodeLogger.getLogger(SegmentOverlayNodeView.class);

    private LabelHiliteProvider<L, T> m_hiliteProvider;

    /* Image cell view pane */
    private ImgViewer m_imgView = null;

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

    private ActionListener m_leftQuickViewListener;

    private ActionListener m_bottomQuickViewListener;

    private ActionListener m_OverviewListener;

    private PlainCellView m_cellView;

    private ActionListener m_prevRowListener;

    private ActionListener m_nextRowListener;

    /**
     * Constructor
     *
     * @param model
     */
    public SegmentOverlayNodeView(final SegmentOverlayNodeModel<T, L> model) {
        super(model);
        m_row = -1;

        initTableView();

        setComponent(m_tableView);

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
    }

    /* Initializes the img view (right side of the split pane)*/
    private void initImgView() {
        m_imgView = new ImgViewer();
        m_imgView
                .addViewerComponent(new AWTImageProvider(20, new CombinedRU(new ImageRU<T>(true), new LabelingRU<L>())));

        m_imgView.getOverViewButton().addActionListener(m_OverviewListener);

        m_imgView.addViewerComponent(new ImgLabelingViewInfoPanel<T, L>());
        m_imgView.addViewerComponent(new ImgCanvas<T, Img<T>>());
        m_imgView.addViewerComponent(ViewerComponents.MINIMAP_PLANE_SELECTION.createInstance());
        m_imgView.addViewerComponent(new ExpandingPanel("Image Enhancement", ViewerComponents.IMAGE_ENHANCE
                .createInstance(), true));
        m_imgView.addViewerComponent(new ExpandingPanel("Renderer Selection", new RendererSelectionPanel<T>(), true));

        m_imgView.addViewerComponent(new ExpandingPanel("Transparency", new TransparencyColorSelectionPanel()));
        m_imgView.addViewerComponent(new ExpandingPanel("Label Filter", new LabelFilterPanel<L>(getNodeModel()
                .getInternalTables().length > SegmentOverlayNodeModel.PORT_SEG)));

        m_imgView.addViewerComponent(new ExpandingPanel("Navigation", new TableOverviewPanel(), true));
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

    private void rowChanged(final int row) {



        //if the imgView isn't visible yet, add it to the spit pane -> happens when a cell is selected the first time
        if (m_imgView == null) {
            initImgView();
            m_cellView = new PlainCellView(m_tableView, m_imgView);
        }

        m_row = row;

        try {

            boolean labelingOnly = (m_tableContentView.getModel().getColumnCount() == 1);

            RandomAccessibleInterval<T> underlyingInterval;
            String imgName = "";
            String imgSource = "";

            LabelingValue<L> currentLabelingCell;

            if (labelingOnly) {
                currentLabelingCell =
                        (LabelingValue<L>)m_tableContentView.getContentModel()
                                .getValueAt(row, SegmentOverlayNodeModel.COL_IDX_SINGLE_LABELING);

                final T max = (T)new ByteType();
                max.setReal(max.getMaxValue());
                underlyingInterval =
                        ConstantUtils.constantRandomAccessibleInterval(max,
                                                                       currentLabelingCell.getDimensions().length,
                                                                       new FinalInterval(currentLabelingCell
                                                                               .getLabeling()));
            } else {
                currentLabelingCell =
                        (LabelingValue<L>)m_tableContentView.getContentModel()
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
            RandomAccessibleInterval<LabelingType<L>> labeling = currentLabelingCell.getLabeling();
            LabelingMetadata labelingMetadata = currentLabelingCell.getLabelingMetadata();

            final Map<String, Object> transformationInputMap = new HashMap<String, Object>();
            transformationInputMap.put(LabelTransformVariables.LabelingName.toString(), labelingMetadata.getName());
            transformationInputMap.put(LabelTransformVariables.LabelingSource.toString(), labelingMetadata.getSource());
            transformationInputMap.put(LabelTransformVariables.ImgName.toString(), imgName);
            transformationInputMap.put(LabelTransformVariables.ImgSource.toString(), imgSource);
            transformationInputMap.put(LabelTransformVariables.RowID.toString(), m_tableContentView.getContentModel()
                    .getRowKey(row));

            // read only converter
            Converters.convert(labeling, new Converter<LabelingType<L>, LabelingType<String>>() {

                @Override
                public void convert(final LabelingType<L> arg0, final LabelingType<String> arg1) {
                    for (L label : arg0) {
                        transformationInputMap.put(LabelTransformVariables.Label.toString(), label.toString());
                        arg1.add(getNodeModel().getTransformer().transform(transformationInputMap));
                    }
                }
            }, (LabelingType<String>)Util.getTypeFromInterval(labeling).createVariable());

            if (!Intervals.equalDimensions(underlyingInterval, currentLabelingCell.getLabeling())) {
                labeling =
                        MiscViews.synchronizeDimensionality(currentLabelingCell.getLabeling(),
                                                            currentLabelingCell.getLabelingMetadata(),
                                                            underlyingInterval,

                                                            (ImgPlus<T>)underlyingInterval);
                labelingMetadata =
                        new DefaultLabelingMetadata((ImgPlus<T>)underlyingInterval, labelingMetadata, labelingMetadata,
                                labelingMetadata.getLabelingColorTable());

            }

            m_imgView.getEventService().publish(new LabelingWithMetadataChgEvent<L>(labeling, labelingMetadata));

            m_imgView.getEventService().publish(new ImgAndLabelingChgEvent<T, L>(underlyingInterval, labeling,
                                                        labelingMetadata, labelingMetadata, labelingMetadata));

            m_imgView.getEventService().publish(new ImgRedrawEvent());

            m_imgView.getOverViewButton().addActionListener(m_OverviewListener);

            if (getComponent() == m_tableView) {

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
    @SuppressWarnings("unchecked")
    @Override
    public void valueChanged(final ListSelectionEvent e) {

        final int row = m_tableContentView.getSelectionModel().getLeadSelectionIndex();

        if ((row == m_row) || e.getValueIsAdjusting()) {
            return;
        }

        rowChanged(row);

    }

    /**
     * Creates the Listeners used in the displayed views.
     */
    private void initializeListeners() {
        m_prevRowListener = new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                int r = m_row - 1;
                rowChanged(r);

            }

        };

        m_nextRowListener = new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                int r = m_row + 1;
                rowChanged(r);

            }

        };

        m_OverviewListener = new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                if (getComponent() == m_cellView) {
                    if (m_cellView.isTableViewVisible()) {
                        m_cellView.hideTableView();
                    }
                    m_tableContentView.clearSelection();
                    setComponent(m_tableView);
                } else {
                    // Should not happen.
                }

            }
        };
    }
}
