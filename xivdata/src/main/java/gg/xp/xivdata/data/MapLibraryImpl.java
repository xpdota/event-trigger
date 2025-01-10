package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MapLibraryImpl {

	private final Map<String, XivMap> byFileName = new HashMap<>();
	private final Map<Integer, XivMap> values;

	public MapLibraryImpl(InputStream input) {
		values = CompressedObjectStreamLoader.loadFrom(input, XivMap::getId);
		values.values().forEach(value -> byFileName.put(value.getFilename(), value));
	}

	public @Nullable XivMap forId(int id) {
		return values.get(id);
	}

	public @Nullable XivMap forId(long id) {
		return values.get((int) id);
	}

	public @Nullable XivMap forFilename(String mapFileName) {
		return byFileName.get(mapFileName);
	}
}
