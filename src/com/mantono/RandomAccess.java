package com.mantono;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * All data structures that implements the {@link Iterable} interface allows the
 * retrieval of a random element in the data structure. For example;
 *
 * <pre>
 * {@code
 * 	final SecureRandom random = new SecureRandom();
 * 	final int elementIndex = random.nextInt(setOfElements.size());
 * 	int i = 0;
 * 	Iterator<Object> iterator = setOfElements.iterator();
 * 	while(iterator.hasNext())
 * 	{
 * 		if(i++ == elementIndex)
 * 			return iterator.next();
 * 		iterator.next();
 * 	}
 * }
 * </pre>
 *
 * would retrieve a random element from the collection
 * <code>setOfElements</code>, but it would be done with a time complexity of
 * <code>O(n)</code>.
 *
 * This interface is for data structures that supports retrieval of a random
 * element within the data structure by a dedicated method. This is preferably
 * implemented with a time complexity of <code>O(log n)</code> or
 * <code>O(1)</code> as opposed to using an iterator for random access of data
 * element as with the example above.
 *
 * Unlike the default {@link java.util.RandomAccess} found in the Java Standard
 * Library, this interface can be applied to for examples a {@link Set} and not
 * only a {@link List} implementations. Additionally, this interface has one
 * declared method, unlike the previously mentioned interface which is only a
 * maker interface.
 * 
 * @author Anton &Ouml;sterberg
 *
 * @param <T> the type for the elements in the data structure.
 */

public interface RandomAccess<T>
{
	/**
	 * Get a random element, but do not remove it.
	 * 
	 * @return a random element in the data structure
	 * 
	 * @throws NoSuchElementException if this data structure is empty
	 */
	T getRandomElement();
}
