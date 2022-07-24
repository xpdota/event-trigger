package gg.xp.xivsupport.callouts.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.SoundFilesManager;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;

import java.awt.*;

@ScanMe
public class SoundFileTab implements PluginTab {

	private final SoundFilesManager backend;

	public SoundFileTab(SoundFilesManager backend) {
		this.backend = backend;
	}

	@Override
	public String getTabName() {
		return "Audio Files";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel files = new TitleBorderFullsizePanel("Audio Files");

		return null;
	}
}
