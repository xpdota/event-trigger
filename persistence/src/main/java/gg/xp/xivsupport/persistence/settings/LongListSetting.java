package gg.xp.xivsupport.persistence.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.xivsupport.persistence.PersistenceProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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

	/**
	 * Modify the list according to a mutator function. The function receives a copy of the list of values, and
	 * should modify the list in-place. If the list was changed, then the modified list will become the new values.
	 *
	 * @param mutator The function
	 * @return whether any modifications were made.
	 */
	public boolean mutate(Consumer<List<Long>> mutator) {
		List<Long> initial = get();
		List<Long> after = new ArrayList<>(get());
		mutator.accept(after);
		if (!Objects.equals(initial, after)) {
			set(after);
			return true;
		}
		return false;
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
