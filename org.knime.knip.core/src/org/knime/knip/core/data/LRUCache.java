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
package org.knime.knip.core.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LRU cache, based on <code>LinkedHashMap</code>.<br>
 * This cache has a fixed maximum number of elements (<code>cacheSize</code>). If the cache is full and another entry is
 * added, the LRU (least recently used) entry is dropped.
 * <p>
 * This class is thread-safe. All methods of this class are synchronized.<br>
 * Author: Christian d'Heureuse (<a href="http://www.source-code.biz">www.source-code.biz</a>)<br>
 * License: <a href="http://www.gnu.org/licenses/lgpl.html">LGPL</a>.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LRUCache<K, V> {

    private static final float HASH_TABLE_LOAD_FACTOR = 0.75f;

    private LinkedHashMap<K, V> m_map;

    private int m_cacheSize;

    /**
     * Creates a new LRU cache.
     * 
     * @param cacheSize the maximum number of entries that will be kept in this cache.
     */
    public LRUCache(final int cacheSize) {
        this.m_cacheSize = cacheSize;
        final int hashTableCapacity = (int)Math.ceil(cacheSize / HASH_TABLE_LOAD_FACTOR) + 1;
        m_map = new LinkedHashMap<K, V>(hashTableCapacity, HASH_TABLE_LOAD_FACTOR, true) {
            // (an anonymous inner class)
            private static final long serialVersionUID = 1;

            @Override
            protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
                return size() > LRUCache.this.m_cacheSize;
            }
        };
    }

    /**
     * Retrieves an entry from the cache.<br>
     * The retrieved entry becomes the MRU (most recently used) entry.
     * 
     * @param key the key whose associated value is to be returned.
     * @return the value associated to this key, or null if no value with this key exists in the cache.
     */
    public synchronized V get(final K key) {
        return m_map.get(key);
    }

    /**
     * Adds an entry to this cache. If the cache is full, the LRU (least recently used) entry is dropped.
     * 
     * @param key the key with which the specified value is to be associated.
     * @param value a value to be associated with the specified key.
     */
    public synchronized void put(final K key, final V value) {
        m_map.put(key, value);
    }

    /**
     * Clears the cache.
     */
    public synchronized void clear() {
        m_map.clear();
    }

    /**
     * Returns the number of used entries in the cache.
     * 
     * @return the number of entries currently in the cache.
     */
    public synchronized int usedEntries() {
        return m_map.size();
    }

    /**
     * Returns a <code>Collection</code> that contains a copy of all cache entries.
     * 
     * @return a <code>Collection</code> with a copy of the cache content.
     */
    public synchronized Collection<Map.Entry<K, V>> getAll() {
        return new ArrayList<Map.Entry<K, V>>(m_map.entrySet());
    }

} // end class LRUCache

