package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivsupport.gui.tables.renderers.ScaledImageComponent;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class IconComponent extends Component {

	private @Nullable ScaledImageComponent drawIcon;

	public void setIcon(@Nullable ScaledImageComponent icon) {
		this.drawIcon = icon;
	}

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

}
