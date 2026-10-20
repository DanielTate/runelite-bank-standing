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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class UnlocksTest
{
	@Test
	public void rangeGrowsWithLevel()
	{
		assertEquals(1, Unlocks.bankRange(1));
		assertEquals(1, Unlocks.bankRange(19));
		assertEquals(2, Unlocks.bankRange(20));
		assertEquals(3, Unlocks.bankRange(30));
		assertEquals(8, Unlocks.bankRange(80));
		assertEquals(8, Unlocks.bankRange(98));
		assertEquals(9, Unlocks.bankRange(99));
	}

	@Test
	public void eachRangeLineIsAnIncrease()
	{
		for (Unlocks.Unlock unlock : Unlocks.ALL)
		{
			if (unlock.text.startsWith("Bank stand within"))
			{
				assertEquals(unlock.level > 1, Unlocks.bankRange(unlock.level - 1) < Unlocks.bankRange(unlock.level));
			}
		}
	}

	@Test
	public void nextUnlock()
	{
		assertEquals(5, Unlocks.nextUnlockLevel(1));
		assertEquals(10, Unlocks.nextUnlockLevel(5));
		assertEquals(15, Unlocks.nextUnlockLevel(10));
		assertEquals(85, Unlocks.nextUnlockLevel(80));
		assertEquals(90, Unlocks.nextUnlockLevel(85));
		assertEquals(99, Unlocks.nextUnlockLevel(90));
		assertEquals(-1, Unlocks.nextUnlockLevel(99));
	}

	@Test
	public void startDelayShortens()
	{
		assertEquals(StandTracker.START_DELAY_TICKS, Unlocks.startDelayTicks(1));
		assertEquals(42, Unlocks.startDelayTicks(15));
		assertEquals(17, Unlocks.startDelayTicks(99));
	}

	@Test
	public void cosmeticsByLevel()
	{
		assertEquals(Unlocks.Rug.NONE, Unlocks.rug(9));
		assertEquals(Unlocks.Rug.WORN, Unlocks.rug(10));
		assertEquals(Unlocks.Rug.RED, Unlocks.rug(98));
		assertEquals(Unlocks.Rug.GOLD, Unlocks.rug(99));
		assertEquals(Unlocks.Seat.NONE, Unlocks.seat(59));
		assertEquals(Unlocks.Seat.STOOL, Unlocks.seat(60));
		assertEquals(Unlocks.Seat.THRONE, Unlocks.seat(99));
		assertEquals("Loiterer", Unlocks.title(1));
		assertEquals("Bank Standing Master", Unlocks.title(99));
		assertEquals(0, Unlocks.peekTicks(4));
		assertEquals(2, Unlocks.peekTicks(5));
		assertEquals(3, Unlocks.peekTicks(10));
		assertEquals(8, Unlocks.peekTicks(50));
		assertEquals(10, Unlocks.peekTicks(69));
		assertEquals(17, Unlocks.peekTicks(70));
		assertEquals(Unlocks.PEEK_TICKS, Unlocks.peekTicks(85));
		assertEquals(Unlocks.PEEK_TICKS, Unlocks.peekTicks(99));
	}

	@Test
	public void everyCategoryHasLines()
	{
		for (Unlocks.Category category : Unlocks.Category.values())
		{
			assertTrue(category.title, !Unlocks.in(category).isEmpty());
		}
		for (Unlocks.Unlock unlock : Unlocks.ALL)
		{
			assertTrue(unlock.text, unlock.item > 0 || unlock.sprite > 0);
		}
	}

	@Test
	public void guideIsInLevelOrder()
	{
		int last = 0;
		for (Unlocks.Unlock unlock : Unlocks.ALL)
		{
			assertTrue(unlock.level >= last);
			last = unlock.level;
		}
	}
}
