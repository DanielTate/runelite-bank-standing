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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashSet;
import java.util.Set;

/**
 * Things the collection log notes the first time they happen: each kind of hitsplat, each
 * holiday stood through, and a few odd moments. They're saved in {@link Progress#moments} by
 * key, with when they first happened. None of them hint at the hidden bonuses.
 */
public final class Moments
{
	/** Saved in place of a time for things that happened before the log tracked them. */
	public static final long BEFORE_TRACKING = 0;

	static final int BACK_TO_BACK_MIN_MAX_HIT = 10;
	static final long STATUE_TICKS = Records.TICKS_PER_HOUR;
	static final long OVERTIME_TICKS = 12 * Records.TICKS_PER_HOUR;
	static final int BANK_HOPPER_BANKS = 5;
	static final int WORLD_TOUR_WORLDS = 5;

	/** The hitsplats a drop can show. */
	public enum Splat
	{
		HIT("splat:hit", "Hit", null),
		MAX_HIT("splat:max-hit", "Max hit", null),
		BOOSTED("splat:boosted", "Above the max", null),
		PHRASE("splat:phrase", "Phrase", Keywords.Kind.ANYWHERE),
		PHRASE_BANK("splat:phrase-bank", "Phrase at a bank", Keywords.Kind.LOCATION),
		PHRASE_DATE("splat:phrase-date", "Holiday phrase", Keywords.Kind.DATE),
		PHRASE_BOTH("splat:phrase-both", "Holiday phrase at a bank", Keywords.Kind.DATE_AND_LOCATION);

		public final String key;
		public final String title;
		/** For a phrase splat, the kind of phrase it's for. */
		public final Keywords.Kind phrase;

		Splat(String key, String title, Keywords.Kind phrase)
		{
			this.key = key;
			this.title = title;
			this.phrase = phrase;
		}

		/** The splat a drop shows: a phrase's own, else boosted, else max hit, else a plain hit. */
		public static Splat of(boolean maxHit, boolean boosted, Keywords.Kind phrase)
		{
			if (phrase != null)
			{
				for (Splat splat : values())
				{
					if (splat.phrase == phrase)
					{
						return splat;
					}
				}
			}
			return boosted ? BOOSTED : maxHit ? MAX_HIT : HIT;
		}
	}

	/** Holidays, as the log names them. */
	public enum HolidayEntry
	{
		CHRISTMAS(Holiday.CHRISTMAS, "Christmas", "20 to 31 December"),
		NEW_YEAR(Holiday.NEW_YEAR, "New Year", "31 December to 2 January"),
		HALLOWEEN(Holiday.HALLOWEEN, "Halloween", "25 October to 1 November"),
		EASTER(Holiday.EASTER, "Easter", "Easter Sunday, three days either side"),
		BIRTHDAY(Holiday.BIRTHDAY, "RuneScape's birthday", "20 to 24 February"),
		FALADOR_MASSACRE(Holiday.FALADOR_MASSACRE, "Falador Massacre", "6 June");

		public final Holiday holiday;
		public final String title;
		public final String when;

		HolidayEntry(Holiday holiday, String title, String when)
		{
			this.holiday = holiday;
			this.title = title;
			this.when = when;
		}

		public String key()
		{
			return "holiday:" + holiday.name().toLowerCase();
		}
	}

	/** The odd moments. Their descriptions are shown, so none of them may touch a bonus. */
	public enum Odd
	{
		MIDNIGHT("odd:midnight", "Midnight", "Be standing as the day ticks over (UTC)"),
		NIGHT_SHIFT("odd:night-shift", "Night shift", "Stand between 3 and 4 in the morning, your time"),
		BACK_TO_BACK("odd:back-to-back", "Back to back", "Two max hits in a row, with a max hit of 10 or more"),
		STATUE("odd:statue", "Statue", "Stand for an hour without moving the camera"),
		BANK_HOPPER("odd:bank-hopper", "Bank hopper", "Stand at 5 different banks in one login"),
		WORLD_TOUR("odd:world-tour", "World tour", "Stand on 5 different worlds in one day"),
		OVERTIME("odd:overtime", "Overtime", "Stand for 12 hours in one day"),
		FRIDAY_13TH("odd:friday-13th", "Unlucky for some", "Stand on a Friday the 13th"),
		LEAP_DAY("odd:leap-day", "Leap day", "Stand on 29 February");

		public final String key;
		public final String title;
		public final String description;

		Odd(String key, String title, String description)
		{
			this.key = key;
			this.title = title;
			this.description = description;
		}
	}

	/** What a stand needs remembered between ticks to spot the odd moments. Not saved. */
	public static final class Watch
	{
		private LocalDate lastUtcDay;
		private int lastYaw = Integer.MIN_VALUE;
		private int lastPitch = Integer.MIN_VALUE;
		private long stillTicks;
		private int maxHitsInARow;
		private final Set<String> loginBanks = new HashSet<>();
		private LocalDate worldDay;
		private final Set<Integer> worlds = new HashSet<>();

		/** Call when a stand ends. */
		public void standEnded()
		{
			lastUtcDay = null;
			lastYaw = Integer.MIN_VALUE;
			stillTicks = 0;
			maxHitsInARow = 0;
		}

		/** Call on logging out or hopping. Worlds are kept, since hopping is the point. */
		public void loggedOut()
		{
			standEnded();
			loginBanks.clear();
		}
	}

	private Moments()
	{
	}

	public static boolean has(Progress p, String key)
	{
		return p.moments.containsKey(key);
	}

	/** Notes a moment; true the first time. */
	public static boolean record(Progress p, String key, long now)
	{
		return p.moments.putIfAbsent(key, now) == null;
	}

	/** Fills in what earlier saves already show happened: plain hits, and phrase splats. */
	public static void backfill(Progress p, Keywords keywords)
	{
		if (p.rolls > 0 || p.xp > 0)
		{
			p.moments.putIfAbsent(Splat.HIT.key, BEFORE_TRACKING);
		}
		for (Keywords.Kind kind : keywords.foundKinds(p))
		{
			p.moments.putIfAbsent(Splat.of(false, false, kind).key, BEFORE_TRACKING);
		}
	}

	/**
	 * Called for every drop; returns the splat if it's the first of its kind. Normal drops also
	 * count toward {@link Odd#BACK_TO_BACK}, which is returned through {@code odd}.
	 */
	public static Splat drop(Progress p, Watch w, boolean maxHit, boolean boosted, Keywords.Kind phrase,
		int normalMax, long now, Set<Odd> odd)
	{
		if (phrase == null)
		{
			w.maxHitsInARow = maxHit && normalMax >= BACK_TO_BACK_MIN_MAX_HIT ? w.maxHitsInARow + 1 : 0;
			if (w.maxHitsInARow >= 2 && record(p, Odd.BACK_TO_BACK.key, now))
			{
				odd.add(Odd.BACK_TO_BACK);
			}
		}
		Splat splat = Splat.of(maxHit, boosted, phrase);
		return record(p, splat.key, now) ? splat : null;
	}

	/** Called for every standing tick; returns holidays and odd moments that are new. */
	public static void standingTick(Progress p, Watch w, Tick t, Set<HolidayEntry> holidays, Set<Odd> odd)
	{
		for (HolidayEntry entry : HolidayEntry.values())
		{
			if (entry.holiday.isOn(t.utcDay) && record(p, entry.key(), t.now))
			{
				holidays.add(entry);
			}
		}

		if (w.lastUtcDay != null && !w.lastUtcDay.equals(t.utcDay))
		{
			note(p, Odd.MIDNIGHT, t.now, odd);
		}
		w.lastUtcDay = t.utcDay;

		if (t.local.getHour() == 3)
		{
			note(p, Odd.NIGHT_SHIFT, t.now, odd);
		}
		LocalDate localDay = t.local.toLocalDate();
		if (localDay.getDayOfMonth() == 13 && localDay.getDayOfWeek() == DayOfWeek.FRIDAY)
		{
			note(p, Odd.FRIDAY_13TH, t.now, odd);
		}
		if (localDay.getMonth() == Month.FEBRUARY && localDay.getDayOfMonth() == 29)
		{
			note(p, Odd.LEAP_DAY, t.now, odd);
		}

		if (t.yaw == w.lastYaw && t.pitch == w.lastPitch)
		{
			if (++w.stillTicks >= STATUE_TICKS)
			{
				note(p, Odd.STATUE, t.now, odd);
			}
		}
		else
		{
			w.stillTicks = 0;
		}
		w.lastYaw = t.yaw;
		w.lastPitch = t.pitch;

		if (t.namedBank != null)
		{
			w.loginBanks.add(t.namedBank);
			if (w.loginBanks.size() >= BANK_HOPPER_BANKS)
			{
				note(p, Odd.BANK_HOPPER, t.now, odd);
			}
		}

		if (!t.utcDay.equals(w.worldDay))
		{
			w.worldDay = t.utcDay;
			w.worlds.clear();
		}
		w.worlds.add(t.world);
		if (w.worlds.size() >= WORLD_TOUR_WORLDS)
		{
			note(p, Odd.WORLD_TOUR, t.now, odd);
		}

		if (p.todayTicks >= OVERTIME_TICKS)
		{
			note(p, Odd.OVERTIME, t.now, odd);
		}
	}

	private static void note(Progress p, Odd moment, long now, Set<Odd> odd)
	{
		if (record(p, moment.key, now))
		{
			odd.add(moment);
		}
	}

	/** One standing tick, as the odd moments see it. */
	public static final class Tick
	{
		public long now;
		public LocalDate utcDay;
		public LocalDateTime local;
		public int yaw;
		public int pitch;
		/** The named bank stood at, or null. */
		public String namedBank;
		public int world;
	}
}
