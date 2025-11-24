package gg.xp.xivsupport.persistence;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

public abstract class BaseStringPersistenceProvider implements PersistenceProvider {

	private static final Logger log = LoggerFactory.getLogger(BaseStringPersistenceProvider.class);
	// For now, let this use jackson2 backwards compatible defaults.
	private static final ObjectMapper mapper = JsonMapper.builder().configureForJackson2().build();

	@Override
	public void save(@NotNull String key, @NotNull Object value) {
		String convertedValue;
		if (value.getClass().isAnnotationPresent(UseJsonSer.class)) {
			try {
				convertedValue = mapper.writeValueAsString(value);
			}
			catch (JacksonException e) {
				log.error("Error serializing value", e);
				throw new RuntimeException(e);
			}
		}
		else {
			convertedValue = mapper.convertValue(value, String.class);
		}
		setValue(rewriteKey(key), convertedValue);
	}

	@Override
	public <X> X get(@NotNull String key, @NotNull Class<X> type, @Nullable X dflt) {
		if (type.isPrimitive() && dflt == null) {
			throw new IllegalArgumentException("Cannot use 'null' as a default if 'type' is a primitive");
		}
		String raw = getValue(rewriteKey(key));
		if (raw == null) {
			return dflt;
		}
		else {
			if (type.isAnnotationPresent(UseJsonSer.class)) {
				try {
					return mapper.readValue(raw, type);
				}
				catch (JacksonException e) {
					log.error("Error deserializing value", e);
					return dflt;
				}
			}
			else {
				return mapper.convertValue(raw, type);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X get(@NotNull String key, @NotNull TypeReference<X> type, @Nullable X dflt) {
		// If Type is wrapping a non-generic class, just use the normal class logic
		if (type.getType() instanceof Class<?> cls) {
			if (cls.isPrimitive() && dflt == null) {
				throw new IllegalArgumentException("Cannot use 'null' as a default if 'type' is a primitive");
			}
			return get(key, (Class<X>) cls, dflt);
		}
		String raw = getValue(rewriteKey(key));
		if (raw == null) {
			return dflt;
		}
		else {
			return mapper.convertValue(raw, type);
		}
	}

	public @Nullable String getRaw(@NotNull String key) {
		return getValue(key);
	}

	public void saveRaw(String key, String value) {
		setValue(key, value);
	}

	@Override
	public void clearAll() {
		clearAllValues();
	}

	@Override
	public void delete(@NotNull String key) {
		deleteValue(rewriteKey(key));
	}

	/**
	 * Since not every format may be able to handle every cdKey correctly, provide an optional
	 * way to massage the cdKey into a better format (e.g. stripping special characters)
	 *
	 * @param originalKey Original cdKey
	 * @return Massaged cdKey
	 */
	protected String rewriteKey(String originalKey) {
		return originalKey;
	}

	protected abstract void setValue(@NotNull String key, @Nullable String value);

	protected abstract void deleteValue(@NotNull String key);

	protected abstract @Nullable String getValue(@NotNull String key);

	protected abstract void clearAllValues();

}
