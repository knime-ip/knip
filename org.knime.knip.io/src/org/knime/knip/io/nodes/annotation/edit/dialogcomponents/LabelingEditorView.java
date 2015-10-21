package org.knime.knip.io.nodes.annotation.edit.dialogcomponents;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;

import org.knime.core.data.DataCell;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableView;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.nodes.view.PlainCellView;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ExpandingPanel;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorMinimapAndPlaneSelectionPanel;
import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorRowColKeyChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelingWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.TableOverviewDisableEvent;
import org.knime.knip.core.ui.imgviewer.panels.ImgNormalizationPanel;
import org.knime.knip.core.ui.imgviewer.panels.RendererSelectionPanel;
import org.knime.knip.core.ui.imgviewer.panels.TransparencyPanel;
import org.knime.knip.core.ui.imgviewer.panels.infobars.LabelingViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.providers.AWTImageProvider;
import org.knime.knip.core.ui.imgviewer.panels.providers.CombinedRU;
import org.knime.knip.core.ui.imgviewer.panels.providers.ImageRU;
import org.knime.knip.io.nodes.annotation.AbstractDefaultAnnotatorView;
import org.knime.knip.io.nodes.annotation.AnnotatorView;
import org.knime.knip.io.nodes.annotation.deprecated.AnnotatorImgCanvas;
import org.knime.knip.io.nodes.annotation.edit.SettingsModelLabelEditor;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorChangeTracker;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorLabelingAccess;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorLabelingConverter;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorManager;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorRowKey;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorResetRowEvent;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ConstantUtils;
import net.imglib2.view.Views;

public class LabelingEditorView<T extends RealType<T> & NativeType<T>, L extends Comparable<L>> extends
		AbstractDefaultAnnotatorView<LabelingEditorChangeTracker>implements AnnotatorView<LabelingEditorChangeTracker> {

	public static final String FIXED_COL = "FirstMatch";

	private LabelingEditorManager m_annotationManager;

	private LabelingEditorLabelingRU m_renderUnit;

	private LabelingEditorLabelPanel m_labelPanel;

	private EventService m_eventService;

	private RandomAccessibleInterval<LabelingType<String>> m_currentChangeLabeling;

	private RandomAccessibleInterval<LabelingType<String>> m_currentStringLabeling;

	private LabelingValue<L> m_currentCell;

	private RowColKey m_currentKey;

	private Set<String> m_currentLabels;

	public LabelingEditorView(final SettingsModelLabelEditor sm) {
		m_annotationManager = sm.getManager();
		m_labelPanel = new LabelingEditorLabelPanel(sm.getNewLabels());
		createAnnotator();

	}

	// AnnotatorView

	@Override
	public LabelingEditorChangeTracker getAnnotation(final RowColKey key) {
		return m_annotationManager.getTrackerMap().get(key);
	}

	@Override
	public void setAnnotation(final RowColKey key, final LabelingEditorChangeTracker annotation) {

		m_annotationManager.getTrackerMap().put(key, annotation);
	}

	@Override
	public List<RowColKey> getIdentifiersOfManagedSources() {

		// Fetch all modified Changetrackers from the manager and return them
		return new LinkedList<RowColKey>(m_annotationManager.getTrackerMap().keySet());
	}
	// AbstractDefaultAnnotatorView

	@Override
	protected void createAnnotator() {
		// table viewer
		m_tableContentView = new TableContentView();
		m_tableContentView.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_tableContentView.getSelectionModel().addListSelectionListener(this);
		m_tableContentView.getColumnModel().getSelectionModel().addListSelectionListener(this);
		TableView tableView = new TableView(m_tableContentView);

		m_mainPanel.removeAll();

		m_mainPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridheight = GridBagConstraints.REMAINDER;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		PlainCellView v = new PlainCellView(tableView, (ImgViewer) createAnnotatorComponent());
		v.setEventService(m_eventService);
		m_eventService.publish(new TableOverviewDisableEvent(false, false));
		m_mainPanel.add(v, gbc);
	}

	@Override
	protected JComponent createAnnotatorComponent() {
		final ImgViewer annotator = new ImgViewer();
		annotator.addViewerComponent(new AWTImageProvider(0,
				new CombinedRU(new ImageRU<T>(true), m_renderUnit = new LabelingEditorLabelingRU())));
		annotator.addViewerComponent(m_annotationManager);

		annotator.addViewerComponent(new AnnotatorMinimapAndPlaneSelectionPanel());
		annotator.addViewerComponent(m_labelPanel, true, true);
		annotator.addViewerComponent(new ExpandingPanel("Normalization", new ImgNormalizationPanel<T, Img<T>>()));
		annotator.addViewerComponent(new ExpandingPanel("Renderer Selection", new RendererSelectionPanel<T>()));
		annotator.addViewerComponent(new ExpandingPanel("Transparency", new TransparencyPanel()));
		annotator.addViewerComponent(new LabelingViewInfoPanel<>());
		annotator.addViewerComponent(new AnnotatorImgCanvas<T>());
		annotator.doneAdding();
		m_eventService = annotator.getEventService();
		m_eventService.subscribe(this);
		return annotator;
	}

	private RandomAccessible<LabelingType<String>> m_lab;

	@SuppressWarnings("unchecked")
	@Override
	protected void currentSelectionChanged(final DataCell[] currentRow, final int currentColNr, final RowColKey key) {

		ImgPlusCell<T> imgPlusCell = null;

		// We're only interested in rows.

		// If there is a missing cell, abort.
		for (final DataCell c : currentRow) {
			if (c.isMissing()) {
				return;
			}
		}

		if (currentRow.length == 2) {
			// Labeling and image
			if (currentRow[0] instanceof ImgPlusValue) {
				imgPlusCell = (ImgPlusCell<T>) currentRow[0];
				m_currentCell = (LabelingValue<L>) currentRow[1];
			} else {
				imgPlusCell = (ImgPlusCell<T>) currentRow[1];
				m_currentCell = (LabelingValue<L>) currentRow[0];
			}

			m_currentKey = new LabelingEditorRowKey(key.getRowName(), m_currentCell.getDimensions());
			m_eventService.publish(
					new ImgWithMetadataChgEvent<T>(imgPlusCell.getImgPlus().getImg(), imgPlusCell.getMetadata()));
		} else {
			m_currentCell = (LabelingValue<L>) currentRow[0];
			final Img<BitType> view = new ImgView<BitType>(Views.interval(
					ConstantUtils.constantRandomAccessible(new BitType(true), m_currentCell.getDimensions().length),
					m_currentCell.getLabeling()), null);

			m_currentKey = new LabelingEditorRowKey(key.getRowName(), m_currentCell.getDimensions());

			m_eventService.publish(new ImgWithMetadataChgEvent<BitType>(view,
					new DefaultImgMetadata(m_currentCell.getDimensions().length)));
		}

		final LabelingEditorChangeTracker currTrack = m_annotationManager.getTracker(m_currentKey);

		RandomAccessible<LabelingType<String>> converterLabeling = Converters.convert(m_currentCell.getLabeling(),
				new LabelingEditorLabelingConverter<L>(currTrack),
				m_currentCell.getLabeling().randomAccess().get().createVariable(String.class));
		RandomAccessibleInterval<? extends LabelingType<?>> originalLabeling = m_currentCell.getLabeling();

		m_annotationManager.setLabeling(converterLabeling);
		m_annotationManager.setOriginalLabeling(originalLabeling);
		m_annotationManager.setMap(currTrack.getMap());

		// Set labels in the LabelPanel
		final Set<String> labels = currTrack.getCurrentLabels(
				LabelingEditorLabelingAccess.getLabels(originalLabeling.randomAccess().get().getMapping()));

		m_labelPanel.setCurrentLabels(labels);
		//
		// // Broadcast labeling to all components
		final LabelingMetadata meta = new DefaultLabelingMetadata(m_currentCell.getLabelingMetadata());
		m_eventService.publish(new AnnotatorRowColKeyChgEvent(m_currentKey));
		m_eventService.publish(
				new LabelingWithMetadataChgEvent(Views.interval(converterLabeling, m_currentCell.getLabeling()), meta));
		m_eventService.publish(new ImgRedrawEvent());
		//
		m_renderUnit.setTracker(currTrack);
	}

	@Override
	protected EventService getEventService() {
		return m_eventService;
	}

	@EventListener
	public void onRowReset(final LabelingEditorResetRowEvent e) {
		m_annotationManager.resetTrackerMap(m_currentKey);
		m_labelPanel.clearLabels();

		if (m_currentCell != null) {
			final Set<String> labels = m_annotationManager.getTracker(m_currentKey)
					.getCurrentLabels(LabelingEditorLabelingAccess
							.getLabels(m_currentCell.getLabeling().randomAccess().get().getMapping()));
			m_labelPanel.setCurrentLabels(labels);

		}

		// m_eventService.publish(new LabelingEditorLabelingModifiedEvent());
		m_eventService.publish(new ImgRedrawEvent());
	}

}
