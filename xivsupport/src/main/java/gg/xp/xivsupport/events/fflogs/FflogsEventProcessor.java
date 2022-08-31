package gg.xp.xivsupport.events.fflogs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.TickEvent;
import gg.xp.xivsupport.events.actlines.events.TickType;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HitSeverity;
import gg.xp.xivsupport.events.actlines.parsers.FakeFflogsTimeSource;
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

public class FflogsEventProcessor {
	private static final Logger log = LoggerFactory.getLogger(FflogsEventProcessor.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	private final AtomicInteger counter = new AtomicInteger();
	private final Map<Long, Long> fflogsProvidedMapping = new HashMap<>();
	private final List<XivPlayerCharacter> partyList = new ArrayList<>();
	private final XivStateImpl state;
	private final @Nullable FakeFflogsTimeSource fakeTimeSource;

	// TODO: move more stuff to XivState interface
	public FflogsEventProcessor(PicoContainer container, XivStateImpl state) {
		this.state = state;
		fakeTimeSource = container.getComponent(FakeFflogsTimeSource.class);
	}

	// TODO: multiple instances
	private @Nullable XivCombatant getCombatant(@Nullable Long id, @Nullable Long instanceId) {
		if (id == null) {
			return null;
		}
		else if (id == -1) {
			return XivCombatant.ENVIRONMENT;
		}
		Long gameId = fflogsProvidedMapping.get(id);
		// Give unique IDs to each instanceId
		if (gameId == null) {
			gameId = 0x4800_0000 + id * 0x100 + (instanceId == null ? 0 : instanceId + 1);
		}
		XivCombatant knownCbt = state.getCombatant(gameId);
		if (knownCbt == null) {
			return new XivCombatant(id, "Combatant " + id);
		}
		else {
			return knownCbt;
		}
	}

	private double convertCoordinate(long rawCoord) {
		return (rawCoord) / 100.0;
	}

	private @Nullable XivCombatant getCombatant(@Nullable Long id, @Nullable Long instanceId, @Nullable Object resourcesRaw) {
		XivCombatant cbt = getCombatant(id, instanceId);
		if (cbt != null && resourcesRaw != null) {
			RawResources resources = mapper.convertValue(resourcesRaw, RawResources.class);
			HitPoints hp = new HitPoints(resources.hitPoints, resources.maxHitPoints);
			Position pos = new Position(convertCoordinate(resources.x), convertCoordinate(resources.y), resources.z, resources.facing / 1000.0 * Math.PI);
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
	}


	@JsonIgnoreProperties(ignoreUnknown = true)
	private record CombatantsInfoAura(
			long source,
			long ability,
			long stacks,
			String name
	) {
	}

	private Job convertJob(String jobName) {
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
					long id = actor.gameID();
					// TODO: primary player
					boolean isPlayer = actor.type().equals("Player");
					Long ownerId = actor.petOwner();
					if (ownerId == null) {
						ownerId = 0L;
					}
					fflogsProvidedMapping.put(actor.id(), actor.gameID());
					// TODO: Job
					return new RawXivCombatantInfo(id, actor.name(), isPlayer ? convertJob(actor.subType()).getId() : 0, isPlayer ? 1 : 2, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, "TODO", 0, 0, 0, ownerId);
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
					Object aurasRaw = rawEvent.getField("auras");
					if (aurasRaw != null) {
						List<CombatantsInfoAura> auras = mapper.convertValue(aurasRaw, new TypeReference<>() {
						});
						auras.forEach(aura -> {
							XivCombatant auraSource = getCombatant(aura.source, null);
							context.accept(new BuffApplied(convertStatus(aura.ability % 1_000_000), 9999, auraSource, source, aura.stacks));
						});
					}
					if (source instanceof XivPlayerCharacter pc) {
						partyList.add(pc);
						state.setPartyList(partyList.stream().map(pm -> new RawXivPartyInfo(pm.getId(), pm.getName(), 0, pm.getJob().getId(), (int) pm.getLevel(), true)).toList());
					}
				}
				case "damage", "calculateddamage" -> {
					Long amount = rawEvent.getTypedField("amount", Long.class, 0L);
					if (rawEvent.getTypedField("tick", boolean.class, false)) {
						long rawEffectId = rawEvent.abilityId();
						// 500k is "combined"
						long effectId = rawEffectId == 500_000 ? 0 : rawEffectId % 1_000_000;
						context.accept(new TickEvent(target, TickType.DOT, amount, effectId));
					}
					else {
						// TODO: severity
						context.accept(new GenericDamageEvent(source, target, new XivAbility(rawEvent.abilityId()), amount, HitSeverity.NORMAL));
					}
				}
				case "heal", "calculatedheal" -> {
					Long amount = rawEvent.getTypedField("amount", Long.class, 0L);
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
						// TODO: severity
						context.accept(new GenericHealEvent(source, target, new XivAbility(rawEvent.abilityId()), amount, HitSeverity.NORMAL));
					}
					break;
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
				case "applybuff", "applybuffstack", "applydebuff", "applydebuffstack", "removebuffstack", "removedebuffstack", "refreshbuff", "refreshdebuff" -> {
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

	private XivStatusEffect convertStatus(long rawStatus) {
		return new XivStatusEffect(rawStatus % 1_000_000);
	}
}
