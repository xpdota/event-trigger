package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.OptBoolean;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;
import gg.xp.xivsupport.models.XivCombatant;

public class SourcePartyMemberFilter implements SimpleCondition<HasSourceEntity> {

	@Description("Invert (not in party)")
	public boolean invert;
	@EditorIgnore
	private final XivState state;

	public SourcePartyMemberFilter(@JacksonInject(useInput = OptBoolean.FALSE) XivState state) {
		this.state = state;
	}

	@Override
	public String fixedLabel() {
		return "Source is party member";
	}

	@Override
	public String dynamicLabel() {
		return String.format("Source %s in your party", invert ? "is not" : "is");
	}

	@Override
	public boolean test(HasSourceEntity hasSourceEntity) {
		XivCombatant cbt = hasSourceEntity.getSource().walkParentChain();
		//noinspection SuspiciousMethodCalls
		boolean isInParty = state.getPartyList().contains(cbt);
		return isInParty != invert;
	}
}
