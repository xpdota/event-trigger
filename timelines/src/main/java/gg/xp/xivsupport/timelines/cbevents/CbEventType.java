package gg.xp.xivsupport.timelines.cbevents;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ChatLineEvent;
import gg.xp.xivsupport.events.actlines.events.RawAddCombatantEvent;
import gg.xp.xivsupport.events.actlines.events.SystemLogMessageEvent;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
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
	/*
		Further design notes:

		Instead of building a map directly, just make a new class to hold all the relevant data:
		1. Key
		2. Conversion
		3. Display
		4. Input validation

	 */

	GameLog(ChatLineEvent.class, List.of(
			new CbfMap<>("code", "event.code", intConv(ChatLineEvent::getCode, 16)),
			new CbfMap<>("line", "event.line", strConv(ChatLineEvent::getLine)),
			// TODO - extra conditions
			new CbfMap<>("message", "event.line (msg)", strConv(ChatLineEvent::getLine)),
			new CbfMap<>("echo", "event.line (echo)", strConv(ChatLineEvent::getLine)),
			new CbfMap<>("dialog", "event.line (dialog)", strConv(ChatLineEvent::getLine))
	)),
	StartsUsing(AbilityUsedEvent.class, List.of(
			new CbfMap<>("sourceId", "event.source.id", id(AbilityUsedEvent::getSource)),
			new CbfMap<>("source", "event.source.name", named(AbilityUsedEvent::getSource)),
			new CbfMap<>("targetId", "event.target.id", id(AbilityUsedEvent::getTarget)),
			new CbfMap<>("target", "event.target.name", named(AbilityUsedEvent::getTarget)),
			new CbfMap<>("id", "event.ability.id", id(AbilityUsedEvent::getAbility)),
			new CbfMap<>("ability", "event.ability.name", named(AbilityUsedEvent::getAbility))
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
	))


	// TODO: the rest of the events
	;


	private final Holder<?> data;

	<X extends Event> CbEventType(Class<X> eventType, List<CbfMap<? super X>> fieldMappings) {
		this.data = new Holder<X>(eventType, fieldMappings);
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
	public Predicate<Event> make(Map<String, String> values) {
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


		public Predicate<Event> make(Map<String, String> values) {
			Predicate<X> combined = eventType::isInstance;
			for (var entry : values.entrySet()) {
				CbConversion<? super X> convToCondition = this.condMap.get(entry.getKey());
				if (convToCondition == null) {
					throw new IllegalArgumentException("Unknown condition: " + entry);
				}
				Predicate<? super X> converted = convToCondition.convert(entry.getValue());
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
}
