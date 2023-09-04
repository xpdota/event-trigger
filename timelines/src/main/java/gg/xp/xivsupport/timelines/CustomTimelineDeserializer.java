package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

public class CustomTimelineDeserializer extends JsonDeserializer<CustomTimelineItem> {
	@Override
	public CustomTimelineItem deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
		ObjectMapper codec = (ObjectMapper) jsonParser.getCodec();
		ObjectNode root = codec.readTree(jsonParser);
		JsonNode labelField = root.get("label");
		if (labelField != null && labelField.isBoolean() && labelField.booleanValue()) {
			// Is label
			return codec.convertValue(root, CustomTimelineLabel.class);
		}
		else {
			return codec.convertValue(root, CustomTimelineEntry.class);

		}
	}
}
