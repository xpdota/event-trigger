package gg.xp.xivsupport.callouts.gui;

import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.StringSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;

import javax.swing.*;
import java.awt.event.ActionListener;

public class CalloutSettingGui {

	private final JCheckBox callCheckbox;
	private final JPanel ttsPanel;
	private final JPanel textPanel;
	private final JCheckBox ttsCheckbox;
	private final JTextField ttsTextBox;
	private final JCheckBox textCheckbox;
	private final JTextField textTextBox;
	private final BooleanSetting allText;
	private final BooleanSetting allTts;
	private boolean enabledByParent = true;

	public CalloutSettingGui(ModifiedCalloutHandle call) {
		callCheckbox = new BooleanSettingGui(call.getEnable(), call.getDescription()).getComponent();
		BooleanSetting enableTts = call.getEnableTts();
		StringSetting ttsSetting = call.getTtsSetting();
		BooleanSetting enableText = call.getEnableText();
		StringSetting textSetting = call.getTextSetting();
		this.allText = call.getAllTextEnabled();
		this.allTts = call.getAllTtsEnabled();

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
		allText.addListener(this::recalcEnabledDisabledStatus);
		allTts.addListener(this::recalcEnabledDisabledStatus);
	}

	private void recalcEnabledDisabledStatus() {
		callCheckbox.setEnabled(enabledByParent);
		boolean effectivelyEnabled = callCheckbox.isSelected() && enabledByParent;
		if (effectivelyEnabled) {
			if (allTts.get()) {
				ttsCheckbox.setEnabled(true);
				ttsTextBox.setEnabled(ttsCheckbox.isSelected());
			}
			else {
				ttsCheckbox.setEnabled(false);
				ttsTextBox.setEnabled(false);
			}
			if (allText.get()) {
				textCheckbox.setEnabled(true);
				textTextBox.setEnabled(textCheckbox.isSelected());
			}
			else {
				textCheckbox.setEnabled(false);
				textTextBox.setEnabled(false);

			}
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
