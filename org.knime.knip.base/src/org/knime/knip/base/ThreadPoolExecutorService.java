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
package org.knime.knip.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.util.ThreadPool;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ThreadPoolExecutorService implements ExecutorService {

    private final ThreadPool m_pool;

    public ThreadPoolExecutorService(final ThreadPool pool) {
        if (pool == KNIMEConstants.GLOBAL_THREAD_POOL) {
            throw new IllegalArgumentException("KNIME global threadpool can't be wrapped. Create subpool");
        }
        m_pool = pool;
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        m_pool.wait(timeout, (int)unit.toNanos(timeout));
        return isTerminated();
    }

    @Override
    public void execute(final Runnable command) {
        m_pool.enqueue(command);
    }

    public ThreadPool getThreadPool() {
        return m_pool;
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        final List<Future<T>> l = new ArrayList<Future<T>>();
        for (final Callable<T> task : tasks) {

            l.add(m_pool.submit(task));
        }
        return l;
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout,
                                         final TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException,
            ExecutionException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public boolean isShutdown() {
        return m_pool.getMaxThreads() == 0;
    }

    @Override
    public boolean isTerminated() {
        return (m_pool.getRunningThreads() == 0) && isShutdown();
    }

    @Override
    public void shutdown() {
        m_pool.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        m_pool.interruptAll();
        m_pool.shutdown();
        return new ArrayList<Runnable>();
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return m_pool.enqueue(task);
    }

    @Override
    public Future<?> submit(final Runnable task) {

        final Callable<Void> callable = new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                task.run();
                return null;
            }
        };

        return m_pool.enqueue(callable);

    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        throw new UnsupportedOperationException("Operation not supported");
    }
}
