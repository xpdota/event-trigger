package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class MultiSlotAutomarkSetting<X extends Enum<X>> extends ObservableSetting {

	private final BooleanSetting touched;
	private final Map<X, AutomarkSetting> settings;
	private final Class<X> enumCls;

	public MultiSlotAutomarkSetting(PersistenceProvider pers, String settingKeyBase, Class<X> enumCls, Map<X, MarkerSign> defaults) {
		touched = new BooleanSetting(pers, settingKeyBase, false);
		settings = new EnumMap<>(enumCls);
		for (X member : enumCls.getEnumConstants()) {
			MarkerSign dflt = defaults.get(member);
			boolean enableByDefault;
			if (dflt == null) {
				enableByDefault = false;
				dflt = MarkerSign.ATTACK_NEXT;
			}
			else {
				enableByDefault = true;
			}
			AutomarkSetting setting = new AutomarkSetting(pers, settingKeyBase + '.' + member.name() + '.', enableByDefault, dflt);
			setting.addListener(this::notifyListeners);
			setting.addAndRunListener(() -> {
				if (setting.isSet()) {
					touched.set(true);
				}
			});
			settings.put(member, setting);
		}
		this.enumCls = enumCls;
	}

	public Class<X> getEnumCls() {
		return enumCls;
	}

	public Map<X, AutomarkSetting> getSettings() {
		// Values can be mutated internally, but don't let external callers manipulate the actual map.
		return Collections.unmodifiableMap(settings);
	}

	public @Nullable MarkerSign getMarkerFor(X item) {
		AutomarkSetting setting = settings.get(item);
		if (setting == null) {
			return null;
		}
		return setting.getEffectiveMarker();
	}

	public void copyDefaultsFrom(MultiSlotAutomarkSetting<X> other) {
		if (this.touched.get()) {
			return;
		}
		other.getSettings().forEach((k, v) -> {
			EnumSetting<MarkerSign> thisSetting = this.getSettings().get(k).getWhichMark();
			EnumSetting<MarkerSign> thatSetting = v.getWhichMark();
			if (thatSetting.isSet() && !thisSetting.isSet()) {
				thisSetting.set(thatSetting.get());
			}
		});
	}
}
