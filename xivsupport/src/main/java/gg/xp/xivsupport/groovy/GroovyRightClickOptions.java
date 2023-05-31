package gg.xp.xivsupport.groovy;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.GuiMain;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.util.GuiUtil;
import org.picocontainer.PicoContainer;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class GroovyRightClickOptions {

	private long count = 1;

	public GroovyRightClickOptions(RightClickOptionRepo rcop, GroovyManager mgr, PicoContainer container) {
		rcop.addOption(CustomRightClickOption.forRow("Save As Temp Variable", Object.class, item -> {
			String tempVarName = "groovy_temp_" + count++;
			mgr.getGlobalBinding().setVariable(tempVarName, item);
			String message = String.format("Row (%s) copied to '%s'", item.getClass().getSimpleName(), tempVarName);
			int result = JOptionPane.showOptionDialog(container.getComponent(GuiMain.class).getMainFrame(), message, "Copied", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"Close", "Copy to Clipboard"}, "Copy to Clipboard");
			if (result == JOptionPane.NO_OPTION) {
				GuiUtil.copyTextToClipboard(tempVarName);
			}
		}));
	}
}
