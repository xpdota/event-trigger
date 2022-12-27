package gg.xp.xivsupport.groovy;

import groovy.lang.Binding;
import groovy.lang.MissingPropertyException;

import java.util.LinkedHashMap;
import java.util.Map;

public class SubBinding extends Binding {

	private final Binding parent;

	public SubBinding(Binding parent) {
		this.parent = parent;
	}

	@Override
	public Object getVariable(String name) {
		if (super.hasVariable(name)) {
			return super.getVariable(name);
		}
		else {
			return parent.getVariable(name);
		}
	}

	@Override
	public boolean hasVariable(String name) {
		return super.hasVariable(name) || parent.hasVariable(name);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public Map getVariables() {
		Map out = new LinkedHashMap(parent.getVariables());
		out.putAll(super.getVariables());
		return out;
	}

	@Override
	public Object getProperty(String property) {
		try {
			return super.getProperty(property);
		}
		catch (MissingPropertyException e) {
			return parent.getProperty(property);
		}
	}
}
