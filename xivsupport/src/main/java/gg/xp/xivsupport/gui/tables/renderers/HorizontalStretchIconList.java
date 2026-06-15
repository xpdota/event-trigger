package gg.xp.xivsupport.gui.tables.renderers;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.Serial;
import java.util.Collections;
import java.util.List;

/**
 * Similar to {@link ComponentListStretchyRenderer}, but used for situations where you might not know the desired height
 * beforehand.
 * <p>
 * This class is purpose-built for putting multiple icons into a visual callout, so it likely isn't very useful outside of that purpose.
 */
public class HorizontalStretchIconList extends JComponent {
	@Serial
	private static final long serialVersionUID = -124182066710853800L;
	private List<AutoHeightScalingIcon> components = Collections.emptyList();
	private final float aspectRatio;
	private Color bg;

	public HorizontalStretchIconList(float aspectRatio) {
		this.aspectRatio = aspectRatio;
	}

	@Override
	public Dimension getPreferredSize() {
		if (!isPreferredSizeSet()) {
			// The only part that matters is the aspect ratio
			// 20 is a placeholder value
			int count = components.size();
			// 20 px per component (assumes 1:1 aspect ratio - this is corrected via xPadding)
			// Then factor in xPadding
			return new Dimension((int) (count * 20 * aspectRatio), 20);
		}
		return super.getPreferredSize();
	}

	public void reset() {
		this.components = Collections.emptyList();
		this.bg = null;
		this.setToolTipText(null);
	}

	public void setComponents(List<AutoHeightScalingIcon> components) {
		this.components = components;
	}

	@Override
	public void setBackground(Color bg) {
		this.bg = bg;
	}

	@Override
	public void paint(Graphics g) {
		Color background = bg;
		Rectangle bounds = getBounds();
		int cellHeight = bounds.height;
		int cellWidth = bounds.width;
		Graphics2D graphics = ((Graphics2D) g);
		AffineTransform transform = graphics.getTransform();
		if (background != null) {
			g.setColor(background);
			g.fillRect(0, 0, cellWidth, cellHeight);
		}
//		if (rightToLeft) {
//			// TODO: do we need to remove 1 from the right edge?
//			int curX = 0;
//			transform.translate(cellWidth, 0);
//			for (Component component : components) {
//				Dimension prefSize = component.getPreferredSize();
//				int prefWidth = prefSize.width;
//				int prefHeight = prefSize.height;
//				int actualHeight = Math.min(prefHeight, cellHeight);
//				int remainingX = cellWidth - curX;
//				int actualWidth = Math.min(prefWidth, remainingX);
//				if (actualWidth <= 0) {
//					break;
//				}
//				// TODO: the left component will be weird
//				component.setBounds((cellWidth - curX) - actualWidth, 0, actualWidth, actualHeight);
//				transform.translate(-actualWidth, 0);
//				graphics.setTransform(transform);
//				component.paint(g);
//				int delta = actualWidth + xPadding;
//				transform.translate(-xPadding, 0);
//				graphics.setTransform(transform);
//
////			g.translate(delta, 0);
//				curX += delta;
//			}
//		}
//		else {
		int curX = 0;
		for (AutoHeightScalingIcon component : components) {
			int prefWidth = Math.floorDiv(cellWidth, components.size());
			int remainingX = cellWidth - curX;
			int actualWidth = Math.min(prefWidth, remainingX);
			if (actualWidth <= 0) {
				break;
			}
			component.setBounds(curX, 0, actualWidth, cellHeight);
			component.validate();
			component.paint(g);
			int delta = actualWidth;
			transform.translate(delta, 0);
			graphics.setTransform(transform);
//			g.translate(delta, 0);
			curX += delta;
		}
//		}
	}


	@Override
	public boolean isOpaque() {
		return getBackground() != null;
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
