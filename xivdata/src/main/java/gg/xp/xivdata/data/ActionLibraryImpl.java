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
//	private static final Logger log = LoggerFactory.getLogger(ActionLibraryImpl.class);

	private static final Pattern texFilePattern = Pattern.compile("(\\d+)\\.tex");
//	private static final Pattern omenConePattern = Pattern.compile("_fan(\\d+)_");

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
		int omenId = row.getIntOrDefault(54, -1);
		int coneAngle = switch (omenId) {
			// TODO: Yucky awk script made this, pull the actual Omen EXD later
			case 3 -> 60;
			case 4, 184, 163 -> 90;
			case 5, 379, 185, 164, 101, 60 -> 120;
			case 15, 459, 349, 297, 213, 17, 16 -> 270;
			case 28, 456, 283 -> 150;
			case 61, 194, 107, 279, 481, 457 -> 180;
			case 91 -> 240;
			case 98 -> 60;
			case 99 -> 30;
			case 100 -> 60;
			case 105 -> 30;
			case 128 -> 210;
			case 146 -> 20;
			case 159 -> 60;
			case 183 -> 60;
			case 206 -> 45;
			case 207 -> 45;
			case 221 -> 1;
			case 235 -> 100;
			case 236 -> 100;
			case 258 -> 60;
			case 306 -> 130;
			case 346 -> 145;
			case 378 -> 15;
			case 407 -> 225;
			case 431 -> 360;
			case 437 -> 1;
			case 438 -> 2;
			case 452 -> 30;
			case 455 -> 60;
			case 458 -> 210;
			case 462 -> 240;
			case 465 -> 45;


			// Just assume some kind of default
			default -> 30;
		};
		return new ActionInfo(id, name, imageId, cd, maxCharges, categoryRaw.intern(), isPlayerAbility, recast, castType, effectRange, xAxisModifier, coneAngle);
	}
}
