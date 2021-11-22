package gg.xp.xivsupport.events.misc.pulls;

import gg.xp.reevent.context.SubState;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.FadeInEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.FadeOutEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.VictoryEvent;
import gg.xp.xivsupport.events.misc.ProxyForAppendOnlyList;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.CombatantType;

import java.util.ArrayList;
import java.util.List;

public class PullTracker implements SubState {

	private final List<Pull> pulls = new ArrayList<>();
	private Pull currentPull;

	// Special case - initial plugin startup
	@HandleEvents(order = 500)
	public void startPull_initialStart(EventContext context, ZoneChangeEvent event) {
		if (currentPull == null) {
			Pull myPull = new Pull(event, context.getStateInfo().get(XivState.class).getZone());
			pulls.add(myPull);
			currentPull = myPull;
		}
	}

	@HandleEvents
	public void startPull(EventContext context, FadeInEvent event) {
		Pull myPull = new Pull(event, context.getStateInfo().get(XivState.class).getZone());
		pulls.add(myPull);
		currentPull = myPull;
	}

	@HandleEvents
	public void updateCombatInfo(EventContext context, XivStateRecalculatedEvent event) {
		if (currentPull != null) {
			XivState state = context.getStateInfo().get(XivState.class);
			state.getPartyList().forEach(currentPull::addPlayer);
			state.getCombatantsListCopy().stream()
					.filter(c -> !c.isPc())
					.forEach(currentPull::addEnemy);
		}
	}

	// TODO: start of combat - currently would depend on cactbot event source, which we don't want to depend on
	@HandleEvents
	public void startCombat(EventContext context, AbilityUsedEvent abilityUsed) {
		if (currentPull != null && currentPull.getStatus().equals(PullStatus.PRE_PULL)) {
			if (abilityUsed.getSource().isPc() && abilityUsed.getTarget().getType() == CombatantType.NPC) {
				currentPull.setCombatStart(abilityUsed);
			}
		}
	}

	@HandleEvents
	public void wipe(EventContext context, FadeOutEvent wipe) {
		if (currentPull != null) {
			currentPull.setEnd(wipe);
			currentPull = null;
		}
	}

	@HandleEvents
	public void leaveZone(EventContext context, ZoneChangeEvent zoneChange) {
		if (currentPull != null) {
			currentPull.setEnd(zoneChange);
			currentPull = null;
		}
	}

	@HandleEvents
	public void victory(EventContext context, VictoryEvent victory) {
		if (currentPull != null) {
			currentPull.setEnd(victory);
			currentPull = null;
		}
	}

	public List<Pull> getPulls() {
		return new ProxyForAppendOnlyList<>(pulls);
	}

}
