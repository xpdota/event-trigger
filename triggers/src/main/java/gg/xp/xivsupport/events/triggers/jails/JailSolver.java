package gg.xp.xivsupport.events.triggers.jails;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
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
import gg.xp.xivsupport.persistence.settings.EnumListSetting;
import gg.xp.xivsupport.persistence.settings.LongSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CalloutRepo("Titan Gaols")
public class JailSolver implements FilteredEventHandler {
	private final ModifiableCallout first = new ModifiableCallout("First Jail", "First");
	private final ModifiableCallout second = new ModifiableCallout("Second Jail", "Second");
	private final ModifiableCallout third = new ModifiableCallout("Third Jail", "Third");
	private static final Logger log = LoggerFactory.getLogger(JailSolver.class);

	private final List<XivPlayerCharacter> jailedPlayers = new ArrayList<>();
	private final BooleanSetting enableTts;
	private final BooleanSetting enableAutomark;
	private final BooleanSetting overrideZoneLock;
	private final XivState state;
	private final Set<Job> allValidJobs;
	private List<Job> currentJailSort;
	private final EnumListSetting<Job> sortSetting;
	private final LongSetting jailClearDelay;

	private final Comparator<XivPlayerCharacter> playerJailSortComparator = Comparator.<XivPlayerCharacter, Integer>comparing(player -> {
		int index = currentJailSort.indexOf(player.getJob());
		if (index == -1) {
			// Return a big value so it sorts last
			log.warn("Couldn't determine jail prio for player {}", player);
			return 65536;
		}
		return index;
	}).thenComparing(XivPlayerCharacter::getName);

	private final Comparator<Job> defaultJailSortComparator = Comparator.<Job, Integer>comparing(job -> {
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
		log.warn("Couldn't determine jail prio for job {}", job);
		return 6;
	}).thenComparing(Enum::ordinal);

	// TODO: scope - this is a perfect opportunity
	public JailSolver(PersistenceProvider persistence, XivState state) {
		enableTts = new BooleanSetting(persistence, "jail-solver.tts.enable", true);
		enableAutomark = new BooleanSetting(persistence, "jail-solver.automark.enable", true);
		overrideZoneLock = new BooleanSetting(persistence, "jail-solver.override-zone-lock", false);
		// TODO: add "upgrades to job X" field to Job so that we can just combine
		// jobs and base classes.
		allValidJobs = Arrays.stream(Job.values())
				.filter(Job::isCombatJob)
				.collect(Collectors.toUnmodifiableSet());
		sortSetting = new EnumListSetting<>(Job.class, persistence, "jail-solver.job-order", EnumListSetting.BadKeyBehavior.RETURN_DEFAULT, null);
		List<Job> listFromSettings = sortSetting.get();
		if (listFromSettings != null) {
			try {
				validateJobSortOrder(listFromSettings);
				currentJailSort = listFromSettings;
			}
			catch (Throwable t) {
				log.error("Saved jail order did not pass validation", t);
			}
		}
		// Fall back to default
		if (currentJailSort == null) {
			currentJailSort = allValidJobs
					.stream()
					.sorted(defaultJailSortComparator)
					.collect(Collectors.toUnmodifiableList());
		}
		this.state = state;
		jailClearDelay = new LongSetting(persistence, "jail-solver.clear-delay", 10000L);
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
		jailedPlayers.sort(playerJailSortComparator);
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
					context.accept(first.getModified());
					break;
				case 2:
					context.accept(second.getModified());
					break;
				case 3:
					context.accept(third.getModified());
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

	public List<Job> getCurrentJailSort() {
		return Collections.unmodifiableList(currentJailSort);
	}

	public void resetJailSort() {
		this.currentJailSort = currentJailSort.stream()
				.sorted(defaultJailSortComparator)
				.collect(Collectors.toUnmodifiableList());
		sortSetting.delete();
	}

	private void validateJobSortOrder(List<Job> newSort) {
		int expectedNumberOfJobs = allValidJobs.size();
		int actualNumberOfJobs = newSort.size();
		if (expectedNumberOfJobs != actualNumberOfJobs) {
			throw new IllegalArgumentException(String.format("New jail sort order was not the same size! %s -> %s", expectedNumberOfJobs, actualNumberOfJobs));
		}
		EnumSet<Job> newSortAsSet = EnumSet.copyOf(newSort);
		int newUniqueSize = newSortAsSet.size();
		if (newUniqueSize != actualNumberOfJobs) {
			throw new IllegalArgumentException("New jail sort had duplicates!");
		}
	}

	public void setJailSort(List<Job> newSort) {
		validateJobSortOrder(newSort);
		this.currentJailSort = List.copyOf(newSort);
		saveJailSort();
	}

	private void saveJailSort() {
		this.sortSetting.set(currentJailSort);
	}

	public List<XivPlayerCharacter> partyOrderPreview() {
		return state.getPartyList().stream().sorted(playerJailSortComparator).collect(Collectors.toList());
	}

	public LongSetting getJailClearDelay() {
		return jailClearDelay;
	}

	public BooleanSetting getOverrideZoneLock() {
		return overrideZoneLock;
	}
}
