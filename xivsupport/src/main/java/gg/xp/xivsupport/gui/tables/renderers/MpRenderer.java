package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.ManaPoints;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class MpRenderer extends ResourceBarRenderer<ManaPoints> {
	private static final Color mpColor = new Color(128, 128, 255, 147);

	public MpRenderer() {
		super(ManaPoints.class);
	}

	@Override
	protected Color getBarColor(double percent, @NotNull ManaPoints item) {
		return mpColor;
	}
}
