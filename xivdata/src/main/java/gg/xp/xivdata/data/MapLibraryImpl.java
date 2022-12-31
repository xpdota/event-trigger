package gg.xp.xivdata.data;

import gg.xp.xivdata.util.ArrayBackedMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MapLibraryImpl {
	private static final Logger log = LoggerFactory.getLogger(MapLibraryImpl.class);

	private final CsvMapLoader<Integer, XivMap> loader;
	// TODO: see about moving this into CsvMapLoader
	private final Map<String, XivMap> byFileName = new HashMap<>();

	public MapLibraryImpl(Supplier<List<String[]>> cellSupplier) {
		loader = CsvMapLoader.builder(
						cellSupplier,
						this::parseRow,
						(row, item) -> row.getIntId())
				.setMapFinisher(ArrayBackedMap::new)
				.preFilterNullIds()
				.build();
	}

	// TODO: this is kind of jank
	private XivMap parseRow(CsvRowHelper row) {
		int offsetX;
		int offsetY;
		int scale;
		String region;
		String place;
		String subPlace;
		String filename;

		try {
			scale = row.getRequiredInt(8);
			offsetX = row.getRequiredInt(9);
			offsetY = row.getRequiredInt(10);
		}
		catch (NumberFormatException nfe) {
			// Ignore the bad value at the top
			return null;
		}
		filename = row.getRaw(7);
		if (filename.isBlank()) {
			filename = null;
		}
		region = row.getRaw(11);
		place = row.getRaw(12);
		subPlace = row.getRaw(13);
		if (subPlace.isEmpty()) {
			subPlace = null;
		}
		XivMap map = new XivMap(offsetX, offsetY, scale, filename, region, place, subPlace);
		if (filename != null) {
			byFileName.put(map.getFilename(), map);
		}
		return map;
	}

	public @Nullable XivMap forId(int id) {
		return loader.read().get(id);
	}

	public @Nullable XivMap forId(long id) {
		return loader.read().get((int) id);
	}

	public @Nullable XivMap forFilename(String mapFileName) {
		loader.read();
		return byFileName.get(mapFileName);
	}
}
