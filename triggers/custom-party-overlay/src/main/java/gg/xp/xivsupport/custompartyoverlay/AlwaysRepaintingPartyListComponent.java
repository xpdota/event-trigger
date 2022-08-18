package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;

public abstract class AlwaysRepaintingPartyListComponent extends BasePartyListComponent {
	@Override
	public void refresh(@Nullable XivPlayerCharacter xpc) {
		super.refresh(xpc);
		getComponent().repaint();
	}
}
