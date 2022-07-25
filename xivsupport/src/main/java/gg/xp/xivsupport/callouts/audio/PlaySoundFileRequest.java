package gg.xp.xivsupport.callouts.audio;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.File;

public class PlaySoundFileRequest extends BaseEvent implements HasPrimaryValue {

	private final File file;

	public PlaySoundFileRequest(File file) {
		if (file == null) {
			throw new IllegalArgumentException("File cannot be null");
		}
		if (!file.exists()) {
			throw new IllegalArgumentException("File does not exist! (" + file + ')');
		}
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("%s", file.getName());
	}
}
