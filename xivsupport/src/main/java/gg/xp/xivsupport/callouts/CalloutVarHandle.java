package gg.xp.xivsupport.callouts;

import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;

import java.lang.reflect.Field;

public class CalloutVarHandle {
	private final StringSetting valueSettingTts;
	private final StringSetting valueSettingText;
	// TODO not supported yet
	private final BooleanSetting isScriptSetting;
	private final BooleanSetting sameText;

	private final CalloutVar original;

	CalloutVarHandle(PersistenceProvider pers, String propStub, CalloutVar original) {
		this.original = original;
		this.valueSettingTts = new StringSetting(pers, propStub + ".value-tts", original.getDefaultValue());
		this.valueSettingText = new StringSetting(pers, propStub + ".value-text", original.getDefaultValue());
		this.sameText = new BooleanSetting(pers, propStub + ".same-text", true);
		this.isScriptSetting = new BooleanSetting(pers, propStub + ".is-script", false);
	}

	public static CalloutVarHandle installHandle(Field f, CalloutVar original, PersistenceProvider persistence, String fullpropStub) {
		CalloutVarHandle v = new CalloutVarHandle(persistence, fullpropStub, original);
		original.attachHandle(v);
		return v;
	}

	/**
	 * @return The effective TTS value.
	 */
	public Object getValueTts() {
		if (isScriptSetting.get()) {
			return "Script vars are not supported yet";
		}
		return valueSettingTts.get();
	}

	/**
	 * @return The effective text value.
	 */
	public Object getValueText() {
		if (sameText.get()) {
			return getValueTts();
		}
		if (isScriptSetting.get()) {
			return "Script vars are not supported yet";
		}
		return valueSettingText.get();
	}

	/**
	 * @return Get the setting that controls the TTS value.
	 */
	public StringSetting getValueSettingTts() {
		return valueSettingTts;
	}

	/**
	 * @return Get the setting that controls the Text value.
	 */
	public StringSetting getValueSettingText() {
		return valueSettingText;
	}

	/**
	 * @return The setting that controls whether to use the same value for text as for TTS.
	 */
	public BooleanSetting getSameText() {
		return sameText;
	}

	/**
	 * @return The original CalloutVar instance that this handle is attached to.
	 */
	public CalloutVar getOriginal() {
		return original;
	}
}
