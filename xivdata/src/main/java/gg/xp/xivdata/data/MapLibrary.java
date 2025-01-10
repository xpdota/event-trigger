package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

public class MapLibrary {

	private static final MapLibraryImpl INSTANCE = new MapLibraryImpl(MapLibrary.class.getResourceAsStream("/xiv/maps/Map.oos.gz"));

	private static XivMap createUnknown(int id) {
		return new XivMap(id, 0, 0, 100, null, "Unknown %s".formatted(id), "Unk", "Unk");
	}

	public static XivMap forId(long id) {
		XivMap map = INSTANCE.forId(id);
		// Maps that don't exist in whatever version of the game files may still have blank stub entries, but these
		// cause the map tab to malfunction due to having a scale factor of 0.
		if (map == null || map.getScaleFactor() == 0.0) {
			return createUnknown((int) id);
		}
		else {
			return map;
		}
	}

	public static @Nullable XivMap forFilename(String mapFileName) {
		return INSTANCE.forFilename(mapFileName);
	}
}
