package gg.xp.events.actlines;

import java.util.EnumMap;
import java.util.Map;

public class FieldMapper<K extends Enum<K>> {

	private final Map<K, String> raw;

	public FieldMapper(Map<K, String> raw) {
		this.raw = new EnumMap<>(raw);
	}

	public String getString(K key) {
		return raw.get(key);
	}

	public long getLong(K key) {
		return Long.parseLong(raw.get(key), 10);
	}

	public long getHex(K key) {
		return Long.parseLong(raw.get(key), 16);
	}

	public double getDouble(K key) {
		return Double.parseDouble(raw.get(key));
	}
}
