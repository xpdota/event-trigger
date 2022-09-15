package gg.xp.xivsupport.gui.components;

import gg.xp.xivsupport.persistence.settings.ResetMenuOption;
import gg.xp.xivsupport.persistence.settings.Resettable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.Serial;
import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorChooser implements Resettable {

	private final JButton button;
	private final JLabel label;
	private final Color noColorSelected = new Color(0, 0, 0, 0);
	private final Supplier<@Nullable Color> getter;
	private final Consumer<@Nullable Color> setter;

	public ColorChooser(String label, Supplier<@Nullable Color> getter, Consumer<@Nullable Color> setter, Supplier<Boolean> enabled) {
		this.getter = getter;
		this.setter = setter;
		this.label = new JLabel(label);
		this.button = new JButton("") {
			@Override
			public Color getBackground() {
				Color bgFromSetting = getter.get();
				return bgFromSetting == null ? noColorSelected : bgFromSetting;
			}

			@Override
			public boolean isEnabled() {
				return enabled.get();
			}
		};

		button.addActionListener(l -> {
			if (button.isEnabled()) {
				Color color = showDialog(button, label, getter.get());
				if (color != null) {
					setter.accept(color);
					button.repaint();
				}
			}
		});
		button.setPreferredSize(new Dimension(50, 20));
		button.setComponentPopupMenu(ResetMenuOption.resetOnlyMenu(this, this::delete));
	}

	public JButton getButtonOnly() {
		return button;
	}

	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		panel.add(label);
		panel.add(button);
		return panel;
	}

	public Component getComponentReversed() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		panel.add(button);
		panel.add(label);
		return panel;
	}

	@Override
	public void delete() {
		setter.accept(null);
		button.repaint();
	}

	@Override
	public boolean isSet() {
		return getter.get() != null;
	}

	private static @Nullable JColorChooser chooser;

	@SuppressWarnings("NonThreadSafeLazyInitialization") // Always called from UI thread
	private static Color showDialog(Component component, String title, @Nullable Color initialColor) {


//		final JColorChooser chooser = new JColorChooser(initialColor != null ? initialColor : Color.white);
		if (chooser == null) {
			chooser = new JColorChooser(initialColor != null ? initialColor : Color.white);

			for (AbstractColorChooserPanel ccPanel : chooser.getChooserPanels()) {
				ccPanel.setColorTransparencySelectionEnabled(true);
			}
		}
		if (initialColor != null) {
			chooser.setColor(initialColor);
		}

		ColorTracker ok = new ColorTracker(chooser);
		JDialog dialog = JColorChooser.createDialog(component, title, true, chooser, ok, null);

		dialog.addComponentListener(new DisposeOnClose());

		// Blocks until user selects something
		dialog.setVisible(true);

		// This will return null if the user cancelled
		return ok.getColor();
	}

	static class ColorTracker implements ActionListener, Serializable {
		@Serial
		private static final long serialVersionUID = 4374545664445189732L;
		JColorChooser chooser;
		Color color;

		public ColorTracker(JColorChooser c) {
			chooser = c;
		}

		public void actionPerformed(ActionEvent e) {
			color = chooser.getColor();
		}

		public Color getColor() {
			return color;
		}
	}

	static class DisposeOnClose extends ComponentAdapter implements Serializable {
		@Serial
		private static final long serialVersionUID = 4081298723879619143L;

		public void componentHidden(ComponentEvent e) {
			Window w = (Window) e.getComponent();
			w.dispose();
		}
	}
}

