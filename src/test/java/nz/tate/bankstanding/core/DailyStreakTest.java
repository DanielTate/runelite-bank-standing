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
import org.junit.Test;

public class DailyStreakTest
{
	private static final LocalDate MON = LocalDate.of(2026, 10, 12);

	private static void stand(Progress p, LocalDate day, long ticks)
	{
		for (long i = 0; i < ticks; i++)
		{
			DailyStreak.standingTick(p, day);
		}
	}

	@Test
	public void aDayCountsAfterThirtyMinutes()
	{
		Progress p = new Progress();
		stand(p, MON, DailyStreak.TICKS_PER_DAY_NEEDED - 1);
		assertEquals(0, DailyStreak.current(p, MON));
		stand(p, MON, 1);
		assertEquals(1, DailyStreak.current(p, MON));
	}

	@Test
	public void consecutiveDaysGrowTheStreak()
	{
		Progress p = new Progress();
		stand(p, MON, DailyStreak.TICKS_PER_DAY_NEEDED);
		stand(p, MON.plusDays(1), DailyStreak.TICKS_PER_DAY_NEEDED);
		assertEquals(2, DailyStreak.current(p, MON.plusDays(1)));
		assertEquals("still alive the next day", 2, DailyStreak.current(p, MON.plusDays(2)));
	}

	@Test
	public void missingADayResetsIt()
	{
		Progress p = new Progress();
		stand(p, MON, DailyStreak.TICKS_PER_DAY_NEEDED);
		assertEquals(0, DailyStreak.current(p, MON.plusDays(2)));
		stand(p, MON.plusDays(2), DailyStreak.TICKS_PER_DAY_NEEDED);
		assertEquals(1, DailyStreak.current(p, MON.plusDays(2)));
	}

	@Test
	public void keepsEachDaysStandingForAYear()
	{
		Progress p = new Progress();
		java.time.LocalDate day = java.time.LocalDate.of(2026, 1, 1);
		DailyStreak.standingTick(p, day);
		DailyStreak.standingTick(p, day);
		org.junit.Assert.assertEquals(Long.valueOf(2), p.dayTicks.get("2026-01-01"));
		DailyStreak.standingTick(p, day.plusDays(DailyStreak.DAY_TICKS_KEPT - 1));
		org.junit.Assert.assertTrue(p.dayTicks.containsKey("2026-01-01"));
		DailyStreak.standingTick(p, day.plusDays(DailyStreak.DAY_TICKS_KEPT));
		org.junit.Assert.assertFalse(p.dayTicks.containsKey("2026-01-01"));
	}

	@Test
	public void keepsEachDaysBanksForAYear()
	{
		Progress p = new Progress();
		java.time.LocalDate day = java.time.LocalDate.of(2026, 1, 1);
		DailyStreak.standingTick(p, day, "ge");
		DailyStreak.standingTick(p, day, "ge");
		DailyStreak.standingTick(p, day, "varrock-west");
		org.junit.Assert.assertEquals(Long.valueOf(2), p.dayBanks.get("2026-01-01").get("ge"));
		org.junit.Assert.assertEquals(Long.valueOf(1), p.dayBanks.get("2026-01-01").get("varrock-west"));
		DailyStreak.standingTick(p, day.plusDays(DailyStreak.DAY_TICKS_KEPT), "ge");
		org.junit.Assert.assertFalse(p.dayBanks.containsKey("2026-01-01"));
	}
}
