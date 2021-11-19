package gg.xp.xivsupport.events.triggers.jails;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.BooleanSetting;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class JailSolver implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(JailSolver.class);

	private final List<XivPlayerCharacter> jailedPlayers = new ArrayList<>();
	private final BooleanSetting enableTts;
	private final BooleanSetting enableAutomark;

	public JailSolver(PersistenceProvider persistence) {
		enableTts = new BooleanSetting(persistence, "jail-solver.tts.enable", true);
		enableAutomark = new BooleanSetting(persistence, "jail-solver.automark.enable", true);
	}

	@Override
	public boolean enabled(EventContext context) {
		// TODO: test this
		return context.getStateInfo().get(XivState.class).zoneIs(0x309L);
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
	public void handleWipe(EventContext context, WipeEvent event) {
		log.info("Cleared jails");
		jailedPlayers.clear();
	}

	@HandleEvents
	public void amResetManual(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("jailreset")) {
			log.info("Cleared jails");
			jailedPlayers.clear();
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
		if (target instanceof XivPlayerCharacter) {
			jailedPlayers.add((XivPlayerCharacter) target);
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
		jailedPlayers.sort(Comparator.comparing(player -> {
			Job job = player.getJob();
			if (job.isMeleeDps()) {
				return 1;
			}
			if (job.isTank()) {
				return 2;
			}
			if (job.isCaster()) {
				return 3;
			}
			if (job.isPranged()) {
				return 4;
			}
			if (job.isHealer()) {
				return 5;
			}
			// Shouldn't happen
			log.warn("Couldn't determine jail prio for player {} job {}", player, job);
			return 6;
		}));
		context.accept(new FinalTitanJailsSolvedEvent(jailedPlayers));
		log.info("Unsorted jails: {}", event.getJailedPlayers());
		log.info("Sorted jails: {}", jailedPlayers);
	}

	@HandleEvents
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
				case 1:
					context.accept(new CalloutEvent("First"));
					break;
				case 2:
					context.accept(new CalloutEvent("Second"));
					break;
				case 3:
					context.accept(new CalloutEvent("Third"));
					break;
			}
		}
	}

	@HandleEvents
	public void automarks(EventContext context, FinalTitanJailsSolvedEvent event) {
		if (enableAutomark.get()) {
			List<XivPlayerCharacter> playersToMark = event.getJailedPlayers();
			log.info("Requesting to mark jailed players: {}", playersToMark.stream().map(XivEntity::getName).collect(Collectors.joining(", ")));
			context.accept(new AutoMarkRequest(playersToMark.get(0)));
			context.accept(new AutoMarkRequest(playersToMark.get(1)));
			context.accept(new AutoMarkRequest(playersToMark.get(2)));
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
}
