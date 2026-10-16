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
	public static final int QUICK_PEEK = 65;
	/** How long the bank can stay open without ending a stand, once Quick peek is unlocked. */
	public static final int PEEK_TICKS = 17;

	/** The rug laid out under you while you stand. */
	public enum Rug
	{
		NONE(0, ""),
		WORN(10, "A worn rug to stand on"),
		BLUE(40, "A blue rug"),
		RED(70, "A red carpet"),
		GOLD(MAX, "A gold carpet");

		public final int level;
		public final String text;

		Rug(int level, String text)
		{
			this.level = level;
			this.text = text;
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
		{"1", "Loiterer"}, {"15", "Regular"}, {"35", "Fixture"}, {"55", "Furniture"},
		{"75", "Part of the Bank"}, {"99", "Bank Standing Master"},
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

	/** Ticks the bank can be open without ending a stand; 0 before Quick peek. */
	public static int peekTicks(int level)
	{
		return level >= QUICK_PEEK ? PEEK_TICKS : 0;
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
	private static final int BOOTS = 11860;
	private static final int WATCH = 2575;
	private static final int BANKERS_NOTE = 28767;
	private static final int[] RUG_ICONS = {8316, 8317, 8318, 8318};
	private static final int STOOL_ICON = 8826;
	private static final int THRONE_ICON = 8360;
	private static final int COINS = 995;
	private static final int SCROLL = 10485;
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
		/** Whether the icon is shown turned gold. */
		public final boolean gold;

		Unlock(int level, String text, Category category, int item, int sprite, boolean gold)
		{
			this.level = level;
			this.text = text;
			this.category = category;
			this.item = item;
			this.sprite = sprite;
			this.gold = gold;
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
					Category.REACH, BOOTS, -1, false));
			}
			if (level > 1 && startDelayTicks(level) < startDelayTicks(level - 1))
			{
				all.add(new Unlock(level, "XP starts after " + Math.round(startDelayTicks(level) * 0.6) + " seconds",
					Category.TIMING, WATCH, -1, false));
			}
			for (Rug rug : Rug.values())
			{
				if (rug != Rug.NONE && rug.level == level)
				{
					all.add(new Unlock(level, rug.text, Category.COMFORT, RUG_ICONS[rug.ordinal() - 1], -1,
						rug == Rug.GOLD));
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
			if (level == QUICK_PEEK)
			{
				all.add(new Unlock(level, "Quick peek: open the bank for 10 seconds", Category.TIMING,
					BANKERS_NOTE, -1, false));
			}
			if (level == 1 || !title(level).equals(title(level - 1)))
			{
				all.add(new Unlock(level, title(level), Category.TITLES, SCROLL, -1, level >= MAX));
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
