package gg.xp.xivsupport.sys;

import java.util.concurrent.locks.Lock;

public class LockAdapter implements AutoCloseable {


	private final Lock lock;

	public LockAdapter(Lock lock) {
		lock.lock();
		this.lock = lock;
	}

	@Override
	public void close() {
		lock.unlock();
	}

}
