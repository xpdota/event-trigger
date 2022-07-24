package gg.xp.xivsupport.persistence.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class CustomJsonListSetting<X> extends ObservableSetting {

	private static final Logger log = LoggerFactory.getLogger(CustomJsonListSetting.class);

	private final PersistenceProvider pers;
	private final String settingKey;
	private final String failuresKey;
	private final Class<X> type;
	private final ObjectMapper mapper;

	private List<X> items;

	private CustomJsonListSetting(
			PersistenceProvider pers,
			String settingKey,
			String failuresKey,
			Class<X> type,
			ObjectMapper mapper,
			Supplier<List<X>> defaultProvider
			) {
		this.pers = pers;
		this.settingKey = settingKey;
		this.failuresKey = failuresKey;
		this.type = type;
		this.mapper = mapper;

		String strVal = pers.get(settingKey, String.class, null);
		List<X> items;
		if (strVal == null) {
			items = defaultProvider.get();
		}
		else {
			try {
				items = new ArrayList<>();
				List<JsonNode> nodes = mapper.readValue(strVal, new TypeReference<>() {
				});
				List<JsonNode> failed = new ArrayList<>();
				for (JsonNode node : nodes) {
					try {
						X item = mapper.convertValue(node, type);
						items.add(item);
					}
					catch (Throwable jpe) {
						log.error("Item failed to convert to {}: \n{}\n", type, node, jpe);
						failed.add(node);
					}
				}
				if (!failed.isEmpty()) {
					String failedSetting = pers.get(failuresKey, String.class, "[]");
					List<String> otherFailues = mapper.readValue(failedSetting, new TypeReference<>() {
					});
					List<String> failures = new ArrayList<>(otherFailues);
					failures.addAll(failed.stream().map(Object::toString).toList());
					pers.save(failuresKey, mapper.writeValueAsString(failures));
					log.error("One or more {} items failed to load - they have been saved to the setting '{}'", type, failuresKey);

				}
			}
			catch (Throwable e) {
				log.error("Error loading setting {} (type {})", settingKey, type, e);
				log.error("Dump of data:\n{}", strVal);
				throw new RuntimeException(String.format("There was an error loading setting %s (type %s). Check the log.", settingKey, type), e);
			}
		}
		log.info("Loaded setting {}", settingKey);
		this.items = items;
	}

	private void makeListWritable() {
		if (!(items instanceof ArrayList<?>)) {
			items = new ArrayList<>(items);
		}
	}

	public void commit() {
		try {
			String serialized = mapper.writeValueAsString(items);
			pers.save(settingKey, serialized);
			notifyListeners();
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public List<X> getItems() {
		return new ArrayList<>(items);
	}

	public void setItems(List<X> items) {
		this.items = new ArrayList<>(items);
		commit();
	}

	public void addItem(X item) {
		makeListWritable();
		items.add(item);
		commit();
	}

	public boolean removeItem(X item) {
		makeListWritable();
		boolean removed = items.remove(item);
		if (removed) {
			commit();
		}
		return removed;
	}
//
//	public static <X> Builder<X> builder(@NotNull PersistenceProvider pers, @NotNull Class<X> type, @NotNull String settingKey, @NotNull String failuresKey) {
//
//	}

	public static class Builder<X> {
		private final PersistenceProvider pers;
		private final String settingKey;
		private final String failuresKey;
		private final Class<X> type;
		private ObjectMapper mapper;
		private Supplier<List<X>> defaultProvider;

		private Builder(@NotNull PersistenceProvider pers, @NotNull Class<X> type, @NotNull String settingKey, @NotNull String failuresKey) {
			this.pers = pers;
			this.settingKey = settingKey;
			this.failuresKey = failuresKey;
			this.type = type;
		}

		public void withMapper(ObjectMapper mapper) {
			this.mapper = mapper;
		}

		public void withDefaultProvider(Supplier<List<X>> defaultProvider) {
			this.defaultProvider = defaultProvider;
		}

		public void withDefaultValues(List<X> defaultValues) {
			this.defaultProvider = () -> defaultValues;
		}

		public CustomJsonListSetting<X> build() {
			return new CustomJsonListSetting<>(
					pers,
					settingKey,
					failuresKey,
					type,
					mapper != null ? mapper : new ObjectMapper(),
					defaultProvider != null ? defaultProvider : Collections::emptyList
			);
		}

	}
}
