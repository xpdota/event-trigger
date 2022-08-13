package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class MpBar extends JComponent {

	public static final Color defaultMpColor = new Color(128, 128, 255, 255);
	public static final Color defaultBgColor = new Color(60, 63, 65);
	private static final Color borderNormal = defaultMpColor.darker();
	private static final Color borderEmpty = new Color(60, 63, 65, 255);

	private Color mpColor = defaultMpColor;

	private Color effectiveMpColor;
	private Color effectiveEmpty;
	private @Nullable Color effectiveBorderColor;
	// Take care of this internally
	private @Nullable Color bgColor = defaultBgColor;
	//	private Color textColor;
	private double basePercentDisplay;
	private boolean display;
	private int fgTransparency = 255;
	private int bgTransparency = 255;

	private final FractionDisplayHelper textDelegate = new FractionDisplayHelper();

	public MpBar() {
//		setOpaque(false);
		add(textDelegate);
	}

	public void setTextMode(BarFractionDisplayOption textMode) {
		textDelegate.setDisplayMode(textMode);
	}

	public void setTextColor(Color textColor) {
		textDelegate.setForeground(textColor);
	}

	public void setFgTransparency(int fgTransparency) {
		this.fgTransparency = fgTransparency;
	}

	public void setBgTransparency(int bgTransparency) {
		this.bgTransparency = bgTransparency;
	}

	public void setMpColor(Color mpColor) {
		this.mpColor = mpColor;
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
			effectiveMpColor = getForegroundColor();
			effectiveEmpty = getBackgroundColor();
			effectiveBorderColor = effectiveCurrent > 0 ? borderNormal : borderEmpty;
			basePercentDisplay = percent;

			textDelegate.setValue(mp);
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

	private int getBorderWidth() {
		return 1;
	}

	private void setTextBounds() {
		Rectangle bounds = getBounds();
		int bw = getBorderWidth();
		textDelegate.setBounds(bw, bw, bounds.width - 2 * bw, bounds.height - 2 * bw);
	}

	@Override
	public void revalidate() {
		setTextBounds();
		textDelegate.revalidate();
	}

	@Override
	public void validate() {
		setTextBounds();
		textDelegate.validate();
	}

	@Override
	public void paint(Graphics g) {
		if (display) {
			super.paint(g);
		}
	}

	@SuppressWarnings("SuspiciousNameCombination")
	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		AffineTransform old = g.getTransform();
		AffineTransform t = new AffineTransform(old);
		double xScale = t.getScaleX();
		double yScale = t.getScaleY();
//		t.scale(1 / xScale, 1 / yScale);
		t.setTransform(1.0, 0, 0, 1.0, Math.floor(t.getTranslateX()), Math.floor(t.getTranslateY()));
		g.setTransform(t);
		int realWidth = (int) Math.floor(getWidth() * xScale);
		int realHeight = (int) Math.floor(getHeight() * yScale);
//		g.clearRect(0, 0, realWidth, realHeight);
		int borderWidth = getBorderWidth();
		int innerWidth = realWidth - 2 * borderWidth;
		int innerHeight = realHeight - 2 * borderWidth;
		int barWidth = (int) (innerWidth * basePercentDisplay);
		int emptyWidth = innerWidth - barWidth;

		// TODO: missing 1px at the right of the bar
		if (barWidth > 0) {
			g.setColor(effectiveMpColor);
			g.fillRect(borderWidth, borderWidth, barWidth, innerHeight);
		}

		if (emptyWidth > 0) {
			g.setColor(effectiveEmpty);
			g.fillRect(barWidth + borderWidth, borderWidth, emptyWidth + borderWidth, innerHeight);
		}

		if (effectiveBorderColor != null) {
			g.setColor(effectiveBorderColor);
			g.drawRect(0, 0, realWidth - 1, realHeight - 1);
		}

		g.setTransform(old);
	}

//	public void setTextColor(Color textColor) {
//		this.textColor = textColor;
//	}
}
