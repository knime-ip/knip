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
package org.knime.knip.io.nodes.annotation.edit.control;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorDeleteAllEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorDeleteEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorHighlightEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorRemoveAllEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorRemoveEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorRenameAddedEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorToolChangedEvent;
import org.knime.knip.io.nodes.annotation.edit.tools.AbstractLabelingEditorTool;
import org.knime.knip.io.nodes.annotation.edit.tools.LabelingEditorModificationTool;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.roi.labeling.LabelingType;

public class LabelingEditorManager extends HiddenViewerComponent {

	/** default serial id */
	private static final long serialVersionUID = 1L;

	// Array to temporary hold the currently selected labels.
	private String[] m_selectedLabels;

	private RandomAccessible<? extends LabelingType<?>> m_currentOriginalLabeling;

	private PlaneSelectionEvent m_sel;

	// The managers event service
	private EventService m_eventService;

	// The selected Tool.
	private AbstractLabelingEditorTool m_currentTool;

	// The RowKey of the current row
	private RowColKey m_currentRowKey;

	private boolean m_filter = false;

	private Set<String> m_entriesToKeep;

	// The mapping of RowKeys to trackers
	private Map<RowColKey, LabelingEditorChangeTracker> m_IdTrackerMap;

	public LabelingEditorManager() {
		m_selectedLabels = new String[] { "Unknown" };
		m_IdTrackerMap = new HashMap<RowColKey, LabelingEditorChangeTracker>();
		// Only one tool for now.
		m_currentTool = new LabelingEditorModificationTool();
		RandomMissingColorHandler.setColor("#", LabelingColorTableUtils.NOTSELECTED_RGB);

	}

	@Override
	public void setEventService(final EventService eventService) {
		m_eventService = eventService;
		eventService.subscribe(this);

		// Both the tool and the trackers need to be listening to the
		// event-service as well.

		m_currentTool.setEventService(eventService);
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
		m_IdTrackerMap.get(k).clear();
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
	public void setTrackerMap(final Map<RowColKey, LabelingEditorChangeTracker> map) {
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
		final long[] dims = new long[e.getRandomAccessibleInterval().numDimensions()];
		e.getRandomAccessibleInterval().dimensions(dims);

		if ((m_sel == null) || !isInsideDims(m_sel.getPlanePos(), dims)) {
			m_sel = new PlaneSelectionEvent(0, 1, new long[e.getRandomAccessibleInterval().numDimensions()]);
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
			m_IdTrackerMap.put(m_currentRowKey, new LabelingEditorChangeTracker());

		LabelingEditorChangeTracker currentTracker = m_IdTrackerMap.get(m_currentRowKey);
		currentTracker.setFiltered(m_filter);
		currentTracker.setFilteredLabels(m_entriesToKeep);
	}


	public void setOriginalLabeling(final RandomAccessible<? extends LabelingType<?>> labeling) {
		m_currentOriginalLabeling = labeling;
	}

	/**
	 * Called when a label is DELETED from the labeling
	 * 
	 * @param e
	 */
	@EventListener
	public void onLabelDelete(final LabelingEditorDeleteEvent e) {
		LabelingEditorChangeTracker t = m_IdTrackerMap.get(m_currentRowKey);

		for (String label : e.getDeletedLabels())
			t.delete(label);
		
		m_eventService.publish(new ImgRedrawEvent());

	}

	/**
	 * Called when labels are removed from a labeling by the user.
	 * 
	 * @param e
	 *            The delete-event
	 */
	@EventListener
	public void onLabelRemove(final LabelingEditorRemoveEvent e) {

		LabelingEditorChangeTracker t = m_IdTrackerMap.get(m_currentRowKey);

		for (String label : e.getDeletedLabel())
			t.remove(e.getOldLabels(), label);
		m_eventService.publish(new ImgRedrawEvent());
	}

	@EventListener
	public void onLabelRemoveAll(final LabelingEditorRemoveAllEvent e) {

		LabelingEditorChangeTracker t = m_IdTrackerMap.get(m_currentRowKey);

		Set<String> curr = t.get(e.getDeletedLabels());

		for (String s : curr)
			t.remove(e.getDeletedLabels(), s);
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

		LabelingEditorChangeTracker t = m_IdTrackerMap.get(m_currentRowKey);

		for (String label : e.getNewLabels())
			t.insert(e.getOldLabels(), label);

		m_eventService.publish(new ImgRedrawEvent());

	}

	@EventListener
	public void onDeleteAll(final LabelingEditorDeleteAllEvent e) {
		for (String label : e.getDeletedLabels())
			for (LabelingEditorChangeTracker t : m_IdTrackerMap.values())
				t.delete(label);
	}

	@EventListener
	public void onLabelRename(final LabelingEditorRenameAddedEvent e) {
		LabelingEditorChangeTracker t = m_IdTrackerMap.get(m_currentRowKey);
		t.rename(e.getOldName(), e.getNewName());

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
	 * TBD
	 * 
	 * @param e
	 *            The mouse-event
	 */
	@EventListener
	public void onMousePressed(final ImgViewerMousePressedEvent e) {
		if ((m_currentOriginalLabeling != null) && (m_currentTool != null)) {
			RandomAccess<? extends LabelingType<?>> ra = m_currentOriginalLabeling.randomAccess();
			final long[] sel = m_sel.getPlanePos();

			sel[m_sel.getPlaneDimIndex1()] = e.getPosX();
			sel[m_sel.getPlaneDimIndex2()] = e.getPosY();
			ra.setPosition(sel);
			Set<String> labels = new HashSet<String>();
			for (Object o : ra.get()) {
				labels.add(o.toString());
			}
			m_currentTool.onMousePressed(e, labels, m_selectedLabels, e.isControlDown());
		}

	}

	/**
	 * Resets this manager back to its default state.
	 */
	@EventListener
	public void reset(final AnnotatorResetEvent e) {
		// Return to default
		m_selectedLabels = new String[] { "Unknown" };
		// for (final LabelingEditorChangeTracker tracker : m_IdTrackerMap
		// .values()) {
		// tracker.reset();
		// }

		m_IdTrackerMap.clear();
	}

	@Override
	public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
		// Intentionally left blank
	}

	@Override
	public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
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
			settings.addLongArray(prefix + "KEY_" + i + "_LABELINGDIMS", k.getLabelingDims());
			m_IdTrackerMap.get(s).saveSettingsTo(settings, i);
			++i;
		}
	}

	@EventListener
	/**
	 * Called when the user activates filtering
	 * 
	 * @param e
	 *            The triggering Event
	 */
	public void onFilterEnabled(final LabelingEditorHighlightEvent e) {
		if (e.isFilterEnabled()) {
			m_entriesToKeep = new HashSet<String>(e.getHighlightedLabels());
			m_filter = true;
		} else {
			m_entriesToKeep = null;
			m_filter = false;
		}
		LabelingEditorChangeTracker currentTracker = m_IdTrackerMap.get(m_currentRowKey);
		currentTracker.setFilteredLabels(m_entriesToKeep);
		currentTracker.setFiltered(m_filter);
	}

	/**
	 * Loads the settings contained in the given NodeSettings.
	 * 
	 * @param settings
	 *            The settings to load from
	 * @throws InvalidSettingsException
	 */
	public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		final String prefix = "LABELING_ANNOTATOR_MANAGER_";
		final int numRows = settings.getInt(prefix + "NUMROWS");

		for (int i = 0; i < numRows; ++i) {
			final RowColKey key = new LabelingEditorRowKey(settings.getString(prefix + "KEY_" + i + "_ROW"),
					settings.getLongArray(prefix + "KEY_" + i + "_LABELINGDIMS"));
			final LabelingEditorChangeTracker val = new LabelingEditorChangeTracker();

			val.loadSettingsFrom(settings, i);
			m_IdTrackerMap.put(key, val);
		}
	}

}
