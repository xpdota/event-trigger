package gg.xp.xivsupport.sys;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.ThreadFactory;

public class Threading {
	public static ThreadFactory namedDaemonThreadFactory(String nameStub) {
		return new BasicThreadFactory.Builder()
				.wrappedFactory(Thread.ofVirtual().factory())
				.namingPattern(nameStub + "-%d")
				.daemon(true)
				.build();
	}
}
