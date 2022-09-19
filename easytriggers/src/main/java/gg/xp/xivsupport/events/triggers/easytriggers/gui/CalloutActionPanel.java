package gg.xp.xivsupport.events.triggers.easytriggers.gui;

import gg.xp.xivsupport.events.triggers.easytriggers.actions.DurationBasedCalloutAction;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.GenericCalloutAction;
import gg.xp.xivsupport.events.triggers.easytriggers.model.AcceptsSaveCallback;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import gg.xp.xivsupport.gui.components.ColorChooser;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.util.GuiUtil;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;

// TODO
public class CalloutActionPanel extends TitleBorderPanel implements AcceptsSaveCallback {
	private Runnable saveCallback;

	// TODO: this doesn't receive auto-save functionality
	public CalloutActionPanel(GenericCalloutAction action) {
		super((action instanceof DurationBasedCalloutAction ? "Callout with Duration" : "Callout"));
		setLayout(new GridBagLayout());
		GridBagConstraints c = GuiUtil.defaultGbc();
		c.insets = new Insets(1, 2, 1, 2);
		c.anchor = GridBagConstraints.NORTH;
		TextFieldWithValidation<String> ttsField = new TextFieldWithValidation<>(Function.identity(), editTriggerThenSave(action::setTts), action.getTts());
		TextFieldWithValidation<String> textField = new TextFieldWithValidation<>(Function.identity(), editTriggerThenSave(action::setText), action.getText());
		c.weightx = 0;
		add(GuiUtil.labelFor("TTS", ttsField), c);
		c.gridx++;
		c.weightx = 1;
		add(ttsField, c);
		c.gridx = 0;
		c.weightx = 0;
		c.gridy++;
		add(GuiUtil.labelFor("Text", textField), c);
		c.gridx++;
		add(textField, c);
		c.gridy = 0;
		c.gridx = 2;
		{
			TextFieldWithValidation<Long> hangTime = new TextFieldWithValidation<>(Long::parseLong, editTriggerThenSave(action::setHangTime), () -> Long.toString(action.getHangTime()));
			JCheckBox plusDuration = new JCheckBox();
			if (action instanceof DurationBasedCalloutAction dbca) {
				plusDuration.setEnabled(true);
				plusDuration.setSelected(dbca.isPlusDuration());
				plusDuration.addActionListener(l -> dbca.setPlusDuration(plusDuration.isSelected()));
				plusDuration.addActionListener(l -> requestSave());

			}
			else {
				plusDuration.setSelected(false);
				plusDuration.setEnabled(false);

			}

			JCheckBox useIcon = new JCheckBox();
			useIcon.setSelected(action.isUseIcon());
			useIcon.addActionListener(l -> action.setUseIcon(useIcon.isSelected()));
			useIcon.addActionListener(l -> requestSave());

			Component textColor = new ColorChooser("Text Color", action::getColor, editTriggerThenSave(action::setColor), () -> true).getButtonOnly();

			c.gridy = 0;
			add(GuiUtil.labelFor("Hang Time", hangTime), c);
			c.gridy++;
			add(GuiUtil.labelFor("Plus cast/buff duration", plusDuration), c);
			c.gridy = 0;

			c.gridx++;
			add(hangTime, c);
			c.gridy++;
			add(plusDuration, c);
			c.gridy = 0;
			c.gridx++;
			add(GuiUtil.labelFor("Use ability/buff icon", useIcon), c);
			c.gridy++;
			add(GuiUtil.labelFor("Text Color Override", textColor), c);
			c.gridy = 0;
			c.gridx++;
			add(useIcon, c);
			c.gridy++;
			add(textColor, c);
		}
	}

	private <X> Consumer<X> editTriggerThenSave(Consumer<X> modification) {
		return modification.andThen((unused) -> requestSave());
	}

	private void requestSave() {
		if (saveCallback != null) {
			saveCallback.run();
		}
	}

	@Override
	public void setSaveCallback(Runnable saveCallback) {
		this.saveCallback = saveCallback;
	}
}
