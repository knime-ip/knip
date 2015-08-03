package org.knime.knip.featurenode.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counting Completion Service,
 *
 * @author seebacher
 *
 * @param <V>
 */
public class CountingCompletionService<V> extends ExecutorCompletionService<V> {
	private final AtomicLong submittedTasks = new AtomicLong();
	private final AtomicLong completedTasks = new AtomicLong();

	public CountingCompletionService(final Executor executor) {
		super(executor);
	}

	public CountingCompletionService(final Executor executor, final BlockingQueue<Future<V>> queue) {
		super(executor, queue);
	}

	@Override
	public Future<V> submit(final Callable<V> task) {
		final Future<V> future = super.submit(task);
		this.submittedTasks.incrementAndGet();
		return future;
	}

	@Override
	public Future<V> submit(final Runnable task, final V result) {
		final Future<V> future = super.submit(task, result);
		this.submittedTasks.incrementAndGet();
		return future;
	}

	@Override
	public Future<V> take() throws InterruptedException {
		final Future<V> future = super.take();
		this.completedTasks.incrementAndGet();
		return future;
	}

	@Override
	public Future<V> poll() {
		final Future<V> future = super.poll();
		if (future != null)
			this.completedTasks.incrementAndGet();
		return future;
	}

	@Override
	public Future<V> poll(final long timeout, final TimeUnit unit) throws InterruptedException {
		final Future<V> future = super.poll(timeout, unit);
		if (future != null)
			this.completedTasks.incrementAndGet();
		return future;
	}

	public long getNumberOfCompletedTasks() {
		return this.completedTasks.get();
	}

	public long getNumberOfSubmittedTasks() {
		return this.submittedTasks.get();
	}

	public boolean hasUncompletedTasks() {
		return this.completedTasks.get() < this.submittedTasks.get();
	}
}