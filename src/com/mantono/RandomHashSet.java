package com.mantono;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class RandomHashSet<T> implements RandomAccess<T>, Set<T>, Serializable
{
	private final static int[] PRIMES = {23, 53, 97, 193, 389, 769, 1543, 3079, 6151, 12289, 24593, 49157, 98317,
			196613, 393241, 786433, 1572869, 3145739, 6291469, 12582917, 25165843, 50331653, 100663319, 201326611,
			402653189, 805306457, 1610612741};

	private final static float LOAD_FACTOR = 1.6f;

	private List<T>[][] table;
	private volatile int arraySize = 11;
	private volatile int primeIndex = -1;
	private final AtomicInteger size = new AtomicInteger();
	private final ReentrantReadWriteLock[] locks;
	private final Random random;

	/**
	 * Private constructor which is called by most other constructors. This
	 * constructor offers no extra functionality or options for the end user,
	 * but reduces code complexity and code duplication.
	 *
	 * @param elementCount the expected amount of elements that this data structure will
	 *                     hold.
	 * @param random       the random generator that is used when retrieving random
	 *                     elements.
	 * @throws IllegalArgumentException if <code>elementCount</code> is negative.
	 */
	private RandomHashSet(final int elementCount, final Random random, final int maxThreads)
	{
		initTable(maxThreads, elementCount);
		this.locks = new ReentrantReadWriteLock[maxThreads];
		initLocks();
		this.random = random;
	}

	/**
	 * Initiate the {@link ReentrantReadWriteLock} locks used for locking the data matrix.
	 */
	private void initLocks()
	{
		for(int i = 0; i < locks.length; i++)
			this.locks[i] = new ReentrantReadWriteLock(true);
	}

	/**
	 * Constructor for this class.
	 *
	 * @param elementCount the expected amount of elements that this data structure will
	 *                     hold.
	 * @param seed         is the initial seed that the {@link Random} generator will be
	 *                     initialized with. Setting a specific seed is useful when the
	 *                     the data structure will be used in scientific evaluation, or
	 *                     for some other reason where the access of the elements should
	 *                     be random but the order that they were accessed in should be
	 *                     repeatable (by entering the same seed).
	 * @throws IllegalArgumentException if <code>elementCount</code> is negative.
	 */
	public RandomHashSet(final int elementCount, final long seed)
	{
		this(elementCount, new Random(seed), 8);
	}

	/**
	 * Constructor for this class. When no second argument is given and the seed
	 * to the random generator is ommitted, an instance of {@link SecureRandom}
	 * is used instead of simply {@link Random}. This is a better approach when
	 * it is required that the sequence of random elements fetched through
	 * {@link RandomHashSet#getRandomElement()} is truly unpredictable.
	 *
	 * @param elementCount the expected amount of elements that this data structure will
	 *                     hold.
	 * @throws IllegalArgumentException if <code>elementCount</code> is negative.
	 */
	public RandomHashSet(final int elementCount)
	{
		this(elementCount, new SecureRandom(), 8);
	}

	/**
	 * Constructor for this class.
	 *
	 * @param elementCount the expected amount of elements that this data structure will
	 *                     hold.
	 * @param threads      the maximum number of threads that this object is expected to be
	 *                     used by simultaneously.
	 * @param seed         is the initial seed that the {@link Random} generator will be
	 *                     initialized with. Setting a specific seed is useful when the
	 *                     the data structure will be used in scientific evaluation, or
	 *                     for some other reason where the access of the elements should
	 *                     be random but the order that they were accessed in should be
	 *                     repeatable (by entering the same seed).
	 * @throws IllegalArgumentException if <code>elementCount</code> is negative or <code>threads</code> is less than one.
	 */
	public RandomHashSet(final int elementCount, final int threads, final long seed)
	{
		this(elementCount, new Random(seed), threads);
	}

	/**
	 * Constructor for this class. When no second argument is given and the seed
	 * to the random generator is ommitted, an instance of {@link SecureRandom}
	 * is used instead of simply {@link Random}. This is a better approach when
	 * it is required that the sequence of random elements fetched through
	 * {@link RandomHashSet#getRandomElement()} is truly unpredictable.
	 *
	 * @param elementCount the expected amount of elements that this data structure will
	 *                     hold.
	 * @param threads      the maximum number of threads that this object is expected to be
	 *                     used by simultaneously.
	 * @throws IllegalArgumentException if <code>elementCount</code> is negative or <code>threads</code> is less than one.
	 */
	public RandomHashSet(final int elementCount, final int threads)
	{
		this(elementCount, new SecureRandom(), threads);
	}

	/**
	 * Constructor for creating a new {@link RandomHashSet} from another
	 * {@link RandomHashSet}. The {@link Random} generator that is used in the
	 * originating set will be kept in the new set.
	 *
	 * @param set the {@link RandomHashSet} which this set should be recreated
	 *            from.
	 */
	public RandomHashSet(final RandomHashSet<T> set)
	{
		try
		{
			takeAllLocks(set);
			this.locks = new ReentrantReadWriteLock[set.locks.length];
			initLocks();
			this.table = set.table;
			this.random = set.random;
			this.arraySize = set.arraySize;
			this.primeIndex = set.primeIndex;
			size.set(set.size());
		}
		finally
		{
			releaseAllLocks(set);
		}

	}

	/**
	 * Constructor for creating a new {@link RandomHashSet} with the elements from an existing
	 * {@link Collection}. Since a seed to the random generator is ommitted, an
	 * instance of {@link SecureRandom} is used instead of simply
	 * {@link Random}.
	 *
	 * @param collection the {@link Collection} which this set should be recreated from.
	 */
	public RandomHashSet(final Collection<T> collection)
	{
		this(collection.size(), new SecureRandom(), 8);
		addAll(collection);
	}

	/**
	 * Constructor for creating a new {@link RandomHashSet} with the elements from an existing
	 * {@link Set}. This constructor uses a regular {@link Random} generator.
	 *
	 * @param set the {@link Set} which this set should be recreated from.
	 */
	public RandomHashSet(final Set<T> set, final long seed)
	{
		this(set.size(), new Random(seed), 8);
		addAll(set);
	}

	/**
	 * Constructor for this class. This constructor will use the default array
	 * size of 11 upon initialization.
	 *
	 * @param seed is the initial seed that the {@link Random} generator will be
	 *             initialized with. Setting a specific seed is useful when the
	 *             the data structure will be used in scientific evaluation, or
	 *             for some other reason where the access of the elements should
	 *             be random but the order that they were accessed in should be
	 *             repeatable (by entering the same seed).
	 */
	public RandomHashSet(final long seed)
	{
		this(1, seed);
	}

	/**
	 * Constructor for this class. This constructor will use the default array
	 * size of 11 upon initialization.
	 */
	public RandomHashSet()
	{
		this(1);
	}

	/**
	 * Initialize the <code>table</code> to accommodate the given number of
	 * elements.
	 *
	 * @param elementCount the expected amount of elements that this data structure will
	 *                     hold.
	 * @throws IllegalArgumentException if <code>elementCount</code> is negative.
	 */
	private void initTable(final int threads, final int elementCount)
	{
		if(elementCount < 0)
			throw new IllegalArgumentException("Initial size for set cannot be less than zero.");
		if(threads < 1)
			throw new IllegalArgumentException("Threads cannot be less than one.");
		final int elementsPerThreads = elementCount / threads;
		setArraySize(elementsPerThreads / 3);
		this.table = new ArrayList[threads][arraySize];
	}

	/**
	 * Take all locks from the given set. This is only used when operations are performed on the entire matrix,
	 * like expanding, shrinking or copying the underlying the matrix.
	 *
	 * @param set the set from which the locks will be acquired.
	 */
	private void takeAllLocks(RandomHashSet<T> set)
	{
		for(int i = 0; i < set.locks.length; i++)
			writeLock(i);
	}

	/**
	 * Release all locks from the given set. This is only used after operations are performed on the entire matrix,
	 * like expanding, shrinking or copying the underlying the matrix.
	 *
	 * @param set the set from which the locks will be released.
	 */
	private void releaseAllLocks(RandomHashSet<T> set)
	{
		for(int i = 0; i < set.locks.length; i++)
			writeUnlock(i);
	}

	/**
	 * Acquire the {@link ReadLock} for a specific object.
	 *
	 * @param obj objects which read lock to acquire.
	 */
	private void readLock(Object obj)
	{
		readLock(obj.hashCode());
	}

	/**
	 * Release the {@link ReadLock} for a specific object.
	 *
	 * @param obj objects which read lock to release.
	 */
	private void readUnlock(Object obj)
	{
		readUnlock(obj.hashCode());
	}

	/**
	 * Acquire the {@link WriteLock} for a specific object.
	 *
	 * @param obj objects which write lock to acquire.
	 */
	private void writeLock(Object obj)
	{
		writeLock(obj.hashCode());
	}

	/**
	 * Release the {@link WriteLock} for a specific object.
	 *
	 * @param obj objects which write lock to release.
	 */
	private void writeUnlock(Object obj)
	{
		writeUnlock(obj.hashCode());
	}

	private void readLock(final int hashCode)
	{
		final int lockIndex = indexOfLock(hashCode);
		final ReadLock lock = locks[lockIndex].readLock();
		lock.lock();
	}

	private void readUnlock(final int hashCode)
	{
		final int lockIndex = indexOfLock(hashCode);
		locks[lockIndex].readLock().unlock();
	}

	private void writeLock(final int hashCode)
	{
		try
		{
			final int lockIndex = indexOfLock(hashCode);
			final WriteLock lock = locks[lockIndex].writeLock();
			while(!lock.tryLock(5, TimeUnit.MILLISECONDS))
			{
				Thread.sleep(Thread.currentThread().hashCode() % 10);
			}
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	private void writeUnlock(final int hashCode)
	{
		final int lockIndex = indexOfLock(hashCode);
		locks[lockIndex].writeLock().unlock();
	}

	private int indexOfLock(final int hashCode)
	{
		return Math.abs(hashCode % locks.length);
	}

	/**
	 * Adds an element to an array.}.
	 *
	 * @param e     the element that should be added.
	 * @param array the array that the record should be added to.
	 * @return true if the element was successfully added to the the given array
	 * index, else false.
	 */
	private boolean addToTable(T e, List<T>[][] array)
	{
		try
		{
			writeLock(e);
			final int lockIndex = indexOfLock(e.hashCode());
			final int index = hashIndex(e, array);
			if(index >= array.length)
				throw new ConcurrentModificationException(
						"Illegal state: Trying to insert in index " + index
								+ " but size of array is " + array.length + "(" + arraySize
								+ ")\n");
			if(array[lockIndex][index] == null)
				array[lockIndex][index] = (List<T>) new ArrayList<Object>();
			if(array[lockIndex][index].contains(e))
				return false;
			return array[lockIndex][index].add(e);
		}
		finally
		{
			writeUnlock(e);
			if(needsResizing())
				changeTableSize();
		}
	}

	private boolean moveToTable(T e, List<T>[][] array)
	{

		final int lockIndex = indexOfLock(e.hashCode());
		if(!locks[lockIndex].writeLock().isHeldByCurrentThread())
			throw new IllegalMonitorStateException();
		final int index = hashIndex(e, array);
		if(index >= array.length)
			throw new ConcurrentModificationException(
					"Illegal state: Trying to insert in index " + index
							+ " but size of array is " + array.length + "(" + arraySize
							+ ")\n");
		if(array[lockIndex][index] == null)
			array[lockIndex][index] = (List<T>) new ArrayList<Object>();
		if(array[lockIndex][index].contains(e))
			return false;
		return array[lockIndex][index].add(e);
	}

	/**
	 * Change the size of the internal data matrix.
	 */
	private void changeTableSize()
	{
		try
		{
			takeAllLocks(this);
			if(!needsResizing())
				return;
			changePrime();
			rehashTable();
		}
		finally
		{
			releaseAllLocks(this);
		}
	}

	/**
	 * Creates a new array when the conditions require it to change size. All
	 * elements will have to be rehashed to fit in the new arrays size.
	 */
	private void rehashTable()
	{
		final List<T>[][] rehashedArray = (List<T>[][]) new ArrayList[locks.length][arraySize];
		for(int n = 0; n < locks.length; n++)
		{
			for(int i = 0; i < table.length; i++)
			{
				final List<T> list = table[n][i];
				if(list != null)
					for(T e : list)
						moveToTable(e, rehashedArray);
			}
		}

		table = rehashedArray;
	}

	/**
	 * Computes the hash for an element and converts it to an index matching the
	 * size of an array.
	 *
	 * @param obj   to compute the hash for.
	 * @param array the array which length has to be taken into consideration.
	 * @return an index based on the hash for the element and which is within
	 * bounds for the size of the array.
	 */
	private int hashIndex(Object obj, Object[] array)
	{
		final int hash = obj.hashCode();
		return hashIndex(hash, array);
	}

	private int hashIndex(int hash, Object[] array)
	{
		final int index = Math.abs(hash % array.length);
		return index;
	}

	private float lowerThreshold()
	{
		return arraySize * (LOAD_FACTOR / 5) - 8;
	}

	private float upperThreshold()
	{
		return arraySize * LOAD_FACTOR;
	}

	private boolean needsResizing()
	{
		return size() < lowerThreshold() || size() > upperThreshold();
	}

	/**
	 * Changes the array size (on next rehash of table) to the next lesser
	 * or greater prime
	 * number.
	 */
	private boolean changePrime()
	{
		if(size() < lowerThreshold())
			arraySize = PRIMES[--primeIndex];
		else if(size() > upperThreshold())
			arraySize = PRIMES[++primeIndex];
		else
			return false;
		return true;
	}

	/**
	 * Finds the next prime number that is equal or greater to the given
	 * argument, and sets the future array size to that number.
	 *
	 * @param initialApproximateArraySize the number that should be compared to.
	 */
	private int setArraySize(int initialApproximateArraySize)
	{
		if(initialApproximateArraySize < PRIMES[0])
			return PRIMES[0];
		for(int i = 0; i < PRIMES.length; i++)
		{
			if(PRIMES[i] >= initialApproximateArraySize)
			{
				arraySize = PRIMES[i];
				primeIndex = i;
				return arraySize;
			}
		}
		return PRIMES[0];
	}

	private List<T> getList(Object obj)
	{
		final int index = hashIndex(obj, table);
		final int lockIndex = indexOfLock(obj.hashCode());
		return table[lockIndex][index];
	}

	@Override
	public boolean add(T arg0)
	{
		if(addToTable(arg0, table))
		{
			size.incrementAndGet();
			return true;
		}

		return false;
	}

	@Override
	public boolean addAll(Collection<? extends T> collection)
	{
		if(collection == this)
			return false;
		boolean changed = false;
		for(T element : collection)
			if(add(element))
				changed = true;

		return changed;
	}

	@Override
	public void clear()
	{
		try
		{
			takeAllLocks(this);
			primeIndex = 0;
			arraySize = 11;
			table = new ArrayList[locks.length][arraySize];
			size.set(0);
		}
		finally
		{
			releaseAllLocks(this);
		}
	}

	@Override
	public boolean contains(Object obj)
	{
		try
		{
			readLock(obj);
			final List<T> list = getList(obj);
			if(list == null)
				return false;

			return list.contains(obj);
		}
		finally
		{
			readUnlock(obj);
		}
	}

	@Override
	public boolean containsAll(Collection<?> collection)
	{
		for(Object obj : collection)
			if(!contains(obj))
				return false;

		return true;
	}

	@Override
	public boolean isEmpty()
	{
		return size() == 0;
	}

	@Override
	public Iterator<T> iterator()
	{
		return new TableIterator<T>();
	}

	@Override
	public boolean remove(Object obj)
	{
		try
		{
			writeLock(obj);
			final List<T> list = getList(obj);
			if(list == null)
				return false;
			if(list.remove(obj))
			{
				size.decrementAndGet();
				return true;
			}
			return false;
		}
		finally
		{
			writeUnlock(obj);
			final float threshold = arraySize * (LOAD_FACTOR / 5) - 8;
			if(size.get() < threshold)
			{
				changeTableSize();
			}
		}
	}

	@Override
	public boolean removeAll(Collection<?> collection)
	{
		boolean changed = false;
		for(Object obj : collection)
			if(remove(obj))
				changed = true;

		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> c)
	{
		Set<?> set = new HashSet<>(c);
		try
		{
			takeAllLocks(this);
			boolean changed = false;
			Iterator<T> iter = iterator();
			while(iter.hasNext())
			{
				if(!set.contains(iter.next()))
				{
					iter.remove();
					changed = true;
				}
			}

			return changed;
		}
		finally
		{
			releaseAllLocks(this);
		}
	}

	@Override
	public int size()
	{
		return size.get();
	}

	@Override
	public Object[] toArray()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T getRandomElement()
	{
		try
		{
			readLock(1);
			if(isEmpty())
				throw new NoSuchElementException("Empty set");
			int index = random.nextInt(arraySize);
			int lockIndex = random.nextInt(locks.length);
			List<T> list = null;

			while(list == null)
			{
				list = table[lockIndex][index];
				index = ++index % arraySize;
				if(index == 0)
					lockIndex = ++lockIndex % locks.length;
			}

			readLock(index);
			try
			{

				final int listIndex = random.nextInt(list.size());
				return list.get(listIndex);
			}
			finally
			{
				readUnlock(index);
			}
		}
		finally
		{
			readUnlock(1);
		}
	}

	@Override
	public String toString()
	{

		final StringBuilder content = new StringBuilder("[");
		for(T e : this)
			content.append(e.toString() + ", ");
		content.deleteCharAt(content.length() - 1);
		content.deleteCharAt(content.length() - 1);
		content.append(']');

		return content.toString();
	}

	private class TableIterator<T> implements Iterator<T>
	{
		private final int startSize;
		private int arrayIndex, listIndex, lockIndex, traversedElements;
		private boolean hasElement = false;

		public TableIterator()
		{
			this.arrayIndex = this.traversedElements = 0;
			this.lockIndex = 0;
			this.listIndex = -1;
			this.startSize = size.get();
		}

		@Override
		public boolean hasNext()
		{
			return traversedElements < startSize;
		}

		@Override
		public T next()
		{
			final int currentLockIndex = lockIndex;
			try
			{
				readLock(currentLockIndex);
				if(!hasNext())
					throw new NoSuchElementException();
				final T next = nextElement();
				traversedElements++;
				hasElement = true;
				return next;
			}
			finally
			{
				readUnlock(currentLockIndex);
			}
		}

		private T nextElement()
		{
			T e = null;

			while(e == null)
			{
				listIndex++;
				if(table[lockIndex][arrayIndex] == null || table[lockIndex][arrayIndex].size() <= listIndex)
				{
					arrayIndex++;
					if(arrayIndex == table[0].length)
					{
						arrayIndex = 0;
						lockIndex++;
					}
					listIndex = -1;
					continue;
				}

				e = (T) table[lockIndex][arrayIndex].get(listIndex);
			}

			return e;
		}

		@Override
		public void remove()
		{
			final int hashCode = table[lockIndex][arrayIndex].hashCode();
			try
			{
				writeLock(table[lockIndex][arrayIndex]);
				if(!hasElement)
					throw new IllegalStateException();
				table[lockIndex][arrayIndex].remove(listIndex);
				size.decrementAndGet();
				hasElement = false;
			}
			finally
			{
				writeUnlock(hashCode);
			}
		}

	}
}
