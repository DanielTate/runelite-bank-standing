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

/**
 * A joke stat: the Bank Standing XP you'd have earned if your other logged-in time had been
 * spent at a bank. Every logged-in tick without a stand counts, at the average rate for your
 * level then, with no modifiers. Counted from {@link Progress#wastedSince}, which is when a
 * version with this first ran.
 */
public final class XpWasted
{
	private XpWasted()
	{
	}

	/** The XP an average stand earns in one tick at this level. */
	public static double perTick(int level)
	{
		return XpRoller.meanRoll(XpRoller.baseMaxHit(level), XpRoller.BASE_RATIO) / XpRoller.ROLL_INTERVAL_TICKS;
	}

	/** One logged-in tick: standing or not. */
	public static void tick(Progress progress, boolean standing, long nowMillis)
	{
		if (progress.wastedSince == 0)
		{
			progress.wastedSince = nowMillis;
		}
		progress.loggedInTicks++;
		if (standing)
		{
			progress.standingTicksLoggedIn++;
		}
		else
		{
			progress.xpWasted += perTick(progress.level());
		}
	}

	/** The share of logged-in time spent standing, 0 to 100, or -1 before there's any. */
	public static int standingPercent(Progress progress)
	{
		if (progress.loggedInTicks <= 0)
		{
			return -1;
		}
		return (int) Math.round(progress.standingTicksLoggedIn * 100.0 / progress.loggedInTicks);
	}
}
