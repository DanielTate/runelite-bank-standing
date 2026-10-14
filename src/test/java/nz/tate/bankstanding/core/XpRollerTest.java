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
	public void maxHitRisesFromOneToTen()
	{
		assertEquals(1, XpRoller.baseMaxHit(1));
		assertEquals(10, XpRoller.baseMaxHit(99));
		assertEquals(13, XpRoller.maxHit(99, 7));
	}

	@Test
	public void rollsStayInRangeAndReachTheMax()
	{
		int highest = 0;
		for (int i = 0; i < 200_000; i++)
		{
			int xp = roller.roll(99, 0, 0);
			assertTrue(xp >= 1 && xp <= 10);
			highest = Math.max(highest, xp);
		}
		assertEquals(10, highest);
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
		assertEquals(0.58, ones / (double) n, 0.01);
	}

	@Test
	public void boostRaisesTheAverageByThatFraction()
	{
		double base = average(0);
		assertEquals(base * 1.05, average(0.05), 0.02);
		assertEquals(base * 1.10, average(0.10), 0.02);
		assertEquals("boost is capped", average(0.10), average(0.5), 0.02);
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
}
