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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableView;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.core.ui.imgviewer.ImgCanvas;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.panels.ImgNormalizationPanel;
import org.knime.knip.core.ui.imgviewer.panels.MinimapPanel;
import org.knime.knip.core.ui.imgviewer.panels.PlaneSelectionPanel;
import org.knime.knip.core.ui.imgviewer.panels.infobars.ImgViewInfoPanel;

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

        final TableView tableView = new TableView(m_tableContentView);

        // scale to thumbnail size, if desired
        final int TABLE_CELL_SIZE = 100;
        tableView.setColumnWidth(TABLE_CELL_SIZE);
        tableView.setRowHeight(TABLE_CELL_SIZE);

        // contructing the image viewer
        final int CACHE_SIZE = 50;
        m_imgViewer = new ImgViewer();
        final ThresholdBufferedImageProvider<T> realProvider = new ThresholdBufferedImageProvider<T>(CACHE_SIZE);
        realProvider.setEventService(m_imgViewer.getEventService());

        m_imgViewer.addViewerComponent(new ImgViewInfoPanel<T>());
        m_imgViewer.addViewerComponent(new ImgCanvas<T, Img<T>>());

        m_imgViewer.addViewerComponent(new MinimapPanel());
        m_imgViewer.addViewerComponent(new PlaneSelectionPanel<T, Img<T>>());
        m_imgViewer.addViewerComponent(new ImgNormalizationPanel<T, Img<T>>());
        m_imgViewer.addViewerComponent(new SetThresholdPanel<T, Img<T>>());
        m_parameterPanel = new ParameterPanel();
        m_imgViewer.addViewerComponent(m_parameterPanel);

        final JPanel content = new JPanel(new BorderLayout());
        content.add(m_imgViewer, BorderLayout.CENTER);

        final JButton continueButton = new JButton("Set parameters and continue execution ...");
        continueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onContinueExecution();
            }
        });
        content.add(continueButton, BorderLayout.SOUTH);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.add(tableView);
        splitPane.add(content);

        setComponent(splitPane);

        // let this class listening to the imgViewer events (registers
        // the
        // onNormalization... method the the event service of the
        // imgviewer)
        m_imgViewer.getEventService().subscribe(this);
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

    /**
     * @param row
     * @param col
     */
    protected void onCellSelectionChanged() {

        final int row = m_tableContentView.getSelectionModel().getLeadSelectionIndex();
        final int col = m_tableContentView.getColumnModel().getSelectionModel().getLeadSelectionIndex();
        final ImgPlus<T> imgPlus =
                ((ImgPlusValue<T>)m_tableContentView.getContentModel().getValueAt(row, col)).getImgPlus();
        m_imgViewer.setImg(imgPlus);

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

}
