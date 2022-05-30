package gg.xp.xivsupport.gui.tables.renderers;

import javax.swing.*;
import java.awt.*;

public class AutoHeightScalingIcon extends JComponent {

	private ScaledImageComponent img;

	public AutoHeightScalingIcon(ScaledImageComponent img) {
		this.img = img;
	}

	@Override
	public Dimension getPreferredSize() {
		if (isPreferredSizeSet()) {
			return super.getPreferredSize();
		}
		else {
			return new Dimension(img.getCurrentSize(), img.getCurrentSize());
		}
	}

	@Override
	public void validate() {
		int desiredHeight = getHeight();
		int currentHeight = img.getCurrentSize();
		if (currentHeight != desiredHeight) {
			img = img.withNewSize(desiredHeight);
		}
	}

	@Override
	public void paint(Graphics g) {
		img.paint(g);
	}
}
