package gg.xp.xivdata.data;

public enum GameLanguage {

	Unknown("unknown"),
	English("en"),
	French("fr"),
	German("de"),
	Japanese("ja"),
	Chinese("cn"),
	Korean("ko"),
	;

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
		throw new IllegalArgumentException("No such language: " + code);
	}
}
