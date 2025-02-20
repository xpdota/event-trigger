package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public record ZoneInfo(int id, @Nullable String dutyName, @Nullable String placeName) implements Serializable {

//	public ZoneInfo {
//		if (dutyName == null && placeName == null) {
//			throw new IllegalArgumentException("Useless ZoneInfo for zone " + id + "! Both dutyName and mapInfo were null.");
//		}
//	}

	public String name() {
		if (dutyName != null) {
			return dutyName;
		}
		else if (placeName != null) {
			return placeName;
		}
		return "(Unknown)";
	}

	public String getCapitalizedName() {
		String name = name();
		if (name.isBlank()) {
			return name;
		}
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
}
