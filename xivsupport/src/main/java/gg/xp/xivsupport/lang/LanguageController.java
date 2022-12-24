package gg.xp.xivsupport.lang;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ScanMe
public class LanguageController {

	private static final Logger log = LoggerFactory.getLogger(LanguageController.class);
	private static final GameLanguage defaultGameLanguage = GameLanguage.English;
	private @Nullable GameLanguage reportedGameLanguage;
	private @Nullable GameLanguage overrideGameLangauge;


	@HandleEvents(order = -1000)
	public void gameLanguageChange(EventContext context, GameLanguageInfoEvent event) {
		GameLanguage newLang = event.getGameLanguage();
		if (newLang == null) {
			throw new IllegalArgumentException("Game language cannot be null!");
		}
		log.info("Game language change: {} -> {}", reportedGameLanguage, newLang);
		reportedGameLanguage = newLang;
	}

	public @NotNull GameLanguage getGameLanguage() {
		if (overrideGameLangauge == null) {
			if (reportedGameLanguage == null) {
				return defaultGameLanguage;
			}
			return reportedGameLanguage;
		}
		else {
			return overrideGameLangauge;
		}
	}

}
