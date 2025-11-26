package gg.xp.xivsupport.events.fflogs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.EntityKilledEvent;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.LimitBreakGaugeEvent;
import gg.xp.xivsupport.events.actlines.events.PlayerStats;
import gg.xp.xivsupport.events.actlines.events.PlayerStatsUpdatedEvent;
import gg.xp.xivsupport.events.actlines.events.RawJobGaugeEvent;
import gg.xp.xivsupport.events.actlines.events.RawPlayerChangeEvent;
import gg.xp.xivsupport.events.actlines.events.TargetabilityUpdate;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.actlines.events.TickEvent;
import gg.xp.xivsupport.events.actlines.events.TickType;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.VictoryEvent;
import gg.xp.xivsupport.events.actlines.parsers.FakeFflogsTimeSource;
import gg.xp.xivsupport.events.state.InCombatChangeEvent;
import gg.xp.xivsupport.events.state.RawXivCombatantInfo;
import gg.xp.xivsupport.events.state.RawXivPartyInfo;
import gg.xp.xivsupport.events.state.XivStateImpl;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivStatusEffect;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class FflogsEventProcessor {
	private static final Logger log = LoggerFactory.getLogger(FflogsEventProcessor.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	private final AtomicInteger counter = new AtomicInteger();
	private final Map<Long, Long> fflogsProvidedMapping = new HashMap<>();
	private final Map<Long, Function<Long, RawXivCombatantInfo>> cbtInstanceMapping = new HashMap<>();
	private final List<XivPlayerCharacter> partyList = new ArrayList<>();
	private final XivStateImpl state;
	private final @Nullable FakeFflogsTimeSource fakeTimeSource;

	// TODO: move more stuff to XivState interface
	public FflogsEventProcessor(PicoContainer container, XivStateImpl state) {
		this.state = state;
		fakeTimeSource = container.getComponent(FakeFflogsTimeSource.class);
	}

	private @Nullable XivCombatant getCombatant(@Nullable Long id, @Nullable Long instanceId) {
		if (id == null) {
			return null;
		}
		else if (id == -1) {
			return XivCombatant.ENVIRONMENT;
		}
		if (instanceId == null || instanceId == 0) {
			instanceId = 1L;
		}
		Long cbtId = fflogsProvidedMapping.get(id);
		if (cbtId != null) {
			cbtId = cbtId + instanceId - 1;
			XivCombatant knownCbt = state.getCombatant(cbtId);
			if (knownCbt != null) {
				return knownCbt;
			}
			else {
				Function<Long, RawXivCombatantInfo> mapping = cbtInstanceMapping.get(id);
				if (mapping != null) {
					log.info("Calculating instanced combatant: ID: {}, Inst: {}", id, instanceId);
					RawXivCombatantInfo applied = mapping.apply(instanceId);
					state.setSpecificCombatants(Collections.singletonList(applied));
					return state.getCombatant(applied.getId());
				}
			}
		}
		// Fallback plan
		// Use a distinct ID range so as to not collide with known combatants
		if (cbtId == null) {
			cbtId = 0x4800_0000 + id * 0x100 + instanceId - 1;
		}
		XivCombatant knownCbt = state.getCombatant(cbtId);
		if (knownCbt == null) {
			return new XivCombatant(id, "Unknown #%s inst %s".formatted(id, instanceId));
		}
		else {
			return knownCbt;
		}
	}

	private static double convertCoordinate(long rawCoord) {
		return (rawCoord) / 100.0;
	}

	private @Nullable XivCombatant getCombatant(@Nullable Long id, @Nullable Long instanceId, @Nullable Object resourcesRaw) {
		XivCombatant cbt = getCombatant(id, instanceId);
		if (cbt != null && resourcesRaw != null) {
			RawResources resources = mapper.convertValue(resourcesRaw, RawResources.class);
			HitPoints hp = new HitPoints(resources.hitPoints, resources.maxHitPoints);
			Position pos = new Position(convertCoordinate(resources.x), convertCoordinate(resources.y), resources.z, resources.convertHeading());
			state.provideCombatantShieldPct(cbt, resources.absorb);
			state.provideCombatantHP(cbt, hp);
			state.provideCombatantMP(cbt, new ManaPoints(resources.mp, resources.maxMP));
			state.provideCombatantPos(cbt, pos);
		}
		return cbt;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record RawResources(
			long hitPoints,
			long maxHitPoints,
			long mp,
			long maxMP,
			long x,
			long y,
			long z,
			long facing,
			long absorb
	) {
		double convertHeading() {
			// First part converts to where 0 is south
			var p1 = -facing + -150.0 * Math.PI;
			// Then, normalize magnitude so that a full rotation is '1'
			var p2 = p1 / (200 * Math.PI);
			// Then, normalize back to circle
			return p2 * 2 * Math.PI;
		}
	}


	@JsonIgnoreProperties(ignoreUnknown = true)
	private record CombatantsInfoAura(
			long source,
			long ability,
			long stacks,
			String name
	) {
	}

	private static Job convertJob(String jobName) {
		jobName = jobName.replaceAll(" ", "");
		String finalJobName = jobName;
		return Arrays.stream(Job.values()).filter(j -> j.getFriendlyName().replaceAll(" ", "").equalsIgnoreCase(finalJobName))
				.findFirst()
				.orElse(Job.ADV);
	}

	@HandleEvents
	public void processFflogsMasterData(EventContext context, FflogsMasterDataEvent masterData) {
		// TODO: Environment is E0000000
		List<FflogsMasterDataEvent.Actor> actors = masterData.getActors();
		List<RawXivCombatantInfo> combatants = actors.stream()
				.map(actor -> {
					long rawId = actor.gameID();

					boolean isPlayer = actor.type().equals("Player");
					long id = isPlayer ? (rawId % 10_000 + 0x1000_000) : (rawId * 100 + 0x4000_0000);
					fflogsProvidedMapping.put(actor.id(), id);
					// TODO: this owner ID doesn't actually work
					Long rawOwnerId = actor.petOwner();
					long ownerId;
					int type;
					if (rawOwnerId == null) {
						ownerId = 0L;
						type = isPlayer ? 1 : 2;
					}
					else {
						// 3 = pet
						type = 3;
						ownerId = rawOwnerId;
					}
					long npcId = isPlayer ? 0 : rawId;
					Function<Long, RawXivCombatantInfo> rawDataProducer = i -> new RawXivCombatantInfo(id - 1 + i, actor.name(), isPlayer ? convertJob(actor.subType()).getId() : 0, type, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, "TODO", npcId, 0, 0, ownerId);
					cbtInstanceMapping.put(actor.id(), rawDataProducer);
					return rawDataProducer.apply(1L);
				})
				.toList();
		state.setPlayer(new XivEntity(0x1234_5678, "Bogus Player"));
		state.setCombatants(combatants);
	}

	@HandleEvents
	public void processFflogsEvent(EventContext context, FflogsRawEvent rawEvent) {
		if (fakeTimeSource != null) {
			fakeTimeSource.setNewTime(rawEvent.getHappenedAt());
			rawEvent.setTimeSource(fakeTimeSource);
		}

		Object rawType = rawEvent.getField("type");
		XivCombatant source = getCombatant(rawEvent.sourceID(), rawEvent.sourceInstance(), rawEvent.getField("sourceResources"));
		XivCombatant target = getCombatant(rawEvent.targetID(), rawEvent.targetInstance(), rawEvent.getField("targetResources"));
		if (rawType instanceof String type) {
			switch (type) {
				// Leaving possible NPEs in place because those wouldn't make sense anyway
				case "combatantinfo" -> {
					if (source instanceof XivPlayerCharacter pc) {
						if (rawEvent.getTypedField("mind", Long.class, null) != null) {
							context.accept(new RawPlayerChangeEvent(source));
							try {
								context.accept(new PlayerStatsUpdatedEvent(
										new PlayerStats(
												((XivPlayerCharacter) source).getJob(),
												rawEvent.getTypedField("strength", Integer.class, 0),
												rawEvent.getTypedField("dexterity", Integer.class, 0),
												rawEvent.getTypedField("vitality", Integer.class, 0),
												rawEvent.getTypedField("intelligence", Integer.class, 0),
												rawEvent.getTypedField("mind", Integer.class, 0),
												rawEvent.getTypedField("piety", Integer.class, 0),
												rawEvent.getTypedField("attack", Integer.class, 0),
												rawEvent.getTypedField("directHit", Integer.class, 0),
												rawEvent.getTypedField("criticalHit", Integer.class, 0),
												rawEvent.getTypedField("attackMagicPotency", Integer.class, 0),
												rawEvent.getTypedField("healMagicPotency", Integer.class, 0),
												rawEvent.getTypedField("determination", Integer.class, 0),
												rawEvent.getTypedField("skillSpeed", Integer.class, 0),
												rawEvent.getTypedField("spellSpeed", Integer.class, 0),
												rawEvent.getTypedField("tenacity", Integer.class, 0)
										)
								));
							}
							catch (Throwable t) {
								log.error("Error loading player stats", t);
							}
						}
						partyList.add(pc);
						state.setPartyList(partyList.stream().map(pm -> new RawXivPartyInfo(pm.getId(), pm.getName(), 0, pm.getJob().getId(), (int) pm.getLevel(), true)).toList());
					}
					Object aurasRaw = rawEvent.getField("auras");
					if (aurasRaw != null) {
						List<CombatantsInfoAura> auras = mapper.convertValue(aurasRaw, new TypeReference<>() {
						});
						auras.forEach(aura -> {
							XivCombatant auraSource = getCombatant(aura.source, null);
							context.accept(new BuffApplied(convertStatus(aura.ability % 1_000_000), 9999, auraSource, source, aura.stacks));
						});
					}
				}
				case "damage", "calculateddamage" -> {
					long amount = rawEvent.getTypedField("amount", Long.class, 0L);
					if (rawEvent.getTypedField("tick", boolean.class, false)) {
						long rawEffectId = rawEvent.abilityId();
						// 500k is "combined"
						long effectId = rawEffectId == 500_000 ? 0 : rawEffectId % 1_000_000;
						context.accept(new TickEvent(target, TickType.DOT, amount, effectId));
					}
					else {
						context.accept(new GenericDamageEvent(source, target, new XivAbility(rawEvent.abilityId()), amount, rawEvent.severity()));
					}
					if (amount > 0 && !state.inCombat()) {
						context.accept(new InCombatChangeEvent(true));
					}
				}
				case "heal", "calculatedheal" -> {
					long amount = rawEvent.getTypedField("amount", Long.class, 0L);
					if (rawEvent.getTypedField("tick", boolean.class, false)) {
						XivStatusEffect status;
						long rawEffectId = rawEvent.abilityId();
						// 500k is "combined", 0x516 is passive regen
						if (rawEffectId == 0x516) {
							status = new XivStatusEffect(0, "Passive Regen");
						}
						else {
							long effectId = rawEffectId == 500_000 ? 0 : rawEffectId % 1_000_000;
							status = new XivStatusEffect(effectId);
						}
						context.accept(new TickEvent(target, TickType.HOT, amount, status));
					}
					else {
						context.accept(new GenericHealEvent(source, target, new XivAbility(rawEvent.abilityId()), amount, rawEvent.severity()));
					}
				}
				case "begincast" -> {
					double duration;
					Double durationRaw = rawEvent.getTypedField("duration", Double.class);
					if (durationRaw == null) {
						duration = 9999;
					}
					else {
						duration = durationRaw / 1000.0;
					}
					context.accept(new AbilityCastStart(
							new XivAbility(rawEvent.abilityId()),
							source,
							target,
							duration));
				}
				case "cast" -> {
					context.accept(new AbilityUsedEvent(
							new XivAbility(rawEvent.abilityId()),
							source,
							target,
							Collections.emptyList(),
							counter.getAndIncrement(),
							0,
							1));
				}
				case "applybuff", "applybuffstack", "applydebuff", "applydebuffstack", "removebuffstack",
				     "removedebuffstack", "refreshbuff", "refreshdebuff" -> {
					int stacks = rawEvent.getTypedField("stack", int.class, 0);
					double duration;
					Double durationRaw = rawEvent.getTypedField("duration", Double.class);
					if (durationRaw == null) {
						duration = 9999;
					}
					else {
						duration = durationRaw / 1000.0;
					}
					context.accept(new BuffApplied(
							convertStatus(rawEvent.abilityId()),
							duration,
							source,
							target,
							stacks
					));
				}
				case "dispel", "removebuff", "removedebuff" -> {
					context.accept(new BuffRemoved(
							convertStatus(rawEvent.abilityId()),
							0,
							getCombatant(rawEvent.getTypedField("sourceID", Long.class), rawEvent.getTypedField("sourceInstance", Long.class)),
							getCombatant(rawEvent.getTypedField("targetID", Long.class), rawEvent.getTypedField("targetInstance", Long.class)),
							0
					));
				}
				case "gaugeupdate" -> {
					long data0 = rawEvent.getHexField("data1");
					long data1 = rawEvent.getHexField("data2");
					long data2 = rawEvent.getHexField("data3");
					long data3 = rawEvent.getHexField("data4");
					long[] in = {data0, data1, data2, data3};
					byte[] out = new byte[16];
					for (int i = 0; i < 4; i++) {
						long part = in[i];
						for (int j = 0; j < 4; j++) {
							out[4 * i + j] = (byte) (part & 0xff);
							part >>= 8;
						}
					}
					Job job = Job.getById(data0 & 0xff);
					context.accept(new RawJobGaugeEvent(
							getCombatant(rawEvent.getHexField("gaugeID"), null),
							job,
							out
					));
				}
				case "tether" -> {
					context.accept(new TetherEvent(
							source,
							target,
							rawEvent.getTypedField("tetherID", Long.class)
					));
				}
				case "headmarker" -> {
					// fflogs reverses these from what we do - the source is the target
					context.accept(new HeadMarkerEvent(
							target,
							rawEvent.getTypedField("markerID", Long.class)
					));
				}
				case "death" -> {
					context.accept(new EntityKilledEvent(source, target));
				}
				case "encounterend" -> {
					boolean killed = rawEvent.getTypedField("kill", boolean.class, false);
					if (killed) {
						context.accept(new VictoryEvent());
					}
					else {
						context.accept(new WipeEvent());
					}
				}
				case "limitbreakupdate" -> {
					context.accept(new LimitBreakGaugeEvent(
							rawEvent.getTypedField("bars", Integer.class),
							rawEvent.getTypedField("value", Integer.class)));
				}
				case "targetabilityupdate" -> {
					context.accept(new TargetabilityUpdate(
							source, target, rawEvent.getTypedField("targetable", Integer.class) == 1
					));
				}
				default -> {
					context.accept(new FflogsUnsupportedEvent(rawEvent));
				}


			}
		}
		else {
			log.error("Encountered fflogs event with missing or malformed event type: {}", rawEvent.getFields());
		}
		state.flushProvidedValues();

	}

	private static XivStatusEffect convertStatus(long rawStatus) {
		return new XivStatusEffect(rawStatus % 1_000_000);
	}
}
