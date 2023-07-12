package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivdata.data.GameLanguage;
import gg.xp.xivsupport.gui.util.HasFriendlyName;

import java.io.Serial;

@SystemEvent
public class RsvEvent extends BaseEvent implements HasPrimaryValue {

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

	@Override
	public String getPrimaryValue() {
		return String.format("%s: %s", rsvKey, rsvValue);
	}
}
