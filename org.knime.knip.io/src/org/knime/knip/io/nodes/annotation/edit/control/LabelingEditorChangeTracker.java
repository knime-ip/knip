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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This Class is an implementation of the @Converter interface, used to convert
 * labels into modified versions of themselves on the fly.
 * 
 * @author Andreas Burger, University of Konstanz
 * 
 */
public class LabelingEditorChangeTracker {

	/**
	 * This class maps the original list to its current representation and
	 * provides additional services such as renaming and filtering.
	 */

	// Maps the original labels to the current changed version. Uses Lists of
	// Strings for easy access.
	private final Map<Set<String>, Set<String>> m_map;

	private final Map<String, Integer> m_occurrence;

	private int m_generation = 1;

	private boolean isFiltered;
	
	private boolean m_isDirty = false;

	private Set<String> m_filteredLabels;

	public LabelingEditorChangeTracker() {
		m_map = new HashMap<Set<String>, Set<String>>();
		m_occurrence = new HashMap<String, Integer>();
		m_filteredLabels = new HashSet<String>();
	}

	public boolean insert(Set<String> existingLabels, String newLabel) {
		boolean changed = false;
		Set<String> knownSet = m_map.get(existingLabels);
		if (knownSet == null) {
			changed = true;
			Set<String> newEntry = new HashSet<String>(existingLabels);
			newEntry.add(newLabel);
			m_map.put(existingLabels, newEntry);
		} else {
			changed = knownSet.add(newLabel);
			m_map.put(existingLabels, knownSet);
		}
		if (changed) {
			m_generation++;
			increaseCounter(newLabel);
			m_isDirty = true;
		}
		return changed;
	}

	public boolean insert(Set<String> existingLabels, Set<String> newLabels) {
		boolean changed = false;
		Set<String> knownSet = m_map.get(existingLabels);
		if (knownSet == null) {
			changed = true;
			Set<String> newEntry = new HashSet<String>(newLabels);
			m_map.put(existingLabels, newEntry);
		} else {
			changed = knownSet.addAll(newLabels);
			m_map.put(existingLabels, knownSet);
		}
		if (changed)
			m_generation++;
		m_isDirty = true;
		return changed;
	}

	public boolean remove(Set<String> existingLabels, String labelToRemove) {
		boolean changed = false;
		Set<String> knownSet = m_map.get(existingLabels);
		if (knownSet == null) {
			changed = true;
			Set<String> newSet = new HashSet<String>(existingLabels);
			newSet.remove(labelToRemove);
			m_map.put(existingLabels, newSet);
		} else {
			changed = knownSet.remove(labelToRemove);
			m_map.put(existingLabels, knownSet);
		}
		if (changed) {
			decreaseCounter(labelToRemove);
			m_generation++;
			m_isDirty = true;
		}
		return changed;
	}

	public void rename(String oldName, String newName) {
		int count = 0;
		for (Set<String> set : m_map.values()) {
			if (set.remove(oldName)){
				set.add(newName);
				++count;
				m_isDirty = true;
			}
		}
		if (m_occurrence.containsKey(newName)) {
			int newval = m_occurrence.get(newName);
			newval += count;
			m_occurrence.put(newName, newval);
		} else
			m_occurrence.put(newName, count);
		
		m_occurrence.remove(oldName);
		m_generation++;

	}

	public void delete(String label) {
		for (Set<String> set : m_map.values()) {
			set.remove(label);
		}
		m_occurrence.remove(label);
		m_generation++;
		m_isDirty = true;
	}

	public Set<String> get(Set<String> key) {
		Set<String> knownSet = m_map.get(key);
		if (knownSet == null) {
			knownSet = new HashSet<String>(key);
			m_map.put(key, knownSet);
		}

		return new HashSet<String>(knownSet);
	}

	private void increaseCounter(String label) {
		if (m_occurrence.containsKey(label)) {
			Integer val = m_occurrence.get(label);
			val = val + 1;
			m_occurrence.put(label, val);
		} else {
			m_occurrence.put(label, 1);
		}
	}

	private void decreaseCounter(String label) {
		if (m_occurrence.containsKey(label)) {
			Integer val = m_occurrence.get(label);
			val = val - 1;
			if (val != 0)
				m_occurrence.put(label, val);
			else
				m_occurrence.remove(label);
		}
	}

	public Set<String> getNewLabels() {
		return m_occurrence.keySet();
	}

	public void clear() {
		m_map.clear();
		m_occurrence.clear();
		m_generation++;
		m_isDirty = false;
	}

	public Map<Set<String>, Set<String>> getMap() {
		return m_map;
	}

	public void setFiltered(boolean filter) {
		isFiltered = filter;
	}

	public boolean isFilteringEnabled() {
		return isFiltered;
	}

	public void setFilteredLabels(Set<String> labels) {
		m_filteredLabels = labels;
		m_generation++;
	}

	public Set<String> getFilteredLabels() {
		return m_filteredLabels;
	}
	
	public boolean isDirty(){
		return m_isDirty;
	}
	
	public Set<String> getCurrentLabels(List<Set<?>> originalLabels) {
		Set<String> result = new HashSet<String>();
		final Set<String> cache = new HashSet<String>();
		for(Set<? extends Object> set: originalLabels)
		{
			cache.clear();
			for(Object t : set)
			{
				cache.add(t.toString());
			}
			Set<String> newLabels = get(cache);
			if(newLabels != null)
				result.addAll(newLabels);
		}
		
		
		
		
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		return m_generation;
	}

	public void saveSettingsTo(NodeSettingsWO settings, int prefix) {
		settings.addBoolean("CHANGETRACKER_" + prefix +"_DIRTYFLAG", m_isDirty);
		saveChangeMap(settings, "CHANGETRACKER_" + prefix);
		saveCountMap(settings, "CHANGETRACKER_C_" + prefix);

	}

	private void saveChangeMap(NodeSettingsWO settings, String prefix) {
		final int mappings = m_map.keySet().size();
		int j = 0;
		settings.addInt(prefix + "_NUMKEYS", mappings);
		for (Set<String> key : m_map.keySet()) {
			Set<String> val = m_map.get(key);
			settings.addStringArray(prefix + "_KEY_" + j, key.toArray(new String[0]));
			settings.addStringArray(prefix + "_VAL_" + j, val.toArray(new String[0]));
			++j;
		}
	}

	private void saveCountMap(NodeSettingsWO settings, String prefix) {
		final int mappings = m_occurrence.keySet().size();
		int j = 0;
		settings.addInt(prefix + "_NUMKEYS", mappings);
		for (String key : m_occurrence.keySet()) {
			Integer val = m_occurrence.get(key);
			settings.addString(prefix + "_KEY_" + j, key);
			settings.addInt(prefix + "_VAL_" + j, val);
			++j;
		}
	}

	public void loadSettingsFrom(NodeSettingsRO settings, int prefix) throws InvalidSettingsException {
		m_isDirty = settings.getBoolean("CHANGETRACKER_" + prefix +"_DIRTYFLAG");
		loadChangeMap(settings, "CHANGETRACKER_" + prefix);
		loadCountMap(settings, "CHANGETRACKER_C_" + prefix);

	}

	private void loadChangeMap(NodeSettingsRO settings, String prefix) throws InvalidSettingsException {
		final int mappings = settings.getInt(prefix + "_NUMKEYS");
		for (int i = 0; i < mappings; ++i) {
			String[] key = settings.getStringArray(prefix + "_KEY_" + i);
			String[] val = settings.getStringArray(prefix + "_VAL_" + i);
			m_map.put(new HashSet<String>(Arrays.asList(key)), new HashSet<String>(Arrays.asList(val)));
		}
	}

	private void loadCountMap(NodeSettingsRO settings, String prefix) throws InvalidSettingsException {
		final int mappings = settings.getInt(prefix + "_NUMKEYS");
		for (int i = 0; i < mappings; ++i) {
			String key = settings.getString(prefix + "_KEY_" + i);
			Integer val = settings.getInt(prefix + "_VAL_" + i);
			m_occurrence.put(key, val);
		}
	}

}
