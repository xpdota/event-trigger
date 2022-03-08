package gg.xp.xivsupport.events.fflogs;

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
import gg.xp.xivsupport.events.state.RawXivCombatantInfo;
import gg.xp.xivsupport.events.state.XivStateImpl;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivStatusEffect;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FflogsEventProcessor {
	private static final Logger log = LoggerFactory.getLogger(FflogsEventProcessor.class);
	private final AtomicInteger counter = new AtomicInteger();
	private final Map<Long, Long> combatantIdToGameId = new HashMap<>();
	private final XivStateImpl state;

	// TODO: move more stuff to XivState interface
	public FflogsEventProcessor(XivStateImpl state) {
		this.state = state;
	}

	// TODO: multiple instances
	private @Nullable XivCombatant getCombatant(@Nullable Long id) {
		if (id == null) {
			return null;
		}
		else if (id == -1) {
			return XivCombatant.ENVIRONMENT;
		}
		Long realId = combatantIdToGameId.get(id);
		if (realId == null) {
			return new XivCombatant(id, "Combatant " + id);
		}
		XivCombatant knownCbt = state.getCombatants().get(realId);
		if (knownCbt == null) {
			return new XivCombatant(id, "Combatant " + id);
		}
		else {
			return knownCbt;
		}
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
					combatantIdToGameId.put(actor.id(), actor.gameID());
					// TODO: Job
					return new RawXivCombatantInfo(id, actor.name(), isPlayer ? convertJob(actor.subType()).getId() : 0, isPlayer ? 1 : 2, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, "TODO", 0, 0, 0, ownerId);
				})
				.toList();
		state.setPlayer(new XivEntity(0x1234_5678, "Bogus Player"));
		state.setCombatants(combatants);
	}

	@HandleEvents
	public void processFflogsEvent(EventContext context, FflogsRawEvent rawEvent) {
		Object rawType = rawEvent.getField("type");
		if (rawType instanceof String type) {
			switch (type) {
				// Leaving possible NPEs in place because those wouldn't make sense anyway
				case "combatantinfo": {
					// TODO
					break;
				}
				case "damage":
				case "calculateddamage": {
					XivCombatant source = getCombatant(rawEvent.sourceID());
					XivCombatant target = getCombatant(rawEvent.targetID());
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
					break;
				}
				case "heal":
				case "calculatedheal": {
					XivCombatant source = getCombatant(rawEvent.sourceID());
					XivCombatant target = getCombatant(rawEvent.targetID());
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
				case "begincast": {
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
							getCombatant(rawEvent.sourceID()),
							getCombatant(rawEvent.targetID()),
							duration));
				}
				case "cast": {
					context.accept(new AbilityUsedEvent(
							new XivAbility(rawEvent.abilityId()),
							getCombatant(rawEvent.sourceID()),
							getCombatant(rawEvent.targetID()),
							Collections.emptyList(),
							counter.getAndIncrement(),
							0,
							1));
					break;
				}
				case "applybuff":
				case "applybuffstack":
				case "applydebuff":
				case "applydebuffstack":
				case "removebuffstack":
				case "removedebuffstack":
				case "refreshbuff": {
					// TODO
					int stacks = rawEvent.getTypedField("stacks", int.class, 0);
					double duration;
					Double durationRaw = rawEvent.getTypedField("duration", Double.class);
					if (durationRaw == null) {
						duration = 9999;
					}
					else {
						duration = durationRaw / 1000.0;
					}
					context.accept(new BuffApplied(
							new XivStatusEffect(rawEvent.abilityId() % 1_000_000),
							duration,
							getCombatant(rawEvent.getTypedField("sourceID", Long.class)),
							getCombatant(rawEvent.getTypedField("targetID", Long.class)),
							stacks
					));
					break;
				}
				case "removebuff":
				case "removedebuff": {
					context.accept(new BuffRemoved(
							new XivStatusEffect(rawEvent.abilityId() % 1_000_000),
							0,
							getCombatant(rawEvent.getTypedField("sourceID", Long.class)),
							getCombatant(rawEvent.getTypedField("targetID", Long.class)),
							0
					));
					break;

				}

				default: {
					context.accept(new FflogsUnsupportedEvent(rawEvent));
				}


			}
		}
		else {
			log.error("Encountered fflogs event with missing or malformed event type: {}", rawEvent.getFields());
		}

	}
}
