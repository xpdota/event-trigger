package gg.xp.xivsupport.events.triggers.easytriggers.gui;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.BooleanSupplier;

public final class EtGuiUtils {

	private EtGuiUtils() {
	}

	public static JButton smallButton(String label, Runnable action) {
		return smallButton(label, action, null);
	}

	public static JButton smallButton(String label, Runnable action, @Nullable BooleanSupplier enabledWhen) {
		var button = compactButton(label, action, enabledWhen);
		button.setPreferredSize(new Dimension(50, button.getPreferredSize().height));
		return button;
	}

	public static JButton compactButton(String label, Runnable action) {
		return compactButton(label, action, null);
	}

	public static JButton compactButton(String label, Runnable action, @Nullable BooleanSupplier enabledWhen) {
		JButton button = new JButton(label) {
			@Override
			public boolean isEnabled() {
				return enabledWhen == null ? super.isEnabled() : enabledWhen.getAsBoolean();
			}
		};
		button.addActionListener(l -> action.run());
//		button.setPreferredSize(new Dimension(50, button.getPreferredSize().height));
		button.setMargin(new Insets(2, 2, 2, 2));
		return button;
	}
}
