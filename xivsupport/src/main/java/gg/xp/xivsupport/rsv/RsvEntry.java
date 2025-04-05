package gg.xp.xivsupport.rsv;

import gg.xp.xivdata.data.*;

public record RsvEntry(GameLanguage language, String key, String value) {
	public int numericId() {
		String[] splits = key.split("_");
		if (splits.length < 4) {
			return -1;
		}
		try {
			return Integer.parseInt(splits[2]);
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}
}
