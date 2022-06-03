package gg.xp.xivsupport.gui.overlay;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.EnumSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class FlyingTextOverlayConfig implements PluginTab {

	private final FlyingTextOverlay overlay;

	public FlyingTextOverlayConfig(FlyingTextOverlay overlay) {
		this.overlay = overlay;
	}


	@Override
	public String getTabName() {
		return "Visual Callouts";
	}

	@Override
	public Component getTabContents() {
		JPanel panel = new TitleBorderFullsizePanel("Visual Callouts");
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 5);
		BooleanSetting enabled = overlay.getEnabled();
		enabled.addListener(panel::repaint);
		panel.add(new BooleanSettingGui(enabled, "Flying Text Enabled").getComponent(), c);

		c.gridy++;

		panel.add(new EnumSettingGui<>(overlay.getAlignmentSetting(), "Text Alignment", enabled::get).getComponent(), c);

		c.gridy++;
		c.weighty = 1;

		panel.add(new ColorSettingGui(overlay.getTextColorSetting(), "Text Color", enabled::get).getComponent(), c);

		c.gridy++;
		c.weighty = 1;

		panel.add(Box.createGlue(), c);
		return panel;
	}

	@Override
	public int getSortOrder() {
		return 2;
	}
}
