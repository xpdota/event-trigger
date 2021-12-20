package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.CurrentMaxPredicted;

import java.awt.*;

public class HpPredictedRenderer extends ResourceBarRendererWithMovement {
	@Override
	protected Color getBarColor(double percent, double percentChange, CurrentMaxPredicted item) {
		Color raw = Color.getHSBColor((float) (0.33f * percent), 0.36f, 0.52f);
		return new Color(raw.getRed(), raw.getGreen(), raw.getBlue(), 128);
	}

	@Override
	protected Color getMovementBarColor(double percent, double percentChange, CurrentMaxPredicted item) {
		if (percentChange > 0) {
			return new Color(64, 128, 64);
		}
		else {
			if (percent + percentChange < 0) {
				return new Color(128, 16, 16, 192);
			}
			else {
				return new Color(64, 16, 16, 192);
			}
		}
	}

	@Override
	protected Color getBorderColor(double percent, double percentChange, CurrentMaxPredicted item, Color originalBg) {
		if (percent == 0.0) {
			return Color.WHITE;
		}
		else if (item.getPredicted() == 0.0) {
			return Color.RED;
		}
		else {
			return originalBg;
		}
	}

}
