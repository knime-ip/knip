/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2012
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 6, 2012 (hornm): created
 */

package org.knime.knip.base.data;

import java.lang.ref.SoftReference;

import org.apache.commons.io.FileUtils;
import org.knime.core.data.util.memory.MemoryReleasable;
import org.knime.core.data.util.memory.MemoryWarningSystem;
import org.knime.core.node.NodeLogger;
import org.knime.knip.core.data.LRUCache;

/**
 * Repository to manage arbitrary objects to cache them
 *
 * @author Martin Horn, Christian Dietz, Michael Zinsmaier, University of Konstanz
 *
 */
public class ObjectRepository {

    /*
     * caches objects created by the cell factory to avoid redundant
     * deserializations of similiar cells
     */
    private final LRUCache<Integer, SoftReference<Object>> CACHED_OBJECTS =
            new LRUCache<Integer, SoftReference<Object>>(5000);

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ObjectRepository.class);

    /* Memory Warning System also needed as we still maintain cells which block memory for KNIME warning system (even if weak references)*/
    private final MemoryWarningSystem m_memoryWarningSystem = MemoryWarningSystem.getInstance();

    private static ObjectRepository m_repo = null;

    private ObjectRepository() {
        m_memoryWarningSystem.setPercentageUsageThreshold(0.7);
        m_memoryWarningSystem.registerListener(new MemoryWarningSystem.MemoryWarningListener() {

            @Override
            public void memoryUsageLow(final long usedMemory, final long maxMemory) {
                LOGGER.debug("Low memory encountered in KNIP. Used memory: "
                        + FileUtils.byteCountToDisplaySize(usedMemory) + "; maximum memory: "
                        + FileUtils.byteCountToDisplaySize(maxMemory) + ".");
                // run in separate thread so that it can be debugged and we don't mess around with system threads
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (CACHED_OBJECTS) {
                            CACHED_OBJECTS.clear();
                        }
                    }
                }, "KNIP-Memory-Cleaner").start();
            }
        });
    }

    /**
     * Caches an object. Can be retrieved by {@link #getCachedObject(MemoryReleasable)}, if not deleted.
     *
     * @param obj
     */
    @SuppressWarnings("javadoc")
    public final void cacheObject(final Object obj) {
        synchronized (CACHED_OBJECTS) {
            final SoftReference<Object> ref = new SoftReference<Object>(obj);
            CACHED_OBJECTS.put(obj.hashCode(), ref);
            ref.get();
            LOGGER.debug("Cached another object!");
        }

    }

    /**
     * Get cached object. Returns null if object was released due to certain memory management conditions.
     *
     * @param obj
     *
     * @return the cached object
     */
    public final Object getCachedObject(final Object obj) {
        if (CACHED_OBJECTS.get(obj.hashCode()) != null) {
            return CACHED_OBJECTS.get(obj.hashCode()).get();
        } else {
            return null;
        }
    }

    /**
     * Singleton on Object Repository
     *
     * @return instance of {@link ObjectRepository}
     */
    public final static ObjectRepository getInstance() {
        if (m_repo == null) {
            m_repo = new ObjectRepository();
        }
        return m_repo;
    }
}
