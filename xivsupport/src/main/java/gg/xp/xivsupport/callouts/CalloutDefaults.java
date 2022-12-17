package gg.xp.xivsupport.callouts;

import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.ObservableMutableBoolean;
import gg.xp.xivsupport.persistence.settings.ParentedBooleanSetting;

public final class CalloutDefaults {
	private final BooleanSetting enableCallout;
	private final BooleanSetting enableTts;
	private final BooleanSetting enableText;

	public static CalloutDefaults withParent(PersistenceProvider pers, String settingKeyBase, CalloutDefaults parent) {
		ParentedBooleanSetting enableCallout = new ParentedBooleanSetting(pers, settingKeyBase + ".default-enabled", parent.getEnableCallout());
		ParentedBooleanSetting ttsSetting = new ParentedBooleanSetting(pers, settingKeyBase + ".tts-default-enabled", parent.getEnableTts());
		ParentedBooleanSetting textSetting = new ParentedBooleanSetting(pers, settingKeyBase + ".text-default-enabled", parent.getEnableText());
		return new CalloutDefaults(enableCallout, ttsSetting, textSetting);
	}

	public static CalloutDefaults noParent(PersistenceProvider pers, String settingKeyBase) {
		BooleanSetting enableCallout = new BooleanSetting(pers, settingKeyBase + ".default-enabled", true);
		BooleanSetting ttsSetting = new BooleanSetting(pers, settingKeyBase + ".tts-default-enabled", true);
		BooleanSetting textSetting = new BooleanSetting(pers, settingKeyBase + ".text-default-enabled", true);
		return new CalloutDefaults(enableCallout, ttsSetting, textSetting);
	}

	public static CalloutDefaults dummy() {
		return noParent(new InMemoryMapPersistenceProvider(), "dummy");
	}

	private CalloutDefaults(BooleanSetting enableCallout, BooleanSetting enableTts, BooleanSetting enableText) {
		this.enableCallout = enableCallout;
		this.enableTts = enableTts;
		this.enableText = enableText;
	}

	public BooleanSetting getEnableCallout() {
		return enableCallout;
	}

	public BooleanSetting getEnableTts() {
		return enableTts;
	}

	public BooleanSetting getEnableText() {
		return enableText;
	}
}
