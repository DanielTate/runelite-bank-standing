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
		assertEquals(20, Unlocks.nextUnlockLevel(1));
		assertEquals(30, Unlocks.nextUnlockLevel(20));
		assertEquals(99, Unlocks.nextUnlockLevel(80));
		assertEquals(-1, Unlocks.nextUnlockLevel(99));
	}
}
