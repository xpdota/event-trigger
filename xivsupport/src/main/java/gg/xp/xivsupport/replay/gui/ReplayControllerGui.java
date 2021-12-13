package gg.xp.xivsupport.replay.gui;

import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.replay.ReplayController;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public final class ReplayControllerGui {

	private final ReplayController controller;
	private final JPanel panel;
	private final JButton advanceButton;
	private final TextFieldWithValidation<Integer> textBox;
	private final JLabel progressLabel;
	private int advanceAmount = 1;

	public ReplayControllerGui(ReplayController controller) {
		this.controller = controller;
		panel = new JPanel();
		panel.setLayout(new WrapLayout());
		panel.setBorder(new TitledBorder("Replay Controls"));
		textBox = new TextFieldWithValidation<>(Integer::parseInt, i -> this.advanceAmount = i, "1");
		advanceButton = new JButton("Advance");
		advanceButton.addActionListener(e -> controller.advanceByAsync(advanceAmount, this::refresh));
		progressLabel = new JLabel();
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
