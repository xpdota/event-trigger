package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.CurrentMaxPair;
import gg.xp.xivsupport.models.CurrentMaxPairImpl;

import java.awt.*;
import java.util.Objects;

public class FractionDisplayHelper extends Component {

	// TODO: this might benefit from a buffer image like DropShadowLabel
	private int textXOffset;
	private int textYOffset;

	private String text = "";
	private BarFractionDisplayOption displayMode = BarFractionDisplayOption.AUTO;
	private CurrentMaxPair data = new CurrentMaxPairImpl(0, 0);
	private CurrentMaxPair lastData;

	public void setValue(CurrentMaxPair data) {
		this.data = data;
		recalc();
	}

	public void setDisplayMode(BarFractionDisplayOption displayMode) {
		this.displayMode = displayMode;
		// Force recalc
		clearComputedData();
	}

	@Override
	public void validate() {
		clearComputedData();
		recalc();
	}

	private void clearComputedData() {
		lastData = null;
	}

	public void recalc() {
		// The idea here is that we want alignment and choosing whether to display the full fraction or not
		// to be based on
		Graphics graphics = getGraphics();
		if (graphics == null) {
			return;
		}
		if (Objects.equals(data, lastData)) {
			return;
		}
		int componentWidth = getWidth();
		long current = data.current();
		long max = data.max();
		FontMetrics fm = graphics.getFontMetrics();

		textYOffset = (int) (getHeight() / 2.0 + fm.getHeight() / 3.0);

		String fullFraction = String.format("%s / %s", max, max);
		int fullTextWidth = fm.stringWidth(fullFraction);
		if (displayMode == BarFractionDisplayOption.NUMERATOR || (displayMode == BarFractionDisplayOption.AUTO && fullTextWidth > componentWidth)) {
			String currentValueString = String.valueOf(current);
			int textWidth = fm.stringWidth(currentValueString);
			// Center text
			this.textXOffset = componentWidth / 2 - textWidth / 2;
			this.text = currentValueString;
		}
		else {
			String valueString = String.format("%s / %s", current, max);
			// We want the '/' to be centered - not the overall text, so recalc the length and offset it
			int shorterTextWidth = fm.stringWidth(valueString);
			this.textXOffset = componentWidth / 2 - fullTextWidth / 2 + (fullTextWidth - shorterTextWidth);
			this.text = valueString;
		}
		lastData = data;
	}


	@Override
	public void paint(Graphics gg) {
		Graphics2D g = (Graphics2D) gg;
		int xOffset = this.textXOffset;
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.drawString(text, xOffset, textYOffset);

	}
}
