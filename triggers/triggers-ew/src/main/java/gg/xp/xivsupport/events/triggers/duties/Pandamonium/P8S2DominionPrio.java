package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.OverridesCalloutGroupEnabledSetting;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastResult;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.JobSortSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@CalloutRepo(name = "P8S Final Boss Dominion Priority", duty = KnownDuty.P8S)
public class P8S2DominionPrio extends AutoChildEventHandler implements FilteredEventHandler, OverridesCalloutGroupEnabledSetting {
	private static final Logger log = LoggerFactory.getLogger(P8S2DominionPrio.class);

	private final JobSortSetting sortSetting;
	private final XivState state;
	private final ActiveCastRepository acr;
	private final BooleanSetting enabled;

	public P8S2DominionPrio(XivState state, ActiveCastRepository acr, PersistenceProvider pers) {
		this.state = state;
		this.acr = acr;
		enabled = new BooleanSetting(pers, "triggers.p8s2.dominion-prio.enabled", false);
		sortSetting = new JobSortSetting(pers, "triggers.p8s2.dominion-prio.job-prio", state);
	}

	@Override
	public boolean enabled(EventContext context) {
		return enabled.get() && state.dutyIs(KnownDuty.P8S);
	}

	public JobSortSetting getSortSetting() {
		return sortSetting;
	}

	private XivState getState() {
		return state;
	}

	private ActiveCastRepository getAcr() {
		return acr;
	}

	public BooleanSetting getEnabledSetting() {
		return enabled;
	}

	private final ModifiableCallout<?> dominionFirstSet1 = new ModifiableCallout<>("Dominion: First Towers, Pos 1", "First Set, 1");
	private final ModifiableCallout<?> dominionFirstSet2 = new ModifiableCallout<>("Dominion: First Towers, Pos 2", "First Set, 2");
	private final ModifiableCallout<?> dominionFirstSet3 = new ModifiableCallout<>("Dominion: First Towers, Pos 3", "First Set, 3");
	private final ModifiableCallout<?> dominionFirstSet4 = new ModifiableCallout<>("Dominion: First Towers, Pos 4", "First Set, 4");
	private final ModifiableCallout<?> dominionSecondSet1 = new ModifiableCallout<>("Dominion: Second Towers, Pos 1", "Second Set, 1");
	private final ModifiableCallout<?> dominionSecondSet2 = new ModifiableCallout<>("Dominion: Second Towers, Pos 2", "Second Set, 2");
	private final ModifiableCallout<?> dominionSecondSet3 = new ModifiableCallout<>("Dominion: Second Towers, Pos 3", "Second Set, 3");
	private final ModifiableCallout<?> dominionSecondSet4 = new ModifiableCallout<>("Dominion: Second Towers, Pos 4", "Second Set, 4");

	private final ModifiableCallout<?> tower1 = new ModifiableCallout<>("Tower Position 1 (W)", "Tower 1");
	private final ModifiableCallout<?> tower2 = new ModifiableCallout<>("Tower Position 2 (WNW)", "Tower 2");
	private final ModifiableCallout<?> tower3 = new ModifiableCallout<>("Tower Position 3 (NW)", "Tower 3");
	private final ModifiableCallout<?> tower4 = new ModifiableCallout<>("Tower Position 4 (NNW)", "Tower 4");
	private final ModifiableCallout<?> tower5 = new ModifiableCallout<>("Tower Position 5 (N)", "Tower 5");
	private final ModifiableCallout<?> tower6 = new ModifiableCallout<>("Tower Position 6 (NNE)", "Tower 6");
	private final ModifiableCallout<?> tower7 = new ModifiableCallout<>("Tower Position 7 (NE)", "Tower 7");
	private final ModifiableCallout<?> tower8 = new ModifiableCallout<>("Tower Position 8 (ENE)", "Tower 8");
	private final ModifiableCallout<?> tower9 = new ModifiableCallout<>("Tower Position 9 (E)", "Tower 9");
	private final Map<Position, ModifiableCallout<?>> positionsToCallouts = new HashMap<>();

	{
		positionsToCallouts.put(Position.of2d(82.00, 100.00), tower1);
		positionsToCallouts.put(Position.of2d(83.37, 93.12), tower2);
		positionsToCallouts.put(Position.of2d(87.27, 87.27), tower3);
		positionsToCallouts.put(Position.of2d(93.11, 83.37), tower4);
		positionsToCallouts.put(Position.of2d(100.00, 82.00), tower5);
		positionsToCallouts.put(Position.of2d(106.89, 83.37), tower6);
		positionsToCallouts.put(Position.of2d(112.73, 87.27), tower7);
		positionsToCallouts.put(Position.of2d(116.63, 93.11), tower8);
		positionsToCallouts.put(Position.of2d(118.00, 100.00), tower9);
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> dominionSq = SqtTemplates.sq(30_000, AbilityCastStart.class,
			acs -> acs.abilityIdMatches(31193),
			(e1, s) -> {
				List<XivPlayerCharacter> firstSet = new ArrayList<>(getState().getPartyList());
				List<XivPlayerCharacter> secondSet = new ArrayList<>(4);
				s.waitEventsQuickSuccession(4, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(31195) && aue.isFirstTarget(), Duration.ofMillis(100))
						.stream()
						.map(AbilityUsedEvent::getTarget)
						.forEach(target -> {
							// Players that get hit are in the second set, so remove them from the first list and add them to the second
							if (target instanceof XivPlayerCharacter pc) {
								firstSet.remove(pc);
								secondSet.add(pc);
							}
						});
				XivPlayerCharacter player = getState().getPlayer();
				ModifiableCallout<?> call;
				boolean inFirstSet;
				int playerIndex;
				if (firstSet.contains(player)) {
					inFirstSet = true;
					firstSet.sort(getSortSetting().getPlayerJailSortComparator());
					call = switch (playerIndex = firstSet.indexOf(player)) {
						case 0 -> dominionFirstSet1;
						case 1 -> dominionFirstSet2;
						case 2 -> dominionFirstSet3;
						case 3 -> dominionFirstSet4;
						default ->
								throw new IllegalArgumentException("Dominion: Error: Bad player Index: " + firstSet.indexOf(player) + ". List was: " + firstSet);
					};
				}
				else if (secondSet.contains(player)) {
					inFirstSet = false;
					secondSet.sort(getSortSetting().getPlayerJailSortComparator());
					call = switch (playerIndex = secondSet.indexOf(player)) {
						case 0 -> dominionSecondSet1;
						case 1 -> dominionSecondSet2;
						case 2 -> dominionSecondSet3;
						case 3 -> dominionSecondSet4;
						default ->
								throw new IllegalArgumentException("Dominion: Error: Bad player Index: " + secondSet.indexOf(player) + ". List was: " + secondSet);
					};
				}
				else {
					log.error("Dominion: Error: Player was not in either list! First set: {}, Second set: {}", firstSet, secondSet);
					return;
				}
				s.updateCall(call.getModified());
				List<XivCombatant> towers;
				// First get the four tower combatants
				if (inFirstSet) {
					do {
						s.waitMs(100);
						// If you're in the first set, the casts may have already started, so use the ACR to find them.
						// Make sure we're actually seeing 4 towers, otherwise refresh and try again
						towers = getAcr().getAll().stream()
								.filter(tracker -> tracker.getCast().abilityIdMatches(31196) && tracker.getResult() == CastResult.IN_PROGRESS)
								.map(tracker -> tracker.getCast().getSource())
								.toList();
					} while (towers.size() != 4);
				}
				else {
					// If second set, wait for the casts
					// I'm not 100% sure that the first set of casts are always after the first set of debuffs, so I want to make sure
					s.waitMs(1000);
					towers = s.waitEventsQuickSuccession(4, AbilityCastStart.class, acs -> acs.abilityIdMatches(31196), Duration.ofMillis(100))
							.stream()
							.map(AbilityCastStart::getTarget)
							.toList();
				}
				s.waitThenRefreshCombatants(100);
				while (true) {
					// Make sure the four towers have actually updated their positions
					List<? extends ModifiableCallout<?>> callouts = towers.stream()
							.map(cbt -> getState().getLatestCombatantData(cbt))
							.map(XivCombatant::getPos)
							.filter(Objects::nonNull)
							.sorted(Comparator.comparing(Position::x))
							.map(cbt -> positionsToCallouts.entrySet().stream().filter(e -> e.getKey().distanceFrom2D(cbt) < 1).findFirst().map(Map.Entry::getValue).orElse(null))
							.filter(Objects::nonNull)
							.toList();
					if (callouts.size() == 4) {
						s.updateCall(callouts.get(playerIndex).getModified());
						return;
					}
					else {
						s.waitThenRefreshCombatants(100);
					}
				}

			});

	@Override
	public BooleanSetting getCalloutGroupEnabledSetting() {
		return enabled;
	}
}
