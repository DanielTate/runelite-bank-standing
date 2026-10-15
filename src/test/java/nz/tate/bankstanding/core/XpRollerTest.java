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

import java.util.Random;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class XpRollerTest
{
	private final XpRoller roller = new XpRoller(new Random(1));

	@Test
	public void maxHitRisesEveryTwoLevelsToFifty()
	{
		assertEquals(1, XpRoller.baseMaxHit(1));
		assertEquals(1, XpRoller.baseMaxHit(2));
		assertEquals(2, XpRoller.baseMaxHit(3));
		assertEquals(25, XpRoller.baseMaxHit(50));
		assertEquals(50, XpRoller.baseMaxHit(99));
		assertEquals(62, XpRoller.maxHit(99, 7));
	}

	@Test
	public void rollsStayInRangeAndReachTheMax()
	{
		int highest = 0;
		for (int i = 0; i < 200_000; i++)
		{
			int xp = roller.roll(99, 0, 0);
			assertTrue(xp >= 1 && xp <= 50);
			highest = Math.max(highest, xp);
		}
		assertEquals(50, highest);
	}

	@Test
	public void oneIsTheMostCommonDrop()
	{
		int ones = 0;
		int n = 100_000;
		for (int i = 0; i < n; i++)
		{
			if (roller.roll(99, 0, 0) == 1)
			{
				ones++;
			}
		}
		assertEquals(0.072, ones / (double) n, 0.005);
	}

	@Test
	public void boostRaisesTheAverageByThatFraction()
	{
		double base = average(0);
		assertEquals(base * 1.10, average(0.10), 0.1);
		assertEquals(base * 1.22, average(0.22), 0.1);
		assertEquals("boost is capped", average(0.22), average(0.5), 0.1);
	}

	private double average(double boost)
	{
		XpRoller seeded = new XpRoller(new Random(7));
		long sum = 0;
		int n = 400_000;
		for (int i = 0; i < n; i++)
		{
			sum += seeded.roll(99, boost, 0);
		}
		return sum / (double) n;
	}

	@Test
	public void boostedRollsAreAlwaysAboveTheNormalMax()
	{
		XpRoller roller = new XpRoller(new java.util.Random(3));
		for (int level : new int[]{1, 50, 99})
		{
			for (int i = 0; i < 1000; i++)
			{
				int hit = roller.boostedRoll(level);
				org.junit.Assert.assertTrue(hit > XpRoller.baseMaxHit(level));
				org.junit.Assert.assertTrue(hit <= XpRoller.baseMaxHit(level) + XpRoller.HIT_PER_BONUS);
			}
		}
	}
}
