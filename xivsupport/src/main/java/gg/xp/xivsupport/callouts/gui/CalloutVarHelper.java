package gg.xp.xivsupport.callouts.gui;

import gg.xp.xivsupport.callouts.CalloutGroup;
import gg.xp.xivsupport.callouts.CalloutVar;
import gg.xp.xivsupport.callouts.CalloutVarHandle;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.StringSettingGui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static java.awt.Font.MONOSPACED;

public class CalloutVarHelper extends JPanel implements Scrollable {

	private final List<JCheckBox> showHides = new ArrayList<>();

	public CalloutVarHelper(List<CalloutGroup> groups) {
		// Padding around the edge so that it doesn't look weird
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		c.ipadx = 5;
		c.gridy = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;

		JPanel settingsPanel = new JPanel();
		JButton expandAll = new JButton("Expand All");
		expandAll.addActionListener(l -> setAllShowHide(true));
		settingsPanel.add(expandAll);

		JButton collapseAll = new JButton("Collapse All");
		collapseAll.addActionListener(l -> setAllShowHide(false));
		settingsPanel.add(collapseAll);

		this.add(settingsPanel, c);
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;

		groups.forEach(group -> {
			List<CalloutVarHandle> vars = group.getVars();
			if (vars.isEmpty()) {
				return;
			}

			c.gridx = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1;
			JPanel groupControls = new JPanel(new GridBagLayout());
			GridBagConstraints subC = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 10, 0);

			JLabel groupLabel = new JLabel(group.getName());
			groupControls.add(groupLabel, subC);

			JCheckBox showHide = new JCheckBox("Show/Hide");
			showHide.setSelected(true);
			subC.gridx++;
			subC.fill = GridBagConstraints.HORIZONTAL;
			subC.ipadx = 10;
			subC.weightx = 1;
			groupControls.add(showHide, subC);
			showHides.add(showHide);

			this.add(groupControls, c);

			vars.forEach(varHandle -> {
				// Start new line
				c.gridy++;
				// Line 1: 1x Indent; Name
				c.gridx = 0;
				c.weightx = 0;
				c.gridwidth = 1;
				Component indentStrut = Box.createHorizontalStrut(10);
				this.add(indentStrut, c);

				c.gridwidth = GridBagConstraints.REMAINDER;
				c.gridx = 1;
				c.weightx = 1;
				CalloutVar original = varHandle.getOriginal();
				var nameLabel = new ReadOnlyText("{ %s }".formatted(original.getName()), false);
				nameLabel.setFocusable(true);
				nameLabel.setFont(Font.getFont(MONOSPACED));
				this.add(nameLabel, c);

				List<Component> variableComponents = new ArrayList<>();
				variableComponents.add(nameLabel);
				variableComponents.add(indentStrut);

				// Line 2: Description
				String desc = original.getExtendedDescription();
				if (desc != null && !desc.isBlank()) {
					c.gridy++;
					c.gridx = 2;
					c.weightx = 1;
					c.gridwidth = GridBagConstraints.REMAINDER;
					ReadOnlyText descriptionComp = new ReadOnlyText(desc, false);
					descriptionComp.setFocusable(true);
					this.add(descriptionComp, c);
					variableComponents.add(descriptionComp);
				}

				// Line 3: Controls
				c.gridy++;
				c.gridwidth = 1;
				c.gridx = 2;
				c.weightx = 0;
				Component controlStrut = Box.createHorizontalStrut(10);
				this.add(controlStrut, c);
				variableComponents.add(controlStrut);

				c.gridx++;
				c.weightx = 1;
				JPanel ttsPanel = makeTtsPanel(varHandle);
				this.add(ttsPanel, c);
				variableComponents.add(ttsPanel);

				c.gridx++;
				c.weightx = 0;
				Component middleStrut = Box.createHorizontalStrut(10);
				this.add(middleStrut, c);
				variableComponents.add(middleStrut);

				c.gridx++;
				c.weightx = 1;
				JPanel textPanel = makeTextPanel(varHandle);
				this.add(textPanel, c);
				variableComponents.add(textPanel);

				c.gridx++;
				c.weightx = 0;
				Component rightStrut = Box.createHorizontalStrut(2);
				this.add(rightStrut, c);
				variableComponents.add(rightStrut);

				showHide.getModel().addChangeListener(l -> {
					boolean visible = showHide.isSelected();
					variableComponents.forEach(comp -> comp.setVisible(visible));
				});

				// Extra padding between entries
				c.gridy++;
				c.gridx = 0;
				c.gridwidth = GridBagConstraints.REMAINDER;
				Component bottomStrut = Box.createVerticalStrut(10);
				this.add(bottomStrut, c);
				variableComponents.add(bottomStrut);
			});
		});

		c.weighty = 1;
		c.gridx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add(new JPanel(), c);
	}

	private JPanel makeTtsPanel(CalloutVarHandle varHandle) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.add(new JLabel("TTS: "));
		panel.add(new StringSettingGui(varHandle.getValueSettingTts(), null).getTextBoxOnly());
		return panel;
	}

	private JPanel makeTextPanel(CalloutVarHandle varHandle) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.add(new JLabel("Text: "));
		JCheckBox sameTextCb = new BooleanSettingGui(varHandle.getSameText(), "Same as TTS:").getComponent();
		sameTextCb.setHorizontalTextPosition(SwingConstants.LEFT);
		panel.add(sameTextCb);
		panel.add(new StringSettingGui(varHandle.getValueSettingText(), null, () -> !varHandle.getSameText().get()).getTextBoxOnly());

		varHandle.getSameText().addListener(() -> SwingUtilities.invokeLater(panel::updateUI));

		return panel;
	}

	public void setAllShowHide(boolean showHide) {
		SwingUtilities.invokeLater(() -> showHides.forEach(sh -> sh.setSelected(showHide)));
	}

	public static boolean hasVars(List<CalloutGroup> groups) {
		return groups.stream().anyMatch(g -> !g.getVars().isEmpty());
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 20;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return (int) (getVisibleRect().height / 2.5);
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
}
