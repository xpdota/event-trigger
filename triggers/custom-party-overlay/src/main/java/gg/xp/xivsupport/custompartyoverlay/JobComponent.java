package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ScaledImageComponent;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class JobComponent extends DataWatchingCustomPartyComponent<Job> {

	private ScaledImageComponent drawIcon;

	@Override
	protected Component makeComponent() {

		return new Component() {
			@Override
			public void paint(Graphics g) {
				ScaledImageComponent icon = drawIcon;
				if (icon == null) {
					return;
				}
				Rectangle bounds = getBounds();
				int size = Math.min(bounds.width, bounds.height);
				icon.paint(g, size);
			}
		};
	}

	@Override
	protected Job extractData(@NotNull XivPlayerCharacter xpc) {
		return xpc.getJob();
	}

	@Override
	protected void applyData(Job data) {
		this.drawIcon = IconTextRenderer.getIconOnly(data);
		getComponent().repaint();
	}
}
