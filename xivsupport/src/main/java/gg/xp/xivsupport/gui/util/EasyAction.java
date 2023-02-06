package gg.xp.xivsupport.gui.util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

public class EasyAction {

	private final String name;
	private final Runnable runAction;
	private final Supplier<Boolean> enabled;
	private final KeyStroke key;

	public EasyAction(String name, Runnable runAction, Supplier<Boolean> enabled, KeyStroke key) {
		this.name = name;
		this.runAction = runAction;
		this.enabled = enabled;
		this.key = key;
	}

	public void configureComponent(JComponent component) {
		if (key != null) {
			component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, this);
			component.getActionMap().put(this, asAction());
		}
	}

	public void run() {
		if (enabled.get()) {
			runAction.run();
		}
	}

	public Action asAction() {
		return new AbstractAction(name) {
//			{
//				putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
//			}

			@Override
			public void actionPerformed(ActionEvent e) {
				runAction.run();
			}

			@Override
			public boolean isEnabled() {
				return EasyAction.this.enabled.get();
			}
		};
	}

	public Action asActionWithKey() {
		return new AbstractAction(name) {
			{
				putValue(Action.ACCELERATOR_KEY, key);
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				runAction.run();
			}

			@Override
			public boolean isEnabled() {
				return EasyAction.this.enabled.get();
			}
		};
	}

	public String getHotkeyAsString() {
		return InputEvent.getModifiersExText(key.getModifiers()) + " + " + KeyEvent.getKeyText(key.getKeyCode());
	}

	public JButton asButtonWithKeyLabel() {
		JButton button = new JButton(asAction());
		button.setText(button.getText() + " (" + getHotkeyAsString() + ')');
		return button;
	}

	public JButton asButton() {
		JButton button = new JButton(asAction());
		if (key != null) {
			button.setToolTipText(button.getText() + " (" + getHotkeyAsString() + ')');
		}
		return button;
	}
}
