package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.gui.AutoMarkGui;
import gg.xp.xivsupport.gui.nav.GlobalUiRegistry;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;

public class AutoMarkTargetAction implements Action<HasTargetEntity> {

	@JsonProperty
	public MarkerSign marker = MarkerSign.ATTACK_NEXT;
	@Description("Configure Marks")
	@JsonIgnore
	public Runnable configure;

	public AutoMarkTargetAction(@JacksonInject GlobalUiRegistry reg) {
		configure = () -> reg.activateItem(AutoMarkGui.class);
	}

	@Override
	public void accept(EasyTriggerContext context, HasTargetEntity event) {
		XivCombatant target = event.getTarget();
		if (target instanceof XivPlayerCharacter xpc) {
			context.accept(new SpecificAutoMarkRequest(xpc, marker));
		}
	}

	@Override
	public String fixedLabel() {
		return "Mark Target";
	}

	@Override
	public String dynamicLabel() {
		return "Mark Target with " + marker.getFriendlyName();
	}
}
