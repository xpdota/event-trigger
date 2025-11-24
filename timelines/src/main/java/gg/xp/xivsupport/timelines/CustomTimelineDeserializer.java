package gg.xp.xivsupport.timelines;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.node.ObjectNode;

public class CustomTimelineDeserializer extends ValueDeserializer<CustomTimelineItem> {
	@Override
	public CustomTimelineItem deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
		ObjectReadContext orc = jsonParser.objectReadContext();
		ObjectNode root = orc.readTree(jsonParser);

		JsonNode labelField = root.get("label");
		try (JsonParser jp = deserializationContext.treeAsTokens(root)) {
			if (labelField != null && labelField.isBoolean() && labelField.booleanValue()) {
				// Is label
				return orc.readValue(jp, CustomTimelineLabel.class);
			}
			else {
				return orc.readValue(jp, CustomTimelineEntry.class);
			}
		}
	}
}
