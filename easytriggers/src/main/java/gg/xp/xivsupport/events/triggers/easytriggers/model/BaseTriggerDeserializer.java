package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
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
		try {
			return (BaseTrigger<?>) defaultDeserializer.deserialize(parser, ctx);
		}
		catch (Throwable t) {
			log.info("Error deserializing trigger", t);
			return new FailedDeserializationTrigger(ctx.readTree(parser), t);
		}
	}

}
