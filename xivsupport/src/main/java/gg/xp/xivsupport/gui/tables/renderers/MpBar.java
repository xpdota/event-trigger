package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class MpBar extends JComponent {

	private static final Color mpColor = new Color(128, 128, 255, 255);

	private Color baseHpColor;
	private Color emptyColor;
	private @Nullable Color borderColor;
	// Take care of this internally
	private @Nullable Color bgColor = new Color(60, 63, 65);
	//	private Color textColor;
	private double basePercentDisplay;
	private String[] textOptions;
	private boolean display;
	private int fgTransparency = 255;
	private int bgTransparency = 255;
	private final JLabel label = new JLabel("", SwingConstants.CENTER) {
		@Override
		public boolean isVisible() {
			return display;
		}
	};

	public MpBar() {
//		setOpaque(false);
		setTextOptions("");
		add(label);
		label.setOpaque(false);
	}

	public void setFgTransparency(int fgTransparency) {
		this.fgTransparency = fgTransparency;
	}

	public void setBgTransparency(int bgTransparency) {
		this.bgTransparency = bgTransparency;
	}

	@Override
	public void setBackground(Color bg) {
		this.bgColor = bg;
	}

	public void setData(XivCombatant cbt) {
		if (cbt == null) {
			setData((ManaPoints) null);
		}
		else {
			setData(cbt.getMp());
		}
	}

	public void setData(ManaPoints mp) {
		if (mp == null || mp.max() == 0) {
			display = false;
		}
		else {
			display = true;
			double percent;
			long actualMax = mp.max();
			long actualCurrent = mp.current();
			if (actualMax < actualCurrent) {
				actualCurrent = actualMax;
			}
			int effectiveMax = (int) actualMax;
			int effectiveCurrent = (int) actualCurrent;
			percent = effectiveCurrent / (double) effectiveMax;
			baseHpColor = getForegroundColor();
			emptyColor = getBackgroundColor();
			borderColor = effectiveCurrent > 0 ? mpColor : emptyColor;
			basePercentDisplay = percent;

			String longText = String.format("%s / %s", effectiveCurrent, effectiveMax);
			setTextOptions(longText, String.valueOf(effectiveCurrent));
		}
	}

	private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

	private Color getBackgroundColor() {
		Color bg = this.bgColor;
		if (bg == null) {
			return TRANSPARENT;
		}
		return RenderUtils.withAlpha(bg, bgTransparency);
	}

	private Color getForegroundColor() {
		Color fg = mpColor;
		if (fg == null) {
			return TRANSPARENT;
		}
		return RenderUtils.withAlpha(fg, fgTransparency);
	}

	private void setTextOptions(String... textOptions) {
		if (textOptions.length == 0) {
			throw new IllegalArgumentException("Must specify text");
		}
		this.textOptions = textOptions;
	}

	private void checkLabel() {
		int width = getWidth();
		for (String text : textOptions) {
			label.setText(text);
			if (label.getPreferredSize().width <= width - 2 * getBorderWidth()) {
				break;
			}
		}
		label.setBounds(0, 0, getWidth(), getHeight());
	}

	int getBorderWidth() {
		return 1;
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
		if (!display) {
			return;
		}
		Graphics2D g = (Graphics2D) graphics;
		AffineTransform old = g.getTransform();
		AffineTransform t = new AffineTransform(old);
		double xScale = t.getScaleX();
		double yScale = t.getScaleY();
//		t.scale(1 / xScale, 1 / yScale);
		t.setTransform(1.0, 0, 0, 1.0, Math.round(t.getTranslateX()), Math.round(t.getTranslateY()));
		g.setTransform(t);
		int realWidth = (int) Math.floor(getWidth() * xScale);
		int realHeight = (int) Math.floor(getHeight() * yScale);
//		g.clearRect(0, 0, realWidth, realHeight);
		int borderWidth = getBorderWidth();
		int innerWidth = realWidth - 2 * borderWidth;
		int innerHeight = realHeight - 2 * borderWidth;
		int width1 = (int) (innerWidth * basePercentDisplay);
		int width3 = innerWidth - width1;

		// TODO: missing 1px at the right of the bar
		if (width1 > 0) {
			g.setColor(baseHpColor);
			g.fillRect(borderWidth, borderWidth, width1, innerHeight);
		}

		if (width3 > 0) {
			g.setColor(emptyColor);
			g.fillRect(width1 + borderWidth, borderWidth, width3 + borderWidth, innerHeight);
		}

		if (borderColor != null) {
			g.setColor(borderColor);
			g.drawRect(0, 0, realWidth - 1, realHeight - 1);
		}

		g.setTransform(old);
	}

//	public void setTextColor(Color textColor) {
//		this.textColor = textColor;
//	}
}
