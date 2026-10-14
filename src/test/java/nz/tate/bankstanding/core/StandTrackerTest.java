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
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class StandTrackerTest
{
	private final StandTracker tracker = new StandTracker();

	private int stand(int ticks)
	{
		int rolls = 0;
		for (int i = 0; i < ticks; i++)
		{
			if (tracker.tick(3185, 3436, 0, true, false))
			{
				rolls++;
			}
		}
		return rolls;
	}

	@Test
	public void firstDropComesAfterThirtySecondsThenEveryInterval()
	{
		int interval = XpRoller.ROLL_INTERVAL_TICKS;
		assertEquals(0, stand(StandTracker.START_DELAY_TICKS));
		assertFalse(tracker.isStanding());
		assertEquals(0, stand(interval - 1));
		assertTrue(tracker.isStanding());
		assertEquals(1, stand(1));
		assertEquals(10, stand(interval * 10));
	}

	@Test
	public void walkingRestartsTheDelay()
	{
		stand(60);
		assertFalse(tracker.tick(3186, 3436, 0, true, false));
		assertFalse(tracker.isStanding());
		assertEquals(0, stand(StandTracker.START_DELAY_TICKS));
	}

	@Test
	public void usingTheBankRestartsTheDelay()
	{
		stand(60);
		tracker.tick(3185, 3436, 0, true, true);
		assertFalse(tracker.isStanding());
	}

	@Test
	public void leavingRangeRestartsTheDelay()
	{
		stand(60);
		tracker.tick(3185, 3436, 0, false, false);
		assertFalse(tracker.isStanding());
	}

	@Test
	public void realXpRestartsTheDelay()
	{
		stand(60);
		tracker.realXpGained();
		assertFalse(tracker.isStanding());
		assertEquals(0, stand(StandTracker.START_DELAY_TICKS));
	}

	@Test
	public void theFirstPositionAfterResetIsNotMovement()
	{
		stand(10);
		tracker.reset();
		assertFalse(tracker.tick(3000, 3000, 0, true, false));
		for (int i = 1; i < StandTracker.START_DELAY_TICKS + 4; i++)
		{
			tracker.tick(3000, 3000, 0, true, false);
		}
		assertTrue(tracker.isStanding());
	}
}
