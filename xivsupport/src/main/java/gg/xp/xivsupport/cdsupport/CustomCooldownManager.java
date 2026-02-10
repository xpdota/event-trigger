package gg.xp.xivsupport.cdsupport;

import tools.jackson.core.type.TypeReference;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.CustomJsonListSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@ScanMe
public class CustomCooldownManager {

	private static final Logger log = LoggerFactory.getLogger(CustomCooldownManager.class);
	private static final String settingKey = "custom-cooldowns.my-cooldowns";
	private static final String failedSettingKey = "custom-cooldowns.failed-to-load";
	private final CustomJsonListSetting<CustomCooldown> setting;

	private final List<CustomCooldown> cds;

	public CustomCooldownManager(PersistenceProvider pers, EventMaster master) {
		this.setting = CustomJsonListSetting.builder(pers, new TypeReference<CustomCooldown>() {
		}, settingKey, failedSettingKey).build();
		setting.addListener(() -> master.pushEvent(new CustomCooldownsUpdated()));
		this.cds = new ArrayList<>(setting.getItems());
	}

	public List<CustomCooldown> getCooldowns() {
		return Collections.unmodifiableList(cds);
	}

	public List<ExtendedCooldownDescriptor> getAllCds() {
		return Stream.concat(Arrays.stream(Cooldown.values()), getCooldowns().stream().map(CustomCooldown::buildCd)).toList();
	}

	public void addCooldown(CustomCooldown cooldown) {
		cds.add(cooldown);
		commit();
	}

	public void removeCooldown(CustomCooldown cooldown) {
		cds.remove(cooldown);
		commit();
	}

	public void commit() {
		setting.setItems(cds);
	}


}
