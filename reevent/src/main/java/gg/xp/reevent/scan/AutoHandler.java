package gg.xp.reevent.scan;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.TypedEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

public class AutoHandler implements TypedEventHandler<Event> {

	private static final Logger log = LoggerFactory.getLogger(AutoHandler.class);

	private final Method method;
	private final AutoHandlerInvoker invoker;
	private final String methodLabel;
	private final String extraLabel;
	private final Object clazzInstance;
	private final Class<? extends Event> eventClass;
	private final int order;
	private final Class<?> clazz;
	private final AutoHandlerConfig config;
	private final boolean onlyInLive;
	private final boolean compact;

	private volatile boolean enabled = true;

	@SuppressWarnings("unchecked")
	public AutoHandler(Class<?> clazz, Method method, Object clazzInstance, AutoHandlerConfig config) {
		this.clazz = clazz;
		this.config = config;
		if (method == null) {
			throw new IllegalArgumentException("Method cannot be null");
		}
		if (clazzInstance == null) {
			throw new IllegalArgumentException("Instance cannot be null");
		}
		// TODO: exception types
		Class<?>[] paramTypes = method.getParameterTypes();
		Class<?> declaring = method.getDeclaringClass();
		Class<?> actualCls = clazzInstance.getClass();
		String tmpMethodLabel = declaring.getSimpleName() + '.' + method.getName();
		log.trace("Setting up method {}", tmpMethodLabel);
		if (paramTypes.length == 1) {
			if (!Event.class.isAssignableFrom(paramTypes[0])) {
				throw new IllegalStateException("Error setting up method %s: method signature must be (EventContext, Event), but was (%s)".formatted(
						tmpMethodLabel,
						Arrays.stream(paramTypes).map(cls -> {
							String cn = cls.getCanonicalName();
							if (cn != null) {
								return cn;
							}
							else {
								return cls.toString();
							}
						}).collect(Collectors.joining(", "))));
			}
			this.eventClass = (Class<? extends Event>) paramTypes[0];
			compact = true;
		}
		else if (paramTypes.length == 2) {
			if (!EventContext.class.isAssignableFrom(paramTypes[0]) || !Event.class.isAssignableFrom(paramTypes[1])) {
				throw new IllegalStateException("Error setting up method %s: method signature must be (EventContext, Event), but was (%s)".formatted(
						tmpMethodLabel,
						Arrays.stream(paramTypes).map(cls -> {
							String cn = cls.getCanonicalName();
							if (cn != null) {
								return cn;
							}
							else {
								return cls.toString();
							}
						}).collect(Collectors.joining(", "))));
			}
			this.eventClass = (Class<? extends Event>) paramTypes[1];
			compact = false;
		}
		else {
			throw new IllegalStateException("Error setting up method %s: wrong number of parameters (should be 1 or 2, but was %d)".formatted(tmpMethodLabel, paramTypes.length));
		}
		this.method = method;
		this.invoker = compact ? new AutoHandlerInvokerCompact(method, clazzInstance) : new AutoHandlerInvokerWide(method, clazzInstance);
		if (actualCls.equals(declaring)) {
			this.methodLabel = "%s.%s:%s".formatted(declaring.getSimpleName(), method.getName(), eventClass.getSimpleName());
		}
		else {
			this.methodLabel = "%s(%s).%s:%s".formatted(declaring.getSimpleName(), actualCls.getSimpleName(), method.getName(), eventClass.getSimpleName());

		}
		this.clazzInstance = clazzInstance;
		HandleEvents annotation = this.method.getAnnotation(HandleEvents.class);
		if (annotation != null) {
			order = annotation.order();
			String name = annotation.name();
			if (name == null || name.isEmpty()) {
				extraLabel = null;
			}
			else {
				extraLabel = name;
			}
		}
		else {
			order = 0;
			extraLabel = null;
		}
		onlyInLive = method.isAnnotationPresent(LiveOnly.class) || clazz.isAnnotationPresent(LiveOnly.class);
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String toString() {
		return String.format("AutoHandler(%s)", methodLabel);
	}

	public String getTopoLabel() {
		if (extraLabel == null) {
			return String.format("%s(%s)", method.getName(), eventClass.getSimpleName());
		}
		else {
			return String.format("%s -- %s(%s)", extraLabel, method.getName(), eventClass.getSimpleName());

		}
	}

	public String getTopoKey() {
		return String.format("%s.%s", method.getName(), eventClass.getSimpleName());
	}

	public String getLongTopoLabel() {
		return String.format("%s.%s(%s)", clazz.getSimpleName(), method.getName(), eventClass.getSimpleName());
	}

	public Class<?> getHandlerClass() {
		return clazz;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (enabled != this.enabled) {
			log.info("AutoHandler {} is now {}", getLongTopoLabel(), enabled ? "ENABLED" : "DISABLED");
			this.enabled = enabled;
		}
	}

	private boolean warnedAboutTestOnlySkip;

	@Override
	public void handle(EventContext context, Event event) {
		if (!enabled) {
			return;
		}
		if (!eventClass.isInstance(event)) {
			return;
		}
		if (clazzInstance instanceof FilteredEventHandler feh) {
			if (!feh.enabled(context)) {
				return;
			}
		}
		if (config.isNotLive() && onlyInLive) {
			if (!warnedAboutTestOnlySkip) {
				log.info("Skipping {} because it is disabled for tests", getLongTopoLabel());
				warnedAboutTestOnlySkip = true;
			}
			return;
		}
		try {
//			if (compact) {
//				method.invoke(clazzInstance, event);
//			}
//			else {
//				method.invoke(clazzInstance, context, event);
//			}
			// Quick math on this implementation:
			/*
				Used a DSR log
				Skipped first 1000, then enabled profiling
				~780k total events
				~44,220,000 total handler invocations
				Old #1:
					Time sum: 32.212s
					Do-nothing handler: .231us mean, 333.4us max, 180.27ms sum
				Old #2:
					Time sum: 31.966s
					Do-nothing handler: .299us mean, 436.3us max, 232.67ms sum
				New #1:
					Time sum: 31.999s
					Do-nothing handler: .217us mean, 244.6us max, 168.75ms sum

				seems to save about 50ns per invocation
				257 total handlers
				= 12.8ns per event? not worth?
			 */
			invoker.handle(context, event);
		}
		catch (Throwable e) {
			log.error("Error invoking trigger method {}", methodLabel, e);
		}
	}

	@Override
	public boolean requiresContext() {
//		return true;
		return invoker.requiresContext();
	}

	@Override
	public Class<? extends Event> getType() {
		return eventClass;
	}
}
