package gg.xp.xivsupport.callouts;

import gg.xp.xivsupport.speech.CalloutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModifiableCallout {

	private static final Logger log = LoggerFactory.getLogger(ModifiableCallout.class);

	private final String description;
	private final String defaultTtsText;
	private final String defaultVisualText;
	private final long defaultVisualHangTime;

	private volatile ModifiedCalloutHandle handle;

	public ModifiableCallout(String description, String text) {
		this.description = description;
		defaultTtsText = text;
		defaultVisualText = text;
		defaultVisualHangTime = 5000L;
	}

	public void attachHandle(ModifiedCalloutHandle handle) {
		this.handle = handle;
	}

	public String getDescription() {
		return description;
	}

	public String getOriginalTts() {
		return defaultTtsText;
	}

	public String getOriginalVisualText() {
		return defaultVisualText;
	}

	public CalloutEvent getModified() {
		if (handle == null) {
			log.warn("ModifiableCallout does not have handle yet ({})", description);
			return new CalloutEvent(defaultTtsText, defaultVisualText);
		}
		return new CalloutEvent(handle.getTtsSetting().get(), handle.getTextSetting().get());
	}
}
