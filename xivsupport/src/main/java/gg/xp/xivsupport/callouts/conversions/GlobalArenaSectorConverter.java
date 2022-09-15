package gg.xp.xivsupport.callouts.conversions;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@ScanMe
public class GlobalArenaSectorConverter {

	private static final Logger log = LoggerFactory.getLogger(GlobalArenaSectorConverter.class);
	private final EnumSetting<DefaultArenaSectorConversion> mainSetting;
	private final Map<ArenaSector, StringSetting> perSectorSettings;
	private final PersistenceProvider pers;
	private boolean suppressWarning;
	private @Nullable KnownDuty currentDuty;
	private final Map<KnownDuty, DutySpecificArenaSectorConverter> dutySpecific = new EnumMap<>(KnownDuty.class);
	private final Object lock = new Object();

	// TODO: per-zone stuff
	public GlobalArenaSectorConverter(PersistenceProvider pers) {
		mainSetting = new EnumSetting<>(pers, "callout-processor.arena-sector-conversion.type", DefaultArenaSectorConversion.class, DefaultArenaSectorConversion.FULL);
		perSectorSettings = new EnumMap<>(ArenaSector.class);
		for (ArenaSector value : ArenaSector.values()) {
			perSectorSettings.put(value, new StringSetting(pers, "callout-processor.arena-sector.specific." + value.name(), value.getFriendlyName()));
		}
		this.pers = pers;
	}

	@Contract("null -> null")
	public @Nullable DutySpecificArenaSectorConverter getDutySpecificConverter(KnownDuty duty) {
		if (duty == null) {
			return null;
		}
		Long zoneId = duty.getZoneId();
		if (zoneId == null) {
			return null;
		}
		synchronized (lock) {
			return dutySpecific.computeIfAbsent(duty, unused -> makeDutySpecificConverter(zoneId));
		}
	}

	private @NotNull DutySpecificArenaSectorConverter makeDutySpecificConverter(long zoneId) {
		return new DutySpecificArenaSectorConverter(pers, zoneId);
	}

	private @Nullable DutySpecificArenaSectorConverter getConverterForCurrentZone() {
		KnownDuty duty = currentDuty;
		if (duty == null) {
			return null;
		}
		return getDutySpecificConverter(duty);
	}

	@HandleEvents
	public void zoneChange(EventContext context, ZoneChangeEvent event) {
		currentDuty = KnownDuty.forZone(event.getZone().getId());
	}

	public EnumSetting<DefaultArenaSectorConversion> getMainSetting() {
		return mainSetting;
	}

	public Map<ArenaSector, StringSetting> getPerSectorSettings() {
		return Collections.unmodifiableMap(perSectorSettings);
	}

	public String convert(ArenaSector sector) {
		DutySpecificArenaSectorConverter converter = getConverterForCurrentZone();
		if (converter != null) {
			String result = converter.valueForSector(sector);
			if (result != null) {
				return result;
			}
		}
		switch (mainSetting.get()) {
			case ABBREVIATION -> {
				return sector.getAbbreviation();
			}
			case CUSTOM -> {
				StringSetting setting = perSectorSettings.get(sector);
				// Shouldn't happen
				if (setting == null) {
					if (!suppressWarning) {
						log.error("perSectorSetting was null for sector {}!", sector);
						suppressWarning = true;
					}
					return sector.getFriendlyName();
				}
				return setting.get();
			}
			default -> {
				return sector.getFriendlyName();
			}
		}
	}
}
