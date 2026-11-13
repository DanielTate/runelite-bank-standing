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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BankOfTheDayTest
{
	@Test
	public void everyBankComesUpOnceBeforeAnyRepeats()
	{
		List<String> banks = BankRegions.allKeys();
		long first = Math.floorDiv(LocalDate.of(2026, 11, 13).toEpochDay(), banks.size()) * banks.size();
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < banks.size(); i++)
		{
			seen.add(BankOfTheDay.key(LocalDate.ofEpochDay(first + i)));
		}
		assertEquals(new HashSet<>(banks), seen);
	}

	@Test
	public void sameDaySameBank()
	{
		LocalDate day = LocalDate.of(2026, 11, 13);
		assertEquals(BankOfTheDay.key(day), BankOfTheDay.key(LocalDate.of(2026, 11, 13)));
		assertTrue(BankRegions.allKeys().contains(BankOfTheDay.key(day)));
		assertNotEquals(BankOfTheDay.key(day), BankOfTheDay.key(day.plusDays(1)));
	}

	@Test
	public void changesAtUtcMidnight()
	{
		long midnight = LocalDate.of(2026, 11, 14).toEpochDay() * 86_400_000L;
		assertEquals(LocalDate.of(2026, 11, 13), BankOfTheDay.today(midnight - 1));
		assertEquals(LocalDate.of(2026, 11, 14), BankOfTheDay.today(midnight));
		assertEquals(1, BankOfTheDay.millisUntilChange(midnight - 1));
		assertEquals(86_400_000L, BankOfTheDay.millisUntilChange(midnight));
		assertEquals("1m", BankOfTheDay.timeLeft(midnight - 1));
		assertEquals("5h 30m", BankOfTheDay.timeLeft(midnight - (5 * 60 + 30) * 60_000L));
	}

	@Test
	public void tutorialIslandIsNeverPicked()
	{
		assertTrue(!BankRegions.allKeys().contains("tutorial-island"));
	}
}
