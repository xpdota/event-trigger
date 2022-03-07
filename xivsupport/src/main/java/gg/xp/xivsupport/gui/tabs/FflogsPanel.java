package gg.xp.xivsupport.gui.tabs;

import gg.xp.xivsupport.events.fflogs.FflogsController;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.persistence.gui.StringSettingGui;
import org.picocontainer.PicoContainer;

public class FflogsPanel extends TitleBorderFullsizePanel {
	public FflogsPanel(PicoContainer container) {
		super("FFLogs API");
		FflogsController fflogs = container.getComponent(FflogsController.class);
		add(new StringSettingGui(fflogs.clientId(), "FFLogs Client ID").getComponent());
		add(new StringSettingGui(fflogs.clientSecret(), "FFLogs Client Secret").getComponent());
	}
}
