package com.mantono;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RandomHashSet<T> implements RandomAccess<T>, Set<T>
{

	/**
	 * 
	 */
	private final static int[] PRIMES = {23, 53, 97, 193, 389, 769, 1543, 3079, 6151, 12289, 24593, 49157, 98317,
			196613, 393241, 786433, 1572869, 3145739, 6291469, 12582917, 25165843, 50331653, 100663319, 201326611,
			402653189, 805306457, 1610612741};

	private List<T>[] table;
	private volatile int arraySize = 11;
	private volatile int primeIndex = -1;
	private volatile int count = 0;
	private final Lock hashLock = new ReentrantLock();
	private final Condition hashFinished = hashLock.newCondition();
	private final Random random;

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
	 */
	public RandomHashSet(final int elementCount, final long seed)
	{
		setArraySize(elementCount / 3);
		this.table = new ArrayList[arraySize];
		this.random = new Random(seed);
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
	 */
	public RandomHashSet(final int elementCount)
	{
		setArraySize(elementCount / 3);
		this.table = new ArrayList[arraySize];
		this.random = new SecureRandom();
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
	 * Adds a {@link CollisionRecord} to array of {@link CollisionSet}.
	 * 
	 * @param e the {@link CollisionRecord} that should be added.
	 * @param array the array that the record should be added to.
	 * @return true if it was successfully added to the {@link CollisionSet} at
	 * the given array index, else false.
	 */
	private boolean addToTable(T e, List<T>[] array)
	{
		try
		{
			hashLock.lock();
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
			hashLock.unlock();
		}
	}

	/**
	 * Expands the size of the internal array.
	 */
	private void expand()
	{
		hashLock.lock();
		advanceTotNextPrime();
		rehashTable();
		hashFinished.signalAll();
		hashLock.unlock();
	}

	/**
	 * Shrinks the size of the internal array. This method is currently not
	 * used.
	 */
	private void shrink()
	{
		hashLock.lock();
		previousPrime();
		rehashTable();
		hashFinished.signalAll();
		hashLock.unlock();
	}

	/**
	 * Waits for this class {@link ReentrantLock} to be released that is locking
	 * the internal array.
	 * 
	 * A more compact version of {@link Condition#await()} with exception
	 * handling taken care of, so this will not have to repeated every time
	 * <code>hashFinished.await()</code> would be called.
	 */
	private void waitForHash()
	{
		try
		{
			while(table.length != arraySize)
				hashFinished.await();
		}
		catch(InterruptedException e)
		{
			hashLock.unlock();
			e.printStackTrace();
		}
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
	 * @param element to compute the hash for.
	 * @param array the array which length has to be taken into consideration.
	 * @return an index based on the hash for the element and which is within
	 * bounds for the size of the array.
	 */
	private int hashIndex(T obj, Object[] array)
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
}
