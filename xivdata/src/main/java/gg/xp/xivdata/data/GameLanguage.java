package gg.xp.xivdata.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum GameLanguage {

	Unknown("unknown"),
	English("en"),
	French("fr"),
	German("de"),
	Japanese("ja"),
	Chinese("cn"),
	Korean("ko"),
	TraditionalChinese("tc")
	;

	private static final Logger log = LoggerFactory.getLogger(GameLanguage.class);

	private final String shortCode;

	GameLanguage(String shortCode) {
		this.shortCode = shortCode;
	}

	public String getShortCode() {
		return shortCode;
	}

	public static GameLanguage valueOfShort(String code) {
		for (GameLanguage value : values()) {
			if (value.shortCode.equalsIgnoreCase(code)) {
				return value;
			}
		}
		log.warn("Unknown language code: {}", code);
		return Unknown;
	}
}
