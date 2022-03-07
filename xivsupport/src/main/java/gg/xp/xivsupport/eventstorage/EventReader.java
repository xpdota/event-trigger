package gg.xp.xivsupport.eventstorage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.fflogs.FflogsMasterDataEvent;
import gg.xp.xivsupport.events.fflogs.FflogsRawEvent;
import gg.xp.xivsupport.persistence.Compressible;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public final class EventReader {

	private EventReader() {
	}

	public static List<Event> readEventsFromResource(String resourcePath) {
		InputStream stream = EventReader.class.getResourceAsStream(resourcePath);
		return readEventsFromInputStream(stream);
	}

	public static List<Event> readEventsFromFile(File file) {
		try (InputStream stream = new FileInputStream(file)) {
			return readEventsFromInputStream(stream);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<Event> readEventsFromInputStream(InputStream stream) {
		List<Event> events;
		try (GZIPInputStream gzip = new GZIPInputStream(stream);
		     ObjectInputStream ois = new ObjectInputStream(gzip)) {
			// TODO: security
			ObjectInputFilter filter = filterInfo -> {
				if (filterInfo.serialClass() == null) {
					return ObjectInputFilter.Status.ALLOWED;
				}
				if (filterInfo.depth() != 1 || Event.class.isAssignableFrom(filterInfo.serialClass())) {
					return ObjectInputFilter.Status.ALLOWED;
				}
				else {
					return ObjectInputFilter.Status.REJECTED;
				}
			};
			ois.setObjectInputFilter(filter);
			events = new ArrayList<>();
			try {
				while (true) {
					Event event = (Event) ois.readObject();
					event.setImported(true);
					if (event instanceof Compressible) {
						((Compressible) event).decompress();
					}
					events.add(event);
				}
			}
			catch (EOFException eof) {
				// done reading
			}
		}
		catch (Throwable e) {
			throw new RuntimeException("Error reading events", e);
		}
		return events;
	}

	public static List<ACTLogLineEvent> readActLogResource(String resourcePath) {
		List<String> lines;
		try {
			lines = Files.readAllLines(Path.of(EventReader.class.getResource(resourcePath).toURI()));
		}
		catch (Throwable t) {
			throw new RuntimeException(t);
		}
		return lines.stream()
				.filter(s -> !s.isEmpty())
				.map(ACTLogLineEvent::new)
				.collect(Collectors.toList());
	}

	public static List<ACTLogLineEvent> readActLogFile(File file) {
		List<String> lines;
		try {
			lines = Files.readAllLines(file.toPath());
		}
		catch (Throwable t) {
			throw new RuntimeException(t);
		}
		return lines.stream()
				.filter(s -> !s.isEmpty())
				.map(ACTLogLineEvent::new)
				.collect(Collectors.toList());
	}

	public static List<Event> readFflogsJson(JsonNode rootNode) {
		ObjectMapper mapper = new ObjectMapper();

		JsonNode startTimeNode = rootNode.at("/reportData/report/startTime");
		Instant start = Instant.ofEpochMilli(mapper.convertValue(startTimeNode, Long.class));

		JsonNode masterNode = rootNode.at("/reportData/report/masterData");
		List<Event> out = new ArrayList<>();
		out.add(mapper.convertValue(masterNode, FflogsMasterDataEvent.class));

		JsonNode eventsNode = rootNode.at("/reportData/report/events/data");
		List<Map<String, Object>> maps = mapper.convertValue(eventsNode, new TypeReference<>() {
		});
		out.addAll(maps.stream().map(map -> {
			FflogsRawEvent raw = new FflogsRawEvent(map);
			Long timeOffset = raw.getTypedField("timestamp", Long.class);
			Instant actualTime = start.plusMillis(timeOffset);
			raw.setHappenedAt(actualTime);
			return raw;
		}).toList());
		return out;
	}
}
