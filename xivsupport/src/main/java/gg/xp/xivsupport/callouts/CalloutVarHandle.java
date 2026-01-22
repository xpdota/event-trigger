package gg.xp.xivsupport.callouts;

import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;

import java.lang.reflect.Field;

public class CalloutVarHandle {
	private final StringSetting valueSetting;
	// TODO not supported yet
	private final BooleanSetting isScriptSetting;

	private final CalloutVar original;

	CalloutVarHandle(PersistenceProvider pers, String propStub, CalloutVar original) {
		this.original = original;
		this.valueSetting = new StringSetting(pers, propStub + ".value", original.getDefaultValue());
		this.isScriptSetting = new BooleanSetting(pers, propStub + ".is-script", false);
	}

	public static CalloutVarHandle installHandle(Field f, CalloutVar original, PersistenceProvider persistence, String fullpropStub) {
		CalloutVarHandle v = new CalloutVarHandle(persistence, fullpropStub, original);
		original.attachHandle(v);
		return v;
	}

	public Object getValue() {
		if (isScriptSetting.get()) {
			return "Script vars are not supported yet";
		}
		return valueSetting.get();
	}
}
