package gg.xp.xivsupport.events.triggers.duties.ewult.omega;

import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import javax.swing.*;
import java.awt.*;

public class BooleanSettingHidingPanel extends JPanel {

	private final Component mainComponent;
	private final BooleanSetting setting;

	public BooleanSettingHidingPanel(BooleanSetting setting, String label, Component mainComponent, boolean padding) {
		super();
		JCheckBox checkbox = new BooleanSettingGui(setting, label).getComponent();
		if (padding) {
			setLayout(new GridBagLayout());
			GridBagConstraints c = GuiUtil.defaultGbc();
			c.anchor = GridBagConstraints.NORTHWEST;
			c.fill = GridBagConstraints.NONE;
			c.weighty = 0;
			add(checkbox, c);
			c.gridy++;
			add(mainComponent, c);
			c.gridy++;
			c.weighty = 1;
			c.weightx = 1;
			add(Box.createGlue(), c);
		}
		else {
			setLayout(new BorderLayout());
			add(checkbox, BorderLayout.NORTH);
			add(mainComponent, BorderLayout.CENTER);
		}
		this.setting = setting;
		this.mainComponent = mainComponent;
		setting.addAndRunListener(this::checkVis);
	}

	private void checkVis() {
		mainComponent.setVisible(setting.get());
	}
}
