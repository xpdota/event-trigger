package gg.xp.events.misc;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

public class ProxyForAppendOnlyList<E> extends AbstractList<E> implements RandomAccess {

	private final List<E> backingList;
	private final int size;

	public ProxyForAppendOnlyList(List<E> list) {
		if (!(list instanceof RandomAccess)) {
			throw new IllegalArgumentException("Use a random access list type with this, not " + list.getClass());
		}
		backingList = list;
		size = list.size();
//		int size = list.size();
//		backingList = new ArrayList<>(size);
//		for (int i = 0; i < list.size(); i++) {
//			backingList.add(list.get(i));
//		}
	}

	@Override
	public E get(int index) {
		if (index >= size) {
			throw new IndexOutOfBoundsException(String.format("Index %s is out of bounds for list of %s items", index, size));
		}
		return backingList.get(index);
	}

	@Override
	public int size() {
		return size;
	}
}
