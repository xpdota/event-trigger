package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.StringOperator;

import java.util.Locale;

public class AbilityNameFilter implements SimpleCondition<HasAbility> {

	public StringOperator operator = StringOperator.EQ;
	@Description("Use Local Language (instead of English)")
	public boolean localLanguage;
	@Description("Case Sensitive")
	public boolean caseSensitive;
	@Description("Ability Name")
	public String expected;

	@Override
	public boolean test(HasAbility hasAbility) {
		String abilityName;
		String expected = this.expected;
		if (!localLanguage) {
			ActionInfo actionInfo = ActionLibrary.forId(hasAbility.getAbility().getId());
			if (actionInfo == null) {
				return false;
			}
			abilityName = actionInfo.name();
		}
		else {
			abilityName = hasAbility.getAbility().getName();
		}
		if (!caseSensitive) {
			abilityName = abilityName.toLowerCase(Locale.ROOT);
			expected = expected.toLowerCase(Locale.ROOT);
		}
		return operator.checkString(abilityName, expected);
	}

	@Override
	public String fixedLabel() {
		return "Ability Name";
	}

	@Override
	public String dynamicLabel() {
		return "Ability Name " + operator.getFriendlyName() + ' ' + expected;
	}

	@Override
	public Class<HasAbility> getEventType() {
		return HasAbility.class;
	}
}
