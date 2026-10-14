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

/**
 * Pins the headline promise: about 200 days of idle time to 99 with typical modifiers,
 * computed from the expected roll at every level rather than by simulation.
 */
public class BalanceTest
{
	private static final double SECONDS_PER_ROLL = XpRoller.ROLL_INTERVAL_TICKS * 0.6;

	@Test
	public void noModifiersTakesAbout210Days()
	{
		assertEquals(210, daysTo99(0), 3);
	}

	@Test
	public void typicalModifiersTakeAbout200Days()
	{
		assertEquals(200, daysTo99(0.05), 3);
	}

	@Test
	public void everythingStackedTakesAbout191Days()
	{
		assertEquals(191, daysTo99(XpRoller.MAX_BOOST), 3);
	}

	private static double daysTo99(double boost)
	{
		double seconds = 0;
		for (int level = 1; level < Experience.MAX_LEVEL; level++)
		{
			int needed = Experience.xpForLevel(level + 1) - Experience.xpForLevel(level);
			double perRoll = XpRoller.meanRoll(XpRoller.baseMaxHit(level), XpRoller.BASE_RATIO) * (1 + boost);
			seconds += needed / perRoll * SECONDS_PER_ROLL;
		}
		return seconds / 86_400;
	}
}
