package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivCombatant;

import java.awt.*;

public class RefreshingHpBarTestTrigger {

	private final ModifiableCallout<PlayerHpTracker> haurch_hp = new ModifiableCallout<>("Haurchefant HP bar", "", "HP", PlayerHpTracker::isExpired)
			.guiProvider(PlayerHpTracker::getComponent);
	private final XivState state;

	public RefreshingHpBarTestTrigger(XivState state) {
		this.state = state;
	}

	public XivState getState() {
		return state;
	}

	private final class PlayerHpTracker {
		private final XivCombatant haurchInitial;
		private final BaseEvent e1;

		private PlayerHpTracker(XivCombatant combatant, BaseEvent e1) {
			this.haurchInitial = combatant;
			this.e1 = e1;
		}

		public XivCombatant getNewData() {
			return getState().getLatestCombatantData(haurchInitial);
		}

		public boolean isExpired() {
			if (e1.getEffectiveTimeSince().toSeconds() > 1200) {
				return true;
			}
			if (getNewData() == null) {
				return true;
			}
			return false;
		}

		public Component getComponent() {
			RefreshingHpBar bar = new RefreshingHpBar(this::getNewData);
			bar.setPreferredSize(new Dimension(200, 20));
			bar.setFgTransparency(220);
			bar.setBgTransparency(128);
			return bar;
		}
	}

	@HandleEvents
	public void debug(EventContext context, DebugCommand dbg) {
		if (dbg.getCommand().equals("hptest")) {
			PlayerHpTracker tracker = new PlayerHpTracker(state.getPlayer(), dbg);
			context.accept(haurch_hp.getModified(tracker));
		}
	}

}
