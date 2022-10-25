package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public interface RefreshablePartyListComponent {
	Component getComponent();
	void refresh(@Nullable XivPlayerCharacter xpc);
}
