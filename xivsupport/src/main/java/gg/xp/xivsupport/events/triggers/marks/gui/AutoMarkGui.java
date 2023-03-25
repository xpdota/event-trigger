package gg.xp.xivsupport.events.triggers.marks.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.services.ServiceSelectorGui;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkHandler;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkKeyHandler;
import gg.xp.xivsupport.events.triggers.marks.adv.AutoMarkServiceSelector;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.EnumSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class AutoMarkGui implements PluginTab {

	private final AutoMarkHandler marks;
	private final AutoMarkKeyHandler keyHandler;
	private final AutoMarkServiceSelector serv;

	public AutoMarkGui(AutoMarkHandler marks, AutoMarkKeyHandler keyHandler, AutoMarkServiceSelector serv) {
		this.marks = marks;
		this.keyHandler = keyHandler;
		this.serv = serv;
	}

	@Override
	public String getTabName() {
		return "AutoMark";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Marks");
		JButton helpButton = new JButton("Help");
		helpButton.addActionListener(l -> {
			JOptionPane.showMessageDialog(SwingUtilities.getRoot(helpButton), helpText);
		});
		JButton macroHelpButton = new JButton("Macro Help");
		macroHelpButton.addActionListener(l -> {
			JOptionPane.showMessageDialog(SwingUtilities.getRoot(macroHelpButton), macroHelpText);
		});
		Component serviceSelectorLabel = new JLabel("AutoMark System:");
		Component serviceSelector = new ServiceSelectorGui(serv, "", () -> true, true).getComboBoxOnly();
		serv.addListener(outer::repaint);
		Component langMode = new EnumSettingGui<>(marks.getLanguageSetting(), "Game Client Language", () -> true).getComponent();
		Component useFKeys = new BooleanSettingGui(keyHandler.getUseFkeys(), "For Macros Only: Use F1-F9 (Instead of NumPad 1-9)", () -> keyHandler.getHandle().enabled()).getComponent();

		ReadOnlyText text = new ReadOnlyText("Note: PostNamazu or Telesto is REQUIRED for triggers that place specific markers (rather than just doing '/mk attack' such as Titan Jails)");

		GuiUtil.simpleTopDownLayout(outer, 400, helpButton, macroHelpButton, serviceSelectorLabel, serviceSelector, langMode, useFKeys, text);

		return outer;
	}

	private static final String helpText = """
			Instructions:
			You can use automarkers via Telesto or old-style macros. Telesto is recommended, since there is less setup
			and supports all the different marker types.
						
			To set up Telesto, install it in-game (Dalamud plugin), then head to the Telesto tab in triggevent to
			enable it and make sure it's working. Then, on the Automarks page, enable Telesto automarks.
						
			Then, test it with the command '/e c:samtest bind1 1'.
						
			If you are using a client that calls the red marker 'stop' instead of 'ignore', enable "Korean Client Mode".
						
			If you wish to use Macros instead, click the "Macro Help" button.
			""";

	private static final String macroHelpText = """
			You only need these instructions if you would prefer to use macros instead of Telesto. You should use
			Telesto if possible.
			
			You need one free hotbar for this.
						
			Place nine macros on a hotbar. The should look like this:
			/mk attack <1>
			and the second:
			/mk attack <2>
			...
			all the way up to the eighth:
			/mk attack <8>
						
			Finally, the ninth should be:
			/merror off
			/mk clear <1>
			/mk clear <2>
			/mk clear <3>
			/mk clear <4>
			/mk clear <5>
			/mk clear <6>
			/mk clear <7>
			/mk clear <8>
						
			Bind these nine buttons to either Numpad1-9, or F1-F9. I recommend the former, since F1-F8 by default is the party list selection.
						
			To test, use the command '/e c:amtest x y z' (without the quotes) where x, y, and z are the party slots of the players you want to mark.
						
			If you aren't in a party, you can still do '/e c:amtest 1' to test it on yourself.
			""";
}
