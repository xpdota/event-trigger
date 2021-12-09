package gg.xp.xivsupport.callouts.gui;

import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.StringSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class CalloutSettingGui {

	private final JCheckBox callCheckbox;
	private final JPanel ttsPanel;
	private final JPanel textPanel;
	private final JCheckBox ttsCheckbox;
	private final Component ttsTextBox;
	private final JCheckBox textCheckbox;
	private final Component textTextBox;
	private boolean enabledByParent = true;

	public CalloutSettingGui(ModifiedCalloutHandle call) {
		callCheckbox = new BooleanSettingGui(call.getEnable(), call.getDescription()).getComponent();
		BooleanSetting enableTts = call.getEnableTts();
		StringSetting ttsSetting = call.getTtsSetting();
		BooleanSetting enableText = call.getEnableText();
		StringSetting textSetting = call.getTextSetting();

		{
			ttsPanel = new JPanel();
			ttsPanel.setLayout(new BoxLayout(ttsPanel, BoxLayout.LINE_AXIS));
			ttsCheckbox = new BooleanSettingGui(enableTts, "TTS:").getComponent();
			ttsCheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
			ttsPanel.add(ttsCheckbox);
			ttsTextBox = new StringSettingGui(ttsSetting, null).getTextBoxOnly();
			ttsPanel.add(ttsTextBox);
		}
		{
			textPanel = new JPanel();
			textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.LINE_AXIS));
			textCheckbox = new BooleanSettingGui(enableText, "Text:").getComponent();
			textCheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
			textPanel.add(textCheckbox);
			textTextBox = new StringSettingGui(textSetting, null).getTextBoxOnly();
			textPanel.add(textTextBox);
		}
		recalcEnabledDisabledStatus();
		ActionListener l = e -> recalcEnabledDisabledStatus();
		callCheckbox.addActionListener(l);
		ttsCheckbox.addActionListener(l);
		textCheckbox.addActionListener(l);
	}

	private void recalcEnabledDisabledStatus() {
		callCheckbox.setEnabled(enabledByParent);
		boolean effectivelyEnabled = callCheckbox.isSelected() && enabledByParent;
		if (effectivelyEnabled) {
			ttsCheckbox.setEnabled(true);
			ttsTextBox.setEnabled(ttsCheckbox.isSelected());
			textCheckbox.setEnabled(true);
			textTextBox.setEnabled(textCheckbox.isSelected());
		}
		else {
			ttsCheckbox.setEnabled(false);
			textCheckbox.setEnabled(false);
			ttsTextBox.setEnabled(false);
			textTextBox.setEnabled(false);
		}
	}

	public void setEnabledByParent(boolean enabledByParent) {
		this.enabledByParent = enabledByParent;
		recalcEnabledDisabledStatus();
	}

	public JCheckBox getCallCheckbox() {
		return callCheckbox;
	}

	public JPanel getTtsPanel() {
		return ttsPanel;
	}

	public JPanel getTextPanel() {
		return textPanel;
	}
}
