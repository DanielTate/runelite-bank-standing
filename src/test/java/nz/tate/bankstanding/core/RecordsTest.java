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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RecordsTest
{
	private static final long START = 1_700_000_000_000L;
	private static final long TICK = 600;

	private final Progress progress = new Progress();
	private final Session session = new Session();

	/** Stands for {@code ticks}, earning {@code xpPerRoll} each roll, like the plugin does. */
	private long stand(long now, int ticks, int xpPerRoll)
	{
		for (int i = 1; i <= ticks; i++)
		{
			now += TICK;
			session.ticks++;
			DailyStreak.standingTick(progress, java.time.LocalDate.of(2026, 1, 1));
			progress.records.standingTick(progress, session, now);
			if (i % XpRoller.ROLL_INTERVAL_TICKS == 0)
			{
				int before = progress.xp;
				progress.xp = Experience.addXp(progress.xp, xpPerRoll);
				session.xp += progress.xp - before;
				progress.records.gainedXp(before, progress.xp, now);
			}
		}
		return now;
	}

	@Test
	public void startsFromAnExistingSave()
	{
		progress.xp = 5000;
		progress.bank("varrock-west", 3185, 3436).ticks = 1234;
		progress.records.begin(progress, START);
		assertEquals(START, progress.records.firstSeen);
		assertEquals(1234, progress.records.totalTicks);
		assertEquals("start", progress.records.milestones.get(0).kind);

		progress.records.begin(progress, START + 999);
		assertEquals(START, progress.records.firstSeen);
	}

	@Test
	public void honestPlayHasAConsistentHistory()
	{
		progress.records.begin(progress, START);
		long now = stand(START, 20_000, 10);
		assertTrue(progress.records.check(progress, now).isEmpty());
		assertNotNull(progress.records.find("level", 10));
		assertNotNull(progress.records.find("stand", 1));
		assertNotNull(progress.records.find("stand", 3));
		assertEquals(20_000, progress.records.longestStandTicks);
		assertTrue(progress.records.bestXpPerHour > 0);
		assertEquals(20_000, progress.records.bestDayTicks);
	}

	@Test
	public void aStandEndingResetsTheCurrentStand()
	{
		progress.records.begin(progress, START);
		long now = stand(START, 100, 1);
		progress.records.standEnded();
		stand(now, 50, 1);
		assertEquals(100, progress.records.longestStandTicks);
		assertEquals(150, progress.records.totalTicks);
	}

	@Test
	public void editedXpIsCaught()
	{
		progress.records.begin(progress, START);
		long now = stand(START, 600, 5);
		progress.xp = 50_000_000;
		assertFalse(progress.records.check(progress, now).isEmpty());
	}

	@Test
	public void tooMuchStandingTimeIsCaught()
	{
		progress.records.begin(progress, START);
		long now = stand(START, 6000, 5);
		Records.Milestone hour = progress.records.find("stand", 1);
		hour.at = START + 60_000;
		assertFalse(progress.records.check(progress, now).isEmpty());
	}

	@Test
	public void developerChangesAreFlagged()
	{
		progress.records.begin(progress, START);
		progress.records.edited = true;
		assertFalse(progress.records.check(progress, START).isEmpty());
	}

	@Test
	public void theBestRateNeedsHalfAnHour()
	{
		progress.records.begin(progress, START);
		stand(START, 2000, 50);
		assertEquals(0, progress.records.bestXpPerHour);
	}
}
