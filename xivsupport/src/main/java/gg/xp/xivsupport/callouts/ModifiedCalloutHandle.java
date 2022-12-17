package gg.xp.xivsupport.callouts;

import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.ColorSetting;
import gg.xp.xivsupport.persistence.settings.LongSetting;
import gg.xp.xivsupport.persistence.settings.ParentedBooleanSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class ModifiedCalloutHandle {

	private final BooleanSetting enable;
	private final BooleanSetting enableTts;
	private final StringSetting ttsSetting;
	private final BooleanSetting enableText;
	private final BooleanSetting sameText;
	private final StringSetting textSetting;
	private final StringSetting soundSetting;
	private final LongSetting hangTimeSetting;
	private final ModifiableCallout<?> original;
	private final @Nullable BooleanSetting allTts;
	private final @Nullable BooleanSetting allText;
	private final ColorSetting textColorOverride;
	private boolean isEnabledByParent = true;

	// Only used for testing
	ModifiedCalloutHandle(PersistenceProvider persistenceProvider, String propStub, ModifiableCallout<?> original, @Nullable BooleanSetting allTts, @Nullable BooleanSetting allText) {
		this(persistenceProvider, propStub, original, allTts, allText, CalloutDefaults.dummy());
	}

	public ModifiedCalloutHandle(PersistenceProvider persistenceProvider, String propStub, ModifiableCallout<?> original, @Nullable BooleanSetting allTts, @Nullable BooleanSetting allText, CalloutDefaults defaults) {
		this.allTts = allTts;
		this.allText = allText;
		boolean enabledByDefault = original.isEnabledByDefault();
		if (enabledByDefault) {
			enable = new ParentedBooleanSetting(persistenceProvider, propStub + ".enabled", defaults.getEnableCallout());
		}
		else {
			enable = new BooleanSetting(persistenceProvider, propStub + ".enabled", false);
		}
		enableTts = new ParentedBooleanSetting(persistenceProvider, propStub + ".tts-enabled", defaults.getEnableTts());
		ttsSetting = new StringSetting(persistenceProvider, propStub + ".tts", original.getOriginalTts());
		enableText = new ParentedBooleanSetting(persistenceProvider, propStub + ".text-enabled", defaults.getEnableText());
		textSetting = new StringSetting(persistenceProvider, propStub + ".text", original.getOriginalVisualText());
		soundSetting = new StringSetting(persistenceProvider, propStub + ".sound", "");
		sameText = new BooleanSetting(persistenceProvider, propStub + ".text-same", false);
		// Logic for defaulting the "same as TTS" setting:
		// If sameText is already set (regardless of the value it is set to, do nothing)
		// Disabling this because it causes a lot more problems than it solves
//		if (!sameText.isSet()) {
//			// If the settings are identical, set it
//			// TODO: needs to be fixed
//			// If a callout is initially the same, and the user DOES NOT CUSTOMIZE, but the underlying call changes,
//			// the same setting will still be on in the current impl.
//			if (Objects.equals(ttsSetting.get(), textSetting.get())) {
//				sameText.set(true);
//			}
//			// Note that this will apply even if the tts/text have been customized. That is, if, down the line, you
//			// happen to set them to the same value.
//		}
		hangTimeSetting = new LongSetting(persistenceProvider, propStub + ".text.hangtime", 5000L);
		textColorOverride = new ColorSetting(persistenceProvider, propStub + ".text.color", null);
		this.original = original;
	}

	@SuppressWarnings("UnusedReturnValue")
	public static ModifiedCalloutHandle installHandle(ModifiableCallout<?> original, PersistenceProvider pers, String propStub) {
		// TODO
		return installHandle(original, pers, propStub, null, null, CalloutDefaults.dummy());
	}

	public static ModifiedCalloutHandle installHandle(ModifiableCallout<?> original, PersistenceProvider pers, String propStub, @Nullable BooleanSetting allTts, @Nullable BooleanSetting allText, CalloutDefaults parent) {
		ModifiedCalloutHandle modified = new ModifiedCalloutHandle(pers, propStub, original, allTts, allText, parent);
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

	public StringSetting getSoundSetting() {
		return soundSetting;
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

	public @Nullable String getSoundFileIdentifier() {
		String s = soundSetting.get();
		if (s.isEmpty()) {
			return null;
		}
		return s;
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

	public ColorSetting getTextColorOverride() {
		return textColorOverride;
	}
}
