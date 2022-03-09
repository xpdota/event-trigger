package gg.xp.xivsupport.events.fflogs;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SelfCompletingFuture<V> implements Future<V> {

	private final Callable<V> producer;
	private final Object lock = new Object();
	private volatile boolean done;
	private volatile V value;
	private volatile Throwable failure;

	public SelfCompletingFuture(Callable<V> producer) {
		this.producer = producer;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public V get() throws ExecutionException {
		if (done) {
			return getValueInternal();
		}
		synchronized (lock) {
			compute();
		}
		return getValueInternal();
	}

	private void compute() {
		try {
			value = producer.call();
		} catch (Throwable e) {
			failure = e;
		}
		done = true;
	}

	private V getValueInternal() throws ExecutionException {
		if (failure != null) {
			throw new ExecutionException(failure);
		}
		return value;
	}

	@Override
	public V get(long timeout, @NotNull TimeUnit unit) throws ExecutionException {
		// not really supported
		return get();
	}
}
