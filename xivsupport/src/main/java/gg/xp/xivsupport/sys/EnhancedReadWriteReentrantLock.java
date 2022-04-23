package gg.xp.xivsupport.sys;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EnhancedReadWriteReentrantLock {

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	public LockAdapter read() {
		return new LockAdapter(lock.readLock());
	}

	public LockAdapter write() {
		return new LockAdapter(lock.writeLock());
	}

}
