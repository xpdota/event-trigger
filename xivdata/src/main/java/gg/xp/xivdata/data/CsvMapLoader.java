package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvMapLoader<K, V> {

	private static final Logger log = LoggerFactory.getLogger(CsvMapLoader.class);

	// Configuration
	private final Supplier<Stream<CsvRowHelper>> rowSupplier;
	private final Function<CsvRowHelper, @Nullable V> rowReader;
	private final BiFunction<CsvRowHelper, V, K> keyExtractor;
	private final Function<Map<K, V>, Map<K, V>> mapFinisher;

	// State
	private final Object lock = new Object();
	private Map<K, V> csvValues = Collections.emptyMap();
	private volatile boolean isLoaded;

	private CsvMapLoader(Supplier<Stream<CsvRowHelper>> rowSupplier, Function<CsvRowHelper, @Nullable V> rowReader, BiFunction<CsvRowHelper, V, K> keyExtractor, Function<Map<K, V>, Map<K, V>> mapFinisher) {
		this.rowSupplier = rowSupplier;
		this.rowReader = rowReader;
		this.keyExtractor = keyExtractor;
		this.mapFinisher = mapFinisher;
	}

	public Map<K, V> read() {
		if (!isLoaded) {
			synchronized (lock) {
				if (!isLoaded) {
					load();
				}
			}
		}
		return Collections.unmodifiableMap(csvValues);
	}

	private record IntermediateParse<V>(CsvRowHelper rowHelper, V value) {
	}

	private void load() {

		Map<K, V> unfinished = rowSupplier.get()
				.map(row -> {
					try {
						return new IntermediateParse<>(row, rowReader.apply(row));
					}
					catch (Throwable t) {
						log.error("Error parsing row!", t);
						throw new RuntimeException("Error parsing row '" + row.getLine() + '\'', t);
					}
				})
				.filter(item -> item.value != null)
				.collect(Collectors.toMap(item -> keyExtractor.apply(item.rowHelper, item.value), item -> item.value));
		csvValues = mapFinisher.apply(unfinished);
		isLoaded = true;

	}

	public static <K, V> Builder<K, V> builder(Supplier<List<String[]>> cellSupplier, Function<CsvRowHelper, @Nullable V> rowReader, BiFunction<CsvRowHelper, V, K> keyExtractor) {
		Supplier<Stream<CsvRowHelper>> rowSupplier = () -> cellSupplier.get().stream().map(CsvRowHelper::ofRow);
		return new Builder<>(rowSupplier, rowReader, keyExtractor);
	}

	public static final class Builder<K, V> {
		private final Supplier<Stream<CsvRowHelper>> rowSupplier;
		private Function<CsvRowHelper, @Nullable V> rowReader;
		private final BiFunction<CsvRowHelper, V, K> keyExtractor;
		private Function<Map<K, V>, Map<K, V>> mapFinisher = Function.identity();

		private Builder(Supplier<Stream<CsvRowHelper>> rowSupplier, Function<CsvRowHelper, @Nullable V> rowReader, BiFunction<CsvRowHelper, V, K> keyExtractor) {
			this.rowSupplier = rowSupplier;
			this.rowReader = rowReader;
			this.keyExtractor = keyExtractor;
		}

		public Builder<K, V> setMapFinisher(Function<Map<K, V>, Map<K, V>> mapFinisher) {
			this.mapFinisher = mapFinisher;
			return this;
		}

		public Builder<K, V> preFilterNullIds() {
			var originalReader = rowReader;
			rowReader = row -> {
				if (!row.hasValidId()) {
					return null;
				}
				return originalReader.apply(row);
			};
			return this;
		}

		public CsvMapLoader<K, V> build() {
			return new CsvMapLoader<>(rowSupplier, rowReader, keyExtractor, mapFinisher);
		}

	}

}
