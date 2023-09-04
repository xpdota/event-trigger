package gg.xp.xivsupport.events.actlines.parsers;

import java.time.Duration;

public class FakeTestingTimeSource extends FakeTimeSource {

	public void advanceBy(Duration duration) {
		setNewTime(now().plus(duration));
	}
}
