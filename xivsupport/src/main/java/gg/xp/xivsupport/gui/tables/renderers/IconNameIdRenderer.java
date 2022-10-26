package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.data.*;

import javax.swing.*;
import java.awt.*;

public class IconNameIdRenderer extends JPanel {

	private final JLabel mainLabel;
	private final JLabel idLabel;

	public IconNameIdRenderer() {
		super(false);
		setLayout(new BorderLayout());
		mainLabel = new JLabel();
		idLabel = new JLabel();
		Font defaultFont = idLabel.getFont();
		idLabel.setFont(new Font("Monospaced", defaultFont.getStyle(), defaultFont.getSize()));
		reset();
	}

	public void reset() {
		removeAll();
		add(mainLabel, BorderLayout.CENTER);
		add(idLabel, BorderLayout.EAST);
	}

	public void setIcon(HasIconURL iconUrl) {
		if (iconUrl == null) {
			return;
		}
		ScaledImageComponent icon = IconTextRenderer.getIconOnly(iconUrl);
		if (icon != null) {
			add(icon, BorderLayout.WEST);
		}
	}

	public void setMainText(String text) {
		if (text == null) {
			mainLabel.setVisible(false);
		}
		else {
			mainLabel.setVisible(true);
			mainLabel.setText(text);
		}
	}

	public void setIdText(String text) {
		if (text == null) {
			idLabel.setVisible(false);
		}
		else {
			idLabel.setVisible(true);
			// TODO: space is dumb hack to make padding
			idLabel.setText(text + "  ");
		}
	}

	public void setIdAlpha(int alpha) {
		idLabel.setForeground(RenderUtils.withAlpha(idLabel.getForeground(), alpha));
	}

	public void formatFrom(Component other) {
		setBackground(other.getBackground());
	}

	// Not supporting this yet
//	public void setIconNoCache(HasIconURL icon) {
//		add(IconTextRenderer.getComponent(icon), BorderLayout.WEST);
//
//	}


}
