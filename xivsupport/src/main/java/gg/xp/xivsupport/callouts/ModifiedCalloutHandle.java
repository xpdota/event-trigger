package gg.xp.xivsupport.callouts;

import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.LongSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;
import org.jetbrains.annotations.Nullable;

public final class ModifiedCalloutHandle {

	private final BooleanSetting enable;
	private final BooleanSetting enableTts;
	private final StringSetting ttsSetting;
	private final BooleanSetting enableText;
	private final StringSetting textSetting;
	private final LongSetting hangTimeSetting;
	private final ModifiableCallout original;
	private boolean isEnabledByParent = true;

	public ModifiedCalloutHandle(PersistenceProvider persistenceProvider, String propStub, ModifiableCallout original) {
		enable = new BooleanSetting(persistenceProvider, propStub + ".enabled", true);
		enableTts = new BooleanSetting(persistenceProvider, propStub + ".tts-enabled", true);
		ttsSetting = new StringSetting(persistenceProvider, propStub + ".tts", original.getOriginalTts());
		enableText = new BooleanSetting(persistenceProvider, propStub + ".text-enabled", true);
		textSetting = new StringSetting(persistenceProvider, propStub + ".text", original.getOriginalVisualText());
		// TODO 5000
		hangTimeSetting = new LongSetting(persistenceProvider, propStub + ".text.hangtime", 5000L);
		this.original = original;
	}

	// TODO: enable/disable
	public StringSetting getTtsSetting() {
		return ttsSetting;
	}

	public StringSetting getTextSetting() {
		return textSetting;
	}

	public BooleanSetting getEnable() {
		return enable;
	}

	public BooleanSetting getEnableTts() {
		return enableTts;
	}

	public BooleanSetting getEnableText() {
		return enableText;
	}

	public LongSetting getHangTimeSetting() {
		return hangTimeSetting;
	}

	public String getDescription() {
		return original.getDescription();
	}

	public void setEnabledByParent(boolean enabledByParent) {
		isEnabledByParent = enabledByParent;
	}

	public boolean isEffectivelyEnabled() {
		return getEnable().get() && isEnabledByParent;
	}
}
