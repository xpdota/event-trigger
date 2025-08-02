package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;

public class BaseTriggerDeserializer extends StdDeserializer<BaseTrigger<?>> {

	private static final Logger log = LoggerFactory.getLogger(BaseTriggerDeserializer.class);
	@Serial
	private static final long serialVersionUID = -8113670702348928653L;

	private final JsonDeserializer<?> defaultDeserializer;

	public BaseTriggerDeserializer(JsonDeserializer<?> defaultDeserializer) {
		super(BaseTrigger.class);
		this.defaultDeserializer = defaultDeserializer;
	}

	@Override
	public BaseTrigger<?> deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JacksonException {
		JsonNode node = parser.readValueAsTree();
		try {
			// Try to use the standard deserialization process
			ObjectMapper mapper = (ObjectMapper) parser.getCodec();
			// Create a new parser from the node to avoid parser state issues
			JsonParser newParser = mapper.treeAsTokens(node);
			// Move to the first token
			newParser.nextToken();

			JavaType valueType = getValueType(ctx);
			BeanDescription desc = ctx.getConfig().introspect(valueType);
			JsonDeserializer<Object> deser = ctx.getFactory().createBeanDeserializer(ctx, valueType, desc);

			// Use the standard deserialization process
			if (defaultDeserializer instanceof BeanDeserializer bd) {
				BaseTrigger<?> result = (BaseTrigger<?>) deser.deserialize(newParser, ctx);
				return result;
			}
			else {
				BaseTrigger<?> result = (BaseTrigger<?>) defaultDeserializer.deserialize(newParser, ctx);
				return result;
			}
		}
		catch (StackOverflowError t) {
			// Plugging a StackOverflow into the logger can cause yet another stack overflow, so just toString it.
			log.error("Error deserializing trigger: {}", t.toString());
			// Return a FailedDeserializationTrigger with the original JSON and error
			return new FailedDeserializationTrigger(node, t);
		}
		catch (Throwable t) {
			log.error("Error deserializing trigger", t);
			// Return a FailedDeserializationTrigger with the original JSON and error
			return new FailedDeserializationTrigger(node, t);
		}
	}

}
