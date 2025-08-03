package gg.xp.xivsupport.events.triggers.easytriggers.gui.tree;

import gg.xp.xivsupport.events.triggers.easytriggers.model.HasChildTriggers;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.util.List;

public record TriggerTransferData(Selections selections) {

	public @Nullable HasChildTriggers sourceParentFolder() {
		List<TreePath> selectedPaths = selections.getSelectedPaths();
		if (selectedPaths.isEmpty()) {
			return null;
		}
		Object parent = selectedPaths.get(0).getParentPath().getLastPathComponent();
		if (parent instanceof HasChildTriggers hct) {
			return hct;
		}
		return null;
	}

}
