package gg.xp.xivsupport.events;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class CloseTo extends TypeSafeMatcher<Long> {

	private final long value;
	private final long error;

	public CloseTo(long value, long error) {
		this.value = value;
		this.error = error;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("a numeric value within ").appendValue(error).appendText(" of ").appendValue(value);
	}

	@Override
	protected boolean matchesSafely(Long aLong) {
		return Math.abs(aLong - value) <= error;
	}
}
