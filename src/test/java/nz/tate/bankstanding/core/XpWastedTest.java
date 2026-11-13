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

import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class XpWastedTest
{
	@Test
	public void countsOnlyTimeNotStanding()
	{
		Progress p = new Progress();
		XpWasted.tick(p, true, 1000);
		XpWasted.tick(p, false, 2000);
		XpWasted.tick(p, false, 3000);
		XpWasted.tick(p, false, 4000);
		assertEquals(1000, p.wastedSince);
		assertEquals(4, p.loggedInTicks);
		assertEquals(25, XpWasted.standingPercent(p));
		assertEquals(3 * XpWasted.perTick(1), p.xpWasted, 1e-9);
	}

	@Test
	public void anHourWastedAt99IsAnHourOfAverageStanding()
	{
		double perHour = XpWasted.perTick(99) * Session.TICKS_PER_HOUR;
		assertEquals(XpRoller.meanRoll(50, XpRoller.BASE_RATIO) * 1000, perHour, 1e-6);
	}

	@Test
	public void nothingYetIsMinusOne()
	{
		assertEquals(-1, XpWasted.standingPercent(new Progress()));
	}

	@Test
	public void mergeKeepsTheCopyThatCountedLongest()
	{
		Progress a = new Progress();
		a.xp = 100;
		a.loggedInTicks = 10;
		a.standingTicksLoggedIn = 5;
		a.xpWasted = 20;
		a.wastedSince = 500;
		Progress b = new Progress();
		b.loggedInTicks = 40;
		b.standingTicksLoggedIn = 10;
		b.xpWasted = 90;
		b.wastedSince = 300;
		Progress merged = Progress.merge(Arrays.asList(a, b));
		assertEquals(40, merged.loggedInTicks);
		assertEquals(10, merged.standingTicksLoggedIn);
		assertEquals(90, merged.xpWasted, 0);
		assertEquals(300, merged.wastedSince);
	}
}
