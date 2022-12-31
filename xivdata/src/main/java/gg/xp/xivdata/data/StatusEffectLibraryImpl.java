package gg.xp.xivdata.data;

import gg.xp.xivdata.util.ArrayBackedMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatusEffectLibraryImpl {

	private static final Logger log = LoggerFactory.getLogger(StatusEffectLibraryImpl.class);

	private static final Map<Long, StatusEffectIcon> cache = new ConcurrentHashMap<>();
	private final CsvMapLoader<Integer, StatusEffectInfo> loader;

	public StatusEffectLibraryImpl(Supplier<List<String[]>> cellSupplier) {
		loader = CsvMapLoader.builder(
						cellSupplier,
						StatusEffectLibraryImpl::parseRow,
						(row, item) -> (int) item.statusEffectId())
				.setMapFinisher(ArrayBackedMap::new)
				.preFilterNullIds()
				.build();
	}

	// TODO: this is kind of jank
	private static StatusEffectInfo parseRow(CsvRowHelper row) {
		int id = row.getIntId();
		String rawImg = row.getRaw(3);
		if (rawImg.isEmpty()) {
			log.warn("Image was empty!");
			return null;
		}
		long imageId;
		long maxStacks = row.getRequiredInt(5);
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
		int partyListPrio = row.getIntOrDefault(19, 200);
		String name = row.getRaw(1).intern();
		if (imageId == 0 && name.isEmpty()) {
			return null;
		}
		return new StatusEffectInfo(id,
				imageId,
				maxStacks,
				name,
				row.getRaw(2),
				row.getRequiredBool(16),
				row.getRequiredBool(18),
				partyListPrio,
				row.getRequiredBool(27)
		);
	}

	private static final Pattern texFilePattern = Pattern.compile("(\\d+)\\.tex");

	public Map<Integer, StatusEffectInfo> getAll() {
		return loader.read();
	}

	public @Nullable StatusEffectInfo forId(long id) {
		return getAll().get((int) id);
	}

	public @Nullable StatusEffectInfo forId(int id) {
		return getAll().get(id);
	}

	public int getMaxStacks(long buffId) {
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

	public int calcActualStacks(long buffId, long rawStacks) {
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

	public @Nullable StatusEffectIcon iconForId(long id, long stacks) {
		StatusEffectInfo statusEffectInfo = forId(id);
		if (statusEffectInfo == null) {
			return null;
		}
		long effectiveIconId = statusEffectInfo.iconIdForStackCount(stacks);
		return iconId(effectiveIconId);
	}

	public @Nullable StatusEffectIcon iconId(long effectiveIconId) {
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
