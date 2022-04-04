package gg.xp.xivsupport.gui.overlay;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;

@ScanMe
public class OverlayConfig {
	private final BooleanSetting show;
	private final BooleanSetting forceShow;
	private final BooleanSetting ignoreRepaint;
	private final IntSetting bufferSetting;
	private final IntSetting minFps;
	private final IntSetting maxFps;

	public OverlayConfig(PersistenceProvider persistence) {
		show = new BooleanSetting(persistence, "xiv-overlay.show", true);
		forceShow = new BooleanSetting(persistence, "xiv-overlay.force-show", false);
		bufferSetting = new IntSetting(persistence, XivOverlay.bufferNumSettingKey, 0, 0, 3);
		minFps = new IntSetting(persistence, "xiv-overlay.min-fps", 15, 1, 600);
		maxFps = new IntSetting(persistence, "xiv-overlay.max-fps", 30, 1, 600);
		ignoreRepaint = new BooleanSetting(persistence, "xiv-overlay.ignore-repaint", false);
	}

	public BooleanSetting getShow() {
		return show;
	}

	public BooleanSetting getForceShow() {
		return forceShow;
	}

	public IntSetting getBufferSetting() {
		return bufferSetting;
	}

	public IntSetting getMinFps() {
		return minFps;
	}

	public IntSetting getMaxFps() {
		return maxFps;
	}

	public BooleanSetting getIgnoreRepaint() {
		return ignoreRepaint;
	}
}
