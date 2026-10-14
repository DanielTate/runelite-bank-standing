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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What the side panel shows, worked out from saved progress. Results only: nothing here
 * hints at why a drop was bigger.
 */
public final class StatsView
{
	public static final class BankRow
	{
		public final String name;
		public final String time;
		public final String xp;

		BankRow(String name, String time, String xp)
		{
			this.name = name;
			this.time = time;
			this.xp = xp;
		}
	}

	public final int level;
	/** The title for this level, like "Regular". */
	public final String title;
	public final String totalXp;
	public final String toNextLevel;
	/** Percent through the current level, 0 to 100. */
	public final int percent;
	public final List<BankRow> banks;

	public final String sessionXp;
	public final String sessionTime;
	public final String xpPerHour;
	public final String todayTime;

	public StatsView(Progress progress, Session session, String today)
	{
		sessionXp = number(session.xp);
		sessionTime = duration(session.ticks);
		xpPerHour = number(session.xpPerHour());
		todayTime = duration(today.equals(progress.todayDate) ? progress.todayTicks : 0);

		level = progress.level();
		title = Unlocks.title(level);
		totalXp = number(progress.xp);
		if (level >= Experience.MAX_LEVEL)
		{
			toNextLevel = "Maxed";
			percent = 100;
		}
		else
		{
			int start = Experience.xpForLevel(level);
			int next = Experience.xpForLevel(level + 1);
			toNextLevel = number(next - progress.xp);
			percent = (int) ((progress.xp - start) * 100L / (next - start));
		}

		List<Map.Entry<String, BankRecord>> entries = new ArrayList<>(progress.banks.entrySet());
		entries.sort(Comparator.comparingLong((Map.Entry<String, BankRecord> e) -> e.getValue().ticks).reversed());
		banks = new ArrayList<>();
		for (Map.Entry<String, BankRecord> entry : entries)
		{
			BankRecord record = entry.getValue();
			banks.add(new BankRow(BankNames.displayName(entry.getKey(), record),
				duration(record.ticks), number(record.xp)));
		}
	}

	static String number(long value)
	{
		return String.format(Locale.ENGLISH, "%,d", value);
	}

	/** "3h 05m", or "12m" under an hour. */
	static String duration(long ticks)
	{
		long minutes = ticks * 600 / 60_000;
		if (minutes < 60)
		{
			return minutes + "m";
		}
		return String.format(Locale.ENGLISH, "%dh %02dm", minutes / 60, minutes % 60);
	}
}
