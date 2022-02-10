package gg.xp.xivdata.jobs;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatusEffectIcon implements HasIconURL {

	private static final Logger log = LoggerFactory.getLogger(StatusEffectIcon.class);

	private final URL url;

	private static boolean loaded;
	private static final Map<Long, StatusEffectIcon> cache = new HashMap<>();
	private static final Map<Long, StatusEffectInfo> csvValues = new HashMap<>();

	private static void readCsv() {
		readCsv(() -> ReadCsv.cellsFromResource("/xiv/statuseffect/Status.csv"));
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
				String stacks = row[5];
				if (rawImg.isEmpty()) {
					return;
				}
				long imageId;
				long maxStacks;
				maxStacks = Long.parseLong(stacks);
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
				if (imageId != 0) {
					csvValues.put(id, new StatusEffectInfo(imageId, maxStacks, row[1], row[2]));
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
		readCsv();
		csvValues.values().stream().distinct().sorted().map(s -> String.format("%06d", s)).forEach(System.out::println);
	}

	public static Map<Long, StatusEffectInfo> getCsvValues() {
		if (!loaded) {
			readCsv();
		}
		return Collections.unmodifiableMap(csvValues);
	}

	public static void readAltCsv(File file) {
		readCsv(() -> ReadCsv.cellsFromFile(file));
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

	public static @Nullable StatusEffectIcon forId(long id, long stacks) {
		if (!loaded) {
			readCsv();
		}
		StatusEffectInfo iconId = csvValues.get(id);
		if (iconId == null) {
			return null;
		}
		long effectiveIconId = iconId.iconForStackCount(stacks);
		StatusEffectIcon result = cache.computeIfAbsent(effectiveIconId, missingId -> {
			URL resource = StatusEffectIcon.class.getResource(String.format("/xiv/icon/%06d_hr1.png", missingId));
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
