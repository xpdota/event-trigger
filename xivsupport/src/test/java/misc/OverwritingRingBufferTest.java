package misc;

import gg.xp.xivsupport.events.misc.OverwritingRingBuffer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class OverwritingRingBufferTest {

	@Test
	void testBasic() {
		OverwritingRingBuffer<Integer> buf = new OverwritingRingBuffer<>(4);
		Assert.assertNull(buf.read());
		buf.write(1);
		buf.write(2);
		buf.write(3);
		Assert.assertEquals(buf.read(), 1);
		Assert.assertEquals(buf.read(), 2);
		Assert.assertEquals(buf.read(), 3);
		Assert.assertNull(buf.read());
		buf.write(4);
		buf.write(5);
		buf.write(6);
		Assert.assertEquals(buf.read(), 4);
		Assert.assertEquals(buf.read(), 5);
		Assert.assertEquals(buf.read(), 6);
		Assert.assertNull(buf.read());
	}

	@Test
	void testOverwrite() {
		OverwritingRingBuffer<Integer> buf = new OverwritingRingBuffer<>(4);
		Assert.assertNull(buf.read());
		buf.write(1);
		buf.write(2);
		buf.write(3);
		buf.write(4);
		buf.write(5);
		Assert.assertEquals(buf.read(), 2);
		Assert.assertEquals(buf.read(), 3);
		Assert.assertEquals(buf.read(), 4);
		Assert.assertEquals(buf.read(), 5);
		Assert.assertNull(buf.read());
	}

}
