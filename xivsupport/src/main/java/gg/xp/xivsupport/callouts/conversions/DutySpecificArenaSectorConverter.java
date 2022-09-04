package gg.xp.xivsupport.callouts.conversions;

import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.StringSetting;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class DutySpecificArenaSectorConverter {
	private final Map<ArenaSector, StringSetting> perSectorSettings;

	public DutySpecificArenaSectorConverter(PersistenceProvider pers, long zone) {
		perSectorSettings = new EnumMap<>(ArenaSector.class);
		for (ArenaSector value : ArenaSector.values()) {
			perSectorSettings.put(value, new StringSetting(pers, "callout-processor.arena-sector.zone-specific.%s.%s".formatted(zone, value.name()), null));
		}
	}

	public Map<ArenaSector, StringSetting> getPerSectorSettings() {
		return Collections.unmodifiableMap(perSectorSettings);
	}

	public @Nullable String valueForSector(ArenaSector sector) {
		StringSetting setting = perSectorSettings.get(sector);
		if (setting == null || setting.get() == null || setting.get().isBlank()) {
			return null;
		}
		else {
			return setting.get();
		}
	}


}
