package gg.xp.xivsupport.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseStringPersistenceProvider implements PersistenceProvider {

	private static final Logger log = LoggerFactory.getLogger(BaseStringPersistenceProvider.class);
	private static final ObjectMapper mapper = new ObjectMapper();

	@Override
	public void save(@NotNull String key, @NotNull Object value) {
		String convertedValue;
		if (value.getClass().isAnnotationPresent(UseJsonSer.class)) {
			try {
				convertedValue = mapper.writeValueAsString(value);
			}
			catch (JsonProcessingException e) {
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
				catch (JsonProcessingException e) {
					log.error("Error deserializing value", e);
					return dflt;
				}
			}
			else {
				return mapper.convertValue(raw, type);
			}
		}
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
