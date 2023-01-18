package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.GameLanguage;

import java.io.Serial;

public class RsvEvent extends BaseEvent {

	@Serial
	private static final long serialVersionUID = -1410580304908803623L;
	private final GameLanguage lang;
	private final String rsvKey;
	private final String rsvValue;

	public RsvEvent(GameLanguage lang, String rsvKey, String rsvValue) {
		this.lang = lang;
		this.rsvKey = rsvKey;
		this.rsvValue = rsvValue;
	}

	public GameLanguage getLang() {
		return lang;
	}

	public String getRsvKey() {
		return rsvKey;
	}

	public String getRsvValue() {
		return rsvValue;
	}
}
