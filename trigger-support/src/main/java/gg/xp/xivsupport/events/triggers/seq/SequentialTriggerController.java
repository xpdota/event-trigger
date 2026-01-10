package gg.xp.xivsupport.events.triggers.seq;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastCancel;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.CastLocationDataEvent;
import gg.xp.xivsupport.events.actlines.events.DescribesCastLocation;
import gg.xp.xivsupport.events.delaytest.BaseDelayedEvent;
import gg.xp.xivsupport.events.state.RefreshCombatantsRequest;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastResult;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.speech.BaseCalloutEvent;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.speech.HasCalloutTrackingKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SequentialTriggerController<X extends BaseEvent> {

	private static final Logger log = LoggerFactory.getLogger(SequentialTriggerController.class);
	private static final AtomicInteger threadIdCounter = new AtomicInteger();
	//	private final Instant expiresAt;
	private final BooleanSupplier expired;
	private final Thread triggerThread;
	private final Object lock = new Object();
	private final X initialEvent;
	private final int timeout;
	private volatile X currentEvent;
	private volatile EventContext context;
	private volatile boolean done;
	private volatile boolean processing = true;
	private volatile boolean die;
	private volatile boolean dieSilently;
	private volatile boolean cycleProcessingTimeExceeded;
	private volatile @Nullable Predicate<X> filter;
	private final Map<String, Object> params = new LinkedHashMap<>();

	// To be called from external thread
	public SequentialTriggerController(EventContext initialEventContext, X initialEvent, BiConsumer<X, SequentialTriggerController<X>> triggerCode, int timeout) {
		expired = () -> initialEvent.getEffectiveTimeSince().toMillis() > timeout;
		this.timeout = timeout;
//		expiresAt = initialEvent.getHappenedAt().plusMillis(timeout);
		context = initialEventContext;
		triggerThread = new Thread(() -> {
			try {
				triggerCode.accept(initialEvent, this);
			}
			catch (SequentialTriggerPleaseDie e) {
				log.info("Sequential Trigger Requested to End");
			}
			catch (Throwable t) {
				log.error("Error in sequential trigger", t);
			}
			finally {
				synchronized (lock) {
					log.info("Sequential trigger done");
					processing = false;
					done = true;
					lock.notifyAll();
				}
			}
		}, "SequentialTrigger-" + threadIdCounter.getAndIncrement());
		this.initialEvent = initialEvent;
		triggerThread.setDaemon(true);
		triggerThread.setPriority(Thread.MAX_PRIORITY);
		triggerThread.start();
		synchronized (lock) {
			waitProcessingDone();
		}
	}

	// To be called from internal thread
	public void accept(Event event) {
		log.info("Accepting: {}", event);
		context.accept(event);
	}

	public void enqueue(Event event) {
		if (event instanceof DelayedSqtEvent) {
			log.trace("Enqueueing: {}", event);
		}
		else {
			log.info("Enqueueing: {}", event);
		}
		context.enqueue(event);
	}

	public void waitThenRefreshCombatants(long delay) {
		waitMs(delay);
		accept(new RefreshCombatantsRequest());
		waitMs(delay);
	}

	public void refreshCombatants() {
		accept(new RefreshCombatantsRequest());
	}

	public void refreshCombatants(long delay) {
		accept(new RefreshCombatantsRequest());
		waitMs(delay);
	}

	public void forceExpire() {
		synchronized (lock) {
			// Also make it configurable as to whether or not a wipe ends the trigger
			log.info("Sequential trigger force expired");
			die = true;
			lock.notifyAll();
		}
	}

	public void stopSilently() {
		synchronized (lock) {
			log.info("Sequential trigger stopping by request");
			dieSilently = true;
			die = true;
			lock.notifyAll();
		}
	}

	@SystemEvent
	static class DelayedSqtEvent extends BaseDelayedEvent {

		@Serial
		private static final long serialVersionUID = -4212233775615486768L;

		DelayedSqtEvent(long ms) {
			super(ms);
		}

		@Override
		public String toString() {
			return "DelayedSqtEvent(%s)".formatted(delayMs);
		}
	}

	public void waitDuration(Duration duration) {
		waitMs(duration.toMillis());
	}

	public void waitMs(long ms) {
		log.trace("in waitMs");
		if (ms <= 0) {
			log.warn("waitMs called with non-positive value: {}", ms);
		}
		long initial = initialEvent.getEffectiveTimeSince().toMillis();
		long doneAt = initial + ms;
		// This can stop waiting when it hits the original event (due to the delay), OR any other event (which is more
		// likely when replaying)
		DelayedSqtEvent event = new DelayedSqtEvent(ms);
		enqueue(event);
		waitEvent(BaseEvent.class, e -> initialEvent.getEffectiveTimeSince().toMillis() >= doneAt);
	}

	private @Nullable HasCalloutTrackingKey lastCall;

	public @Nullable HasCalloutTrackingKey getLastCall() {
		return lastCall;
	}

	public void expireLastCall() {
		if (lastCall instanceof RawModifiedCallout<?> rmc) {
			rmc.forceExpire();
		}
		else if (lastCall instanceof BaseCalloutEvent bce) {
			bce.forceExpire();
		}
	}

	/**
	 * Accept a new callout event, BUT mark it as "replacing" any previous call
	 * i.e. update callout text + emit a new TTS
	 *
	 * @param call The new callout
	 */
	public void updateCall(CalloutEvent call) {
		if (call == null) {
			return;
		}
		if (lastCall != null) {
			call.setReplaces(lastCall);
		}
		lastCall = call;
		accept(call);
	}

	/**
	 * Retrives current callout params. Set new values using {@link #setParam(String, Object)}
	 *
	 * @return The current callout parameters map
	 */
	public Map<String, Object> getParams() {
		return new HashMap<>(params);
	}

	/**
	 * Sets a parameter which will be passed into ModifiableCallout instances in {@link #call} or {@link #updateCall}.
	 *
	 * @param name  The param/variable name
	 * @param value The value
	 */
	public void setParam(String name, Object value) {
		params.put(name, value);
	}
//
//	/**
//	 * Sets a parameter which will be passed into ModifiableCallout instances in {@link #call} or {@link #updateCall}.
//	 * This version uses a supplier rather than a concrete value, and the value is re-evaluated every time
//	 * {@link #getParams()} is called (including when 'call' or 'updateCall' is used).
//	 *
//	 * @param name  The param/variable name
//	 * @param value The value supplier
//	 */
//	public void setParam(String name, Supplier<? extends Object> value) {
//		TODO: not done
//		params.put(name, value);
//	}

	/**
	 * Replace the last call used with updateCall (if any) with this one. Automatically handles parameters set with
	 * {@link #setParam(String, Object)}. Equivalent to calling {@code updateCall(call.getModified(getParams())}.
	 * <p>
	 * This particular version does not take an event. See {@link #updateCall(ModifiableCallout, Object)} if you are
	 * supplying an event.
	 *
	 * @param call The callout
	 */
	public void updateCall(ModifiableCallout<?> call) {
		if (call == null) {
			return;
		}
		updateCall(call.getModified(getParams()));
	}

	/**
	 * Replace the last call used with updateCall (if any) with this one. Automatically handles parameters set with
	 * {@link #setParam(String, Object)}. Equivalent to calling {@code updateCall(call.getModified(event, getParams())}.
	 * <p>
	 * This particular version takes an event. See {@link #updateCall(ModifiableCallout)} if you are
	 * NOT supplying an event.
	 *
	 * @param call The callout
	 * @return The modified call.
	 */
	@Contract("null, _ -> null; !null, _ -> !null")
	public <C> @Nullable RawModifiedCallout<C> updateCall(ModifiableCallout<C> call, C event) {
		if (call == null) {
			return null;
		}
		RawModifiedCallout<C> out = call.getModified(event, getParams());
		updateCall(out);
		return out;
	}

	/**
	 * Trigger a callout. Does not replace nor is replaced by any other call unless explicitly done by your code.
	 * Automatically handles parameters set with {@link #setParam(String, Object)}.
	 * Equivalent to calling {@code accept(call.getModified(getParams())}.
	 * <p>
	 * This particular version does NOT take an event. See {@link #call(ModifiableCallout, Object)} if you are
	 * supplying an event.
	 *
	 * @param call The callout
	 * @return The modified call
	 */
	@Contract("null -> null; !null -> !null")
	public @Nullable RawModifiedCallout<?> call(ModifiableCallout<?> call) {
		if (call == null) {
			return null;
		}
		RawModifiedCallout<?> out = call.getModified(getParams());
		accept(out);
		return out;
	}

	/**
	 * Trigger a callout. Does not replace nor is replaced by any other call unless explicitly done by your code.
	 * Automatically handles parameters set with {@link #setParam(String, Object)}.
	 * Equivalent to calling {@code accept(call.getModified(getParams())}.
	 * <p>
	 * This particular version takes an event. See {@link #call(ModifiableCallout)} if you are
	 * NOT supplying an event.
	 *
	 * @param call The callout
	 * @return The modified call
	 */
	@Contract("null, _ -> null; !null, _ -> !null")
	public <C> @Nullable RawModifiedCallout<C> call(ModifiableCallout<C> call, C event) {
		if (call == null) {
			return null;
		}
		RawModifiedCallout<C> out = call.getModified(event, getParams());
		accept(out);
		return out;
	}

	/**
	 * @return The duration since this trigger began
	 */
	public Duration timeSinceStart() {
		return initialEvent.getEffectiveTimeSince();
	}

	/**
	 * Accept a new callout event, BUT mark it as "replacing" any previous call
	 * i.e. update callout text + emit a new TTS
	 *
	 * @param call The new callout
	 */
	public void updateCall(RawModifiedCallout<?> call) {
		// These extra conditions are needed because there might be a call that is disabled/has no text.
		// e.g. Call 1 = "foo", Call 2 = empty/null, Call 3 = "bar"
		// We would want "bar" to replace "foo", with call 2 being a no-op as far as text is concerned because it has
		// no text.
		// What would happen without this is that call 2 would "replace" call 1, but the visual text overlay
		// would ignore it because it is blank. Then call 3 would replace call 2, but the visual text overlay completely
		// forgot about call 2.
		if (call.getText() != null && !call.getText().isBlank()) {
			if (lastCall != null) {
				call.setReplaces(lastCall);
			}
			lastCall = call;
		}
		accept(call);
	}


	// TODO: warning/error if you try to wait for an event type that is incompatible with X
	// i.e. if both X and Y are concrete types, and Y is not a subclass of X, then it should fail-fast

	// To be called from internal thread
	public <Y> Y waitEvent(Class<Y> eventClass) {
		return waitEvent(eventClass, (e) -> true);
	}

	// To be called from internal thread
	public <Y> Y waitEvent(Class<Y> eventClass, Predicate<Y> eventFilter) {
		log.trace("Waiting for specific event");
		return (Y) waitEvent(event -> eventClass.isInstance(event) && eventFilter.test((Y) event));
	}

	// To be called from internal thread

	/**
	 * Wait for a fixed number of events and put them in a list. Equivalent to calling {@link #waitEvent(Class)}
	 * multiple times and adding the results to a list.
	 *
	 * @param events     The number of events to wait for
	 * @param eventClass The class of events to wait for
	 * @param <Y>        The class of events to wait for
	 * @return The list of events
	 */
	public <Y> List<Y> waitEvents(int events, Class<Y> eventClass) {
		return waitEvents(events, eventClass, e -> true);
	}

	// To be called from internal thread

	/**
	 * Wait for a fixed number of events matching a condition and put them in a list. Equivalent to calling
	 * {@link #waitEvent(Class, Predicate)} multiple times and adding the results to a list.
	 *
	 * @param events      The number of events to wait for
	 * @param eventClass  The class of events to wait for
	 * @param eventFilter The condition for the events
	 * @param <Y>         The class of events to wait for
	 * @return The list of events
	 */
	public <Y> List<Y> waitEvents(int events, Class<Y> eventClass, Predicate<Y> eventFilter) {
		return IntStream.range(0, events)
				.mapToObj(i -> waitEvent(eventClass, eventFilter))
				.toList();
	}

	/**
	 * Wait for a certain number of events, but with multiple filters in an 'or' fashion. This method returns a map
	 * of filters to a list of events that passed that filter. With 'exclusive' set to true, an event that matches
	 * more than one filter will only be assigned to the first in the return value.
	 *
	 * @param limit      Number of events
	 * @param timeoutMs  Timeout in ms to wait for events
	 * @param eventClass Class of event
	 * @param exclusive  false if you would like an event to be allowed to match multiple filters, rather than movingn
	 *                   on to the next event after a single match.
	 * @param filters    The list of filters.
	 * @param <P>        The type of filter.
	 * @param <Y>        The type of event.
	 * @return A map, where the keys are the filters, and the values are a list of events that matched that
	 * filter.
	 */
	public <P extends Predicate<? super Y>, Y> Map<P, List<Y>> groupEvents(int limit, int timeoutMs, Class<Y> eventClass, boolean exclusive, List<P> filters) {
		Duration end = timeSinceStart().plusMillis(timeoutMs);
//		log.info("End ms: {}", end.toMillis());
		DelayedSqtEvent event = new DelayedSqtEvent(timeoutMs);
		enqueue(event);
		List<Y> rawEvents = waitEventsUntil(limit, eventClass, e -> filters.stream().anyMatch(p -> p.test(e)), BaseEvent.class, unused -> {
			Duration tss = timeSinceStart();
//			log.info("TSS ms: {}", tss.toMillis());
			return tss.compareTo(end) > 0;
		});
		log.info("groupEvents: {} total", rawEvents.size());
		Map<P, List<Y>> out = new HashMap<>(filters.size());
		// Even if there were no matches for a particular filter, we should set that key's value to an empty list
		for (P filter : filters) {
			out.put(filter, new ArrayList<>());
		}
		for (Y rawEvent : rawEvents) {
			for (P filter : filters) {
				if (filter.test(rawEvent)) {
					out.get(filter).add(rawEvent);
					if (exclusive) {
						break;
					}
				}
			}
		}
		return out;
	}

	/**
	 * Like {@link #groupEvents(int, int, Class, boolean, List)}, but uses {@link EventCollector} objects to filter
	 * and collect the result. This method works exactly like groupEvents, but does not return anything. Rather, you
	 * would query each of your EventCollectors to see what was matched. Each collector specifies what should be
	 * matched, and will be populated with the matches.
	 *
	 * @param limit      Number of events
	 * @param timeoutMs  Timeout in ms to wait for events
	 * @param eventClass Class of event
	 * @param exclusive  false if you would like an event to be allowed to match multiple filters, rather than moving
	 *                   on to the next event after a single match.
	 * @param collectors The list of collectors.
	 * @param <Y>        The type of event.
	 */
	public <Y> void collectEvents(int limit, int timeoutMs, Class<Y> eventClass, boolean exclusive, List<EventCollector<? super Y>> collectors) {
		Map<EventCollector<? super Y>, List<Y>> result = groupEvents(limit, timeoutMs, eventClass, exclusive, collectors);
		for (EventCollector<? super Y> collector : collectors) {
			collector.provideEvents(result.get(collector));
		}
	}

	public <Y extends BaseEvent> List<Y> waitEventsQuickSuccession(int limit, Class<Y> eventClass, Predicate<Y> eventFilter) {
		return waitEventsQuickSuccession(limit, eventClass, eventFilter, Duration.ofMillis(200));
	}

	public <Y extends BaseEvent> List<Y> waitEventsQuickSuccession(int limit, Class<Y> eventClass, Predicate<Y> eventFilter, Duration maxDelta) {
		List<Y> out = new ArrayList<>();
		Y last = waitEvent(eventClass, eventFilter);
		out.add(last);
		while (true) {
			X event = waitEvent(e -> true);
			// First possibility - event we're interested int
			if (eventClass.isInstance(event) && eventFilter.test((Y) event)) {
				out.add((Y) event);
				last = (Y) event;
				// If we have reached the limit, return it now
				if (out.size() >= limit) {
					return out;
				}
			}
			// Second possibility - hit our stop trigger
			else if (last.getEffectiveTimeSince().compareTo(maxDelta) > 0) {
				log.info("Sequential trigger stopping on {}", event);
				return out;
			}
			// Third possibility - keep looking
		}
	}

	public void waitBuffRemoved(StatusEffectRepository repo, BuffApplied buff) {
		while (repo.statusOrRefreshActive(buff)) {
			waitEvent(BuffRemoved.class, br -> br.getBuff().equals(buff.getBuff()) && br.getTarget().equals(buff.getTarget()));
		}
	}

	public BuffApplied findOrWaitForBuff(StatusEffectRepository repo, Predicate<BuffApplied> condition) {
		BuffApplied buff = repo.findBuff(condition);
		if (buff != null) {
			return buff;
		}
		else {
			return waitEvent(BuffApplied.class, condition);
		}
	}

	/**
	 * Find an active cast, or wait for the matching cast to start.
	 *
	 * @param repo           The ActiveCastRepository
	 * @param condition      The condition for the cast, i.e. the same thing you would feed to {@link #waitEvent}
	 *                       and similar methods
	 * @param includeExpired Whether to allow already-completed casts.
	 * @return The cast.
	 */
	public AbilityCastStart findOrWaitForCast(ActiveCastRepository repo, Predicate<AbilityCastStart> condition, boolean includeExpired) {
		var castMaybe = repo.getAll().stream().filter(ct -> {
			if (!includeExpired && ct.getResult() != CastResult.IN_PROGRESS) {
				return false;
			}
			return condition.test(ct.getCast());
		}).findFirst().map(CastTracker::getCast).orElse(null);
		if (castMaybe != null) {
			return castMaybe;
		}
		else {
			return waitEvent(AbilityCastStart.class, condition);
		}
	}

	/**
	 * Like {@link #findOrWaitForCast(ActiveCastRepository, Predicate, boolean)}, but also requires that the cast have
	 * location data. Waits if it does not find said data.
	 *
	 * @param repo           The ActiveCastRepository
	 * @param condition      The condition for the cast, i.e. the same thing you would feed to {@link #waitEvent}
	 *                       and similar methods
	 * @param includeExpired Whether to allow already-completed casts.
	 * @return The cast.
	 */
	public AbilityCastStart findOrWaitForCastWithLocation(ActiveCastRepository repo, Predicate<AbilityCastStart> condition, boolean includeExpired) {
		var castMaybe = repo.getAll().stream().filter(ct -> {
			if (!includeExpired && ct.getResult() != CastResult.IN_PROGRESS) {
				return false;
			}
			return condition.test(ct.getCast()) && ct.getCast().getLocationInfo() != null;
		}).findFirst().map(CastTracker::getCast).orElse(null);
		if (castMaybe != null) {
			return castMaybe;
		}
		else {
			return waitEvent(CastLocationDataEvent.class, clde -> condition.test(clde.originalEvent())).originalEvent();
		}
	}

	public DescribesCastLocation<AbilityCastStart> waitForCastLocation(AbilityCastStart event) {
		if (event.getLocationInfo() != null) {
			return event.getLocationInfo();
		}
		return waitEvent(CastLocationDataEvent.class, clde -> clde.originalEvent() == event);
	}

	/**
	 * Wait for a cast to finish, or return immediately if it already has finished.
	 *
	 * @param repo The ActiveCastRepository
	 * @param cast The cast whose finish you wish to wait for.
	 * @return The event that ended the cast. Will usually be an {@link AbilityUsedEvent}, but can also be other
	 * event types such as {@link AbilityCastCancel} for when the cast is interrupted.
	 */
	public BaseEvent waitCastFinished(ActiveCastRepository repo, AbilityCastStart cast) {
		var castMaybe = repo.getAll().stream().filter(ct -> ct.getCast() == cast).findFirst().orElse(null);
		if (castMaybe != null) {
			BaseEvent end = castMaybe.getEnd();
			if (end != null) {
				return end;
			}
		}
		// TODO: not correct - should also check for AbilityCastCancel
		return waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == cast);
	}


	public List<AbilityUsedEvent> collectAoeHits(Predicate<AbilityUsedEvent> condition) {
		List<AbilityUsedEvent> out = new ArrayList<>(8);
		AbilityUsedEvent aue;
		do {
			aue = waitEvent(AbilityUsedEvent.class, condition);
			out.add(aue);
		} while (!aue.isLastTarget());
		return out;
	}

	/**
	 * Wait for an event matching a filter, but bail if we see an event matching a different filter.
	 *
	 * @param eventClass The event we want.
	 * @param eventFilter The filter for the event.
	 * @param stopOnType The type of event we want to stop on.
	 * @param stopOn The filter for the stop event.
	 * @return The event, unless we hit the stop condition first, in which case null is returned.
	 * @param <Y> The type of event we want.
	 * @param <Z> The type of event we want to stop on.
	 */
	public <Y, Z> @Nullable Y waitEventUntil(Class<Y> eventClass, Predicate<Y> eventFilter, Class<Z> stopOnType, Predicate<Z> stopOn) {
		List<Y> events = waitEventsUntil(1, eventClass, eventFilter, stopOnType, stopOn);
		if (events.isEmpty()) {
			return null;
		}
		else {
			return events.get(0);
		}
	}


	/**
	 * Wait for events matching a filter, but stop collecting events once we see an event matching a different filter,
	 * or when we hit our limit.
	 *
	 * @param limit The maximum number of events to wait for.
	 * @param eventClass The event we want.
	 * @param eventFilter The filter for the event.
	 * @param stopOnType The type of event we want to stop on.
	 * @param stopOn The filter for the stop event.
	 * @return The list of events collected.
	 * @param <Y> The type of event we want.
	 * @param <Z> The type of event we want to stop on.
	 */
	public <Y, Z> List<Y> waitEventsUntil(int limit, Class<Y> eventClass, Predicate<Y> eventFilter, Class<Z> stopOnType, Predicate<Z> stopOn) {
		List<Y> out = new ArrayList<>();
		while (true) {
			X event = waitEvent(e -> true);
			// First possibility - event we're interested int
			if (eventClass.isInstance(event) && eventFilter.test((Y) event)) {
				out.add((Y) event);
				// If we have reached the limit, return it now
				if (out.size() >= limit) {
					return out;
				}
			}
			// Second possibility - hit our stop trigger
			else if (stopOnType.isInstance(event) && stopOn.test((Z) event)) {
				log.info("Sequential trigger stopping on {}", event);
				return out;
			}
			// Third possibility - keep looking
		}
	}

	// To be called from internal thread
	private X waitEvent(Predicate<X> filter) {
		synchronized (lock) {
			processing = false;
			currentEvent = null;
			context = null;
			this.filter = filter;
			lock.notifyAll();
			while (true) {
				if (die) {
					if (dieSilently) {
						throw new SequentialTriggerPleaseDie("Sequential trigger stopping by request");
					}
					// Deprecated, but.......?
					// Seems better than leaving threads hanging around doing nothing.
//					thread.stop();
					throw new SequentialTriggerTimeoutException("Trigger ran out of time waiting for event");
				}
				if (cycleProcessingTimeExceeded) {
					throw new SequentialTriggerTimeoutException("Trigger exceeded max cycle time");
				}
				if (currentEvent != null) {
					X event = currentEvent;
					currentEvent = null;
					this.filter = null;
					return event;
				}

				try {
					lock.wait();
				}
				// TODO: use this as a stop condition
				catch (InterruptedException e) {
					throw new SequentialTriggerTimeoutException("Trigger was interrupted", e);
				}
			}
		}
	}

	// To be called from external thread
	public void provideEvent(EventContext ctx, X event) {
		synchronized (lock) {
			// TODO: expire on wipe?
			// Also make it configurable as to whether or not a wipe ends the trigger
			if (expired.getAsBoolean()) {
//			if (event.getHappenedAt().isAfter(expiresAt)) {
				log.warn("Sequential trigger expired by event after {}/{}ms: {}", initialEvent.getEffectiveTimeSince().toMillis(), timeout, event);
				die = true;
				lock.notifyAll();
				return;
			}
			Predicate<X> filt = filter;
			if (filt != null && !filt.test(event)) {
				return;
			}
			// First, set fields
			context = ctx;
			currentEvent = event;
			// Indicate that we are currently processing
			processing = true;
			// Then, tell it to resume
			lock.notifyAll();
			// Wait for it to be done
			waitProcessingDone();
		}
	}

	private static final int defaultCycleProcessingTime = 500;
	private static final int cycleProcessingTime;

	// Workaround for integration tests exceeding cycle time
	static {
		String prop = System.getProperty("sequentialTriggerCycleTime");
		if (prop != null) {
			int value;
			try {
				value = Integer.parseInt(prop);
			}
			catch (NumberFormatException nfe) {
				value = defaultCycleProcessingTime;
			}
//			cycleProcessingTime = value;
			// TODO
			cycleProcessingTime = 30000;
		}
		else {
			cycleProcessingTime = defaultCycleProcessingTime;
		}
	}

	private void waitProcessingDone() {
		// "done" means waiting for another event
		long startTime = System.currentTimeMillis();
		int timeoutMs = cycleProcessingTime;
		long failAt = startTime + timeoutMs;
		while (processing && !done) {
			try {
				long timeLeft = failAt - System.currentTimeMillis();
				if (timeLeft <= 0) {
					String formattedStackTrace = Arrays.stream(triggerThread.getStackTrace())
							.map(element -> "\t at %s".formatted(element.toString()))
							.collect(Collectors.joining("\n"));
					log.error("Cycle processing time max ({}ms) exceeded. Trigger thread stack:\n{}", timeoutMs, formattedStackTrace);
					cycleProcessingTimeExceeded = true;
					return;
				}
				//noinspection WaitNotifyWhileNotSynced
				lock.wait(timeLeft);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

	}

	// To be called from external thread
	public boolean isDone() {
		return done || die;
	}
}