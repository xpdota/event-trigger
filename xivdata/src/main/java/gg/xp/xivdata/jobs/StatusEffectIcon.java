package gg.xp.xivdata.jobs;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusEffectIcon implements HasIconURL {

	private static final Logger log = LoggerFactory.getLogger(StatusEffectIcon.class);

	private final URL url;

	private static boolean loaded;
	private static final Map<Long, StatusEffectIcon> cache = new HashMap<>();
	private static final Map<Long, Long> csvValues = new HashMap<>();

	private static void readCsv() {
		List<String[]> arrays;
		try (CSVReader csvReader = new CSVReader(new InputStreamReader(StatusEffectIcon.class.getResourceAsStream("/xiv/statuseffect/Status.csv")))) {
			arrays = csvReader.readAll();
		}
		catch (IOException | CsvException e) {
			log.error("Could not load icons!", e);
			return;
		}
		finally {
			loaded = true;
		}
		arrays.forEach(row -> {
			try {
				long id = Long.parseLong(row[0]);
				long imageId = Long.parseLong(row[3]);
				if (imageId != 0) {
					csvValues.put(id, imageId);
				}
			}
			catch (NumberFormatException nfe) {
				// Ignore non-numeric
			}
		});

		// If we fail, it's always going to fail, so continue without icons.
	}

	public static void main(String[] args) {
		readCsv();
		csvValues.values().stream().distinct().sorted().map(s -> String.format("%06d", s)).forEach(System.out::println);
	}

	// Special value to indicate no icon
	private static final StatusEffectIcon NULL_MARKER;

	static {
		try {
			NULL_MARKER = new StatusEffectIcon(new URL("http://bar/"));
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static StatusEffectIcon forId(long id) {
		if (!loaded) {
			readCsv();
		}
		StatusEffectIcon result = cache.computeIfAbsent(id, missingId -> {
			URL resource = StatusEffectIcon.class.getResource(String.format("/xiv/statuseffect/icons/%06d.png", csvValues.get(missingId)));
			if (resource == null) {
				return NULL_MARKER;
			}
			return new StatusEffectIcon(resource);
		});
		if (result == NULL_MARKER) {
			return null;
		}
		return result;
	}

	private StatusEffectIcon(URL url) {
		this.url = url;
	}

	@Override
	public URL getIcon() {
		return url;
	}
}
