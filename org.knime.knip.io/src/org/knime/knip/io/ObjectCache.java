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
package org.knime.knip.io;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import org.knime.knip.core.data.LRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Multiple object caches.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class ObjectCache {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ObjectCache.class);

	private static Map<String, ObjectCache> m_caches = new HashMap<String, ObjectCache>();

	/**
	 * Adds a new object to the cache specified by the cacheID. If the cache
	 * doesn't exist yet, it will be created.
	 * 
	 * @param cacheID
	 * @param objID
	 * @param obj
	 */
	public static void addObject(final String cacheID, final String objID,
			final Object obj) {
		LOGGER.debug("Caching object " + objID + " in cache " + cacheID + ".");
		getCache(cacheID).m_cachedObjects.put(objID, new SoftReference<Object>(
				obj));
	}

	/**
	 * Clears the whole image cache.
	 */
	public static void clearCache(final String cacheID) {
		final ObjectCache c = m_caches.get(cacheID);
		if (c != null) {
			c.m_cachedObjects.clear();
		}
	}

	/*
	 * Adds a new source to the list of available image factories.
	 * 
	 * @param cacheID a unique ID for that specific cache
	 * 
	 * @return
	 */
	private static void createCache(final String cacheID) {
		final ObjectCache c = new ObjectCache();
		m_caches.put(cacheID, c);

	}

	/*
	 * Returns the cache with the specified ID. If the cache doesn't exist, yet,
	 * it will be created.
	 * 
	 * @param cacheID
	 * 
	 * @return
	 */
	private static ObjectCache getCache(final String cacheID) {
		ObjectCache c = m_caches.get(cacheID);
		if (c == null) {
			createCache(cacheID);
			c = m_caches.get(cacheID);
		}
		return c;
	}

	/**
	 * Returns a cached object for the cache specified by the cacheID. If the
	 * cache doesn't exist yet, it will be created.
	 * 
	 * @param cacheID
	 * @param objID
	 * @return can be <code>null</code> if the object wasn't cached yet, or was
	 *         removed from the cache.
	 */
	public static Object getCachedObject(final String cacheID,
			final String objID) {
		final SoftReference<Object> res = getCache(cacheID).m_cachedObjects
				.get(objID);
		if (res != null) {
			return res.get();
		}
		return null;
	}

	/*---------some functions regarding the caching mechanism*/

	private final int m_cacheSize = 100;

	private final LRUCache<String, SoftReference<Object>> m_cachedObjects = new LRUCache<String, SoftReference<Object>>(
			m_cacheSize);

}
