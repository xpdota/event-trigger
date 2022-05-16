package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class HpBar extends JComponent {

	private final JLabel label = new JLabel("", SwingConstants.CENTER);
	private static final Color predictedDead = new Color(108, 7, 7);
	private static final Color actualDead = new Color(49, 7, 7);
	private static final Color defaultShieldColor = new Color(199, 176, 60);

	private Color baseHpColor;
	private Color diffColor;
	private Color emptyColor;
	private Color shieldColor;
	private @Nullable Color borderColor;
	// Take care of this internally
	private @Nullable Color bgColor = new Color(60, 63, 65);
	//	private Color textColor;
	private double basePercent;
	private double basePercentDisplay;
	private double diffPercent;
	private double shieldPercent;
	private String[] textOptions;
	private boolean display;
	private int fgTransparency = 255;
	private int bgTransparency = 255;

	public HpBar() {
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

	private void calcShieldColor() {
		shieldColor = RenderUtils.withAlpha(defaultShieldColor, fgTransparency);
	}

	public void setData(XivCombatant cbt, long diff) {
		setData(cbt.getHp(), diff, cbt.getShieldAmount());
	}

	public void setData(HitPoints hp, long diff, long shield) {
		if (hp == null || hp.getMax() == 0) {
			display = false;
		}
		else {
			display = true;
			double percent;
			double percentChange;
			long actualMax = hp.getMax();
			long actualCurrent = hp.getCurrent();
			long actualPredicted = actualCurrent + diff;
			if (actualMax < actualCurrent) {
				actualCurrent = actualMax;
			}
			int effectiveMax = (int) actualMax;
			int effectiveCurrent = (int) actualCurrent;
			int effectivePredicted = (int) actualPredicted;
			percent = effectiveCurrent / (double) effectiveMax;
			basePercent = percent;
			shieldPercent = shield / (double) effectiveMax;
			if (shieldPercent > 1) {
				shieldPercent = 1;
			}
			percentChange = (effectivePredicted - effectiveCurrent) / (double) effectiveMax;
			baseHpColor = computeBarColor(percent);
			emptyColor = getBackgroundColor();
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
				diffColor = computeMovementBarColor(percent, percentChange);
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
			String longText = String.format("%s / %s", displayAmount, actualMax);
			setTextOptions(longText, String.valueOf(displayAmount));
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
		int width2 = (int) (innerWidth * diffPercent);
		int width3 = innerWidth - width1 - width2;
		int shieldWidth = (int) (innerWidth * shieldPercent);

		// TODO: missing 1px at the right of the bar
		if (width1 > 0) {
			g.setColor(baseHpColor);
			g.fillRect(borderWidth, borderWidth, width1, innerHeight);
		}

		if (width2 > 0) {
			g.setColor(diffColor);
			g.fillRect(width1 + borderWidth, borderWidth, width2 + borderWidth, innerHeight);
		}

		if (width3 > 0) {
			g.setColor(emptyColor);
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
			g.setColor(shieldColor);
			g.fillRect(shieldX, shieldY, shieldWidth, shieldHeight);
		}


		g.setTransform(old);
	}

	private Color computeBarColor(double percent) {
		Color raw = Color.getHSBColor((float) (0.33f * percent), 0.36f, 0.52f);
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
			return getBackground();
		}
	}

//	public void setTextColor(Color textColor) {
//		this.textColor = textColor;
//	}
}
