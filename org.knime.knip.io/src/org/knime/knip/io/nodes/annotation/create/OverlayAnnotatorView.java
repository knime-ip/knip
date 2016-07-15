/*
 * ------------------------------------------------------------------------
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
package org.knime.knip.io.nodes.annotation.create;

import java.awt.BorderLayout;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

import org.knime.core.data.DataCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ExpandingPanel;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorMinimapAndPlaneSelectionPanel;
import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorToolbar;
import org.knime.knip.core.ui.imgviewer.annotator.OverlayAnnotatorManager;
import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;
import org.knime.knip.core.ui.imgviewer.annotator.create.AnnotatorLabelPanel;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorResetEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorRowColKeyChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.TablePositionEvent;
import org.knime.knip.core.ui.imgviewer.overlay.Overlay;
import org.knime.knip.core.ui.imgviewer.overlay.OverlayElement2D;
import org.knime.knip.core.ui.imgviewer.panels.ImgNormalizationPanel;
import org.knime.knip.core.ui.imgviewer.panels.RendererSelectionPanel;
import org.knime.knip.core.ui.imgviewer.panels.TransparencyPanel;
import org.knime.knip.core.ui.imgviewer.panels.infobars.ImgViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.providers.AWTImageProvider;
import org.knime.knip.core.ui.imgviewer.panels.providers.ImageRU;
import org.knime.knip.core.ui.imgviewer.panels.providers.OverlayRU;
import org.knime.knip.io.nodes.annotation.AbstractDefaultAnnotatorView;
import org.knime.knip.io.nodes.annotation.AnnotatorView;
import org.knime.knip.io.nodes.annotation.deprecated.AnnotatorImgCanvas;

import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class OverlayAnnotatorView<T extends RealType<T> & NativeType<T>> extends AbstractDefaultAnnotatorView<Overlay>
		implements AnnotatorView<Overlay> {

	private OverlayAnnotatorManager<T> m_manager;

	private EventService m_eventService;

	private AnnotatorLabelPanel m_annotatorLabelPanel;

	public OverlayAnnotatorView() {
		m_manager = new OverlayAnnotatorManager<T>();
		createAnnotator();
	}

	// AnnotatorView

	@Override
	public Overlay getAnnotation(RowColKey key) {
		return m_manager.getOverlay(key);
	}

	@Override
	public void setAnnotation(RowColKey key, Overlay overlay) {

		if (m_manager.getOverlayMap().size() == 0) {
			m_annotatorLabelPanel.clearLabels();
		}

		// assumption overlays that should be added like this come from
		// serialization => they belong to the input table and they need a
		// reference to the event service
		overlay.setEventService(m_eventService);
		m_manager.addOverlay(key, overlay);

		for (OverlayElement2D o : overlay.getElements()) {
			m_annotatorLabelPanel.addLabels(o.getLabels());
		}

	}

	@Override
	public List<RowColKey> getIdentifiersOfManagedSources() {
		LinkedList<RowColKey> ret = new LinkedList<RowColKey>();
		Map<RowColKey, Overlay> map = m_manager.getOverlayMap();

		// add all none empty overlays
		for (RowColKey key : map.keySet()) {
			if (map.get(key).getElements().length > 0) {
				ret.add(key);
			}
		}

		return ret;
	}

	@Override
	public void reset() {
		m_eventService.publish(new AnnotatorResetEvent());
	}

	// AbstractDefaultAnnotator

	@SuppressWarnings("rawtypes")
	@Override
	protected JComponent createAnnotatorComponent() {
		ImgViewer annotator = new ImgViewer();
		annotator.addViewerComponent(new AWTImageProvider(0, new OverlayRU<String>(new ImageRU<T>(0.0))));
		annotator.addViewerComponent(m_manager);
		annotator.addViewerComponent(new AnnotatorImgCanvas<T>());
		annotator.addViewerComponent(new ExpandingPanel("Renderer Selection", new RendererSelectionPanel<T>()));
		annotator.addViewerComponent(new AnnotatorMinimapAndPlaneSelectionPanel());
		annotator.addViewerComponent(
				new ExpandingPanel("Labels", m_annotatorLabelPanel = new AnnotatorLabelPanel(), true));
		annotator.addViewerComponent(AnnotatorToolbar.createStandardToolbar());
		annotator.addViewerComponent(new ExpandingPanel("Brightness and Contrast", new ImgNormalizationPanel<>()));
		// annotator.addViewerComponent(new PlaneSelectionPanel<T, Img<T>>());

		annotator.addViewerComponent(new ExpandingPanel("Transparency", new TransparencyPanel()));
		annotator.addViewerComponent(new ExpandingPanel("Info", new ImgViewInfoPanel()));

		m_eventService = annotator.getEventService();
		m_eventService.subscribe(this);
		isViewActive = true;
		return annotator;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void currentSelectionChanged(DataCell[] currentRow, int currentColNr, RowColKey key) {
		if (!isViewActive) {
			m_mainPanel.removeAll();
			m_mainPanel.add(m_view, BorderLayout.CENTER);
			m_mainPanel.repaint();
			isViewActive = true;
		}
		// Filter for non-imgPlus-cells
		if (!currentRow[currentColNr].getType().isCompatible(ImgPlusValue.class))
			return;
		if (!currentRow[currentColNr].isMissing()) {

			ImgPlus<T> imgPlus = ((ImgPlusValue<T>) currentRow[currentColNr]).getImgPlus();

			m_eventService.publish(new ImgWithMetadataChgEvent(imgPlus.getImg(), imgPlus));
			m_eventService.publish(new AnnotatorRowColKeyChgEvent(key));
			m_eventService.publish(new ImgRedrawEvent());
			m_eventService.publish(
					new TablePositionEvent(m_tableContentView.getColumnCount(), m_tableContentView.getRowCount(),
							m_currentCol + 1, m_currentRow + 1, m_tableContentView.getColumnName(m_currentCol),
							m_tableContentView.getContentModel().getRowKey(m_currentRow).toString()));
		}
	}

	@Override
	protected EventService getEventService() {
		return m_eventService;
	}
}
