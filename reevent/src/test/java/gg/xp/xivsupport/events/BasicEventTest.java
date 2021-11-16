package gg.xp.xivsupport.events;

import gg.xp.reevent.context.BasicStateStore;
import gg.xp.reevent.events.BasicEvent;
import gg.xp.reevent.events.BasicEventDistributor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class BasicEventTest {

	@Test
	void testBasicEvent() {
		BasicEventDistributor queue = new BasicEventDistributor(new BasicStateStore());
		List<String> seen = new ArrayList<>();
		queue.registerHandler(BasicEvent.class, (q, event) -> {
			String value = event.getValue();
			seen.add(value);
			if (value.equals("foo")) {
				q.accept(new BasicEvent("bar"));
				q.accept(new BasicEvent("baz"));
			}
		});
		queue.acceptEvent(new BasicEvent("foo"));
		Assert.assertEquals(seen, List.of("foo", "bar", "baz"));
	}
}
