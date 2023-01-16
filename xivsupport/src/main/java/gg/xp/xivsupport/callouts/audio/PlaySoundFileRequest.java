package gg.xp.xivsupport.callouts.audio;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.util.Handleable;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.File;
import java.io.Serial;

public class PlaySoundFileRequest extends BaseEvent implements HasPrimaryValue, Handleable {

	@Serial
	private static final long serialVersionUID = 2447989121192776549L;
	private final File file;
	private boolean isHandled;

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

	public boolean isHandled() {
		return isHandled;
	}

	public void setHandled() {
		isHandled = true;
	}
}
