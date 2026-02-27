package gg.xp.xivsupport.events.fflogs;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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


public final class FflogsImportProcessor {
	private FflogsImportProcessor() {
	}

	public static List<Event> readFflogsJson(List<JsonNode> rootNodes) {
		boolean first = true;
		ObjectMapper mapper = new ObjectMapper();
		List<Event> out = new ArrayList<>();

		for (JsonNode rootNode : rootNodes) {

			JsonNode reportNode = rootNode.at("/reportData/report");
			JsonNode startTimeNode = reportNode.at("/startTime");
			Instant start = Instant.ofEpochMilli(mapper.convertValue(startTimeNode, Long.class));


			// We only need to look at masterData, fights, etc on the first go around
			if (first) {
				JsonNode fightNode = reportNode.at("/fights/0");
				{
					JsonNode masterNode = reportNode.at("/masterData");
					FflogsFightCombatantData fightCombatantData = mapper.convertValue(fightNode, FflogsFightCombatantData.class);
					JsonNode masterActorsNode = masterNode.at("/actors");
					List<FflogsMasterDataEvent.Actor> actors = mapper.convertValue(masterActorsNode, new TypeReference<>() {
					});

					out.add(new FflogsMasterDataEvent(actors, fightCombatantData));
				}

				{
					JsonNode zoneIdNode = fightNode.at("/gameZone/id");
					Long zoneId = mapper.convertValue(zoneIdNode, Long.class);
					JsonNode zoneNameNode = fightNode.at("/gameZone/id");
					String zoneName = mapper.convertValue(zoneNameNode, String.class);
					if (zoneId != null) {
						ZoneChangeEvent zce = new ZoneChangeEvent(new XivZone(zoneId, zoneName == null ? "" : zoneName));
						out.add(zce);
					}
				}
				{
					JsonNode mapIdNode = fightNode.at("/maps/0/id");
					Long mapId = mapper.convertValue(mapIdNode, Long.class);
					if (mapId != null) {
						MapChangeEvent mce = new MapChangeEvent(MapLibrary.forId(mapId));
						out.add(mce);
					}
				}
				first = false;
			}

			{
				JsonNode eventsNode = reportNode.at("/events/data");
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
