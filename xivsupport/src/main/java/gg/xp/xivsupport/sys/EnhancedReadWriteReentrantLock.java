package gg.xp.xivsupport.sys;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class EnhancedReadWriteReentrantLock {

	private final LockAdapter readAdp;
	private final LockAdapter writeAdp;

	public EnhancedReadWriteReentrantLock() {
		ReadWriteLock lock = new ReentrantReadWriteLock();
		readAdp = new LockAdapter(lock.readLock());
		writeAdp = new LockAdapter(lock.writeLock());
	}

	public LockAdapter read() {
		readAdp.lock();
		return readAdp;
	}

	public LockAdapter write() {
		writeAdp.lock();
		return writeAdp;
	}

}
