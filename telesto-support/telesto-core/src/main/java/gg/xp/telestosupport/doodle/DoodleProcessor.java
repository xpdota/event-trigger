package gg.xp.telestosupport.doodle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.telestosupport.TelestoMain;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import java.util.concurrent.atomic.AtomicInteger;

public class DoodleProcessor implements FilteredEventHandler {

	private final ObjectMapper mapper = new ObjectMapper();
	private final TelestoMain telesto;
	private final BooleanSetting doodleSetting;

	public DoodleProcessor(TelestoMain telesto, PersistenceProvider pers) {
		this.telesto = telesto;
		doodleSetting = new BooleanSetting(pers, "telesto-support.doodle-support.enable", false);
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

	public BooleanSetting enableDoodles() {
		return doodleSetting;
	}

	private final short sessionId = (short) (Math.random() * 16384);
	private final AtomicInteger counter = new AtomicInteger();

	private String nextName() {
		return String.format("anon-doodle-%d-%d", sessionId, counter.getAndIncrement());
	}

	@Override
	public boolean enabled(EventContext context) {
		return doodleSetting.get();
	}
}
