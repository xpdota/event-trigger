package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

public class CooldownSetting {
	private final BooleanSetting overlay;
	private final BooleanSetting ttsOnUse;
	private final BooleanSetting ttsWhenReady;

	public CooldownSetting(PersistenceProvider persistence, String settingKeyBase, boolean defaultOverlay, boolean defaultTts) {
		BooleanSetting legacySetting = new BooleanSetting(persistence, settingKeyBase, defaultOverlay);
		this.overlay = new BooleanSetting(persistence, settingKeyBase + ".overlay", defaultOverlay);
		this.ttsWhenReady = new BooleanSetting(persistence, settingKeyBase + ".tts", defaultTts);
		this.ttsOnUse = new BooleanSetting(persistence, settingKeyBase + ".tts-on-use", false);
		if (legacySetting.isSet() && !overlay.isSet()) {
			overlay.set(legacySetting.get());
		}
		if (legacySetting.isSet() && !ttsWhenReady.isSet()) {
			ttsWhenReady.set(legacySetting.get());
		}
	}

	public BooleanSetting getOverlay() {
		return overlay;
	}

	public BooleanSetting getTtsOnUse() {
		return ttsOnUse;
	}

	public BooleanSetting getTtsReady() {
		return ttsWhenReady;
	}
}
