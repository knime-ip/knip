package org.knime.knip.io.nodes.annotation.edit.control;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imglib2.RandomAccess;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;

import ome.xml.model.AnnotationRef;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableUtils;
import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsSelChgEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorResetEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorRowColKeyChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerMousePressedEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.panels.HiddenViewerComponent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorAddEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorDeleteAddedEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorDeleteEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorHighlightEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorRenameAddedEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorToolChangedEvent;
import org.knime.knip.io.nodes.annotation.edit.tools.AbstractLabelingEditorTool;
import org.knime.knip.io.nodes.annotation.edit.tools.LabelingEditorModificationTool;

public class LabelingEditorManager extends HiddenViewerComponent {

	/** default serial id */
	private static final long serialVersionUID = 1L;

	// Array to temporary hold the currently selected labels.
	private String[] m_selectedLabels;

	// The currently selected labeling
	private Labeling<String> m_currentLabeling;

	private PlaneSelectionEvent m_sel;

	// The managers event service
	private EventService m_eventService;

	// The selected Tool.
	private AbstractLabelingEditorTool m_currentTool;

	// The RowKey of the current row
	private RowColKey m_currentRowKey;

	private boolean m_filter = false;

	private List<String> m_entriesToKeep;

	// The mapping of RowKeys to trackers
	private Map<RowColKey, LabelingEditorChangeTracker> m_IdTrackerMap;

	public LabelingEditorManager() {
		m_selectedLabels = new String[] { "Unknown" };
		m_IdTrackerMap = new HashMap<RowColKey, LabelingEditorChangeTracker>();
		// Only one tool for now.
		m_currentTool = new LabelingEditorModificationTool();
		RandomMissingColorHandler.setColor("#",
				LabelingColorTableUtils.NOTSELECTED_RGB);

	}

	@Override
	public void setEventService(final EventService eventService) {
		m_eventService = eventService;
		eventService.subscribe(this);

		// Both the tool and the trackers need to be listening to the
		// event-service as well.

		m_currentTool.setEventService(eventService);
		for (final LabelingEditorChangeTracker t : m_IdTrackerMap.values())
			t.setEventService(eventService);
	}

	/**
	 * Retrieve the Map containing ALL trackers.
	 * 
	 * @return A Map with all trackers known to this manager
	 */
	public Map<RowColKey, LabelingEditorChangeTracker> getTrackerMap() {
		return m_IdTrackerMap;
	}

	public void resetTrackerMap(RowColKey k) {
		m_IdTrackerMap.get(k).reset();
	}

	/**
	 * Obtain a specific tracker for a certain row
	 * 
	 * @param key
	 *            The key of the target row
	 * @return The tracker of the given row
	 */
	public LabelingEditorChangeTracker getTracker(final RowColKey key) {
		LabelingEditorChangeTracker result = m_IdTrackerMap.get(key);
		if (result == null) {
			result = new LabelingEditorChangeTracker();
			result.setEventService(m_eventService);
			m_IdTrackerMap.put(key, result);
		}

		return result;
	}

	/**
	 * Set the Map of known trackers.
	 * 
	 * @param map
	 *            The map to use in this manager
	 */
	public void setTrackerMap(
			final Map<RowColKey, LabelingEditorChangeTracker> map) {
		m_IdTrackerMap = map;
	}

	/**
	 * Called, when the selected labels are changed.
	 * 
	 * @param e
	 *            The change-event
	 */
	@EventListener
	public void onSelectedLabelsChg(final AnnotatorLabelsSelChgEvent e) {
		m_selectedLabels = e.getLabels();
	}

	/**
	 * Called when the selected tool is changed. Note: Currently not possible.
	 * 
	 * @param e
	 *            The change-event
	 */
	@EventListener
	public void onToolChange(final LabelingEditorToolChangedEvent e) {
		m_currentTool = e.getTool();
	}

	@EventListener
	public void onUpdate(final IntervalWithMetadataChgEvent<?, ?> e) {
		final long[] dims = new long[e.getRandomAccessibleInterval()
				.numDimensions()];
		e.getRandomAccessibleInterval().dimensions(dims);

		if ((m_sel == null) || !isInsideDims(m_sel.getPlanePos(), dims)) {
			m_sel = new PlaneSelectionEvent(0, 1, new long[e
					.getRandomAccessibleInterval().numDimensions()]);
		}
	}

	/**
	 * Called, when the selected row changes. <br>
	 * 
	 * @param e
	 *            The change-event
	 */
	@EventListener
	public void onRowChange(final AnnotatorRowColKeyChgEvent e) {
		m_currentRowKey = e.getKey();

		// Make sure the row has a mapping.
		if (!m_IdTrackerMap.containsKey(m_currentRowKey))
			m_IdTrackerMap.put(m_currentRowKey,
					new LabelingEditorChangeTracker());

		updateFiltering();
	}

	public void setLabeling(final Labeling<String> labeling) {
		m_currentLabeling = labeling;
	}

	/**
	 * Called when labels are removed from a labeling by the user.
	 * 
	 * @param e
	 *            The delete-event
	 */
	@EventListener
	public void onLabelRemove(final LabelingEditorDeleteEvent e) {
		final List<String> labels = new LinkedList<String>(
				e.getModifiedLabels());

		// Labelings have to be sorted!
		Collections.sort(labels);

		final List<String> removedLabels = new LinkedList<String>(
				e.getDeletedLabel());

		LabelingEditorChangeTracker tracker = m_IdTrackerMap
				.get(m_currentRowKey);
		tracker.remove(labels, removedLabels);

		m_eventService.publish(new ImgRedrawEvent());
	}

	/**
	 * Called, when a Label is added to a labeling by the user.
	 * 
	 * @param e
	 *            The add-event.
	 */
	@EventListener
	public void onLabelAdd(final LabelingEditorAddEvent e) {
		final List<String> labels = new LinkedList<String>(
				e.getModifiedLabels());

		final List<String> addedLabels = new LinkedList<String>(
				e.getNewLabels());

		// Labelings have to be sorted!
		Collections.sort(labels);

		m_IdTrackerMap.get(m_currentRowKey).insert(labels, addedLabels);

		m_eventService.publish(new ImgRedrawEvent());
	}

	@EventListener
	/**
	 * Triggered when the user deletes a new label in the dialog.
	 * @param e The triggering event
	 */
	public void onAddedLabelDelete(final LabelingEditorDeleteAddedEvent e) {
		for (LabelingEditorChangeTracker t : m_IdTrackerMap.values()) {
			t.delete(e.getDeletedLabels());
		}
	}

	@EventListener
	/**
	 * Triggered when the user renames a new label in the dialog.
	 * @param e The triggering event
	 */
	public void onAddedLabelRename(final LabelingEditorRenameAddedEvent e) {
		for (LabelingEditorChangeTracker t : m_IdTrackerMap.values()) {
			t.rename(e.getOldName(), e.getNewName());
		}
	}

	@EventListener
	public void onUpdate(final PlaneSelectionEvent sel) {
		m_sel = sel;
	}

	private boolean isInsideDims(final long[] planePos, final long[] dims) {
		if (planePos.length != dims.length) {
			return false;
		}

		for (int d = 0; d < planePos.length; d++) {
			if (planePos[d] >= dims[d]) {
				return false;
			}
		}

		return true;
	}

	/**
	 * This function handles mouse input. The selected labels are computed and
	 * passed on to the currently selected tool.
	 * 
	 * @param e
	 *            The mouse-event
	 */
	@EventListener
	public void onMousePressed(final ImgViewerMousePressedEvent e) {
		Collection<String> labels = new HashSet<String>();
		if ((m_currentLabeling != null) && (m_currentTool != null)) {
			RandomAccess<LabelingType<String>> ra = m_currentLabeling
					.randomAccess();

			final long[] sel = m_sel.getPlanePos();

			sel[m_sel.getPlaneDimIndex1()] = e.getPosX();
			sel[m_sel.getPlaneDimIndex2()] = e.getPosY();
			final long[] seld = new long[sel.length];
			for (int i = 0; i < seld.length; ++i) {
				if(sel[i] <= m_currentLabeling.realMax(i))
					seld[i] = sel[i];
				else
					seld[i] = (long)m_currentLabeling.realMax(i);
			}

			ra.setPosition(seld);
			labels = new HashSet<String>(ra.get().getLabeling());
		}
		if (labels.isEmpty())
			return;
		m_currentTool.onMousePressed(e, labels, m_selectedLabels);
	}

	/**
	 * Resets this manager back to its default state.
	 */
	@EventListener
	public void reset(final AnnotatorResetEvent e) {
		// Return to default
		m_selectedLabels = new String[] { "Unknown" };
//		for (final LabelingEditorChangeTracker tracker : m_IdTrackerMap
//				.values()) {
//			tracker.reset();
//		}
		
		m_IdTrackerMap.clear();
	}

	@Override
	public void saveComponentConfiguration(final ObjectOutput out)
			throws IOException {
		// Intentionally left blank
	}

	@Override
	public void loadComponentConfiguration(final ObjectInput in)
			throws IOException, ClassNotFoundException {
		// Intentionally left blank
	}

	/**
	 * Returns all the Rows known to this tracker.
	 * 
	 * @return A set containing all row keys
	 */
	public Set<RowColKey> getInternedRows() {
		return m_IdTrackerMap.keySet();
	}

	/**
	 * Saves this managers settings to the given NodeSettings.
	 * 
	 * @param settings
	 *            The settings to save to
	 */
	public void saveSettingsTo(final NodeSettingsWO settings) {
		final String prefix = "LABELING_ANNOTATOR_MANAGER_";
		final int numRows = m_IdTrackerMap.keySet().size();
		settings.addInt(prefix + "NUMROWS", numRows);
		int i = 0;
		for (final RowColKey s : m_IdTrackerMap.keySet()) {
			LabelingEditorRowKey k = (LabelingEditorRowKey) s;
			settings.addString(prefix + "KEY_" + i + "_ROW", k.getRowName());
			settings.addLongArray(prefix + "KEY_" + i + "_LABELINGDIMS",
					k.getLabelingDims());
			m_IdTrackerMap.get(s).saveSettingsTo(settings, i);
			++i;
		}
	}

	@EventListener
	/**
	 * Called when the user activates filtering
	 * @param e The triggering Event
	 */
	public void onFilterEnabled(final LabelingEditorHighlightEvent e) {
		if (e.isFilterEnabled()) {
			m_entriesToKeep = new LinkedList<>(e.getHighlightedLabels());
			m_filter = true;
		} else {
			m_entriesToKeep = null;
			m_filter = false;
		}
		updateFiltering();
	}

	/*
	 * Propagates filter settings to all trackers.
	 */
	private void updateFiltering() {
		LabelingEditorChangeTracker currentTracker = m_IdTrackerMap
				.get(m_currentRowKey);
		if (currentTracker != null) {
			if (m_filter) {
				currentTracker.enableFiltering(m_entriesToKeep);
			} else
				currentTracker.disableFiltering();
		}
	}

	/**
	 * Loads the settings contained in the given NodeSettings.
	 * 
	 * @param settings
	 *            The settings to load from
	 * @throws InvalidSettingsException
	 */
	public void loadSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		final String prefix = "LABELING_ANNOTATOR_MANAGER_";
		final int numRows = settings.getInt(prefix + "NUMROWS");

		for (int i = 0; i < numRows; ++i) {
			final RowColKey key = new LabelingEditorRowKey(
					settings.getString(prefix + "KEY_" + i + "_ROW"),
					settings.getLongArray(prefix + "KEY_" + i + "_LABELINGDIMS"));
			final LabelingEditorChangeTracker val = new LabelingEditorChangeTracker();

			if (m_eventService != null)
				val.setEventService(m_eventService);

			val.loadSettingsFrom(settings, i);
			m_IdTrackerMap.put(key, val);
		}
	}

}
