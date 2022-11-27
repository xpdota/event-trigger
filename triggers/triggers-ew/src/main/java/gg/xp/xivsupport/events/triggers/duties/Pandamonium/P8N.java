package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastResult;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.gui.util.HasFriendlyName;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@CalloutRepo(name = "P8N", duty = KnownDuty.P8N)
public class P8N extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P8N.class);
	private final ModifiableCallout<AbilityCastStart> genesisOfFlame = ModifiableCallout.durationBasedCall("Genesis Of Flame", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> rearingRampage = ModifiableCallout.durationBasedCall("Rearing Rampage", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> ektothermos = ModifiableCallout.durationBasedCall("Ektothermos", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> sunforgePhoenix = ModifiableCallout.durationBasedCall("Sunforge Phoenix", "In");
	private final ModifiableCallout<AbilityCastStart> sunforgeSerpent = ModifiableCallout.durationBasedCall("Sunforge Serpent", "Out");
	private final ModifiableCallout<AbilityCastStart> reforgedReflectionQuadruped = ModifiableCallout.durationBasedCall("Reforged Reflection Quadruped", "Quadruped");
	private final ModifiableCallout<AbilityCastStart> reforgedReflectionSerpent = ModifiableCallout.durationBasedCall("Reforged Reflection Serpent", "Serpent");
	private final ModifiableCallout<AbilityCastStart> hemitheosFlare = ModifiableCallout.durationBasedCall("Hemitheos's Flare", "Spread");

	private final ModifiableCallout<AbilityCastStart> fourfoldFiresSafe = ModifiableCallout.durationBasedCall("Fourfold Fires Safe Spot", "{safe}");
	private final ModifiableCallout<AbilityCastStart> flameviper = ModifiableCallout.durationBasedCall("Flameviper", "Tankbuster on {event.target}");
	private final ModifiableCallout<AbilityCastStart> petrifaction = ModifiableCallout.durationBasedCall("Petrifaction", "Look {safe}");
	private final ModifiableCallout<AbilityCastStart> volcanicTorchesSafe = ModifiableCallout.durationBasedCall("Volcanic Torches Safe", "{safe}");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	public P8N(XivState state, ActiveCastRepository activeCastRepository) {
		this.state = state;
		this.activeCastRepository = activeCastRepository;
	}

	private final XivState state;
	private XivState getState() {
		return this.state;
	}

	private final ActiveCastRepository activeCastRepository;
	private ActiveCastRepository getActiveCastRepository() {
		return this.activeCastRepository;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.zoneIs(0x43F);
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		long id = event.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call;
		if (id == 0x7905)
			call = genesisOfFlame;
		else if (id == 0x78EC)
			call = sunforgeSerpent;
		else if (id == 0x78ED)
			call = sunforgePhoenix;
		else if (id == 0x794B)
			call = reforgedReflectionQuadruped;
		else if (id == 0x794C)
			call = reforgedReflectionSerpent;
		else if (id == 0x7908)
			call = flameviper;
		else if (id == 0x79AB)
			call = rearingRampage;
		else if (id == 0x78FE)
			call = ektothermos;
		else if (id == 0x7906)
			call = hemitheosFlare;
		else
			return;

		context.accept(call.getModified(event));
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> cthonicVent = new SequentialTrigger<>(
			10_000,
			BaseEvent.class, event -> event instanceof AbilityCastStart acs && acs.abilityIdMatches(0x78F5, 0x794D, 0x78F6),
			(e1, s) -> {
				List<AbilityCastStart> cthonicCasts = new ArrayList<>(s.waitEvents(1, AbilityCastStart.class, event -> event.abilityIdMatches(0x78F5, 0x794D, 0x78F6)));
				cthonicCasts.add((AbilityCastStart) e1);
				List<XivCombatant> suneaters;
				log.info("CthonicVent: Got suneater casts");
				s.waitMs(100);
				s.refreshCombatants(100);
				log.info("CthonicVent: done with delay");
				suneaters = cthonicCasts.stream().map(acs -> this.getState().getLatestCombatantData(acs.getSource())).collect(Collectors.toList());
				log.info("CthonicVent: done finding positions, finding safe spots");

				if(suneaters.size() != 2) {
					log.error("CthonicVent: Invalid number of suneaters found! Data: {}", cthonicCasts);
					return;
				}
				Set<ArenaSector> safe = EnumSet.copyOf(ArenaSector.quadrants);
				safe.remove(arenaPos.forCombatant(suneaters.get(0)));
				safe.remove(arenaPos.forCombatant(suneaters.get(1)));
				ArenaSector combined = ArenaSector.tryCombineTwoQuadrants(new ArrayList<>(safe));

				Map<String, Object> args = Map.of("safe", combined == null ? safe : combined);
				s.accept(fourfoldFiresSafe.getModified(cthonicCasts.get(0), args));
			}
	);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> volcanicTorches = new SequentialTrigger<>(
			15_000,
			BaseEvent.class, event -> event instanceof AbilityCastStart acs && acs.abilityIdMatches(0x78F8),
			(e1, s) -> {
				s.waitMs(100);
				s.refreshCombatants(100);
				log.info("VolcanicTorches: done with delay");
				List <XivCombatant> tiles = getActiveCastRepository()
						.getAll()
						.stream()
						.filter(castTracker -> castTracker.getCast().abilityIdMatches(0x78F8) && castTracker.getResult() == CastResult.IN_PROGRESS)
						.map(ct -> ct.getCast().getSource())
						.map(cbt -> getState().getLatestCombatantData(cbt))
						.toList();
				log.info("VolcanicTorcher: done finding positions");

				if(tiles.size() != 12 && tiles.size() != 8) {
					log.error("VolcanicTorches: invalid number of tiles found! Data: {}", tiles);
					return;
				}

				Set<VolcanicTorchesColumnsAndRow> safe = EnumSet.copyOf(VolcanicTorchesColumnsAndRow.all);
				for(XivCombatant c : tiles) {
					int x = (int)(Math.round(c.getPos().x()) - 85)/10;
					int y = (int)(Math.round(c.getPos().y()) - 85)/10;
					if (x == 0)
						safe.remove(VolcanicTorchesColumnsAndRow.OUTER_WEST);
					else if (x == 1)
						safe. remove(VolcanicTorchesColumnsAndRow.INNER_WEST);
					else if (x == 2)
						safe.remove(VolcanicTorchesColumnsAndRow.INNER_EAST);
					else if (x == 3)
						safe.remove(VolcanicTorchesColumnsAndRow.OUTER_EAST);
					else
						log.error("Volcanic torches: Invalid x value! x: {}", x);

					if (y == 0)
						safe.remove(VolcanicTorchesColumnsAndRow.OUTER_NORTH);
					else if (y == 1)
						safe.remove(VolcanicTorchesColumnsAndRow.INNER_NORTH);
					else if (y == 2)
						safe.remove(VolcanicTorchesColumnsAndRow.INNER_SOUTH);
					else if (y == 3)
						safe.remove(VolcanicTorchesColumnsAndRow.OUTER_SOUTH);
					else
						log.error("Volcanic torches: Invalid y value! y: {}", y);
				}
				VolcanicTorchesColumnsAndRow combined = VolcanicTorchesColumnsAndRow.tryCombine(new ArrayList<>(safe));

				Map<String, Object> args = Map.of("safe", combined == null ? safe : combined);
				s.accept(volcanicTorchesSafe.getModified((AbilityCastStart) e1, args));
			}
	);

	private enum VolcanicTorchesColumnsAndRow implements HasFriendlyName {

		OUTER_WEST("Outer West"), //x = 85
		INNER_WEST("Inner West"), //x = 95
		OUTER_EAST("Outer East"), //x = 115
		INNER_EAST("Inner East"), //x = 105
		OUTER_NORTH("Outer North"), //y = 85
		INNER_NORTH("Inner North"), //y = 95
		OUTER_SOUTH("Outer South"), //y = 115
		INNER_SOUTH("Inner South"), //y = 105
		HORIZONTAL_MIDDLE("Horizontal Middle"),
		VERTICAL_MIDDLE("Vertical Middle");

		private final String friendlyName;
		public String getFriendlyName() {
			return  friendlyName;
		}

		VolcanicTorchesColumnsAndRow(String friendlyName) {
			this.friendlyName = friendlyName;
		}

		private static final List<VolcanicTorchesColumnsAndRow> all = List.of(OUTER_WEST, INNER_WEST, OUTER_EAST, INNER_EAST, OUTER_NORTH, INNER_NORTH, OUTER_SOUTH, INNER_SOUTH);

		private static @Nullable P8N.VolcanicTorchesColumnsAndRow tryCombine(List<VolcanicTorchesColumnsAndRow> columnsAndRows) {
			if(columnsAndRows.size() != 2) {
				return null;
			}

			columnsAndRows = new ArrayList<>(columnsAndRows);
			columnsAndRows.sort(Comparator.naturalOrder());
			VolcanicTorchesColumnsAndRow first = columnsAndRows.get(0);
			VolcanicTorchesColumnsAndRow second = columnsAndRows.get(1);
			return switch (first) {
				case INNER_WEST -> second == INNER_EAST ? VERTICAL_MIDDLE : null;
				case INNER_NORTH -> second == INNER_SOUTH ? HORIZONTAL_MIDDLE : null;
				default -> null;
			};
		}
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> intoTheShadows = new SequentialTrigger<>(
			13_000,
			BaseEvent.class, event -> event instanceof AbilityCastStart acs && acs.abilityIdMatches(0x78FB),
			(e1, s) -> {
				List<AbilityCastStart> petrifactionCasts = new ArrayList<>(s.waitEvents(2, AbilityCastStart.class, event -> event.abilityIdMatches(0x78FC)));
				List<XivCombatant> gorgons;
				log.info("IntoTheShadows: got gorgon casts");
				s.waitMs(100);
				s.refreshCombatants(100);
				log.info("IntoTheShadows: done with delay");
				gorgons = petrifactionCasts.stream().map(acs -> this.getState().getLatestCombatantData(acs.getSource())).collect(Collectors.toList());

				if(gorgons.size() != 2) {
					log.error("IntoTheShadows: Invalid number of gorgons found! Data: {}", petrifactionCasts);
					return;
				}
				log.info("IntoTheShadows: done finding actors, getting safe directions");

				Set<ArenaSector> safe = EnumSet.copyOf(ArenaSector.all);
				gorgons.stream()
						.map(arenaPos::forCombatant)
						.forEach(badSector -> {
							log.info("IntoTheShadows: Unsafe direction: {}", badSector);
							safe.remove(badSector);
							safe.remove(badSector.plusEighths(1));
							safe.remove(badSector.plusEighths(-1));
							safe.remove(badSector.opposite());
							safe.remove(badSector.opposite().plusEighths(1));
							safe.remove(badSector.opposite().plusEighths(-1));
						});

				Map<String, Object> args = Map.of("safe", safe);
				s.accept(petrifaction.getModified(petrifactionCasts.get(0), args));
			}
	);

	//TODO: blazing footfalls sequential. 7900 boss cast, 790b line kb precast, 790c circle precast. kb precasts happen before 7900 finishes casting.
}
