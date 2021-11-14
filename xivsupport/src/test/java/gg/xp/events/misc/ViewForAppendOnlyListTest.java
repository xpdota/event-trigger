package gg.xp.events.misc;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.testng.Assert.*;

public class ViewForAppendOnlyListTest {

	@Test
	void testTheListView() {
		List<String> backingList = new ArrayList<>();
		backingList.add("Foo");
		backingList.add("Bar1");

		List<String> view = new ProxyForAppendOnlyList<>(backingList);

		Assert.assertEquals(view.size(), 2);

		List<String> out = new ArrayList<>();

		Iterator<String> iter = view.iterator();
		Assert.assertTrue(iter.hasNext());
		out.add(iter.next());
		Assert.assertTrue(iter.hasNext());

		backingList.add("Bar2");

		Assert.assertEquals(view.size(), 2);
		Assert.assertTrue(iter.hasNext());
		out.add(iter.next());
		Assert.assertFalse(iter.hasNext());

		Assert.assertThrows(NoSuchElementException.class, iter::next);
	}


}