package gg.xp.xivsupport.sys;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class EnhancedReadWriteReentrantLockTest {

	/**
	 * Test that a read lock can be acquired and released using try-with-resources.
	 */
	@Test
	public void testReadLock() {
		EnhancedReadWriteReentrantLock lock = new EnhancedReadWriteReentrantLock();
		AtomicBoolean locked = new AtomicBoolean(false);
		try (LockAdapter ignored = lock.read()) {
			locked.set(true);
		}
		MatcherAssert.assertThat(locked.get(), Matchers.is(true));
	}

	/**
	 * Test that a write lock can be acquired and released using try-with-resources.
	 */
	@Test
	public void testWriteLock() {
		EnhancedReadWriteReentrantLock lock = new EnhancedReadWriteReentrantLock();
		AtomicBoolean locked = new AtomicBoolean(false);
		try (LockAdapter ignored = lock.write()) {
			locked.set(true);
		}
		MatcherAssert.assertThat(locked.get(), Matchers.is(true));
	}

	/**
	 * Test that multiple read locks can be held simultaneously by the same thread.
	 */
	@Test
	public void testMultipleReadLocks() {
		EnhancedReadWriteReentrantLock lock = new EnhancedReadWriteReentrantLock();
		try (LockAdapter ignored1 = lock.read()) {
			try (LockAdapter ignored2 = lock.read()) {
				// Should not block
			}
		}
	}

	/**
	 * Test that a write lock is exclusive and blocks other write lock attempts from different threads.
	 *
	 * @throws InterruptedException if the wait is interrupted
	 * @throws ExecutionException if the background task fails
	 * @throws TimeoutException if the timeout expires
	 */
	@Test
	public void testWriteLockExclusivity() throws InterruptedException, ExecutionException, TimeoutException {
		EnhancedReadWriteReentrantLock lock = new EnhancedReadWriteReentrantLock();
		AtomicBoolean writeLocked = new AtomicBoolean(false);
		CompletableFuture<Void> secondLockAcquired = new CompletableFuture<>();

		try (LockAdapter ignored = lock.write()) {
			writeLocked.set(true);
			CompletableFuture.runAsync(() -> {
				try (LockAdapter ignored2 = lock.write()) {
					secondLockAcquired.complete(null);
				}
			});

			try {
				secondLockAcquired.get(100, TimeUnit.MILLISECONDS);
				Assert.fail("Second write lock should have been blocked");
			} catch (TimeoutException e) {
				// Expected
			}
		}
		
		secondLockAcquired.get(1, TimeUnit.SECONDS);
	}

	/**
	 * Test that a write lock blocks read lock attempts from different threads.
	 *
	 * @throws InterruptedException if the wait is interrupted
	 * @throws ExecutionException if the background task fails
	 * @throws TimeoutException if the timeout expires
	 */
	@Test
	public void testReadWriteExclusivity() throws InterruptedException, ExecutionException, TimeoutException {
		EnhancedReadWriteReentrantLock lock = new EnhancedReadWriteReentrantLock();
		CompletableFuture<Void> readLockAcquired = new CompletableFuture<>();

		try (LockAdapter ignored = lock.write()) {
			CompletableFuture.runAsync(() -> {
				try (LockAdapter ignored2 = lock.read()) {
					readLockAcquired.complete(null);
				}
			});

			try {
				readLockAcquired.get(100, TimeUnit.MILLISECONDS);
				Assert.fail("Read lock should have been blocked by write lock");
			} catch (TimeoutException e) {
				// Expected
			}
		}
		
		readLockAcquired.get(1, TimeUnit.SECONDS);
	}
    
	/**
	 * Test that a read lock blocks write lock attempts from different threads.
	 *
	 * @throws InterruptedException if the wait is interrupted
	 * @throws ExecutionException if the background task fails
	 * @throws TimeoutException if the timeout expires
	 */
    @Test
	public void testWriteAfterReadExclusivity() throws InterruptedException, ExecutionException, TimeoutException {
		EnhancedReadWriteReentrantLock lock = new EnhancedReadWriteReentrantLock();
		CompletableFuture<Void> writeLockAcquired = new CompletableFuture<>();

		try (LockAdapter ignored = lock.read()) {
			CompletableFuture.runAsync(() -> {
				try (LockAdapter ignored2 = lock.write()) {
					writeLockAcquired.complete(null);
				}
			});

			try {
				writeLockAcquired.get(100, TimeUnit.MILLISECONDS);
				Assert.fail("Write lock should have been blocked by read lock");
			} catch (TimeoutException e) {
				// Expected
			}
		}
		
		writeLockAcquired.get(1, TimeUnit.SECONDS);
	}

	/**
	 * Test the reentrant behavior of the lock, specifically acquiring a write lock
	 * multiple times and then a read lock within the same thread.
	 */
	@Test
	public void testReentrancy() {
		EnhancedReadWriteReentrantLock lock = new EnhancedReadWriteReentrantLock();
		try (LockAdapter ignored1 = lock.write()) {
			try (LockAdapter ignored2 = lock.write()) {
				try (LockAdapter ignored3 = lock.read()) {
					// All good
				}
			}
		}
	}
}
