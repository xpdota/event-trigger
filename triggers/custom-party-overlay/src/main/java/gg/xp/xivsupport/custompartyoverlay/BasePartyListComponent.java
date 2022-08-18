package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public abstract class BasePartyListComponent implements RefreshablePartyListComponent {

	protected abstract Component makeComponent();
	protected abstract void reformatComponent(@NotNull XivPlayerCharacter xpc);
	protected @Nullable Component component;

	@Override
	public Component getComponent() {
		if (component == null) {
			component = makeComponent();
		}
		return component;
	}

	@Override
	public void refresh(@Nullable XivPlayerCharacter xpc) {
		if (component == null) {
			return;
		}
		if (xpc == null) {
			if (component.isVisible()) {
				component.setVisible(false);
			}
		}
		else {
			if (!component.isVisible()) {
				component.setVisible(true);
			}
			reformatComponent(xpc);
		}
	}
}
