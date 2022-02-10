package gg.xp.xivsupport.gui.tables.renderers;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class TimelineBar extends JComponent {

	private final JLabel label = new JLabel("", SwingConstants.LEFT);
	private final JLabel rightLabel = new JLabel("", SwingConstants.LEFT);

	private Color color1;
	private Color color2;
	private Color color3;
	private Color borderColor;
	private Color textColor;
	private double percent1;
	private double percent2;
	private String[] textOptions;
	private ScaledImageComponent icon;

	public TimelineBar() {
		setLeftTextOptions("");
		add(label);
		label.setOpaque(false);
		add(rightLabel);
	}

	public void setLeftTextOptions(String... textOptions) {
		if (textOptions.length == 0) {
			throw new IllegalArgumentException("Must specify text");
		}
		this.textOptions = textOptions;
	}

	public void setRightText(String rightText) {
		rightLabel.setText(rightText);
		rightLabel.revalidate();
	}

	public void seticon(ScaledImageComponent component) {
		this.icon = component;
	}

	public Color getColor1() {
		return color1;
	}

	public void setColor1(Color color1) {
		this.color1 = color1;
	}

	public Color getColor2() {
		return color2;
	}

	public void setColor2(Color color2) {
		this.color2 = color2;
	}

	public Color getColor3() {
		return color3;
	}

	public void setColor3(Color color3) {
		this.color3 = color3;
	}

	public Color getBorderColor() {
		return borderColor;
	}

	public void setBorderColor(Color borderColor) {
		this.borderColor = borderColor;
	}

	public double getPercent1() {
		return percent1;
	}

	public void setPercent1(double percent1) {
		this.percent1 = percent1;
	}

	public double getPercent2() {
		return percent2;
	}

	public void setPercent2(double percent2) {
		this.percent2 = percent2;
	}

	private void checkLabel() {
		// Leave some space on the edges
		int space = 5;
		int leftBound = space;
		int fullWidth = getWidth();
		int rightBound = fullWidth - space;
		// TODO: just redo this to make the left + right components both dynamic
		if (icon != null) {
			leftBound += icon.getPreferredSize().width;
		}
		int rightWidth = rightLabel.getPreferredSize().width;
		rightBound -= rightWidth;
		int remainingWidth = rightBound - leftBound;
		for (String text : textOptions) {
			label.setText(text);
			if (label.getPreferredSize().width <= (remainingWidth - (2 * getBorderWidth()))) {
				break;
			}
		}
		int height = getHeight();
		label.setBounds(leftBound, 0, remainingWidth, height);
		rightLabel.setBounds(fullWidth - rightWidth - space, 0, rightWidth + space, height);
//		rightLabel.revalidate();
	}

	int getBorderWidth() {
		return borderColor == null ? 0 : 1;
	}

	@Override
	public void revalidate() {
		checkLabel();
	}

	@Override
	public void validate() {
		checkLabel();
	}

	@Override
	protected void paintChildren(Graphics g) {
		super.paintChildren(g);
		if (icon != null) {
			icon.paint(g);
		}
	}

	@SuppressWarnings("SuspiciousNameCombination")
	@Override
	protected void paintComponent(Graphics g) {
		AffineTransform old = ((Graphics2D) g).getTransform();
		AffineTransform t = new AffineTransform(old);
		double xScale = t.getScaleX();
		double yScale = t.getScaleY();
//		t.scale(1 / xScale, 1 / yScale);
		t.setTransform(1.0, 0, 0, 1.0, Math.round(t.getTranslateX()), Math.round(t.getTranslateY()));
		((Graphics2D) g).setTransform(t);
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

		if (borderColor != null) {
			g.setColor(borderColor);
			g.drawRect(0, 0, realWidth - 1, realHeight - 1);
		}

		((Graphics2D) g).setTransform(old);
	}

	public Color getTextColor() {
		return textColor;
	}

	public void setTextColor(Color textColor) {
		this.textColor = textColor;
	}
}
