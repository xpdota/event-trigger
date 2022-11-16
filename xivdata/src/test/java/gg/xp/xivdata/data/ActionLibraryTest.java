package gg.xp.xivdata.data;

import org.testng.annotations.Test;

import java.util.Map;

public class ActionLibraryTest {

	@Test
	public void testCheckForErrors() {
		Map<Long, ActionInfo> all = ActionLibrary.getAll();
		if (all.isEmpty()) {
			throw new RuntimeException("ActionLibrary was empty!");
		}
		ActionLibrary.checkForErrors();
	}
}