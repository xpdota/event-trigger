package gg.xp.xivsupport.gameversion;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;

public class GameVersionController {

	private GameVersion version = GameVersion.UNKNOWN_LATEST;

	@HandleEvents(order = -1000)
	public void setVersion(EventContext context, GameVersionEvent event) {
		version = event.getVersion();
	}

	public GameVersion getVersion() {
		return version;
	}

	public boolean isExactly(GameVersion other) {
		return version.equals(other);
	}

	public boolean isNewerThan(GameVersion other) {
		return version.compareTo(other) > 0;
	}

	public boolean isAtLeast(GameVersion other) {
		return version.compareTo(other) >= 0;
	}

	public boolean isOlderThan(GameVersion other) {
		return version.compareTo(other) < 0;
	}

	public boolean isUpTo(GameVersion other) {
		return version.compareTo(other) <= 0;
	}
}
