package gg.xp.xivdata.data;

import gg.xp.xivdata.util.ArrayBackedMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ZoneLibraryImpl {

	public static final Logger log = LoggerFactory.getLogger(ZoneLibraryImpl.class);

	private final CsvMapLoader<Integer, ZoneInfo> loader;

	public ZoneLibraryImpl(Supplier<List<String[]>> cellSupplier) {
		loader = CsvMapLoader.builder(
						cellSupplier,
						ZoneLibraryImpl::parseRow,
						(row, item) -> item.id())
				.setMapFinisher(ArrayBackedMap::new)
				.preFilterNullIds()
				.build();
	}

	private static @Nullable ZoneInfo parseRow(CsvRowHelper row) {
		int id = row.getIntId();
		String placeName = row.getStringOrNull(6);
//		String mapFileName = row.getStringOrNull(7);
		String dutyName = row.getStringOrNull(11);
		if (placeName == null && dutyName == null) {
			return null;
		}
		if (placeName != null) {
			placeName = placeName.intern();
		}
//		if (mapFileName != null) {
//			mapFileName = mapFileName.intern();
//		}
		if (dutyName != null) {
			dutyName = dutyName.intern();
		}
//		XivMap map = MapLibrary.forFilename(mapFileName);
		return new ZoneInfo(id, dutyName, placeName);
	}


	public Map<Integer, ZoneInfo> getAll() {
		return loader.read();
	}

	public @Nullable ZoneInfo infoForZone(int id) {
		return getAll().get(id);
	}

	public @Nullable String nameForZone(int id) {
		ZoneInfo zoneInfo = infoForZone(id);
		return zoneInfo == null ? null : zoneInfo.name();
	}

	public @Nullable String capitalizedNameForZone(int id) {
		ZoneInfo value = infoForZone(id);
		return value == null ? null : value.getCapitalizedName();
	}

	public ZoneInfo infoForZoneOrUnknown(int id) {
		ZoneInfo zoneInfo = infoForZone(id);
		if (zoneInfo == null) {
			return new ZoneInfo(id, "Unknown", null);
		}
		else {
			return zoneInfo;
		}
	}
}
