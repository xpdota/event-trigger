package gg.xp.util;

import java.util.Arrays;
import java.util.Collection;

public class BitField {

	private long[] array = {0L};
	private static final long SIZE_LIMIT = Integer.MAX_VALUE * 64L;

	/**
	 * Creates a BitField where all values are initially false
	 */
	public BitField() {
	}

	public static BitField fromLong(Collection<Long> longs) {
		BitField bf = new BitField();
		long max = 0;
		for (long number : longs) {
			assertValidIndex(number);
			max = Math.max(max, number);
		}
		bf.ensureSize(max);
		for (long number : longs) {
			bf.set(number, true);
		}
		return bf;
	}

	public static BitField fromInts(Collection<Integer> ints) {
		BitField bf = new BitField();
		int max = 0;
		for (int integer : ints) {
			max = Math.max(max, integer);
		}
		bf.ensureSize(max);
		for (int integer : ints) {
			bf.set(integer, true);
		}
		return bf;
	}

	public boolean get(int index) {
		return get((long) index);
	}

	public boolean get(long index) {
		if (!isValidIndex(index)) {
			return false;
		}
		int arrayIndex = (int) (index / 64);
		if (arrayIndex >= array.length) {
			return false;
		}
		int bitIndex = (int) (index % 64);
		long arrayValue = array[arrayIndex];
		return ((arrayValue >> bitIndex) & 1) == 1;
	}

	public boolean set(int index, boolean value) {
		return set((long) index, value);
	}

	public boolean set(long index, boolean value) {
		assertValidIndex(index);
		int arrayIndex = (int) (index / 64);
		// If setting a false value outside of our known range, we know the value is false and that
		// it is a no-op.
		if (arrayIndex > array.length && !value) {
			return false;
		}
		ensureSize(index);
		int bitIndex = (int) (index % 64);
		long arrayValue = array[arrayIndex];
		boolean oldValue = ((arrayValue >> bitIndex) & 1) == 1;
		long bitmask = 1L << bitIndex;
		if (value) {
			arrayValue |= bitmask;
		}
		else {
			arrayValue &= ~bitmask;
		}
		array[arrayIndex] = arrayValue;
		return oldValue;
	}

	public boolean isValidIndex(long index) {
		return index >= 0 && index < SIZE_LIMIT;
	}

	public static void assertValidIndex(long index) {
		if (index < 0) {
			throw new IllegalArgumentException("Index cannot be negative: " + index);
		}
		if (index >= SIZE_LIMIT) {
			throw new IllegalArgumentException("Index is too large: " + index);
		}
	}

	public void ensureSize(long index) {
		assertValidIndex(index);
		long neededIndex = index / 64;
		if (array.length < neededIndex) {
			int newSize = (int) Math.max(neededIndex + 1, array.length * 1.2);
			array = Arrays.copyOf(array, newSize);
		}
	}

}
