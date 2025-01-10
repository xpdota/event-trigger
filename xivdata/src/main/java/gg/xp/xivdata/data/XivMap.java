package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class XivMap implements Serializable {

	private static final Logger log = LoggerFactory.getLogger(XivMap.class);
	@Serial
	private static final long serialVersionUID = -4708756454369252820L;

	public static final XivMap UNKNOWN = new XivMap(0, 0, 0, 100, null, "Unknown", "Unknown", "Unknown");

	@Deprecated // Use MapLibrary directly
	public static XivMap forId(long id) {
		return MapLibrary.forId(id);
	}

	private final int id;

	private final int offsetX;
	private final int offsetY;

	private final double scaleFactor;

	private final @Nullable String filename;
	private final String region;
	private final String place;
	private final @Nullable String subPlace;
	private final @Nullable URL url;

	// TODO: is "MapMarkerRange" useful?
	public XivMap(int id, int offsetX, int offsetY, int scaleFactor, @Nullable String filename, String region, String place, @Nullable String subPlace) {
		this.id = id;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.scaleFactor = ((double) scaleFactor) / 100;
		this.filename = filename;
		this.region = region;
		this.place = place;
		this.subPlace = subPlace;
		// Example filename: u5d2/01
		if (filename == null) {
			url = null;
		}
		else {
			String[] parts = filename.split("/");
			if (parts.length == 2) {
				String stub = parts[0];
				String index = parts[1];
				String urlStr = String.format("https://beta.xivapi.com/api/1/asset/map/%s/%s", stub, index);
				URL url;
				try {
					url = new URL(urlStr);
				}
				catch (MalformedURLException e) {
					url = null;
					log.error("Error forming map URL '{}'", urlStr, e);
				}
				this.url = url;
			}
			else {
				log.error("I don't know how to formulate a map URL from filename: '{}'", filename);
				url = null;
			}
		}
	}

	public int getId() {
		return id;
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

	public @Nullable String getFilename() {
		return filename;
	}

	public URL getImage() {
		return url;
	}

	@Override
	public String toString() {
		return String.format("XivMap(%s:%s:%s)", region, place, subPlace);
	}
}

