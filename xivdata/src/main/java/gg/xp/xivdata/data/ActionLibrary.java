package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionLibrary {
	private static final Logger log = LoggerFactory.getLogger(ActionLibrary.class);

	private static volatile boolean loaded;
	// TODO: improve cache performance by making a map implementation backed by an array
	private static final Map<Long, ActionIcon> cache = new ConcurrentHashMap<>();
	private static final Map<Long, ActionInfo> csvValues = new HashMap<>();

	private static void readCsv() {
		readCsv(() -> ReadCsv.cellsFromResource("/xiv/actions/Action.csv"));
	}

	// TODO: this is kind of jank
	private static void readCsv(Supplier<List<String[]>> cellProvider) {
		List<String[]> arrays;
		try {
			arrays = cellProvider.get();
			arrays.forEach(row -> {
				long id;
				try {
					id = Long.parseLong(row[0]);
				}
				catch (NumberFormatException nfe) {
					// Ignore the bad value at the top
					return;
				}
				String rawImg = row[3];
				if (rawImg.isEmpty()) {
					return;
				}
				long imageId;
				try {
					imageId = Long.parseLong(rawImg);
				}
				catch (NumberFormatException nfe) {
					Matcher matcher = texFilePattern.matcher(rawImg);
					if (matcher.find()) {
						imageId = Long.parseLong(matcher.group(1));
					}
					else {
						throw new RuntimeException("Invalid image specifier: " + rawImg, nfe);
						// Ignore non-numeric
//					return;
					}
				}
				long cd;
				try {
					cd = Long.parseLong(row[40]);
				}
				catch (NumberFormatException nfe) {
					log.error(String.format("Error reading cooldown: '%s'. Entire row: %s", row[40], Arrays.toString(row)));
					cd = 0;
				}
				int maxCharges;
				try {
					maxCharges = Integer.parseInt(row[43]);
					if (maxCharges <= 0) {
						maxCharges = 1;
					}
				}
				catch (NumberFormatException nfe) {
					log.error(String.format("Error reading max charges: '%s'. Entire row: %s", row[43], Arrays.toString(row)));
					maxCharges = 1;
				}
				if (imageId != 0) {
					String categoryRaw = row[50];
					csvValues.put(id, new ActionInfo(id, row[1].intern(), imageId, cd, maxCharges, categoryRaw.intern()));
				}
			});
		}
		catch (Throwable e) {
			log.error("Could not load icons!", e);
			return;
		}
		finally {
			loaded = true;
		}

		// If we fail, it's always going to fail, so continue without icons.
	}

	private static final Pattern texFilePattern = Pattern.compile("(\\d+)\\.tex");

	public static void main(String[] args) {
		ensureLoaded();
		csvValues.values().stream().distinct().sorted().map(s -> String.format("%06d", s)).forEach(System.out::println);
	}

	private static final Object initLock = new Object();
	private static void ensureLoaded() {
		if (!loaded) {
			synchronized (initLock) {
				if (!loaded) {
					readCsv();
				}
			}
		}
	}

	public static Map<Long, ActionInfo> getAll() {
		ensureLoaded();
		return Collections.unmodifiableMap(csvValues);
	}

	public static @Nullable ActionInfo forId(long id) {
		ensureLoaded();
		return csvValues.get(id);
	}

	public static void readAltCsv(File file) {
		readCsv(() -> ReadCsv.cellsFromFile(file));
	}

	// Special value to indicate no icon
	private static final ActionIcon NULL_MARKER;

	static {
		try {
			NULL_MARKER = new ActionIcon(new URL("http://bar/"));
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	// Unlike status effects, which are more complicated due to multi-icon for different stack numbers, this maps
	// the ability ID to icon directly, skipping the "icon ID" step.
	public static @Nullable ActionIcon iconForId(long id) {
		ActionIcon result = cache.computeIfAbsent(id, missingId -> {
			ActionInfo actionInfo = forId(missingId);
			if (actionInfo == null) {
				return NULL_MARKER;
			}
			long iconId = actionInfo.iconId();
			URL resource = ActionIcon.class.getResource(String.format("/xiv/icon/%06d_hr1.png", iconId));
			if (resource == null) {
				return NULL_MARKER;
			}
			return new ActionIcon(resource);
		});
		if (result == NULL_MARKER) {
			return null;
		}
		return result;
	}

	static @Nullable ActionIcon iconForInfo(ActionInfo info) {
		ActionIcon result = cache.computeIfAbsent(info.actionid(), missingId -> {
			long iconId = info.iconId();
			URL resource = ActionIcon.class.getResource(String.format("/xiv/icon/%06d_hr1.png", iconId));
			if (resource == null) {
				return NULL_MARKER;
			}
			return new ActionIcon(resource);
		});
		if (result == NULL_MARKER) {
			return null;
		}
		return result;

	}
}
