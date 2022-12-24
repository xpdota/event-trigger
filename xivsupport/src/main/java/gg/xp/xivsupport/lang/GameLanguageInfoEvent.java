package gg.xp.xivsupport.lang;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public class GameLanguageInfoEvent extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = 7714527798298591594L;
	private final GameLanguage gameLanguage;

	public GameLanguageInfoEvent(GameLanguage gameLanguage) {
		this.gameLanguage = gameLanguage;
	}

	public GameLanguage getGameLanguage() {
		return gameLanguage;
	}

	@Override
	public String getPrimaryValue() {
		return gameLanguage.toString();
	}
}
