package gg.xp.xivsupport.gui.tabs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.SimplifiedPropertiesFilePersistenceProvider;
import gg.xp.xivsupport.persistence.settings.CustomJsonListSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

@ScanMe
public class UpdaterConfig {

	private static final String propsOverrideFileName = "update.properties";
	private final File propsOverride;
	private final PersistenceProvider updatePropsFilePers;
	private final File installDir;
	private final StringSetting branchSetting;
	private final StringSetting urlTemplateSetting;
	private final CustomJsonListSetting<AddonDef> addonSetting;

	public UpdaterConfig(PersistenceProvider pers) {
		this.installDir = Platform.getInstallDir();
		propsOverride = Paths.get(installDir.toString(), propsOverrideFileName).toFile();
		updatePropsFilePers = new SimplifiedPropertiesFilePersistenceProvider(propsOverride);
		branchSetting = new StringSetting(updatePropsFilePers, "branch", "stable");
		urlTemplateSetting = new StringSetting(updatePropsFilePers, "url_template", "https://xpdota.github.io/event-trigger/%s/v2/%s");
		addonSetting = CustomJsonListSetting.<AddonDef>builder(pers, new TypeReference<>() {
		}, "update-config.addon-manager.addon-list", "update-config.addon-manager.addon-list-failed").build();
	}

	public StringSetting getBranchSetting() {
		return branchSetting;
	}

	public StringSetting getUrlTemplateSetting() {
		return urlTemplateSetting;
	}

	public CustomJsonListSetting<AddonDef> getAddonSetting() {
		return addonSetting;
	}

	public void addNewAddon(String addonInfoUrl) {
		if (!addonInfoUrl.endsWith("INFO")) {
			addonInfoUrl = addonInfoUrl + "/INFO";
		}
		ObjectMapper mapper = new ObjectMapper();
		AddonDef newAddonDef;
		try {
			newAddonDef = mapper.readValue(addonInfoUrl, new TypeReference<>() {
			});
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		addonSetting.addItem(newAddonDef);
	}
}
