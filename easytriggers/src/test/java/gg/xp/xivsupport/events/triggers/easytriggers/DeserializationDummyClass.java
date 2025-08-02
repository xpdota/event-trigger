package gg.xp.xivsupport.events.triggers.easytriggers;

import java.util.concurrent.atomic.AtomicInteger;

public class DeserializationDummyClass {
	private static final AtomicInteger instantiationCount = new AtomicInteger();

	public DeserializationDummyClass() {
		instantiationCount.incrementAndGet();
	}

	public static int getInstantiationCount() {
		return instantiationCount.get();
	}
}
