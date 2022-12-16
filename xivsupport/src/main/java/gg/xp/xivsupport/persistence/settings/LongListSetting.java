package gg.xp.xivsupport.persistence.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.xivsupport.persistence.PersistenceProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LongListSetting extends ObservableSetting implements Resettable {
	private final PersistenceProvider persistence;
	private final String settingKey;
	private final List<Long> dflt;
	private List<Long> cached;
	private static final ObjectMapper mapper = new ObjectMapper();

	public LongListSetting(PersistenceProvider persistence, String settingKey, long[] dflt) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = Arrays.stream(dflt).boxed().toList();
	}

	public List<Long> get() {
		if (cached == null) {
			String asString = persistence.get(settingKey, String.class, null);
			if (asString == null) {
				return cached = dflt;
			}
			try {
				return cached = Collections.unmodifiableList(mapper.readValue(asString, new TypeReference<List<Long>>() {
				}));
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}
		else {
			return Collections.unmodifiableList(cached);
		}
	}

	public void set(List<Long> newValue) {
		cached = new ArrayList<>(newValue);
		try {
			persistence.save(settingKey, mapper.writeValueAsString(newValue));
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		finally {
			notifyListeners();
		}
	}

	@Override
	public boolean isSet() {
		return cached != null;
	}

	@Override
	public void delete() {
		cached = null;
		notifyListeners();
	}
}
