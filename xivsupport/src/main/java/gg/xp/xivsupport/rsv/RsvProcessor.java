package gg.xp.xivsupport.rsv;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.RsvEvent;
import gg.xp.xivsupport.lang.GameLanguageInfoEvent;
import gg.xp.xivsupport.lang.LanguageController;

public class RsvProcessor {

	private final LanguageController languageController;

	public RsvProcessor(LanguageController languageController) {
		this.languageController = languageController;
	}

	private void setLang() {
		PersistentRsvLibrary.INSTANCE.setCurrentLang(languageController.getGameLanguage());
	}

	@HandleEvents
	public void langEvent(EventContext context, GameLanguageInfoEvent event) {
		setLang();
	}

	@HandleEvents
	public void rsvEvent(EventContext context, RsvEvent event) {
		PersistentRsvLibrary.INSTANCE.set(event.getLang(), event.getRsvKey(), event.getRsvValue());
	}

}
