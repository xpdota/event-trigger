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
import java.util.Map;

public final class ZoneLibrary {

	public static final Logger log = LoggerFactory.getLogger(ZoneLibrary.class);

	private static volatile boolean loaded;
	private static final Object lock = new Object();
	private static Map<Integer, ZoneInfo> fileValues = Collections.emptyMap();

	private ZoneLibrary() {
	}

	private static void ensureLoaded() {
		if (!loaded) {
			synchronized (lock) {
				if (!loaded) {
					readFile();
				}
			}
		}
	}

	// TODO: something is slightly wrong here -
	private static void readFile() {
		Map<Integer, ZoneInfo> data = new HashMap<>();
		try {
			InputStream res = ZoneLibrary.class.getResourceAsStream("/xiv/zones/zones.txt");
			if (res == null) {
				return;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(res));
			reader.lines().forEach(line -> {
				if (line.isBlank()) {
					return;
				}
				String[] split = line.split(":", 2);
				if (split.length != 2) {
					log.error("Bad line in zones: '{}'", split);
					return;
				}
				int id = Integer.parseInt(split[0]);
				String dutyName = split[1];
//				XivMap mapInfo = XivMap.forId(id);
				// Whoops. Map ID is not the place ID, this doesn't work without some extra logic.
				XivMap mapInfo = null;
				data.put(id, new ZoneInfo(id, dutyName, mapInfo));
			});
		}
		finally {
			fileValues = new ArrayBackedMap<>(data);
			loaded = true;
		}

	}

	public static Map<Integer, ZoneInfo> getFileValues() {
		ensureLoaded();
		return Collections.unmodifiableMap(fileValues);
	}

	public static @Nullable ZoneInfo infoForZone(int id) {
		ensureLoaded();
		return fileValues.get(id);
	}

	public static @Nullable String nameForZone(int id) {
		ensureLoaded();
		ZoneInfo value = fileValues.get(id);
		return value == null ? null : value.name();
	}

	public static @Nullable String capitalizedNameForZone(int id) {
		ensureLoaded();
		ZoneInfo value = fileValues.get(id);
		return value == null ? null : value.getCapitalizedName();
	}
}
