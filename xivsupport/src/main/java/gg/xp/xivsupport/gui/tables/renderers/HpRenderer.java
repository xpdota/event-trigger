package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.HitPoints;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class HpRenderer extends ResourceBarRenderer<HitPoints> {
	public HpRenderer() {
		super(HitPoints.class);
	}

	@Override
	protected Color getBarColor(double percent, @NotNull HitPoints item) {
		return Color.getHSBColor((float) (0.33f * percent), 0.36f, 0.52f);
	}
}
