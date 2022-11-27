package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.ActionInfo;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.Job;
import gg.xp.xivdata.data.StatusEffectInfo;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.ChatLineEvent;
import gg.xp.xivsupport.events.actlines.events.EntityKilledEvent;
import gg.xp.xivsupport.events.actlines.events.RawPlayerChangeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.state.CombatantsUpdateRaw;
import gg.xp.xivsupport.events.state.PartyForceOrderChangeEvent;
import gg.xp.xivsupport.events.state.XivStateImpl;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivStatusEffect;
import gg.xp.xivsupport.models.XivWorld;
import gg.xp.xivsupport.models.XivZone;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrokenACTHack implements FilteredEventHandler {

//	private static final Logger log = LoggerFactory.getLogger(BrokenACTHack.class);
//
//	private final HitPoints fakeHp = new HitPoints(50_000, 50_000);
//	private final ManaPoints fakeMp = ManaPoints.of(10_000, 10_000);
//	private final Position fakePos = new Position(100, 100, 100, 0.0);
//
//	private final MutableInt pcCounter = new MutableInt(0x1000_0001);
//	private final MutableInt npcCounter = new MutableInt(0x4000_0001);
//
//	@NotNull
//	private XivPlayerCharacter makePc(String name, Job job, boolean isLocalPlayerCharacter) {
//		return new XivPlayerCharacter(pcCounter.getAndIncrement(), name, job, XivWorld.of(), isLocalPlayerCharacter, 1, fakeHp, fakeMp, fakePos, 0, 0, 1, 90, 0, 0);
//	}
//
//	@NotNull
//	private XivCombatant makeNpc(String name, long npcId, long npcNameId) {
//		return new XivCombatant(npcCounter.getAndIncrement(), name, false, false, 2, fakeHp, fakeMp, fakePos, npcId, npcNameId, 0, 90, 0, 0);
//	}
//
//	private final XivPlayerCharacter pc1 = makePc("Wynn Dohz", Job.SGE, true);
//	private final XivPlayerCharacter pc2 = makePc("Mari Haunt", Job.PLD, false);
//	private final XivPlayerCharacter pc3 = makePc("Fay Chan", Job.DRK, false);
//	private final XivPlayerCharacter pc4 = makePc("Kait Pyre", Job.WHM, false);
//	private final XivPlayerCharacter pc5 = makePc("Sensha Desu", Job.DRG, false);
//	private final XivPlayerCharacter pc6 = makePc("Tenebris Solais", Job.NIN, false);
//	private final XivPlayerCharacter pc7 = makePc("Cherry Raven", Job.BRD, false);
//	private final XivPlayerCharacter pc8 = makePc("Jane Mayus", Job.SMN, false);
//
//	private final XivCombatant thordan = makeNpc("King Thordan", 12604, 3632);
//	private final XivCombatant grin = makeNpc("Ser Grinnaux", 12602, 3639);
//	private final XivCombatant adel = makeNpc("Ser Adelphel", 12601, 3634);
//	private final XivCombatant jan = makeNpc("Ser Janlenoux", 12632, 3635);
//
//	private final XivCombatant vedr = makeNpc("Vedrfolnir", 12646, 3984);
//	private final XivCombatant hraes = makeNpc("Hraesvelgr", 12613, 4954);
//	private final XivCombatant nidd = makeNpc("Nidhogg", 12612, 3458);
//
//
//	private final List<XivPlayerCharacter> theParty = List.of(pc1, pc2, pc3, pc4, pc5, pc6, pc7, pc8);
//	private final List<XivCombatant> allEntities = List.of(
//			pc1, pc2, pc3, pc4, pc5, pc6, pc7, pc8,
//			thordan, grin, adel, jan, vedr, hraes, nidd);
//	private final XivStateImpl state;
//
//	public BrokenACTHack(XivStateImpl state) {
//		this.state = state;
//	}
//
//
//	private boolean enabled;
//
	@Override
	public boolean enabled(EventContext context) {
		return true;
//		return enabled;
	}

//	private XivCombatant getCombatantByName(String name) {
//		if (name == null || name.isBlank()) {
//			return XivCombatant.ENVIRONMENT;
//		}
//		if (name.equalsIgnoreCase("you")) {
//			return pc1;
//		}
//		Matcher matcher = stripServer.matcher(name);
//		if (matcher.matches()) {
//			name = matcher.group(1);
//		}
//		String finalName = name;
//		return state.getCombatantsListCopy().stream().filter(cbt -> cbt.getName().equalsIgnoreCase(finalName)).findAny().orElse(XivCombatant.ENVIRONMENT);
//	}
//
//	private static final long UNKNOWN_STATUS = -1;
//	private final HashMap<String, Long> statusCache = new HashMap<>();
//
//	private XivStatusEffect getStatusByName(String name) {
//		long id = statusCache.computeIfAbsent(name, n -> StatusEffectLibrary.getAll()
//				.values()
//				.stream()
//				.filter(status -> status.name().equalsIgnoreCase(n))
//				.sorted(Comparator.comparing(StatusEffectInfo::statusEffectId).reversed())
//				.mapToLong(StatusEffectInfo::statusEffectId)
//				.findFirst()
//				.orElse(UNKNOWN_STATUS));
//		if (id == UNKNOWN_STATUS) {
//			id = 0x000F0000 + name.hashCode() & 0xFFFF;
//		}
//		return new XivStatusEffect(id, name);
//	}
//
//	private XivAbility getAbilityByName(String name) {
//		if (name.equalsIgnoreCase("Geirskogul")) {
//			return new XivAbility(0x6711);
//		}
//		long id = statusCache.computeIfAbsent(name, n -> ActionLibrary.getAll()
//				.values()
//				.stream()
//				.filter(status -> status.name().equalsIgnoreCase(n))
//				.sorted(Comparator.comparing(ActionInfo::actionid).reversed())
//				.mapToLong(ActionInfo::actionid)
//				.findFirst()
//				.orElse(UNKNOWN_STATUS));
//		if (id == UNKNOWN_STATUS) {
//			id = 0x000F0000 + name.hashCode() & 0xFFFF;
//		}
//		return new XivAbility(id, name);
//	}
//
//	@HandleEvents
//	public void handleEvents(EventContext context, DebugCommand event) {
//		if (event.getCommand().equals("dsr")) {
//			log.warn("!!!Enabled broken ACT hack!!!");
//			enabled = true;
//			context.accept(new ZoneChangeEvent(new XivZone(0x3C8, "Dragonsong fake zone")));
//			context.accept(new CombatantsUpdateRaw(allEntities.stream().map(XivCombatant::toRaw).toList(), true));
//			context.accept(new RawPlayerChangeEvent(pc1));
//			context.accept(new PartyForceOrderChangeEvent(theParty.stream().map(XivEntity::getId).toList()));
//		}
//	}
//
//	private final Pattern stripServer = Pattern.compile("([A-Z][a-z'-]+ [A-Z][a-z'-]+)([A-Z][a-z]+)");
//	private final Pattern killEvent = Pattern.compile("(?<target>.*) (?:are|is) defeated(?: by (?<source>[^.]*))?");
//	private final Pattern buffGain = Pattern.compile("([A-Za-z1-3].*) (?:gain|gains|suffer|suffers) the effect of ([^.]+)");
//	private final Pattern buffLoss = Pattern.compile("([A-Za-z1-3].*) (?:lose|loses|recover from|recovers from) the effect of ([^.]+)");
//	private final Pattern castStart = Pattern.compile("([A-Za-z1-3].*) (?:begins? casting|readies|ready) ([^.]+)");
//	private final Pattern used = Pattern.compile("([A-Za-z1-3].*) (?:uses?|casts) ([^.]+)");
//
//	@HandleEvents
//	public void handleEvents(EventContext context, ChatLineEvent cle) {
//		if (!enabled) {
//			return;
//		}
//		String line = cle.getLine();
//		// Strip those weird characters
//		line = line.replaceAll("[\\uE000-\\uEFFFF]", "");
//		{
//			final Matcher killMatch;
//			if ((killMatch = killEvent.matcher(line)).find()) {
//				context.accept(new EntityKilledEvent(getCombatantByName(killMatch.group("source")), getCombatantByName(killMatch.group("target"))));
//				return;
//			}
//		}
//		{
//			final Matcher buffGainMatch;
//			if ((buffGainMatch = buffGain.matcher(line)).find()) {
//				context.accept(new BuffApplied(getStatusByName(buffGainMatch.group(2)), 30.0, XivCombatant.ENVIRONMENT, getCombatantByName(buffGainMatch.group(1)), 0));
//				return;
//			}
//		}
//		{
//			final Matcher buffLossMatch;
//			if ((buffLossMatch = buffLoss.matcher(line)).find()) {
//				context.accept(new BuffRemoved(getStatusByName(buffLossMatch.group(2)), 30.0, XivCombatant.ENVIRONMENT, getCombatantByName(buffLossMatch.group(1)), 0));
//				return;
//			}
//		}
//		{
//			final Matcher castStartMatch;
//			if ((castStartMatch = castStart.matcher(line)).find()) {
//				context.accept(new AbilityCastStart(getAbilityByName(castStartMatch.group(2)), getCombatantByName(castStartMatch.group(1)), XivCombatant.ENVIRONMENT, 5.0));
//				return;
//			}
//		}
//		{
//			final Matcher usesMatch;
//			if ((usesMatch = used.matcher(line)).find()) {
//				context.accept(new AbilityUsedEvent(getAbilityByName(usesMatch.group(2)), getCombatantByName(usesMatch.group(1)), XivCombatant.ENVIRONMENT, Collections.emptyList(), 1, 0, 1));
//				return;
//			}
//		}
//	}
//

}
