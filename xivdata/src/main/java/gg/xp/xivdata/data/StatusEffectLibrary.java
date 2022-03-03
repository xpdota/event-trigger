package gg.xp.xivdata.data;

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

public class StatusEffectLibrary {

	private static final Logger log = LoggerFactory.getLogger(StatusEffectLibrary.class);

	private static volatile boolean loaded;
	private static final Object lock = new Object();
	private static final Map<Long, StatusEffectInfo> csvValues = new HashMap<>();
	private static final Map<Long, StatusEffectIcon> cache = new HashMap<>();

	private static void ensureLoaded() {
		if (!loaded) {
			synchronized (lock) {
				if (!loaded) {
					readCsv();
				}
			}
		}
	}

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
					csvValues.put(id, new StatusEffectInfo(id, imageId, maxStacks, row[1], row[2]));
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
		csvValues.values().stream().distinct().sorted().map(s -> String.format("%06d", s.statusEffectId())).forEach(System.out::println);
	}

	public static Map<Long, StatusEffectInfo> getAll() {
		ensureLoaded();
		return Collections.unmodifiableMap(csvValues);
	}

	public static void readAltCsv(File file) {
		readCsv(() -> ReadCsv.cellsFromFile(file));
	}


	public static @Nullable StatusEffectInfo forId(long id) {
		ensureLoaded();
		return csvValues.get(id);
	}

	public static int getMaxStacks(long buffId) {
		// There are two main considerations here.
		// Sometimes, the 'stacks' value is used to represent something other than stacks (like on NIN)
		// Therefore, we have to assume that it is a garbage value and assume 0 stacks (i.e. not a stacking buff)
		// if rawStacks > maxStacks.
		// However, there are also unknown status effects, therefore we just assume 16 is the max for those, since that
		// seems to be the max for any legitimate buff.
		StatusEffectInfo statusEffectInfo = forId(buffId);
		long maxStacks;
		if (statusEffectInfo == null) {
			maxStacks = 16;
		}
		else {
			maxStacks = statusEffectInfo.maxStacks();
		}
		//noinspection NumericCastThatLosesPrecision - never that high
		return (int) maxStacks;
	}

	public static int calcActualStacks(long buffId, long rawStacks) {
		int maxStacks = getMaxStacks(buffId);
		if (rawStacks >= 0 && rawStacks <= maxStacks) {
			return (int) rawStacks;
		}
		else {
			return 0;
		}

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

	public static @Nullable StatusEffectIcon iconForId(long id, long stacks) {
		StatusEffectInfo statusEffectInfo = forId(id);
		if (statusEffectInfo == null) {
			return null;
		}
		long effectiveIconId = statusEffectInfo.iconIdForStackCount(stacks);
		return iconId(effectiveIconId);
	}

	public static @Nullable StatusEffectIcon iconId(long effectiveIconId) {
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
}
