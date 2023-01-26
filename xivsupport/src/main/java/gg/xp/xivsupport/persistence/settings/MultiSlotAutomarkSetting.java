package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class MultiSlotAutomarkSetting<X extends Enum<X>> extends ObservableSetting {

	private final Map<X, AutomarkSetting> settings;

	public MultiSlotAutomarkSetting(PersistenceProvider pers, String settingKeyBase, Class<X> enumCls, Map<X, MarkerSign> defaults) {
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
			settings.put(member, setting);
		}
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
}
