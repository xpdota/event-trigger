package gg.xp.xivsupport.timelines.cbevents;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.AbilityCastCancel;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlExtraEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlSelfExtraEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.ChatLineEvent;
import gg.xp.xivsupport.events.actlines.events.EntityKilledEvent;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.MapEffectEvent;
import gg.xp.xivsupport.events.actlines.events.RawAddCombatantEvent;
import gg.xp.xivsupport.events.actlines.events.RawRemoveCombatantEvent;
import gg.xp.xivsupport.events.actlines.events.SystemLogMessageEvent;
import gg.xp.xivsupport.events.actlines.events.TargetabilityUpdate;
import gg.xp.xivsupport.events.misc.BattleTalkEvent;
import gg.xp.xivsupport.events.misc.NpcYellEvent;
import gg.xp.xivsupport.events.state.InCombatChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static gg.xp.xivsupport.timelines.cbevents.CbConversions.boolToInt;
import static gg.xp.xivsupport.timelines.cbevents.CbConversions.id;
import static gg.xp.xivsupport.timelines.cbevents.CbConversions.intConv;
import static gg.xp.xivsupport.timelines.cbevents.CbConversions.named;
import static gg.xp.xivsupport.timelines.cbevents.CbConversions.strConv;

public enum CbEventType {
	// TODO: when it comes to editing, try to do input validation

	// Each enum constant is the name of the Cactbot type in netlog_defs.ts
	//
	GameLog(ChatLineEvent.class, List.of(
			new CbfMap<>("code", "event.code", intConv(ChatLineEvent::getCode, 16)),
			new CbfMap<>("name", "event.name", strConv(ChatLineEvent::getName)),
			new CbfMap<>("line", "event.line", strConv(ChatLineEvent::getLine)),
			// TODO - extra conditions
			new CbfMap<>("message", "event.line (msg)", strConv(ChatLineEvent::getLine)),
			new CbfMap<>("echo", "event.line (echo)", strConv(ChatLineEvent::getLine)),
			new CbfMap<>("dialog", "event.line (dialog)", strConv(ChatLineEvent::getLine))
	)),
	StartsUsing(AbilityCastStart.class, List.of(
			new CbfMap<>("sourceId", "event.source.id", id(AbilityCastStart::getSource)),
			new CbfMap<>("source", "event.source.name", named(AbilityCastStart::getSource)),
			new CbfMap<>("targetId", "event.target.id", id(AbilityCastStart::getTarget)),
			new CbfMap<>("target", "event.target.name", named(AbilityCastStart::getTarget)),
			new CbfMap<>("id", "event.ability.id", id(AbilityCastStart::getAbility)),
			new CbfMap<>("ability", "event.ability.name", named(AbilityCastStart::getAbility))
	)),
	Ability(AbilityUsedEvent.class, List.of(
			new CbfMap<>("sourceId", "event.source.id", id(AbilityUsedEvent::getSource)),
			new CbfMap<>("source", "event.source.name", named(AbilityUsedEvent::getSource)),
			new CbfMap<>("targetId", "event.target.id", id(AbilityUsedEvent::getTarget)),
			new CbfMap<>("target", "event.target.name", named(AbilityUsedEvent::getTarget)),
			new CbfMap<>("id", "event.ability.id", id(AbilityUsedEvent::getAbility)),
			new CbfMap<>("ability", "event.ability.name", named(AbilityUsedEvent::getAbility))
	)),
	InCombat(InCombatChangeEvent.class, List.of(
			// TODO: kind of fake
			new CbfMap<>("inACTCombat", "event.inCombat", boolToInt(InCombatChangeEvent::isInCombat)),
			new CbfMap<>("inGameCombat", "event.inCombat", boolToInt(InCombatChangeEvent::isInCombat))
	)),
	SystemLogMessage(SystemLogMessageEvent.class, List.of(
			new CbfMap<>("instance", "event.instance", intConv(SystemLogMessageEvent::getUnknown, 16)),
			new CbfMap<>("id", "event.id", intConv(SystemLogMessageEvent::getId, 16)),
			new CbfMap<>("param0", "event.param0", intConv(SystemLogMessageEvent::getParam0, 16)),
			new CbfMap<>("param1", "event.param1", intConv(SystemLogMessageEvent::getParam1, 16)),
			new CbfMap<>("param2", "event.param2", intConv(SystemLogMessageEvent::getParam2, 16))
	)),
	// TODO: finally do more work on XivStateImpl to have it emit added/removed events
	// In the meantime, the raw ACT versions should work fine.
	AddedCombatant(RawAddCombatantEvent.class, List.of(
			new CbfMap<>("name", "event.entity.name", named(RawAddCombatantEvent::getEntity))
	)),
	RemovedCombatant(RawRemoveCombatantEvent.class, List.of(
			new CbfMap<>("name", "event.entity.name", named(RawRemoveCombatantEvent::getEntity))
	)),
	NameToggle(TargetabilityUpdate.class, List.of(
			new CbfMap<>("id", "event.source.id", id(TargetabilityUpdate::getSource)),
			new CbfMap<>("name", "event.source.name", named(TargetabilityUpdate::getSource)),
			new CbfMap<>("targetId", "event.target.id", id(TargetabilityUpdate::getSource)),
			new CbfMap<>("targetName", "event.target.name", named(TargetabilityUpdate::getSource)),
			new CbfMap<>("toggle", "event.targetable", boolToInt(TargetabilityUpdate::isTargetable))
	)),
	ActorControl(ActorControlEvent.class, List.of(
			new CbfMap<>("instance", "event.instance", intConv(ActorControlEvent::getInstance, 16)),
			new CbfMap<>("command", "event.command", intConv(ActorControlEvent::getCommand, 16)),
			new CbfMap<>("data0", "event.data0", intConv(ActorControlEvent::getData0, 16)),
			new CbfMap<>("data1", "event.data1", intConv(ActorControlEvent::getData1, 16)),
			new CbfMap<>("data2", "event.data2", intConv(ActorControlEvent::getData2, 16)),
			new CbfMap<>("data3", "event.data3", intConv(ActorControlEvent::getData3, 16))
	)),
	GainsEffect(BuffApplied.class, List.of(
			new CbfMap<>("sourceId", "event.source.id", id(BuffApplied::getSource)),
			new CbfMap<>("source", "event.source.name", named(BuffApplied::getSource)),
			new CbfMap<>("targetId", "event.target.id", id(BuffApplied::getTarget)),
			new CbfMap<>("target", "event.target.name", named(BuffApplied::getTarget)),
			new CbfMap<>("effectId", "event.buff.id", id(BuffApplied::getBuff)),
			new CbfMap<>("effect", "event.buff.name", named(BuffApplied::getBuff)),
			new CbfMap<>("count", "event.rawStacks", intConv(BuffApplied::getRawStacks, 16))
	)),
	MapEffect(MapEffectEvent.class, List.of(
			new CbfMap<>("instance", "event.instanceContentId", intConv(MapEffectEvent::getInstanceContentId, 16)),
			new CbfMap<>("flags", "event.flags", intConv(MapEffectEvent::getFlags, 16)),
			new CbfMap<>("location", "event.location", intConv(MapEffectEvent::getLocation, 16)),
			new CbfMap<>("data0", "event.data1", intConv(MapEffectEvent::getUnknown1, 16)),
			new CbfMap<>("data1", "event.data2", intConv(MapEffectEvent::getUnknown2, 16))
	)),
	NetworkCancelAbility(AbilityCastCancel.class, List.of(
			new CbfMap<>("sourceId", "event.source.id", id(AbilityCastCancel::getSource)),
			new CbfMap<>("source", "event.source.name", named(AbilityCastCancel::getSource)),
			new CbfMap<>("id", "event.ability.id", id(AbilityCastCancel::getAbility)),
			new CbfMap<>("name", "event.ability.name", named(AbilityCastCancel::getAbility)),
			new CbfMap<>("reason", "event.reason", strConv(AbilityCastCancel::getReason))
	)),
	HeadMarker(HeadMarkerEvent.class, List.of(
			new CbfMap<>("targetId", "event.target.id", id(HeadMarkerEvent::getTarget)),
			new CbfMap<>("target", "event.target.name", named(HeadMarkerEvent::getTarget)),
			new CbfMap<>("id", "event.markerId", intConv(HeadMarkerEvent::getMarkerId, 16))
	)),
	WasDefeated(EntityKilledEvent.class, List.of(
			new CbfMap<>("sourceId", "event.source.id", id(EntityKilledEvent::getSource)),
			new CbfMap<>("source", "event.source.name", named(EntityKilledEvent::getSource)),
			new CbfMap<>("targetId", "event.target.id", id(EntityKilledEvent::getTarget)),
			new CbfMap<>("target", "event.target.name", named(EntityKilledEvent::getTarget))
	)),
	NpcYell(NpcYellEvent.class, List.of(
			new CbfMap<>("sourceId", "event.source.id", id(NpcYellEvent::getSource)),
			new CbfMap<>("npcNameId", "event.source.bNpcNameId", intConv((NpcYellEvent event) -> event.getSource().getbNpcNameId(), 16)),
			new CbfMap<>("npcYellId", "event.yell.id()", intConv(event -> (long) event.getYell().id(), 16))
	)),
	BattleTalk2(BattleTalkEvent.class, List.of(
			new CbfMap<>("sourceId", "event.source.id", id(BattleTalkEvent::getSource)),
			new CbfMap<>("npcNameId", "event.source.bNpcNameId", intConv(event -> event.getSource().getbNpcNameId(), 16)),
			new CbfMap<>("instanceContentTextId", "event.instanceContentTextId", intConv(BattleTalkEvent::getInstanceContentTextId, 16))
	)),
	ActorControlExtra(ActorControlExtraEvent.class, List.of(
			new CbfMap<>("id", "event.target.id", id(ActorControlExtraEvent::getTarget)),
			new CbfMap<>("category", "event.category", intConv(e -> (long) e.getCategory(), 16)),
			new CbfMap<>("param1", "event.data0", intConv(ActorControlExtraEvent::getData0, 16)),
			new CbfMap<>("param2", "event.data1", intConv(ActorControlExtraEvent::getData1, 16)),
			new CbfMap<>("param3", "event.data2", intConv(ActorControlExtraEvent::getData2, 16)),
			new CbfMap<>("param4", "event.data3", intConv(ActorControlExtraEvent::getData3, 16))
	)),
	ActorControlSelfExtra(ActorControlSelfExtraEvent.class, List.of(
			new CbfMap<>("id", "event.instance", id(ActorControlSelfExtraEvent::getTarget)),
			new CbfMap<>("category", "event.command", intConv(e -> (long) e.getCategory(), 16)),
			new CbfMap<>("param1", "event.data0", intConv(ActorControlSelfExtraEvent::getData0, 16)),
			new CbfMap<>("param2", "event.data1", intConv(ActorControlSelfExtraEvent::getData1, 16)),
			new CbfMap<>("param3", "event.data2", intConv(ActorControlSelfExtraEvent::getData2, 16)),
			new CbfMap<>("param4", "event.data3", intConv(ActorControlSelfExtraEvent::getData3, 16)),
			new CbfMap<>("param5", "event.data4", intConv(ActorControlSelfExtraEvent::getData4, 16)),
			new CbfMap<>("param6", "event.data5", intConv(ActorControlSelfExtraEvent::getData5, 16))
	)),


	// TODO: the rest of the events
	;


	private final Holder<?> data;

	<X extends Event> CbEventType(Class<X> eventType, List<CbfMap<? super X>> fieldMappings) {
		this.data = new Holder<>(eventType, fieldMappings);
	}

	public Class<? extends Event> eventType() {
		return data.eventType;
	}

	/**
	 * Make a combined predicate based on the map of values.
	 *
	 * @param values The values
	 * @return The combined predicate
	 */
	public Predicate<Event> make(Map<String, List<String>> values) {
		return this.data.make(values);
	}

	public String displayName() {
		return eventType().getSimpleName();
	}


	private static class Holder<X extends Event> implements CbEventDesc<X> {
		private static final Logger log = LoggerFactory.getLogger(CbEventType.class);
		private final Class<X> eventType;
		private final Map<String, CbConversion<? super X>> condMap;
		private final List<CbfMap<? super X>> fieldMap;

		Holder(Class<X> eventType, List<CbfMap<? super X>> fieldMap) {
			this.eventType = eventType;
			this.fieldMap = fieldMap;
			this.condMap = fieldMap.stream().collect(Collectors.toMap(CbfMap::cbField, CbfMap::conversion));
		}


		public Predicate<Event> make(Map<String, List<String>> values) {
			Predicate<X> combined = eventType::isInstance;
			for (var entry : values.entrySet()) {
				CbConversion<? super X> convToCondition = this.condMap.get(entry.getKey());
				if (convToCondition == null) {
					throw new IllegalArgumentException("Unknown condition: " + entry);
				}
				List<String> valueOptions = entry.getValue();
				Predicate<X> converted = (ignored) -> false;
				for (String value : valueOptions) {
					converted = converted.or(convToCondition.convert(value));
				}
				combined = combined.and(converted);
			}
			//noinspection unchecked - the first check is the type check
			return (Predicate<Event>) combined;
		}

		@Override
		public Class<X> getEventType() {
			return eventType;
		}

		@Override
		public List<CbfMap<? super X>> getFieldMappings() {
			return Collections.unmodifiableList(fieldMap);
		}
	}

	public List<CbfMap<?>> getFieldMappings() {
		return (List<CbfMap<?>>) this.data.getFieldMappings();
	}
}
