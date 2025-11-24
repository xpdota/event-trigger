package gg.xp.xivsupport.callouts.audio;

import tools.jackson.core.type.TypeReference;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.CustomJsonListSetting;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

@ScanMe
public class SoundFilesManager {
	private static final String settingKey = "sound-files.my-sound-files";
	private static final String failedSettingKey = "sound-files.failed-to-load";
	public static final SoundFile NONE = new SoundFile();

	static {
		NONE.name = "None";
	}

	private final CustomJsonListSetting<SoundFile> setting;

	public SoundFilesManager(PersistenceProvider pers) {
		this.setting = CustomJsonListSetting.builder(pers, new TypeReference<SoundFile>() {
		}, settingKey, failedSettingKey).build();
	}

	public @Nullable SoundFile getSoundFileByName(String name) {
		if (name == null) {
			return null;
		}
		return getSoundFiles().stream().filter(sf -> sf.name.equals(name)).findFirst().orElse(null);
	}

	public List<SoundFile> getSoundFiles() {
		return setting.getItems();
	}

	public List<String> getSoundFilesAndNone() {
		return Stream.concat(Stream.of(NONE), getSoundFiles().stream())
				.map(s -> s.name)
				.toList();
	}

	public void addSoundFile(SoundFile soundFile) {
		setting.addItem(soundFile);
	}

	public void removeSoundFile(SoundFile soundFile) {
		setting.removeItem(soundFile);
	}
}
