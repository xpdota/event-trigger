package gg.xp.xivsupport.gameversion;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public final class GameVersionEvent extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = 5820627206539944938L;
	private final GameVersion version;

	private GameVersionEvent(GameVersion version) {
		this.version = version;
	}

	public static GameVersionEvent fromString(String value) {
		return new GameVersionEvent(GameVersion.fromString(value));
	}

	public GameVersion getVersion() {
		return version;
	}

	@Override
	public String getPrimaryValue() {
		return version.toString();
	}
}
