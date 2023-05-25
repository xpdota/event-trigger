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

public class ActionLibraryImpl {
	private static final Logger log = LoggerFactory.getLogger(ActionLibraryImpl.class);

	private static final Pattern texFilePattern = Pattern.compile("(\\d+)\\.tex");

	private final Map<Long, ActionIcon> iconCache = new ConcurrentHashMap<>();
	private final CsvMapLoader<Integer, ActionInfo> loader;

	public ActionLibraryImpl(Supplier<List<String[]>> cellSupplier) {
		loader = CsvMapLoader.builder(
						cellSupplier,
						ActionLibraryImpl::parseRow,
						(row, item) -> (int) item.actionid())
				.setMapFinisher(ArrayBackedMap::new)
				.preFilterNullIds()
				.build();
	}

	public Map<Integer, ActionInfo> getAll() {
		return loader.read();
	}

	public @Nullable ActionInfo forId(int id) {
		return getAll().get(id);
	}

	public @Nullable ActionInfo forId(long id) {
		return getAll().get((int) id);
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
	public @Nullable ActionIcon iconForId(long id) {
		ActionIcon result = iconCache.computeIfAbsent(id, missingId -> {
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

	@Nullable ActionIcon iconForInfo(ActionInfo info) {
		ActionIcon result = iconCache.computeIfAbsent(info.actionid(), missingId -> {
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

	private static ActionInfo parseRow(CsvRowHelper row) {
		int id = row.getIntId();
		String rawImg = row.getRaw(3);
		if (rawImg.isEmpty()) {
//			log.warn("Image was empty!");
			return null;
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
			}
		}
		long cd = row.getLongOrDefault(40, 0);
		long recast = row.getLongOrDefault(38, 0);
		int maxCharges = row.getIntOrDefault(43, 0);
		if (maxCharges <= 0) {
			maxCharges = 1;
		}
		boolean isPlayerAbility = row.getRequiredBool(68); //Boolean.parseBoolean(row[68]);

		String name = row.getRaw(1).intern();
		if (imageId == 0 && name.isEmpty()) {
			return null;
		}
		String categoryRaw = row.getRaw(50);
		int castType = row.getIntOrDefault(28, 0);
		int effectRange = row.getIntOrDefault(29, 0);
		int xAxisModifier = row.getIntOrDefault(30, 0);

		return new ActionInfo(id, name, imageId, cd, maxCharges, categoryRaw.intern(), isPlayerAbility, recast, castType, effectRange, xAxisModifier);
	}
}
