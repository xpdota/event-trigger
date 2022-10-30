package gg.xp.xivsupport.gui.nav;

import javax.annotation.Nullable;
import java.util.List;

public record GuiRef(@Nullable String primaryName, List<String> names, Runnable activator) {
	public void activate() {
		activator.run();
	}
}
