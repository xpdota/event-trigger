package gg.xp.xivsupport.events.triggers.easytriggers.gui.tree;

import gg.xp.xivsupport.events.triggers.easytriggers.model.BaseTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.TriggerFolder;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;

public record TriggerTransferData(BaseTrigger<?> trigger, TreePath path) {

	public @Nullable TriggerFolder sourceParentFolder() {
		Object parent = path.getParentPath().getLastPathComponent();
		if (parent instanceof TriggerFolder) {
			return (TriggerFolder) parent;
		}
		return null;
	}

}
