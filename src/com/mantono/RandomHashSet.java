package com.mantono;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RandomHashSet<T> implements RandomAccess<T>, Set<T>
{

	/**
	 * 
	 */
	private final static int[] PRIMES = {23, 53, 97, 193, 389, 769, 1543, 3079, 6151, 12289, 24593, 49157, 98317,
			196613, 393241, 786433, 1572869, 3145739, 6291469, 12582917, 25165843, 50331653, 100663319, 201326611,
			402653189, 805306457, 1610612741};

	private final static float LOAD_FACTOR = 1.6f;

	private List<T>[] table;
	private volatile int arraySize = 11;
	private volatile int primeIndex = -1;
	private AtomicInteger size = new AtomicInteger();
	private final ReentrantReadWriteLock tableLock = new ReentrantReadWriteLock(true);
	private final Lock writeLock = tableLock.writeLock();
	private final Lock readLock = tableLock.readLock();
	private final Random random;

	/**
	 * Private constructor which is called by most other constructors. This
	 * constructor offers no extra functionality or options for the end user,
	 * but reduces code complexity and code duplication.
	 *
	 * @param elementCount the expected amount of elements that this data
	 * structure will hold.
	 * @param random the random generator that is used when retrieveing random
	 * elements.
	 * @throws IllegalArgumentException if <code>elementCount</code> is
	 * negative.
	 */
	private RandomHashSet(final int elementCount, final Random random)
	{
		initTable(elementCount);
		this.random = random;
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param elementCount the expected amount of elements that this data
	 * structure will hold.
	 * @param seed is the initial seed that the {@link Random} generator will be
	 * initialized with. Setting a specific seed is useful when the the data
	 * structure will be used in scientific evaluation, or for some other reason
	 * where the access of the elements should be random but the order that they
	 * were accessed in should be repeatable (by entering the same seed).
	 * @throws IllegalArgumentException if <code>elementCount</code> is
	 * negative.
	 */
	public RandomHashSet(final int elementCount, final long seed)
	{
		this(elementCount, new Random(seed));
	}

	/**
	 * Constructor for this class. When no second argument is given and the seed
	 * to the random generator is ommitted, an instance of {@link SecureRandom}
	 * is used instead of simply {@link Random}. This is a better approach when
	 * it is required that the sequence of random elements fetched through
	 * {@link RandomHashSet#getRandomElement()} is truly unpredictable.
	 * 
	 * @param elementCount the expected amount of elements that this data
	 * structure will hold.
	 * @throws IllegalArgumentException if <code>elementCount</code> is
	 * negative.
	 */
	public RandomHashSet(final int elementCount)
	{
		this(elementCount, new SecureRandom());
	}

	/**
	 * Constructor for creating a new {@link RandomHashSet} from another
	 * {@link RandomHashSet}. The {@link Random} generator that is used in the
	 * originating set will be kept in the new set.
	 * 
	 * @param set the {@link RandomHashSet} which this set should be recreated
	 * from.
	 */
	public RandomHashSet(final RandomHashSet<T> set)
	{
		try
		{
			set.readLock.lock();
			this.table = set.table;
			this.random = set.random;
			this.arraySize = set.arraySize;
			this.primeIndex = set.primeIndex;
			this.size = set.size;
		}
		finally
		{
			set.readLock.unlock();
		}

	}

	/**
	 * Constructor for creating a new {@link RandomHashSet} from an existing
	 * {@link Set}. Since a seed to the random generator is ommitted, an
	 * instance of {@link SecureRandom} is used instead of simply
	 * {@link Random}.
	 * 
	 * @param set the {@link Set} which this set should be recreated from.
	 */
	public RandomHashSet(final Set<T> set)
	{
		this(set.size(), new SecureRandom());
		addAll(set);
	}

	/**
	 * Constructor for creating a new {@link RandomHashSet} from an existing
	 * {@link Set}. This constructor uses a regular {@link Random} generator.
	 * 
	 * @param set the {@link Set} which this set should be recreated from.
	 */
	public RandomHashSet(final Set<T> set, final long seed)
	{
		this(set.size(), new Random(seed));
		addAll(set);
	}

	/**
	 * Constructor for this class. This constructor will use the default array
	 * size of 11 upon initialization.
	 * 
	 * @param seed is the initial seed that the {@link Random} generator will be
	 * initialized with. Setting a specific seed is useful when the the data
	 * structure will be used in scientific evaluation, or for some other reason
	 * where the access of the elements should be random but the order that they
	 * were accessed in should be repeatable (by entering the same seed).
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
	 * @param elementCount the expected amount of elements that this data
	 * structure will hold.
	 * @throws IllegalArgumentException if <code>elementCount</code> is
	 * negative.
	 */
	private void initTable(final int elementCount)
	{
		if(elementCount < 0)
			throw new IllegalArgumentException("Initial size for set cannot be less than zero.");
		setArraySize(elementCount / 3);
		this.table = new ArrayList[arraySize];
	}

	/**
	 * Adds an element to an array.}.
	 * 
	 * @param e the element that should be added.
	 * @param array the array that the record should be added to.
	 * @return true if the element was successfully added to the the given array
	 * index, else false.
	 */
	private boolean addToTable(T e, List<T>[] array)
	{
		try
		{
			writeLock.lock();
			final int index = hashIndex(e, array);
			if(index >= array.length)
				throw new ConcurrentModificationException("Illegal state: Trying to insert in index " + index
						+ " but size of array is " + array.length + "(" + arraySize + ")\n");
			if(array[index] == null)
				array[index] = (List<T>) new ArrayList<Object>();
			if(array[index].contains(e))
				return false;
			return array[index].add(e);
		}
		finally
		{
			final float threshold = arraySize * LOAD_FACTOR;
			if(size.get() > threshold)
				expand();
			writeLock.unlock();
		}
	}

	/**
	 * Expands the size of the internal array.
	 */
	private void expand()
	{
		writeLock.lock();
		advanceTotNextPrime();
		rehashTable();
		writeLock.unlock();
	}

	/**
	 * Shrinks the size of the internal array.
	 */
	private void shrink()
	{
		writeLock.lock();
		previousPrime();
		rehashTable();
		writeLock.unlock();
	}

	/**
	 * Creates a new array when the conditions require it to change size. All
	 * elements will have to be rehashed to fit in the new arrays size.
	 */
	private void rehashTable()
	{
		final List<T>[] rehashedArray = (List<T>[]) new ArrayList[arraySize];
		for(int i = 0; i < table.length; i++)
		{
			final List<T> list = table[i];
			if(list != null)
				for(T e : list)
					addToTable(e, rehashedArray);
		}

		table = rehashedArray;
	}

	/**
	 * Computes the hash for an element and converts it to an index matching the
	 * size of an array.
	 * 
	 * @param obj to compute the hash for.
	 * @param array the array which length has to be taken into consideration.
	 * @return an index based on the hash for the element and which is within
	 * bounds for the size of the array.
	 */
	private int hashIndex(Object obj, Object[] array)
	{
		final int hash = obj.hashCode();
		final int index = hash % array.length;
		return index;
	}

	/**
	 * Changes the array size (on next rehash of table) to the next greater
	 * prime number.
	 */
	private void advanceTotNextPrime()
	{
		arraySize = PRIMES[++primeIndex];
	}

	/**
	 * Changes the array size (on next rehash of table) to the next lesser prime
	 * number.
	 */
	private void previousPrime()
	{
		arraySize = PRIMES[--primeIndex];
	}

	/**
	 * Finds the next prime number that is equal or greater to the given
	 * argument, and sets the future array size to that number.
	 * 
	 * @param initialApproximateArraySize the number that should be compared to.
	 */
	private void setArraySize(int initialApproximateArraySize)
	{
		if(initialApproximateArraySize < 11)
			return;
		for(int i = 0; i < PRIMES.length; i++)
		{
			if(PRIMES[i] >= initialApproximateArraySize)
			{
				arraySize = PRIMES[i];
				primeIndex = i;
				return;
			}
		}
	}

	private List<T> getList(Object obj)
	{
		final int index = hashIndex(obj, table);
		return table[index];
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
			writeLock.lock();
			primeIndex = 0;
			arraySize = 11;
			table = new ArrayList[arraySize];
			size.set(0);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	@Override
	public boolean contains(Object obj)
	{
		try
		{
			readLock.lock();
			final List<T> list = getList(obj);
			if(list == null)
				return false;

			return list.contains(obj);
		}
		finally
		{
			readLock.unlock();
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
			writeLock.lock();
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
			final float threshold = arraySize * (LOAD_FACTOR / 5) - 8;
			if(size.get() < threshold)
				shrink();
			writeLock.unlock();
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
			writeLock.lock();
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
			writeLock.unlock();
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
			readLock.lock();
			if(isEmpty())
				throw new NoSuchElementException("Empty set");
			int index = random.nextInt(arraySize);
			List<T> list = null;

			while(list == null)
			{
				list = table[index];
				index = ++index % arraySize;
			}

			index = random.nextInt(list.size());
			return list.get(index);
		}
		finally
		{
			readLock.unlock();
		}
	}

	private class TableIterator<T> implements Iterator<T>
	{
		private final int startSize;
		private int arryIndex, listIndex, traversedElements;
		private boolean hasElement = false;

		public TableIterator()
		{
			this.arryIndex = this.traversedElements = 0;
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
			try
			{
				readLock.lock();
				if(!hasNext())
					throw new NoSuchElementException();
				final T next = nextElement();
				traversedElements++;
				hasElement = true;
				return next;
			}
			finally
			{
				readLock.unlock();
			}
		}

		private T nextElement()
		{
			T e = null;

			while(e == null)
			{
				listIndex++;
				if(table[arryIndex] == null || table[arryIndex].size() <= listIndex)
				{
					arryIndex++;
					listIndex = -1;
					continue;
				}

				e = (T) table[arryIndex].get(listIndex);
			}

			return e;
		}

		@Override
		public void remove()
		{
			try
			{
				writeLock.lock();
				if(!hasElement)
					throw new IllegalStateException();
				table[arryIndex].remove(listIndex);
				size.decrementAndGet();
				hasElement = false;
			}
			finally
			{
				writeLock.unlock();
			}
		}

	}
}
