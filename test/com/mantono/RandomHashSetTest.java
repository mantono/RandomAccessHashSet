package com.mantono;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Test;

public class RandomHashSetTest
{

	@Test
	public void testAdd()
	{
		Set<Integer> set = new RandomHashSet<Integer>();
		assertTrue(set.add(1));
		assertFalse(set.add(1));
	}
	
	@Test
	public void testRemove()
	{
		Set<Integer> set = new RandomHashSet<Integer>();
		assertTrue(set.add(1));
		assertTrue(set.remove(1));
		assertFalse(set.remove(1));
	}
	

	@Test
	public void testContains()
	{
		Set<Integer> set = new RandomHashSet<Integer>();
		assertTrue(set.add(1));
		assertTrue(set.contains(1));
	}

	@Test
	public void testSize()
	{
		Set<Integer> set = new RandomHashSet<Integer>();
		assertEquals(0, set.size());
		set.add(1);
		set.add(2);
		set.add(3);
		assertEquals(3, set.size());
		set.add(3);
		assertEquals(3, set.size());
		set.remove(2);
		assertEquals(2, set.size());
	}

	@Test
	public void testIsEmpty()
	{
		Set<Integer> set = new RandomHashSet<Integer>();
		assertTrue(set.isEmpty());
		set.add(1);
		assertFalse(set.isEmpty());
		set.remove(1);
		assertTrue(set.isEmpty());
	}

	@Test
	public void testClear()
	{
		Set<Integer> set = new RandomHashSet<Integer>();
		set.add(1);
		set.add(2);
		set.add(3);
		assertEquals(3, set.size());
		set.clear();
		assertEquals(0, set.size());
		set.add(1);
		set.add(2);
		assertEquals(2, set.size());
	}
	
	@Test
	public void testExpand()
	{
		final int limit = 51;
		Set<Integer> set = new RandomHashSet<Integer>();
		for(int i = 0; i < limit; i++)
			set.add(i);
		assertEquals(limit, set.size());
		set.clear();
		assertEquals(0, set.size());
		for(int i = 0; i < limit; i++)
			set.add(i);
		assertEquals(limit, set.size());
	}
	
	@Test
	public void testShrink()
	{
		final int limit = 193;
		Set<Integer> set = new RandomHashSet<Integer>();
		for(int i = 0; i < limit; i++)
			set.add(i);
		assertEquals(limit, set.size());

		for(int i = 0; i < limit; i++)
			set.remove(i);
		assertEquals(0, set.size());
		
		for(int i = 0; i < limit; i++)
			set.add(i);
		assertEquals(limit, set.size());
	}
	
	@Test(expected=NoSuchElementException.class)
	public void testExceptionOnEmptySet()
	{
		RandomHashSet<Integer> set = new RandomHashSet<Integer>();
		set.getRandomElement();
	}
	
	@Test()
	public void testGetRandomElementWithOneElement()
	{
		RandomHashSet<Integer> set = new RandomHashSet<Integer>();
		set.add(1);
		assertTrue(1 == set.getRandomElement());
	}
	
	@Test()
	public void testGetRandomElementWithStaticSeed()
	{
		RandomHashSet<Integer> set = new RandomHashSet<Integer>(42L);
		set.add(1);
		set.add(2);
		set.add(3);
		assertTrue(1 == set.getRandomElement());
		assertTrue(1 == set.getRandomElement());
		assertTrue(3 == set.getRandomElement());
		assertTrue(1 == set.getRandomElement());
		assertTrue(1 == set.getRandomElement());
		assertTrue(1 == set.getRandomElement());
		assertTrue(2 == set.getRandomElement());
		assertTrue(2 == set.getRandomElement());
	}
	
	@Test
	public void testRetainAll()
	{
		RandomHashSet<Integer> set = new RandomHashSet<Integer>(42L);
		set.add(1);
		set.add(2);
		set.add(3);
		set.add(4);
		
		List<Integer> list = new ArrayList<Integer>(4);
		list.add(3);
		list.add(4);
		list.add(5);
		list.add(6);
		
		assertTrue(set.retainAll(list));
		assertEquals(2, set.size());
		assertTrue(set.contains(3));
		assertTrue(set.contains(4));
		assertFalse(set.retainAll(list));
		assertEquals(2, set.size());
	}
}