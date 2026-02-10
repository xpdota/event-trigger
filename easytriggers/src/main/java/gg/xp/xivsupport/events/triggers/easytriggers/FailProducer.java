package gg.xp.xivsupport.events.triggers.easytriggers;

import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JsonNode;

@FunctionalInterface
public interface FailProducer<F> {
	F makeFail(JsonNode node, @Nullable Throwable failure);
}
