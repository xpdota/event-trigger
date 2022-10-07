package gg.xp.xivsupport.gui.tables.renderers;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class ResourceBar extends JComponent {

	private final JLabel label = new JLabel("", SwingConstants.CENTER);

	private Color color1;
	private Color color2;
	private Color color3;
	private Color tickColor;
	private @Nullable Color borderColor;
	private Color textColor;
	private double percent1;
	private double percent2;
	private String[] textOptions;
	private @Nullable TickRenderInfo bottomTicks;
	private @Nullable TickRenderInfo topTicks;

	public ResourceBar() {
		setTextOptions("");
		add(label);
		label.setOpaque(false);
	}

	public void setTextOptions(String... textOptions) {
		if (textOptions.length == 0) {
			throw new IllegalArgumentException("Must specify text");
		}
		this.textOptions = textOptions;
	}

	public void setColor1(Color color1) {
		this.color1 = color1;
	}

	public void setColor2(Color color2) {
		this.color2 = color2;
	}

	public void setColor3(Color color3) {
		this.color3 = color3;
	}

	public void setTickColor(Color tickColor) {
		this.tickColor = tickColor;
	}

	public void setBorderColor(@Nullable Color borderColor) {
		this.borderColor = borderColor;
	}

	public void setPercent1(double percent1) {
		this.percent1 = percent1;
	}

	public void setPercent2(double percent2) {
		this.percent2 = percent2;
	}

	private void checkLabel() {
		int width = getWidth();
		for (String text : textOptions) {
			label.setText(text);
			if (label.getPreferredSize().width <= (width - (2 * getBorderWidth()))) {
				break;
			}
		}
		label.setBounds(0, 0, getWidth(), getHeight());
	}

	int getBorderWidth() {
		return borderColor == null ? 0 : 1;
	}

	@Override
	public void revalidate() {
//		super.revalidate();
		checkLabel();
	}

	@Override
	public void validate() {
//		super.validate();
		checkLabel();
	}

	@SuppressWarnings("SuspiciousNameCombination")
	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = ((Graphics2D) graphics);
		AffineTransform old = g.getTransform();
		AffineTransform t = new AffineTransform(old);
		double xScale = t.getScaleX();
		double yScale = t.getScaleY();
//		t.scale(1 / xScale, 1 / yScale);
		t.setTransform(1.0, 0, 0, 1.0, Math.round(t.getTranslateX()), Math.round(t.getTranslateY()));
		g.setTransform(t);
		int realWidth = (int) Math.floor(getWidth() * xScale);
		int realHeight = (int) Math.floor(getHeight() * yScale);
		int borderWidth = getBorderWidth();
		int innerWidth = realWidth - (2 * borderWidth);
		int innerHeight = realHeight - (2 * borderWidth);
		int width1 = (int) (innerWidth * percent1);
		int width2 = (int) (innerWidth * percent2);
		int width3 = innerWidth - width1 - width2;

		// TODO: missing 1px at the right of the bar
		if (width1 > 0) {
			g.setColor(color1);
			g.fillRect(borderWidth, borderWidth, width1, innerHeight);
		}

		if (width2 > 0) {
			g.setColor(color2);
			g.fillRect(width1 + borderWidth, borderWidth, width2 + borderWidth, innerHeight);
		}

		if (width3 > 0) {
			g.setColor(color3);
			g.fillRect(width1 + width2 + borderWidth, borderWidth, width3 + borderWidth, innerHeight);
		}

		if (bottomTicks != null) {
			if (tickColor == null) {
				g.setColor(label.getForeground());
			}
			else {
				g.setColor(tickColor);
			}
			double current = bottomTicks.offset();
			while (current < 1.0d) {
				int x = (int) (borderWidth + innerWidth * current);
				int top = (int) ((4.0d * realHeight / 5.0d) - borderWidth);
				int height = realHeight - top - borderWidth;
				g.fillRect(x, top, 3, height);
				current += bottomTicks.interval();
			}
		}
		if (topTicks != null) {
			if (tickColor == null) {
				g.setColor(label.getForeground());
			}
			else {
				g.setColor(tickColor);
			}
			double current = topTicks.offset();
			while (current < 1.0d) {
				int x = (int) (borderWidth + innerWidth * current);
				int height = (int) ((1.0d * realHeight / 5.0d));
				int top = borderWidth;
				g.fillRect(x, top, 3, height);
				current += topTicks.interval();
			}
		}

		if (borderColor != null) {
			g.setColor(borderColor);
			g.drawRect(0, 0, realWidth - 1, realHeight - 1);
		}

		g.setTransform(old);
	}

	public void setTextColor(Color textColor) {
		this.label.setForeground(textColor);
	}

	public void setBottomTicks(@Nullable TickRenderInfo bottomTicks) {
		this.bottomTicks = bottomTicks;
	}

	public void setTopTicks(@Nullable TickRenderInfo topTicks) {
		this.topTicks = topTicks;
	}
}
