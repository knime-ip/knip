/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2015
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.knip.core;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.knime.core.util.ThreadPool;
import org.scijava.Priority;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.thread.ThreadService;

/**
 * KNIP thread service for managing active threads.
 *
 * TODO this is way too much we have to implement!
 *
 * @author Christian Dietz, University of Konstanz
 */
@Plugin(type = Service.class, priority = Priority.HIGH_PRIORITY)
public final class KNIPThreadService extends AbstractService implements ThreadService {

    private static final String SCIJAVA_THREAD_PREFIX = "SciJava-";

    private static WeakHashMap<Thread, Thread> parents = new WeakHashMap<Thread, Thread>();

    @Parameter
    private LogService log;

    private ThreadPool pool = new ThreadPool(Runtime.getRuntime().availableProcessors() * 8);

    private ExecutorService es;

    private int nextThread = 0;

    private boolean disposed;

    /**
     *
     */
    public KNIPThreadService() {
        es = new ThreadPoolExecutorService(pool);
    }

    // -- ThreadService methods --

    @Override
    public <V> Future<V> run(final Callable<V> code) {
        if (disposed) {
            return null;
        }
        try {
            return pool.submit(wrap(code));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Future<?> run(final Runnable code) {
        if (disposed) {
            return null;
        }
        return pool.enqueue(wrap(code));
    }

    @Override
    public boolean isDispatchThread() {
        return EventQueue.isDispatchThread();
    }

    @Override
    public void invoke(final Runnable code) throws InterruptedException, InvocationTargetException {
        pool.submit(code);
    }

    @Override
    public void queue(final Runnable code) {
        pool.enqueue(code);
    }

    @Override
    public Thread getParent(final Thread thread) {
        return parents.get(thread != null ? thread : Thread.currentThread());
    }

    @Override
    public ThreadContext getThreadContext(final Thread thread) {
        final String name = thread.getName();

        // check for same context
        if (name.startsWith(contextThreadPrefix())) {
            return ThreadContext.SAME;
        }

        // check for different context
        if (name.startsWith(SCIJAVA_THREAD_PREFIX)) {
            return ThreadContext.OTHER;
        }

        // recursively check parent thread
        final Thread parent = getParent(thread);
        if (parent == thread || parent == null) {
            return ThreadContext.NONE;
        }
        return getThreadContext(parent);
    }

    // -- Disposable methods --

    @Override
    public void dispose() {
        disposed = true;
        if (pool != null) {
            pool.shutdown();
        }
    }

    // -- ThreadFactory methods --

    @Override
    public Thread newThread(final Runnable r) {
        final String threadName = contextThreadPrefix() + nextThread++;
        return new Thread(r, threadName);
    }

    private Runnable wrap(final Runnable r) {
        final Thread parent = Thread.currentThread();
        return new Runnable() {
            @Override
            public void run() {
                final Thread thread = Thread.currentThread();
                try {
                    if (parent != thread) {
                        parents.put(thread, parent);
                    }
                    r.run();
                } finally {
                    if (parent != thread) {
                        parents.remove(thread);
                    }
                }
            }
        };
    }

    private <V> Callable<V> wrap(final Callable<V> c) {
        final Thread parent = Thread.currentThread();
        return new Callable<V>() {
            @Override
            public V call() throws Exception {
                final Thread thread = Thread.currentThread();
                try {
                    if (parent != thread) {
                        parents.put(thread, parent);
                    }
                    return c.call();
                } finally {
                    if (parent != thread) {
                        parents.remove(thread);
                    }
                }
            }
        };
    }

    private String contextThreadPrefix() {
        final String contextHash = Integer.toHexString(context().hashCode());
        return SCIJAVA_THREAD_PREFIX + contextHash + "-Thread-";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutorService getExecutorService() {
        return es;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExecutorService(final ExecutorService arg0) {
        throw new UnsupportedOperationException("Executor Context can't be set");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<?> queue(final String id, final Runnable code) {
        // TODO this is very special... ask curtis why this is part of the thread service
        throw new UnsupportedOperationException("Unsupported Operation!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V> Future<V> queue(final String id, final Callable<V> code) {
        // TODO this is very special... ask curtis why this is part of the thread service
        throw new UnsupportedOperationException("Unsupported Operation!");
    }

}
