package gg.xp.xivsupport.custompartyoverlay.name;

import gg.xp.xivsupport.custompartyoverlay.BasePartyListComponent;
import gg.xp.xivsupport.gui.tables.renderers.DropShadowLabel;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class NameComponent extends BasePartyListComponent {

	private final DropShadowLabel label = new DropShadowLabel();
	private final NameComponentConfig config;

	public NameComponent(NameComponentConfig config) {
		this.config = config;
		config.addAndRunListener(this::reformatFromSettings);
	}

	private void reformatFromSettings() {
		label.setForeground(config.getFontColor().get());
		label.setAlignment(config.getAlignment().get());
		label.setEnableShadow(config.getDropShadow().get());
		label.repaint();
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
