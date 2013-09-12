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
package org.knime.knip.io.nodes.annotation.interactive;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTable;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableView;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorLabelPanel;
import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorResetEvent;
import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorToolbar;
import org.knime.knip.core.ui.imgviewer.annotator.interactive.OverlayAnnotatorManager;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.overlay.Overlay;
import org.knime.knip.core.ui.imgviewer.panels.ImgNormalizationPanel;
import org.knime.knip.core.ui.imgviewer.panels.PlaneSelectionPanel;
import org.knime.knip.core.ui.imgviewer.panels.RendererSelectionPanel;
import org.knime.knip.core.ui.imgviewer.panels.TransparencyPanel;
import org.knime.knip.core.ui.imgviewer.panels.infobars.ImgViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.providers.OverlayBufferedImageProvider;
import org.knime.knip.io.nodes.annotation.AnnotatorImgCanvas;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class OverlayAnnotatorView<T extends RealType<T> & NativeType<T>>
		implements AnnotatorView<String>, ListSelectionListener {

	private final JPanel m_mainPanel = new JPanel();

	/*
	 * does not listen to events of the event service but may trigger them.
	 */
	private EventService m_eventService;

	private OverlayAnnotatorManager<T> m_manager;

	private TableContentView m_tableContentView;

	private int m_currentRow = -1;
	private int m_currentCol = -1;

	public OverlayAnnotatorView() {
		createAnnotator();
	}

	@Override
	public JPanel getAnnotatorPanel() {
		return m_mainPanel;
	}

	@Override
	public Overlay<String> getOverlay(String srcName) {
		return m_manager.getOverlay(srcName);
	}

	@Override
	public void setOverlay(String srcName, Overlay<String> overlay) {
		// assumption overlays that should be added like this come from
		// serialization => they belong to the input table and they need a
		// reference to the event service
		overlay.setEventService(m_eventService);
		m_manager.addOverlay(srcName, overlay);
	}

	@Override
	public void reset() {
		m_eventService.publish(new AnnotatorResetEvent());
		m_currentRow = -1;
		m_currentCol = -1;
	}

	@Override
	public List<String> getOverlaySrcNames() {
		LinkedList<String> ret = new LinkedList<String>();
		Map<String, Overlay<String>> map = m_manager.getOverlayMap();

		// add all none empty overlays
		for (String srcName : map.keySet()) {
			if (map.get(srcName).getElements().length > 0) {
				ret.add(srcName);
			}
		}

		return ret;
	}

	@Override
	public void setInputTable(DataTable inputTable) {
		m_tableContentView.setModel(new TableContentModel(inputTable));
		// Scale to thumbnail size
		m_tableContentView.validate();
		m_tableContentView.repaint();
	}

	private void createAnnotator() {
		// table viewer
		m_tableContentView = new TableContentView();
		m_tableContentView.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
		m_tableContentView.getSelectionModel().addListSelectionListener(this);
		m_tableContentView.getColumnModel().getSelectionModel()
				.addListSelectionListener(this);
		TableView tableView = new TableView(m_tableContentView);

		// annotator
		m_manager = new OverlayAnnotatorManager<T>();
		ImgViewer annotator = new ImgViewer();

		annotator
				.addViewerComponent(new OverlayBufferedImageProvider<T, String>());
		annotator.addViewerComponent(m_manager);
		annotator.addViewerComponent(new AnnotatorLabelPanel());
		annotator.addViewerComponent(AnnotatorToolbar.createStandardToolbar());
		annotator.addViewerComponent(new AnnotatorMinimapPanel());
		annotator.addViewerComponent(new ImgNormalizationPanel<T, Img<T>>());
		annotator.addViewerComponent(new PlaneSelectionPanel<T, Img<T>>());
		annotator.addViewerComponent(new RendererSelectionPanel<T>());
		annotator.addViewerComponent(new TransparencyPanel());
		annotator.addViewerComponent(new ImgViewInfoPanel<T>());
		annotator.addViewerComponent(new AnnotatorImgCanvas<T>());

		m_eventService = annotator.getEventService();

		// split pane
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.add(tableView);
		splitPane.add(annotator);
		splitPane.setDividerLocation(300);

		m_mainPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;

		m_mainPanel.add(splitPane, gbc);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		final int row = m_tableContentView.getSelectionModel()
				.getLeadSelectionIndex();
		final int col = m_tableContentView.getColumnModel().getSelectionModel()
				.getLeadSelectionIndex();

		if ((row == m_currentRow && col == m_currentCol)
				|| e.getValueIsAdjusting()) {
			return;
		}

		m_currentRow = row;
		m_currentCol = col;

		try {
			final DataCell currentImgCell = m_tableContentView
					.getContentModel().getValueAt(m_currentRow, m_currentCol);
			ImgPlus<T> imgPlus = ((ImgPlusValue<T>) currentImgCell)
					.getImgPlus();

			m_eventService.publish(new ImgWithMetadataChgEvent<T>(imgPlus,
					imgPlus));
			m_eventService.publish(new ImgRedrawEvent());
		} catch (final IndexOutOfBoundsException e2) {
			return;
		}
	}

}
