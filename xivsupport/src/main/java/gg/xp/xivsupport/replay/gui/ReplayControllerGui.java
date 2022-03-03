package gg.xp.xivsupport.replay.gui;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.persistence.PropertiesFilePersistenceProvider;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.replay.ReplayController;
import org.picocontainer.MutablePicoContainer;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public final class ReplayControllerGui {

	private final ReplayController controller;
	private final JPanel panel;
	private final JButton advanceButton;
	private final TextFieldWithValidation<Integer> textBox;
	private final JLabel progressLabel;
	private int advanceAmount = 1;

	public ReplayControllerGui(MutablePicoContainer container, ReplayController controller) {
		this.controller = controller;
		panel = new JPanel();
		panel.setLayout(new WrapLayout());
		panel.setBorder(new TitledBorder("Replay Controls"));
		textBox = new TextFieldWithValidation<>(Integer::parseInt, i -> this.advanceAmount = i, "1");
		advanceButton = new JButton("Advance");
		controller.addCallback(this::refresh);
		advanceButton.addActionListener(e -> controller.advanceByAsync(advanceAmount));
		progressLabel = new JLabel();
		PropertiesFilePersistenceProvider provider = container.getComponent(PropertiesFilePersistenceProvider.class);
		if (provider != null) {
			JCheckBox cb = new BooleanSettingGui(provider.getReadOnlySetting(), "Don't Save Settings").getComponent();
			cb.setToolTipText("When using replays, it can be useful to block settings changes, as things like echo commands may change settings.\nAlso useful if you just want to play around while having an easy way to revert.");
			panel.add(cb);
		}
		panel.add(textBox);
		panel.add(advanceButton);
		panel.add(progressLabel);
		updateLabel();
	}

	private void refresh() {
		SwingUtilities.invokeLater(this::updateLabel);
	}

	private void updateLabel() {
		progressLabel.setText(String.format("Replay: %s / %s", controller.getCurrentPosition(), controller.getCount()));
	}

	public JPanel getPanel() {
		return panel;
	}

}
