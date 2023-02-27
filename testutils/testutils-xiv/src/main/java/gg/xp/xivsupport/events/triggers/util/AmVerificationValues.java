package gg.xp.xivsupport.events.triggers.util;

import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;

import java.util.Objects;

public final class AmVerificationValues implements HasEvent {
	private final long when;
	private final MarkerSign marker;
	private final Job targetJob;
	private final Event event;

	public AmVerificationValues(
			long when, MarkerSign marker,
			Job targetJob,
			Event event) {
		this.when = when;
		this.marker = marker;
		this.targetJob = targetJob;
		this.event = event;
	}

	@Override
	public Event event() {
		return event;
	}

	public long when() {
		return when;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AmVerificationValues that = (AmVerificationValues) o;
		return when == that.when && marker == that.marker && targetJob == that.targetJob;
	}

	@Override
	public int hashCode() {
		return Objects.hash(when, marker, targetJob);
	}

	@Override
	public String toString() {
		if (targetJob == Job.ADV) {
			if (event == null) {
				return String.format("clearAll(%s)", when);
			}
			else {
				return String.format("clearAll(%s) // from %s", when, event);
			}
		}
		else if (event == null) {
			return String.format("mark(%s, %s, %s)", when, marker, targetJob);
		}
		else {
			return String.format("mark(%s, %s, %s) // from %s", when, marker, targetJob, event);
		}
	}
}
