package gg.xp.xivsupport.events.triggers.easytriggers.gui.tree;

import gg.xp.xivsupport.events.triggers.easytriggers.model.BaseTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.TriggerFolder;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;

public record Selection(BaseTrigger<?> trigger, TreePath path) {
	public @Nullable TriggerFolder getParent() {
		if (path.getParentPath() == null) {
			return null;
		}
		if (path.getParentPath().getLastPathComponent() instanceof TriggerFolder tf) {
			return tf;
		}
		return null;
	}
}
