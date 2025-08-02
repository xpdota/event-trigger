package gg.xp.xivsupport.events.triggers.easytriggers.gui.tree;

import gg.xp.xivsupport.events.triggers.easytriggers.model.BaseTrigger;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record Selections(List<Selection> selections) {
	/**
	 * @return If at least one item is selected
	 */
	public boolean hasSelection() {
		return !this.selections.isEmpty();
	}

	/**
	 * @return if exactly one item is selected
	 */
	public boolean hasSingleSelection() {
		return this.selections.size() == 1;
	}

	/**
	 * @return If this selection has at least one item selected, and all selected items have the same parent
	 * (i.e. you aren't selecting things in different parents)
	 */
	public boolean hasConsistentParentSelection() {
		if (this.selections.isEmpty()) {
			return false;
		}
		Map<TreePath, List<TreePath>> parents = getSelectedPaths().stream().collect(Collectors.groupingBy(TreePath::getParentPath));
		return parents.size() == 1;
	}

	/**
	 * @return If this selection has at least one item selected, and no selected item has another item as
	 * a parent (recursively, i.e. including transitive parents).
	 */
	public boolean hasNonSelfNestedSelection() {
		if (this.selections.isEmpty()) {
			return false;
		}
		Set<TreePath> parents = new HashSet<>();
		// Iterate through each directly selected item
		for (Selection selection : selections) {
			TreePath currentParent = selection.path().getParentPath();
			// Stop if the parent was already added
			while (currentParent != null && parents.add(currentParent)) {
				currentParent = currentParent.getParentPath();
			}
		}
		// Iterate again and check if any elements are a parent of any other element
		for (Selection selection : selections) {
			if (parents.contains(selection.path())) {
				return false;
			}
		}
		return true;
	}

	public @Nullable Selection getSingleSelection() {
		if (this.selections.size() != 1) {
			return null;
		}
		return this.selections.get(0);
	}

	public List<BaseTrigger<?>> getSelectedTriggers() {
		return selections.stream().<BaseTrigger<?>>map(Selection::trigger).toList();
	}

	public List<TreePath> getSelectedPaths() {
		return selections.stream().map(Selection::path).toList();
	}
}
