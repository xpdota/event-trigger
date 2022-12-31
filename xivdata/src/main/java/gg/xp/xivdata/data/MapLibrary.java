package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

public class MapLibrary {

	private static final MapLibraryImpl INSTANCE = new MapLibraryImpl(() -> ReadCsv.cellsFromResource("/xiv/maps/Map.csv"));

	public static XivMap forId(long id) {
		XivMap map = INSTANCE.forId(id);
		return (map == null) ? XivMap.UNKNOWN : map;
	}

	public static @Nullable XivMap forFilename(String mapFileName) {
		return INSTANCE.forFilename(mapFileName);
	}
}
