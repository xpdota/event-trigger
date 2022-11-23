package gg.xp.xivsupport.gui.tabs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.SimplifiedPropertiesFilePersistenceProvider;
import gg.xp.xivsupport.persistence.settings.CustomJsonListSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ScanMe
public class UpdaterConfig {

	private static final String propsOverrideFileName = "update.properties";
	private static final Pattern allowedDirName = Pattern.compile("([A-Za-z][A-Za-z0-9-_]+)");
	private final File propsOverride;
	private final PersistenceProvider updatePropsFilePers;
	private final File installDir;
	private final StringSetting branchSetting;
	private final StringSetting urlTemplateSetting;
	private final CustomJsonListSetting<AddonDef> addonSetting;
	private final StringSetting addonUrlsSetting;
	private Runnable updateRunnable = () -> {

	};

	public UpdaterConfig(PersistenceProvider pers) {
		this.installDir = Platform.getInstallDir();
		propsOverride = Paths.get(installDir.toString(), propsOverrideFileName).toFile();
		updatePropsFilePers = new SimplifiedPropertiesFilePersistenceProvider(propsOverride);
		branchSetting = new StringSetting(updatePropsFilePers, "branch", "stable");
		urlTemplateSetting = new StringSetting(updatePropsFilePers, "url_template", "https://xpdota.github.io/event-trigger/%s/v2/%s");
		addonUrlsSetting = new StringSetting(updatePropsFilePers, "addons", "");
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

	public void removeAddon(AddonDef addon) {
		addonSetting.removeItem(addon);
		syncAddonConfig();
	}

	public void addNewAddon(String addonInfoUrl) {
		if (!addonInfoUrl.endsWith("INFO")) {
			addonInfoUrl += "/INFO";
		}
		ObjectMapper mapper = new ObjectMapper();
		AddonDef newAddonDef;
		try {
			newAddonDef = mapper.readValue(new URL(addonInfoUrl), new TypeReference<>() {
			});
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (newAddonDef.url == null) {
			newAddonDef.url = addonInfoUrl;
		}
		if (newAddonDef.name == null) {
			throw new AddonValidationException("Addon did not have a name");
		}
		if (newAddonDef.dirName == null) {
			throw new AddonValidationException("Addon did not define an installation sub-directory");
		}
		if (!allowedDirName.matcher(newAddonDef.dirName).matches()) {
			throw new AddonValidationException("Addon dir name is not allowed: " + allowedDirName);
		}
		if (newAddonDef.urlPattern == null) {
			throw new AddonValidationException("Addon did not define a download URL");
		}
		addonSetting.addItem(newAddonDef);
		syncAddonConfig();
	}

	private void syncAddonConfig() {
		addonUrlsSetting.set(addonSetting.getItems()
				.stream()
				.map(ad -> ad.dirName + ':' + ad.urlPattern)
				.collect(Collectors.joining("\n"))
		);
	}


	public void runUpdaterNow() {
		updateRunnable.run();
	}

	public void setUpdateRunnable(Runnable updateRunnable) {
		this.updateRunnable = updateRunnable;
	}
}
