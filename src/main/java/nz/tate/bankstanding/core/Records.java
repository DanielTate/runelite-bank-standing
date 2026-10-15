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
import java.util.List;

/**
 * Personal records and a timestamped history of milestones, saved with {@link Progress}.
 * Field names are permanent: they are the save format.
 *
 * <p>Every milestone notes the wall-clock time, the total standing time and the XP at that
 * moment, so the history can be checked with {@link #check}: nobody can stand longer than real
 * time allows, or gain more XP than standing that long can give. It can't stop an edited save,
 * but it keeps a consistent story that a leaderboard could check later.
 */
public final class Records
{
	public static final long TICKS_PER_HOUR = 6000;
	public static final long TICKS_PER_MINUTE = TICKS_PER_HOUR / 60;
	/**
	 * Single stands this long, in minutes, are milestones. The game logs you out after six
	 * hours, so the longest leaves room for logging in and the start delay.
	 */
	public static final int[] STAND_MINUTES = {60, 180, 350};
	/** XP totals that are milestones. */
	static final int[] XP_MARKS = {100_000_000, Experience.MAX_XP};
	/** Sessions shorter than this don't count for the best XP per hour. */
	static final long MIN_RATE_TICKS = 3000;
	private static final long MILLIS_PER_TICK = 600;
	private static final long MILLIS_PER_DAY = 86_400_000L;

	/** One moment in the history. {@code kind} is "start", "level", "stand" or "xp". */
	public static final class Milestone
	{
		public String kind;
		/** The level, the stand length in minutes (hours in saves from 1.1 and before), or the XP total. */
		public int value;
		/** Wall-clock time, in epoch milliseconds. */
		public long at;
		public long totalTicks;
		public int xp;

		Milestone(String kind, int value, long at, long totalTicks, int xp)
		{
			this.kind = kind;
			this.value = value;
			this.at = at;
			this.totalTicks = totalTicks;
			this.xp = xp;
		}
	}

	/** When the plugin first saw this account. */
	public long firstSeen;
	/** All standing time ever (from when records began, plus bank totals before that). */
	public long totalTicks;
	public long longestStandTicks;
	public long bestDayTicks;
	public String bestDayDate;
	public long bestXpPerHour;
	/** Set by the developer tools, which change XP directly. */
	public boolean edited;
	public List<Milestone> milestones = new ArrayList<>();

	/** Not saved: the stand under way. */
	private transient long standTicks;

	/**
	 * Starts the history the first time an account is seen. Saves from before records existed
	 * already have XP and bank time, so those become the starting point.
	 */
	public void begin(Progress progress, long now)
	{
		if (firstSeen != 0)
		{
			return;
		}
		firstSeen = now;
		for (BankRecord bank : progress.banks.values())
		{
			totalTicks += bank.ticks;
		}
		milestones.add(new Milestone("start", progress.level(), now, totalTicks, progress.xp));
	}

	/** Call once for every tick spent standing. */
	public void standingTick(Progress progress, Session session, long now)
	{
		totalTicks++;
		standTicks++;
		if (standTicks > longestStandTicks)
		{
			longestStandTicks = standTicks;
		}
		for (int minutes : STAND_MINUTES)
		{
			if (standTicks == minutes * TICKS_PER_MINUTE && findStand(minutes) == null)
			{
				milestones.add(new Milestone("stand", minutes, now, totalTicks, progress.xp));
			}
		}
		if (progress.todayTicks > bestDayTicks)
		{
			bestDayTicks = progress.todayTicks;
			bestDayDate = progress.todayDate;
		}
		if (session.ticks >= MIN_RATE_TICKS && session.xpPerHour() > bestXpPerHour)
		{
			bestXpPerHour = session.xpPerHour();
		}
	}

	/** Ticks in the stand under way. */
	public long standTicks()
	{
		return standTicks;
	}

	/** Call when a stand ends, for any reason. */
	public void standEnded()
	{
		standTicks = 0;
	}

	/** Call after XP is added: notes every level and XP mark passed on the way. */
	public void gainedXp(int before, int after, long now)
	{
		int fromLevel = Experience.levelForXp(before);
		int toLevel = Experience.levelForXp(after);
		for (int level = fromLevel + 1; level <= toLevel; level++)
		{
			milestones.add(new Milestone("level", level, now, totalTicks, after));
		}
		for (int mark : XP_MARKS)
		{
			if (before < mark && after >= mark)
			{
				milestones.add(new Milestone("xp", mark, now, totalTicks, after));
			}
		}
	}

	/** When a milestone was first reached, or null. */
	public Milestone find(String kind, int value)
	{
		for (Milestone milestone : milestones)
		{
			if (milestone.kind.equals(kind) && milestone.value == value)
			{
				return milestone;
			}
		}
		return null;
	}

	/** The milestone for a single stand this many minutes long; older saves stored hours. */
	public Milestone findStand(int minutes)
	{
		Milestone found = find("stand", minutes);
		if (found == null && minutes % 60 == 0)
		{
			found = find("stand", minutes / 60);
		}
		return found;
	}

	/**
	 * Replays the history and lists anything that couldn't have happened: standing time that
	 * outruns the clock, XP faster than the best possible rate, or time going backwards. An
	 * empty list means the history is consistent.
	 */
	public List<String> check(Progress progress, long now)
	{
		List<String> problems = new ArrayList<>();
		if (edited)
		{
			problems.add("changed with the developer tools");
		}
		Milestone last = null;
		for (Milestone milestone : milestones)
		{
			String name = milestone.kind + " " + milestone.value;
			if (milestone.at < firstSeen)
			{
				problems.add(name + " is dated before the account was first seen");
			}
			if (last != null)
			{
				long wall = milestone.at - last.at;
				long ticks = milestone.totalTicks - last.totalTicks;
				if (wall < 0 || ticks < 0)
				{
					problems.add(name + " goes back in time");
				}
				else
				{
					// One tick of slack per minute for clock jitter.
					if (ticks > wall / MILLIS_PER_TICK + wall / 60_000 + 1)
					{
						problems.add(name + " needs more standing time than had passed");
					}
					if (milestone.xp - last.xp > maxXp(ticks, wall))
					{
						problems.add(name + " gained XP faster than standing allows");
					}
				}
			}
			last = milestone;
		}
		if (last != null && progress.xp - last.xp > maxXp(totalTicks - last.totalTicks, now - last.at))
		{
			problems.add("XP since the last milestone is more than standing allows");
		}
		return problems;
	}

	/**
	 * The most XP {@code ticks} of standing, over {@code wallMillis} of real time, can give:
	 * every roll at the top max hit, plus every chat bonus allowed on each day that passed.
	 */
	static long maxXp(long ticks, long wallMillis)
	{
		long top = XpRoller.maxHit(Experience.MAX_LEVEL, XpRoller.MAX_BONUS_HIT);
		long days = Math.max(0, wallMillis) / MILLIS_PER_DAY + 2;
		return (ticks / XpRoller.ROLL_INTERVAL_TICKS + 1) * top + days * Keywords.DAILY_CAP * top;
	}
}
