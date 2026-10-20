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
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
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
	/** Below this a max hit is too easy to count for the log: at a max of 2 it's every other drop. */
	static final int MAX_HIT_LOG_MIN_MAX_HIT = 10;
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
	static final long DROP_PARTY_TICKS = 100;
	static final long PERSONAL_SPACE_TICKS = 1000;
	static final int CLUE_TIERS = 6;
	static final long TOURIST_TICKS = 1000;
	static final int CROWD_PLEASER_PLAYERS = 20;
	static final long FREQUENT_FLYER_MILLIS = 2 * 60 * 1000L;
	static final long BILLION = 1_000_000_000L;
	static final int QUICK_CHANGE_OUTFITS = 5;
	static final List<String> MOLE_BANKS = Arrays.asList("keldagrim", "dorgesh-kaan", "tzhaar", "camdozaal");
	static final int MOLE_PEOPLE_BANKS = 3;
	static final List<String> ISLAND_BANKS = Arrays.asList("lunar-isle", "neitiznot", "jatizso", "ape-atoll",
		"fossil-island", "mos-le-harmless", "isle-of-souls", "etceteria");
	static final int ISLAND_HOPPER_BANKS = 5;
	static final int QUEUE_FOR_FRED_PLAYERS = 5;
	static final long SAD_GHOST_MILLIS = 10 * 60 * 1000L;
	static final String ARCHIVE_WEST = "fractured-archive-west";
	static final String ARCHIVE_EAST = "fractured-archive-east";
	static final List<String> KOUREND_BANKS = Arrays.asList("hosidius", "arceuus", "lovakengj", "shayzien",
		"port-piscarilius");

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
		POISON("Poison"),
		PLACES("Places");

		public final String title;

		Category(String title)
		{
			this.title = title;
		}
	}

	/**
	 * The moments. Their descriptions are shown, so none of them may touch a bonus. A hidden
	 * one shows as a secret, with only its hint, until it's found.
	 */
	public enum Odd
	{
		MIDNIGHT("odd:midnight", "Midnight", "Be standing as the day ticks over", Category.ODD),
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
		SAD_GHOST("odd:sad-ghost", "Sad ghost", "Stand at the Fractured Archive within 10 minutes of dying",
			Category.ODD),
		SPECTATOR("odd:spectator", "Spectator", "Stand for an hour at the Fractured Archive's west chest", Category.ODD),

		EARLY_BIRD("odd:early-bird", "Early bird", "Stand between 5 and 6 in the morning, your time", Category.CALENDAR),
		LUNCH_BREAK("odd:lunch-break", "Lunch break", "Stand between 12 and 1 in the afternoon, your time",
			Category.CALENDAR),
		WEEKEND_WARRIOR("odd:weekend-warrior", "Weekend warrior", "Stand on both days of a weekend", Category.CALENDAR),
		FULL_WEEK("odd:full-week", "Full week", "Stand on every day of the week", Category.CALENDAR),
		ALL_YEAR("odd:all-year", "All year round", "Stand in every month of the year", Category.CALENDAR),
		ARCHIVE_ANNIVERSARY("odd:archive-anniversary", "Anniversary",
			"Stand at the Fractured Archive on 20 October, the day it opened", Category.CALENDAR),

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
		QUEUE_FOR_FRED("odd:queue-for-fred", "Queue for Fred",
			"Stand at the Fractured Archive with 5 or more players around you", Category.CROWDS),
		ALONE_IN_THE_ARCHIVE("odd:alone-in-the-archive", "Alone in the archive",
			"Stand at the Fractured Archive with nobody else in sight", Category.CROWDS),

		LIVING_DANGEROUSLY("odd:living-dangerously", "Living dangerously", "Stand on a PvP world", Category.WORLDS),
		THE_ORIGINAL("odd:the-original", "The original", "Stand on world 301", Category.WORLDS),
		ELITE_COMPANY("odd:elite-company", "Elite company", "Stand on a skill total world", Category.WORLDS),

		POISONED("odd:poisoned", "Poisoned", "Stand while poisoned", Category.POISON),
		OWN_MEDICINE("odd:own-medicine", "Taste of your own medicine",
			"Stand while poisoned and wearing something poisonous", Category.POISON),

		HOT_SEAT("odd:hot-seat", "Hot seat", "Things are heating up at the bank.", Category.ODD, true),
		WALK_IT_OFF("odd:walk-it-off", "Walk it off", "Time heals all wounds.", Category.ODD, true),
		NOT_NOW("odd:not-now", "Not now", "Some visitors are best ignored.", Category.ODD, true),
		NOT_BUDGING("odd:not-budging", "Not budging", "Stand your ground.", Category.ODD, true),
		MAX_CASH("odd:max-cash", "Max cash", "Your pockets can't hold any more.", Category.ODD, true),
		CLUE_HOARDER("odd:clue-hoarder", "Clue hoarder", "The treasure can wait.", Category.ODD, true),
		APRIL_FOOLS("odd:april-fools", "April Fools", "No joke.", Category.CALENDAR, true),
		PARTY_POOPER("odd:party-pooper", "Party pooper", "Not everyone likes a party.", Category.CROWDS, true),
		PERSONAL_SPACE("odd:personal-space", "Personal space", "A little too close for comfort.", Category.CROWDS,
			true),
		FORGOT_SOMETHING("odd:forgot-something", "Forgot something?", "Some islanders expect the right book.",
			Category.ODD, true),
		MONTOYAS_BRIEFING("odd:montoyas-briefing", "Montoya's briefing", "The west side has its own word.",
			Category.ODD, true),
		SIGNED_THE_REGISTER("odd:signed-the-register", "Signed the register", "The east side keeps records.",
			Category.ODD, true),

		WHERE_IT_BEGAN("place:where-it-all-began", "Where it all began", "Before the mainland."),
		OFF_THE_MAP("place:off-the-map", "Off the map", "Not every bank is on the map."),
		BOLD_CHOICE("place:bold-choice", "Bold choice", "Some banks come with a skull warning."),
		MOLE_PEOPLE("place:mole-people", "Mole people", "Never see the sun."),
		ISLAND_HOPPER("place:island-hopper", "Island hopper", "Bring a boat."),
		SIBLING_RIVALRY("place:sibling-rivalry", "Sibling rivalry", "Pick a side. Or don't."),
		NIGHT_HEIST("place:night-heist", "Night heist", "Thieves keep odd hours."),
		GRAVEYARD_SHIFT("place:graveyard-shift", "Graveyard shift", "The dead don't sleep either."),
		FEELING_THE_HEAT("place:feeling-the-heat", "Feeling the heat", "Hot enough for you?"),
		OFF_SEASON("place:off-season", "Off season", "Wrong time of year."),
		TOURIST_TRAP("place:tourist-trap", "Tourist trap", "Through the gate and no further."),
		GRAND_TOUR("place:grand-tour", "Grand tour", "See the whole kingdom."),
		FREQUENT_FLYER("place:frequent-flyer", "Frequent flyer", "Quick on your feet."),
		RAGS_TO_RICHES("place:rags-to-riches", "Rags to riches", "From nothing to everything."),
		KITCHEN_NIGHTMARE("place:kitchen-nightmare", "Kitchen nightmare", "Not really a bank."),
		ELVEN_WELCOME("place:elven-welcome", "Elven welcome", "Hidden in the trees."),
		CROWD_PLEASER("place:crowd-pleaser", "Crowd pleaser", "Old habits die hard."),
		SEA_LEGS("place:sea-legs", "Sea legs", "Steady on."),
		NEIGHBOURHOOD_WATCH("place:neighbourhood-watch", "Neighbourhood watch", "Someone has to keep an eye on it."),
		HIGH_SOCIETY("place:high-society", "High society", "Dress for the bank you want."),
		QUICK_CHANGE("place:quick-change", "Quick change", "Dress for every occasion."),
		FREDS_WAITING_ROOM("place:freds-waiting-room", "Fred's waiting room", "Beneath Lumbridge, behind a cliff face."),
		WEST_WING_EAST_WING("place:west-wing-east-wing", "West wing, east wing", "Strategy or grouping?"),
		LAST_STOP("place:last-stop", "Last stop", "No banking once you're inside."),
		GUTHIX_SLEEPS("place:guthix-sleeps", "Guthix sleeps", "Balance in all things.");

		public final String key;
		public final String title;
		/** What to do, or for a hidden moment only a hint. */
		public final String description;
		public final Category category;
		public final boolean hidden;

		Odd(String key, String title, String description, Category category)
		{
			this(key, title, description, category, false);
		}

		/** A hidden place. */
		Odd(String key, String title, String hint)
		{
			this(key, title, hint, Category.PLACES, true);
		}

		Odd(String key, String title, String description, Category category, boolean hidden)
		{
			this.key = key;
			this.title = title;
			this.description = description;
			this.category = category;
			this.hidden = hidden;
		}
	}

	/** What a stand needs remembered between ticks to spot the odd moments. Not saved. */
	public static final class Watch
	{
		private LocalDate lastDay;
		private int lastYaw = Integer.MIN_VALUE;
		private int lastPitch = Integer.MIN_VALUE;
		private long stillTicks;
		private long prayerlessTicks;
		private int maxHitsInARow;
		private int onesInARow;
		/** When the last rare drop came, or -1 for none yet. */
		private long lastRareAt = -1;
		private final Set<String> loginBanks = new HashSet<>();
		private LocalDate worldDay;
		private final Set<Integer> worlds = new HashSet<>();
		private long balloonTicks;
		private long sharedTileTicks;
		private long nightWatchTicks;
		/** Healing from 1 hitpoint without eating: under way, and the last hitpoints seen. */
		private boolean healing;
		private int lastHitpoints;
		/** The region of the bank last stood at, and when. */
		private String lastRegion;
		private long lastRegionAt;

		/** Call when a stand ends. */
		public void standEnded()
		{
			lastDay = null;
			lastYaw = Integer.MIN_VALUE;
			stillTicks = 0;
			prayerlessTicks = 0;
			maxHitsInARow = 0;
			onesInARow = 0;
			balloonTicks = 0;
			sharedTileTicks = 0;
			nightWatchTicks = 0;
			healing = false;
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
		int normalMax, LocalDate day, long now, Set<Odd> odd)
	{
		boolean logMax = maxHit && normalMax >= MAX_HIT_LOG_MIN_MAX_HIT;
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
			if (day != null && !day.toString().equals(p.firstRollDate))
			{
				p.firstRollDate = day.toString();
				if (logMax)
				{
					note(p, Odd.FIRST_TRY, now, odd);
				}
			}
		}
		Splat splat = Splat.of(logMax, boosted, phrase);
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

	/**
	 * Called each tick the player stands on a fire near a bank: notes the moment, and returns
	 * true if a boosted roll is due (the first time, then at most once an hour).
	 */
	public static boolean onFire(Progress p, long now, Set<Odd> odd)
	{
		note(p, Odd.HOT_SEAT, now, odd);
		if (p.lastFireRoll != 0 && now - p.lastFireRoll < SIT_COOLDOWN_MILLIS)
		{
			return false;
		}
		p.lastFireRoll = now;
		return true;
	}

	/** Notes a moment the plugin spots itself, like a random event left waiting. */
	public static void notice(Progress p, Odd moment, long now, Set<Odd> odd)
	{
		note(p, moment, now, odd);
	}

	/** Called with the outfits that counted toward a roll, for {@link Odd#QUICK_CHANGE}. */
	public static void outfitsWorn(Progress p, LocalDate day, Collection<String> outfitKeys, long now, Set<Odd> odd)
	{
		Set<String> notes = notes(p, day);
		int outfits = 0;
		for (String key : outfitKeys)
		{
			notes.add(key);
		}
		for (String note : notes)
		{
			if (note.startsWith(Modifiers.OUTFIT))
			{
				outfits++;
			}
		}
		if (outfits >= QUICK_CHANGE_OUTFITS)
		{
			note(p, Odd.QUICK_CHANGE, now, odd);
		}
	}

	/** What's been done today toward the one-day moments, cleared when the day changes. */
	private static Set<String> notes(Progress p, LocalDate day)
	{
		String date = day.toString();
		if (!date.equals(p.notesDate))
		{
			p.notesDate = date;
			p.notes.clear();
		}
		return p.notes;
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
			if (entry.holiday.isOn(t.day) && record(p, entry.key(), t.now))
			{
				holidays.add(entry);
			}
		}

		if (w.lastDay != null && !w.lastDay.equals(t.day))
		{
			note(p, Odd.MIDNIGHT, t.now, odd);
		}
		w.lastDay = t.day;

		if (t.local.getHour() == 3)
		{
			note(p, Odd.NIGHT_SHIFT, t.now, odd);
		}
		LocalDate localDay = t.day;
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

		if (!t.day.equals(w.worldDay))
		{
			w.worldDay = t.day;
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

		hidden(p, w, t, localDay, odd);
		places(p, w, t, odd);
	}

	/** The hidden moments that aren't places. */
	private static void hidden(Progress p, Watch w, Tick t, LocalDate localDay, Set<Odd> odd)
	{
		if (localDay.getMonth() == Month.APRIL && localDay.getDayOfMonth() == 1)
		{
			note(p, Odd.APRIL_FOOLS, t.now, odd);
		}

		w.balloonTicks = t.balloons && "falador-east".equals(t.namedBank) ? w.balloonTicks + 1 : 0;
		if (w.balloonTicks >= DROP_PARTY_TICKS)
		{
			note(p, Odd.PARTY_POOPER, t.now, odd);
		}

		w.sharedTileTicks = t.sharedTile ? w.sharedTileTicks + 1 : 0;
		if ("lunar-isle".equals(t.namedBank) && t.spellbook != null && t.spellbook != Spellbook.LUNAR)
		{
			note(p, Odd.FORGOT_SOMETHING, t.now, odd);
		}
		if (w.sharedTileTicks >= PERSONAL_SPACE_TICKS)
		{
			note(p, Odd.PERSONAL_SPACE, t.now, odd);
		}

		if (t.maxCash)
		{
			note(p, Odd.MAX_CASH, t.now, odd);
		}
		if (t.clueTiers >= CLUE_TIERS)
		{
			note(p, Odd.CLUE_HOARDER, t.now, odd);
		}

		// From 1 hitpoint back to full in one stand, one point at a time and without eating.
		if (w.healing)
		{
			if (t.eating || t.hitpoints - w.lastHitpoints > 1)
			{
				w.healing = false;
			}
			else if (t.maxHitpoints > 1 && t.hitpoints >= t.maxHitpoints)
			{
				w.healing = false;
				t.healed = true;
				note(p, Odd.WALK_IT_OFF, t.now, odd);
			}
			w.lastHitpoints = t.hitpoints;
		}
		if (!w.healing && t.hitpoints == 1 && !t.eating)
		{
			w.healing = true;
			w.lastHitpoints = 1;
		}
	}

	private static void places(Progress p, Watch w, Tick t, Set<Odd> odd)
	{
		String bank = t.namedBank;
		if (bank == null)
		{
			return;
		}
		int hour = t.local.getHour();
		Set<String> notes = notes(p, t.day);
		notes.add("bank:" + bank);

		placeAt(p, bank, "tutorial-island", Odd.WHERE_IT_BEGAN, t.now, odd);
		placeAt(p, bank, "zanaris", Odd.OFF_THE_MAP, t.now, odd);
		placeAt(p, bank, "mage-arena", Odd.BOLD_CHOICE, t.now, odd);
		placeAt(p, bank, "culinaromancers-chest", Odd.KITCHEN_NIGHTMARE, t.now, odd);
		placeAt(p, bank, "lletya", Odd.ELVEN_WELCOME, t.now, odd);
		archive(p, t, bank, odd);

		if (visited(notes, MOLE_BANKS) >= MOLE_PEOPLE_BANKS)
		{
			note(p, Odd.MOLE_PEOPLE, t.now, odd);
		}
		if (visited(notes, ISLAND_BANKS) >= ISLAND_HOPPER_BANKS)
		{
			note(p, Odd.ISLAND_HOPPER, t.now, odd);
		}
		if (visited(notes, KOUREND_BANKS) == KOUREND_BANKS.size())
		{
			note(p, Odd.GRAND_TOUR, t.now, odd);
		}
		if (w.loginBanks.contains("neitiznot") && w.loginBanks.contains("jatizso"))
		{
			note(p, Odd.SIBLING_RIVALRY, t.now, odd);
		}

		if (bank.equals("rogues-den") && (hour == 2 || hour == 3))
		{
			note(p, Odd.NIGHT_HEIST, t.now, odd);
		}
		if (bank.equals("port-phasmatys") && hour == 0)
		{
			note(p, Odd.GRAVEYARD_SHIFT, t.now, odd);
		}
		if (bank.equals("tzhaar") && t.standTicks >= Records.TICKS_PER_HOUR)
		{
			note(p, Odd.FEELING_THE_HEAT, t.now, odd);
		}
		Month month = t.local.getMonth();
		if (bank.equals("wintertodt") && (month == Month.JUNE || month == Month.JULY || month == Month.AUGUST))
		{
			note(p, Odd.OFF_SEASON, t.now, odd);
		}
		if (bank.equals("shantay-pass") && t.standTicks >= TOURIST_TICKS)
		{
			note(p, Odd.TOURIST_TRAP, t.now, odd);
		}
		if (bank.equals("emirs-arena") && t.playersAround >= CROWD_PLEASER_PLAYERS)
		{
			note(p, Odd.CROWD_PLEASER, t.now, odd);
		}
		if (bank.equals("bank-boat") && t.standTicks >= Records.TICKS_PER_HOUR)
		{
			note(p, Odd.SEA_LEGS, t.now, odd);
		}
		w.nightWatchTicks = bank.equals("draynor") && (hour >= 22 || hour < 6) ? w.nightWatchTicks + 1 : 0;
		if (w.nightWatchTicks >= Records.TICKS_PER_HOUR)
		{
			note(p, Odd.NEIGHBOURHOOD_WATCH, t.now, odd);
		}
		if (bank.equals("varrock-west") && t.wornValue >= BILLION)
		{
			note(p, Odd.HIGH_SOCIETY, t.now, odd);
		}
		if (bank.equals("ge"))
		{
			if (t.emptyHanded)
			{
				notes.add("ge-empty-handed");
			}
			else if (t.wornValue >= BILLION && notes.contains("ge-empty-handed"))
			{
				note(p, Odd.RAGS_TO_RICHES, t.now, odd);
			}
		}

		String region = BankRegions.regionOf(bank);
		if (region != null)
		{
			if (w.lastRegion != null && !region.equals(w.lastRegion) && t.now - w.lastRegionAt <= FREQUENT_FLYER_MILLIS)
			{
				note(p, Odd.FREQUENT_FLYER, t.now, odd);
			}
			w.lastRegion = region;
			w.lastRegionAt = t.now;
		}
	}

	/** The Fractured Archive's two chests, in the raid lobby under Lumbridge. */
	public static boolean atArchive(String bank)
	{
		return ARCHIVE_WEST.equals(bank) || ARCHIVE_EAST.equals(bank);
	}

	private static void archive(Progress p, Tick t, String bank, Set<Odd> odd)
	{
		if (!atArchive(bank))
		{
			return;
		}
		note(p, Odd.FREDS_WAITING_ROOM, t.now, odd);
		if (p.banks.containsKey(ARCHIVE_WEST) && p.banks.containsKey(ARCHIVE_EAST))
		{
			note(p, Odd.WEST_WING_EAST_WING, t.now, odd);
		}
		if (t.inventoryEmpty)
		{
			note(p, Odd.LAST_STOP, t.now, odd);
		}
		if (t.prayer == 0)
		{
			note(p, Odd.GUTHIX_SLEEPS, t.now, odd);
		}
		if (t.playersAround >= QUEUE_FOR_FRED_PLAYERS)
		{
			note(p, Odd.QUEUE_FOR_FRED, t.now, odd);
		}
		if (t.playersAround == 0)
		{
			note(p, Odd.ALONE_IN_THE_ARCHIVE, t.now, odd);
		}
		if (t.diedAt > 0 && t.now - t.diedAt <= SAD_GHOST_MILLIS)
		{
			note(p, Odd.SAD_GHOST, t.now, odd);
		}
		if (t.local.getMonth() == Month.OCTOBER && t.local.getDayOfMonth() == 20)
		{
			note(p, Odd.ARCHIVE_ANNIVERSARY, t.now, odd);
		}
		if (bank.equals(ARCHIVE_WEST) && t.standTicks >= Records.TICKS_PER_HOUR)
		{
			note(p, Odd.SPECTATOR, t.now, odd);
		}
	}

	/** A phrase that only works at one of the Archive's chests was said there. */
	public static void archivePhrase(Progress p, String bank, long now, Set<Odd> odd)
	{
		if (ARCHIVE_WEST.equals(bank))
		{
			note(p, Odd.MONTOYAS_BRIEFING, now, odd);
		}
		else if (ARCHIVE_EAST.equals(bank))
		{
			note(p, Odd.SIGNED_THE_REGISTER, now, odd);
		}
	}

	private static void placeAt(Progress p, String bank, String place, Odd moment, long now, Set<Odd> odd)
	{
		if (bank.equals(place))
		{
			note(p, moment, now, odd);
		}
	}

	private static int visited(Set<String> notes, List<String> banks)
	{
		int visited = 0;
		for (String bank : banks)
		{
			if (notes.contains("bank:" + bank))
			{
				visited++;
			}
		}
		return visited;
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
		public LocalDate day;
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
		/** Current and real hitpoints, and whether an eating or drinking animation is playing. */
		public int hitpoints;
		public int maxHitpoints;
		public boolean eating;
		/** Drop party balloons in the scene. */
		public boolean balloons;
		/** Another player on your own tile. */
		public boolean sharedTile;
		/** A full stack of coins in the inventory. */
		public boolean maxCash;
		/** How many different clue scroll tiers are in the inventory. */
		public int clueTiers;
		/** Nothing worn and nothing carried. */
		public boolean emptyHanded;
		/** Nothing in the inventory, whatever's worn. */
		public boolean inventoryEmpty;
		/** When you last died, in epoch milliseconds, or 0 if not this session. */
		public long diedAt;
		public long wornValue;
		/** The spellbook you're on, or null if unknown. */
		public Spellbook spellbook;
		/** Set by the moments when hitpoints made it from 1 back to full without eating. */
		public boolean healed;
	}
}
