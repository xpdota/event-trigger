package gg.xp.xivdata.data;

import org.testng.annotations.Test;

public class ActionLibraryTest {

	@Test
	public void testCheckForErrors() {
		ActionLibrary.getAll();
		ActionLibrary.checkForErrors();
	}
}