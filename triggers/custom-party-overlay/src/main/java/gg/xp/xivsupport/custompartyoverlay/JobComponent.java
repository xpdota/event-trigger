package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivsupport.gui.tables.renderers.ComponentListRenderer;
import gg.xp.xivsupport.gui.tables.renderers.DropShadowLabel;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ScaledImageComponent;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class JobComponent extends BasePartyListComponent {

	private ScaledImageComponent drawIcon;
	private final ComponentListRenderer listRenderer;
	private final DropShadowLabel label = new DropShadowLabel();

	public JobComponent() {
		listRenderer = new ComponentListRenderer(2, true);
		listRenderer.setOpaque(false);
		label.setForeground(new Color(255, 255, 255));
	}

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
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		this.drawIcon = IconTextRenderer.getIconOnly(xpc.getJob());
	}
}
