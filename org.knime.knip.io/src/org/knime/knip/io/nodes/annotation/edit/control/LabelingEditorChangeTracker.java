package org.knime.knip.io.nodes.annotation.edit.control;

import java.util.ArrayList;
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
	 * Think of the way that this class works as a relation using foreign keys.
	 * One map contains the original list and a corresponding foreign key. The
	 * second map then stores the actual changed list with the foreign key as
	 * its primary. For sake of convenience and to avoid redundancy, these maps
	 * are both <List<String>, List<String>> instead of <List<String>, Int> and
	 * <Int, List<String>> respectively.
	 */

	// Maps the original labels to the current changed version. Uses Lists of
	// Strings for easy access.
	private Map<List<String>, List<String>> m_changedLabels = new HashMap<List<String>, List<String>>();

	// Manages the changed maps. Uses Lists as keys to avoid multiple hash
	// calculations.
	private Map<List<String>, List<String>> m_backMapping = new HashMap<List<String>, List<String>>();

	private int m_generation = 0;

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

		final List<String> mapped = m_backMapping.get(list);

		// If there is a mapping ...
		if (mapped != null) {
			// Remove the current mapped entry
			m_backMapping.remove(mapped);

			// Remove the labels from the mapped list
			mapped.removeAll(deletedEntries);

			// For hashing
			++m_generation;

			// Put the changed mapping back into the HashMap
			m_backMapping.put(mapped, mapped);

			// Notify the Labeling of the changes
			intern(mapped);

		}

		else {
			// First, create new mapping
			final LinkedList<String> changedList = new LinkedList<>(list);
			m_changedLabels.put(list, changedList);
			m_backMapping.put(changedList, changedList);

			// Update the created mapping
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

		final List<String> mapped = m_backMapping.get(list);

		// If there is a mapping ...
		if (mapped != null) {
			// Remove the current mapped entry
			m_backMapping.remove(mapped);

			// Add the labels to the mapped list, avoid duplicates
			final int i = mapped.size();

			for (final String s : addedEntries)
				if (!mapped.contains(s)) {
					mapped.add(s);
				}

			// Increase hash only if actual insertion took place
			if (mapped.size() - i > 0) {
				++m_generation;
				// Labelings are always sorted.
				Collections.sort(mapped);

				// Notify the LabelingType of the changes
				intern(mapped);
			}

			m_backMapping.put(mapped, mapped);
		}

		else {
			// First, create new mapping
			final LinkedList<String> changedList = new LinkedList<>(list);
			m_changedLabels.put(list, changedList);
			m_backMapping.put(changedList, changedList);

			// Update the created mapping
			insert(changedList, addedEntries);
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
		final Map<List<String>, List<String>> backmapping = new HashMap<List<String>, List<String>>();

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
			backmapping.put(valSet, valSet);
		}
		m_changedLabels = changedLabels;
		m_backMapping = backmapping;
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

		if (modified == null) {
			modified = new ArrayList<String>(input.getLabeling());
		}
		output.setLabeling(modified);
	}

	/**
	 * Resets the tracker to an empty state.
	 */
	public void reset() {
		m_changedLabels.clear();
		m_backMapping.clear();
		m_generation++;

	}
	
	public int getNumberOfModifiedLabels()
	{
		return m_changedLabels.keySet().size();
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

}
