package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;

public class JobIconComponent extends IconComponent {

	public void setJob(@Nullable Job job) {
		if (job == null) {
			setIcon(null);
		}
		else {
			setIcon(IconTextRenderer.getIconOnly(job));
		}
	}

	public void setJobFrom(@Nullable XivCombatant cbt) {
		if (cbt instanceof XivPlayerCharacter xpc) {
			setJob(xpc.getJob());
		}
		else {
			setIcon(null);
		}
	}

}
