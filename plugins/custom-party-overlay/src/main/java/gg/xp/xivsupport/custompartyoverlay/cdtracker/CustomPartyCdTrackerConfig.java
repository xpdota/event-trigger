package gg.xp.xivsupport.custompartyoverlay.cdtracker;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.cdsupport.CustomCooldown;
import gg.xp.xivsupport.cdsupport.CustomCooldownManager;
import gg.xp.xivsupport.cdsupport.CustomCooldownsUpdated;
import gg.xp.xivsupport.events.triggers.jobs.gui.DefaultCdTrackerColorProvider;
import gg.xp.xivsupport.events.triggers.jobs.gui.SettingsCdTrackerColorProvider;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.ObservableSetting;
import groovyjarjarantlr4.v4.runtime.misc.IntSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ScanMe
public class CustomPartyCdTrackerConfig extends ObservableSetting {

	private static final Logger log = LoggerFactory.getLogger(CustomPartyCdTrackerConfig.class);

	//	private List<ExtendedCooldownDescriptor> allCds = Collections.emptyList();
	private final Map<Cooldown, CustomPartyCdSetting> partyCdsBuiltin = new LinkedHashMap<>();
	private final Map<ExtendedCooldownDescriptor, CustomPartyCdSetting> partyCdsCustom = new LinkedHashMap<>();
	private Map<ExtendedCooldownDescriptor, CustomPartyCdSetting> partyCds = Collections.emptyMap();
	private static final String settingKeyBase = "custom-party.cd-tracker";
	private static final String cdKeyStub = settingKeyBase + ".enable-cd.";

	private final CustomCooldownManager customCooldownManager;
	private final PersistenceProvider persistence;
	private final IntSetting spacing;
	private final IntSetting borderWidth;
	private final IntSetting borderRoundness;
	private final BooleanSetting rightToLeft;
	private final SettingsCdTrackerColorProvider colors;

	public CustomPartyCdTrackerConfig(CustomCooldownManager customCooldownManager, PersistenceProvider persistence) {
		for (Cooldown cd : Cooldown.values()) {
			// Filter out invalid - we need an icon, and we can't do charge display (yet)
			if (isValidCd(cd)) {
				CustomPartyCdSetting newSetting = new CustomPartyCdSetting(persistence, getKey(cd) + ".party", defaultSetting(cd));
				setupListener(newSetting);
				partyCdsBuiltin.put(cd, newSetting);
			}
		}
		this.customCooldownManager = customCooldownManager;
		this.persistence = persistence;
		this.colors = SettingsCdTrackerColorProvider.of(persistence, settingKeyBase + ".colors", DefaultCdTrackerColorProvider.INSTANCE);
		this.spacing = new IntSetting(persistence, settingKeyBase + ".spacing", 2, 0, 1000);
		this.borderWidth = new IntSetting(persistence, settingKeyBase + ".border-width", 2, 1, 100);
		this.borderRoundness = new IntSetting(persistence, settingKeyBase + ".border-roundness", 0, 0, 100);
		this.rightToLeft = new BooleanSetting(persistence, settingKeyBase + ".right-to-left", true);
		List.of(spacing, borderWidth, borderRoundness, rightToLeft)
				.forEach(setting -> setting.addListener(this::notifyListeners));
		refreshCustoms();
	}

	private static String getKey(ExtendedCooldownDescriptor buff) {
		return cdKeyStub + buff.getSettingKeyStub();
	}

	private static boolean isValidCd(ExtendedCooldownDescriptor cd) {
		return ActionLibrary.iconForId(cd.getPrimaryAbilityId()) != null && cd.getMaxCharges() <= 1;
	}

	private synchronized void refreshCustoms() {
		log.info("Refreshing custom CDs");
		List<CustomCooldown> customs = customCooldownManager.getCooldowns();
		partyCdsCustom.clear();
		for (CustomCooldown custom : customs) {
			ExtendedCooldownDescriptor cd;
			try {
				cd = custom.buildCd();
			}
			catch (Throwable t) {
				log.error("Error loading custom cooldown ({}, {})", custom.nameOverride, String.format("0x%X", custom.primaryAbilityId));
				continue;
			}
			if (isValidCd(cd)) {
				CustomPartyCdSetting newCdSetting = new CustomPartyCdSetting(persistence, getKey(cd) + ".party", false);
				setupListener(newCdSetting);
				partyCdsCustom.put(cd, newCdSetting);
			}
		}
		Map<ExtendedCooldownDescriptor, CustomPartyCdSetting> partyCds = new LinkedHashMap<>();
		partyCds.putAll(partyCdsCustom);
		partyCds.putAll(partyCdsBuiltin);
		this.partyCds = partyCds;
//		List<ExtendedCooldownDescriptor> all = new ArrayList<>();
//		all.addAll(partyCds.keySet());
//		allCds = all;
		log.info("Number of CDs: {} builtin, {} custom", partyCdsBuiltin.size(), customs.size());
		notifyListeners();
	}

	private void setupListener(CustomPartyCdSetting newCdSetting) {
		newCdSetting.getEnable().addListener(this::notifyListeners);
	}

	public Map<ExtendedCooldownDescriptor, CustomPartyCdSetting> getSettings() {
		return Collections.unmodifiableMap(partyCds);
	}

	public List<ExtendedCooldownDescriptor> getEnabledCds() {
		return partyCds.entrySet().stream().filter(e -> e.getValue().getEnable().get()).map(Map.Entry::getKey).toList();
	}

	@HandleEvents
	public void refreshCustoms(EventContext context, CustomCooldownsUpdated updated) {
		refreshCustoms();
	}

	public SettingsCdTrackerColorProvider getColors() {
		return colors;
	}

	public IntSetting getSpacing() {
		return spacing;
	}

	public IntSetting getBorderRoundness() {
		return borderRoundness;
	}

	public BooleanSetting getRightToLeft() {
		return rightToLeft;
	}

	public IntSetting getBorderWidth() {
		return borderWidth;
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	private static boolean defaultSetting(ExtendedCooldownDescriptor ecd) {
		return defaultEnabled.contains(ecd);
	}

	private static final Set<Cooldown> defaultEnabled = EnumSet.of(
			Cooldown.HallowedGround,
			Cooldown.Holmgang,
			Cooldown.LivingDead,
			Cooldown.Superbolide,
			Cooldown.Temperance,
			Cooldown.Benediction,
			Cooldown.Swiftcast,
			Cooldown.ChainStratagem,
			Cooldown.Expedient,
			Cooldown.Divination,
			Cooldown.Panhaima,
			Cooldown.Brotherhood,
			Cooldown.Mug,
			Cooldown.ArcaneCircle,
			Cooldown.Troubadour,
			Cooldown.RadiantFinale,
			Cooldown.TechnicalStep,
			Cooldown.Devilment,
			Cooldown.ShieldSamba,
			Cooldown.SearingLight,
			Cooldown.Embolden
	);
}
