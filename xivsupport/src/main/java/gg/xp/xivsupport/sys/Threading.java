package gg.xp.xivsupport.sys;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.ThreadFactory;

public final class Threading {
	private Threading() {
	}

	public static ThreadFactory namedDaemonThreadFactory(String nameStub) {
		return new BasicThreadFactory.Builder()
				.namingPattern(nameStub + "-%d")
				.daemon(true)
				.build();
	}
}
