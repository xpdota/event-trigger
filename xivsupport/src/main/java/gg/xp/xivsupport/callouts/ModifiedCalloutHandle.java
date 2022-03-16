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
	private final BooleanSetting sameText;
	private final StringSetting textSetting;
	private final LongSetting hangTimeSetting;
	private final ModifiableCallout<?> original;
	private final @Nullable BooleanSetting allTts;
	private final @Nullable BooleanSetting allText;
	private boolean isEnabledByParent = true;

	public ModifiedCalloutHandle(PersistenceProvider persistenceProvider, String propStub, ModifiableCallout<?> original, @Nullable BooleanSetting allTts, @Nullable BooleanSetting allText) {
		this.allTts = allTts;
		this.allText = allText;
		enable = new BooleanSetting(persistenceProvider, propStub + ".enabled", true);
		enableTts = new BooleanSetting(persistenceProvider, propStub + ".tts-enabled", true);
		ttsSetting = new StringSetting(persistenceProvider, propStub + ".tts", original.getOriginalTts());
		enableText = new BooleanSetting(persistenceProvider, propStub + ".text-enabled", true);
		textSetting = new StringSetting(persistenceProvider, propStub + ".text", original.getOriginalVisualText());
		sameText = new BooleanSetting(persistenceProvider, propStub + ".text-same", false);
		// TODO: this was a legacy compat hack, not necessary anymore
		sameText.set(sameText.get());
		hangTimeSetting = new LongSetting(persistenceProvider, propStub + ".text.hangtime", 5000L);
		this.original = original;
	}

	@SuppressWarnings("UnusedReturnValue")
	public static ModifiedCalloutHandle installHandle(ModifiableCallout<?> original, PersistenceProvider pers, String propStub) {
		return installHandle(original, pers, propStub, null, null);
	}
	public static ModifiedCalloutHandle installHandle(ModifiableCallout<?> original, PersistenceProvider pers, String propStub, @Nullable BooleanSetting allTts, @Nullable BooleanSetting allText) {
		ModifiedCalloutHandle modified = new ModifiedCalloutHandle(pers, propStub, original, allTts, allText);
		original.attachHandle(modified);
		return modified;
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

	public BooleanSetting getSameText() {
		return sameText;
	}

	public LongSetting getHangTimeSetting() {
		return hangTimeSetting;
	}

	public @Nullable String getEffectiveTts() {
		if (isTtsEffectivelyEnabled()) {
			return ttsSetting.get();
		}
		else {
			return null;
		}
	}

	public @Nullable String getEffectiveText() {
		if (isTextEffectivelyEnabled()) {
			if (sameText.get()) {
				return ttsSetting.get();
			}
			else {
				return textSetting.get();
			}
		}
		else {
			return null;
		}
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

	public boolean isTtsEffectivelyEnabled() {
		return isEffectivelyEnabled() && (allTts == null || allTts.get()) && getEnableTts().get();
	}

	public boolean isTextEffectivelyEnabled() {
		return isEffectivelyEnabled() && (allText == null || allText.get()) && getEnableText().get();
	}

	public BooleanSetting getAllTextEnabled() {
		return allText;
	}

	public BooleanSetting getAllTtsEnabled() {
		return allTts;
	}
}
