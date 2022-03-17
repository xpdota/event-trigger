package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;

import java.util.function.Predicate;
import java.util.regex.Pattern;

// TODO
// TODO: on second thought, DON'T implement this until we have a SecurityManager for groovy stuff, too much malicious
// potential.
public class GroovyEventFilter implements Condition<Object> {

	public Predicate<Object> groovyScript = (obj) -> true;
	@Description("Regex")
	public Pattern regex = Pattern.compile("^Regex Here$");

	@Override
	public String fixedLabel() {
		return "Groovy Filter";
	}

	@Override
	public String dynamicLabel() {
		return "(Groovy Expression)";
	}

	@Override
	public boolean test(Object event) {
		return groovyScript.test(event);
	}

}
