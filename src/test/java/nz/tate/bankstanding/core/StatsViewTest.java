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

public class StatsViewTest
{
	@Test
	public void showsLevelProgressAndRemainingXp()
	{
		Progress progress = new Progress();
		progress.xp = 83 + 46;
		StatsView view = new StatsView(progress);

		assertEquals(2, view.level);
		assertEquals("129", view.totalXp);
		assertEquals("45", view.toNextLevel);
		assertEquals(50, view.percent);
	}

	@Test
	public void level99IsMaxedButXpStillShows()
	{
		Progress progress = new Progress();
		progress.xp = 20_000_000;
		StatsView view = new StatsView(progress);

		assertEquals("Maxed", view.toNextLevel);
		assertEquals("20,000,000", view.totalXp);
	}

	@Test
	public void banksAreSortedByTimeSpent()
	{
		Progress progress = new Progress();
		progress.bank("edgeville", 0, 0).ticks = 100;
		progress.bank("ge", 0, 0).ticks = 6000 * 3 + 500;

		StatsView view = new StatsView(progress);

		assertEquals("Grand Exchange", view.banks.get(0).name);
		assertEquals("3h 05m", view.banks.get(0).time);
		assertEquals("1m", view.banks.get(1).time);
	}
}
