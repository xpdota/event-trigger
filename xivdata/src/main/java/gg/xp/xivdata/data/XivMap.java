package gg.xp.xivdata.data;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XivMap {

	private static final Logger log = LoggerFactory.getLogger(XivMap.class);
	private static boolean loaded;
	private static final Map<Long, XivMap> maps = new HashMap<>();

	private static void readCsv() {
		List<String[]> arrays;
		try (CSVReader csvReader = new CSVReader(new InputStreamReader(XivMap.class.getResourceAsStream("/xiv/maps/Map.csv")))) {
			arrays = csvReader.readAll();
		}
		catch (IOException | CsvException e) {
			log.error("Could not load icons!", e);
			return;
		}
		finally {
			loaded = true;
		}
		arrays.forEach(row -> {
			long id;
			int offsetX;
			int offsetY;
			int scale;
			String region;
			String place;
			String subPlace;

			try {
				id = Long.parseLong(row[0]);
				scale = Integer.parseInt(row[8]);
				offsetX = Integer.parseInt(row[9]);
				offsetY = Integer.parseInt(row[10]);
			}
			catch (NumberFormatException nfe) {
				// Ignore the bad value at the top
				return;
			}
			region = row[11];
			place = row[12];
			subPlace = row[13];
			if (subPlace.isEmpty()) {
				subPlace = null;
			}
			maps.put(id, new XivMap(offsetX, offsetY, scale, region, place, subPlace));
		});
		log.info("Loaded {} maps", maps.size());

	}

	public static final XivMap UNKNOWN = new XivMap(0, 0, 100, "Unknown", "Unknown", "Unknown");

	public static XivMap forId(long id) {
		if (!loaded) {
			readCsv();
		}
		return maps.getOrDefault(id, UNKNOWN);
	}


	private final int offsetX;
	private final int offsetY;

	private final double scaleFactor;

	private final String region;
	private final String place;
	private final @Nullable String subPlace;

	// TODO: is "MapMarkerRange" useful?
	public XivMap(int offsetX, int offsetY, int scaleFactor, String region, String place, @Nullable String subPlace) {
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.scaleFactor = ((double) scaleFactor) / 100;
		this.region = region;
		this.place = place;
		this.subPlace = subPlace;
	}


	public int getOffsetX() {
		return offsetX;
	}

	public int getOffsetY() {
		return offsetY;
	}

	public double getScaleFactor() {
		return scaleFactor;
	}

	public String getRegion() {
		return region;
	}

	public String getPlace() {
		return place;
	}

	public @Nullable String getSubPlace() {
		return subPlace;
	}

	@Override
	public String toString() {
		return String.format("XivMap(%s:%s:%s)", region, place, subPlace);
	}
}

