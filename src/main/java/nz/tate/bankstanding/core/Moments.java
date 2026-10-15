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
import java.util.EnumSet;
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
	static final long OUT_OF_FAITH_TICKS = Records.TICKS_PER_HOUR;
	static final long OVERTIME_TICKS = 12 * Records.TICKS_PER_HOUR;
	static final int BANK_HOPPER_BANKS = 5;
	static final int WORLD_TOUR_WORLDS = 5;
	static final int ROUGH_PATCH_MIN_MAX_HIT = 20;
	static final long COFFEE_BREAK_TICKS = 1000;
	static final long FULL_SHIFT_TICKS = 8 * Records.TICKS_PER_HOUR;
	static final long LOYAL_CUSTOMER_TICKS = 24 * Records.TICKS_PER_HOUR;
	static final long LIGHTNING_MILLIS = 60 * 60 * 1000L;
	static final int RUSH_HOUR_PLAYERS = 50;
	static final long GOOD_COMPANY_TICKS = 1000;
	static final int ORIGINAL_WORLD = 301;

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

	/** How the moments are grouped in the log. */
	public enum Category
	{
		ODD("Odd moments"),
		CALENDAR("Calendar"),
		STREAKS("Streaks"),
		LUCK("Luck"),
		CROWDS("Crowds"),
		WORLDS("Worlds"),
		POISON("Poison");

		public final String title;

		Category(String title)
		{
			this.title = title;
		}
	}

	/** The moments. Their descriptions are shown, so none of them may touch a bonus. */
	public enum Odd
	{
		MIDNIGHT("odd:midnight", "Midnight", "Be standing as the day ticks over (UTC)", Category.ODD),
		NIGHT_SHIFT("odd:night-shift", "Night shift", "Stand between 3 and 4 in the morning, your time", Category.ODD),
		BACK_TO_BACK("odd:back-to-back", "Back to back", "Two max hits in a row, with a max hit of 10 or more",
			Category.ODD),
		STATUE("odd:statue", "Statue", "Stand for an hour without moving the camera", Category.ODD),
		OUT_OF_FAITH("odd:out-of-faith", "Out of faith", "Stand for an hour with no prayer points", Category.ODD),
		BANK_HOPPER("odd:bank-hopper", "Bank hopper", "Stand at 5 different banks in one login", Category.ODD),
		WORLD_TOUR("odd:world-tour", "World tour", "Stand on 5 different worlds in one day", Category.ODD),
		OVERTIME("odd:overtime", "Overtime", "Stand for 12 hours in one day", Category.ODD),
		FRIDAY_13TH("odd:friday-13th", "Unlucky for some", "Stand on a Friday the 13th", Category.ODD),
		LEAP_DAY("odd:leap-day", "Leap day", "Stand on 29 February", Category.ODD),
		TAKE_A_SEAT("odd:take-a-seat", "Take a seat", "Do the Sit emote while bank standing", Category.ODD),

		EARLY_BIRD("odd:early-bird", "Early bird", "Stand between 5 and 6 in the morning, your time", Category.CALENDAR),
		LUNCH_BREAK("odd:lunch-break", "Lunch break", "Stand between 12 and 1 in the afternoon, your time",
			Category.CALENDAR),
		WEEKEND_WARRIOR("odd:weekend-warrior", "Weekend warrior", "Stand on both days of a weekend", Category.CALENDAR),
		FULL_WEEK("odd:full-week", "Full week", "Stand on every day of the week", Category.CALENDAR),
		ALL_YEAR("odd:all-year", "All year round", "Stand in every month of the year", Category.CALENDAR),

		STREAK_7("odd:streak-7", "Regular", "Stand 7 days in a row", Category.STREAKS),
		STREAK_30("odd:streak-30", "Devoted", "Stand 30 days in a row", Category.STREAKS),
		STREAK_100("odd:streak-100", "Part of the furniture", "Stand 100 days in a row", Category.STREAKS),
		COFFEE_BREAK("odd:coffee-break", "Coffee break", "One stand of 10 minutes", Category.STREAKS),
		FULL_SHIFT("odd:full-shift", "Full shift", "Stand for 8 hours in one day", Category.STREAKS),
		LOYAL_CUSTOMER("odd:loyal-customer", "Loyal customer", "Stand for 24 hours in total at one bank",
			Category.STREAKS),

		FIRST_TRY("odd:first-try", "First try", "A max hit on your first roll of the day", Category.LUCK),
		ROUGH_PATCH("odd:rough-patch", "Rough patch", "Three 1s in a row, with a max hit of 20 or more",
			Category.LUCK),
		LIGHTNING("odd:lightning", "Lightning strikes twice", "Two rare drops within an hour", Category.LUCK),

		RUSH_HOUR("odd:rush-hour", "Rush hour", "Stand with 50 or more players around you", Category.CROWDS),
		GHOST_TOWN("odd:ghost-town", "Ghost town", "Stand at the Grand Exchange with nobody else in sight",
			Category.CROWDS),
		GOOD_COMPANY("odd:good-company", "Good company", "Stand beside another player who's been still for 10 minutes",
			Category.CROWDS),

		LIVING_DANGEROUSLY("odd:living-dangerously", "Living dangerously", "Stand on a PvP world", Category.WORLDS),
		THE_ORIGINAL("odd:the-original", "The original", "Stand on world 301", Category.WORLDS),
		ELITE_COMPANY("odd:elite-company", "Elite company", "Stand on a skill total world", Category.WORLDS),

		POISONED("odd:poisoned", "Poisoned", "Stand while poisoned", Category.POISON),
		OWN_MEDICINE("odd:own-medicine", "Taste of your own medicine",
			"Stand while poisoned and wearing something poisonous", Category.POISON);

		public final String key;
		public final String title;
		public final String description;
		public final Category category;

		Odd(String key, String title, String description, Category category)
		{
			this.key = key;
			this.title = title;
			this.description = description;
			this.category = category;
		}
	}

	/** What a stand needs remembered between ticks to spot the odd moments. Not saved. */
	public static final class Watch
	{
		private LocalDate lastUtcDay;
		private int lastYaw = Integer.MIN_VALUE;
		private int lastPitch = Integer.MIN_VALUE;
		private long stillTicks;
		private long prayerlessTicks;
		private int maxHitsInARow;
		private int onesInARow;
		private LocalDate lastRollDay;
		/** When the last rare drop came, or -1 for none yet. */
		private long lastRareAt = -1;
		private final Set<String> loginBanks = new HashSet<>();
		private LocalDate worldDay;
		private final Set<Integer> worlds = new HashSet<>();

		/** Call when a stand ends. */
		public void standEnded()
		{
			lastUtcDay = null;
			lastYaw = Integer.MIN_VALUE;
			stillTicks = 0;
			prayerlessTicks = 0;
			maxHitsInARow = 0;
			onesInARow = 0;
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
	 * count toward the luck moments, which are returned through {@code odd}.
	 */
	public static Splat drop(Progress p, Watch w, boolean maxHit, boolean boosted, Keywords.Kind phrase,
		int normalMax, long now, Set<Odd> odd)
	{
		return drop(p, w, maxHit ? normalMax : 2, maxHit, boosted, phrase, normalMax, null, now, odd);
	}

	public static Splat drop(Progress p, Watch w, int hit, boolean maxHit, boolean boosted, Keywords.Kind phrase,
		int normalMax, LocalDate utcDay, long now, Set<Odd> odd)
	{
		if (phrase == null)
		{
			w.maxHitsInARow = maxHit && normalMax >= BACK_TO_BACK_MIN_MAX_HIT ? w.maxHitsInARow + 1 : 0;
			if (w.maxHitsInARow >= 2)
			{
				note(p, Odd.BACK_TO_BACK, now, odd);
			}
			w.onesInARow = hit == 1 && normalMax >= ROUGH_PATCH_MIN_MAX_HIT ? w.onesInARow + 1 : 0;
			if (w.onesInARow >= 3)
			{
				note(p, Odd.ROUGH_PATCH, now, odd);
			}
			if (utcDay != null && !utcDay.equals(w.lastRollDay))
			{
				w.lastRollDay = utcDay;
				if (maxHit)
				{
					note(p, Odd.FIRST_TRY, now, odd);
				}
			}
		}
		Splat splat = Splat.of(maxHit, boosted, phrase);
		return record(p, splat.key, now) ? splat : null;
	}

	/** How often sitting down pays out a boosted roll. */
	public static final long SIT_COOLDOWN_MILLIS = 60 * 60 * 1000L;

	/**
	 * Called when the player does the Sit emote during a stand: notes the moment, and returns
	 * true if a boosted roll is due (at most once an hour).
	 */
	public static boolean sat(Progress p, long now, Set<Odd> odd)
	{
		note(p, Odd.TAKE_A_SEAT, now, odd);
		if (now - p.lastSitRoll < SIT_COOLDOWN_MILLIS)
		{
			return false;
		}
		p.lastSitRoll = now;
		return true;
	}

	/** Called for each rare drop; two within an hour is {@link Odd#LIGHTNING}. */
	public static void rareDrop(Progress p, Watch w, long now, Set<Odd> odd)
	{
		if (w.lastRareAt >= 0 && now - w.lastRareAt <= LIGHTNING_MILLIS)
		{
			note(p, Odd.LIGHTNING, now, odd);
		}
		w.lastRareAt = now;
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

		w.prayerlessTicks = t.prayer == 0 ? w.prayerlessTicks + 1 : 0;
		if (w.prayerlessTicks >= OUT_OF_FAITH_TICKS)
		{
			note(p, Odd.OUT_OF_FAITH, t.now, odd);
		}

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

		calendar(p, localDay, t, odd);

		if (t.standTicks >= COFFEE_BREAK_TICKS)
		{
			note(p, Odd.COFFEE_BREAK, t.now, odd);
		}
		if (p.todayTicks >= FULL_SHIFT_TICKS)
		{
			note(p, Odd.FULL_SHIFT, t.now, odd);
		}
		if (t.bankTicks >= LOYAL_CUSTOMER_TICKS)
		{
			note(p, Odd.LOYAL_CUSTOMER, t.now, odd);
		}

		if (t.playersAround >= RUSH_HOUR_PLAYERS)
		{
			note(p, Odd.RUSH_HOUR, t.now, odd);
		}
		if (t.playersAround == 0 && "ge".equals(t.namedBank))
		{
			note(p, Odd.GHOST_TOWN, t.now, odd);
		}
		if (t.stillestNeighbourTicks >= GOOD_COMPANY_TICKS)
		{
			note(p, Odd.GOOD_COMPANY, t.now, odd);
		}

		if (t.pvpWorld)
		{
			note(p, Odd.LIVING_DANGEROUSLY, t.now, odd);
		}
		if (t.world == ORIGINAL_WORLD)
		{
			note(p, Odd.THE_ORIGINAL, t.now, odd);
		}
		if (t.skillTotalWorld)
		{
			note(p, Odd.ELITE_COMPANY, t.now, odd);
		}

		if (t.poisoned)
		{
			note(p, Odd.POISONED, t.now, odd);
			if (t.wearingPoison)
			{
				note(p, Odd.OWN_MEDICINE, t.now, odd);
			}
		}
	}

	/** Notes the local day as stood, and checks the moments that depend on which days. */
	private static void calendar(Progress p, LocalDate day, Tick t, Set<Odd> odd)
	{
		int hour = t.local.getHour();
		if (hour == 5)
		{
			note(p, Odd.EARLY_BIRD, t.now, odd);
		}
		if (hour == 12)
		{
			note(p, Odd.LUNCH_BREAK, t.now, odd);
		}
		if (!p.daysStood.add(day.toString()))
		{
			return;
		}

		if (day.getDayOfWeek() == DayOfWeek.SUNDAY && p.daysStood.contains(day.minusDays(1).toString()))
		{
			note(p, Odd.WEEKEND_WARRIOR, t.now, odd);
		}

		Set<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);
		Set<Month> months = EnumSet.noneOf(Month.class);
		for (String stood : p.daysStood)
		{
			LocalDate date = LocalDate.parse(stood);
			weekdays.add(date.getDayOfWeek());
			months.add(date.getMonth());
		}
		if (weekdays.size() == DayOfWeek.values().length)
		{
			note(p, Odd.FULL_WEEK, t.now, odd);
		}
		if (months.size() == Month.values().length)
		{
			note(p, Odd.ALL_YEAR, t.now, odd);
		}

		int streak = 0;
		while (p.daysStood.contains(day.minusDays(streak).toString()))
		{
			streak++;
		}
		if (streak >= 7)
		{
			note(p, Odd.STREAK_7, t.now, odd);
		}
		if (streak >= 30)
		{
			note(p, Odd.STREAK_30, t.now, odd);
		}
		if (streak >= 100)
		{
			note(p, Odd.STREAK_100, t.now, odd);
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
		public int prayer;
		/** The named bank stood at, or null. */
		public String namedBank;
		public int world;
		/** Ticks in the stand under way, and in total at this bank. */
		public long standTicks;
		public long bankTicks;
		/** Other players in the scene. */
		public int playersAround;
		/** How long the stillest player beside you (within a few tiles) has been still. */
		public long stillestNeighbourTicks;
		public boolean pvpWorld;
		public boolean skillTotalWorld;
		public boolean poisoned;
		/** Wearing a poisoned weapon or a toxic item. */
		public boolean wearingPoison;
	}
}
