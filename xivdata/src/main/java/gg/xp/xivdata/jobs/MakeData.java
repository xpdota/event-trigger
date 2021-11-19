package gg.xp.xivdata.jobs;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MakeData {
	private static final Logger log = LoggerFactory.getLogger(MakeData.class);

	public static void main(String[] args) throws IOException, URISyntaxException {
		// Kind of bad
		List<String> lines = Files.readAllLines(Path.of(MakeData.class.getResource("/abilities.js").toURI()));
		String wholeString = lines.stream().map(line -> {
					if (line.startsWith("var")) {
						return "[";
					}
//				.replaceAll(" //(.*)", "")
					return line
//							.replaceAll("order: (\\d+),", "order: $1")
//							.replaceFirst("([a-zA-Z].+): ", "\"$1\": ")
							;
				}
		).collect(Collectors.joining("\n"));


		ObjectMapper mapper = JsonMapper.builder()
				.enable(JsonParser.Feature.ALLOW_COMMENTS)
				.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
				.enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
				.build();

		JsonNode tree = mapper.readTree(wholeString);
		List<Map<String, Object>> maps = mapper.convertValue(tree, new TypeReference<>() {

		});

		List<Map<String, Object>> out = maps.stream()
				.filter(m -> m.get("type").equals("DoT"))
				.filter(m -> m.get("ttstype").equals(1))
				.sorted(Comparator.comparing(d -> (Integer) d.get("id")))
				.collect(Collectors.toList());

		log.info("Number of dots: {}", out.size());

		List<DotData> data = new ArrayList<>();

		out.forEach(item -> {
			String name = item.get("name").toString();
			String[] split = name.split(" ", 2);
			String nameStub = split[0];
			int id = ((Integer) item.get("id"));
			Job job = Job.valueOf(item.get("job").toString());
			DotData existing = data.stream()
					.filter(d -> d.firstName.equalsIgnoreCase(nameStub) && d.job == job)
					.findAny()
					.orElse(null);
			if (existing == null) {
				DotData newData = new DotData();
				newData.firstName = name;
				newData.lastName = name;
				newData.job = job;
				newData.names.add(name);
				newData.ids.add(id);
				data.add(newData);
			}
			else {
				String nameRest = split[1];
				existing.lastName = name;
				existing.names.add(nameRest);
				existing.ids.add(id);
			}
		});

		data.sort(Comparator.comparing(DotData::getEnumName));
		data.forEach(d -> {
			System.out.printf("%s(%s, \"%s\", %s),%n", d.getEnumName(), d.job, d.getLabel(), d.getIds());
		});


	}

	private static class DotData {
		private String firstName;
		private String lastName;
		private Job job;
		private final List<String> names = new ArrayList<>();
		private final List<Integer> ids = new ArrayList<>();

		// Using first name as enum ID
		String getEnumName() {
			return job.name() + '_' + firstName.replaceAll("[^A-Za-z]", "");
		}

		String getLabel() {
			return String.join("/", names);
		}

		String getIds() {
			return ids.stream()
					.map(i -> "0x" + Integer.toString(i, 16) + 'L')
					.collect(Collectors.joining(", "));
		}

	}
}
