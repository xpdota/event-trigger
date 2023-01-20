package gg.xp.xivsupport.persistence.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class CustomJsonListSetting<X> extends ObservableSetting {

	private static final Logger log = LoggerFactory.getLogger(CustomJsonListSetting.class);

	private final PersistenceProvider pers;
	private final String settingKey;
	private final String failuresKey;
	private final TypeReference<X> type;
	private final BiConsumer<JsonNode, X> postContstruct;
	private final ObjectMapper mapper;

	private List<X> items;

	private CustomJsonListSetting(
			PersistenceProvider pers,
			String settingKey,
			String failuresKey,
			TypeReference<X> type,
			ObjectMapper mapper,
			Supplier<List<X>> defaultProvider,
			BiConsumer<JsonNode, X> postContstruct) {
		this.pers = pers;
		this.settingKey = settingKey;
		this.failuresKey = failuresKey;
		this.type = type;
		this.mapper = mapper;
		this.postContstruct = postContstruct;

		boolean forceCommit = false;

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
						postContstruct.accept(node, item);
						items.add(item);
					}
					catch (Throwable jpe) {
						log.error("Item failed to convert to {}: \n{}\n", type, node, jpe);
						failed.add(node);
					}
				}
				if (!failed.isEmpty()) {
					String failedSetting = pers.get(failuresKey, String.class, "[]");
					List<String> otherFailures = mapper.readValue(failedSetting, new TypeReference<>() {
					});
					List<String> failures = new ArrayList<>(otherFailures);
					failures.addAll(failed.stream().map(Object::toString).toList());
					pers.save(failuresKey, mapper.writeValueAsString(failures));
					log.error("One or more {} items failed to load - they have been saved to the setting '{}'", type, failuresKey);
					forceCommit = true;

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
		if (forceCommit) {
			commit();
		}
	}

	private void makeListWritable() {
		if (!(items instanceof ArrayList<?>)) {
			items = new ArrayList<>(items);
		}
	}

	public void commit() {
		try {
			List<JsonNode> nodes = new ArrayList<>();
			//noinspection Convert2streamapi
			for (X item : items) {
				nodes.add(mapper.valueToTree(item));
			}
			String serialized = mapper.writeValueAsString(nodes);
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

	public List<String> getFailedItems() {
		String failedSetting = pers.get(failuresKey, String.class, "[]");
		List<String> failures;
		try {
			failures = mapper.readValue(failedSetting, new TypeReference<>() {
			});
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		return failures;
	}

	public void tryRecoverFailures() {
		List<String> failed = getFailedItems();
		if (failed.isEmpty()) {
			// Nothing to do
			return;
		}
		log.info("Attempting to recover {} items", failed.size());
		List<String> stillFailing = new ArrayList<>();
		List<X> noLongerFailing = new ArrayList<>();
		for (String failedItem : failed) {
			try {
				JsonNode node = mapper.readTree(failedItem);
				X item = mapper.convertValue(node, type);
				postContstruct.accept(node, item);
				noLongerFailing.add(item);
			}
			catch (Throwable e) {
				stillFailing.add(failedItem);
				log.trace("Failure: ", e);
			}
		}
		String value;
		try {
			value = mapper.writeValueAsString(stillFailing);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		log.info("After recovery: {} recovered, {} still failing", noLongerFailing.size(), stillFailing.size());
		noLongerFailing.forEach(this::addItem);
		pers.save(failuresKey, value);
		commit();
		log.info("Recovery complete");
	}

	public static <X> Builder<X> builder(@NotNull PersistenceProvider pers, @NotNull TypeReference<X> type, @NotNull String settingKey, @NotNull String failuresKey) {
		return new Builder<>(pers, type, settingKey, failuresKey);
	}

	public static class Builder<X> {
		private final PersistenceProvider pers;
		private final String settingKey;
		private final String failuresKey;
		private final TypeReference<X> type;
		private ObjectMapper mapper;
		private Supplier<List<X>> defaultProvider;
		private BiConsumer<JsonNode, X> postConstruct = (node, item) -> {
		};

		private Builder(@NotNull PersistenceProvider pers, @NotNull TypeReference<X> type, @NotNull String settingKey, @NotNull String failuresKey) {
			this.pers = pers;
			this.settingKey = settingKey;
			this.failuresKey = failuresKey;
			this.type = type;
		}

		public Builder<X> withMapper(ObjectMapper mapper) {
			this.mapper = mapper;
			return this;
		}

		public Builder<X> withDefaultProvider(Supplier<List<X>> defaultProvider) {
			this.defaultProvider = defaultProvider;
			return this;
		}

		public Builder<X> withDefaultValues(List<X> defaultValues) {
			this.defaultProvider = () -> defaultValues;
			return this;
		}

		public Builder<X> postConstruct(BiConsumer<JsonNode, X> postConstruct) {
			this.postConstruct = postConstruct;
			return this;
		}

		@SuppressWarnings("Convert2MethodRef")
		public CustomJsonListSetting<X> build() {
			return new CustomJsonListSetting<>(
					pers,
					settingKey,
					failuresKey,
					type,
					mapper != null ? mapper : new ObjectMapper(),
					defaultProvider != null ? defaultProvider : () -> Collections.emptyList(),
					postConstruct);
		}

	}
}
