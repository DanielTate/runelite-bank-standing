/*
 * Copyright (c) 2026, dan <dan@tate.nz>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nz.tate.bankstanding.core;

import java.time.LocalDate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

public class CollectionLogTest
{
	@Test
	public void keysAreUniqueAndRatesGetRarer()
	{
		Set<String> keys = new HashSet<>();
		for (CollectionLog.Drop drop : CollectionLog.DROPS)
		{
			assertTrue(keys.add(drop.key));
			assertTrue(drop.hidden ? drop.rate == 0 : drop.rate >= 500);
			assertEquals(drop.group == CollectionLog.Group.SEASONAL, drop.holiday != null);
		}
	}

	@Test
	public void hiddenDropsAreNeverRolled()
	{
		Random everyRollHits = new Random()
		{
			@Override
			public int nextInt(int bound)
			{
				return 0;
			}
		};
		CollectionLog.Drop rarest = CollectionLog.roll(everyRollHits, LocalDate.of(2026, 8, 1), "ge");
		assertFalse(rarest.hidden);
		assertTrue(CollectionLog.find(CollectionLog.BANDAGES).hidden);
	}

	@Test
	public void rareDropsAreRareAndJunkIsCommon()
	{
		Random random = new Random(1);
		LocalDate ordinaryDay = LocalDate.of(2026, 8, 1);
		int rare = 0;
		int junk = 0;
		for (int i = 0; i < 1_000_000; i++)
		{
			CollectionLog.Drop drop = CollectionLog.roll(random, ordinaryDay, "ge");
			if (drop == null)
			{
				continue;
			}
			assertTrue(drop.holiday == null);
			if (drop.rare())
			{
				rare++;
			}
			else
			{
				junk++;
			}
		}
		// Rare: about 20 + 4 + 1 + 27 (outfit) per million rolls. Junk: about 4,000.
		assertTrue(rare > 25 && rare < 90);
		assertTrue(junk > 3_000 && junk < 5_000);
	}

	@Test
	public void seasonalDropsOnlyOnTheirHoliday()
	{
		Random random = new Random(2);
		boolean cobweb = false;
		for (int i = 0; i < 200_000; i++)
		{
			CollectionLog.Drop drop = CollectionLog.roll(random, LocalDate.of(2026, 10, 31), "ge");
			cobweb |= drop != null && drop.key.equals("cobweb");
			assertTrue(drop == null || drop.holiday == null || drop.holiday == Holiday.HALLOWEEN);
		}
		assertTrue(cobweb);
	}

	@Test
	public void archiveJunkOnlyAtTheArchive()
	{
		Random random = new Random(3);
		boolean archiveJunk = false;
		for (int i = 0; i < 200_000; i++)
		{
			CollectionLog.Drop drop = CollectionLog.roll(random, LocalDate.of(2026, 8, 1), "fractured-archive-east");
			archiveJunk |= drop != null && drop.group == CollectionLog.Group.ARCHIVE;
		}
		assertTrue(archiveJunk);
		for (int i = 0; i < 200_000; i++)
		{
			CollectionLog.Drop drop = CollectionLog.roll(random, LocalDate.of(2026, 8, 1), "ge");
			assertTrue(drop == null || drop.group != CollectionLog.Group.ARCHIVE);
		}
	}

	@Test
	public void collectCountsAndRemembersTheFirst()
	{
		Progress progress = new Progress();
		progress.rolls = 1234;
		CollectionLog.Drop pin = CollectionLog.find("lost-bank-pin");
		assertTrue(CollectionLog.collect(progress, pin, 99));
		progress.rolls = 5000;
		assertFalse(CollectionLog.collect(progress, pin, 500));
		CollectionLog.Obtained obtained = progress.collection.get("lost-bank-pin");
		assertEquals(2, obtained.count);
		assertEquals(99, obtained.firstAt);
		assertEquals(1234, obtained.firstRolls);
		assertFalse(CollectionLog.hasPet(progress));
		CollectionLog.collect(progress, CollectionLog.find("pet-moneybag"), 1);
		assertTrue(CollectionLog.hasPet(progress));
	}

	@Test
	public void banksCountOnlyListedOnes()
	{
		Progress progress = new Progress();
		progress.bank("varrock-west", 3185, 3441);
		progress.bank("region-12345", 1, 1);
		progress.bank("tutorial-island", 3122, 3124);
		assertEquals(1, CollectionLog.banksVisited(progress));
	}
}
