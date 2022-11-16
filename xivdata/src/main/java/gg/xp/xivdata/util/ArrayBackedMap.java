package gg.xp.xivdata.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ArrayBackedMap<V> implements Map<Integer, V> {

	private final Object[] values;

	public ArrayBackedMap(Map<Integer, V> data) {
		if (data.isEmpty()) {
			values = new Object[0];
		}
		else {
			int maxKey = data.keySet().stream().mapToInt(Integer::intValue).max().getAsInt();
			values = new Object[maxKey + 1];
			data.forEach((k, v) -> values[k] = v);
		}
	}

	@Override
	public int size() {
		return values.length;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		for (Object o : values) {
			if (Objects.equals(o, value)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public @Nullable V get(Object key) {
		if (key instanceof Integer intKey) {
			if (intKey >= size()) {
				// Out of bounds
				return null;
			}
			return (V) values[intKey];
		}
		// Not an int
		return null;
	}

	@Nullable
	@Override
	public V put(Integer key, V value) {
		throw new UnsupportedOperationException("Read-only map");
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException("Read-only map");
	}

	@Override
	public void putAll(@NotNull Map<? extends Integer, ? extends V> m) {
		throw new UnsupportedOperationException("Read-only map");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("Read-only map");
	}

	@SuppressWarnings("Convert2streamapi")
	@NotNull
	@Override
	public Set<Integer> keySet() {
		Set<Integer> set = new HashSet<>(values.length);
		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				set.add(i);
			}
		}
		return set;
	}

	@SuppressWarnings("unchecked")
	@NotNull
	@Override
	public Collection<V> values() {
		return (Collection<V>) Arrays.stream(values).filter(Objects::nonNull).collect(Collectors.toSet());
	}

	@NotNull
	@Override
	public Set<Entry<Integer, V>> entrySet() {
		Set<Entry<Integer, V>> set = new HashSet<>(values.length);
		for (int i = 0; i < values.length; i++) {
			final int j = i;
			if (values[i] != null) {
				Entry<Integer, V> entry = new Entry<>() {
					@Override
					public Integer getKey() {
						return j;
					}

					@SuppressWarnings("unchecked")
					@Override
					public V getValue() {
						return (V) values[j];
					}

					@Override
					public V setValue(V value) {
						throw new UnsupportedOperationException("Read-only map");
					}
				};
				set.add(entry);
			}
		}
		return set;
	}
}
