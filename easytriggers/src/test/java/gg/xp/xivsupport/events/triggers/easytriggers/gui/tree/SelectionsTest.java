package gg.xp.xivsupport.events.triggers.easytriggers.gui.tree;

import gg.xp.xivsupport.events.triggers.easytriggers.model.BaseTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.TriggerFolder;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.Collections;
import java.util.List;

public class SelectionsTest {

	/*
	 * Test data structure:
	 *
	 * Root
	 * ├── Folder 1
	 * │   ├── Trigger 1
	 * │   └── Folder 2
	 * │       └── Trigger 2
	 * └── Trigger 3
	 */
	private EasyTrigger<?> trigger1;
	private EasyTrigger<?> trigger2;
	private EasyTrigger<?> trigger3;
	private TriggerFolder folder1;
	private TriggerFolder folder2;

	private DefaultMutableTreeNode rootNode;
	private DefaultMutableTreeNode folder1Node;
	private DefaultMutableTreeNode folder2Node;
	private DefaultMutableTreeNode trigger1Node;
	private DefaultMutableTreeNode trigger2Node;
	private DefaultMutableTreeNode trigger3Node;

	private TreePath rootPath;
	private TreePath folder1Path;
	private TreePath folder2Path;
	private TreePath trigger1Path;
	private TreePath trigger2Path;
	private TreePath trigger3Path;

	@BeforeMethod
	public void setup() {
		// Create triggers and folders
		trigger1 = new EasyTrigger<>();
		trigger1.setName("Trigger 1");

		trigger2 = new EasyTrigger<>();
		trigger2.setName("Trigger 2");

		trigger3 = new EasyTrigger<>();
		trigger3.setName("Trigger 3");

		folder1 = new TriggerFolder();
		folder1.setName("Folder 1");

		folder2 = new TriggerFolder();
		folder2.setName("Folder 2");

		// Set up parent-child relationships
		folder1.addChildTrigger(trigger1);
		folder2.addChildTrigger(trigger2);
		folder1.addChildTrigger(folder2);

		// Create tree nodes
		rootNode = new DefaultMutableTreeNode("Root");
		folder1Node = new DefaultMutableTreeNode(folder1);
		folder2Node = new DefaultMutableTreeNode(folder2);
		trigger1Node = new DefaultMutableTreeNode(trigger1);
		trigger2Node = new DefaultMutableTreeNode(trigger2);
		trigger3Node = new DefaultMutableTreeNode(trigger3);

		// Build tree structure
		rootNode.add(folder1Node);
		folder1Node.add(trigger1Node);
		folder1Node.add(folder2Node);
		folder2Node.add(trigger2Node);
		rootNode.add(trigger3Node);

		// Create tree paths
		rootPath = new TreePath(rootNode);
		folder1Path = rootPath.pathByAddingChild(folder1Node);
		folder2Path = folder1Path.pathByAddingChild(folder2Node);
		trigger1Path = folder1Path.pathByAddingChild(trigger1Node);
		trigger2Path = folder2Path.pathByAddingChild(trigger2Node);
		trigger3Path = rootPath.pathByAddingChild(trigger3Node);
	}

	@Test
	public void testHasSelection() {
		// Test with empty selections
		Selections emptySelections = new Selections(Collections.emptyList());
		Assert.assertFalse(emptySelections.hasSelection(), "Empty selections should return false for hasSelection()");

		// Test with one selection
		Selection selTrigger1 = new Selection(trigger1, trigger1Path);
		Selections singleSelection = new Selections(List.of(selTrigger1));
		Assert.assertTrue(singleSelection.hasSelection(), "Single selection should return true for hasSelection()");

		// Test with multiple selections
		Selection selTrigger2 = new Selection(trigger2, trigger2Path);
		Selections multipleSelections = new Selections(List.of(selTrigger1, selTrigger2));
		Assert.assertTrue(multipleSelections.hasSelection(), "Multiple selections should return true for hasSelection()");
	}

	@Test
	public void testHasSingleSelection() {
		// Test with empty selections
		Selections emptySelections = new Selections(Collections.emptyList());
		Assert.assertFalse(emptySelections.hasSingleSelection(), "Empty selections should return false for hasSingleSelection()");

		// Test with one selection
		Selection selTrigger1 = new Selection(trigger1, trigger1Path);
		Selections singleSelection = new Selections(List.of(selTrigger1));
		Assert.assertTrue(singleSelection.hasSingleSelection(), "Single selection should return true for hasSingleSelection()");

		// Test with multiple selections
		Selection selTrigger2 = new Selection(trigger2, trigger2Path);
		Selections multipleSelections = new Selections(List.of(selTrigger1, selTrigger2));
		Assert.assertFalse(multipleSelections.hasSingleSelection(), "Multiple selections should return false for hasSingleSelection()");
	}

	@Test
	public void testHasConsistentParentSelection() {
		// Test with empty selections
		Selections emptySelections = new Selections(Collections.emptyList());
		Assert.assertFalse(emptySelections.hasConsistentParentSelection(), "Empty selections should return false for hasConsistentParentSelection()");

		// Test with one selection
		Selection selTrigger1 = new Selection(trigger1, trigger1Path);
		Selections singleSelection = new Selections(List.of(selTrigger1));
		Assert.assertTrue(singleSelection.hasConsistentParentSelection(), "Single selection should return true for hasConsistentParentSelection()");

		// Test with multiple selections with same parent
		Selection selFolder2 = new Selection(folder2, folder2Path);
		Selections sameParentSelections = new Selections(List.of(selTrigger1, selFolder2));
		Assert.assertTrue(sameParentSelections.hasConsistentParentSelection(),
				"Multiple selections with same parent should return true for hasConsistentParentSelection()");

		// Test with multiple selections with different parents
		Selection selTrigger3 = new Selection(trigger3, trigger3Path);
		Selections differentParentSelections = new Selections(List.of(selTrigger1, selTrigger3));
		Assert.assertFalse(differentParentSelections.hasConsistentParentSelection(),
				"Multiple selections with different parents should return false for hasConsistentParentSelection()");
	}

	@Test
	public void testHasNonSelfNestedSelection() {
		// Test with empty selections
		Selections emptySelections = new Selections(Collections.emptyList());
		Assert.assertFalse(emptySelections.hasNonSelfNestedSelection(), "Empty selections should return false for hasNonSelfNestedSelection()");

		// Test with one selection
		Selection selTrigger1 = new Selection(trigger1, trigger1Path);
		Selections singleSelection = new Selections(List.of(selTrigger1));
		Assert.assertTrue(singleSelection.hasNonSelfNestedSelection(), "Single selection should return true for hasNonSelfNestedSelection()");

		// Test with multiple non-nested selections
		Selection selTrigger3 = new Selection(trigger3, trigger3Path);
		Selections nonNestedSelections = new Selections(List.of(selTrigger1, selTrigger3));
		Assert.assertTrue(nonNestedSelections.hasNonSelfNestedSelection(),
				"Multiple non-nested selections should return true for hasNonSelfNestedSelection()");

		// Test with nested selections (parent and child)
		Selection selFolder1 = new Selection(folder1, folder1Path);
		Selections nestedSelections = new Selections(List.of(selFolder1, selTrigger1));
		Assert.assertFalse(nestedSelections.hasNonSelfNestedSelection(),
				"Nested selections should return false for hasNonSelfNestedSelection()");

		// Test three non-nested
		Selection selTrigger2 = new Selection(trigger2, trigger2Path);
		Selections threeNonNestedSelections = new Selections(List.of(selTrigger1, selTrigger2, selTrigger3));
		Assert.assertTrue(threeNonNestedSelections.hasNonSelfNestedSelection(),
				"Three non-nested selections should return true for hasNonSelfNestedSelection()");

		// Test transitive
		Selections transitiveSelections = new Selections(List.of(selFolder1, selTrigger2));
		Assert.assertFalse(transitiveSelections.hasNonSelfNestedSelection(),
				"Nested selections should return false for hasNonSelfNestedSelection() even when transitive");
	}

	@Test
	public void testGetSingleSelection() {
		// Test with empty selections
		Selections emptySelections = new Selections(Collections.emptyList());
		Assert.assertNull(emptySelections.getSingleSelection(), "Empty selections should return null for getSingleSelection()");

		// Test with one selection
		Selection selTrigger1 = new Selection(trigger1, trigger1Path);
		Selections singleSelection = new Selections(List.of(selTrigger1));
		Assert.assertEquals(singleSelection.getSingleSelection(), selTrigger1,
				"Single selection should return the selection for getSingleSelection()");

		// Test with multiple selections
		Selection selTrigger2 = new Selection(trigger2, trigger2Path);
		Selections multipleSelections = new Selections(List.of(selTrigger1, selTrigger2));
		Assert.assertNull(multipleSelections.getSingleSelection(),
				"Multiple selections should return null for getSingleSelection()");
	}

	@Test
	public void testGetSelectedTriggers() {
		// Test with empty selections
		Selections emptySelections = new Selections(Collections.emptyList());
		Assert.assertTrue(emptySelections.getSelectedTriggers().isEmpty(),
				"Empty selections should return empty list for getSelectedTriggers()");

		// Test with one selection
		Selection selTrigger1 = new Selection(trigger1, trigger1Path);
		Selections singleSelection = new Selections(List.of(selTrigger1));
		List<BaseTrigger<?>> singleTriggers = singleSelection.getSelectedTriggers();
		Assert.assertEquals(singleTriggers.size(), 1, "Single selection should return list with one trigger");
		Assert.assertEquals(singleTriggers.get(0), trigger1, "Single selection should return the correct trigger");

		// Test with multiple selections
		Selection selTrigger2 = new Selection(trigger2, trigger2Path);
		Selections multipleSelections = new Selections(List.of(selTrigger1, selTrigger2));
		List<BaseTrigger<?>> multipleTriggers = multipleSelections.getSelectedTriggers();
		Assert.assertEquals(multipleTriggers.size(), 2, "Multiple selections should return list with multiple triggers");
		Assert.assertTrue(multipleTriggers.contains(trigger1), "Multiple selections should contain first trigger");
		Assert.assertTrue(multipleTriggers.contains(trigger2), "Multiple selections should contain second trigger");
	}

	@Test
	public void testGetSelectedPaths() {
		// Test with empty selections
		Selections emptySelections = new Selections(Collections.emptyList());
		Assert.assertTrue(emptySelections.getSelectedPaths().isEmpty(),
				"Empty selections should return empty list for getSelectedPaths()");

		// Test with one selection
		Selection selTrigger1 = new Selection(trigger1, trigger1Path);
		Selections singleSelection = new Selections(List.of(selTrigger1));
		List<TreePath> singlePaths = singleSelection.getSelectedPaths();
		Assert.assertEquals(singlePaths.size(), 1, "Single selection should return list with one path");
		Assert.assertEquals(singlePaths.get(0), trigger1Path, "Single selection should return the correct path");

		// Test with multiple selections
		Selection selTrigger2 = new Selection(trigger2, trigger2Path);
		Selections multipleSelections = new Selections(List.of(selTrigger1, selTrigger2));
		List<TreePath> multiplePaths = multipleSelections.getSelectedPaths();
		Assert.assertEquals(multiplePaths.size(), 2, "Multiple selections should return list with multiple paths");
		Assert.assertTrue(multiplePaths.contains(trigger1Path), "Multiple selections should contain first path");
		Assert.assertTrue(multiplePaths.contains(trigger2Path), "Multiple selections should contain second path");
	}
}