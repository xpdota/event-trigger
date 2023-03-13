package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class FileSetting extends ValueSetting<File> {

	public FileSetting(PersistenceProvider persistence, String settingKey, @NotNull File dflt) {
		super(persistence, settingKey, dflt);
	}
}
