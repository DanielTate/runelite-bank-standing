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

/** A UTC day counts once you've earned Bank Standing XP for 30 minutes of it. */
public final class DailyStreak
{
	static final long TICKS_PER_DAY_NEEDED = 3000;

	private DailyStreak()
	{
	}

	/** Call once per standing tick. */
	public static void standingTick(Progress p, LocalDate today)
	{
		String day = today.toString();
		if (!day.equals(p.todayDate))
		{
			p.todayDate = day;
			p.todayTicks = 0;
		}
		p.todayTicks++;
		if (p.todayTicks == TICKS_PER_DAY_NEEDED)
		{
			boolean continues = today.minusDays(1).toString().equals(p.streakDate);
			p.streakDays = continues ? p.streakDays + 1 : 1;
			p.streakDate = day;
		}
	}

	/** The streak as it stands today: it survives until a whole day is missed. */
	public static int current(Progress p, LocalDate today)
	{
		if (today.toString().equals(p.streakDate) || today.minusDays(1).toString().equals(p.streakDate))
		{
			return p.streakDays;
		}
		return 0;
	}
}
