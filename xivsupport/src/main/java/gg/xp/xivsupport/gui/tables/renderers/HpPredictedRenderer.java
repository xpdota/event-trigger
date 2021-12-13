package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.CurrentMaxPredicted;

import java.awt.*;

public class HpPredictedRenderer extends ResourceBarRendererWithMovement {
	@Override
	protected Color getBarColor(double percent, double percentChange, CurrentMaxPredicted item) {
		return Color.getHSBColor((float) (0.33f * percent), 0.36f, 0.58f);
	}

	@Override
	protected Color getMovementBarColor(double percent, double percentChange, CurrentMaxPredicted item) {
		if (percentChange > 0) {
			return new Color(64, 128, 64);
		}
		else {
			if (percent + percentChange < 0) {
				return new Color(128, 16, 16);

			}
			else {
				return new Color(64, 16, 16);
			}
		}
	}

}
