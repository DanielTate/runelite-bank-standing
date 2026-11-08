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
import java.time.YearMonth;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class StatsViewTest
{
	@Test
	public void showsLevelProgressAndRemainingXp()
	{
		Progress progress = new Progress();
		progress.xp = 415 + 230;
		StatsView view = view(progress);

		assertEquals(2, view.level);
		assertEquals("645", view.totalXp);
		assertEquals("225", view.toNextLevel);
		assertEquals(50, view.percent);
	}

	@Test
	public void level99IsMaxedButXpStillShows()
	{
		Progress progress = new Progress();
		progress.xp = 70_000_000;
		StatsView view = view(progress);

		assertEquals("Maxed", view.toNextLevel);
		assertEquals("70,000,000", view.totalXp);
	}

	@Test
	public void banksAreSortedByTimeSpent()
	{
		Progress progress = new Progress();
		progress.bank("edgeville", 0, 0).ticks = 100;
		progress.bank("ge", 0, 0).ticks = 6000 * 3 + 500;

		StatsView view = view(progress);

		assertEquals("Grand Exchange", view.banks.get(0).name);
		assertEquals("3h 05m", view.banks.get(0).time);
		assertEquals("1m", view.banks.get(1).time);
	}

	@Test
	public void sessionStatsAndXpPerHour()
	{
		Session session = new Session();
		session.ticks = 3000;
		session.xp = 450;
		StatsView view = new StatsView(new Progress(), session, "2026-10-14");

		assertEquals("450", view.sessionXp);
		assertEquals("30m", view.sessionTime);
		assertEquals("900", view.xpPerHour);
	}

	@Test
	public void xpPerHourWaitsForTheFirstMinute()
	{
		Session session = new Session();
		session.ticks = 6;
		session.xp = 5;
		assertEquals(0, session.xpPerHour());
	}

	@Test
	public void todaysTimeOnlyCountsToday()
	{
		Progress progress = new Progress();
		progress.todayDate = "2026-10-13";
		progress.todayTicks = 600;
		assertEquals("0m", new StatsView(progress, new Session(), "2026-10-14").todayTime);
		assertEquals("6m", new StatsView(progress, new Session(), "2026-10-13").todayTime);
	}

	@Test
	public void sessionGraphTakesAReadingEachMinute()
	{
		Session session = new Session();
		for (int tick = 0; tick < 250; tick++)
		{
			session.xp += 2;
			session.standingTick();
		}
		assertArrayEquals(new long[]{0, 200, 400}, new StatsView(new Progress(), session, "2026-10-14").sessionXpSamples);
	}

	@Test
	public void daysKnowWhichDaysWereStoodAndSkipTheFuture()
	{
		Progress progress = new Progress();
		progress.daysStood.add("2026-10-14");
		progress.daysStood.add("2026-10-12");
		progress.daysStood.add("2026-01-01");
		progress.daysStood.add("2026-10-20");
		StatsView.Days days = view(progress).days;
		assertTrue(days.stood(LocalDate.parse("2026-10-12")));
		assertFalse(days.stood(LocalDate.parse("2026-10-13")));
		// After "today" (2026-10-14) doesn't count, even if a save says so.
		assertFalse(days.stood(LocalDate.parse("2026-10-20")));
		assertEquals(2, days.daysStoodIn(YearMonth.of(2026, 10)));
		assertEquals(YearMonth.of(2026, 1), days.first);
	}

	@Test
	public void daysWithNoStandingStartAtThisMonth()
	{
		assertEquals(YearMonth.of(2026, 10), view(new Progress()).days.first);
	}

	@Test
	public void longerDaysGetStrongerShades()
	{
		Progress progress = new Progress();
		progress.daysStood.add("2026-10-12");
		progress.dayTicks.put("2026-10-13", 600L);
		progress.dayTicks.put("2026-10-14", 7000L);
		progress.todayDate = "2026-10-14";
		progress.todayTicks = 20000;
		StatsView.Days days = view(progress).days;
		// Stood, but from before times were kept: the lightest shade.
		assertEquals(1, days.shade(LocalDate.parse("2026-10-12")));
		assertEquals(0, days.ticks(LocalDate.parse("2026-10-12")));
		assertEquals(1, days.shade(LocalDate.parse("2026-10-13")));
		// Today counts the larger of the two records.
		assertEquals(20000, days.ticks(LocalDate.parse("2026-10-14")));
		assertEquals(StatsView.CALENDAR_SHADES, days.shade(LocalDate.parse("2026-10-14")));
		assertEquals(0, days.shade(LocalDate.parse("2026-10-11")));
		assertEquals(20600, days.ticksIn(YearMonth.of(2026, 10)));
		assertEquals(3, days.daysStoodIn(YearMonth.of(2026, 10)));
	}

	@Test
	public void dayDetailsListBanksMostTimeFirst()
	{
		Progress progress = new Progress();
		progress.bank("varrock-west", 1, 1);
		progress.dayBanks.put("2026-10-13", new java.util.TreeMap<>(java.util.Map.of("ge", 1000L, "varrock-west", 4000L)));
		progress.dayTicks.put("2026-10-13", 5000L);
		progress.daysStood.add("2026-10-13");
		progress.daysStood.add("2026-10-12");
		StatsView.Days days = view(progress).days;
		java.util.List<StatsView.DayBank> banks = days.banks(LocalDate.parse("2026-10-13"));
		assertEquals(2, banks.size());
		assertEquals("40m", banks.get(0).time);
		assertEquals(1.0, banks.get(0).share, 0.001);
		assertEquals(0.25, banks.get(1).share, 0.001);
		// Stood before banks were kept per day: nothing to list.
		assertTrue(days.banks(LocalDate.parse("2026-10-12")).isEmpty());
	}

	@Test
	public void bankBarsAreAShareOfTheTopBank()
	{
		Progress progress = new Progress();
		progress.bank("a", 1, 1).ticks = 400;
		progress.bank("b", 2, 2).ticks = 100;
		StatsView view = view(progress);
		assertEquals(1.0, view.banks.get(0).share, 0.001);
		assertEquals(0.25, view.banks.get(1).share, 0.001);
	}

	private static StatsView view(Progress progress)
	{
		return new StatsView(progress, new Session(), "2026-10-14");
	}
}
