package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StatusEffectLibraryImpl {

	private static final Logger log = LoggerFactory.getLogger(StatusEffectLibraryImpl.class);

	private static final Map<Long, StatusEffectIcon> cache = new ConcurrentHashMap<>();
	private final Map<Integer, StatusEffectInfo> values;

	public StatusEffectLibraryImpl(InputStream input) {
		log.info("Loading status effect library");
		values = CompressedObjectStreamLoader.loadFrom(input, (StatusEffectInfo t) -> (int) t.statusEffectId());
		log.info("Loaded status effect library");
	}

	public Map<Integer, StatusEffectInfo> getAll() {
		return Collections.unmodifiableMap(values);
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
