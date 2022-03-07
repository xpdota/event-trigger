package gg.xp.xivsupport.gui;

import gg.xp.xivsupport.events.fflogs.FflogsReportLocator;
import gg.xp.xivsupport.gui.util.CatchFatalError;

public class GuiWithFflogsDownload {

	public static void main(String[] args) {
		CatchFatalError.run(() -> {
			LaunchImportedFflogs.fromUrl(FflogsReportLocator.fromURL("https://www.fflogs.com/reports/a:vxb6B8zjkApVrfgR#fight=3&type=damage-done"));
//			LaunchImportedFflogs.fromUrl(FflogsReportLocator.fromURL("https://www.fflogs.com/reports/a:XdvFm2qZRcap9DGK#fight=6"));
		});
	}
}
