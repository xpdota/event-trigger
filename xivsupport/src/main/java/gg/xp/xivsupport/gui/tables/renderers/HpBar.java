package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.CurrentMaxPairImpl;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class HpBar extends JComponent {

	private static final Color predictedDead = new Color(108, 7, 7);
	private static final Color actualDead = new Color(49, 7, 7);

	public static final Color defaultBgColor = new Color(60, 63, 65);
	public static final Color defaultShieldColor = new Color(199, 176, 60);
	public static final Color defaultEmptyGradientColor = Color.getHSBColor(0.0f, 0.36f, 0.52f);
	public static final Color defaultFullGradientColor = Color.getHSBColor(0.33f, 0.36f, 0.52f);
	public static final Color defaultFullHpColor = Color.getHSBColor(0.33f, 0.50f, 0.52f);
	public static final Color defaultTextColor = new Color(187, 187, 187);

	private Color emptyGradientColor = defaultEmptyGradientColor;
	private Color fullGradientColor = defaultFullGradientColor;
	private Color shieldColor = defaultShieldColor;
	private Color fullHpColor = defaultFullHpColor;

	private Color effectiveBaseHpColor;
	private Color effectiveDiffColor;
	private Color effectiveEmptyColor;
	private Color effectiveShieldColor;
	private @Nullable Color borderColor;
	// Take care of this internally
	private @Nullable Color bgColor = defaultBgColor;
	//	private Color textColor;
	private double basePercentDisplay;
	private double diffPercent;
	private double shieldPercent;
	private boolean display;
	private int fgTransparency = 255;
	private int bgTransparency = 255;

	private final FractionDisplayHelper textDelegate = new FractionDisplayHelper();

	public HpBar() {
		add(textDelegate);
		setTextColor(defaultTextColor);
	}

	public void setTextMode(BarFractionDisplayOption textMode) {
		textDelegate.setDisplayMode(textMode);
	}

	public void setEmptyGradientColor(Color emptyGradientColor) {
		this.emptyGradientColor = emptyGradientColor;
	}

	public void setFullGradientColor(Color fullGradientColor) {
		this.fullGradientColor = fullGradientColor;
	}

	public void setShieldColor(Color shieldColor) {
		this.shieldColor = shieldColor;
	}

	public void setFgTransparency(int fgTransparency) {
		this.fgTransparency = fgTransparency;
	}

	public void setBgTransparency(int bgTransparency) {
		this.bgTransparency = bgTransparency;
	}

	public void setFullHpColor(Color fullHpColor) {
		this.fullHpColor = fullHpColor;
	}

	public void setTextColor(Color textColor) {
		textDelegate.setForeground(textColor);
	}

	@Override
	public void setBackground(Color bg) {
		this.bgColor = bg;
	}

	private void calcShieldColor() {
		effectiveShieldColor = RenderUtils.withAlpha(shieldColor, fgTransparency);
	}

	public void setData(XivCombatant cbt, long diff) {
		if (cbt == null) {
			setData(null, 0, 0);
		}
		else {
			setData(cbt.getHp(), diff, cbt.getShieldAmount());
		}
	}

	public void setData(HitPoints hp, long diff, long shield) {
		if (hp == null || hp.max() == 0) {
			display = false;
		}
		else {
			display = true;
			double percent;
			double percentChange;
			long actualMax = hp.max();
			long actualCurrent = hp.current();
			long actualPredicted = actualCurrent + diff;
			if (actualMax < actualCurrent) {
				actualCurrent = actualMax;
			}
			int effectiveMax = (int) actualMax;
			int effectiveCurrent = (int) actualCurrent;
			int effectivePredicted = (int) actualPredicted;
			percent = effectiveCurrent / (double) effectiveMax;
			shieldPercent = shield / (double) effectiveMax;
			if (shieldPercent > 1) {
				shieldPercent = 1;
			}
			percentChange = (effectivePredicted - effectiveCurrent) / (double) effectiveMax;
			effectiveBaseHpColor = computeBarColor(percent);
			effectiveEmptyColor = getBackgroundColor();
			borderColor = computeBorderColor(percent, actualPredicted);
			calcShieldColor();

			if (percentChange == 0) {
				// No predicted change - logic is easy:
				// | Current | Max - Current |
				basePercentDisplay = percent;
				diffPercent = 0;
			}
			else {
				// Predicted higher than current
				// | Current | ΔPredicted | Max - (Current + Predicted) |
				effectiveDiffColor = computeMovementBarColor(percent, percentChange);
				if (percentChange > 0) {
					if (percent + percentChange > 1) {
						// Cap current + predicted to 100% since you can't overheal
						percentChange = 1 - percent;
					}

					basePercentDisplay = percent;
					diffPercent = percentChange;
				}
				else {
					// Predicted lower than current
					// | Current - ΔPredicted | ΔPredicted | Max - (Current) |
					if (percent + percentChange < 0) {
						// Don't let it go below zero
						percentChange = -1 * percent;
					}

					basePercentDisplay = percent + percentChange;
					diffPercent = -1 * percentChange;
				}
			}

			long displayAmount = actualPredicted < 0 ? 0 : actualPredicted;
			textDelegate.setValue(new CurrentMaxPairImpl(displayAmount, actualMax));
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

	private int getBorderWidth() {
		return 1;
	}

	private void setTextBounds() {
		Rectangle bounds = getBounds();
		int bw = getBorderWidth();
		textDelegate.setBounds(bw, bw, bounds.width - 2 * bw, bounds.height - 2 * bw);
		textDelegate.validate();
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		setTextBounds();
	}

	@Override
	public void revalidate() {
		setTextBounds();
	}

	@Override
	public void validate() {
		setTextBounds();
	}

	@Override
	public void paint(Graphics g) {
		// Skip painting, including children, if data is null
		if (display) {
			super.paint(g);
			int foo = 1+2;
		}
	}

	@Override
	protected void paintChildren(Graphics g) {
		textDelegate.paint(g);
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
		int width1 = (int) (innerWidth * basePercentDisplay);
		int width2 = (int) (innerWidth * diffPercent);
		int width3 = innerWidth - width1 - width2;
		int shieldWidth = (int) (innerWidth * shieldPercent);

		// TODO: missing 1px at the right of the bar
		if (width1 > 0) {
			g.setColor(effectiveBaseHpColor);
			g.fillRect(borderWidth, borderWidth, width1, innerHeight);
		}

		if (width2 > 0) {
			g.setColor(effectiveDiffColor);
			g.fillRect(width1 + borderWidth, borderWidth, width2 + borderWidth, innerHeight);
		}

		if (width3 > 0) {
			g.setColor(effectiveEmptyColor);
			g.fillRect(width1 + width2 + borderWidth, borderWidth, width3 + borderWidth, innerHeight);
		}

		if (borderColor != null) {
			g.setColor(borderColor);
			g.drawRect(0, 0, realWidth - 1, realHeight - 1);
		}

		if (shieldWidth > 0) {
			int shieldY = (int) (innerHeight * 0.83);
			int shieldHeight = realHeight - borderWidth - shieldY;
			// Try to bump against bar if possible, otherwise right-align
			int shieldX = borderWidth + (width3 >= shieldWidth ? innerWidth - width3 : innerWidth - shieldWidth);
			g.setColor(effectiveShieldColor);
			g.fillRect(shieldX, shieldY, shieldWidth, shieldHeight);
		}


		g.setTransform(old);
	}

	private Color computeBarColor(double percent) {
		if (percent >= 0.9999) {
			return RenderUtils.withAlpha(fullHpColor, fgTransparency);
		}
		Color empty = emptyGradientColor;
		Color full = fullGradientColor;
		float[] emptyHsb = Color.RGBtoHSB(empty.getRed(), empty.getGreen(), empty.getBlue(), null);
		float[] fullHsb = Color.RGBtoHSB(full.getRed(), full.getGreen(), full.getBlue(), null);
		float[] blended = new float[3];
		for (int i = 0; i < 3; i++) {
			blended[i] = (float) (percent * fullHsb[i] + (emptyHsb[i] * (1.0f - percent)));
		}
		Color raw = Color.getHSBColor(blended[0], blended[1], blended[2]);
		return new Color(raw.getRed(), raw.getGreen(), raw.getBlue(), fgTransparency);
	}

	private Color computeMovementBarColor(double percent, double percentChange) {
		if (percentChange > 0) {
			return new Color(64, 128, 64, fgTransparency);
		}
		else {
			if (percent + percentChange < 0) {
				return new Color(128, 16, 16, fgTransparency);
			}
			else {
				return new Color(64, 16, 16, fgTransparency);
			}
		}
	}

	private Color computeBorderColor(double percent, long predicted) {
		if (percent == 0.0) {
			return actualDead;
		}
		else if (predicted <= 0) {
			return predictedDead;
		}
		else {
			Color bg = this.bgColor;
			if (bg == null) {
				return TRANSPARENT;
			}
			return bg;
		}
	}

//	public void setTextColor(Color textColor) {
//		this.textColor = textColor;
//	}
}
