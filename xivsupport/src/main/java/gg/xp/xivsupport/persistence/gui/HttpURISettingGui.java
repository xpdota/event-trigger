package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.persistence.settings.HttpURISetting;
import gg.xp.xivsupport.persistence.settings.ResetMenuOption;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.Locale;

public class HttpURISettingGui {

	private final TextFieldWithValidation<URI> textBox;
	private final HttpURISetting setting;
	private final String label;
	private JLabel jLabel;
	private volatile boolean resetInProgress;
	private volatile boolean setInProgress;

	public HttpURISettingGui(HttpURISetting setting, String label) {
		this(setting, label, false);
	}

	public HttpURISettingGui(HttpURISetting setting, String label, boolean listen) {
		this.setting = setting;
		textBox = new TextFieldWithValidation<>(str -> {
			URI uri = URI.create(str);
			String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
			if ("http".equals(scheme) || "https".equals(scheme)) {
				return uri;
			}
			else {
				throw new IllegalArgumentException("Protocol must be HTTP or HTTPS");
			}
		}, this::setNewValue, () -> setting.get().toString());
		textBox.setColumns(20);
		textBox.setComponentPopupMenu(ResetMenuOption.resetOnlyMenu(setting, this::reset));
		if (listen) {
			setting.addListener(() -> {
				if (!setInProgress) {
					textBox.resetText();
				}
			});
		}
		this.label = label;
	}

	private void setNewValue(URI newValue) {
		if (!resetInProgress) {
			setInProgress = true;
			setting.set(newValue);
			setInProgress = false;
		}
	}

	private void reset() {
		resetInProgress = true;
		try {
			textBox.setText(setting.getDefault().toString());
			setting.delete();
		}
		finally {
			resetInProgress = false;
		}
	}

	public Component getTextBoxOnly() {
		return textBox;
	}

	public Component getLabelOnly() {
		if (jLabel == null) {
			jLabel = new JLabel(label);
			jLabel.setLabelFor(textBox);
		}
		return jLabel;
	}

	public Component getResetButton() {
		JButton jButton = new JButton("Reset");
		jButton.setMargin(new Insets(3, 8, 3, 8));
		jButton.addActionListener(l -> {
			// TODO: Kind of bad
			textBox.setText(setting.getDefault().toString());
			setting.resetToDefault();
		});
		return jButton;
	}

	public JPanel getComponent() {
		Component textBox = getTextBoxOnly();
		Component resetButton = getResetButton();
		JPanel box = new JPanel() {
			@Override
			public void setEnabled(boolean enabled) {
				super.setEnabled(enabled);
				textBox.setEnabled(enabled);
				resetButton.setEnabled(enabled);
			}
		};
		box.setLayout(new WrapLayout());
		box.add(textBox);
		box.add(resetButton);
		box.add(getLabelOnly());
		box.setMaximumSize(box.getPreferredSize());
		return box;
	}
}
