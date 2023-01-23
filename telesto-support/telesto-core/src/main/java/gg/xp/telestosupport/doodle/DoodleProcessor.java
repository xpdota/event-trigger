package gg.xp.telestosupport.doodle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.telestosupport.TelestoMain;

import java.util.concurrent.atomic.AtomicInteger;

public class DoodleProcessor {

	private final ObjectMapper mapper = new ObjectMapper();
	private final TelestoMain telesto;

	public DoodleProcessor(TelestoMain telesto) {
		this.telesto = telesto;
	}

	@HandleEvents(order = -1_000)
	public void drawDoodle(EventContext context, CreateDoodleRequest request) {
		DoodleSpec spec = request.getSpec();
		if (spec.name == null) {
			spec.name = nextName();
		}
		JsonNode json = mapper.valueToTree(spec);
		context.accept(telesto.makeMessage(2_000_001, "EnableDoodle", json, false));
	}

	private final short sessionId = (short) (Math.random() * 16384);
	private final AtomicInteger counter = new AtomicInteger();

	private String nextName() {
		return String.format("anon-doodle-%d-%d", sessionId, counter.getAndIncrement());
	}

}
