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

import java.lang.ref.WeakReference;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.knime.core.node.NodeLogger;

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
    private static final HashMap<Integer, WeakReference<Object>> CACHED_OBJECTS =
            new HashMap<Integer, WeakReference<Object>>();

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
    public final void cacheObject(final Object obj) {
        synchronized (CACHED_OBJECTS) {
            final WeakReference<Object> ref = new WeakReference<Object>(obj);
            CACHED_OBJECTS.put(obj.hashCode(), ref);
            LOGGER.debug(CACHED_OBJECTS.size() + " objects cached.");
        }

    }

    /**
     * Get cached object. Returns null if object was released due to certain memory management conditions.
     * 
     * @param obj
     * @return
     */
    public final Object getCachedObject(final Object obj) {
        if (CACHED_OBJECTS.get(obj) != null) {
            return CACHED_OBJECTS.get(obj.hashCode()).get();
        } else {
            return null;
        }
    }

    /**
     * Singleton on Object Repository
     * 
     * @return
     */
    public final static ObjectRepository getInstance() {
        if (m_repo == null) {
            m_repo = new ObjectRepository();
        }
        return m_repo;
    }
}
