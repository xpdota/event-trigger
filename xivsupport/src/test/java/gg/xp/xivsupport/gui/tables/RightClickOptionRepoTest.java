package gg.xp.xivsupport.gui.tables;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class RightClickOptionRepoTest {

	@Test
	void testParenting() {
		CustomRightClickOption foo = CustomRightClickOption.forRow("foo", Integer.class, Integer::toHexString);
		CustomRightClickOption bar = CustomRightClickOption.forRow("bar", Integer.class, Integer::toHexString);
		CustomRightClickOption baz = CustomRightClickOption.forRow("baz", Integer.class, Integer::toHexString);
		var parent = RightClickOptionRepo.of(foo);
		var child = parent.withMore(baz);
		Assert.assertEquals(child.getOptions(), List.of(foo, baz));
		parent.addOption(bar);
		// The child is NOT an independent instance. It should receive changes to parents.
		Assert.assertEquals(child.getOptions(), List.of(foo, bar, baz));
	}

}