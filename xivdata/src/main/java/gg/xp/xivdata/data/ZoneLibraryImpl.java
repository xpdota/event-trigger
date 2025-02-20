package gg.xp.xivdata.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

public final class ZoneLibraryImpl {

	public static final Logger log = LoggerFactory.getLogger(ZoneLibraryImpl.class);

	private final Map<Integer, ZoneInfo> values;

	public ZoneLibraryImpl(InputStream input) {
		values = CompressedObjectStreamLoader.loadFrom(input, ZoneInfo::id);
	}

	public Map<Integer, ZoneInfo> getAll() {
		return Collections.unmodifiableMap(values);
	}

	public @Nullable ZoneInfo infoForZone(int id) {
		return values.get(id);
	}

	public @Nullable String nameForZone(int id) {
		ZoneInfo zoneInfo = infoForZone(id);
		return zoneInfo == null ? null : zoneInfo.name();
	}

	public @Nullable String capitalizedNameForZone(int id) {
		ZoneInfo value = infoForZone(id);
		return value == null ? null : value.getCapitalizedName();
	}

	public @NotNull ZoneInfo infoForZoneOrUnknown(int id) {
		ZoneInfo zoneInfo = infoForZone(id);
		if (zoneInfo == null) {
			return new ZoneInfo(id, "Unknown", null);
		}
		else {
			return zoneInfo;
		}
	}
}
