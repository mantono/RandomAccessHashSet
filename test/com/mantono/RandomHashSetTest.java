package com.mantono;

import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

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
	
	@Test
	public void testConstructorWithLargeInitialSize()
	{
		Set<Integer> set = new RandomHashSet<Integer>(300);
		for(int i = 0; i < 500; i++)
			set.add(i);
		
		assertEquals(500, set.size());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConstructorWithNegativeInitialSize()
	{
		Set<Integer> set = new RandomHashSet<Integer>(-1);
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
	public void testDeterministicGetRandomElementWithStaticSeed()
	{
		final ArrayList[] outcomes = new ArrayList[2];

		for(int i = 0; i < 2; i++)
		{
			outcomes[i] = new ArrayList<Integer>(10);
			RandomHashSet<Integer> set = new RandomHashSet<Integer>(42L);
			set.add(1);
			set.add(2);
			set.add(3);
			for(int n = 0; n < 10; n++)
				outcomes[i].add(set.getRandomElement());

		}

		assertEquals(outcomes[0], outcomes[1]);
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
	
	@Test
	public void testIteratorAdd()
	{
		RandomHashSet<Integer> set = new RandomHashSet<Integer>(42L);
		set.add(1);
		set.add(2);
		set.add(3);
		set.add(4);
		
		Iterator<Integer> iter = set.iterator();
		int iterations = 0;
		int sum = 0;
		
		while(iter.hasNext())
		{
			sum += iter.next();
			iterations++;
		}
		
		assertEquals(iterations, set.size());
		assertEquals(10, sum);
	}
	
	@Test
	public void testIteratorRemove()
	{
		RandomHashSet<Integer> set = new RandomHashSet<Integer>(42L);
		set.add(1);
		set.add(2);
		set.add(3);
		
		Iterator<Integer> iter = set.iterator();
		
		while(iter.hasNext())
		{
			iter.next();
			iter.remove();
		}
		
		assertTrue(set.isEmpty());
	}
	
	@Test(expected=IllegalStateException.class)
	public void testIteratorRemoveTwice()
	{
		RandomHashSet<Integer> set = new RandomHashSet<Integer>(42L);
		set.add(1);
		set.add(2);
		Iterator<Integer> iter = set.iterator();
		iter.next();
		iter.remove();
		iter.remove();
	}
	
	@Test(expected=NoSuchElementException.class)
	public void testIteratorTooManyNext()
	{
		RandomHashSet<Integer> set = new RandomHashSet<Integer>();
		set.add(1);
		Iterator<Integer> iter = set.iterator();
		iter.next();
		iter.next();
	}

	@Test
	public void addAllWithChanges()
	{
		RandomHashSet<Integer> set = new RandomHashSet<Integer>();
		List<Integer> list = new ArrayList(4);
		list.add(1);
		list.add(2);
		list.add(3);
		list.add(4);
		
		assertTrue(set.addAll(list));
	}
	
	@Test
	public void addAllWithPartialChanges()
	{
		RandomHashSet<Integer> set = new RandomHashSet<Integer>();
		set.add(1);
		set.add(2);
		set.add(3);
		List<Integer> list = new ArrayList(4);
		list.add(1);
		list.add(2);
		list.add(3);
		list.add(4);
		
		assertTrue(set.addAll(list));
	}
	
	@Test
	public void addAllWithNoChanges()
	{
		RandomHashSet<Integer> set = new RandomHashSet<Integer>();
		set.add(1);
		set.add(2);
		List<Integer> list = new ArrayList(4);
		list.add(1);
		list.add(2);
		
		assertFalse(set.addAll(list));
	}
	
	@Test
	public void addAllWithSelf()
	{
		RandomHashSet<Integer> set = new RandomHashSet<Integer>();
		set.add(1);
		set.add(2);
		
		assertFalse(set.addAll(set));
	}
	
	@Test
	public void containsAllNoMissing()
	{
		RandomHashSet<Integer> set = new RandomHashSet<Integer>();
		set.add(1);
		set.add(2);
		List<Integer> list = new ArrayList(4);
		list.add(1);
		list.add(2);
		
		assertTrue(set.containsAll(list));
	}
	
	@Test
	public void containsAllWithMissing()
	{
		RandomHashSet<Integer> set = new RandomHashSet<Integer>();
		set.add(1);
		List<Integer> list = new ArrayList(4);
		list.add(1);
		list.add(2);

		assertFalse(set.containsAll(list));
	}

	@Test
	public void threadPerformanceTest()
	{
		BlockingQueue<Instruction> instructions = createInstructions(200_000);
		final Duration timeSequential = executeInstructions(instructions, 1);

		System.out.println("Time for sequential: " + timeSequential);

		System.gc();

		instructions = createInstructions(200_000);
		final Duration timeConcurrent = executeInstructions(instructions, 10);

		System.out.println("Time for concurrent: " + timeConcurrent);

		assertEquals(-1, timeConcurrent.compareTo(timeSequential));
	}

	private Duration executeInstructions(BlockingQueue<Instruction> instructions, int threads)
	{
		RandomHashSet<Integer> set = new RandomHashSet<>(instructions.size() / 2, threads);
		ThreadPoolExecutor tpx = new ThreadPoolExecutor(threads, threads, 300, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100));
		Future<?>[] results = new Future[threads];

		final Instant start = Instant.now();

		for(int i = 0; i < threads; i++)
			results[i] = tpx.submit(new ThreadTester(i, set, instructions));

		try
		{
			for(int i = 0; i < results.length; i++)
				results[i].get();
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
		catch(ExecutionException e)
		{
			e.printStackTrace();
		}

		final Instant finished = Instant.now();
		return Duration.between(start, finished);
	}

	private BlockingQueue<Instruction> createInstructions(int count)
	{
		final BlockingQueue<Instruction> instructions = new ArrayBlockingQueue<>(count);
		final int possibleInstructions = Instruction.values().length;

		Instruction inst;
		final Random rand = new Random();
		do
		{
			final int rb = rand.nextInt(possibleInstructions);
			inst = Instruction.values()[rb];
		}
		while(instructions.offer(inst));

		return instructions;
	}

	private enum Instruction
	{
		ADD,
		CONTAINS,
		REMOVE;
	}

	private class ThreadTester implements Runnable
	{
		private final int id;
		private final RandomHashSet<Integer> set;
		private final BlockingQueue<Instruction> instructions;

		ThreadTester(final int threadId, RandomHashSet<Integer> set, BlockingQueue<Instruction> instructions)
		{
			this.id = threadId;
			this.instructions = instructions;
			this.set = set;
		}

		@Override
		public void run()
		{
			Thread.currentThread().setName("t:" + id);
			Random r = new Random();
			while(!instructions.isEmpty())
			{
				final int e = r.nextInt();
				try
				{
					final Instruction ins = instructions.poll(100, TimeUnit.MILLISECONDS);
					if(ins == null)
						return;
					switch(ins)
					{
						case ADD:
							set.add(e);
							break;
						case CONTAINS:
							set.contains(e);
							break;
						case REMOVE:
							set.remove(e);
							break;
					}
				}
				catch(InterruptedException e1)
				{
					e1.printStackTrace();
				}
			}
		}
	}
}