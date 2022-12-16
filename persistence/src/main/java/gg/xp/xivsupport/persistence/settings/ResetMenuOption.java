package gg.xp.xivsupport.persistence.settings;

import javax.swing.*;

public final class ResetMenuOption {
	private ResetMenuOption() {
	}

	public static JMenuItem resetSetting(Resettable setting, Runnable resetAction) {
		// TODO: also work action into here
		JMenuItem reset = new JMenuItem("Reset to Default") {
			@Override
			public boolean isEnabled() {
				return setting.isSet();
			}

			@Override
			public String getToolTipText() {
				if (isEnabled()) {
					return "Reset this setting to the default";
				}
				else {
					return "Already the default, cannot reset";
				}
			}
		};
		reset.addActionListener(l -> {
			resetAction.run();
		});
		return reset;
	}

	public static JPopupMenu resetOnlyMenu(Resettable setting, Runnable resetAction) {
		JPopupMenu menu = new JPopupMenu();
		JMenuItem reset = resetSetting(setting, resetAction);
		menu.add(reset);
		return menu;
	}
}
