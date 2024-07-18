package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;
import gg.xp.xivsupport.speech.ProcessedCalloutEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Supplier;

public class DutyCalloutFilter implements Condition<ProcessedCalloutEvent> {

	@Description("Class")
	public String calloutClass;
	@Description("Field")
	public String calloutField;

	@Override
	public @Nullable String fixedLabel() {
		return "Callout came from specific duty trigger";
	}

	@Override
	public String dynamicLabel() {
		return "Callout came from %s:%s".formatted(calloutClass, calloutField);
	}

	@Override
	public boolean test(EasyTriggerContext ctx, ProcessedCalloutEvent event) {
		if (event.getParent() instanceof RawModifiedCallout<?> raw) {
			ModifiedCalloutHandle handle = raw.getHandle();
			if (handle == null) {
				return false;
			}
			Field field = handle.getField();
			if (field == null) {
				return false;
			}
			if (Objects.equals(field.getDeclaringClass().getSimpleName(), calloutClass)
			    && Objects.equals(field.getName(), calloutField)) {
				Event originalEvent = raw.getParent();
				ctx.addVariable("originalEvent", originalEvent);
				ctx.addVariable("originalParams", raw.getArguments());
				ctx.addVariable("originalTts", event.getCallText());
				ctx.addVariable("originalText", (Supplier<String>) event::getVisualText);
				return true;
			}
		}
		return false;
	}
}
