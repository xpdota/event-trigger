package gg.xp.xivsupport.events.triggers.marks;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoMarkHandler {

	private static final Logger log = LoggerFactory.getLogger(AutoMarkHandler.class);
	private final BooleanSetting useTelesto;
	private final BooleanSetting koreanMode;
	private final XivState state;

	public AutoMarkHandler(PersistenceProvider persistence, XivState state) {
		// TODO: make this automatic
		useTelesto = new BooleanSetting(persistence, "auto-marks.use-telesto", false);
		koreanMode = new BooleanSetting(persistence, "auto-marks.korean-mode", false);
		this.state = state;
	}

	public BooleanSetting getUseTelesto() {
		return useTelesto;
	}

	public BooleanSetting getKoreanMode() {
		return koreanMode;
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
