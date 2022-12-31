package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class ZoneLibrary {

	public static final Logger log = LoggerFactory.getLogger(ZoneLibrary.class);

	private static final ZoneLibraryImpl INSTANCE = new ZoneLibraryImpl(() -> ReadCsv.cellsFromResource("/xiv/territory/TerritoryType.csv"));

	private ZoneLibrary() {
	}


	public static Map<Integer, ZoneInfo> getFileValues() {
		return INSTANCE.getAll();
	}

	public static @Nullable ZoneInfo infoForZone(int id) {
		return INSTANCE.infoForZone(id);
	}

	public static @Nullable String nameForZone(int id) {
		return INSTANCE.nameForZone(id);
	}

	@Nullable
	public static String capitalizedNameForZone(int id) {
		return INSTANCE.capitalizedNameForZone(id);
	}

	public static ZoneInfo infoForZoneOrUnknown(int id) {
		return INSTANCE.infoForZoneOrUnknown(id);
	}
}
