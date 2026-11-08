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

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
		/** Time here as a share of the top bank's, 0 to 1, for the row's bar. */
		public final double share;
		/** 1 for the most-stood bank. */
		public final int rank;
		/** The collection log region it's listed under, or null for an unlisted bank. */
		public final String region;
		/** Time here as a percent of all bank time, like "42%", or "<1%". */
		public final String percent;
		/** "12 visits", or null before visits were counted. */
		public final String visits;
		/** When you first stood here, or null if not known. */
		public final String firstStood;

		BankRow(String name, String time, String xp, double share, int rank, String region, String percent,
			String visits, String firstStood)
		{
			this.name = name;
			this.time = time;
			this.xp = xp;
			this.share = share;
			this.rank = rank;
			this.region = region;
			this.percent = percent;
			this.visits = visits;
			this.firstStood = firstStood;
		}
	}

	/** One bank stood at on a day, for the calendar's day details. */
	public static final class DayBank
	{
		public final String name;
		public final String time;
		/** Time here as a share of that day's top bank, 0 to 1. */
		public final double share;

		DayBank(String name, String time, double share)
		{
			this.name = name;
			this.time = time;
			this.share = share;
		}
	}

	/** How many shades the days-stood calendar uses for days stood, lightest first. */
	public static final int CALENDAR_SHADES = 4;
	/** The least standing for each shade above the first: 30 minutes, 1 hour, 3 hours. */
	private static final long[] SHADE_TICKS = {3000, 6000, 18000};

	/**
	 * Every day stood and, for the last year, how long, for the side panel's month calendar.
	 * A copy, so the panel can page through months while the game keeps saving.
	 */
	public static final class Days
	{
		public final LocalDate today;
		/** The earliest month with any standing, or this month if there's none. */
		public final YearMonth first;
		private final Map<String, Long> ticks;
		private final Set<String> stood;
		private final Map<String, Map<String, Long>> banks;
		private final Map<String, String> bankNames = new HashMap<>();

		Days(Progress p, LocalDate today)
		{
			this.today = today;
			banks = new HashMap<>();
			for (Map.Entry<String, Map<String, Long>> day : p.dayBanks.entrySet())
			{
				banks.put(day.getKey(), new HashMap<>(day.getValue()));
			}
			for (Map.Entry<String, BankRecord> bank : p.banks.entrySet())
			{
				bankNames.put(bank.getKey(), BankNames.displayName(bank.getKey(), bank.getValue()));
			}
			ticks = new HashMap<>(p.dayTicks);
			if (p.todayDate != null)
			{
				ticks.merge(p.todayDate, p.todayTicks, Math::max);
			}
			stood = new HashSet<>(p.daysStood);
			for (Map.Entry<String, Long> entry : ticks.entrySet())
			{
				if (entry.getValue() > 0)
				{
					stood.add(entry.getKey());
				}
			}
			YearMonth earliest = YearMonth.from(today);
			for (String day : stood)
			{
				try
				{
					YearMonth month = YearMonth.from(LocalDate.parse(day));
					if (month.isBefore(earliest))
					{
						earliest = month;
					}
				}
				catch (DateTimeParseException e)
				{
					// Not a date: skip it.
				}
			}
			first = earliest;
		}

		public boolean stood(LocalDate day)
		{
			return !day.isAfter(today) && stood.contains(day.toString());
		}

		/** Standing ticks that day, or 0 when not stood or not known (days from before they were kept). */
		public long ticks(LocalDate day)
		{
			return day.isAfter(today) ? 0 : ticks.getOrDefault(day.toString(), 0L);
		}

		/** 0 for not stood, else 1 (a short stand, or time not known) to {@link #CALENDAR_SHADES} (3 hours or more). */
		public int shade(LocalDate day)
		{
			if (!stood(day))
			{
				return 0;
			}
			int shade = 1;
			for (long least : SHADE_TICKS)
			{
				if (ticks(day) >= least)
				{
					shade++;
				}
			}
			return shade;
		}

		/**
		 * The banks stood at on a day, most time first, or an empty list when that wasn't
		 * recorded (days from before 1.8) or the day wasn't stood.
		 */
		public List<DayBank> banks(LocalDate day)
		{
			List<DayBank> list = new ArrayList<>();
			Map<String, Long> stoodAt = day.isAfter(today) ? null : banks.get(day.toString());
			if (stoodAt == null || stoodAt.isEmpty())
			{
				return list;
			}
			long top = 0;
			for (long time : stoodAt.values())
			{
				top = Math.max(top, time);
			}
			List<Map.Entry<String, Long>> entries = new ArrayList<>(stoodAt.entrySet());
			entries.sort(Map.Entry.<String, Long>comparingByValue().reversed());
			for (Map.Entry<String, Long> entry : entries)
			{
				String name = bankNames.getOrDefault(entry.getKey(), entry.getKey());
				list.add(new DayBank(name, duration(entry.getValue()), top > 0 ? (double) entry.getValue() / top : 0));
			}
			return list;
		}

		public int daysStoodIn(YearMonth month)
		{
			int days = 0;
			for (int d = 1; d <= month.lengthOfMonth(); d++)
			{
				days += stood(month.atDay(d)) ? 1 : 0;
			}
			return days;
		}

		public long ticksIn(YearMonth month)
		{
			long total = 0;
			for (int d = 1; d <= month.lengthOfMonth(); d++)
			{
				total += ticks(month.atDay(d));
			}
			return total;
		}
	}

	public final int level;
	/** The title for this level, like "Regular". */
	public final String title;
	public final String totalXp;
	public final String toNextLevel;
	/** How long the next level takes at this session's XP per hour: "-" before there's a rate, null at 99. */
	public final String timeToLevel;
	/** Percent through the current level, 0 to 100. */
	public final int percent;
	public final List<BankRow> banks;

	public final String sessionXp;
	public final String sessionTime;
	public final String xpPerHour;
	public final String todayTime;
	/** This session's XP after each minute of standing, starting from 0. */
	public final long[] sessionXpSamples;
	public final Days days;

	/** Personal records as label and value pairs, in the order shown. */
	public final List<String[]> records = new ArrayList<>();

	public StatsView(Progress progress, Session session, String today)
	{
		sessionXp = number(session.xp);
		sessionTime = duration(session.ticks);
		xpPerHour = number(session.xpPerHour());
		todayTime = duration(today.equals(progress.todayDate) ? progress.todayTicks : 0);
		sessionXpSamples = session.xpSamples.stream().mapToLong(Long::longValue).toArray();
		days = new Days(progress, LocalDate.parse(today));

		level = progress.level();
		title = Unlocks.title(level);
		totalXp = number(progress.xp);
		if (level >= Experience.MAX_LEVEL)
		{
			toNextLevel = "Maxed";
			timeToLevel = null;
			percent = 100;
		}
		else
		{
			int start = Experience.xpForLevel(level);
			int next = Experience.xpForLevel(level + 1);
			toNextLevel = number(next - progress.xp);
			long rate = session.xpPerHour();
			timeToLevel = rate == 0 ? "-" : duration((next - progress.xp) * Session.TICKS_PER_HOUR / rate);
			percent = (int) ((progress.xp - start) * 100L / (next - start));
		}

		addRecords(progress.records);

		List<Map.Entry<String, BankRecord>> entries = new ArrayList<>(progress.banks.entrySet());
		entries.sort(Comparator.comparingLong((Map.Entry<String, BankRecord> e) -> e.getValue().ticks).reversed());
		banks = new ArrayList<>();
		long top = entries.isEmpty() ? 0 : entries.get(0).getValue().ticks;
		long all = 0;
		for (Map.Entry<String, BankRecord> entry : entries)
		{
			all += entry.getValue().ticks;
		}
		for (Map.Entry<String, BankRecord> entry : entries)
		{
			BankRecord record = entry.getValue();
			banks.add(new BankRow(BankNames.displayName(entry.getKey(), record),
				duration(record.ticks), number(record.xp), top > 0 ? (double) record.ticks / top : 0,
				banks.size() + 1, BankRegions.regionOf(entry.getKey()), percent(record.ticks, all),
				record.visits > 0 ? number(record.visits) + (record.visits == 1 ? " visit" : " visits") : null,
				record.firstAt > 0 ? date(record.firstAt) : null));
		}
	}

	private void addRecords(Records r)
	{
		if (r.firstSeen > 0)
		{
			records.add(new String[]{"Started", date(r.firstSeen)});
		}
		records.add(new String[]{"Total standing", duration(r.totalTicks)});
		records.add(new String[]{"Longest stand", duration(r.longestStandTicks)});
		records.add(new String[]{"Best day", duration(r.bestDayTicks)});
		records.add(new String[]{"Best XP per hour", r.bestXpPerHour > 0 ? number(r.bestXpPerHour) : "-"});
		reached(r, "level", Experience.MAX_LEVEL, "Reached 99");
		reached(r, "xp", Experience.MAX_XP, "Reached 200m");
	}

	/** "2026-10-15 (183 days)": when a milestone was reached and how long it took. */
	private void reached(Records r, String kind, int value, String label)
	{
		Records.Milestone milestone = r.find(kind, value);
		if (milestone != null)
		{
			long days = (milestone.at - r.firstSeen) / 86_400_000L;
			records.add(new String[]{label, date(milestone.at) + " (" + days + (days == 1 ? " day)" : " days)")});
		}
	}

	/** A part of a whole as a whole percent, with "<1%" for anything smaller but not nothing. */
	static String percent(long part, long whole)
	{
		if (whole <= 0)
		{
			return "0%";
		}
		long percent = Math.round(part * 100.0 / whole);
		return percent == 0 && part > 0 ? "<1%" : percent + "%";
	}

	public static String date(long epochMillis)
	{
		return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString();
	}

	static String number(long value)
	{
		return String.format(Locale.ENGLISH, "%,d", value);
	}

	/** "3h 05m", or "12m" under an hour. */
	public static String duration(long ticks)
	{
		long minutes = ticks * 600 / 60_000;
		if (minutes < 60)
		{
			return minutes + "m";
		}
		return String.format(Locale.ENGLISH, "%dh %02dm", minutes / 60, minutes % 60);
	}
}
