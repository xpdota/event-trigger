package gg.xp.reevent.events;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public abstract class BaseEvent implements Event {

	private static final Logger log = LoggerFactory.getLogger(BaseEvent.class);
	private static final long serialVersionUID = 6147224373832437718L;
	private Event parent;
	private EventHandler<?> source;

	private Instant happenedAt = Instant.now();
	private Instant enqueuedAt;
	private Instant pumpedAt;
	private Instant pumpFinishedAt;
	private long delayedEnqueueAt;

	public void setParent(Event parent) {
		if (this.parent != null) {
			throw new IllegalStateException("Event already has a parent");
		}
		this.parent = parent;
	}

	@Override
	public @Nullable Event getParent() {
		return parent;
	}

	@Override
	public Instant getHappenedAt() {
		return happenedAt;
	}

	@Override
	public void setHappenedAt(Instant happenedAt) {
		this.happenedAt = happenedAt;
	}

	@Override
	public Instant getEnqueuedAt() {
		return enqueuedAt;
	}

	@Override
	public void setEnqueuedAt(Instant enqueuedAt) {
		if (this.enqueuedAt != null) {
			log.error("Event {} already has an enqueuedAt time!", this);
		}
		this.enqueuedAt = enqueuedAt;
	}

	@Override
	public Instant getPumpedAt() {
		return pumpedAt;
	}

	@Override
	public void setPumpedAt(Instant pumpedAt) {
		if (this.pumpedAt != null) {
			log.error("Event {} already has a pumpedAt time!", this);
		}
		this.pumpedAt = pumpedAt;
	}

	@Override
	public Instant getPumpFinishedAt() {
		return pumpedAt;
	}

	@Override
	public void setPumpFinishedAt(Instant pumpedAt) {
		if (this.pumpFinishedAt != null) {
			log.error("Event {} already has a pumpFinishedAt time!", this);
		}
		this.pumpFinishedAt = pumpedAt;
	}

	@Override
	public EventHandler<?> getSourceEventHandler() {
		return source;
	}

	@Override
	public void setSourceEventHandler(EventHandler<?> source) {
		this.source = source;
	}

	@Override
	public long delayedEnqueueAt() {
		return delayedEnqueueAt;
	}

	public void setDelayedEnqueueAt(long delayedEnqueueAt) {
		this.delayedEnqueueAt = delayedEnqueueAt;
	}

	public void setDelayedEnqueueOffset(Duration fromNow) {
		setDelayedEnqueueOffset(fromNow.toMillis());
	}

	public void setDelayedEnqueueOffset(long fromNowMillis) {
		this.delayedEnqueueAt = System.currentTimeMillis() + fromNowMillis;
	}
}
