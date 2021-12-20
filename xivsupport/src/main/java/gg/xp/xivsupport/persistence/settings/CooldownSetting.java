package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

public class CooldownSetting {
	private final BooleanSetting overlay;
	private final BooleanSetting tts;

	public CooldownSetting(PersistenceProvider persistence, String settingKeyBase, boolean defaultOverlay, boolean defaultTts) {
		BooleanSetting legacySetting = new BooleanSetting(persistence, settingKeyBase, defaultOverlay);
		this.overlay = new BooleanSetting(persistence, settingKeyBase + ".overlay", defaultOverlay);
		this.tts = new BooleanSetting(persistence, settingKeyBase + ".tts", defaultTts);
		if (legacySetting.isSet() && !overlay.isSet()) {
			overlay.set(legacySetting.get());
		}
		if (legacySetting.isSet() && !tts.isSet()) {
			tts.set(legacySetting.get());
		}
	}

	public BooleanSetting getOverlay() {
		return overlay;
	}

	public BooleanSetting getTts() {
		return tts;
	}

	public boolean shouldTrack() {
		return overlay.get() || tts.get();
	}
}
