package gg.xp.xivsupport.events.fflogs;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.actlines.parsers.FakeFflogsTimeSource;
import gg.xp.xivsupport.eventstorage.EventReader;
import gg.xp.xivsupport.gui.imprt.ListEventIterator;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FflogsImportTest {

	@Test
	public void testLoadsPlaysAndCaptures() {
		// Read the test data
		InputStream resource = FflogsImportTest.class.getResourceAsStream("/test_fflogs_import.json");
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(resource);
		if (root.has("data")) {
			root = root.get("data");
		}
		List<Event> input = EventReader.readFflogsJson(Collections.singletonList(root));

		// Standard init but set PLS to fflogs
		MutablePicoContainer pico = XivMain.importInit();
		EventDistributor dist = pico.getComponent(EventDistributor.class);
		EventMaster master = pico.getComponent(EventMaster.class);
		pico.addComponent(FakeFflogsTimeSource.class);
		pico.getComponent(PrimaryLogSource.class).setLogSource(KnownLogSource.FFLOGS);

		// Track FflogsRawEvents and their children to ensure that every raw event gets mapped to at least one
		// "parsed" event.
		Map<FflogsRawEvent, List<Event>> rawToChildren = new IdentityHashMap<>();
		dist.registerHandler(Event.class, (context, event) -> {
			// If it's a direct fflogs event, initialize it to an empty list
			if (event instanceof FflogsRawEvent raw) {
				rawToChildren.put(raw, new ArrayList<>());
			}
			// If it's a child of a fflogs event, add it to the list of children for that event
			else if (event.getParent() instanceof FflogsRawEvent parent) {
				rawToChildren.get(parent).add(event);
			}
		});

		// Play the events via a real ReplayController
		dist.acceptEvent(new InitEvent());
		ReplayController replay = new ReplayController(master, new ListEventIterator<>(input), false);
		pico.addComponent(replay);
		replay.advanceBy(Integer.MAX_VALUE);

		Assert.assertFalse(rawToChildren.isEmpty(), "rawToChildren map is empty");

		// New requirement: Every FflogsRawEvent must have at least one child event
		List<FflogsRawEvent> missingChildren = rawToChildren.entrySet().stream()
				.filter(entry -> entry.getValue().isEmpty())
				.map(Map.Entry::getKey)
				.toList();

		if (!missingChildren.isEmpty()) {
			String details = missingChildren.stream()
					.limit(10)
					.map(raw -> "%s at %d (source:%d)".formatted(raw.type(), raw.timestamp(), raw.sourceID()))
					.collect(Collectors.joining(", "));
			Assert.fail("Some FflogsRawEvents did not produce any child events (%d missing). Examples: [%s]"
					.formatted(missingChildren.size(), details));
		}
	}
}
