package gg.xp.xivsupport.events.triggers.duties.ewex;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcAbilityUsedCallout;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@CalloutRepo(name = "EX6", duty = KnownDuty.GolbezEx)
public class EX6 extends AutoChildEventHandler implements FilteredEventHandler {

	private final ArenaPos ap = new ArenaPos(100, 100, 5, 5);
	private XivState state;
	private ActiveCastRepository acr;

	public EX6(XivState state, ActiveCastRepository acr) {
		this.state = state;
		this.acr = acr;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.GolbezEx);
	}

	private final ModifiableCallout<AbilityCastStart> phasesOfBlade = ModifiableCallout.durationBasedCall("Phases of the Blade", "Back then Front");
	private final ModifiableCallout<AbilityUsedEvent> phasesOfBladeFup = new ModifiableCallout<>("Phases of the Blade Followup", "Front");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> phasesOfBladeSq = SqtTemplates.sq(15_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x86db),
			(e1, s) -> {
				s.updateCall(phasesOfBlade, e1);
				AbilityUsedEvent e2 = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(e1.getAbility().getId()));
				s.updateCall(phasesOfBladeFup, e2);
			});

	@NpcCastCallout(0x84B3)
	private final ModifiableCallout<AbilityCastStart> bindingCold = ModifiableCallout.<AbilityCastStart>durationBasedCall("Binding Cold", "Raidwide with DoT").statusIcon(0x823);

	@NpcCastCallout(0x84AD)
	private final ModifiableCallout<AbilityCastStart> voidMeteor = ModifiableCallout.durationBasedCall("Void Meteor", "Buster, Multiple Hits");

	@NpcCastCallout(0x8471)
	private final ModifiableCallout<AbilityCastStart> blackFang = ModifiableCallout.durationBasedCall("Void Meteor", "Raidwide, Multiple Hits");

	private final ModifiableCallout<AbilityCastStart> twister1 = ModifiableCallout.durationBasedCall("Lingering Spark (Wait)", "Twister");
	private final ModifiableCallout<AbilityCastStart> twister2 = ModifiableCallout.durationBasedCall("Lingering Spark (Move)", "Move");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> twisterSq = SqtTemplates.sq(20_000, AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x8468),
			(e1, s) -> {
				s.updateCall(twister1, e1);
				AbilityCastStart e2 = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x846A));
				s.updateCall(twister2, e2);
			});

	private final ModifiableCallout<HeadMarkerEvent> towersKb = new ModifiableCallout<>("Towers: KB on You", "Knockback");
	private final ModifiableCallout<HeadMarkerEvent> towersCleave = new ModifiableCallout<>("Towers: Cleave on You", "Cleave");
	private final ModifiableCallout<HeadMarkerEvent> towersFlare = new ModifiableCallout<>("Towers: Flare on You", "Flare");
	private final ModifiableCallout<HeadMarkerEvent> towersNothing = new ModifiableCallout<>("Towers: Nothing on You", "Tower");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> towersSq = SqtTemplates.sq(30_000, HeadMarkerEvent.class,
			hme -> hme.getMarkerOffset() == 130,
			(kb, s) -> {
				List<HeadMarkerEvent> flares = s.waitEventsQuickSuccession(2, HeadMarkerEvent.class, hme -> hme.getMarkerOffset() == 129, Duration.ofSeconds(1));
				HeadMarkerEvent myFlare = flares.stream().filter(hme -> hme.getTarget().isThePlayer()).findFirst().orElse(null);
				if (kb.getTarget().isThePlayer()) {
					s.updateCall(towersKb, kb);
					HeadMarkerEvent kbFollowUp = s.waitEvent(HeadMarkerEvent.class, hme -> hme.getTarget().isThePlayer());
					s.updateCall(towersCleave, kbFollowUp);
				}
				else if (myFlare != null) {
					s.updateCall(towersFlare, myFlare);
				}
				else {
					s.updateCall(towersNothing);
				}
			});

	private final ModifiableCallout<AbilityCastStart> exaflares1 = new ModifiableCallout<>("Exaflares 1", "Exaflares and Buddies");
	private final ModifiableCallout<AbilityCastStart> exaflares2 = new ModifiableCallout<>("Exaflares 2", "Exaflares and Buddies");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> exaflaresSq = SqtTemplates.multiInvocation(60_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x84A4),
			(e1, s) -> {
				s.updateCall(exaflares1, e1);
			}, (e1, s) -> {
				s.updateCall(exaflares2, e1);
			});

	@NpcCastCallout(value = 0x84AB, suppressMs = 1000)
	private final ModifiableCallout<AbilityCastStart> abyssalQuasar = ModifiableCallout.durationBasedCall("Abyssal Quasar", "Partners");
	@NpcCastCallout(value = 0x845C, suppressMs = 1000)
	private final ModifiableCallout<AbilityCastStart> voidAeroIII = ModifiableCallout.durationBasedCall("Void Aero III", "Partners");
	@NpcCastCallout(value = 0x8496, suppressMs = 1000)
	private final ModifiableCallout<AbilityCastStart> immolatingShade = ModifiableCallout.durationBasedCall("Immolating Shade", "Healer Stacks");
	@NpcCastCallout(value = 0x8462, suppressMs = 1000)
	private final ModifiableCallout<AbilityCastStart> voidBlizzardIII = ModifiableCallout.durationBasedCall("Void Blizzard III", "Healer Stacks");

	@NpcCastCallout(0x8480)
	private final ModifiableCallout<AbilityCastStart> eventideTriad = ModifiableCallout.durationBasedCall("Eventide Triad", "Role Cleaves");
	@NpcCastCallout(0x8484)
	private final ModifiableCallout<AbilityCastStart> eventideFall = ModifiableCallout.durationBasedCall("Eventide Fall", "Light Parties");

	private final ModifiableCallout<?> azdajasShadowOutStack = new ModifiableCallout<>("Azdaja's Shadow: Stocking Out/Stack", "Out and Healer Stacks Later");
	private final ModifiableCallout<?> azdajasShadowInSpread = new ModifiableCallout<>("Azdaja's Shadow: Stocking In/Spread", "In and Spread Later");
	private final ModifiableCallout<AbilityCastStart> phasesOfShadowBackFront = ModifiableCallout.durationBasedCall("Phases of the Shadow", "Back then Front, {in ? \"In then Spread\" : \"Out then Stack\"}");
	private final ModifiableCallout<AbilityCastStart> phasesOfShadowFront = ModifiableCallout.durationBasedCall("Phases of the Shadow", "Front");
	private final ModifiableCallout<AbilityCastStart> phasesOfShadowIn = ModifiableCallout.durationBasedCall("Phases of the Shadow", "In then Spread");
	private final ModifiableCallout<AbilityCastStart> phasesOfShadowOut = ModifiableCallout.durationBasedCall("Phases of the Shadow", "Out then Stacks");
	private final ModifiableCallout<AbilityCastStart> phasesOfShadowSpread = ModifiableCallout.durationBasedCall("Phases of the Shadow", "Spread");
	private final ModifiableCallout<AbilityCastStart> phasesOfShadowStacks = ModifiableCallout.durationBasedCall("Phases of the Shadow", "Healer Stacks");
//	private ModifiableCallout<AbilityCastStart> phasesOfShadow = ModifiableCallout.durationBasedCall("Phases of the Shadow", "Back then Front, {in ? \"In then Spread\" : \"Out then Stack\"}");

	@NpcAbilityUsedCallout({0x8478, 0x8479})
	private final ModifiableCallout<AbilityUsedEvent> flamesOfEventide = new ModifiableCallout<>("Flames of Eventide", "Cleave Busters");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> phasesOfShadowSq = SqtTemplates.sq(90_000, AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x8478, 0x8479),
			(e1, s) -> {
				boolean in;
				if (e1.abilityIdMatches(0x8478)) {
					in = false;
					s.updateCall(azdajasShadowOutStack);
				}
				else {
					in = true;
					s.updateCall(azdajasShadowInSpread);
				}
				s.setParam("in", in);
				// TODO: 86E7 vs 86E6
				AbilityCastStart backFrontCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x86E6, 0x86E7));
				s.updateCall(phasesOfShadowBackFront, backFrontCast);
				// TODO: 86EE vs 86EF
				// Collect these for later
				AbilityCastStart frontCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x86EE, 0x86EF));
				AbilityCastStart donutOrCircle = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x86EC, 0x86ED));
				AbilityUsedEvent backHit = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(backFrontCast.getAbility().getId()));
				s.updateCall(phasesOfShadowFront, frontCast);
				AbilityUsedEvent frontHit = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(frontCast.getAbility().getId()));
				if (in) {
					s.updateCall(phasesOfShadowIn, donutOrCircle);
				}
				else {
					s.updateCall(phasesOfShadowOut, donutOrCircle);
				}
				AbilityUsedEvent donutHit = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(donutOrCircle.getAbility().getId()));
				AbilityCastStart stackOrSpread = acr.getActiveCastById(0x8494, 0x8496).map(CastTracker::getCast).orElse(null);
				if (in) {
					s.updateCall(phasesOfShadowSpread, stackOrSpread);
				}
				else {
					s.updateCall(phasesOfShadowStacks, stackOrSpread);
				}
			}
	);

	private final ModifiableCallout<?> gale1 = new ModifiableCallout<>("Gale: Initial", "{firingOrder[0]}, {firingOrder[1]}, {firingOrder[2]}, {firingOrder[3]}");
	private final ModifiableCallout<?> gale2 = new ModifiableCallout<>("Gale: Second", "{firingOrder[1]}", "{firingOrder[1]}, {firingOrder[2]}, {firingOrder[3]}");
	private final ModifiableCallout<?> gale3 = new ModifiableCallout<>("Gale: Third", "{firingOrder[2]}", "{firingOrder[2]}, {firingOrder[3]}");
	private final ModifiableCallout<?> gale4 = new ModifiableCallout<>("Gale: Fourth", "{firingOrder[3]}", "{firingOrder[3]}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> gale = SqtTemplates.sq(30_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8458),
			(e1, s) -> {
				s.waitThenRefreshCombatants(100);
				List<ArenaSector> firingOrder = state.npcsById(16218)
						.stream()
						.sorted(Comparator.comparing(cbt -> -cbt.getId()))
						.map(ap::forCombatant)
						.toList();
				s.setParam("firingOrder", firingOrder);
				s.updateCall(gale1);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8458));
				s.updateCall(gale2);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8459));
				s.updateCall(gale3);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x845A));
				s.updateCall(gale4);
			});

}
