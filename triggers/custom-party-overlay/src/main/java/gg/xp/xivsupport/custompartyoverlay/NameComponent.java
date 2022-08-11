package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivsupport.gui.overlay.TextAlignment;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class NameComponent extends BasePartyListComponent {

	private final DropShadowLabel label = new DropShadowLabel();

	public NameComponent() {
		label.setForeground(new Color(255, 255, 255));
		label.setAlignment(TextAlignment.RIGHT);
	}

	@Override
	protected Component makeComponent() {
		return label;
	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		label.setText(xpc.getName());
	}
}
