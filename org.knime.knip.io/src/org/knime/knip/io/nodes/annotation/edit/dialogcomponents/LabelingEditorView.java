package org.knime.knip.io.nodes.annotation.edit.dialogcomponents;

import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.ConstantUtils;
import net.imglib2.view.Views;

import org.knime.core.data.DataCell;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.annotator.AnnotatorMinimapPanel;
import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorRowColKeyChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelingWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.panels.ImgNormalizationPanel;
import org.knime.knip.core.ui.imgviewer.panels.PlaneSelectionPanel;
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
import org.knime.knip.io.nodes.annotation.edit.control.EditorLabeling;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorChangeTracker;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorManager;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorRowKey;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorResetRowEvent;

public class LabelingEditorView<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
		extends AbstractDefaultAnnotatorView<LabelingEditorChangeTracker>
		implements AnnotatorView<LabelingEditorChangeTracker> {

	public static final String FIXED_COL = "FirstMatch";

	private LabelingEditorManager m_annotationManager;

	private LabelingEditorLabelingRU m_renderUnit;

	private LabelingEditorLabelPanel m_labelPanel;

	private EventService m_eventService;

	private RandomAccessibleInterval<LabelingType<String>> m_currentChangeLabeling;

	private RandomAccessibleInterval<LabelingType<String>> m_currentStringLabeling;

	private LabelingValue<L> m_currentCell;

	private RowColKey m_currentKey;

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
	public void setAnnotation(final RowColKey key,
			final LabelingEditorChangeTracker annotation) {

		m_annotationManager.getTrackerMap().put(key, annotation);
	}

	@Override
	public List<RowColKey> getIdentifiersOfManagedSources() {

		// Fetch all modified Changetrackers from the manager and return them
		return new LinkedList<RowColKey>(m_annotationManager.getTrackerMap()
				.keySet());
	}

	// AbstractDefaultAnnotatorView

	@Override
	protected JComponent createAnnotatorComponent() {
		final ImgViewer annotator = new ImgViewer();
		annotator.addViewerComponent(new AWTImageProvider(0, new CombinedRU(
				new ImageRU<T>(true),
				m_renderUnit = new LabelingEditorLabelingRU())));
		annotator.addViewerComponent(m_annotationManager);
		annotator.addViewerComponent(m_labelPanel);
		annotator.addViewerComponent(new AnnotatorMinimapPanel());
		annotator.addViewerComponent(new ImgNormalizationPanel<T, Img<T>>());
		annotator.addViewerComponent(new PlaneSelectionPanel<T, Img<T>>());
		annotator.addViewerComponent(new RendererSelectionPanel<T>());
		annotator.addViewerComponent(new TransparencyPanel());
		annotator.addViewerComponent(new LabelingViewInfoPanel<>());
		annotator.addViewerComponent(new AnnotatorImgCanvas<T>());
		m_eventService = annotator.getEventService();
		m_eventService.subscribe(this);
		return annotator;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void currentSelectionChanged(final DataCell[] currentRow,
			final int currentColNr, final RowColKey key) {

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

			m_currentKey = new LabelingEditorRowKey(key.getRowName(),
					m_currentCell.getDimensions());
			m_eventService.publish(new ImgWithMetadataChgEvent<T>(imgPlusCell
					.getImgPlus().getImg(), imgPlusCell.getMetadata()));
		} else {
			m_currentCell = (LabelingValue<L>) currentRow[0];
			final Img<BitType> view = new ImgView<BitType>(Views.interval(
					ConstantUtils.constantRandomAccessible(new BitType(true),
							m_currentCell.getDimensions().length),
					m_currentCell.getLabeling()), null);

			m_currentKey = new LabelingEditorRowKey(key.getRowName(),
					m_currentCell.getDimensions());

			m_eventService
					.publish(new ImgWithMetadataChgEvent<BitType>(view,
							new DefaultImgMetadata(m_currentCell
									.getDimensions().length)));
		}

		// Convert the input labeling into a string-based labeling using the
		// underlying changetrackers to keep up to date with user changes

		final LabelingEditorChangeTracker currTrack = m_annotationManager
				.getTracker(m_currentKey);

		// Ensure that the complete labeling is loaded
		currTrack.disableFiltering();
		
		EditorLabeling lab = new EditorLabeling((ImgLabeling<L, IntType>) m_currentCell.getLabeling());
		
//		ImgLabeling<L, IntType> t_lab = (ImgLabeling<L, IntType>) m_currentCell.getLabeling();
//		
//		LabelingMapping<L> map = t_lab.getMapping();
//		
//		LabelingType<L> t_l = t_lab.firstElement();

//		RandomAccessibleInterval<LabelingType<String>> conv = Converters
//				.convert(
//						Converters
//								.convert(
//										(RandomAccessibleInterval<LabelingType<L>>) m_currentCell
//												.getLabeling(),
//										new ToStringLabelingConverter<L>(),
//										new LabelingType<String>()), currTrack,
//						(LabelingType<String>) new LELabelingType(
//								m_eventService));
//
//		final Labeling<String> convertedLabeling = new LabelingView<String>(
//				conv, null);
//
//		final Labeling<String> stringLabeling = new LabelingView<String>(
//				Converters.convert(
//						(RandomAccessibleInterval<LabelingType<L>>) m_currentCell
//								.getLabeling(),
//						new ToStringLabelingConverter<L>(),
//						new LabelingType<String>()), null);

		m_annotationManager.setLabeling(lab);
//
//		m_currentChangeLabeling = convertedLabeling;
//
//		m_currentStringLabeling = stringLabeling;
//
//		// Required to initialize the labeling.
//		@SuppressWarnings("unused")
//		final List<String> dummy = new LinkedList<String>(
//				m_currentChangeLabeling.getLabels());
//
//		// Set labels in the LabelPanel
//		m_labelPanel.clearLabels();
//		final List<String> labels = new LinkedList<String>(
//				m_currentStringLabeling.getLabels());
//
//		Collections.sort(labels);
//		m_labelPanel.addLabels(labels);
//
//		// Broadcast labeling to all components
		final LabelingMetadata meta = new DefaultLabelingMetadata(
				m_currentCell.getLabelingMetadata());
		m_eventService.publish(new AnnotatorRowColKeyChgEvent(m_currentKey));
		m_eventService.publish(new LabelingWithMetadataChgEvent<String>(
				lab, meta));
		m_eventService.publish(new ImgRedrawEvent());
//
//		m_renderUnit.setTracker(currTrack);
	}

	@Override
	protected EventService getEventService() {
		return m_eventService;
	}

	@EventListener
	public void onRowReset(final LabelingEditorResetRowEvent e) {
//		m_annotationManager.resetTrackerMap(m_currentKey);
//		m_labelPanel.clearLabels();
//
//		if (m_currentStringLabeling != null) {
//			final List<String> labels = new LinkedList<String>(
//					m_currentStringLabeling.getLabels());
//			Collections.sort(labels);
//			m_labelPanel.addLabels(labels);
//		}
//
//		// m_eventService.publish(new LabelingEditorLabelingModifiedEvent());
//		m_eventService.publish(new ImgRedrawEvent());
	}

}
