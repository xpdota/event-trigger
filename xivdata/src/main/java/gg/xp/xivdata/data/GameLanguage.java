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
}
