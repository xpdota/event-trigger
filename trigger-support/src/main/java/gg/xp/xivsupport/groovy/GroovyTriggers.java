package gg.xp.xivsupport.groovy;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventHandler;
import gg.xp.reevent.events.TypedEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.callouts.CalloutTrackingKey;
import gg.xp.xivsupport.callouts.SingleValueReplacement;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerConcurrencyMode;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.groovy.helpers.CustomGString;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.speech.BasicCalloutEvent;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.speech.HasCalloutTrackingKey;
import gg.xp.xivsupport.speech.ProcessedCalloutEvent;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.GString;
import groovy.lang.GroovyObjectSupport;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.GroovySandbox;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ScanMe
public class GroovyTriggers {

	private static final Logger log = LoggerFactory.getLogger(GroovyTriggers.class);
	private final GroovySandbox sandbox;
	private final SingleValueReplacement svr;

	public GroovyTriggers(GroovyManager manager, SingleValueReplacement svr) {
		this.sandbox = manager.getSandbox();
		this.svr = svr;
	}

	private record InternalEventHandler<X extends Event>(
			Object uniqueKey,
			TypedEventHandler<X> handler
	) {
	}

	private List<InternalEventHandler<?>> handlers = Collections.emptyList();
	private final Object lock = new Object();
	private final Map<Object, InternalEventHandler<?>> mapping = new LinkedHashMap<>();

	public <X extends Event> void addHandler(Object key, Class<X> eventType, BiConsumer<X, EventContext> handler) {
		synchronized (lock) {
			log.info("Adding event handler '{}'", key);
			InternalEventHandler<?> replaced = mapping.put(key, new InternalEventHandler<X>(key, new TypedEventHandler<>() {
				@Override
				public Class<? extends X> getType() {
					return eventType;
				}

				@Override
				public void handle(EventContext context, X event) {
					handler.accept(event, context);
				}
			}));
			if (replaced != null) {
				log.info("Replaced an old event handler for key '{}'", key);
			}
			recheck();
		}
	}

	public <X extends Event> void removeHandler(Object key) {
		synchronized (lock) {
			log.info("Removing event handler '{}'", key);
			InternalEventHandler<?> removed = mapping.remove(key);
			if (removed == null) {
				log.info("Nothing to remove for event handler key '{}'", key);
			}
			else {
				log.info("Removed event handler '{}'", key);
			}
			recheck();
		}
	}

	public <X extends Event> void addGroovyHandler(Object key, Class<X> eventType, Closure<?> handler) {
		addHandler(key, eventType, (event, context) -> {
			try (SandboxScope ignored = sandbox.enter()) {
				handler.call(event, context);
			}
		});
	}

	private void recheck() {
		List<InternalEventHandler<?>> newList = new ArrayList<>(mapping.size());
		newList.addAll(mapping.values());
		handlers = newList;
	}


	@SuppressWarnings("unchecked")
	@HandleEvents
	public void handle(EventContext context, Event event) {
		for (InternalEventHandler<?> handler : handlers) {
			if (handler.handler.getType().isInstance(event)) {
				((EventHandler<Event>) handler.handler).handle(context, event);
			}
		}
	}

	public class Builder<X extends BaseEvent> {
		String name;
		Class<X> type;
		Predicate<X> condition = event -> true;
		BiConsumer<X, EventContext> handler;
		BiConsumer<X, SequentialTriggerController<BaseEvent>> sq;
		int timeout = 120_000;
		Closure<?> rawSqHandler;
		SequentialTriggerConcurrencyMode concurrencyMode = SequentialTriggerConcurrencyMode.BLOCK_NEW;

		public Builder<X> named(String name) {
			this.name = name;
			return this;
		}

		public Builder<X> type(Class<X> eventType) {
			this.type = eventType;
			return this;
		}

		public Builder<X> when(Closure<Boolean> condition) {
			if (condition.getMaximumNumberOfParameters() != 1) {
				throw new IllegalArgumentException("'when' must take a single parameter.");
			}
			Class<?> paramType = condition.getParameterTypes()[0];
			if (Event.class.isAssignableFrom(paramType) && type == null) {
				type = (Class<X>) paramType;
			}
			this.condition = condition::call;
			return this;
		}

		public Builder<X> then(Closure<?> handler) {
			int params = handler.getMaximumNumberOfParameters();
			this.handler = switch (params) {
				case 0 -> (event, context) -> handler.call();
				case 1 -> (event, context) -> handler.call(event);
				case 2 -> handler::call;
				default ->
						throw new IllegalArgumentException("Must have zero, one, or two arguments on the event handler");
			};
			return this;
		}

		public Builder<X> concurrency(SequentialTriggerConcurrencyMode concurrencyMode) {
			this.concurrencyMode = concurrencyMode;
			return this;
		}

		public SequentialTriggerConcurrencyMode getBlock() {
			return SequentialTriggerConcurrencyMode.BLOCK_NEW;
		}

		public SequentialTriggerConcurrencyMode getReplace() {
			return SequentialTriggerConcurrencyMode.REPLACE_OLD;
		}

		public SequentialTriggerConcurrencyMode getConcurrent() {
			return SequentialTriggerConcurrencyMode.CONCURRENT;
		}

		public Builder<X> sequence(@DelegatesTo(GroovySqHelper.class) Closure<?> sequentialTriggerBody) {
			if (sequentialTriggerBody.getMaximumNumberOfParameters() != 2) {
				throw new IllegalArgumentException("Sequence must have two arguments (event and sequential trigger controller)");
			}
			this.sq = sequentialTriggerBody::call;
			this.rawSqHandler = sequentialTriggerBody;
			return this;
		}

		public Builder<X> timeout(int timeout) {
			this.timeout = timeout;
			return this;
		}


		private void finish() {
			if (name == null) {
				throw new IllegalArgumentException("Must specify a unique name for the event handler.");
			}
			if (type == null) {
				throw new IllegalArgumentException("Must specify the event type for the event handler, or specify the parameter type on 'when'.");
			}
			if (handler == null) {
				if (sq == null) {
					throw new IllegalArgumentException("Must specify what the event handler should do with either 'then' or 'sequence'.");
				}
				else {
					SequentialTrigger<BaseEvent> sqFinalized = SqtTemplates.sq(timeout, type, condition, (e1, s) -> {
						try (SandboxScope ignored = sandbox.enter()) {
							// This doesn't work right unless we clone
							if (concurrencyMode == SequentialTriggerConcurrencyMode.CONCURRENT) {
								Closure<?> clonedSqHandler = (Closure<?>) rawSqHandler.clone();
								clonedSqHandler.setResolveStrategy(Closure.DELEGATE_FIRST);
								clonedSqHandler.setDelegate(new GroovySqHelper<>(s));
								clonedSqHandler.call(e1, s);
							}
							else {
								rawSqHandler.setDelegate(new GroovySqHelper<>(s));
								rawSqHandler.setResolveStrategy(Closure.DELEGATE_FIRST);
								sq.accept(e1, s);
							}
						}
					});
					sqFinalized.setConcurrency(this.concurrencyMode);
					addHandler(name, BaseEvent.class, (event, context) -> {
						try (SandboxScope ignored = sandbox.enter()) {
							sqFinalized.feed(context, event);
						}
					});
				}
			}
			else {
				if (sq != null) {
					throw new IllegalArgumentException("You must specify 'then' OR 'sequence', not both");
				}
				addHandler(name, type, (event, context) -> {
					try (SandboxScope ignored = sandbox.enter()) {
						if (condition.test(event)) {
							handler.accept(event, context);
						}
					}
				});
			}
		}
	}

	public void add(@DelegatesTo(Builder.class) Closure<?> closure) {
		Builder<BaseEvent> builder = new Builder<>();
		closure.setDelegate(builder);
		closure.setResolveStrategy(Closure.DELEGATE_FIRST);
		closure.run();
		builder.finish();
	}

	private BooleanSupplier wrapBooleanSupplier(BooleanSupplier supplier) {
		return () -> {
			try (SandboxScope ignored = sandbox.enter()) {
				return supplier.getAsBoolean();
			}
		};
	}

	private <X> Supplier<X> wrapSupplier(Supplier<X> supplier) {
		return () -> {
			try (SandboxScope ignored = sandbox.enter()) {
				return supplier.get();
			}
		};
	}

	/**
	 * Provides some wrapper methods over {@link SequentialTriggerController} to make things more convenient in
	 * Groovy-land.
	 *
	 * @param <X> The event type for the sequential trigger (usually BaseEvent)
	 */
	public class GroovySqHelper<X extends BaseEvent> extends GroovyObjectSupport {
		private final SequentialTriggerController<X> controller;
		private final Binding binding;
		private HasCalloutTrackingKey last;

		public GroovySqHelper(SequentialTriggerController<X> controller) {
			this.controller = controller;
			this.binding = new Binding();
		}

		public CalloutEvent callout(@DelegatesTo(GroovyCalloutBuilder.class) Closure<?> closure) {
			GroovyCalloutBuilder gcb = new GroovyCalloutBuilder(() -> last);
			closure.setDelegate(gcb);
			closure.setResolveStrategy(Closure.DELEGATE_FIRST);
			closure.run();
			Supplier<String> text = gcb.text;
			Duration timeBasis = controller.timeSinceStart();
			Duration expiresAt = timeBasis.plusMillis(gcb.duration);
			BooleanSupplier expired = gcb.expired == null ? () -> controller.timeSinceStart().compareTo(expiresAt) > 0 : wrapBooleanSupplier(gcb.expired);
			ProcessedCalloutEvent callout = new ProcessedCalloutEvent(
					new CalloutTrackingKey(),
					gcb.tts,
					wrapSupplier(text),
					expired,
					gcb.guiProvider,
					gcb.color,
					gcb.soundFile
			);
			callout.setReplaces(gcb.replaces);
			controller.accept(callout);
			log.info("Callout {} replaces {}", callout.trackingKey(), callout.replaces() == null ? null : callout.replaces().trackingKey());
			this.last = callout;
			return callout;
		}

		public CalloutEvent callout(String ttsAndText) {
			BasicCalloutEvent event = new BasicCalloutEvent(ttsAndText);
			controller.accept(event);
			return event;
		}

		public void waitMs(int ms) {
			controller.waitMs(ms);
		}

		@Override
		public Object invokeMethod(String name, Object args) {
			return super.invokeMethod(name, args);
		}

		@Override
		public Object getProperty(String propertyName) {
			if (binding.hasVariable(propertyName)) {
				return binding.getVariable(propertyName);
			}
			return super.getProperty(propertyName);
		}

		@Override
		public void setProperty(String propertyName, Object newValue) {
			binding.setVariable(propertyName, newValue);
		}
	}

	public class GroovyCalloutBuilder extends GroovyObjectSupport {
		private final Supplier<HasCalloutTrackingKey> last;
		@Nullable String tts;
		@NotNull Supplier<@Nullable String> text = () -> null;
		int duration = 5000;
		@Nullable HasCalloutTrackingKey replaces;
		@Nullable BooleanSupplier expired;
		@Nullable Color color;
		@Nullable String soundFile;
		@NotNull Supplier<@Nullable Component> guiProvider = () -> null;

		public GroovyCalloutBuilder(Supplier<HasCalloutTrackingKey> last) {
			this.last = last;
		}

		private Supplier<String> convertGs(GString string) {
			GString modified = CustomGString.of(string.getValues(), string.getStrings(), svr::singleReplacement);
			return modified::toString;
		}

		public GroovyCalloutBuilder tts(String tts) {
			this.tts = tts;
			return this;
		}

		public GroovyCalloutBuilder tts(Supplier<String> tts) {
			return tts(tts.get());
		}

		public GroovyCalloutBuilder tts(GString tts) {
			return tts(convertGs(tts).get());
		}

		public GroovyCalloutBuilder text(String text) {
			return text(() -> text);
		}

		public GroovyCalloutBuilder text(Supplier<String> text) {
			this.text = text;
			return this;
		}

		public GroovyCalloutBuilder text(GString text) {
			return text(convertGs(text));
		}

		public GroovyCalloutBuilder both(String both) {
			tts(both);
			text(both);
			return this;
		}

		public GroovyCalloutBuilder both(Supplier<String> both) {
			tts(both);
			text(both);
			return this;
		}

		public GroovyCalloutBuilder both(GString both) {
			tts(both);
			text(both);
			return this;
		}

		public GroovyCalloutBuilder duration(int duration) {
			this.duration = duration;
			return this;
		}

		public GroovyCalloutBuilder displayWhile(BooleanSupplier displayWhile) {
			this.expired = () -> !displayWhile.getAsBoolean();
			return this;
		}

		public GroovyCalloutBuilder replaces(CalloutTrackingKey replaces) {
			this.replaces = () -> replaces;
			return this;
		}

		public GroovyCalloutBuilder replaces(HasCalloutTrackingKey replaces) {
			this.replaces = replaces;
			return this;
		}

		public GroovyCalloutBuilder color(Color color) {
			this.color = color;
			return this;
		}

		public GroovyCalloutBuilder color(int r, int g, int b, int a) {
			this.color = new Color(r, g, b, a);
			return this;
		}

		public GroovyCalloutBuilder color(int r, int g, int b) {
			this.color = new Color(r, g, b);
			return this;
		}

		public GroovyCalloutBuilder sound(String soundFile) {
			this.soundFile = soundFile;
			return this;
		}

		public GroovyCalloutBuilder gui(Supplier<Component> guiProvider) {
			this.guiProvider = guiProvider;
			return this;
		}

		public HasCalloutTrackingKey getLast() {
			return last.get();
		}
//
//		@Override
//		public Object getProperty(String propertyName) {
//			return super.getProperty(propertyName);
//		}

		/**
		 * Adds a specific status icon to a callout.
		 *
		 * @param statusId The status effect ID
		 * @return this (builder pattern)
		 */
		public GroovyCalloutBuilder statusIcon(long statusId) {
			this.guiProvider = () -> IconTextRenderer.getStretchyIcon(StatusEffectLibrary.iconForId(statusId, 0));
			return this;
		}

		/**
		 * Adds a specific status icon to a callout.
		 *
		 * @param statusId The status effect ID
		 * @param stacks   The stack count to use for the icon
		 * @return this (builder pattern)
		 */
		public GroovyCalloutBuilder statusIcon(long statusId, long stacks) {
			this.guiProvider = () -> IconTextRenderer.getStretchyIcon(StatusEffectLibrary.iconForId(statusId, stacks));
			return this;
		}

		/**
		 * Adds a specific ability icon to a callout.
		 *
		 * @param abilityId The ability ID
		 * @return this (builder pattern)
		 */
		public GroovyCalloutBuilder abilityIcon(long abilityId) {
			this.guiProvider = () -> IconTextRenderer.getStretchyIcon(ActionLibrary.iconForId(abilityId));
			return this;
		}
	}

}
