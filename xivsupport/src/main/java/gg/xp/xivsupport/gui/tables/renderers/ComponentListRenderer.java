package gg.xp.xivsupport.gui.tables.renderers;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;

public class ComponentListRenderer extends JComponent {
	private static final long serialVersionUID = -124182066710853800L;
	private List<Component> components;
	private final int xPadding;

	public ComponentListRenderer(int xPadding) {
		this.xPadding = xPadding;
		this.setOpaque(false);
	}

	public void setComponents(List<Component> components) {
		this.components = components;
	}

	// TODO: should this be paint, or paintcomponent?
	@Override
	public void paint(Graphics g) {
		Color background = getBackground();
		Rectangle bounds = getBounds();
		int cellHeight = bounds.height;
		int cellWidth = bounds.width;
		Graphics2D graphics = ((Graphics2D) g);
		AffineTransform transform = graphics.getTransform();
		if (background != null) {
			g.setColor(background);
			g.fillRect(0, 0, cellWidth, cellHeight);
		}
		int curX = 0;
		for (Component component : components) {
			Dimension prefSize = component.getPreferredSize();
			int prefWidth = prefSize.width;
			int prefHeight = prefSize.height;
			int actualHeight = Math.min(prefHeight, cellHeight);
			int remainingX = cellWidth - curX;
			int actualWidth = Math.min(prefWidth, remainingX);
			if (actualWidth <= 0) {
				break;
			}
			component.setBounds(curX, 0, actualWidth, actualHeight);
			component.paint(g);
			int delta = actualWidth + xPadding;
			transform.translate(delta, 0);
			graphics.setTransform(transform);
//			g.translate(delta, 0);
			curX += delta;
		}
	}


	@Override
	public boolean isOpaque() {
		return true;
	}

	@Override
	public void validate() {
//		super.validate();
	}

	@Override
	public void revalidate() {
//		super.revalidate();
	}

	@Override
	public void invalidate() {
//		super.invalidate();
	}

	@Override
	protected void paintChildren(Graphics g) {
//		super.paintChildren(g);
	}
}
