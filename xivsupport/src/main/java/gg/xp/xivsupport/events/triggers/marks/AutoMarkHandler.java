package gg.xp.xivsupport.events.triggers.marks;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.lang.LanguageController;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoMarkHandler {

	private static final Logger log = LoggerFactory.getLogger(AutoMarkHandler.class);
	private final BooleanSetting useTelesto;
	private final BooleanSetting koreanMode;
	private final LanguageController langController;
	private final XivState state;
	private final EnumSetting<AutoMarkLanguage> languageSetting;

	public AutoMarkHandler(PersistenceProvider persistence, XivState state, LanguageController langController) {
		// TODO: make this automatic
		useTelesto = new BooleanSetting(persistence, "auto-marks.use-telesto", false);
		koreanMode = new BooleanSetting(persistence, "auto-marks.korean-mode", false);
		this.langController = langController;
		AutoMarkLanguage defaultLangSetting;
		if (koreanMode.get()) {
			defaultLangSetting = AutoMarkLanguage.JP;
		}
		else {
			defaultLangSetting = AutoMarkLanguage.Automatic;
		}
		languageSetting = new EnumSetting<>(persistence, "auto-marks.client-language", AutoMarkLanguage.class, defaultLangSetting);
		this.state = state;
	}

	@Deprecated
	public BooleanSetting getUseTelesto() {
		return useTelesto;
	}

	public EnumSetting<AutoMarkLanguage> getLanguageSetting() {
		return languageSetting;
	}

	public AutoMarkLanguage getEffectiveLanguage() {
		AutoMarkLanguage lang = languageSetting.get();
		if (lang == AutoMarkLanguage.Automatic) {
			return switch (langController.getGameLanguage()) {
				case Unknown -> AutoMarkLanguage.EN;
				case English -> AutoMarkLanguage.EN;
				case French -> AutoMarkLanguage.EN;
				case German -> AutoMarkLanguage.DE;
				case Japanese -> AutoMarkLanguage.JP;
				// TODO: is this correct?
				case Chinese -> AutoMarkLanguage.JP;
				// TODO: is this correct?
				case TraditionalChinese -> AutoMarkLanguage.JP;
				case Korean -> AutoMarkLanguage.JP;
			};
		}
		else {
			return lang;
		}
	}

	@HandleEvents
	public void findPartySlot(EventContext context, AutoMarkRequest event) {
		XivPlayerCharacter player = event.getPlayerToMark();
		int index = state.getPartySlotOf(player);
		if (index >= 0) {
			int partySlot = index + 1;
			log.info("Resolved player {} to party slot {}", player.getName(), partySlot);
			context.accept(new AutoMarkSlotRequest(partySlot));
		}
		else {
			log.error("Couldn't resolve player '{}' to party slot", player.getName());
		}
	}
}
