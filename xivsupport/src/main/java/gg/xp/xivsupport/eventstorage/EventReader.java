package gg.xp.xivsupport.eventstorage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.parsers.ActLineParseException;
import gg.xp.xivsupport.events.fflogs.FflogsMasterDataEvent;
import gg.xp.xivsupport.events.fflogs.FflogsRawEvent;
import gg.xp.xivsupport.gui.imprt.EventIterator;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.persistence.Compressible;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public final class EventReader {

	private static final Logger log = LoggerFactory.getLogger(EventReader.class);

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
			// Security - only allow event subtypes
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
			while (true) {
				Event event;
				try {
					event = (Event) ois.readObject();
				}
				catch (EOFException eof) {
					// done reading
					break;
				}
//				catch (WriteAbortedException t) {
//					log.error("Error deserializing event", t);
//					continue;
//				}
				event.setImported(true);
				if (event instanceof Compressible compressible) {
					compressible.decompress();
				}
				events.add(event);
			}
		}
		catch (Throwable e) {
			throw new RuntimeException("Error reading events", e);
		}
		return events;
	}

	public static EventIterator<ACTLogLineEvent> readActLogResource(String resourcePath) {
		List<String> lines;
		try {
			InputStream resource = EventReader.class.getResourceAsStream(resourcePath);
			if (resource == null) {
				throw new IllegalArgumentException("The resource '%s' does not exist".formatted(resourcePath));
			}
			lines = IOUtils.readLines(resource, StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		// TODO: this uses the old tech but the builtin files are small
		return makeIteratorFromRawLines(lines.stream());
	}

	public static EventIterator<ACTLogLineEvent> readActLogFile(File file) {
		Stream<String> lines;
		try {
			lines = Files.lines(file.toPath());
		}
		catch (Throwable t) {
			throw new RuntimeException(t);
		}
		return makeIteratorFromRawLines(lines);
	}

	@NotNull
	private static EventIterator<ACTLogLineEvent> makeIteratorFromRawLines(Stream<String> lines) {
		AtomicInteger ai = new AtomicInteger(1);
		Iterator<ACTLogLineEvent> iterator = lines
				.sequential()
				.filter(s -> !s.isEmpty())
				.map(logLine -> {
					try {
						return new ACTLogLineEvent(logLine, ai.getAndIncrement());
					}
					catch (Throwable t) {
						ActLineParseException exc = new ActLineParseException(logLine, t);
						log.error("Error parsing log line", exc);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.iterator();
		return new EventIterator<ACTLogLineEvent>() {
			private volatile boolean hasNext = iterator.hasNext();
			private volatile Event prev;

			@Override
			public boolean hasMore() {
				return hasNext;
			}

			@Override
			public @Nullable ACTLogLineEvent getNext() {
				ACTLogLineEvent next = iterator.next();
				if (prev instanceof ACTLogLineEvent prevAct) {
					if (next.getLineNum() != prevAct.getLineNum() + 1) {
						log.error("Bad line num!");
					}
				}
				prev = next;
				hasNext = iterator.hasNext();
				return next;
			}
		};
	}

	public static List<Event> readFflogsJson(List<JsonNode> rootNodes) {
		boolean first = true;
		ObjectMapper mapper = new ObjectMapper();
		List<Event> out = new ArrayList<>();

		for (JsonNode rootNode : rootNodes) {

			JsonNode startTimeNode = rootNode.at("/reportData/report/startTime");
			Instant start = Instant.ofEpochMilli(mapper.convertValue(startTimeNode, Long.class));


			if (first) {
				{
					JsonNode masterNode = rootNode.at("/reportData/report/masterData");
					out.add(mapper.convertValue(masterNode, FflogsMasterDataEvent.class));
				}

				{
					JsonNode zoneIdNode = rootNode.at("/reportData/report/fights/0/gameZone/id");
					Long zoneId = mapper.convertValue(zoneIdNode, Long.class);
					JsonNode zoneNameNode = rootNode.at("/reportData/report/fights/0/gameZone/id");
					String zoneName = mapper.convertValue(zoneNameNode, String.class);
					if (zoneId != null) {
						ZoneChangeEvent zce = new ZoneChangeEvent(new XivZone(zoneId, zoneName == null ? "" : zoneName));
						out.add(zce);
					}
				}
				{
					JsonNode mapIdNode = rootNode.at("/reportData/report/fights/0/maps/0/id");
					Long mapId = mapper.convertValue(mapIdNode, Long.class);
					if (mapId != null) {
						MapChangeEvent mce = new MapChangeEvent(MapLibrary.forId(mapId));
						out.add(mce);
					}
				}
				first = false;
			}

			{
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
			}
		}
		return out;
	}
}
