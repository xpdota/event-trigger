package gg.xp.xivsupport.callouts;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class CalloutTrackingKey implements Serializable {

	private static final AtomicLong counter = new AtomicLong();
	@Serial
	private static final long serialVersionUID = -7028940434431353410L;

	private final long key;

	public CalloutTrackingKey() {
		key = counter.getAndIncrement();
	}

	public long getKey() {
		return key;
	}

	@Override
	public String toString() {
		return "CalloutTrackingKey(%s)".formatted(key);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CalloutTrackingKey that = (CalloutTrackingKey) o;
		return getKey() == that.getKey();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getKey());
	}
}
