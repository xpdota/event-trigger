package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.HitPointsWithPredicted;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class HpPredictedRenderer extends ResourceBarRendererWithMovement<HitPointsWithPredicted> {

	private static final Color predictedDead = new Color(108, 7, 7);
	private static final Color actualDead = new Color(49, 7, 7);

	public HpPredictedRenderer() {
		super(HitPointsWithPredicted.class);
	}

	@Override
	protected Color getBarColor(double percent, double percentChange, @NotNull HitPointsWithPredicted item) {
		Color raw = Color.getHSBColor((float) (0.33f * percent), 0.36f, 0.52f);
		return new Color(raw.getRed(), raw.getGreen(), raw.getBlue(), 160);
	}

	@Override
	protected Color getMovementBarColor(double percent, double percentChange, @NotNull HitPointsWithPredicted item) {
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
	protected Color getBorderColor(double percent, double percentChange, @NotNull HitPointsWithPredicted item, Color originalBg) {
		if (percent == 0.0) {
			return actualDead;
		}
		else if (item.getPredicted() == 0.0) {
			return predictedDead;
		}
		else {
			return originalBg;
		}
	}

}
