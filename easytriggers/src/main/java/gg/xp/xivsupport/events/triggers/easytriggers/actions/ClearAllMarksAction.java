package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import com.fasterxml.jackson.annotation.JacksonInject;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.gui.AutoMarkGui;
import gg.xp.xivsupport.gui.tabs.GlobalUiRegistry;

public class ClearAllMarksAction implements Action<Event> {

	@Description("Configure Marks")
	public Runnable configure;

	public ClearAllMarksAction(@JacksonInject GlobalUiRegistry reg) {
		configure = () -> reg.activateItem(AutoMarkGui.class);
	}

	@Override
	public void accept(EasyTriggerContext context, Event event) {
		context.getEventContext().accept(new ClearAutoMarkRequest());
	}

	@Override
	public String fixedLabel() {
		return "Clear All Marks";
	}

	@Override
	public String dynamicLabel() {
		return "Clear All Marks";
	}
}
