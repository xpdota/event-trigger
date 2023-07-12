package gg.xp.xivsupport.events.misc;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;

public class BucketList<E> extends AbstractList<E> implements RandomAccess {

	private final class Bucket {
		private final ArrayList<E> list;

		private Bucket(ArrayList<E> list) {
			this.list = list;
		}

		private boolean isFull() {
			return list.size() >= bucketSize;
		}
	}

	private final List<Bucket> buckets = new ArrayList<>(List.of(new Bucket(new ArrayList<>())));
	private final int bucketSize;

	public BucketList(int bucketSize) {
		this.bucketSize = bucketSize;
	}

	@Override
	public E get(int index) {
		if (index < 0) {
			throw new IndexOutOfBoundsException(index);
		}
		int bucket = index / bucketSize;
		int idxInBucket = index % bucketSize;
		if (bucket < buckets.size()) {
			Bucket theBucket = buckets.get(bucket);
			return theBucket.list.get(idxInBucket);
		}
		else {
			throw new IndexOutOfBoundsException(index);
		}
	}

	public int bucketCount() {
		return buckets.size();
	}

	private Bucket getCurrentBucket() {
		return buckets.get(buckets.size() - 1);
	}

	private Bucket newBucket() {
		Bucket newBucket = new Bucket(new ArrayList<>());
		buckets.add(newBucket);
		return newBucket;
	}

	public BucketList<E> appendOnlyCopy() {
		BucketList<E> out = new BucketList<>(bucketSize);
		out.buckets.clear();
		out.buckets.addAll(this.buckets);
		return out;
	}

	@Override
	public boolean add(E e) {
		Bucket bucket = getCurrentBucket();
		if (bucket.isFull()) {
			bucket = newBucket();
		}
		return bucket.list.add(e);
	}

	@Override
	public E set(int index, E element) {
		if (index < 0 || index >= size()) {
			throw new IndexOutOfBoundsException(index);
		}
		int bucket = index / bucketSize;
		int idxInBucket = index % bucketSize;
		Bucket theBucket = buckets.get(bucket);
		return theBucket.list.set(idxInBucket, element);
	}

	@Override
	public int size() {
		int lastBucketIndex = buckets.size() - 1;
		return (lastBucketIndex) * bucketSize + buckets.get(lastBucketIndex).list.size();
	}

	public boolean prune() {
		if (buckets.size() <= 1) {
			return false;
		}
		buckets.remove(0);
		return true;
	}

	@Override
	public void clear() {
		buckets.clear();
		newBucket();
	}

	@SuppressWarnings("NullableProblems")
	@Override
	public @NotNull Object[] toArray() {
		int sz = size();
		Object[] out = new Object[sz];
		int i = 0;
		for (Bucket bucket : buckets) {
			Object[] elementData;
			try {
				elementData = (Object[]) ArrayList.class.getDeclaredField("elementData").get(bucket);
			}
			catch (IllegalAccessException | NoSuchFieldException e) {
				throw new RuntimeException(e);
			}
			System.arraycopy(elementData, 0, out, i, elementData.length);
			i += elementData.length;
		}
		return out;
	}

	public <T> T[] toArray(T[] a) {
		T[] out = Arrays.copyOf(a, size());
		int i = 0;
		for (Bucket bucket : buckets) {
			Object[] elementData;
			try {
				elementData = (Object[]) ArrayList.class.getDeclaredField("elementData").get(bucket);
			}
			catch (IllegalAccessException | NoSuchFieldException e) {
				throw new RuntimeException(e);
			}
			System.arraycopy(elementData, 0, out, i, elementData.length);
			i += elementData.length;
		}
		return out;
	}

}
