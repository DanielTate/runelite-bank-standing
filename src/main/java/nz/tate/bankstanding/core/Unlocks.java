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
import java.util.Collections;
import java.util.List;

/**
 * Everything a level unlocks, and the skill guide that lists it. Unlocks only change Bank
 * Standing itself and how it looks to you; none of them touch the hidden bonuses.
 */
public final class Unlocks
{
	public static final int MAX = Experience.MAX_LEVEL;
	public static final int FIDGETS = 25;
	public static final int BANKERS = 30;
	public static final int TEA = 85;
	/** The longest timed peek: five minutes with the bank open, from level 85. */
	public static final int PEEK_TICKS = 500;
	/** At 99 the bank can stay open as long as you like, with no timer. */
	public static final int NO_PEEK_LIMIT = Integer.MAX_VALUE;
	/** The tint that turns an icon gold. */
	public static final int GOLD_TINT = 0xffc83c;
	/** Level, ticks the bank can be open, and the name, from a blink to making yourself at home. */
	private static final Object[][] PEEKS = {
		{5, 8, "Blink"}, {10, 17, "Glance"}, {20, 33, "Quick look"}, {30, 50, "Peek"}, {50, 75, "Long look"},
		{60, 100, "Browse"}, {70, 200, "Rummage"}, {85, PEEK_TICKS, "Leisurely browse"},
		{MAX, NO_PEEK_LIMIT, "Make yourself at home"},
	};

	/** The rug laid out under you while you stand. */
	public enum Rug
	{
		NONE(0, "", 0),
		WORN(10, "A worn rug to stand on", 0x96734f),
		BLUE(40, "A blue rug", 0x466ee6),
		RED(70, "A red carpet", 0xdc2828),
		GOLD(MAX, "A gold carpet", GOLD_TINT);

		public final int level;
		public final String text;
		/** The colour its skill guide icon is tinted. */
		final int tint;

		Rug(int level, String text, int tint)
		{
			this.level = level;
			this.text = text;
			this.tint = tint;
		}
	}

	/** What you sit on while you stand. */
	public enum Seat
	{
		NONE(0, ""),
		STOOL(60, "A stool to sit on"),
		THRONE(MAX, "A gilded throne");

		public final int level;
		public final String text;

		Seat(int level, String text)
		{
			this.level = level;
			this.text = text;
		}
	}

	private static final int[][] START_DELAY = {{1, 50}, {15, 42}, {45, 33}, {75, 25}, {MAX, 17}};
	private static final String[][] TITLES = {
		{"1", "Loiterer"}, {"15", "Regular"}, {"35", "Fixture"}, {"45", "Queue Jumper"}, {"55", "Furniture"},
		{"75", "Part of the Bank"}, {"90", "Honorary Banker"}, {"99", "Bank Standing Master"},
	};

	private Unlocks()
	{
	}

	/** Tiles from a bank: 1 at level 1, then one more every ten levels from 20 to 80, and 9 at 99. */
	public static int bankRange(int level)
	{
		if (level >= MAX)
		{
			return 9;
		}
		return Math.max(1, Math.min(8, level / 10));
	}

	/** Ticks of standing still before the first drop: 30 seconds, down to 10 at 99. */
	public static int startDelayTicks(int level)
	{
		int ticks = START_DELAY[0][1];
		for (int[] step : START_DELAY)
		{
			if (level >= step[0])
			{
				ticks = step[1];
			}
		}
		return ticks;
	}

	/** Ticks the bank can be open without ending a stand; 0 before Blink, {@link #NO_PEEK_LIMIT} at 99. */
	public static int peekTicks(int level)
	{
		int ticks = 0;
		for (Object[] peek : PEEKS)
		{
			if (level >= (int) peek[0])
			{
				ticks = (int) peek[1];
			}
		}
		return ticks;
	}

	/** Whether bankers sometimes bring you a cup of tea. */
	public static boolean tea(int level)
	{
		return level >= TEA;
	}

	public static Rug rug(int level)
	{
		Rug best = Rug.NONE;
		for (Rug rug : Rug.values())
		{
			if (level >= rug.level)
			{
				best = rug;
			}
		}
		return best;
	}

	public static Seat seat(int level)
	{
		return level >= MAX ? Seat.THRONE : level >= Seat.STOOL.level ? Seat.STOOL : Seat.NONE;
	}

	public static boolean fidgets(int level)
	{
		return level >= FIDGETS;
	}

	public static boolean bankersNotice(int level)
	{
		return level >= BANKERS;
	}

	/** Ticks as "5 seconds" or, from a minute, "2 minutes". */
	private static String duration(int ticks)
	{
		long seconds = Math.round(ticks * 0.6);
		if (seconds < 60)
		{
			return seconds + (seconds == 1 ? " second" : " seconds");
		}
		long minutes = seconds / 60;
		return minutes + (minutes == 1 ? " minute" : " minutes");
	}

	/** Which START_DELAY step this level is on, from 0. */
	private static int startStep(int level)
	{
		int step = 0;
		for (int i = 0; i < START_DELAY.length; i++)
		{
			if (level >= START_DELAY[i][0])
			{
				step = i;
			}
		}
		return step;
	}

	/** Which title this level has, from 0. */
	private static int titleStep(int level)
	{
		int step = 0;
		for (int i = 0; i < TITLES.length; i++)
		{
			if (level >= Integer.parseInt(TITLES[i][0]))
			{
				step = i;
			}
		}
		return step;
	}

	public static String title(int level)
	{
		String title = TITLES[0][1];
		for (String[] step : TITLES)
		{
			if (level >= Integer.parseInt(step[0]))
			{
				title = step[1];
			}
		}
		return title;
	}

	/** The skill guide's sidebar, like a real skill's categories. */
	public enum Category
	{
		REACH("Reach"),
		TIMING("Timing"),
		COMFORT("Comfort"),
		COMPANY("Company"),
		TITLES("Titles"),
		CAPE("Cape");

		public final String title;

		Category(String title)
		{
			this.title = title;
		}
	}

	// Icons: item IDs, or sprite IDs for the emotes.
	/** Boots for each tile of reach, better boots the further you can stand. */
	private static final int[] REACH_ICONS = {1061, 4119, 4121, 4123, 4127, 4129, 4131, 11840, 13239};
	/** An hourglass, an ornate hourglass, a watch and a stopwatch, one per START_DELAY step after the first. */
	private static final int[] START_ICONS = {5610, 33742, 2575, 27414};
	/**
	 * One per peek: an eye of newt, an eye patch, a lens, a lantern, goggles, a bank key, a
	 * coin pouch and a banker's note, then a gold banker's note at 99.
	 */
	private static final int[] PEEK_ICONS = {221, 1025, 603, 4550, 9472, 13302, 22521, 28767, 28767};
	/** One per title: an old boot, a beer, nails, boots of lightness, a chair, a gold bar, a briefcase, a crown. */
	private static final int[] TITLE_ICONS = {685, 1917, 1539, 88, 8310, 2357, 30361, 12397};
	/** A flat rug, tinted to match each rug laid out under you. */
	private static final int RUG_ICON = 25362;
	private static final int STOOL_ICON = 8826;
	private static final int THRONE_ICON = 8360;
	private static final int COINS = 995;
	/** A porcelain cup of tea; the same cup appears beside you. */
	public static final int CUP_OF_TEA = 4245;
	private static final int CAPE_ICON = 23351;
	private static final int YAWN_SPRITE = 718;
	private static final int SKILLCAPE_SPRITE = 736;

	/** One line of the skill guide. */
	public static final class Unlock
	{
		public final int level;
		public final String text;
		public final Category category;
		/** The icon: an item, or -1. */
		public final int item;
		/** The icon when it's a sprite instead of an item, or -1. */
		public final int sprite;
		/** The colour the icon is tinted, keeping its light and shade, or 0 for none. */
		public final int tint;

		Unlock(int level, String text, Category category, int item, int sprite, boolean gold)
		{
			this(level, text, category, item, sprite, gold ? GOLD_TINT : 0);
		}

		Unlock(int level, String text, Category category, int item, int sprite, int tint)
		{
			this.level = level;
			this.text = text;
			this.category = category;
			this.item = item;
			this.sprite = sprite;
			this.tint = tint;
		}
	}

	/** The skill guide, in level order. */
	public static final List<Unlock> ALL;

	static
	{
		List<Unlock> all = new ArrayList<>();
		for (int level = 1; level <= MAX; level++)
		{
			if (level == 1 || bankRange(level) > bankRange(level - 1))
			{
				all.add(new Unlock(level, "Bank stand within " + bankRange(level) + (level == 1 ? " tile" : " tiles"),
					Category.REACH, REACH_ICONS[Math.min(bankRange(level), REACH_ICONS.length) - 1], -1, false));
			}
			if (level > 1 && startDelayTicks(level) < startDelayTicks(level - 1))
			{
				all.add(new Unlock(level, "XP starts after " + Math.round(startDelayTicks(level) * 0.6) + " seconds",
					Category.TIMING, START_ICONS[Math.min(startStep(level), START_ICONS.length) - 1], -1, false));
			}
			for (Rug rug : Rug.values())
			{
				if (rug != Rug.NONE && rug.level == level)
				{
					all.add(new Unlock(level, rug.text, Category.COMFORT, RUG_ICON, -1, rug.tint));
				}
			}
			for (Seat seat : Seat.values())
			{
				if (seat != Seat.NONE && seat.level == level)
				{
					all.add(new Unlock(level, seat.text, Category.COMFORT,
						seat == Seat.THRONE ? THRONE_ICON : STOOL_ICON, -1, false));
				}
			}
			if (level == FIDGETS)
			{
				all.add(new Unlock(level, "You fidget while you wait", Category.COMPANY, -1, YAWN_SPRITE, false));
			}
			if (level == BANKERS)
			{
				all.add(new Unlock(level, "Bankers notice you", Category.COMPANY, COINS, -1, false));
			}
			for (int i = 0; i < PEEKS.length; i++)
			{
				Object[] peek = PEEKS[i];
				if (level == (int) peek[0])
				{
					int ticks = (int) peek[1];
					String time = ticks == NO_PEEK_LIMIT ? "as long as you like" : "for " + duration(ticks);
					all.add(new Unlock(level, peek[2] + ": open the bank " + time, Category.TIMING, PEEK_ICONS[i], -1,
						ticks == NO_PEEK_LIMIT));
				}
			}
			if (level == TEA)
			{
				all.add(new Unlock(level, "Bankers bring you tea", Category.COMPANY, CUP_OF_TEA, -1, false));
			}
			if (level == 1 || !title(level).equals(title(level - 1)))
			{
				all.add(new Unlock(level, title(level), Category.TITLES, TITLE_ICONS[titleStep(level)], -1, false));
			}
		}
		all.add(new Unlock(MAX, "Bank Standing cape, worn while you stand", Category.CAPE, CAPE_ICON, -1, true));
		all.add(new Unlock(MAX, "Cape emote", Category.CAPE, -1, SKILLCAPE_SPRITE, false));
		ALL = Collections.unmodifiableList(all);
	}

	/** The guide lines in one category, in level order. */
	public static List<Unlock> in(Category category)
	{
		List<Unlock> lines = new ArrayList<>();
		for (Unlock unlock : ALL)
		{
			if (unlock.category == category)
			{
				lines.add(unlock);
			}
		}
		return lines;
	}

	/** The next level that unlocks something, or -1 at 99. */
	public static int nextUnlockLevel(int level)
	{
		for (Unlock unlock : ALL)
		{
			if (unlock.level > level)
			{
				return unlock.level;
			}
		}
		return -1;
	}
}
