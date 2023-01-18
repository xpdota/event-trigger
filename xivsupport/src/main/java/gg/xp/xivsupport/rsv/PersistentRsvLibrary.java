package gg.xp.xivsupport.rsv;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivdata.data.rsv.*;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.SimplifiedPropertiesFilePersistenceProvider;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PersistentRsvLibrary implements RsvLibrary {

	public static final PersistentRsvLibrary INSTANCE = new PersistentRsvLibrary();

	public static void install() {
		DefaultRsvLibrary.setLibrary(INSTANCE);
	}

	private final Map<GameLanguage, SimplifiedPropertiesFilePersistenceProvider> langMappings = new EnumMap<>(GameLanguage.class);
	private GameLanguage currentLang = GameLanguage.English;

	private PersistentRsvLibrary() {
		Path rsvDir = Platform.getTriggeventDir()
				.resolve("rsv");
		rsvDir.toFile().mkdirs();
		for (GameLanguage lang : GameLanguage.values()) {
			Path rsvFile = rsvDir.resolve(lang.getShortCode() + ".properties");
			langMappings.put(lang, new SimplifiedPropertiesFilePersistenceProvider(rsvFile.toFile()));
		}
	}

	public void setCurrentLang(GameLanguage currentLang) {
		if (currentLang == null) {
			throw new IllegalArgumentException("New language cannot be null");
		}
		this.currentLang = currentLang;
	}

	@Override
	public @Nullable String get(String key) {
		return langMappings.get(currentLang).getRaw(key);
	}

	public void set(GameLanguage lang, String rsvKey, String rsvValue) {
		langMappings.get(lang).saveRaw(rsvKey, rsvValue);
	}

	public List<RsvEntry> dumpAll() {
		return langMappings.entrySet().stream()
				.flatMap(
						langEntry -> langEntry.getValue().entries()
								.stream()
								.map(rsvEntry -> new RsvEntry(langEntry.getKey(), rsvEntry.getKey(), rsvEntry.getValue())))
				.toList();
	}
}
