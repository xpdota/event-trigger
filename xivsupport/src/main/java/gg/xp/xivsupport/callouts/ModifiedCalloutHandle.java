package gg.xp.xivsupport.callouts;

import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.LongSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;
import org.jetbrains.annotations.Nullable;

public final class ModifiedCalloutHandle {

	private final StringSetting ttsSetting;
	private final StringSetting textSetting;
	private final LongSetting hangTimeSetting;
	private final ModifiableCallout original;

	public ModifiedCalloutHandle(PersistenceProvider persistenceProvider, String propStub, ModifiableCallout original) {
		ttsSetting = new StringSetting(persistenceProvider, propStub + ".tts", original.getOriginalTts());
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

	public LongSetting getHangTimeSetting() {
		return hangTimeSetting;
	}

	public String getDescription() {
		return original.getDescription();
	}
}
