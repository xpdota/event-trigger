package gg.xp.xivsupport.events.misc;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

public class ViewForAppendOnlyList<T> implements List<T> {

	private final List<T> backingList;
	private final int size;

	// TODO: as currently implemented, this doesn't do anything, since it's the same as
	// just copying the list....
//	@SuppressWarnings({"ForLoopReplaceableByForEach", "UseBulkOperation"}) // Won't work due to concurrent modifications
	public ViewForAppendOnlyList(List<T> list) {
		backingList = list;
		size = list.size();
//		int size = list.size();
//		backingList = new ArrayList<>(size);
//		for (int i = 0; i < list.size(); i++) {
//			backingList.add(list.get(i));
//		}
	}


	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return backingList.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return backingList.contains(o);
	}

	@NotNull
	@Override
	public Iterator<T> iterator() {
		return listIterator();
	}

	@Override
	public Object @NotNull [] toArray() {
		return backingList.toArray();
	}

	@Override
	public <T1> T1 @NotNull [] toArray(@NotNull T1[] a) {
		return backingList.toArray(a);
	}

	@Override
	public boolean add(T t) {
		throw new UnsupportedOperationException("Read-only list view");
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException("Append-only list");
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		return backingList.containsAll(c);
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends T> c) {
		throw new UnsupportedOperationException("Read-only list view");
	}

	@Override
	public boolean addAll(int index, @NotNull Collection<? extends T> c) {
		throw new UnsupportedOperationException("Append-only list");
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		throw new UnsupportedOperationException("Append-only list");
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		throw new UnsupportedOperationException("Append-only list");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("Append-only list");
	}

	@Override
	public T get(int index) {
		if (index > size) {
			throw new IndexOutOfBoundsException(String.format("Index %s out of range [0, %s]", size - 1));
		}
		return backingList.get(index);
	}

	@Override
	public T set(int index, T element) {
		throw new UnsupportedOperationException("Append-only list");
	}

	@Override
	public void add(int index, T element) {
		throw new UnsupportedOperationException("Append-only list");
	}

	@Override
	public T remove(int index) {
		throw new UnsupportedOperationException("Append-only list");
	}

	@Override
	public int indexOf(Object o) {
		return backingList.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return backingList.lastIndexOf(o);
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return new ViewForAppendOnlyList<>(backingList.subList(fromIndex, toIndex));
	}

	@NotNull
	@Override
	public ListIterator<T> listIterator() {
		return new ListItr(0);
	}

	@NotNull
	@Override
	public ListIterator<T> listIterator(int index) {
		return new ListItr(index);
	}

	@SuppressWarnings("NewExceptionWithoutArguments")
	private class Itr implements Iterator<T> {
		int cursor;       // index of next element to return
		int lastRet = -1; // index of last element returned; -1 if no such

		// prevent creating a synthetic constructor
		Itr() {
		}

		@Override
		public boolean hasNext() {
			return cursor != size();
		}

		@Override
		public T next() {
			int i = cursor;
			if (i >= size())
				throw new NoSuchElementException();
			cursor = i + 1;
			return get(lastRet = i);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Append-only list");
		}

		@Override
		public void forEachRemaining(Consumer<? super T> action) {
			Objects.requireNonNull(action);
			int i = cursor;
			int size = size();
			if (i < size) {
				subList(i, size).forEach(action);
				cursor = i;
				lastRet = i - 1;
			}
		}
	}

	@SuppressWarnings("NewExceptionWithoutArguments")
	private class ListItr extends Itr implements ListIterator<T> {
		ListItr(int index) {
			super();
			//noinspection AssignmentToSuperclassField
			cursor = index;
		}

		@Override
		public boolean hasPrevious() {
			return cursor != 0;
		}

		@Override
		public int nextIndex() {
			return cursor;
		}

		@Override
		public int previousIndex() {
			return cursor - 1;
		}

		@Override
		public T previous() {
			int i = cursor - 1;
			if (i < 0)
				throw new NoSuchElementException();
			if (i >= size())
				throw new NoSuchElementException();
			cursor = i;
			return get(lastRet = i);
		}

		@Override
		public void set(T e) {
			throw new UnsupportedOperationException("Append-only list");
		}

		@Override
		public void add(T e) {
			throw new UnsupportedOperationException("Append-only list");
		}
	}
}
