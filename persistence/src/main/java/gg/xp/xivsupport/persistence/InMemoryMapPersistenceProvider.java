package gg.xp.xivsupport.persistence;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class InMemoryMapPersistenceProvider extends BaseStringPersistenceProvider {


	// Using string/string and doing conversion to more accurately
	// mirror what a real one would do.
	private final Map<String, String> values = new HashMap<>();

	@Override
	protected void setValue(@NotNull String key, @Nullable String value) {
		values.put(key, value);
	}

	@Override
	protected void deleteValue(@NotNull String key) {
		values.remove(key);
	}

	@Override
	protected @Nullable String getValue(@NotNull String key) {
		return values.get(key);
	}

	@Override
	protected void clearAllValues() {
		values.clear();
	}
}
