package gg.xp.xivsupport.events.misc.pulls;

import gg.xp.reevent.context.SubState;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.RawAddCombatantEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.FadeInEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.FadeOutEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.VictoryEvent;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.misc.ProxyForAppendOnlyList;
import gg.xp.xivsupport.events.state.InCombatChangeEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivZone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PullTracker implements SubState {

	private final List<Pull> pulls = new ArrayList<>();
	private final AtomicInteger pullCounter = new AtomicInteger(1);
	private Pull currentPull;

	private final XivState state;

	public PullTracker(XivState state) {
		this.state = state;
	}

	// Special case - initial plugin startup
	@HandleEvents(order = 500)
	public void startPull_initialStart(EventContext context, ZoneChangeEvent event) {
		if (currentPull == null) {
			doPullStart(context, event);
		}
	}

	@HandleEvents
	public void startPull(EventContext context, FadeInEvent event) {
		doPullStart(context, event);
	}

	private void doPullStart(EventContext context, BaseEvent event) {
		if (currentPull != null) {
			currentPull.setEnd(event);
		}
		XivZone zone = state.getZone();
		Pull myPull = new Pull(pullCounter.getAndIncrement(), event, zone);
		pulls.add(myPull);
		currentPull = myPull;
		state.getPartyList().forEach(currentPull::addPlayer);
		state.getCombatantsListCopy().stream()
				.filter(c -> !c.isPc())
				.forEach(currentPull::addEnemy);
		context.accept(new PullStartedEvent());
	}

	private void doPullEnd(EventContext context, BaseEvent event) {
		if (currentPull != null) {
			currentPull.setEnd(event);
			currentPull = null;
			context.accept(new PullEndedEvent());
		}
	}

	@HandleEvents
	public void updateCombatInfo(EventContext context, RawAddCombatantEvent event) {
		if (currentPull != null) {
			XivCombatant entity = event.getEntity();
			if (!entity.isPc()) {
				currentPull.addEnemy(entity);
			}
		}
	}

	@HandleEvents
	public void startCombatByAbility(EventContext context, AbilityUsedEvent abilityUsed) {
		if (currentPull != null && currentPull.getStatus() == PullStatus.PRE_PULL) {
			if (abilityUsed.getSource().isPc() && abilityUsed.getTarget().getType() == CombatantType.NPC) {
				currentPull.setCombatStart(abilityUsed);
			}
		}
	}

	@HandleEvents
	public void inCombatChangeEvent(EventContext context, InCombatChangeEvent event) {
		if (event.isInCombat()) {
			if (currentPull != null && currentPull.getStatus() == PullStatus.PRE_PULL) {
				currentPull.setCombatStart(event);
			}
		}
		else {
			if (currentPull != null && currentPull.getStatus() == PullStatus.COMBAT) {
				// TODO: this doesn't work too well because sometimes it arrives before the victory event
//				doPullEnd(context, event);
			}
		}
	}

	@HandleEvents
	public void forceSplit(EventContext context, DebugCommand cmd) {
		if (cmd.getCommand().equals("splitpull")) {
			doPullStart(context, cmd);
		}
	}

	@HandleEvents
	public void wipe(EventContext context, FadeOutEvent wipe) {
		doPullEnd(context, wipe);
	}

	@HandleEvents
	public void leaveZone(EventContext context, ZoneChangeEvent zoneChange) {
		doPullEnd(context, zoneChange);
	}

	@HandleEvents
	public void victory(EventContext context, VictoryEvent victory) {
		doPullEnd(context, victory);
	}

	@HandleEvents
	public void forceEnd(EventContext context, ForceCombatEnd end) {
//		doPullEnd(context, end);
		if (getCurrentStatus() == PullStatus.COMBAT) {
			doPullStart(context, end);
		}
	}

	public List<Pull> getPulls() {
		return new ProxyForAppendOnlyList<>(pulls);
	}

	public Pull getPull(int pullNum) {
		return pulls.stream().filter(p -> p.getPullNum() == pullNum).findFirst().orElse(null);
	}

	public PullStatus getCurrentStatus() {
		if (currentPull == null) {
			return PullStatus.PRE_PULL;
		}
		else {
			return currentPull.getStatus();
		}
	}

	public Pull getCurrentPull() {
		return currentPull;
	}
}
