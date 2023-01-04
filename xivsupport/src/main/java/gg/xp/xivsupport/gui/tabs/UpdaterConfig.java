package gg.xp.xivsupport.gui.tabs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.SimplifiedPropertiesFilePersistenceProvider;
import gg.xp.xivsupport.persistence.settings.CustomJsonListSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ScanMe
public class UpdaterConfig {

	private static final Logger log = LoggerFactory.getLogger(UpdaterConfig.class);
	private static final String propsOverrideFileName = "update.properties";
	private static final Pattern allowedDirName = Pattern.compile("([A-Za-z][A-Za-z0-9-_]+)");
	private static final ObjectMapper mapper = new ObjectMapper();
	private final File propsOverride;
	private final PersistenceProvider updatePropsFilePers;
	private final File installDir;
	private final StringSetting branchSetting;
	private final StringSetting urlTemplateSetting;
	private final CustomJsonListSetting<AddonDef> addonSetting;
	private final StringSetting addonUrlsSetting;
	private final ExecutorService exs = Executors.newCachedThreadPool();
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
		addonSetting.addListener(this::syncAddonConfig);
		exs.submit(this::refreshAddons);
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
	}

	public void addNewAddon(String addonInfoUrl) {
		if (!addonInfoUrl.endsWith("INFO")) {
			addonInfoUrl += "/INFO";
		}
		AddonDef newAddonDef = getAddonDef(addonInfoUrl);
		addonSetting.addItem(newAddonDef);
	}

	private AddonDef getAddonDef(String url) {
		AddonDef newAddonDef;
		try {
			newAddonDef = mapper.readValue(new URL(url), new TypeReference<>() {
			});
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (newAddonDef.url == null) {
			newAddonDef.url = url;
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
		return newAddonDef;
	}

	// TODO: also need a way of refreshing addon info from INFO files, in case the author wants to change the icon,
	// move the manifest, etc.
	private void syncAddonConfig() {
		addonUrlsSetting.set(addonSetting.getItems()
				.stream()
				.map(ad -> ad.dirName + ':' + ad.urlPattern)
				.collect(Collectors.joining("\n"))
		);
	}

	public void refreshAddons() {
		List<AddonDef> addonsBefore = addonSetting.getItems();
		addonsBefore.forEach(addon -> {
			AddonDef addonDef;
			try {
				addonDef = getAddonDef(addon.url);
			}
			catch (Throwable t) {
				log.error("Error updating info for addon", t);
				return;
			}
			addon.readFrom(addonDef);
		});
		addonSetting.commit();
	}


	public void runUpdaterNow() {
		updateRunnable.run();
	}

	public void setUpdateRunnable(Runnable updateRunnable) {
		this.updateRunnable = updateRunnable;
	}
}
