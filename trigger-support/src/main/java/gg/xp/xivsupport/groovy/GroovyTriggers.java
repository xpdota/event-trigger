package gg.xp.xivsupport.groovy;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventHandler;
import gg.xp.reevent.events.TypedEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import groovy.lang.Closure;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.GroovySandbox;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@ScanMe
public class GroovyTriggers {

	private static final Logger log = LoggerFactory.getLogger(GroovyTriggers.class);
	private final GroovySandbox sandbox;

	public GroovyTriggers(GroovyManager manager) {
		this.sandbox = manager.getSandbox();
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

		public Builder<X> named(String name) {
			this.name = name;
			return this;
		}

		public Builder<X> type(Class<X> eventType) {
			this.type = eventType;
			return this;
		}

		public Builder<X> when(Closure<Boolean> condition) {
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

		public Builder<X> sequence(Closure<?> sequentialTriggerBody) {
			if (sequentialTriggerBody.getMaximumNumberOfParameters() != 2) {
				throw new IllegalArgumentException("Sequence must have two arguments (event and sequential trigger controller)");
			}
			this.sq = sequentialTriggerBody::call;
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
				throw new IllegalArgumentException("Must specify the event type for the event handler.");
			}
			if (handler == null) {
				if (sq == null) {
					throw new IllegalArgumentException("Must specify what the event handler should do with either 'then' or 'sequence'.");
				}
				else {
					SequentialTrigger<BaseEvent> sqFinalized = SqtTemplates.sq(timeout, type, condition, (e1, s) -> {
						try (SandboxScope ignored = sandbox.enter()) {
							sq.accept(e1, s);
						}
					});
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

	public void add(Closure<?> closure) {
		Builder<BaseEvent> builder = new Builder<>();
		closure.setDelegate(builder);
		closure.run();
		builder.finish();
	}
}
