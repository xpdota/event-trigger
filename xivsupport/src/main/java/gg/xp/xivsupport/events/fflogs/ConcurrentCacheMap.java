package gg.xp.xivsupport.events.fflogs;

import org.jetbrains.annotations.Contract;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;

public class ConcurrentCacheMap<K, V> {

	private final Map<K, Future<V>> map = new ConcurrentHashMap<>();
	private final Function<K, V> func;


	public ConcurrentCacheMap(Function<K, V> func) {
		this.func = func;
	}

	public V get(K key) {
		Future<V> future = map.computeIfAbsent(key, (k) -> new SelfCompletingFuture<>(() -> func.apply(k)));
		try {
			return future.get();
		}
		catch (ExecutionException | InterruptedException e) {
			// Since neither Function nor Producer can throw a checked exception, we know this will be fine.
			throwUnchecked(e.getCause());
			//noinspection all - never actually gets hit
			throw null;
		}
	}

	@Contract("_ -> fail")
	private static void throwUnchecked(Throwable exception) {
		throwIt(exception);
	}

	@Contract("_ -> fail")
	private static <E extends Exception> void throwIt(Throwable e) throws E {
		//noinspection unchecked
		throw (E) e;
	}

	public void clear() {
		map.clear();
	}

}

