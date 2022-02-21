package gg.xp.xivdata.data;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ReadData {
	private ReadData() {
	}

	public static List<Map<String, Object>> readData() {
		try {
			// Kind of bad
			List<String> lines = Files.readAllLines(Path.of(MakeDots.class.getResource("/abilities.js").toURI()));
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
			return mapper.convertValue(tree, new TypeReference<>() {

			});
		}
		catch (URISyntaxException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
