package gg.xp.xivsupport.events.actlines.events;

public enum OnlineStatus {
	UNKNOWN,
	ONLINE,
	BUSY,
	CUTSCENE,
	AFK,
	LOOKING_TO_MELD,
	RP,
	//ERP,
	LOOKING_FOR_PARTY,
	OTHER;

	// https://github.com/ngld/OverlayPlugin/blob/45d61da6ec12348c7c90247bf0b012078a7c8f71/OverlayPlugin.Core/EventSources/MiniParseEventSource.cs#L29
	public static OnlineStatus forId(int id) {
		return switch (id) {
			case 0 -> ONLINE;
			case 12 -> BUSY;
			case 15 -> CUTSCENE;
			case 17 -> AFK;
			case 21 -> LOOKING_TO_MELD;
			case 22 -> RP;
			case 23 -> LOOKING_FOR_PARTY;
			default -> OTHER;
		};
	}
}
