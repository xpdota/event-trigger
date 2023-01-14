package gg.xp.xivsupport.events.triggers.jails;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.OverridesCalloutGroupEnabledSetting;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.EntityKilledEvent;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.JobSortSetting;
import gg.xp.xivsupport.persistence.settings.LongSetting;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CalloutRepo(name = "Titan Gaols", duty = KnownDuty.UWU)
public class JailSolver implements FilteredEventHandler, OverridesCalloutGroupEnabledSetting {
	private final ModifiableCallout<FinalTitanJailsSolvedEvent> first = new ModifiableCallout<>("First Jail", "First");
	private final ModifiableCallout<FinalTitanJailsSolvedEvent> second = new ModifiableCallout<>("Second Jail", "Second");
	private final ModifiableCallout<FinalTitanJailsSolvedEvent> third = new ModifiableCallout<>("Third Jail", "Third");
	private final ModifiableCallout<EntityKilledEvent> playerDied = new ModifiableCallout<EntityKilledEvent>("Jailed Player Died", "Cover {jailnum}")
			.extendedDescription("When a player dies, this trigger will tell you which spot to cover.");
	private static final Logger log = LoggerFactory.getLogger(JailSolver.class);

	private final List<XivPlayerCharacter> jailedPlayers = new ArrayList<>();
	private final JobSortSetting setting;
	private final BooleanSetting enableTts;
	private final BooleanSetting enableAutomark;
	private final BooleanSetting overrideZoneLock;
	private final XivState state;
	private final LongSetting jailClearDelay;

	// TODO: scope - this is a perfect opportunity
	public JailSolver(PersistenceProvider persistence, XivState state) {
		enableTts = new BooleanSetting(persistence, "jail-solver.tts.enable", true);
		enableAutomark = new BooleanSetting(persistence, "jail-solver.automark.enable", true);
		overrideZoneLock = new BooleanSetting(persistence, "jail-solver.override-zone-lock", false);
		this.state = state;
		jailClearDelay = new LongSetting(persistence, "jail-solver.clear-delay", 10000L);
		setting = new JobSortSetting(persistence, "jail-solver.job-order", state);
	}

	@Override
	public boolean enabled(EventContext context) {
//		return true;
		return overrideZoneLock.get() || context.getStateInfo().get(XivState.class).zoneIs(0x309L);
	}

	@HandleEvents
	public void amTest(EventContext context, DebugCommand event) {
		XivState xivState = context.getStateInfo().get(XivState.class);
		List<XivPlayerCharacter> partyList = xivState.getPartyList();
		if (event.getCommand().equals("jailtest")) {
			List<String> args = event.getArgs();
			args.subList(1, args.size())
					.stream()
					.mapToInt(Integer::parseInt)
					.forEach(playerNum -> {
						int actualIndex = playerNum - 1;
						XivPlayerCharacter player;
						try {
							player = partyList.get(actualIndex);
						}
						catch (IndexOutOfBoundsException e) {
							log.error("You do not have {} players in the party. Are you in-instance?", playerNum);
							return;
						}
						jailedPlayers.add(player);
						// Fire off new event if we have exactly 3 events
						if (jailedPlayers.size() == 3) {
							context.accept(new UnsortedTitanJailsSolvedEvent(new ArrayList<>(jailedPlayers)));
						}
					});
		}
	}

	@HandleEvents
	public void handleWipe(EventContext context, DutyCommenceEvent event) {
		// TODO: this one can replace the other two but it needs testing
		clearJails();
	}

	@HandleEvents
	public void handleWipe(EventContext context, WipeEvent event) {
		clearJails();
	}

	@HandleEvents
	public void handleWipe(EventContext context, ZoneChangeEvent event) {
		clearJails();
	}

	private void clearJails() {
		log.info("Cleared jails");
		jailedPlayers.clear();
	}

	@HandleEvents
	public void amResetManual(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("jailreset")) {
			clearJails();
		}
	}

	// Clear on this since we use the jail list for determining if a jailed player has died.
	// Once the jail starts casting, it's too late to do anything about a dead jailee.
	@HandleEvents
	public void resetOnFetterBuff(EventContext context, AbilityCastStart event) {
		if (event.abilityIdMatches(0x2B6D)) {
			clearJails();
		}
	}

	@HandleEvents
	public void jailedPlayerDied(EventContext context, EntityKilledEvent event) {
		XivCombatant target = event.getTarget();
		int index;
		//noinspection SuspiciousMethodCalls
		if (target.isPc() && (index = jailedPlayers.indexOf(target)) >= 0) {
			context.accept(playerDied.getModified(event, Map.of("jailnum", index + 1)));
		}
	}

	@HandleEvents
	public void handleJailCast(EventContext context, AbilityUsedEvent event) {
		// Check ability ID - we only care about these two
		long id = event.getAbility().getId();
		if (id != 0x2B6B && id != 0x2B6C) {
			return;
		}
		XivCombatant target = event.getTarget();
		if (target instanceof XivPlayerCharacter pc) {
			jailedPlayers.add(pc);
		}
		log.info("Jailed Players: {}", jailedPlayers.stream().map(XivEntity::getName).collect(Collectors.joining(", ")));

		// Fire off new event if we have exactly 3 events
		if (jailedPlayers.size() == 3) {
			context.accept(new UnsortedTitanJailsSolvedEvent(new ArrayList<>(jailedPlayers)));
		}
	}

	@HandleEvents
	public void sortTheJails(EventContext context, UnsortedTitanJailsSolvedEvent event) {
		// This is where we would do job prio, custom prio, or whatever else you can come up with
		List<XivPlayerCharacter> jailedPlayers = new ArrayList<>(event.getJailedPlayers());
		jailedPlayers.sort(setting.getPlayerJailSortComparator());
		context.accept(new FinalTitanJailsSolvedEvent(jailedPlayers));
		log.info("Unsorted jails: {}", event.getJailedPlayers());
		log.info("Sorted jails: {}", jailedPlayers);
	}

	// Order doesn't realistically affect functionality, but is needed to have consistent behavior in tests
	@HandleEvents(order = 2)
	public void personalCallout(EventContext context, FinalTitanJailsSolvedEvent event) {
		if (enableTts.get()) {
			XivPlayerCharacter me = context.getStateInfo().get(XivState.class).getPlayer();
			List<XivPlayerCharacter> jailedPlayers = event.getJailedPlayers();
			int myIndex = 0;
			for (int i = 0; i < jailedPlayers.size(); i++) {
				if (jailedPlayers.get(i).getId() == me.getId()) {
					myIndex = i + 1;
					break;
				}
			}
			if (myIndex == 0) {
				log.info("Player is not jailed, no personal callout");
			}
			else {
				log.info("Jail index of player: {}", myIndex);
			}
			switch (myIndex) {
				case 1 -> context.accept(first.getModified(event));
				case 2 -> context.accept(second.getModified(event));
				case 3 -> context.accept(third.getModified(event));
			}
		}
	}

	@HandleEvents(order = 1)
	public void automarks(EventContext context, FinalTitanJailsSolvedEvent event) {
		if (enableAutomark.get()) {
			List<XivPlayerCharacter> playersToMark = event.getJailedPlayers();
			log.info("Requesting to mark jailed players: {}", playersToMark.stream().map(XivEntity::getName).collect(Collectors.joining(", ")));
			context.accept(new AutoMarkRequest(playersToMark.get(0)));
			context.accept(new AutoMarkRequest(playersToMark.get(1)));
			context.accept(new AutoMarkRequest(playersToMark.get(2)));
			ClearAutoMarkRequest clear = new ClearAutoMarkRequest();
			clear.setDelayedEnqueueOffset(jailClearDelay.get());
			context.enqueue(clear);
		}
		else {
			log.info("Automark disabled, skipping");
		}
	}

	public BooleanSetting getEnableTts() {
		return enableTts;
	}

	public BooleanSetting getEnableAutomark() {
		return enableAutomark;
	}

	public JobSortSetting getSort() {
		return setting;
	};

	public LongSetting getJailClearDelay() {
		return jailClearDelay;
	}

	public BooleanSetting getOverrideZoneLock() {
		return overrideZoneLock;
	}

	@Override
	public BooleanSetting getCalloutGroupEnabledSetting() {
		return enableTts;
	}
}
