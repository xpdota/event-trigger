package gg.xp.xivsupport.events.triggers.easytriggers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityCastCancel;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityResolvedEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.EntityKilledEvent;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourceEntityTypeFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.TargetEntityTypeFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui.GenericFieldEditor;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ConditionDescription;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EventDescription;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ScanMe
public class EasyTriggers {
	private static final Logger log = LoggerFactory.getLogger(EasyTriggers.class);
	private static final String settingKey = "easy-triggers.my-triggers";
	private static final ObjectMapper mapper = new ObjectMapper();

	private final PersistenceProvider pers;

	private List<EasyTrigger<?>> triggers;

	public EasyTriggers(PersistenceProvider pers) {
		this.pers = pers;
		String strVal = pers.get(settingKey, String.class, null);
		if (strVal == null) {
			triggers = new ArrayList<>();
		}
		else {
			try {
				triggers = mapper.readValue(strVal, new TypeReference<>() {
				});
			}
			catch (JsonProcessingException e) {
				log.error("Error loading Easy Triggers", e);
				log.error("Dump of trigger data:\n{}", strVal);
				throw new RuntimeException("There was an error loading Easy Triggers. Check the log.", e);
			}
		}
	}

	private void save() {
		try {
			String triggersSerialized = mapper.writeValueAsString(triggers);
//			log.info("Saving triggers: {}", triggersSerialized);
			pers.save(settingKey, triggersSerialized);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public void commit() {
		save();
	}

	@HandleEvents
	public void runEasyTriggers(EventContext context, Event event) {
		triggers.forEach(trig -> {
			try {
				trig.handleEvent(context, event);
			}
			catch (Throwable t) {
				log.error("Error running easy trigger '{}'", trig.getName(), t);
			}
		});
	}

	public List<EasyTrigger<?>> getTriggers() {
		return Collections.unmodifiableList(triggers);
	}

	public void addTrigger(EasyTrigger<?> trigger) {
		makeListWritable();
		triggers.add(trigger);
		save();
	}

	public void removeTrigger(EasyTrigger<?> trigger) {
		makeListWritable();
		triggers.remove(trigger);
		save();
	}

	private void makeListWritable() {
		if (!(triggers instanceof ArrayList<EasyTrigger<?>>)) {
			triggers = new ArrayList<>(triggers);
		}
	}

	public void setTriggers(List<EasyTrigger<?>> triggers) {
		this.triggers = new ArrayList<>(triggers);
		save();
	}

	private static final List<EventDescription> eventTypes = List.of(
			new EventDescription(AbilityCastStart.class, "An ability has started casting. Corresponds to ACT 20 lines."),
			new EventDescription(AbilityUsedEvent.class, "An ability has snapshotted. Corresponds to ACT 21/22 lines."),
			new EventDescription(AbilityCastCancel.class, "An ability was interrupted while casting. Corresponds to ACT 23 lines."),
			new EventDescription(EntityKilledEvent.class, "Something died. Corresponds to ACT 25 lines."),
			new EventDescription(BuffApplied.class, "A buff or debuff has been applied. Corresponds to ACT 26 lines."),
			new EventDescription(BuffApplied.class, "A buff or debuff has been removed. Corresponds to ACT 30 lines."),
			new EventDescription(AbilityResolvedEvent.class, "An ability has actually applied. Corresponds to ACT 37 lines."),
			new EventDescription(ActorControlEvent.class, "Conveys various state changes, such as wiping or finishing a raid. Corresponds to ACT 33 lines."),
			new EventDescription(ACTLogLineEvent.class, "Any log line, in text form. Use as a last resort.")
	);

	private static final List<ConditionDescription<?, ?>> conditions = List.of(
			new ConditionDescription<>(AbilityIdFilter.class, HasAbility.class, "Ability ID", AbilityIdFilter::new, (cond, trigger) -> new GenericFieldEditor(cond)),
			new ConditionDescription<>(SourceEntityTypeFilter.class, HasSourceEntity.class, "Source Combatant", SourceEntityTypeFilter::new, (cond, trigger) -> new GenericFieldEditor(cond)),
			new ConditionDescription<>(TargetEntityTypeFilter.class, HasTargetEntity.class, "Target Combatant", TargetEntityTypeFilter::new, (cond, trigger) -> new GenericFieldEditor(cond))
	);

	public static List<EventDescription> getEventDescriptions() {
		return eventTypes;
	}

	public static List<ConditionDescription<?, ?>> getConditions() {
		return conditions;
	}

	@SuppressWarnings("unchecked")
	public static <X extends Condition<Y>, Y> ConditionDescription<X, Y> getConditionDescription(Class<X> cond) {
		ConditionDescription<?, ?> conditionDescription = conditions.stream().filter(item -> item.clazz().equals(cond)).findFirst().orElse(null);
		return (ConditionDescription<X, Y>) conditionDescription;
	}
}
