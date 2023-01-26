package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutomarkSetting extends ObservableSetting {

	private final BooleanSetting enabled;
	private final EnumSetting<MarkerSign> whichMark;

	public AutomarkSetting(PersistenceProvider pers, String settingKeyStub, boolean enabledByDefault, @NotNull MarkerSign defaultSign) {
		this.enabled = new BooleanSetting(pers, settingKeyStub + "enabled", enabledByDefault);
		this.whichMark = new EnumSetting<>(pers, settingKeyStub + "which-marker", MarkerSign.class, defaultSign);
		enabled.addListener(this::notifyListeners);
		whichMark.addListener(this::notifyListeners);
	}

	public BooleanSetting getEnabled() {
		return enabled;
	}

	public EnumSetting<MarkerSign> getWhichMark() {
		return whichMark;
	}

	public @Nullable MarkerSign getEffectiveMarker() {
		if (enabled.get()) {
			return whichMark.get();
		}
		else {
			return null;
		}
	}
}
