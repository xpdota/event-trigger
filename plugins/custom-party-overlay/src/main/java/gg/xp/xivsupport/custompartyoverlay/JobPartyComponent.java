package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class JobPartyComponent extends BasePartyListComponent {

	private final IconComponent inner;

	public JobPartyComponent() {
		inner = new IconComponent();
	}

	@Override
	protected Component makeComponent() {
		return inner;
	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		inner.setIcon(IconTextRenderer.getIconOnly(xpc.getJob()));
	}
}
