package gg.xp.xivsupport.events.triggers.easytriggers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.audio.SoundFilesManager;
import gg.xp.xivsupport.callouts.audio.gui.SoundFileTab;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityCastCancel;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityResolvedEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.ChatLineEvent;
import gg.xp.xivsupport.events.actlines.events.EntityKilledEvent;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.events.actlines.events.HasEffects;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.actlines.events.HasTargetIndex;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TargetabilityUpdate;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.AutoMarkTargetAction;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.CalloutAction;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.ClearAllMarksAction;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.ConditionalAction;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.DurationBasedCalloutAction;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.GroovyAction;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.SoundAction;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.WaitAction;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.gui.ConditionalActionEditor;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.gui.GroovyActionEditor;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.gui.SoundActionEditor;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityNameFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.ChatLineRegexFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.ChatLineTypeFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.DurationFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.EntityType;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.GroovyEventFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.HeadmarkerAbsoluteIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.HeadmarkerRelativeIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.HitSeverityFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.LogLineNumberFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.LogLineRegexFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.OrFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.PlayerHasStatusFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.RefireFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourceEntityNpcIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourceEntityTypeFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourceHasStatusFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourcePartyMemberFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.StatusIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.StatusStacksFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.TargetCountFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.TargetEntityNpcIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.TargetEntityTypeFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.TargetHasStatusFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.TargetIndexFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.TargetPartyMemberFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.TargetabilityChangeFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.TetherEntityTypeFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.TetherIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.ZoneIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui.CompoundConditionEditor;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui.GenericFieldEditor;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui.GroovyFilterEditor;
import gg.xp.xivsupport.events.triggers.easytriggers.creators.EasyTriggerCreationQuestions;
import gg.xp.xivsupport.events.triggers.easytriggers.gui.CalloutActionPanel;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ActionDescription;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ConditionDescription;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerMigrationHelper;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EventDescription;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EventDescriptionImpl;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasMutableActions;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasMutableConditions;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.gui.nav.GlobalUiRegistry;
import gg.xp.xivsupport.gui.tables.filters.ValidationError;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.CustomJsonListSetting;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ScanMe
public final class EasyTriggers {
	private static final Logger log = LoggerFactory.getLogger(EasyTriggers.class);
	private static final String settingKey = "easy-triggers.my-triggers";
	private static final String failedTriggersSettingKey = "easy-triggers.failed-triggers";
	private final ObjectMapper mapper = new ObjectMapper();

	private final PersistenceProvider pers;
	private final PicoContainer pico;
	private final XivState state;
	private final CustomJsonListSetting<EasyTrigger<?>> setting;

	private ArrayList<EasyTrigger<?>> triggers;

	public EasyTriggers(PicoContainer pico, PersistenceProvider pers, XivState state) {
		this.pers = pers;
		this.pico = pico;
		this.state = state;
		mapper.setInjectableValues(new InjectableValues() {
			@Override
			public Object findInjectableValue(Object o, DeserializationContext deserializationContext, BeanProperty beanProperty, Object o1) {
				return inject(beanProperty.getType().getRawClass());
			}
		});
		this.setting = CustomJsonListSetting.<EasyTrigger<?>>builder(pers, new TypeReference<>() {
				}, settingKey, failedTriggersSettingKey)
				.withMapper(mapper)
				.postConstruct(this::doLegacyMigration)
				.build();
		setting.tryRecoverFailures();
		this.triggers = new ArrayList<>(setting.getItems());
		recalc();
	}

	@SuppressWarnings("unchecked")
	private <X> void doLegacyMigration(JsonNode node, EasyTrigger<X> trigger) {
		EasyTriggerMigrationHelper migration = mapper.convertValue(node, EasyTriggerMigrationHelper.class);
		// legacy migration
		if (migration.tts != null || migration.text != null) {
			if (migration.useDuration && HasDuration.class.isAssignableFrom(trigger.getEventType())) {
				DurationBasedCalloutAction action = new DurationBasedCalloutAction();
				action.setText(migration.text);
				action.setTts(migration.tts);
				action.setColorRaw(migration.colorRaw);
				action.setUseIcon(migration.useIcon);
				action.setHangTime(migration.hangTime);
				trigger.addAction((Action<? super X>) action);
			}
			else {
				CalloutAction action = new CalloutAction();
				action.setText(migration.text);
				action.setTts(migration.tts);
				action.setColorRaw(migration.colorRaw);
				action.setUseIcon(migration.useIcon);
				action.setHangTime(migration.hangTime);
				trigger.addAction((Action<? super X>) action);
			}
		}
	}


	private <X> X inject(Class<X> clazz) {
		return pico.getComponent(clazz);
	}

	public String exportToString(List<EasyTrigger<?>> toExport) {
		try {
			return mapper.writeValueAsString(toExport);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Error exporting trigger", e);
		}
	}

	public List<EasyTrigger<?>> importFromString(String string) {
		try {
			List<JsonNode> nodes = mapper.readValue(string, new TypeReference<>() {
			});
			List<EasyTrigger<?>> out = new ArrayList<>(nodes.size());
			for (JsonNode node : nodes) {
				EasyTrigger<?> trigger = mapper.convertValue(node, EasyTrigger.class);
				doLegacyMigration(node, trigger);
				out.add(trigger);
			}
			return out;
		}
		catch (JsonProcessingException e) {
			throw new ValidationError("Error importing trigger: " + e.getMessage(), e);
		}
	}

	private void save() {
		recalc();
		setting.setItems(triggers);
	}

	public void commit() {
		save();
	}

	private void recalc() {
		triggers.forEach(EasyTrigger::recalc);
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
		triggers.add(trigger);
		save();
	}

	public void removeTrigger(EasyTrigger<?> trigger) {
		triggers.remove(trigger);
		save();
	}

	// Be sure to add new types to EasyTriggersTest
	// TODO: might be nice to wire some of the "single value replacement" logic from ModifiableCallout into here, to skip the need for .name on everything
	private final List<EventDescription<?>> eventTypes = new ArrayList<>(List.of(
			new EventDescriptionImpl<>(AbilityCastStart.class,
					"An ability has started casting. Corresponds to ACT 20 lines.",
					"{event.ability}",
					"{event.ability} ({event.estimatedRemainingDuration})",
					List.of(AbilityIdFilter::new)) {
				@Override
				protected Action<? super AbilityCastStart> defaultCallout(String text, String tts) {
					DurationBasedCalloutAction call = new DurationBasedCalloutAction();
					call.setTts(tts);
					call.setText(text);
					return call;
				}

				@Override
				protected Action<? super AbilityCastStart> defaultCallout(String customText) {
					String text;
					String tts;
					if (customText == null || customText.isBlank()) {
						text = defaultText;
						tts = defaultTts;
					}
					else {
						text = customText + " ({event.estimatedRemainingDuration})";
						tts = customText;
					}
					return defaultCallout(text, tts);
				}
			},
			new EventDescriptionImpl<>(AbilityUsedEvent.class,
					"An ability has snapshotted. Corresponds to ACT 21/22 lines.",
					"{event.ability}",
					List.of(AbilityIdFilter::new)),
			new EventDescriptionImpl<>(AbilityCastCancel.class,
					"An ability was interrupted while casting. Corresponds to ACT 23 lines.",
					"{event.ability} interrupted",
					List.of(AbilityIdFilter::new)),
			new EventDescriptionImpl<>(EntityKilledEvent.class,
					"Something died. Corresponds to ACT 25 lines.",
					"{event.target} died",
					List.of(TargetEntityTypeFilter::new)),
			new EventDescriptionImpl<>(BuffApplied.class,
					"A buff or debuff has been applied. Corresponds to ACT 26 lines.",
					"{event.buff} on {event.target}",
					List.of(StatusIdFilter::new)) {
				@Override
				protected Action<? super BuffApplied> defaultCallout(String text, String tts) {
					DurationBasedCalloutAction call = new DurationBasedCalloutAction();
					call.setTts(tts);
					call.setText(text);
					call.setPlusDuration(false);
					return call;
				}
			},
			new EventDescriptionImpl<>(BuffRemoved.class,
					"A buff or debuff has been removed. Corresponds to ACT 30 lines.",
					"{event.buff} lost from {event.target}",
					List.of(StatusIdFilter::new)),
			new EventDescriptionImpl<>(AbilityResolvedEvent.class,
					"An ability has actually applied. Corresponds to ACT 37 lines.",
					"{event.ability} resolved",
					List.of(AbilityIdFilter::new)),
			new EventDescriptionImpl<>(TetherEvent.class,
					"A tether between two combatants. Corresponds to ACT 35 lines.",
					"tether",
					List.of(TetherEntityTypeFilter::new)),
			new EventDescriptionImpl<>(ActorControlEvent.class,
					"Conveys various state changes, such as wiping or finishing a raid. Corresponds to ACT 33 lines.",
					"Actor control {event.command}",
					List.of()),
			new EventDescriptionImpl<>(HeadMarkerEvent.class,
					"Represents a headmarker. Corresponds to ACT 27 lines.",
					"Headmarker",
					List.of()),
			new EventDescriptionImpl<>(TargetabilityUpdate.class,
					"Represents an enemy become targetable or untargetable. Corresponds to ACT 34 lines.",
					"{event.target} {event.targetable ? 'Targetable' : 'Untargetable'}",
					List.of(TargetabilityChangeFilter::new)),
			new EventDescriptionImpl<>(ACTLogLineEvent.class,
					"Any log line, in text form. Use as a last resort.",
					"Log Line {event.rawFields[0]}",
					List.of(LogLineRegexFilter::new)),
			new EventDescriptionImpl<>(ChatLineEvent.class,
					"In-game chat lines. Use as a last resort, e.g. Nael quotes.",
					"{event.name} says {event.line}",
					"Chat Line {event.name}: {event.line}",
					List.of(ChatLineRegexFilter::new))
	));


	// TODO: what should the actual type be
	private Component generic(Object object, Object trigger) {
		return new GenericFieldEditor(object, pico);
	}

	// XXX - DO NOT CHANGE NAMES OF THESE CLASSES OR PACKAGE PATH - FQCN IS PART OF DESERIALIZATION!!!
	private final List<ConditionDescription<?, ?>> conditions = new ArrayList<>(List.of(
			new ConditionDescription<>(RefireFilter.class, Event.class, "Refire Suppression", RefireFilter::new, this::generic),
			new ConditionDescription<>(AbilityIdFilter.class, HasAbility.class, "Ability ID", AbilityIdFilter::new, this::generic),
			new ConditionDescription<>(AbilityNameFilter.class, HasAbility.class, "Ability Name", AbilityNameFilter::new, this::generic),
			new ConditionDescription<>(StatusIdFilter.class, HasStatusEffect.class, "Status Effect ID", StatusIdFilter::new, this::generic),
			new ConditionDescription<>(StatusStacksFilter.class, HasStatusEffect.class, "Status Effect Stack Count", StatusStacksFilter::new, this::generic),
			new ConditionDescription<>(SourceEntityTypeFilter.class, HasSourceEntity.class, "Source Combatant", SourceEntityTypeFilter::new, this::generic),
			new ConditionDescription<>(TargetEntityTypeFilter.class, HasTargetEntity.class, "Target Combatant", TargetEntityTypeFilter::new, this::generic),
			new ConditionDescription<>(SourceEntityNpcIdFilter.class, HasSourceEntity.class, "Source Combatant NPC ID", SourceEntityNpcIdFilter::new, this::generic),
			new ConditionDescription<>(TargetEntityNpcIdFilter.class, HasTargetEntity.class, "Target Combatant NPC ID", TargetEntityNpcIdFilter::new, this::generic),
			new ConditionDescription<>(SourcePartyMemberFilter.class, HasSourceEntity.class, "Source is (not) in Party", () -> new SourcePartyMemberFilter(inject(XivState.class)), this::generic),
			new ConditionDescription<>(TargetPartyMemberFilter.class, HasTargetEntity.class, "Target is (not) in Party", () -> new TargetPartyMemberFilter(inject(XivState.class)), this::generic),
			new ConditionDescription<>(TetherEntityTypeFilter.class, TetherEvent.class, "Target types for Tether Events", TetherEntityTypeFilter::new, this::generic),
			new ConditionDescription<>(TetherIdFilter.class, TetherEvent.class, "Tether ID", TetherIdFilter::new, this::generic),
			new ConditionDescription<>(HeadmarkerAbsoluteIdFilter.class, HeadMarkerEvent.class, "Headmarker ID (Absolute)", HeadmarkerAbsoluteIdFilter::new, this::generic),
			new ConditionDescription<>(HeadmarkerRelativeIdFilter.class, HeadMarkerEvent.class, "Headmarker ID (Relative)", HeadmarkerRelativeIdFilter::new, this::generic),
			new ConditionDescription<>(PlayerHasStatusFilter.class, Event.class, "Player has a specific status effect", () -> new PlayerHasStatusFilter(inject(XivState.class), inject(StatusEffectRepository.class)), this::generic),
			new ConditionDescription<>(SourceHasStatusFilter.class, HasSourceEntity.class, "Source has a specific status effect", () -> new SourceHasStatusFilter(inject(StatusEffectRepository.class)), this::generic),
//			new ConditionDescription<>(SourceHasStatusFilter.class, HasSourceEntity.class, "Source has a specific status effect", () -> new SourceHasStatusFilter(), this::generic),
			new ConditionDescription<>(TargetHasStatusFilter.class, HasTargetEntity.class, "Target has a specific status effect", () -> new TargetHasStatusFilter(inject(StatusEffectRepository.class)), this::generic),
			new ConditionDescription<>(TargetIndexFilter.class, HasTargetIndex.class, "Target Index", TargetIndexFilter::new, this::generic),
			new ConditionDescription<>(TargetCountFilter.class, HasTargetIndex.class, "Target Count", TargetCountFilter::new, this::generic),
			new ConditionDescription<>(DurationFilter.class, HasDuration.class, "Castbar or Status Duration", DurationFilter::new, this::generic),
			new ConditionDescription<>(LogLineRegexFilter.class, ACTLogLineEvent.class, "Log Line Regular Expression (Regex)", LogLineRegexFilter::new, this::generic),
			new ConditionDescription<>(LogLineNumberFilter.class, ACTLogLineEvent.class, "Log Line Number", LogLineNumberFilter::new, this::generic),
			new ConditionDescription<>(ChatLineRegexFilter.class, ChatLineEvent.class, "Chat Line Regular Expression (Regex)", ChatLineRegexFilter::new, this::generic),
			new ConditionDescription<>(ChatLineTypeFilter.class, ChatLineEvent.class, "Chat Line Number", ChatLineTypeFilter::new, this::generic),
			new ConditionDescription<>(HitSeverityFilter.class, HasEffects.class, "Hit Severity (Crit/Direct Hit)", HitSeverityFilter::new, this::generic),
			new ConditionDescription<>(TargetabilityChangeFilter.class, TargetabilityUpdate.class, "Combatant becomes (un)targetable", TargetabilityChangeFilter::new, this::generic),
			new ConditionDescription<>(GroovyEventFilter.class, Event.class, "Make your own filter code with Groovy", () -> new GroovyEventFilter(inject(GroovyManager.class)), (a, b) -> new GroovyFilterEditor<>(a, b)),
			new ConditionDescription<>(ZoneIdFilter.class, Object.class, "Restrict the Zone ID in which this trigger may run", () -> new ZoneIdFilter(inject(XivState.class)), this::generic)
	));

	private final List<ActionDescription<?, ?>> actions = new ArrayList<>(List.of(
			new ActionDescription<>(CalloutAction.class, Event.class, "Basic TTS/Text Callout", CalloutAction::new, (callout, trigger) -> new CalloutActionPanel(callout)),
			new ActionDescription<>(DurationBasedCalloutAction.class, HasDuration.class, "Duration-Based TTS/Text Callout", DurationBasedCalloutAction::new, (callout, trigger) -> new CalloutActionPanel(callout)),
			new ActionDescription<>(AutoMarkTargetAction.class, HasTargetEntity.class, "Mark The Target", () -> new AutoMarkTargetAction(inject(GlobalUiRegistry.class)), this::generic),
			new ActionDescription<>(ClearAllMarksAction.class, Event.class, "Clear All Marks", () -> new ClearAllMarksAction(inject(GlobalUiRegistry.class)), this::generic),
			new ActionDescription<>(WaitAction.class, BaseEvent.class, "Wait a fixed time", WaitAction::new, this::generic),
			new ActionDescription<>(GroovyAction.class, Event.class, "Custom script action", () -> new GroovyAction(inject(GroovyManager.class)), (action, trigger) -> new GroovyActionEditor<>(action, trigger)),
//			(ActionDescription<ConditionalAction<BaseEvent>, BaseEvent>) new ActionDescription<>(ConditionalAction.class, BaseEvent.class, "If/Else Conditional Action", ConditionalAction::new, (action, trigger) -> new ConditionalActionEditor(this, action)),
			new ActionDescription<>(SoundAction.class, Event.class, "Play Sound", SoundAction::new, (action, trigger) -> new SoundActionEditor(inject(SoundFilesManager.class), inject(SoundFileTab.class), action))
	));

	{
		registerConditionType(new ConditionDescription<>(OrFilter.class, Object.class, "Logical OR or multiple conditions", OrFilter::new, (action, trigger) -> new CompoundConditionEditor(this, action)));
		registerActionType(new ActionDescription<>(ConditionalAction.class, BaseEvent.class, "If/Else Conditional Action", ConditionalAction::new, (action, trigger) -> new ConditionalActionEditor(this, action)));
	}

	public List<EventDescription<?>> getEventDescriptions() {
		return Collections.unmodifiableList(eventTypes);
	}

	@SuppressWarnings("unchecked")
	public @Nullable <X> EventDescription<X> getEventDescription(Class<X> event) {
		return (EventDescription<X>) eventTypes.stream().filter(desc -> desc.type().equals(event)).findFirst().orElse(null);
	}

	public List<ConditionDescription<?, ?>> getConditions() {
		return Collections.unmodifiableList(conditions);
	}

	public <X> List<ConditionDescription<?, ?>> getConditionsApplicableTo(HasMutableConditions<X> trigger) {
		return conditions.stream().filter(cdesc -> cdesc.appliesTo(trigger.classForConditions())).toList();
	}

	public List<ActionDescription<?, ?>> getActions() {
		return Collections.unmodifiableList(actions);
	}

	public <X> List<ActionDescription<?, ?>> getActionsApplicableTo(HasMutableActions<X> trigger) {
		return actions.stream().filter(adesc -> adesc.isEnabled() && adesc.appliesTo(trigger.classForActions())).toList();
	}

	public void registerEventType(EventDescription<?> eventDescription) {
		eventTypes.add(eventDescription);
	}

	public void registerActionType(ActionDescription<?, ?> actionDescription) {
		actions.add(actionDescription);
	}

	public void registerConditionType(ConditionDescription<?, ?> conditionDescription) {
		conditions.add(conditionDescription);
	}


	@SuppressWarnings("unchecked")
	public <X extends Condition<Y>, Y> ConditionDescription<X, Y> getConditionDescription(Class<X> cond) {
		ConditionDescription<?, ?> conditionDescription = conditions.stream().filter(item -> item.clazz().equals(cond)).findFirst().orElse(null);
		return (ConditionDescription<X, Y>) conditionDescription;
	}

	@SuppressWarnings("unchecked")
	public <X extends Action<Y>, Y> ActionDescription<X, Y> getActionDescription(Class<X> action) {
		ActionDescription<?, ?> conditionDescription = actions.stream().filter(item -> item.clazz().equals(action)).findFirst().orElse(null);
		return (ActionDescription<X, Y>) conditionDescription;
	}

	public @Nullable EasyTrigger<?> makeTriggerFromEvent(Event event, EasyTriggerCreationQuestions questions) {
		if (event instanceof AbilityCastStart acs) {
			EasyTrigger<AbilityCastStart> trigger = getEventDescription(AbilityCastStart.class).newEmptyInst(questions.askCalloutText());
			trigger.setName(acs.getAbility().getName() + " casting");
			makeSourceConditions(acs).forEach(trigger::addCondition);
			makeTargetConditions(acs).forEach(trigger::addCondition);
			makeAbilityConditions(acs).forEach(trigger::addCondition);
			return trigger;
		}
		else if (event instanceof AbilityUsedEvent abu) {
			EasyTrigger<AbilityUsedEvent> trigger = getEventDescription(AbilityUsedEvent.class).newEmptyInst(questions.askCalloutText());
			trigger.setName(abu.getAbility().getName() + " used");
			makeSourceConditions(abu).forEach(trigger::addCondition);
			makeTargetConditions(abu).forEach(trigger::addCondition);
			makeAbilityConditions(abu).forEach(trigger::addCondition);
			makeTargetIndexConditions(abu).forEach(trigger::addCondition);
			return trigger;
		}
		else if (event instanceof AbilityCastCancel acc) {
			EasyTrigger<AbilityCastCancel> trigger = getEventDescription(AbilityCastCancel.class).newEmptyInst(questions.askCalloutText());
			trigger.setName(acc.getAbility().getName() + " cancelled");
			makeSourceConditions(acc).forEach(trigger::addCondition);
			makeAbilityConditions(acc).forEach(trigger::addCondition);
			return trigger;
		}
		else if (event instanceof AbilityResolvedEvent are) {
			EasyTrigger<AbilityResolvedEvent> trigger = getEventDescription(AbilityResolvedEvent.class).newEmptyInst(questions.askCalloutText());
			trigger.setName(are.getAbility().getName() + " resolved");
			makeSourceConditions(are).forEach(trigger::addCondition);
			makeTargetConditions(are).forEach(trigger::addCondition);
			makeAbilityConditions(are).forEach(trigger::addCondition);
			makeTargetIndexConditions(are).forEach(trigger::addCondition);
			return trigger;
		}
		else if (event instanceof BuffApplied ba) {
			EasyTrigger<BuffApplied> trigger = getEventDescription(BuffApplied.class).newEmptyInst(questions.askCalloutText());
			trigger.setName(ba.getBuff().getName() + " applied");
			makeSourceConditions(ba).forEach(trigger::addCondition);
			makeTargetConditions(ba).forEach(trigger::addCondition);
			makeStatusConditions(ba).forEach(trigger::addCondition);
			return trigger;
		}
		else if (event instanceof BuffRemoved br) {
			EasyTrigger<BuffRemoved> trigger = getEventDescription(BuffRemoved.class).newEmptyInst(questions.askCalloutText());
			trigger.setName(br.getBuff().getName() + " removed");
			makeSourceConditions(br).forEach(trigger::addCondition);
			makeTargetConditions(br).forEach(trigger::addCondition);
			makeStatusConditions(br).forEach(trigger::addCondition);
			return trigger;
		}
		else if (event instanceof HeadMarkerEvent hme) {
			EasyTrigger<HeadMarkerEvent> trigger = getEventDescription(HeadMarkerEvent.class).newEmptyInst(questions.askCalloutText());
			boolean relative = questions.askYesNo("Relative or Absolute Headmarker? Usually, 'Relative' is needed for ShB and higher.", "Relative", "Absolute");
			makeTargetConditions(hme).forEach(trigger::addCondition);
			if (relative) {
				int offset = hme.getMarkerOffset();
				trigger.setName(String.format("Headmarker %s%s", offset >= 0 ? "+" : "-", Math.abs(offset)));
				HeadmarkerRelativeIdFilter filter = new HeadmarkerRelativeIdFilter();
				filter.expected = offset;
				trigger.addCondition(filter);
			}
			else {
				long id = hme.getMarkerId();
				trigger.setName(String.format("Headmarker 0x%x", id));
				HeadmarkerAbsoluteIdFilter filter = new HeadmarkerAbsoluteIdFilter();
				filter.expected = id;
				trigger.addCondition(filter);
			}
			return trigger;

		}

		return null;
	}


	private static List<Condition<HasTargetIndex>> makeTargetIndexConditions(HasTargetIndex hti) {
		if (hti.getTargetIndex() == 0) {
			TargetIndexFilter tif = new TargetIndexFilter();
			tif.expected = 0;
			return Collections.singletonList(tif);
		}
		else {
			return Collections.emptyList();
		}
	}

	private static List<Condition<HasAbility>> makeAbilityConditions(HasAbility ha) {
		AbilityIdFilter aif = new AbilityIdFilter();
		aif.expected = ha.getAbility().getId();
		return Collections.singletonList(aif);
	}

	private static List<Condition<HasStatusEffect>> makeStatusConditions(HasStatusEffect hsa) {
		StatusIdFilter sif = new StatusIdFilter();
		sif.expected = hsa.getBuff().getId();
		// TODO: refresh vs initial app
		return Collections.singletonList(sif);
	}


	private List<Condition<HasSourceEntity>> makeSourceConditions(HasSourceEntity hse) {
		XivCombatant source = hse.getSource();
		if (source.isThePlayer()) {
			SourceEntityTypeFilter etf = new SourceEntityTypeFilter();
			etf.type = EntityType.THE_PLAYER;
			return Collections.singletonList(etf);
		}
		else if (source.walkParentChain().getPartyType() == 1) {
			SourcePartyMemberFilter spmf = new SourcePartyMemberFilter(state);
			return Collections.singletonList(spmf);
		}
		// TODO: party member
		else if (source.isPc()) {
			SourceEntityTypeFilter etf = new SourceEntityTypeFilter();
			etf.type = EntityType.ANY_PLAYER;
			return Collections.singletonList(etf);
		}
		else if (source.getbNpcId() != 0) {
			SourceEntityNpcIdFilter enif = new SourceEntityNpcIdFilter();
			enif.expected = source.getbNpcId();
			return Collections.singletonList(enif);
		}
		else {
			return Collections.emptyList();
		}
	}

	private List<Condition<HasTargetEntity>> makeTargetConditions(HasTargetEntity hte) {
		XivCombatant target = hte.getTarget();
		if (target.isThePlayer()) {
			TargetEntityTypeFilter etf = new TargetEntityTypeFilter();
			etf.type = EntityType.THE_PLAYER;
			return Collections.singletonList(etf);
		}
		else if (target.walkParentChain().getPartyType() == 1) {
			TargetPartyMemberFilter tpmf = new TargetPartyMemberFilter(state);
			return Collections.singletonList(tpmf);
		}
		// TODO: party member
		else if (target.isPc()) {
			TargetEntityTypeFilter etf = new TargetEntityTypeFilter();
			etf.type = EntityType.ANY_PLAYER;
			return Collections.singletonList(etf);
		}
		else if (target.getType() == CombatantType.NPC) {
			TargetEntityTypeFilter etf = new TargetEntityTypeFilter();
			etf.type = EntityType.NPC;
			return Collections.singletonList(etf);
		}
		else {
			return Collections.emptyList();
		}
	}

}
