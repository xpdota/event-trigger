package gg.xp.xivsupport.callouts.gui;

import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.callouts.audio.SoundFilesManager;
import gg.xp.xivsupport.callouts.audio.gui.SoundFileTab;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.util.EasyAction;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.StringSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.ColorSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class CalloutSettingGui {

	private final JCheckBox callCheckbox;
	private final SoundFileTab sft;
	private final JPanel ttsPanel;
	private final JPanel textPanel;
	private final JPanel soundPanel;
	private final JCheckBox ttsCheckbox;
	private final JTextField ttsTextBox;
	private final JCheckBox textCheckbox;
	private final JCheckBox sameCheckBox;
	private final JTextField textTextBox;
	private final BooleanSetting allText;
	private final BooleanSetting allTts;
	private final JPanel colorPickerAndActionsPanel;
	private final JButton colorPicker;
	private final @Nullable Component extendedDescription;
	private boolean enabledByParent = true;

	public CalloutSettingGui(ModifiedCalloutHandle call, SoundFilesManager soundMgr, SoundFileTab sft, List<ExtraCalloutAction> extras) {
		callCheckbox = new BooleanSettingGui(call.getEnable(), call.getDescription(), () -> enabledByParent).getComponent();
		this.extendedDescription = makeExtendedDescription(call.getOriginal().getExtendedDescription());
		this.sft = sft;
		BooleanSetting enableTts = call.getEnableTts();
		StringSetting ttsSetting = call.getTtsSetting();
		BooleanSetting enableText = call.getEnableText();
		BooleanSetting sameText = call.getSameText();
		StringSetting textSetting = call.getTextSetting();
		ColorSetting colorOverride = call.getTextColorOverride();
		this.allText = call.getAllTextEnabled();
		this.allTts = call.getAllTtsEnabled();

		{
			ttsPanel = new JPanel() {
				@Override
				public boolean isEnabled() {
					return CalloutSettingGui.this.isThisCallEnabled() && allTts.get();
				}
			};
			ttsPanel.setLayout(new BoxLayout(ttsPanel, BoxLayout.LINE_AXIS));
			ttsCheckbox = new BooleanSettingGui(enableTts, "TTS:", ttsPanel::isEnabled).getComponent();
			ttsCheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
			ttsPanel.add(ttsCheckbox);
			ttsTextBox = new StringSettingGui(ttsSetting, null, () -> ttsPanel.isEnabled() && ttsCheckbox.isSelected()).getTextBoxOnly();
			ttsPanel.add(ttsTextBox);
		}
		{
			textPanel = new JPanel() {
				@Override
				public boolean isEnabled() {
					return CalloutSettingGui.this.isThisCallEnabled() && allText.get();
				}
			};
			textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.LINE_AXIS));

			textCheckbox = new BooleanSettingGui(enableText, "Text:", textPanel::isEnabled).getComponent();
			textCheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
			textPanel.add(textCheckbox);

			sameCheckBox = new BooleanSettingGui(sameText, "Same as TTS:", () -> textPanel.isEnabled() && textCheckbox.isSelected()).getComponent();
			sameCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
			textPanel.add(sameCheckBox);

			textTextBox = new StringSettingGui(textSetting, null, () -> textPanel.isEnabled() && textCheckbox.isSelected() && !sameCheckBox.isSelected()).getTextBoxOnly();
			textPanel.add(textTextBox);
		}
		{
			soundPanel = new JPanel();
			soundPanel.setLayout(new BoxLayout(soundPanel, BoxLayout.LINE_AXIS));
			JLabel soundLabel = new JLabel("Sound: ");

			JComboBox<String> filePicker = new SoundFilePicker(sft, soundMgr, () -> call.getSoundSetting().get(), file -> call.getSoundSetting().set(file)).getPicker();
			soundPanel.add(soundLabel);
			soundPanel.add(filePicker);
		}
		{
			colorPicker = new ColorSettingGui(colorOverride, "Text Color", textPanel::isEnabled).getButtonOnly();
			colorPicker.setPreferredSize(new Dimension(20, 20));
			colorPicker.setMinimumSize(new Dimension(20, 20));
			colorPicker.setMaximumSize(new Dimension(50, 256));
			colorPickerAndActionsPanel = new JPanel();
			colorPickerAndActionsPanel.setLayout(new BoxLayout(colorPickerAndActionsPanel, BoxLayout.LINE_AXIS));

			colorPickerAndActionsPanel.add(new JLabel("Override Text Color: "));
			colorPickerAndActionsPanel.add(colorPicker);

			if (!extras.isEmpty()) {
				colorPickerAndActionsPanel.add(Box.createHorizontalGlue());
				JButton extraActionsButton = new JButton("More Actions...");
				colorPickerAndActionsPanel.add(extraActionsButton);
				extraActionsButton.addActionListener(l -> {
					JPopupMenu popupMenu = new JPopupMenu();
					popupMenu.addPopupMenuListener(new PopupMenuListener() {
						@Override
						public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
							popupMenu.removeAll();
							for (ExtraCalloutAction extra : extras) {
								ExtraCalloutActionInstance inst = extra.getInstance(call.getOriginal(), call);
								if (inst == null || !inst.isVisible()) {
									continue;
								}
								JMenuItem menuItem = new JMenuItem(inst.getLabel());
								menuItem.setEnabled(inst.isEnabled());
								menuItem.addActionListener(l -> inst.doAction());
								popupMenu.add(menuItem);
							}
							popupMenu.revalidate();

						}

						@Override
						public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

						}

						@Override
						public void popupMenuCanceled(PopupMenuEvent e) {

						}
					});
					popupMenu.show(extraActionsButton, 0, extraActionsButton.getHeight());
				});
			}
		}
		recalcEnabledDisabledStatus();
		ActionListener l = e -> recalcEnabledDisabledStatus();
		callCheckbox.addActionListener(l);
		ttsCheckbox.addActionListener(l);
		textCheckbox.addActionListener(l);
		sameCheckBox.addActionListener(l);
		allText.addListener(this::recalcEnabledDisabledStatus);
		allTts.addListener(this::recalcEnabledDisabledStatus);
	}

	private boolean isThisCallEnabled() {
		return callCheckbox.isSelected() && enabledByParent;
	}

	private void recalcEnabledDisabledStatus() {
//		SwingUtilities.invokeLater(() -> {
//			ttsPanel.setVisible(false);
//			ttsPanel.setVisible(true);
//			textPanel.setVisible(false);
//			textPanel.setVisible(true);
//		});
		callCheckbox.repaint();
		ttsPanel.updateUI();
		textPanel.updateUI();
	}

	public void setEnabledByParent(boolean enabledByParent) {
		this.enabledByParent = enabledByParent;
		recalcEnabledDisabledStatus();
	}

	public JCheckBox getCallCheckbox() {
		return callCheckbox;
	}

	public @Nullable Component getExtendedDescription() {
		return extendedDescription;
	}

	private static @Nullable Component makeExtendedDescription(@Nullable String edesc) {
		if (edesc == null) {
			return null;
		}
		ReadOnlyText text = new ReadOnlyText(edesc, false);
		text.setFocusable(true);
		return text;
	}

	public JPanel getTtsPanel() {
		return ttsPanel;
	}

	public JPanel getTextPanel() {
		return textPanel;
	}

	public JPanel getSoundPanel() {
		return soundPanel;
	}

	public Component getColorPicker() {
		return colorPicker;
	}

	public JPanel getColorPickerAndActionsPanel() {
		return colorPickerAndActionsPanel;
	}

	public void setVisible(boolean visible) {
//		private final JCheckBox callCheckbox;
//		private final JPanel ttsPanel;
//		private final JPanel textPanel;
//		private final JCheckBox ttsCheckbox;
//		private final JTextField ttsTextBox;
//		private final JCheckBox textCheckbox;
//		private final JCheckBox sameCheckBox;
//		private final JTextField textTextBox;
		callCheckbox.setVisible(visible);
		ttsPanel.setVisible(visible);
		textPanel.setVisible(visible);
		colorPickerAndActionsPanel.setVisible(visible);
		soundPanel.setVisible(visible);
		if (extendedDescription != null) {
			extendedDescription.setVisible(visible);
		}

	}
}
