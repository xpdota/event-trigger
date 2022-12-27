package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

public record ZoneInfo(int id, String dutyName, XivMap mapInfo) {

	public ZoneInfo {
		if (dutyName == null && mapInfo == null) {
			throw new IllegalArgumentException("Useless ZoneInfo for zone " + id + "! Both dutyName and mapInfo were null.");
		}
	}

	public @Nullable String placeName() {
		if (mapInfo == null) {
			return null;
		}
		return mapInfo.getPlace();
	}

	public String name() {
		if (dutyName == null) {
			return mapInfo.getPlace();
		}
		else {
			return dutyName;
		}
	}


	public String getCapitalizedName() {
		String name = name();
		if (name.isBlank()) {
			return name;
		}
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
}
