package gg.xp.xivsupport.events.misc;

import org.jetbrains.annotations.Nullable;

public class OverwritingRingBuffer<X> {

	private final int maxSize;
	private final Object[] data;
	private int nextReadPos;
	private int curSize;

	public OverwritingRingBuffer(int size) {
		if (size < 2) {
			throw new IllegalArgumentException("Invalid size: " + size);
		}
		this.maxSize = size;
		this.data = new Object[size];
	}

	@SuppressWarnings("unchecked")
	public @Nullable X read() {
		// If we are caught up, read nothing
		if (curSize == 0) {
			return null;
		}
		// Otherwise, read and post-increment nextReadPos
		else {
			X out = (X) data[nextReadPos];
			incReadPos();
			return out;
		}
	}

	public void write(X value) {
		if (value == null) {
			throw new IllegalArgumentException("May not write null");
		}
		data[getNextWritePos()] = value;
		incWritePos();
	}

	private void incWritePos() {
		// If full, just increment the read pos, effectively evicting the oldest entry
		if (curSize >= maxSize) {
			incReadPosOnly();
		}
		else {
			curSize++;
		}
	}

	private void incReadPos() {
		incReadPosOnly();
		curSize--;
	}

	private void incReadPosOnly() {
		nextReadPos = (nextReadPos + 1) % maxSize;
	}

	private int getNextWritePos() {
		return (nextReadPos + curSize) % maxSize;
	}
}
