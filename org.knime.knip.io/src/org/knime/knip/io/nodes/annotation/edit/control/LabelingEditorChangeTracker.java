package org.knime.knip.io.nodes.annotation.edit.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.imglib2.converter.Converter;
import net.imglib2.labeling.LabelingType;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorListChangedEvent;

/**
 * This Class is an implementation of the @Converter interface, used to convert
 * labels into modified versions of themselves on the fly.
 * 
 * @author Andreas Burger, University of Konstanz
 * 
 */
public class LabelingEditorChangeTracker implements
		Converter<LabelingType<String>, LabelingType<String>> {

	/**
	 * This class maps the original list to its current representation and
	 * provides additional services such as renaming and filtering.
	 */

	// Maps the original labels to the current changed version. Uses Lists of
	// Strings for easy access.
	private Map<List<String>, List<String>> m_changedLabels = new HashMap<List<String>, List<String>>();

	private int m_generation = 0;

	private boolean m_filter = false;

	private List<String> m_entriesToKeep;

	private EventService m_EventService;

	/**
	 * Remove an entry from the modified list associated with a given list
	 * 
	 * @param list
	 *            The list from which to delete entries
	 * @param deletedEntries
	 *            The entries to delete
	 */
	public void remove(final List<String> list,
			final List<String> deletedEntries) {

		List<String> mappedEntry = m_changedLabels.get(list);

		if (mappedEntry != null) {
			boolean changed = mappedEntry.removeAll(deletedEntries);
			if (changed) {
				intern(mappedEntry);
				if (m_filter)
					checkForFiltered(mappedEntry);
				++m_generation;
			}
		} else {
			m_changedLabels.put(list, new LinkedList<String>(list));
			remove(list, deletedEntries);
		}

	}

	/**
	 * Insert an entry into the modified list associated with a given list
	 * 
	 * @param list
	 *            The list in which to insert entries
	 * @param addedEntries
	 *            The entries to add
	 */
	public void insert(final List<String> list, final List<String> addedEntries) {

		List<String> mappedEntry = m_changedLabels.get(list);

		if (mappedEntry != null) {
			boolean changed = false;
			for (String s : addedEntries)
				if (!mappedEntry.contains(s)) {
					mappedEntry.add(s);
					changed = true;
				}
			Collections.sort(mappedEntry);
			if (changed) {
				intern(mappedEntry);
				if (m_filter)
					checkForFiltered(mappedEntry);

				++m_generation;
			}
		} else {
			m_changedLabels.put(list, new LinkedList<String>(list));
			insert(list, addedEntries);
		}

	}

	private void intern(final List<String> listToIntern) {
		m_EventService
				.publish(new LabelingEditorListChangedEvent(listToIntern));
	}

	public void saveSettingsTo(final NodeSettingsWO settings, final int index) {

		final String prefix = "ANNOTATOR_TRACKER_" + index;

		saveMap(settings, prefix + "_MODIFICATIONS_", m_changedLabels);

	}

	/**
	 * Load the trackers settings.
	 * 
	 * @param settings
	 *            Settings to load from
	 * @param index
	 *            The index of the tracker, used do differentiate between
	 *            several trackers
	 * @throws InvalidSettingsException
	 */
	public void loadSettingsFrom(final NodeSettingsRO settings, final int index)
			throws InvalidSettingsException {
		final String prefix = "ANNOTATOR_TRACKER_" + index;

		loadLabelMaps(settings, prefix);

	}

	/**
	 * Saves the labeling-map
	 * 
	 * @param settings
	 *            Settings to save to
	 * @param prefix
	 *            Unique prefix, used to determine the right map of the right
	 *            tracker
	 * @param map
	 *            The map to save
	 */
	private void saveMap(final NodeSettingsWO settings, final String prefix,
			final Map<List<String>, List<String>> map) {

		final int internedLists = map.keySet().size();
		int i = 0;
		settings.addInt(prefix + "NUMSETS_INSERT", internedLists);
		final Iterator<List<String>> it = map.keySet().iterator();
		while (it.hasNext()) {
			final List<String> l = it.next();
			settings.addStringArray(prefix + "INSERT_KEY_" + i,
					l.toArray(new String[0]));
			final List<String> s = map.get(l);
			settings.addStringArray(prefix + "INSERT_VAL_" + i,
					s.toArray(new String[0]));
			++i;
		}
	}

	/**
	 * Loads the maps containing the label changes.
	 * 
	 * @param settings
	 *            Settings to load from
	 * @param prefix
	 *            Unique prefix, used to determine the right map of the right
	 *            tracker
	 * @throws InvalidSettingsException
	 */
	private void loadLabelMaps(final NodeSettingsRO settings,
			final String prefix) throws InvalidSettingsException {

		final String labelPrefix = prefix + "_MODIFICATIONS_";

		final Map<List<String>, List<String>> changedLabels = new HashMap<List<String>, List<String>>();

		final int internedLists = settings.getInt(labelPrefix
				+ "NUMSETS_INSERT");
		for (int i = 0; i < internedLists; ++i) {
			final String[] key = settings.getStringArray(labelPrefix
					+ "INSERT_KEY_" + i);
			final List<String> keyList = new LinkedList<String>();
			for (final String s : key)
				keyList.add(s);
			Collections.sort(keyList);

			final String[] val = settings.getStringArray(labelPrefix
					+ "INSERT_VAL_" + i);
			final List<String> valSet = new LinkedList<String>();
			for (final String s : val)
				valSet.add(s);

			changedLabels.put(keyList, valSet);
		}
		m_changedLabels = changedLabels;
	}

	/**
	 * Enables the filtering of all values except those passed.
	 * 
	 * @param entriesToKeep
	 *            A List of labels to keep
	 */
	public void enableFiltering(List<String> entriesToKeep) {
		m_entriesToKeep = entriesToKeep;
		intern(new LinkedList<String>(Arrays.asList("#")));
		for (List<String> l : m_changedLabels.values()) {
			List<String> copy = new LinkedList<>(l);
			copy.retainAll(entriesToKeep);
			intern(copy);
		}
		++m_generation;

		m_filter = true;
	}

	/**
	 * After calling this method, the tracker will no longer filter out values
	 * not contained in its filter list.
	 */
	public void disableFiltering() {
		// If it was previously enabled, something might have changed.
		if (m_filter)
			++m_generation;
		m_filter = false;
		m_entriesToKeep = null;

	}

	@Override
	public int hashCode() {

		return 31 * super.hashCode() + m_generation;
	}

	/**
	 * Sets the event service of this tracker
	 * 
	 * @param e
	 *            Event Service to listen on
	 */
	public void setEventService(final EventService e) {
		m_EventService = e;
	}

	@Override
	public void convert(final LabelingType<String> input,
			final LabelingType<String> output) {

		final List<String> old = input.getLabeling();
		List<String> modified = m_changedLabels.get(old);

		boolean wasEmpty = false;
		if (old.isEmpty())
			wasEmpty = true;

		if (modified == null) {
			modified = new ArrayList<String>(input.getLabeling());

		}

		if (m_filter) {
			modified = new ArrayList<>(modified);
			modified.retainAll(m_entriesToKeep);
			if (!wasEmpty && modified.isEmpty())
				modified.add("#");
		}

		output.setLabeling(modified);
	}

	/**
	 * Resets the tracker to an empty state.
	 */
	public void reset() {
		for (List<String> l : m_changedLabels.keySet())
			intern(l);

		m_changedLabels.clear();
		m_generation++;

	}

	/**
	 * Returns the number of modifications known to this tracker.
	 * 
	 * @return The number of modified labelings
	 */
	public int getNumberOfModifiedLabels() {
		return m_changedLabels.keySet().size();
	}

	/*
	 * Checks the passed list for values filtered out and interns the resulting
	 * list.
	 */
	private void checkForFiltered(Collection<String> list) {
		List<String> copy = new LinkedList<>(list);
		if (m_entriesToKeep != null)
			copy.retainAll(m_entriesToKeep);
		intern(copy);
	}

	/**
	 * Forces the node to reset the current selection back to input by returning
	 * to initial values.
	 */
	public void restore() {
		for (final List<String> l : m_changedLabels.keySet())
			m_EventService.publish(new LabelingEditorListChangedEvent(l));

		reset();
	}

	/**
	 * Deletes the given Strings from all lists managed by this tracker.
	 * 
	 * @param deletedLabels
	 *            A List of labels to delete
	 */
	public void delete(Collection<String> deletedLabels) {
		for (List<String> l : m_changedLabels.values()) {
			l.removeAll(deletedLabels);
			intern(l);
		}
		++m_generation;
	}

	/**
	 * Renames a label stored in this tracker.
	 * 
	 * @param oldName
	 *            The old name of the label
	 * @param newName
	 *            The designated new name
	 */
	public void rename(String oldName, String newName) {
		for (List<String> l : m_changedLabels.values()) {
			if (l.contains(oldName)) {
				l.remove(oldName);
				l.add(newName);
				checkForFiltered(l);
				intern(l);
			}
		}
		++m_generation;
	}

}
