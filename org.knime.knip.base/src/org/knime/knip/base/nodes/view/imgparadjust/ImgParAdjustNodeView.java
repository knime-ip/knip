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
package org.knime.knip.base.nodes.view.imgparadjust;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableView;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.nodes.view.PlainCellView;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ExpandingPanel;
import org.knime.knip.core.ui.imgviewer.ImgCanvas;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.events.TablePositionEvent;
import org.knime.knip.core.ui.imgviewer.panels.ImgNormalizationPanel;
import org.knime.knip.core.ui.imgviewer.panels.MinimapAndPlaneSelectionPanel;
import org.knime.knip.core.ui.imgviewer.panels.ViewerControlEvent;
import org.knime.knip.core.ui.imgviewer.panels.ViewerScrollEvent;
import org.knime.knip.core.ui.imgviewer.panels.ViewerScrollEvent.Direction;
import org.knime.knip.core.ui.imgviewer.panels.infobars.ImgViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.providers.AWTImageProvider;
import org.knime.knip.core.ui.imgviewer.panels.providers.ThresholdRU;

import net.imagej.ImgPlus;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

/**
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgParAdjustNodeView<T extends RealType<T>> extends NodeView<ImgParAdjustNodeModel> {

    private static final ExecutorService UPDATE_EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger m_counter = new AtomicInteger();

        @Override
        public Thread newThread(final Runnable r) {
            final Thread t = new Thread(r, "ImgParAdjusterView-Updater-" + m_counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    /* the img viewer panel */
    private ImgViewer m_imgViewer;

    private ParameterPanel m_parameterPanel;

    private TableContentView m_tableContentView;

    private PlainCellView m_cellView;

    private TableView m_tableView;

    private int m_col;

    private int m_row;

    private EventService m_eventService;

    private JPanel m_content;

    private JLabel m_statusLabel;

    private JPanel m_tableViewPanel;

    protected static final String m_defStatusBarText =
            "Click on a cell or drag and select multiple cells to continue ...";

    /**
     * @param nodeModel
     */
    protected ImgParAdjustNodeView(final ImgParAdjustNodeModel nodeModel) {
        super(nodeModel);

        // allows the node view to display something during/before the
        // actual
        // node execution
        setShowNODATALabel(false);
    }

    private void initViewComponents() {
        m_tableContentView = new TableContentView(new TableContentModel(getNodeModel().getImageTable()));
        m_tableContentView.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_tableContentView.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                onCellSelectionChanged();
            }
        });
        m_tableContentView.getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                onCellSelectionChanged();
            }
        });

        m_eventService = new EventService();
        m_eventService.subscribe(this);

        m_tableView = new TableView(m_tableContentView);

        // scale to thumbnail size, if desired
        final int TABLE_CELL_SIZE = 100;
        m_tableView.setColumnWidth(TABLE_CELL_SIZE);
        m_tableView.setRowHeight(TABLE_CELL_SIZE);

        // contructing the image viewer
        final int CACHE_SIZE = 50;
        m_imgViewer = new ImgViewer();
        final AWTImageProvider realProvider = new AWTImageProvider(CACHE_SIZE, new ThresholdRU<T>());
        realProvider.setEventService(m_imgViewer.getEventService());

        m_imgViewer.addViewerComponent(realProvider);
        m_imgViewer.addViewerComponent(new ImgViewInfoPanel<T>());
        m_imgViewer.addViewerComponent(new ImgCanvas<T, Img<T>>());

        m_imgViewer.addViewerComponent(new MinimapAndPlaneSelectionPanel());
        m_imgViewer.addViewerComponent(new ExpandingPanel("Normalization", new ImgNormalizationPanel<T, Img<T>>()));
        m_imgViewer.addViewerComponent(new ExpandingPanel("Threshold", new SetThresholdPanel<T, Img<T>>()));
        m_parameterPanel = new ParameterPanel();
        m_imgViewer.addViewerComponent(new ExpandingPanel("Parameters", m_parameterPanel));
        m_imgViewer.doneAdding();

        m_cellView = new PlainCellView(m_tableView, m_imgViewer);

        m_content = new JPanel(new BorderLayout());

        m_tableViewPanel = new JPanel(new BorderLayout());
        m_tableViewPanel.add(m_tableView, BorderLayout.CENTER);

        JPanel statusBar = new JPanel();
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        m_tableViewPanel.add(statusBar, BorderLayout.SOUTH);
        statusBar.setPreferredSize(new Dimension(m_tableViewPanel.getWidth(), 16));
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));
        m_statusLabel = new JLabel(m_defStatusBarText);
        m_statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusBar.add(m_statusLabel);

        m_content.add(m_cellView, BorderLayout.CENTER);
        final JButton continueButton = new JButton("Set parameters and continue execution ...");
        continueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onContinueExecution();
            }
        });
        m_parameterPanel.add(continueButton, BorderLayout.SOUTH);

        setComponent(m_tableViewPanel);

        // let this class listening to the imgViewer events (registers
        // the
        // onNormalization... method the the event service of the
        // imgviewer)
        m_cellView.setEventService(m_eventService);
    }

    private void loadPortContent() {
        UPDATE_EXECUTOR.execute(new Runnable() {
            private TableContentModel m_tableModel;

            @Override
            public void run() {
                m_tableModel = new TableContentModel();
                m_tableModel.setDataTable(getNodeModel().getImageTable());
                initViewComponents();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        if (getNodeModel().getImageTable() == null) {
            setNonExecutingView();
            return;
        } else {
            loadPortContent();
        }

    }

    private boolean cellExists(final int row, final int col) {
        return (col >= 0 && col < m_tableContentView.getColumnCount() && row >= 0
                && row < m_tableContentView.getRowCount());
    }

    /**
     * @param row
     * @param col
     */
    protected void onCellSelectionChanged() {
        final int row = m_tableContentView.getSelectionModel().getLeadSelectionIndex();
        final int col = m_tableContentView.getColumnModel().getSelectionModel().getLeadSelectionIndex();

        onCellSelectionChanged(col, row);
    }

    protected void onCellSelectionChanged(final int col, final int row) {

        if (!cellExists(row, col)) {
            return;
        }

        m_col = col;
        m_row = row;

        if (m_tableContentView.getContentModel().getValueAt(row, col) instanceof ImgPlusValue) {
            final ImgPlus<T> imgPlus =
                    ((ImgPlusValue<T>)m_tableContentView.getContentModel().getValueAt(row, col)).getImgPlus();

            m_imgViewer.setImg(imgPlus);

            if (getComponent() != m_content) {
                setComponent(m_content);
            }
            m_eventService.publish(new TablePositionEvent(m_tableContentView.getColumnCount(),
                    m_tableContentView.getRowCount(), col + 1, row + 1, m_tableContentView.getColumnName(col),
                    m_tableContentView.getContentModel().getRowKey(row).toString()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // nothing to do

    }

    public void onContinueExecution() {
        getNodeModel().pushFlowVariable("saturation", m_parameterPanel.getSaturation());
        getNodeModel().pushFlowVariable("posX", m_parameterPanel.getMouseCoordX());
        getNodeModel().pushFlowVariable("posY", m_parameterPanel.getMouseCoordY());
        getNodeModel().pushFlowVariable("zoomFactor", m_parameterPanel.getZoomFact());
        getNodeModel().pushFlowVariable("thresholdValue", m_parameterPanel.getThresholdVal());
        Map<String, Long> planeSel = m_parameterPanel.getPlaneSelection();
        for (Entry<String, Long> e : planeSel.entrySet()) {
            getNodeModel().pushFlowVariable("pos" + e.getKey(), e.getValue().intValue());
        }
        setNonExecutingView();
        m_imgViewer = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

    }

    private void setNonExecutingView() {

        final JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(new JLabel("Availables variables are:"));
        final Map<String, FlowVariable> variables = getNodeModel().getAvailableFlowVariables();
        for (final FlowVariable entry : variables.values()) {
            p.add(new JLabel(entry.toString()));
        }

        setComponent(p);

    }

    @EventListener
    public void onViewerScrollEvent(final ViewerScrollEvent e) {

        if (e.getDirection() == Direction.NORTH) {
            onCellSelectionChanged(m_row - 1, m_col);
        }
        if (e.getDirection() == Direction.EAST) {
            onCellSelectionChanged(m_row, m_col + 1);
        }
        if (e.getDirection() == Direction.WEST) {
            onCellSelectionChanged(m_row, m_col - 1);
        }
        if (e.getDirection() == Direction.SOUTH) {
            onCellSelectionChanged(m_row + 1, m_col);
        }

    }

    @EventListener
    public void onOverviewToggle(final ViewerControlEvent e) {
        if (getComponent() == m_content) {
            if (m_cellView.isTableViewVisible()) {
                m_cellView.hideTableView();
            }
            m_tableContentView.clearSelection();
            setComponent(m_tableViewPanel);
        }

    }

}
